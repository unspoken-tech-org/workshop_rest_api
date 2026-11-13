#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_NAME=verify
# shellcheck source=common.sh
source "$(dirname -- "${BASH_SOURCE[0]}")/common.sh"

RELEASE_ID="${RELEASE_ID:?RELEASE_ID is required}"

# Canário ponta a ponta (Fase 7, item 3): um trace, uma métrica e um log com o
# mesmo id entram pela porta OTLP publicada, o caminho do Collector de borda da
# PRD, e só aprovam o deploy se forem recuperados por consulta no Tempo, no
# Prometheus e no Loki. O Collector marca tudo como production; o service.name
# próprio mantém o canário fora dos SLOs, alertas e dashboards, que filtram por
# workshop-api e caddy-gateway. Cada execução cria uma série nova de 1 ponto.
CANARY_SERVICE=observability-canary

otlp_push() {
  local otlp_host="$1" signal="$2" payload="$3"
  # Sem -S: a mensagem de erro do curl traria o IP Tailscale para o log público.
  curl -fs --max-time 10 -o /dev/null -H 'Content-Type: application/json' \
    --data-binary "${payload}" "http://${otlp_host}:4318/v1/${signal}" || \
    die "canary: OTLP push of ${signal} failed"
}

backend_get() {
  compose_in "$1" exec -T prometheus wget -q -T 5 -O - "$2" 2>/dev/null
}

uri() {
  jq -rn --arg v "$1" '$v | @uri'
}

canary_roundtrip() {
  local release_dir="$1"
  local attempts="${CANARY_ATTEMPTS:-24}"
  local interval="${CANARY_INTERVAL_SECONDS:-5}"
  local otlp_host trace_id span_id now_ns start_ns resource scope attempt
  local trace_ok=0 metric_ok=0 log_ok=0

  [[ "${attempts}" =~ ^[1-9][0-9]*$ ]] || die "CANARY_ATTEMPTS must be a positive integer"
  [[ "${interval}" =~ ^[1-9][0-9]*$ ]] || die "CANARY_INTERVAL_SECONDS must be a positive integer"
  otlp_host="$(sed -n 's/^OTLP_BIND_ADDRESS=//p' "${release_dir}/.env" | tail -n 1)"
  [[ "${otlp_host}" =~ ^[0-9.]+$ ]] || die "canary: OTLP_BIND_ADDRESS missing from the release .env"

  trace_id="$(od -An -N16 -tx1 /dev/urandom | tr -d ' \n')"
  span_id="$(od -An -N8 -tx1 /dev/urandom | tr -d ' \n')"
  now_ns="$(date +%s%N)"
  start_ns=$(( now_ns - 600000000000 ))
  resource="{\"attributes\":[{\"key\":\"service.name\",\"value\":{\"stringValue\":\"${CANARY_SERVICE}\"}}]}"
  scope='{"name":"control-plane-verify"}'
  echo "Canary ${trace_id} (release ${RELEASE_ID})"

  otlp_push "${otlp_host}" traces "{\"resourceSpans\":[{\"resource\":${resource},\"scopeSpans\":[{\"scope\":${scope},\"spans\":[{\"traceId\":\"${trace_id}\",\"spanId\":\"${span_id}\",\"name\":\"canary\",\"kind\":1,\"startTimeUnixNano\":\"$(( now_ns - 1000000 ))\",\"endTimeUnixNano\":\"${now_ns}\",\"attributes\":[{\"key\":\"canary.release\",\"value\":{\"stringValue\":\"${RELEASE_ID}\"}}]}]}]}]}"
  otlp_push "${otlp_host}" metrics "{\"resourceMetrics\":[{\"resource\":${resource},\"scopeMetrics\":[{\"scope\":${scope},\"metrics\":[{\"name\":\"workshop.canary.delivery\",\"gauge\":{\"dataPoints\":[{\"asInt\":\"1\",\"timeUnixNano\":\"${now_ns}\",\"attributes\":[{\"key\":\"canary.id\",\"value\":{\"stringValue\":\"${trace_id}\"}}]}]}}]}]}]}"
  otlp_push "${otlp_host}" logs "{\"resourceLogs\":[{\"resource\":${resource},\"scopeLogs\":[{\"scope\":${scope},\"logRecords\":[{\"timeUnixNano\":\"${now_ns}\",\"severityNumber\":9,\"severityText\":\"INFO\",\"body\":{\"stringValue\":\"control-plane canary ${trace_id}\"},\"traceId\":\"${trace_id}\",\"spanId\":\"${span_id}\"}]}]}]}"

  for attempt in $(seq 1 "${attempts}"); do
    # Trace inexistente responde 200 com {"trace":{}}: conferir o conteúdo.
    ((trace_ok)) || { backend_get "${release_dir}" "http://tempo:3200/api/v2/traces/${trace_id}" \
      | jq -e --arg s "${CANARY_SERVICE}" '[.. | .stringValue? // empty] | index($s) != null' >/dev/null && trace_ok=1; } || true
    ((metric_ok)) || { backend_get "${release_dir}" "http://127.0.0.1:9090/api/v1/query?query=$(uri "workshop_canary_delivery{canary_id=\"${trace_id}\"}")" \
      | jq -e '.data.result | length > 0' >/dev/null && metric_ok=1; } || true
    ((log_ok)) || { backend_get "${release_dir}" "http://loki:3100/loki/api/v1/query_range?limit=1&start=${start_ns}&query=$(uri "{service_name=\"${CANARY_SERVICE}\"} |= \"${trace_id}\"")" \
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

validate_contract
validate_release_id "${RELEASE_ID}"

release_dir="$(current_release_dir)" || die "no active release"
[[ "${release_dir}" == "${RELEASES_DIR}/${RELEASE_ID}" ]] || \
  die "active release is not ${RELEASE_ID}"

compose_in "${release_dir}" config --quiet
compose_in "${release_dir}" ps
if ! all_services_ready "${release_dir}"; then
  echo "control-plane service is not ready: ${NOT_READY_SERVICE}" >&2
  compose_in "${release_dir}" logs --tail=200 "${NOT_READY_SERVICE}" || true
  exit 1
fi

canary_roundtrip "${release_dir}"

echo "Observability control plane verification passed"
