#!/bin/bash
set -e
cd "${DEPLOY_DIR}"

# Cleanup secrets/credentials on exit (success or failure)
trap 'rm -rf "${DEPLOY_DIR}/.tmp"' EXIT

# Setup .env
mv "${DEPLOY_DIR}/.tmp/.env" ./ 2>/dev/null || mv .tmp/.env ./ 2>/dev/null || true
chmod 600 .env

# Setup JWT keys
mkdir -p config/keys
chmod 755 config config/keys
mv "${DEPLOY_DIR}/.tmp/private-pkcs8.pem" config/keys/ 2>/dev/null || mv .tmp/private-pkcs8.pem config/keys/ 2>/dev/null || true
chmod 644 config/keys/private-pkcs8.pem
mv "${DEPLOY_DIR}/.tmp/public.pem" config/keys/ 2>/dev/null || mv .tmp/public.pem config/keys/ 2>/dev/null || true
chmod 644 config/keys/public.pem

# Setup scrape identity hash (file secret; the API runs as a non-root user, so
# the file is world-readable inside the private config/observability directory)
mkdir -p config/observability
chmod 700 config/observability
mv "${DEPLOY_DIR}/.tmp/scrape_password_hash" config/observability/scrape_password_hash 2>/dev/null || mv .tmp/scrape_password_hash config/observability/scrape_password_hash
chmod 444 config/observability/scrape_password_hash

# Setup DB password and pgBackRest repository (file secrets, never container env;
# the containers run as non-root users, so the files are world-readable inside
# the private config/secrets directory)
mkdir -p config/secrets
chmod 700 config/secrets
mv "${DEPLOY_DIR}/.tmp/db_password" config/secrets/db_password
chmod 444 config/secrets/db_password
mv "${DEPLOY_DIR}/.tmp/pgbackrest-repo1.conf" config/secrets/pgbackrest-repo1.conf
chmod 444 config/secrets/pgbackrest-repo1.conf

# Setup pgbackrest config
if [ -f "${DEPLOY_DIR}/.tmp/pgbackrest.conf" ]; then
  DB_USER=$(grep '^DB_USERNAME=' .env | cut -d'=' -f2)
  sed -i "s/DB_USER_PLACEHOLDER/${DB_USER}/g" "${DEPLOY_DIR}/.tmp/pgbackrest.conf"
  mv "${DEPLOY_DIR}/.tmp/pgbackrest.conf" /srv/pgbackrest/conf/pgbackrest.conf
  chmod 644 /srv/pgbackrest/conf/pgbackrest.conf
elif [ -f ".tmp/pgbackrest.conf" ]; then
  DB_USER=$(grep '^DB_USERNAME=' .env | cut -d'=' -f2)
  sed -i "s/DB_USER_PLACEHOLDER/${DB_USER}/g" ".tmp/pgbackrest.conf"
  mv ".tmp/pgbackrest.conf" /srv/pgbackrest/conf/pgbackrest.conf
  chmod 644 /srv/pgbackrest/conf/pgbackrest.conf
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
docker --config "${DEPLOY_DIR}/.tmp/docker" pull "${REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG}"
docker --config "${DEPLOY_DIR}/.tmp/docker" tag "${REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG}" "${LOCAL_IMAGE_NAME}:latest"

# Restart the service
docker compose -f "${COMPOSE_FILE}" up -d --remove-orphans workshop_spring_app
docker compose -f "${COMPOSE_FILE}" ps

# A recreated workshop-db loses the runtime-only workshop-db-metrics network that
# the edge deploy attaches (postgres-exporter); reattach it when the network exists.
if docker network inspect workshop-db-metrics >/dev/null 2>&1 && \
  [ -z "$(docker inspect --format '{{if index .NetworkSettings.Networks "workshop-db-metrics"}}yes{{end}}' workshop-db)" ]; then
  docker network connect workshop-db-metrics workshop-db || echo "WARNING: could not reattach workshop-db to workshop-db-metrics"
fi

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
