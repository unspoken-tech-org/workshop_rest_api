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
