#!/bin/bash
set -eo pipefail
cd "${DEPLOY_DIR}"

echo "=== OBSERVABILITY QA SERVICES ==="
docker compose -f "${COMPOSE_FILE}" ps

echo ""
echo ">>> Checking Loki QA..."
LOKI_STATUS=$(docker exec loki-qa wget -qO- http://localhost:3100/ready 2>/dev/null || echo "not ready")
echo "Loki QA: ${LOKI_STATUS}"

echo ""
echo ">>> Checking Promtail QA..."
if docker ps --filter "name=promtail-qa" --filter "status=running" | grep -q "promtail-qa"; then
  echo "promtail-qa is running"
  docker logs --tail=50 promtail-qa 2>&1 | grep -E "Listening|target|error" || true
else
  echo "promtail-qa is NOT running"
fi

echo ""
echo ">>> Checking Grafana QA..."
if docker exec grafana-qa wget --spider -q http://localhost:3000/api/health >/dev/null 2>&1; then
  echo "Grafana QA: healthy"
else
  echo "Grafana QA: NOT healthy"
  exit 1
fi

echo ""
echo ">>> Checking labels in Loki..."
LABELS=$(docker exec loki-qa wget -qO- http://localhost:3100/loki/api/v1/labels 2>/dev/null || echo "{}")
echo "$LABELS" | jq '.data' 2>/dev/null || echo "$LABELS"

echo ""
echo ">>> Testing log ingestion..."
sleep 5
for env in qa; do
  QUERY=$(printf 'query={environment="%s"}' "$env")
  RESULT=$(docker exec loki-qa wget -qO- "http://localhost:3100/loki/api/v1/query?$QUERY" 2>/dev/null || echo "{}")
  if echo "$RESULT" | grep -q "stream"; then
    echo "Logs from $env environment found"
  else
    echo "No logs from $env yet (may be normal on first deploy)"
  fi
done
