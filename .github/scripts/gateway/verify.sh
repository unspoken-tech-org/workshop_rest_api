#!/bin/bash
set -euo pipefail

DEPLOY_DIR="${DEPLOY_DIR:-/home/workshop/deploy/gateway_stack}"
cd "${DEPLOY_DIR}"

# docker inspect evita o pipe com grep -q, que sob pipefail falha por SIGPIPE
container_running() {
  [ "$(docker inspect -f '{{.State.Running}}' "$1" 2>/dev/null)" = "true" ]
}

echo ">>> Checking caddy-gateway container..."
if ! container_running caddy-gateway; then
  echo "caddy-gateway not running"
  exit 1
fi
echo "caddy-gateway running"

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
# Lido para variável: grep -q no pipe falharia por SIGPIPE sob pipefail.
METRICS=$(docker exec caddy-gateway curl -sf http://127.0.0.1:9180/metrics || true)
if printf '%s\n' "$METRICS" | grep -q '^caddy_config_last_reload_successful '; then
  echo "Caddy metrics endpoint OK"
else
  echo "Caddy metrics endpoint not responding on :9180/metrics"
  exit 1
fi

echo ">>> Tunnels (warning if not running):"
if container_running cloudflared-gateway-prod; then
  echo "Tunnel Prod OK"
else
  echo "Tunnel Prod Missing (WARNING)"
fi

if container_running cloudflared-gateway-qa; then
  echo "Tunnel QA OK"
else
  echo "Tunnel QA Missing (WARNING)"
fi

echo "=== FINAL CONTAINERS STATUS ==="
docker compose -f "${DEPLOY_DIR}/docker-compose-gateway.yml" ps
