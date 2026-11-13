#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_NAME=verify
# shellcheck source=common.sh
source "$(dirname -- "${BASH_SOURCE[0]}")/common.sh"

RELEASE_ID="${RELEASE_ID:?RELEASE_ID is required}"

# Canário (Fase 7, item 3): entra pela porta OTLP publicada, o caminho do
# Collector de borda da PRD. O Collector marca tudo como production.
otlp_push() {
  local otlp_host="$1" signal="$2" payload="$3"
  # Sem -S: a mensagem de erro do curl traria o IP Tailscale para o log público.
  curl -fs --max-time 10 -o /dev/null -H 'Content-Type: application/json' \
    --data-binary "${payload}" "http://${otlp_host}:4318/v1/${signal}" || \
    die "canary: OTLP push of ${signal} failed"
}

canary_roundtrip() {
  local release_dir="$1"
  local otlp_host trace_id span_id now_ns resource scope

  otlp_host="$(sed -n 's/^OTLP_BIND_ADDRESS=//p' "${release_dir}/.env" | tail -n 1)"
  [[ "${otlp_host}" =~ ^[0-9.]+$ ]] || die "canary: OTLP_BIND_ADDRESS missing from the release .env"

  trace_id="$(od -An -N16 -tx1 /dev/urandom | tr -d ' \n')"
  span_id="$(od -An -N8 -tx1 /dev/urandom | tr -d ' \n')"
  now_ns="$(date +%s%N)"
  resource="{\"attributes\":[{\"key\":\"service.name\",\"value\":{\"stringValue\":\"${CANARY_SERVICE}\"}}]}"
  scope='{"name":"control-plane-verify"}'
  echo "Canary ${trace_id} (release ${RELEASE_ID})"

  otlp_push "${otlp_host}" traces "{\"resourceSpans\":[{\"resource\":${resource},\"scopeSpans\":[{\"scope\":${scope},\"spans\":[{\"traceId\":\"${trace_id}\",\"spanId\":\"${span_id}\",\"name\":\"canary\",\"kind\":1,\"startTimeUnixNano\":\"$(( now_ns - 1000000 ))\",\"endTimeUnixNano\":\"${now_ns}\",\"attributes\":[{\"key\":\"canary.release\",\"value\":{\"stringValue\":\"${RELEASE_ID}\"}}]}]}]}]}"
  otlp_push "${otlp_host}" metrics "{\"resourceMetrics\":[{\"resource\":${resource},\"scopeMetrics\":[{\"scope\":${scope},\"metrics\":[{\"name\":\"workshop.canary.delivery\",\"gauge\":{\"dataPoints\":[{\"asInt\":\"1\",\"timeUnixNano\":\"${now_ns}\",\"attributes\":[{\"key\":\"canary.id\",\"value\":{\"stringValue\":\"${trace_id}\"}}]}]}}]}]}]}"
  otlp_push "${otlp_host}" logs "{\"resourceLogs\":[{\"resource\":${resource},\"scopeLogs\":[{\"scope\":${scope},\"logRecords\":[{\"timeUnixNano\":\"${now_ns}\",\"severityNumber\":9,\"severityText\":\"INFO\",\"body\":{\"stringValue\":\"control-plane canary ${trace_id}\"},\"traceId\":\"${trace_id}\",\"spanId\":\"${span_id}\"}]}]}]}"

  canary_query "${release_dir}" "${trace_id}"
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
