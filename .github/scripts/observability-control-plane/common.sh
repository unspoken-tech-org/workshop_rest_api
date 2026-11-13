#!/usr/bin/env bash
# shellcheck disable=SC2034  # variáveis usadas pelos scripts que carregam este arquivo
# Funções comuns aos scripts remotos do plano de controle. Cada script é
# enviado junto com este arquivo para DEPLOY_DIR/.tmp e o carrega por source.
#
# Layout remoto:
#   DEPLOY_DIR/releases/<release-id>/  Compose, configs e .env de um deploy
#   DEPLOY_DIR/current                 symlink para o release ativo (troca atômica)
#   DEPLOY_DIR/.tmp/                   pacote e scripts da execução corrente
#   DEPLOY_DIR/.tmp/.deployment-started  release anterior, até a confirmação

DEPLOY_DIR="${DEPLOY_DIR:?DEPLOY_DIR is required}"
COMPOSE_FILE="${COMPOSE_FILE:-docker-compose-observability-control-plane-qa.yml}"
COMPOSE_PROJECT_NAME="${COMPOSE_PROJECT_NAME:-observability-control-plane-qa}"
TMP_DIR="${DEPLOY_DIR}/.tmp"
RELEASES_DIR="${DEPLOY_DIR}/releases"
CURRENT_LINK="${DEPLOY_DIR}/current"
DEPLOY_MARKER="${TMP_DIR}/.deployment-started"
RELEASES_TO_KEEP="${RELEASES_TO_KEEP:-3}"

die() {
  echo "control-plane ${SCRIPT_NAME:-script}: $*" >&2
  exit 1
}

validate_contract() {
  [[ "${DEPLOY_DIR}" =~ ^/[A-Za-z0-9._/-]+$ ]] || die "DEPLOY_DIR must be an absolute, shell-safe path"
  [[ "${DEPLOY_DIR}" != *..* ]] || die "DEPLOY_DIR must not contain parent-directory traversal"
  [[ "${DEPLOY_DIR}" == *observability*control* ]] || die "DEPLOY_DIR is not the control-plane directory"
  [[ "${DEPLOY_DIR}" != *api* && "${DEPLOY_DIR}" != *gateway* ]] || die "DEPLOY_DIR overlaps an app or gateway path"
  [[ "${COMPOSE_FILE}" =~ ^[A-Za-z0-9._-]+\.ya?ml$ ]] || die "COMPOSE_FILE must be a YAML file name"
  [[ "${COMPOSE_PROJECT_NAME}" == observability-control-plane-qa ]] || \
    die "COMPOSE_PROJECT_NAME must be observability-control-plane-qa"
  [[ "${RELEASES_TO_KEEP}" =~ ^[1-9][0-9]*$ ]] || die "RELEASES_TO_KEEP must be a positive integer"
}

validate_release_id() {
  [[ "$1" =~ ^[0-9a-f]{12}-[0-9]+-[0-9]+$ ]] || die "invalid release id: $1"
}

# Compose sempre roda no diretório real do release: os bind mounts mudam de
# caminho a cada deploy e os containers são recriados com a configuração nova.
# Volumes e redes dependem só do nome do projeto e sobrevivem à troca.
compose_in() {
  local release_dir="$1"
  shift
  docker compose \
    --project-directory "${release_dir}" \
    -p "${COMPOSE_PROJECT_NAME}" \
    -f "${release_dir}/${COMPOSE_FILE}" \
    "$@"
}

# Para o Alloy antes de um up --force-recreate. O Compose recria o docker-logs-proxy
# antes dele; com o proxy fora, o discovery perde os alvos, e o loki.source.docker
# apaga a posição de um alvo que some com o componente rodando. O Alloy novo leria
# de novo todo o log retido dos containers. Parado antes, ele grava a posição e o
# novo retoma dela. Sem container do Alloy (primeiro deploy), não faz nada.
stop_alloy() {
  docker ps -q \
    --filter "label=com.docker.compose.project=${COMPOSE_PROJECT_NAME}" \
    --filter "label=com.docker.compose.service=alloy" \
    | xargs -r docker stop >/dev/null
}

current_release_dir() {
  [[ -L "${CURRENT_LINK}" ]] || return 1
  readlink -f -- "${CURRENT_LINK}"
}

# Troca atômica do symlink: rename sobre o link existente.
switch_current_to() {
  local release_dir="$1"
  ln -sfn -- "${release_dir}" "${CURRENT_LINK}.next"
  mv -Tf -- "${CURRENT_LINK}.next" "${CURRENT_LINK}"
}

container_ready() {
  local release_dir="$1"
  local service="$2"
  local container_id status health probe

  container_id="$(compose_in "${release_dir}" ps -a -q "${service}")"
  [[ -n "${container_id}" ]] || return 1
  status="$(docker inspect --format '{{.State.Status}}' "${container_id}" 2>/dev/null || true)"
  # Serviço de execução única (restart "no") só conta se terminou com sucesso.
  if [[ "${status}" == "exited" ]]; then
    [[ "$(docker inspect --format '{{.HostConfig.RestartPolicy.Name}} {{.State.ExitCode}}' "${container_id}" 2>/dev/null || true)" == "no 0" ]]
    return
  fi
  [[ "${status}" == "running" ]] || return 1
  health="$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' "${container_id}" 2>/dev/null || true)"
  if [[ "${health}" == "none" ]]; then
    # R2: serviço contínuo sem healthcheck só conta como pronto se declarar o
    # probe que verifica sua prontidão, e esse probe estiver saudável.
    probe="$(docker inspect --format '{{index .Config.Labels "com.workshop.observability.readiness-probe"}}' "${container_id}" 2>/dev/null || true)"
    [[ -n "${probe}" && "${probe}" != "${service}" ]] || return 1
    container_ready "${release_dir}" "${probe}"
    return
  fi
  [[ "${health}" == "healthy" ]]
}

all_services_ready() {
  local release_dir="$1"
  local services service

  mapfile -t services < <(compose_in "${release_dir}" config --services)
  ((${#services[@]} > 0)) || die "Compose file defines no services"
  for service in "${services[@]}"; do
    if ! container_ready "${release_dir}" "${service}"; then
      NOT_READY_SERVICE="${service}"
      return 1
    fi
  done
}

wait_until_ready() {
  local release_dir="$1"
  local attempts="${HEALTH_ATTEMPTS:-36}"
  local interval="${HEALTH_INTERVAL_SECONDS:-5}"
  local attempt

  [[ "${attempts}" =~ ^[1-9][0-9]*$ ]] || die "HEALTH_ATTEMPTS must be a positive integer"
  [[ "${interval}" =~ ^[1-9][0-9]*$ ]] || die "HEALTH_INTERVAL_SECONDS must be a positive integer"

  for attempt in $(seq 1 "${attempts}"); do
    if all_services_ready "${release_dir}"; then
      echo "Observability control plane is ready"
      return 0
    fi
    echo "[${attempt}/${attempts}] waiting for control-plane service ${NOT_READY_SERVICE}"
    sleep "${interval}"
  done

  echo "Observability control plane did not become ready (${NOT_READY_SERVICE})" >&2
  compose_in "${release_dir}" ps || true
  compose_in "${release_dir}" logs --tail=200 "${NOT_READY_SERVICE}" || true
  return 1
}

# Canário ponta a ponta (Fase 7, item 3): trace, métrica e log com o mesmo id,
# recuperados por consulta no Tempo, no Prometheus e no Loki. O service.name
# próprio mantém o canário fora dos SLOs, alertas e dashboards, que filtram por
# workshop-api e caddy-gateway. Cada execução cria uma série nova de 1 ponto.
CANARY_SERVICE=observability-canary

backend_get() {
  compose_in "$1" exec -T prometheus wget -q -T 5 -O - "$2" 2>/dev/null
}

uri() {
  jq -rn --arg v "$1" '$v | @uri'
}

# Espera o canário <id> nos três backends do release <dir>. O Loki procura nos
# últimos 10 minutos.
canary_query() {
  local release_dir="$1" canary_id="$2"
  local attempts="${CANARY_ATTEMPTS:-24}"
  local interval="${CANARY_INTERVAL_SECONDS:-5}"
  local start_ns attempt trace_ok=0 metric_ok=0 log_ok=0

  [[ "${attempts}" =~ ^[1-9][0-9]*$ ]] || die "CANARY_ATTEMPTS must be a positive integer"
  [[ "${interval}" =~ ^[1-9][0-9]*$ ]] || die "CANARY_INTERVAL_SECONDS must be a positive integer"
  [[ "${canary_id}" =~ ^[0-9a-f]{32}$ ]] || die "canary id must be 32 hex characters"
  start_ns=$(( $(date +%s%N) - 600000000000 ))

  for attempt in $(seq 1 "${attempts}"); do
    # Trace inexistente responde 200 com {"trace":{}}: conferir o conteúdo.
    ((trace_ok)) || { backend_get "${release_dir}" "http://tempo:3200/api/v2/traces/${canary_id}" \
      | jq -e --arg s "${CANARY_SERVICE}" '[.. | .stringValue? // empty] | index($s) != null' >/dev/null && trace_ok=1; } || true
    ((metric_ok)) || { backend_get "${release_dir}" "http://127.0.0.1:9090/api/v1/query?query=$(uri "workshop_canary_delivery{canary_id=\"${canary_id}\"}")" \
      | jq -e '.data.result | length > 0' >/dev/null && metric_ok=1; } || true
    ((log_ok)) || { backend_get "${release_dir}" "http://loki:3100/loki/api/v1/query_range?limit=1&start=${start_ns}&query=$(uri "{service_name=\"${CANARY_SERVICE}\"} |= \"${canary_id}\"")" \
      | jq -e '[.data.result[].values[]] | length > 0' >/dev/null && log_ok=1; } || true
    if ((trace_ok && metric_ok && log_ok)); then
      echo "Canary recovered from Tempo, Prometheus and Loki"
      return 0
    fi
    echo "[${attempt}/${attempts}] canary: trace=${trace_ok} metric=${metric_ok} log=${log_ok}"
    sleep "${interval}"
  done
  die "canary not recovered: trace=${trace_ok} metric=${metric_ok} log=${log_ok}"
}
