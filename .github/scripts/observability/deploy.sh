#!/bin/bash
set -eo pipefail
cd "${DEPLOY_DIR}"

# Cleanup secrets/credentials on exit (success or failure)
trap 'rm -rf "${DEPLOY_DIR}/.tmp"' EXIT

mv "${DEPLOY_DIR}/.tmp/.env" ./ 2>/dev/null || true
chmod 600 .env

docker compose -f "${COMPOSE_FILE}" up -d --remove-orphans
docker compose -f "${COMPOSE_FILE}" ps

ATTEMPTS=30
SLEEP=5
for i in $(seq 1 $ATTEMPTS); do
  LOKI_STATUS=$(docker exec loki-unified wget -qO- http://localhost:3100/ready 2>/dev/null || echo "not ready")
  echo "[$i/$ATTEMPTS] loki-unified: ${LOKI_STATUS}"
  if [ "$LOKI_STATUS" = "ready" ]; then
    echo "Loki Unified is ready"
    break
  fi
  sleep $SLEEP
done

if [ "$LOKI_STATUS" != "ready" ]; then
  echo "Loki Unified did not become ready"
  docker logs --tail=200 loki-unified || true
  exit 1
fi

ATTEMPTS=40
for i in $(seq 1 $ATTEMPTS); do
  if docker exec grafana-unified wget --spider -q http://localhost:3000/api/health >/dev/null 2>&1; then
    echo "Grafana Unified is healthy"
    exit 0
  else
    echo "[$i/$ATTEMPTS] grafana-unified is starting..."
  fi
  sleep $SLEEP
done

echo "Grafana Unified did not become healthy"
docker logs --tail=200 grafana-unified || true
exit 1
