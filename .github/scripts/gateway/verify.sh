#!/bin/bash
set -euo pipefail

DEPLOY_DIR="${DEPLOY_DIR:-/home/workshop/deploy/gateway_stack}"
cd "${DEPLOY_DIR}"

# docker inspect evita o pipe com grep -q, que sob pipefail falha por SIGPIPE
container_running() {
  [ "$(docker inspect -f '{{.State.Running}}' "$1" 2>/dev/null)" = "true" ]
}

# R2: container without a healthcheck, or not healthy, fails the verification.
require_healthy() {
  local status
  status=$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' "$1" 2>/dev/null || echo missing)
  if [ "${status}" != "healthy" ]; then
    echo "$1 health is ${status}, expected healthy"
    exit 1
  fi
  echo "$1 is healthy"
}

echo ">>> Checking gateway containers health..."
require_healthy caddy-gateway
require_healthy cloudflared-gateway-prod

echo ">>> Caddy version:"
docker exec caddy-gateway caddy version

echo ">>> Caddy modules (expected: dns.providers.cloudflare, http.handlers.rate_limit, http.handlers.waf.coraza_waf):"
docker exec caddy-gateway caddy list-modules | grep -E "rate_limit|coraza_waf|dns.providers.cloudflare" || {
  echo "Expected modules not found"
  exit 1
}

echo ">>> Caddy health (Admin API :2019/health):"
if docker exec caddy-gateway curl -sf -o /dev/null http://127.0.0.1:2019/config/; then
  echo "Caddy admin API healthy"
else
  echo "Caddy admin API unhealthy"
  exit 1
fi

echo ">>> Gateway tunnel (port 80):"
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -H "Host: api.eletroluk.com" \
  http://localhost:80/actuator/health || echo "000")
echo "HTTP status: $STATUS"
if [ "$STATUS" != "200" ]; then
  echo "Tunnel health check failed"
  exit 1
fi

echo ">>> Caddy metrics (internal site :9180, scraped by the edge collector):"
# Lido para variável e passado por here-string: grep -q num pipe falharia por
# SIGPIPE sob pipefail.
METRICS=$(docker exec caddy-gateway curl -sf http://127.0.0.1:9180/metrics || true)
if grep -q '^caddy_config_last_reload_successful ' <<< "$METRICS"; then
  echo "Caddy metrics endpoint OK"
else
  echo "Caddy metrics endpoint not responding on :9180/metrics"
  exit 1
fi

echo ">>> QA tunnel on this host (warning if not running):"
if container_running cloudflared-gateway-qa; then
  echo "Tunnel QA OK"
else
  echo "Tunnel QA Missing (WARNING)"
fi

echo "=== FINAL CONTAINERS STATUS ==="
docker compose -f "${DEPLOY_DIR}/docker-compose-gateway.yml" ps
