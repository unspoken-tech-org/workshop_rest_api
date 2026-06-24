#!/bin/bash
set -euo pipefail
cd "${DEPLOY_DIR}"

# Cleanup secrets/credentials on exit (success or failure)
trap 'rm -f /tmp/.env ~/.docker/config.json' EXIT

# Setup GHCR auth (config.json copied by the deploy-via-ssh-gateway composite)
mkdir -p ~/.docker
if [ -f ~/.docker/config.json.tmp ]; then
  mv ~/.docker/config.json.tmp ~/.docker/config.json
  chmod 600 ~/.docker/config.json
fi

# Setup .env (moved from the composite)
if [ -f /tmp/.env ]; then
  mv /tmp/.env ./.env
  chmod 600 .env
fi

LOCAL_IMAGE_NAME="${LOCAL_IMAGE_NAME:-workshop/caddy-gateway}"
COMPOSE_FILE="${COMPOSE_FILE:-docker-compose-gateway.yml}"
OVERRIDE_FILE="${COMPOSE_FILE%.yml}.override.yml"
PREVIOUS_OVERRIDE="${OVERRIDE_FILE}.previous"

# Pull the new image from GHCR
docker pull "${REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG}"

# Retag GHCR -> local compose name
docker tag "${REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG}" "${LOCAL_IMAGE_NAME}:latest"

# Backup the PREVIOUS image in production
# Backup BEFORE the retag ensures :backup points to the previous image
# (in case the new image fails the health check, rollback restores the healthy version)
if docker image inspect "${LOCAL_IMAGE_NAME}:latest" >/dev/null 2>&1; then
  # Check if the current :latest tag differs from the new one (avoid overwriting :backup if the image already exists)
  if [ "$(docker inspect --format='{{index .RepoDigests 0}}' "${LOCAL_IMAGE_NAME}:latest" 2>/dev/null || echo "")" != "${REGISTRY}/${IMAGE_NAME}@$(docker inspect --format='{{index .RepoDigests 0}}' "${REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG}" 2>/dev/null | cut -d'@' -f2)" ]; then
    docker tag "${LOCAL_IMAGE_NAME}:latest" "${LOCAL_IMAGE_NAME}:backup"
    echo "Backup created: ${LOCAL_IMAGE_NAME}:backup"
  else
    echo "Image already up-to-date, skipping backup"
  fi
fi

# Override file: expose the real tag in docker ps
# Snapshot the current override before overwriting (rollback-gateway.sh restores the snapshot)
if [ -f "${OVERRIDE_FILE}" ]; then
  cp "${OVERRIDE_FILE}" "${PREVIOUS_OVERRIDE}"
fi

cat > "${OVERRIDE_FILE}" <<EOF
services:
  caddy-gateway:
    image: ${REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG}
EOF

# Restart the service
docker compose -f "${COMPOSE_FILE}" -f "${OVERRIDE_FILE}" up -d --force-recreate caddy-gateway
docker compose -f "${COMPOSE_FILE}" -f "${OVERRIDE_FILE}" ps

# Health check via Docker healthcheck
# Uses the Caddy Admin API at :2019/health (does not depend on backends)
ATTEMPTS=30
SLEEP=5
for i in $(seq 1 $ATTEMPTS); do
  STATUS=$(docker inspect --format='{{json .State.Health.Status}}' caddy-gateway 2>/dev/null | tr -d '"')
  echo "[$i/$ATTEMPTS] caddy-gateway health: ${STATUS:-unknown}"
  if [ "$STATUS" = "healthy" ]; then
    echo "Caddy is healthy"
    exit 0
  fi
  sleep $SLEEP
done

echo "Caddy did not become healthy"
docker logs --tail=200 caddy-gateway || true
exit 1
