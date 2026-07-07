#!/bin/bash
set -euo pipefail
cd "${DEPLOY_DIR}"

COMPOSE_FILE="${COMPOSE_FILE:-docker-compose-observability-qa.yml}"

echo ">>> Stopping observability QA stack..."
docker compose -f "${COMPOSE_FILE}" down --remove-orphans || true

echo ">>> Restarting observability QA stack..."
docker compose -f "${COMPOSE_FILE}" up -d --remove-orphans

ATTEMPTS=30
SLEEP=5
for i in $(seq 1 $ATTEMPTS); do
  LOKI_STATUS=$(curl -s http://localhost:3100/ready 2>/dev/null || echo "not ready")
  echo "[$i/$ATTEMPTS] loki-qa: ${LOKI_STATUS}"
  if [ "$LOKI_STATUS" = "ready" ]; then
    echo "Loki QA is ready after rollback"
    exit 0
  fi
  sleep $SLEEP
done

echo "Loki QA did not become ready after rollback"
docker logs --tail=200 loki-qa || true
exit 1
