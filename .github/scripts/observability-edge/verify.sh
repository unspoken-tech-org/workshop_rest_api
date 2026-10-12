#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_NAME=verify
# shellcheck source=common.sh
source "$(dirname -- "${BASH_SOURCE[0]}")/common.sh"

RELEASE_ID="${RELEASE_ID:?RELEASE_ID is required}"
# Janela para a primeira amostra do scrape: o receiver prometheus/api raspa a
# cada 15 s, então 12 tentativas de 5 s cobrem pelo menos uma coleta.
METRICS_WAIT_ATTEMPTS="${METRICS_WAIT_ATTEMPTS:-12}"
METRICS_WAIT_SECONDS="${METRICS_WAIT_SECONDS:-5}"

validate_contract
validate_release_id "${RELEASE_ID}"

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

api_scrape_up() {
  scrape_health 2>/dev/null | awk '/^up\{/ && /job="workshop-api"/ {print $NF}' || true
}

up_value=""
for attempt in $(seq 1 "${METRICS_WAIT_ATTEMPTS}"); do
  up_value="$(api_scrape_up)"
  if [[ "${up_value}" == 1 ]]; then
    break
  fi
  echo "[${attempt}/${METRICS_WAIT_ATTEMPTS}] workshop-api scrape is not up yet (up=${up_value:-none})"
  sleep "${METRICS_WAIT_SECONDS}"
done

if [[ "${up_value}" != 1 ]]; then
  show_service_logs "${release_dir}" otel-collector
  die "workshop-api scrape is not up (up=${up_value:-none}): the authenticated scrape is not working"
fi
echo "prometheus/api: up{job=\"workshop-api\"} is 1: the authenticated scrape works"

# Informativo: fila de exportação por tipo de dado. A entrega à QA ainda não é
# critério de aprovação (a ACL PRD -> QA é adiada), então a fila pode crescer.
metrics="$(collector_metrics 2>/dev/null)" || metrics=""
echo "Exporter queue size per data type (information only):"
printf '%s\n' "${metrics}" \
  | awk '/^otelcol_exporter_queue_size\{/ {match($0, /data_type="[^"]*"/); print "  " substr($0, RSTART, RLENGTH) " " $NF}' \
  || true

echo "Observability edge verification passed"
