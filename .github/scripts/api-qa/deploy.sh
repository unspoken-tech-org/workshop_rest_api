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
mkdir -p config/qa_keys
mv /tmp/private-pkcs8.pem config/qa_keys/ 2>/dev/null || true
chmod 600 config/qa_keys/private-pkcs8.pem
mv /tmp/public.pem config/qa_keys/ 2>/dev/null || true
chmod 644 config/qa_keys/public.pem

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

# Create pgbackrest config if it doesn't exist
PGBACKREST_CONF="/srv/pgbackrest/conf/pgbackrest.conf"
if [ ! -f "${PGBACKREST_CONF}" ]; then
  echo "Creating pgbackrest configuration..."
  mkdir -p /srv/pgbackrest/conf
  cat > "${PGBACKREST_CONF}" << 'EOF'
[global]
repo1-path=/var/lib/pgbackrest
repo1-retention-full=4
repo1-retention-diff=14
start-fast=y
log-level-console=info
log-level-file=info
log-path=/var/log/pgbackrest

[workshop]
pg1-path=/var/lib/postgresql/data
pg1-port=5432
pg1-user=work_shop_prd
EOF
  echo "pgbackrest config created: ${PGBACKREST_CONF}"
else
  echo "pgbackrest config already exists, skipping"
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
