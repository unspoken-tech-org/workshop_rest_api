#!/bin/bash
set -e
cd "${DEPLOY_DIR}"

# Cleanup secrets/credentials on exit (success or failure)
trap 'rm -f /tmp/.env /tmp/private-pkcs8.pem /tmp/public.pem ~/.docker/config.json' EXIT

# Setup GHCR auth
mkdir -p ~/.docker
mv ~/.docker/config.json.tmp ~/.docker/config.json 2>/dev/null || true
chmod 600 ~/.docker/config.json

# Setup .env
mv /tmp/.env ./ 2>/dev/null || true
chmod 600 .env

# Setup JWT keys
mkdir -p config/keys
mv /tmp/private-pkcs8.pem config/keys/ 2>/dev/null || true
chmod 600 config/keys/private-pkcs8.pem
mv /tmp/public.pem config/keys/ 2>/dev/null || true
chmod 644 config/keys/public.pem

# Setup pgbackrest config
if [ -f "/tmp/pgbackrest.conf" ]; then
  DB_USER=$(grep '^DB_USERNAME=' .env | cut -d'=' -f2)
  sed -i "s/DB_USER_PLACEHOLDER/${DB_USER}/g" "/tmp/pgbackrest.conf"
  mv "/tmp/pgbackrest.conf" /srv/pgbackrest/conf/pgbackrest.conf
  chmod 644 /srv/pgbackrest/conf/pgbackrest.conf
fi

# Setup Dockerfile.pgbackrest
if [ -f "/tmp/Dockerfile.pgbackrest" ]; then
  mv "/tmp/Dockerfile.pgbackrest" "${DEPLOY_DIR}/Dockerfile.pgbackrest"
fi

# Build PostgreSQL image locally if it doesn't exist
PG_IMAGE="workshop/postgres16-pgbackrest:latest"
if ! docker image inspect "${PG_IMAGE}" >/dev/null 2>&1; then
  echo "PostgreSQL image ${PG_IMAGE} not found, building from Dockerfile.pgbackrest..."
  docker build -f "${DEPLOY_DIR}/Dockerfile.pgbackrest" -t "${PG_IMAGE}" "${DEPLOY_DIR}"
  echo "PostgreSQL image built: ${PG_IMAGE}"
else
  echo "PostgreSQL image ${PG_IMAGE} already exists, skipping build"
fi

# Backup the CURRENT image before pulling the new one
# (rollback.sh restores :backup, ensuring a safe fallback)
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
docker compose -f "${COMPOSE_FILE}" up -d --remove-orphans workshop_spring_app
docker compose -f "${COMPOSE_FILE}" ps

# Health check
ATTEMPTS=30
SLEEP=5
for i in $(seq 1 $ATTEMPTS); do
  STATUS=$(docker inspect --format='{{json .State.Health.Status}}' workshop-api 2>/dev/null | tr -d '"')
  echo "[$i/$ATTEMPTS] workshop-api health: ${STATUS:-unknown}"
  if [ "$STATUS" = "healthy" ]; then
    echo "API is healthy"
    exit 0
  fi
  sleep $SLEEP
done
echo "API did not become healthy"
docker logs --tail=200 workshop-api || true
exit 1
