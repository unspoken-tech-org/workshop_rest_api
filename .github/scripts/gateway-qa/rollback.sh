#!/bin/bash
set -Eeuo pipefail
cd "${DEPLOY_DIR}"

LOCAL_IMAGE_NAME="${LOCAL_IMAGE_NAME:-workshop/caddy-gateway-qa}"
COMPOSE_FILE="${COMPOSE_FILE:-docker-compose-gateway-qa.yml}"

if ! docker image inspect "${LOCAL_IMAGE_NAME}:backup" >/dev/null 2>&1; then
  echo "No backup image found"
  exit 1
fi

docker tag "${LOCAL_IMAGE_NAME}:backup" "${LOCAL_IMAGE_NAME}:latest"

# Restore previous compose and Caddyfile (snapshotted by the deploy-via-ssh action)
if [ -f "${COMPOSE_FILE}.previous" ]; then
  echo "Restoring previous compose file"
  mv "${COMPOSE_FILE}.previous" "${COMPOSE_FILE}"
fi
if [ -f "Caddyfile-gateway-qa.previous" ]; then
  echo "Restoring previous Caddyfile"
  rm -rf "Caddyfile-gateway-qa"
  mv "Caddyfile-gateway-qa.previous" "Caddyfile-gateway-qa"
fi

docker compose -f "${COMPOSE_FILE}" up -d --force-recreate caddy-gateway-qa
echo "Rollback executed (alias :latest)"

# Health check post-rollback
ATTEMPTS=30
SLEEP=5
for i in $(seq 1 $ATTEMPTS); do
  STATUS=$(docker inspect --format='{{json .State.Health.Status}}' caddy-gateway-qa 2>/dev/null | tr -d '"')
  echo "[$i/$ATTEMPTS] caddy-gateway-qa health: ${STATUS:-unknown}"
  if [ "$STATUS" = "healthy" ]; then
    echo "Gateway QA is healthy after rollback"
    exit 0
  fi
  sleep $SLEEP
done

echo "Gateway QA did not become healthy after rollback"
docker logs --tail=200 caddy-gateway-qa || true
exit 1
