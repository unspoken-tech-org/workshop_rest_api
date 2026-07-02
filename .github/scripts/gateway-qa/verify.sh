#!/bin/bash
set -e
cd "${DEPLOY_DIR}"

echo ">>> Checking Caddy version..."
docker exec caddy-gateway-qa caddy version

echo ">>> Checking Caddy modules..."
docker exec caddy-gateway-qa caddy list-modules | grep -E "rate_limit|coraza_waf|dns.providers.cloudflare" || echo "Some modules not found (OK if not yet configured)"

echo ">>> Checking Caddy admin API..."
docker exec caddy-gateway-qa curl -sf http://127.0.0.1:2019/config/ && echo "Caddy admin API healthy" || echo "Caddy admin API not responding"

echo ">>> Checking cloudflared QA container..."
docker ps | grep cloudflared-gateway-qa || echo "cloudflared-gateway-qa not running"

echo "=== FINAL CONTAINERS STATUS ==="
docker compose -f "${COMPOSE_FILE}" ps
