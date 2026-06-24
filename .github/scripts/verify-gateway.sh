#!/bin/bash
set -euo pipefail

DEPLOY_DIR="${DEPLOY_DIR:-/home/workshop/deploy/gateway_stack}"
cd "${DEPLOY_DIR}"

echo ">>> Checking caddy-gateway container..."
if ! docker ps | grep -q caddy-gateway; then
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
if docker exec caddy-gateway wget --spider -q -O /dev/null http://127.0.0.1:2019/health; then
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

echo ">>> Tunnels (warning if not running):"
if docker ps | grep -q cloudflared-gateway-prod; then
  echo "Tunnel Prod OK"
else
  echo "Tunnel Prod Missing (WARNING)"
fi

if docker ps | grep -q cloudflared-gateway-qa; then
  echo "Tunnel QA OK"
else
  echo "Tunnel QA Missing (WARNING)"
fi

echo "=== FINAL CONTAINERS STATUS ==="
docker compose -f "${DEPLOY_DIR}/docker-compose-gateway.yml" ps
