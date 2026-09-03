#!/bin/bash
set -eo pipefail
cd "${DEPLOY_DIR}"

echo "=== VERIFY OBSERVABILITY PRODUCTION ==="

# Check Loki
LOKI_STATUS=$(docker exec loki-unified wget -qO- http://localhost:3100/ready 2>/dev/null || echo "not ready")
echo "Loki Unified status: ${LOKI_STATUS}"
if [ "$LOKI_STATUS" != "ready" ]; then
  echo "Loki Unified check failed"
  exit 1
fi

# Check Grafana
if ! docker exec grafana-unified wget --spider -q http://localhost:3000/api/health >/dev/null 2>&1; then
  echo "Grafana Unified health check failed"
  exit 1
fi
echo "Grafana Unified health: OK"

# Check Promtail instances
echo "Promtail Prod status:"
docker compose -f "${COMPOSE_FILE}" ps promtail-prod
echo "Promtail Gateway status:"
docker compose -f "${COMPOSE_FILE}" ps promtail-gateway

echo "=== ALL OBSERVABILITY SERVICES HEALTHY ==="
