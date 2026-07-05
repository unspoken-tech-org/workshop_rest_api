#!/bin/bash
set -eo pipefail
cd "${DEPLOY_DIR}"

# Cleanup secrets/credentials on exit (success or failure)
trap 'rm -rf "${DEPLOY_DIR}/.tmp"' EXIT

# Setup GHCR auth (config.json copied to secure .tmp/docker/ by composite action)
chmod 600 "${DEPLOY_DIR}/.tmp/docker/config.json" 2>/dev/null || true

# Setup .env
mv "${DEPLOY_DIR}/.tmp/.env" ./ 2>/dev/null || true
chmod 600 .env

# Backup the CURRENT image before pulling the new one
if docker image inspect "${LOCAL_IMAGE_NAME}:latest" >/dev/null 2>&1; then
  docker tag "${LOCAL_IMAGE_NAME}:latest" "${LOCAL_IMAGE_NAME}:backup"
  echo "Backup created: ${LOCAL_IMAGE_NAME}:backup"
else
  echo "No previous image found, skipping backup"
fi

# Pull the new image from GHCR and retag to the local compose name
docker --config "${DEPLOY_DIR}/.tmp/docker" pull "${REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG}"
docker tag "${REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG}" "${LOCAL_IMAGE_NAME}:latest"

# Restart the service
docker compose -f "${COMPOSE_FILE}" up -d --remove-orphans caddy-gateway-qa
docker compose -f "${COMPOSE_FILE}" ps

# Health check
ATTEMPTS=30
SLEEP=5
for i in $(seq 1 $ATTEMPTS); do
  STATUS=$(docker inspect --format='{{json .State.Health.Status}}' caddy-gateway-qa 2>/dev/null | tr -d '"')
  echo "[$i/$ATTEMPTS] caddy-gateway-qa health: ${STATUS:-unknown}"
  if [ "$STATUS" = "healthy" ]; then
    echo "Gateway QA is healthy"
    exit 0
  fi
  sleep $SLEEP
done
echo "Gateway QA did not become healthy"
docker logs --tail=200 caddy-gateway-qa || true
exit 1
