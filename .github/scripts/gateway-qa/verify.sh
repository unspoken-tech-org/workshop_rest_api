#!/bin/bash
set -eo pipefail
cd "${DEPLOY_DIR}"

# R2: every gateway container must exist, run and report healthy. A container
# without a healthcheck, or not healthy, fails the verification.
require_healthy() {
  local status
  status=$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' "$1" 2>/dev/null || echo missing)
  if [ "${status}" != "healthy" ]; then
    echo "ERROR: $1 health is ${status}, expected healthy" >&2
    exit 1
  fi
  echo "$1 is healthy"
}

echo ">>> Checking container health..."
require_healthy caddy-gateway-qa
require_healthy cloudflared-gateway-qa

echo ">>> Checking Caddy version..."
docker exec caddy-gateway-qa caddy version

echo ">>> Checking Caddy modules..."
docker exec caddy-gateway-qa caddy list-modules | grep -E "rate_limit|coraza_waf|dns.providers.cloudflare" || echo "Some modules not found (OK if not yet configured)"

echo ">>> Checking Caddy admin API..."
if docker exec caddy-gateway-qa curl -sf -o /dev/null http://127.0.0.1:2019/config/; then
  echo "Caddy admin API healthy"
else
  echo "ERROR: Caddy admin API not responding" >&2
  exit 1
fi

echo ">>> Checking Caddy metrics (internal site :9180)..."
# Lido para variável e passado por here-string: grep -q num pipe falharia por
# SIGPIPE sob pipefail.
METRICS=$(docker exec caddy-gateway-qa curl -sf http://127.0.0.1:9180/metrics || true)
if grep -q '^caddy_config_last_reload_successful ' <<< "$METRICS"; then
  echo "Caddy metrics endpoint OK"
else
  echo "ERROR: Caddy metrics endpoint not responding on :9180/metrics" >&2
  exit 1
fi

echo "=== FINAL CONTAINERS STATUS ==="
docker compose -f "${COMPOSE_FILE}" ps
