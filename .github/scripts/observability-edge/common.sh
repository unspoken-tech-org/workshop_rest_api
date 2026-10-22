#!/usr/bin/env bash
# shellcheck disable=SC2034  # variáveis usadas pelos scripts que carregam este arquivo
# Funções comuns aos scripts remotos do edge de PRD. Cada script é enviado junto
# com este arquivo para DEPLOY_DIR/.tmp e o carrega por source.
#
# Layout remoto:
#   DEPLOY_DIR/releases/<release-id>/    Compose, configs, .env e .secrets de um deploy
#   DEPLOY_DIR/current                   symlink para o release ativo (troca atômica)
#   DEPLOY_DIR/.tmp/                     pacote e scripts da execução corrente
#   DEPLOY_DIR/.tmp/.deployment-started  release anterior, até a confirmação

DEPLOY_DIR="${DEPLOY_DIR:?DEPLOY_DIR is required}"
COMPOSE_FILE="${COMPOSE_FILE:-docker-compose-observability-edge-prd.yml}"
COMPOSE_PROJECT_NAME="${COMPOSE_PROJECT_NAME:-workshop-edge}"
# Rede externa criada pelo Compose da API de PRD (interna). O scrape passa por ela.
METRICS_NETWORK=workshop-metrics
# Banco de PRD (container_name em apps/api/compose/production.yml) e a rede interna
# que só ele e o postgres-exporter compartilham. Esta borda cria a rede e a conecta
# ao banco; o Compose da API não a declara.
DB_CONTAINER=workshop-db
DB_METRICS_NETWORK=workshop-db-metrics
# Papel de leitura do postgres-exporter (pg_monitor), mantido por pg_monitor_role.
PG_MONITOR_ROLE=workshop_metrics
TMP_DIR="${DEPLOY_DIR}/.tmp"
RELEASES_DIR="${DEPLOY_DIR}/releases"
CURRENT_LINK="${DEPLOY_DIR}/current"
DEPLOY_MARKER="${TMP_DIR}/.deployment-started"
RELEASES_TO_KEEP="${RELEASES_TO_KEEP:-3}"

die() {
  echo "edge ${SCRIPT_NAME:-script}: $*" >&2
  exit 1
}

validate_contract() {
  [[ "${DEPLOY_DIR}" =~ ^/[A-Za-z0-9._/-]+$ ]] || die "DEPLOY_DIR must be an absolute, shell-safe path"
  [[ "${DEPLOY_DIR}" != *..* ]] || die "DEPLOY_DIR must not contain parent-directory traversal"
  [[ "${DEPLOY_DIR}" == *observability*edge* ]] || die "DEPLOY_DIR is not the edge directory"
  [[ "${DEPLOY_DIR}" != *api* && "${DEPLOY_DIR}" != *gateway* ]] || die "DEPLOY_DIR overlaps an app or gateway path"
  [[ "${COMPOSE_FILE}" =~ ^[A-Za-z0-9._-]+\.ya?ml$ ]] || die "COMPOSE_FILE must be a YAML file name"
  [[ "${COMPOSE_PROJECT_NAME}" == workshop-edge ]] || die "COMPOSE_PROJECT_NAME must be workshop-edge"
  [[ "${RELEASES_TO_KEEP}" =~ ^[1-9][0-9]*$ ]] || die "RELEASES_TO_KEEP must be a positive integer"
}

validate_release_id() {
  [[ "$1" =~ ^[0-9a-f]{12}-[0-9]+-[0-9]+$ ]] || die "invalid release id: $1"
}

# Compose sempre roda no diretório real do release: os bind mounts mudam de
# caminho a cada deploy e os containers são recriados com a configuração nova.
# Volumes e redes dependem só do nome do projeto e sobrevivem à troca.
# QA_OTLP_ENDPOINT e os caminhos das senhas (scrape e papel do PostgreSQL) não vêm
# do .env: saem de .secrets/ do próprio release e entram só nesta invocação do
# Compose. Um release anterior ao postgres-exporter não tem a senha do papel, mas o
# Compose dele também não a referencia.
compose_in() {
  local release_dir="$1"
  local qa_otlp_endpoint
  shift
  [[ -s "${release_dir}/.secrets/qa-otlp-endpoint" ]] || die "release has no qa-otlp-endpoint secret"
  qa_otlp_endpoint="$(<"${release_dir}/.secrets/qa-otlp-endpoint")"
  QA_OTLP_ENDPOINT="${qa_otlp_endpoint}" \
    EDGE_API_SCRAPE_PASSWORD_FILE="${release_dir}/.secrets/api-scrape-password" \
    EDGE_PG_MONITOR_PASSWORD_FILE="${release_dir}/.secrets/pg-monitor-password" \
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

# Rede interna do banco: criada se faltar e conectada ao workshop-db sem reiniciá-lo.
# Idempotente. Se o banco for recriado pelo Compose da API, a conexão some e volta
# no próximo deploy desta borda.
ensure_db_metrics_network() {
  local attached

  [[ "$(docker inspect --format '{{.State.Status}}' "${DB_CONTAINER}" 2>/dev/null || true)" == running ]] || \
    die "container ${DB_CONTAINER} is not running"
  if ! docker network inspect "${DB_METRICS_NETWORK}" >/dev/null 2>&1; then
    docker network create --driver bridge --internal "${DB_METRICS_NETWORK}" >/dev/null
    echo "Created internal network ${DB_METRICS_NETWORK}"
  fi
  [[ "$(docker network inspect --format '{{.Internal}}' "${DB_METRICS_NETWORK}")" == true ]] || \
    die "network ${DB_METRICS_NETWORK} exists but is not internal"
  attached="$(docker inspect --format "{{if index .NetworkSettings.Networks \"${DB_METRICS_NETWORK}\"}}yes{{end}}" "${DB_CONTAINER}")"
  if [[ "${attached}" != yes ]]; then
    docker network connect "${DB_METRICS_NETWORK}" "${DB_CONTAINER}"
    echo "Connected ${DB_CONTAINER} to ${DB_METRICS_NETWORK}"
  fi
}

# Cria ou atualiza o papel do postgres-exporter com a senha do release: LOGIN,
# membro de pg_monitor (com herança), sem outros privilégios e com até 3 conexões.
# Idempotente. O SQL vai por stdin ao psql do próprio container, que conecta pelo
# socket local como POSTGRES_USER: a senha não entra em argv. A saída do psql é
# descartada, porque um erro pode ecoar o comando. A sessão desliga o registro de
# comandos (inclusive do que falhar) e o pg_stat_statements de comandos utilitários,
# para a senha não chegar ao log nem às estatísticas do PostgreSQL.
pg_monitor_role() {
  local secret_file="$1"
  local password

  password="$(<"${secret_file}")"
  # Charset sem aspas nem barras: o valor entra literal no SQL e no DSN.
  [[ "${password}" =~ ^[A-Za-z0-9._~-]{24,128}$ ]] || \
    die "the pg-monitor password must have 24 to 128 characters from [A-Za-z0-9._~-]"
  # shellcheck disable=SC2016  # as variáveis expandem dentro do container
  docker exec -i "${DB_CONTAINER}" \
    sh -c 'exec psql -X -q -v ON_ERROR_STOP=1 -U "${POSTGRES_USER}" -d "${POSTGRES_DB}"' <<SQL >/dev/null 2>&1 || \
    die "could not create or update the ${PG_MONITOR_ROLE} role (psql output hidden: it may echo the statement)"
SET log_min_error_statement = panic;
SET log_statement = 'none';
SET log_min_duration_statement = -1;
SET log_min_duration_sample = -1;
SET log_transaction_sample_rate = 0;
SET pg_stat_statements.track_utility = off;
SET client_min_messages = warning;
SELECT 'CREATE ROLE ${PG_MONITOR_ROLE}' WHERE NOT EXISTS (SELECT FROM pg_roles WHERE rolname = '${PG_MONITOR_ROLE}')\gexec
ALTER ROLE ${PG_MONITOR_ROLE} WITH LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION NOBYPASSRLS INHERIT CONNECTION LIMIT 3 PASSWORD '${password}';
GRANT pg_monitor TO ${PG_MONITOR_ROLE} WITH INHERIT TRUE;
SQL
  echo "PostgreSQL role ${PG_MONITOR_ROLE} is up to date (pg_monitor)"
}

# Rollback: reaplica a senha do release restaurado quando ela difere da do release
# que falhou. Não é fatal: restaurar a borda não pode depender da saúde do banco; se
# falhar, WorkshopPostgresDown avisa.
restore_pg_monitor_role() {
  local release_dir="$1"
  local failed_dir="$2"
  local secret_file="${release_dir}/.secrets/pg-monitor-password"

  # Release de antes do postgres-exporter: não há senha a restaurar.
  [[ -s "${secret_file}" ]] || return 0
  if cmp -s -- "${secret_file}" "${failed_dir}/.secrets/pg-monitor-password"; then
    return 0
  fi
  (pg_monitor_role "${secret_file}") || \
    echo "edge ${SCRIPT_NAME:-script}: warning: could not restore the ${PG_MONITOR_ROLE} password" >&2
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

# Logs de um serviço sem IPs do tailnet: o repositório é público e o log do job
# também é público.
show_service_logs() {
  local release_dir="$1"
  local service="$2"
  compose_in "${release_dir}" logs --tail=200 "${service}" 2>&1 \
    | sed -E 's/100\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}/100.x.x.x/g' || true
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
      echo "Observability edge collector is ready"
      return 0
    fi
    echo "[${attempt}/${attempts}] waiting for edge service ${NOT_READY_SERVICE}"
    sleep "${interval}"
  done

  echo "Observability edge collector did not become ready (${NOT_READY_SERVICE})" >&2
  compose_in "${release_dir}" ps || true
  show_service_logs "${release_dir}" "${NOT_READY_SERVICE}"
  return 1
}
