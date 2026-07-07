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
  LOKI_STATUS=$(docker exec loki-qa wget -qO- http://localhost:3100/ready 2>/dev/null || echo "not ready")
  echo "[$i/$ATTEMPTS] loki-qa: ${LOKI_STATUS}"
  if [ "$LOKI_STATUS" = "ready" ]; then
    echo "Loki QA is ready"
    break
  fi
  sleep $SLEEP
done

if [ "$LOKI_STATUS" != "ready" ]; then
  echo "Loki QA did not become ready"
  docker logs --tail=200 loki-qa || true
  exit 1
fi

ATTEMPTS=40
for i in $(seq 1 $ATTEMPTS); do
  # Query Grafana health endpoint from within the container since port 3000 is not exposed to the host for security
  if docker exec grafana-qa wget --spider -q http://localhost:3000/api/health >/dev/null 2>&1; then
    echo "Grafana QA is healthy"
    exit 0
  else
    echo "[$i/$ATTEMPTS] grafana-qa is starting..."
  fi
  sleep $SLEEP
done

echo "Grafana QA did not become healthy"
docker logs --tail=200 grafana-qa || true
exit 1
