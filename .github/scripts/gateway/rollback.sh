#!/bin/bash
set -euo pipefail
cd "${DEPLOY_DIR:-/home/workshop/deploy/gateway_stack}"

LOCAL_IMAGE_NAME="${LOCAL_IMAGE_NAME:-workshop/caddy-gateway}"
COMPOSE_FILE="${COMPOSE_FILE:-docker-compose-gateway.yml}"
OVERRIDE_FILE="${COMPOSE_FILE%.yml}.override.yml"
PREVIOUS_OVERRIDE="${OVERRIDE_FILE}.previous"

if ! docker image inspect "${LOCAL_IMAGE_NAME}:backup" >/dev/null 2>&1; then
  echo "No backup image found"
  exit 1
fi

docker tag "${LOCAL_IMAGE_NAME}:backup" "${LOCAL_IMAGE_NAME}:latest"

# Restore previous compose and Caddyfile (snapshotted by deploy workflow sync step)
if [ -f "${COMPOSE_FILE}.previous" ]; then
  echo "Restoring previous compose file"
  mv "${COMPOSE_FILE}.previous" "${COMPOSE_FILE}"
fi
if [ -f "infra/caddy/Caddyfile-gateway.previous" ]; then
  echo "Restoring previous Caddyfile"
  mv "infra/caddy/Caddyfile-gateway.previous" "infra/caddy/Caddyfile-gateway"
fi

if [ -f "${PREVIOUS_OVERRIDE}" ]; then
  mv "${PREVIOUS_OVERRIDE}" "${OVERRIDE_FILE}"
  docker compose -f "${COMPOSE_FILE}" -f "${OVERRIDE_FILE}" up -d --force-recreate caddy-gateway
  echo "Rollback executed (override restored)"
else
  rm -f "${OVERRIDE_FILE}"
  docker compose -f "${COMPOSE_FILE}" up -d --force-recreate caddy-gateway
  echo "Rollback executed (alias :latest)"
fi

# Health check pos-rollback (parity with deploy.sh)
ATTEMPTS=30
SLEEP=5
for i in $(seq 1 $ATTEMPTS); do
  STATUS=$(docker inspect --format='{{json .State.Health.Status}}' caddy-gateway 2>/dev/null | tr -d '"')
  echo "[$i/$ATTEMPTS] caddy-gateway health: ${STATUS:-unknown}"
  if [ "$STATUS" = "healthy" ]; then
    echo "Caddy is healthy after rollback"
    exit 0
  fi
  sleep $SLEEP
done

echo "Caddy did not become healthy after rollback"
docker logs --tail=200 caddy-gateway || true
exit 1
