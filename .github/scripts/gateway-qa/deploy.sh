#!/bin/bash
set -e
cd "${DEPLOY_DIR}"

# Cleanup secrets/credentials on exit (success or failure)
trap 'rm -f /tmp/.env ~/.docker/config.json' EXIT

# Setup GHCR auth
mkdir -p ~/.docker
mv ~/.docker/config.json.tmp ~/.docker/config.json 2>/dev/null || true
chmod 600 ~/.docker/config.json

# Setup .env
mv /tmp/.env ./ 2>/dev/null || true
chmod 600 .env

# Backup the CURRENT image before pulling the new one
if docker image inspect "${LOCAL_IMAGE_NAME}:latest" >/dev/null 2>&1; then
  docker tag "${LOCAL_IMAGE_NAME}:latest" "${LOCAL_IMAGE_NAME}:backup"
  echo "Backup created: ${LOCAL_IMAGE_NAME}:backup"
else
  echo "No previous image found, skipping backup"
fi

# Pull the new image from GHCR and retag to the local compose name
docker pull "${REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG}"
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
