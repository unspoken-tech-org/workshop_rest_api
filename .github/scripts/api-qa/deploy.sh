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

# Setup JWT keys
mkdir -p config/qa_keys
mv "${DEPLOY_DIR}/.tmp/private-pkcs8.pem" config/qa_keys/ 2>/dev/null || true
chmod 600 config/qa_keys/private-pkcs8.pem
mv "${DEPLOY_DIR}/.tmp/public.pem" config/qa_keys/ 2>/dev/null || true
chmod 644 config/qa_keys/public.pem

# Setup pgbackrest config
if [ -f "${DEPLOY_DIR}/.tmp/pgbackrest.conf" ]; then
  DB_USER=$(grep '^DB_USERNAME=' .env | cut -d'=' -f2)
  sed -i "s/DB_USER_PLACEHOLDER/${DB_USER}/g" "${DEPLOY_DIR}/.tmp/pgbackrest.conf"
  mv "${DEPLOY_DIR}/.tmp/pgbackrest.conf" /srv/pgbackrest/conf/pgbackrest.conf
  chmod 644 /srv/pgbackrest/conf/pgbackrest.conf
fi

# Build PostgreSQL image locally if it doesn't exist
# (image not on GHCR — built from Dockerfile.pgbackrest on first deploy)
PG_IMAGE="workshop/postgres16-pgbackrest:latest"
if ! docker image inspect "${PG_IMAGE}" >/dev/null 2>&1; then
  echo "PostgreSQL image ${PG_IMAGE} not found, building from Dockerfile.pgbackrest..."
  docker build -f "${DEPLOY_DIR}/Dockerfile.pgbackrest" -t "${PG_IMAGE}" "${DEPLOY_DIR}"
  echo "PostgreSQL image built: ${PG_IMAGE}"
else
  echo "PostgreSQL image ${PG_IMAGE} already exists, skipping build"
fi

# Backup the CURRENT image before pulling the new one
# (rollback.sh restores :IMAGE_TAG-backup, ensuring a safe fallback)
if docker image inspect "${LOCAL_IMAGE_NAME}:${IMAGE_TAG}" >/dev/null 2>&1; then
  docker tag "${LOCAL_IMAGE_NAME}:${IMAGE_TAG}" "${LOCAL_IMAGE_NAME}:${IMAGE_TAG}-backup"
  echo "Backup created: ${LOCAL_IMAGE_NAME}:${IMAGE_TAG}-backup"
else
  echo "No previous image found for tag ${IMAGE_TAG}, skipping backup"
fi

# Pull the new image from GHCR and retag to the local compose name
docker --config "${DEPLOY_DIR}/.tmp/docker" pull "${REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG}"
docker tag "${REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG}" "${LOCAL_IMAGE_NAME}:${IMAGE_TAG}"

# Restart the service
docker compose -f "${COMPOSE_FILE}" up -d --remove-orphans workshop_spring_app_qa
docker compose -f "${COMPOSE_FILE}" ps


# Health check
ATTEMPTS=30
SLEEP=5
for i in $(seq 1 $ATTEMPTS); do
  STATUS=$(docker inspect --format='{{json .State.Health.Status}}' workshop-api-qa 2>/dev/null | tr -d '"')
  echo "[$i/$ATTEMPTS] workshop-api-qa health: ${STATUS:-unknown}"
  if [ "$STATUS" = "healthy" ]; then
    echo "QA API is healthy"
    exit 0
  fi
  sleep $SLEEP
done
echo "QA API did not become healthy"
docker logs --tail=200 workshop-api-qa || true
exit 1
