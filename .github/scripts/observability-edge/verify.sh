#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_NAME=verify
# shellcheck source=common.sh
source "$(dirname -- "${BASH_SOURCE[0]}")/common.sh"

RELEASE_ID="${RELEASE_ID:?RELEASE_ID is required}"
# Id do canário gerado no runner: o job verify-delivery o procura nos backends da QA.
CANARY_ID="${CANARY_ID:?CANARY_ID is required}"
# Janela para a primeira amostra do scrape: o receiver prometheus/api raspa a
# cada 15 s, então 12 tentativas de 5 s cobrem pelo menos uma coleta.
METRICS_WAIT_ATTEMPTS="${METRICS_WAIT_ATTEMPTS:-12}"
METRICS_WAIT_SECONDS="${METRICS_WAIT_SECONDS:-5}"

validate_contract
validate_release_id "${RELEASE_ID}"
[[ "${CANARY_ID}" =~ ^[0-9a-f]{32}$ ]] || die "CANARY_ID must be 32 hex characters"

release_dir="$(current_release_dir)" || die "no active release"
[[ "${release_dir}" == "${RELEASES_DIR}/${RELEASE_ID}" ]] || \
  die "active release is not ${RELEASE_ID}"

compose_in "${release_dir}" config --quiet
compose_in "${release_dir}" ps
if ! all_services_ready "${release_dir}"; then
  echo "edge service is not ready: ${NOT_READY_SERVICE}" >&2
  show_service_logs "${release_dir}" "${NOT_READY_SERVICE}"
  exit 1
fi

# Checagens explícitas do que a prontidão precisa provar, impressas no log.
probe_id="$(compose_in "${release_dir}" ps -a -q otel-collector-probe)"
[[ "$(docker inspect --format '{{.State.Health.Status}}' "${probe_id}")" == healthy ]] || \
  die "otel-collector-probe is not healthy"
init_id="$(compose_in "${release_dir}" ps -a -q otel-collector-queue-init)"
[[ "$(docker inspect --format '{{.State.Status}} {{.State.ExitCode}}' "${init_id}")" == "exited 0" ]] || \
  die "otel-collector-queue-init did not exit with code 0"
echo "otel-collector-probe is healthy; otel-collector-queue-init exited with code 0"

# Nenhuma porta publicada no host: qualquer binding do projeto reprova.
all_container_ids="$(compose_in "${release_dir}" ps -a -q)"
while read -r container_id; do
  [[ -n "${container_id}" ]] || continue
  published="$(docker inspect --format '{{range $port, $bindings := .NetworkSettings.Ports}}{{if $bindings}}{{$port}} {{end}}{{end}}' "${container_id}")"
  [[ -z "${published}" ]] || die "container ${container_id} publishes ports on the host: ${published}"
done <<< "${all_container_ids}"
echo "The edge project publishes no host ports"

# /metrics do próprio Collector, lido de dentro do probe: é o único container com
# wget e está na mesma rede interna (edge-probe) do Collector.
collector_metrics() {
  compose_in "${release_dir}" exec -T otel-collector-probe \
    wget -qO- http://otel-collector:8888/metrics
}

# Endpoint de saúde do scrape (porta 8889). O exporter escuta em 0.0.0.0, então
# alcança todas as redes do otel-collector, inclusive workshop-metrics; por isso só
# expõe up e target_info (filtro em config.yaml). Mostra a série up de cada alvo.
# up == 1 é o critério. O receiver prometheus/api emite up 0 quando o scrape
# falha (credencial recusada, timeout, API fora). Pontos aceitos não distinguem
# essas falhas, e a telemetria do Collector (8888) não traz up por alvo.
scrape_health() {
  compose_in "${release_dir}" exec -T otel-collector-probe \
    wget -qO- http://otel-collector:8889/metrics
}

scrape_up() {
  scrape_health 2>/dev/null | awk -v job="$1" '/^up\{/ && index($0, "job=\"" job "\"") {print $NF}' || true
}

# pg_up do postgres-exporter: 1 quando o papel workshop_metrics consulta o banco pela
# rede workshop-db-metrics. up do job só prova que o exporter respondeu.
pg_up() {
  scrape_health 2>/dev/null | awk '/^pg_up\{/ && index($0, "job=\"postgres-exporter\"") {print $NF}' || true
}

# Pontos aceitos pelo receiver docker_stats, lidos do Collector de containers pelo
# probe da mesma rede (edge-otlp). Zero ou ausente: a allowlist ou a API do Docker
# não está respondendo, e o receiver não registra nenhum container.
docker_stats_points() {
  compose_in "${release_dir}" exec -T containers-collector-probe \
    wget -qO- http://containers-collector:8888/metrics 2>/dev/null \
    | awk '/^otelcol_receiver_accepted_metric_points\{receiver="docker_stats"/ {sum += $NF; found = 1} END {if (found) printf "%d\n", sum}' || true
}

# Pontos enviados pelo exporter do containers-collector ao Collector de borda: prova
# que o que o receiver aceita sai do container e chega ao Collector. Na 0.161 o nome
# não tem sufixo _total.
containers_sent_points() {
  compose_in "${release_dir}" exec -T containers-collector-probe \
    wget -qO- http://containers-collector:8888/metrics 2>/dev/null \
    | awk '/^otelcol_exporter_sent_metric_points\{/ && index($0, "exporter=\"otlp_grpc/edge\"") {sum += $NF; found = 1} END {if (found) printf "%d\n", sum}' || true
}

# Espera a primeira amostra de cada fonte: up == 1 da API, do node-exporter e do
# postgres-exporter, pg_up == 1, pontos
# aceitos de docker_stats e pontos enviados pelo containers-collector à borda. A
# janela cobre pelo menos uma coleta de 15 s.
api_up=""
node_up=""
postgres_up=""
postgres_db_up=""
containers_points=""
containers_sent=""
for attempt in $(seq 1 "${METRICS_WAIT_ATTEMPTS}"); do
  api_up="$(scrape_up workshop-api)"
  node_up="$(scrape_up node-exporter)"
  postgres_up="$(scrape_up postgres-exporter)"
  postgres_db_up="$(pg_up)"
  containers_points="$(docker_stats_points)"
  containers_sent="$(containers_sent_points)"
  if [[ "${api_up}" == 1 && "${node_up}" == 1 && "${postgres_up}" == 1 && "${postgres_db_up}" == 1 \
    && "${containers_points}" =~ ^[1-9][0-9]*$ && "${containers_sent}" =~ ^[1-9][0-9]*$ ]]; then
    break
  fi
  echo "[${attempt}/${METRICS_WAIT_ATTEMPTS}] waiting: api up=${api_up:-none}, node-exporter up=${node_up:-none}, postgres-exporter up=${postgres_up:-none}, pg_up=${postgres_db_up:-none}, docker_stats points=${containers_points:-none}, sent to edge=${containers_sent:-none}"
  sleep "${METRICS_WAIT_SECONDS}"
done

if [[ "${api_up}" != 1 ]]; then
  show_service_logs "${release_dir}" otel-collector
  die "workshop-api scrape is not up (up=${api_up:-none}): the authenticated scrape is not working"
fi
echo "prometheus/api: up{job=\"workshop-api\"} is 1: the authenticated scrape works"

if [[ "${node_up}" != 1 ]]; then
  show_service_logs "${release_dir}" node-exporter
  die "node-exporter scrape is not up (up=${node_up:-none}): host metrics are not arriving"
fi
echo "prometheus/node: up{job=\"node-exporter\"} is 1: host metrics are being scraped"

if [[ "${postgres_up}" != 1 || "${postgres_db_up}" != 1 ]]; then
  show_service_logs "${release_dir}" postgres-exporter
  die "PostgreSQL metrics are not working (postgres-exporter up=${postgres_up:-none}, pg_up=${postgres_db_up:-none}): check the ${DB_METRICS_NETWORK} network and the ${PG_MONITOR_ROLE} role"
fi
echo "prometheus/postgres: up{job=\"postgres-exporter\"} and pg_up are 1: the monitoring role reads PostgreSQL"

if [[ ! "${containers_points}" =~ ^[1-9][0-9]*$ ]]; then
  show_service_logs "${release_dir}" containers-collector
  die "docker_stats accepted no points (${containers_points:-none}): the container allowlist or the Docker API is not working"
fi
echo "docker_stats: ${containers_points} metric points accepted from the Docker API allowlist"

if [[ ! "${containers_sent}" =~ ^[1-9][0-9]*$ ]]; then
  show_service_logs "${release_dir}" containers-collector
  die "containers-collector sent no points to the edge collector (${containers_sent:-none}): the OTLP path from the containers to the edge is not working"
fi
echo "containers-collector: ${containers_sent} metric points sent to the edge collector (otlp_grpc/edge)"

# Informativo: o scrape do gateway (Caddy :9180) só funciona depois do release do
# gateway com o site de métricas; até lá up fica 0 e o deploy da borda não reprova.
# A ausência é coberta pelos alertas do gateway no plano de controle.
gateway_up="$(scrape_up caddy-gateway)"
if [[ "${gateway_up}" == 1 ]]; then
  echo "prometheus/gateway: up{job=\"caddy-gateway\"} is 1: gateway metrics are being scraped"
else
  echo "WARNING: prometheus/gateway: up{job=\"caddy-gateway\"} is ${gateway_up:-none}: check the :9180 metrics site of the gateway release"
fi

# Canário (Fase 7, item 3): trace, métrica e log com o id do runner entram no
# receiver OTLP do Collector de borda pela sonda da edge-otlp (busybox wget, POST
# HTTP), o mesmo caminho do Alloy e do containers-collector. A entrega à QA, que
# prova a fila e o exporter, é conferida pelo job verify-delivery do workflow.
# service.name próprio: fica fora dos SLOs, alertas e dashboards.
edge_push() {
  compose_in "${release_dir}" exec -T containers-collector-probe \
    wget -q -T 10 -O /dev/null --header 'Content-Type: application/json' \
    --post-data "$2" "http://otel-collector:4318/v1/$1" || die "canary: OTLP push of $1 failed"
}
span_id="$(od -An -N8 -tx1 /dev/urandom | tr -d ' \n')"
now_ns="$(date +%s%N)"
resource='{"attributes":[{"key":"service.name","value":{"stringValue":"observability-canary"}}]}'
scope='{"name":"edge-verify"}'
edge_push traces "{\"resourceSpans\":[{\"resource\":${resource},\"scopeSpans\":[{\"scope\":${scope},\"spans\":[{\"traceId\":\"${CANARY_ID}\",\"spanId\":\"${span_id}\",\"name\":\"canary\",\"kind\":1,\"startTimeUnixNano\":\"$(( now_ns - 1000000 ))\",\"endTimeUnixNano\":\"${now_ns}\",\"attributes\":[{\"key\":\"canary.release\",\"value\":{\"stringValue\":\"${RELEASE_ID}\"}}]}]}]}]}"
edge_push metrics "{\"resourceMetrics\":[{\"resource\":${resource},\"scopeMetrics\":[{\"scope\":${scope},\"metrics\":[{\"name\":\"workshop.canary.delivery\",\"gauge\":{\"dataPoints\":[{\"asInt\":\"1\",\"timeUnixNano\":\"${now_ns}\",\"attributes\":[{\"key\":\"canary.id\",\"value\":{\"stringValue\":\"${CANARY_ID}\"}}]}]}}]}]}]}"
edge_push logs "{\"resourceLogs\":[{\"resource\":${resource},\"scopeLogs\":[{\"scope\":${scope},\"logRecords\":[{\"timeUnixNano\":\"${now_ns}\",\"severityNumber\":9,\"severityText\":\"INFO\",\"body\":{\"stringValue\":\"edge canary ${CANARY_ID}\"},\"traceId\":\"${CANARY_ID}\",\"spanId\":\"${span_id}\"}]}]}]}"
echo "Canary ${CANARY_ID} accepted by the edge collector (release ${RELEASE_ID})"

# Informativo: fila de exportação por tipo de dado.
metrics="$(collector_metrics 2>/dev/null)" || metrics=""
echo "Exporter queue size per data type (information only):"
printf '%s\n' "${metrics}" \
  | awk '/^otelcol_exporter_queue_size\{/ {match($0, /data_type="[^"]*"/); print "  " substr($0, RSTART, RLENGTH) " " $NF}' \
  || true

echo "Observability edge verification passed"
