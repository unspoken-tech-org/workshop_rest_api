#!/bin/bash
set -e
cd "${DEPLOY_DIR}"

# Cleanup secrets/credentials on exit (success or failure)
trap 'rm -rf "${DEPLOY_DIR}/.tmp"' EXIT

# The swap marker holds this run id, so rollback.sh reverts only this run's swap.
if [[ ! "${DEPLOY_RUN_ID:-}" =~ ^[0-9]+$ ]]; then
  echo "DEPLOY_RUN_ID must be the numeric workflow run id" >&2
  exit 1
fi

# A snapshot or swap marker left by an older run must not drive this run's rollback.
# Cleared before the digest check, so a failed check cannot leave a stale marker.
rm -f "${COMPOSE_FILE}.previous" .api-swapped

# Deploy the exact image the build pushed; a registry tag can be moved.
if [[ ! "${IMAGE_DIGEST}" =~ ^sha256:[0-9a-f]{64}$ ]]; then
  echo "IMAGE_DIGEST must be sha256:<64 hex>" >&2
  exit 1
fi
IMAGE_REF="${REGISTRY}/${IMAGE_NAME}@${IMAGE_DIGEST}"

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

# Pull by digest; the local compose tag is set only after the Flyway gate
docker --config "${DEPLOY_DIR}/.tmp/docker" pull "${IMAGE_REF}"
EXPECTED_IMAGE_ID=$(docker image inspect --format '{{.Id}}' "${IMAGE_REF}")

# Backup the image the running API container uses, not the tag: :latest may have
# moved since. rollback.sh restores :backup only when this run swaps the API.
# The backup tag and the compose file that runs it are one pair: set_backup moves both,
# so rollback never pairs the backup image with a compose file that needs the agent.
BACKUP_SNAPSHOT="${COMPOSE_FILE}.backup"
set_backup() {
  # Stage the snapshot before the tag moves: if the copy fails, the old pair stays.
  if [ -f "${COMPOSE_FILE}" ]; then
    cp -p "${COMPOSE_FILE}" "${BACKUP_SNAPSHOT}.tmp"
  else
    rm -f "${BACKUP_SNAPSHOT}.tmp"
  fi
  docker tag "$1" "${LOCAL_IMAGE_NAME}:backup"
  if [ -f "${BACKUP_SNAPSHOT}.tmp" ]; then
    mv "${BACKUP_SNAPSHOT}.tmp" "${BACKUP_SNAPSHOT}"
  else
    # First deploy: no compose file to pair, so a stale snapshot must not survive
    rm -f "${BACKUP_SNAPSHOT}"
  fi
}
PREV_ID=$(docker inspect --format '{{.Image}}' workshop-api 2>/dev/null || true)
# Redeploying the running image (e.g. a re-run) keeps the existing backup and its snapshot
if [ -n "${PREV_ID}" ] && [ "${PREV_ID}" = "${EXPECTED_IMAGE_ID}" ]; then
  echo "workshop-api already runs ${IMAGE_REF}, keeping the existing backup"
elif [ -n "${PREV_ID}" ]; then
  set_backup "${PREV_ID}"
  echo "Backup created from workshop-api: ${LOCAL_IMAGE_NAME}:backup (paired with ${BACKUP_SNAPSHOT})"
elif docker image inspect "${LOCAL_IMAGE_NAME}:latest" >/dev/null 2>&1; then
  set_backup "${LOCAL_IMAGE_NAME}:latest"
  echo "Backup created: ${LOCAL_IMAGE_NAME}:backup (paired with ${BACKUP_SNAPSHOT})"
else
  echo "No previous image found, skipping backup"
fi

# Flyway gate: the migrations in the NEW image must match the database history
# before the API is swapped. A rollback restores the image only, never the schema.
if ! JAR_LISTING=$(docker run --rm --network none --entrypoint unzip "${IMAGE_REF}" -l /app/app.jar); then
  echo "Could not list migrations in ${IMAGE_REF}" >&2
  exit 1
fi
# Migration versions have no leading zeros here (V1..Vn), so sort -V orders them.
IMAGE_VERSIONS=$(printf '%s\n' "${JAR_LISTING}" \
  | grep -o 'BOOT-INF/classes/db/migration/postgresql/V[0-9][0-9_.]*__' \
  | sed -e 's#.*/V##' -e 's#__$##' -e 's/_/./g' | sort -uV || true)
if [ -z "${IMAGE_VERSIONS}" ]; then
  echo "No Flyway migrations found in ${IMAGE_REF}" >&2
  exit 1
fi

db_query() {
  docker exec workshop-db sh -c 'psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Atc "$1"' sh "$1"
}
if docker container inspect workshop-db >/dev/null 2>&1; then
  DB_RUNNING=$(docker inspect --format '{{.State.Running}}' workshop-db)
else
  DB_RUNNING=missing
fi
case "${DB_RUNNING}" in
  true) HAS_HISTORY=$(db_query "SELECT to_regclass('public.flyway_schema_history') IS NOT NULL") ;;
  missing) echo "workshop-db not found: first deploy, no Flyway history"; HAS_HISTORY=f ;;
  *) echo "workshop-db is not running; cannot verify Flyway history" >&2; exit 1 ;;
esac

APPLIED_VERSIONS=""
if [ "${HAS_HISTORY}" = "t" ]; then
  HISTORY=$(db_query "SELECT success, version FROM public.flyway_schema_history")
  FAILED=$(printf '%s\n' "${HISTORY}" | grep '^f|' || true)
  if [ -n "${FAILED}" ]; then
    echo "Failed migrations in flyway_schema_history (success = false):" >&2
    echo "${FAILED}" >&2
    exit 1
  fi
  APPLIED_VERSIONS=$(printf '%s\n' "${HISTORY}" | grep '^t|.' | cut -d'|' -f2 || true)
else
  echo "No flyway_schema_history table yet"
fi

declare -A IN_IMAGE=() APPLIED=()
for v in ${IMAGE_VERSIONS}; do IN_IMAGE[$v]=1; done
for v in ${APPLIED_VERSIONS}; do APPLIED[$v]=1; done

# Applied in the database but absent from the image: downgrade or removed migration.
AHEAD=""
for v in ${APPLIED_VERSIONS}; do
  if [ -z "${IN_IMAGE[$v]:-}" ]; then AHEAD="${AHEAD} ${v}"; fi
done
if [ -n "${AHEAD}" ]; then
  echo "Applied migrations missing from ${IMAGE_REF}:${AHEAD}" >&2
  exit 1
fi

PENDING=""
for v in ${IMAGE_VERSIONS}; do
  if [ -z "${APPLIED[$v]:-}" ]; then PENDING="${PENDING} ${v}"; fi
done
if [ -n "${PENDING}" ]; then
  echo "Pending migrations (in image, not applied):${PENDING}"
  echo "WARNING: rolling back the image does NOT revert schema changes"
else
  echo "No pending migrations"
fi

# Retag after the Flyway gate, so a failed gate never moves :latest
docker tag "${IMAGE_REF}" "${LOCAL_IMAGE_NAME}:latest"

# Mark that this run swapped the API: rollback.sh reverts only when the marker holds this run id
printf '%s\n' "${DEPLOY_RUN_ID}" > .api-swapped

# Swap in the staged compose file (rollback.sh restores the snapshot)
if [ -f "${COMPOSE_FILE}" ]; then
  cp -p "${COMPOSE_FILE}" "${COMPOSE_FILE}.previous"
fi
mv "${DEPLOY_DIR}/.tmp/${COMPOSE_FILE}" "${COMPOSE_FILE}"

# Restart the service
docker compose -f "${COMPOSE_FILE}" up -d --remove-orphans workshop_spring_app

# A recreated workshop-db loses the runtime-only workshop-db-metrics network that
# the edge deploy attaches (postgres-exporter); reattach it when the network exists.
# Runs before the image check below, which can exit, so a failed deploy still reattaches.
if docker network inspect workshop-db-metrics >/dev/null 2>&1 && \
  [ -z "$(docker inspect --format '{{if index .NetworkSettings.Networks "workshop-db-metrics"}}yes{{end}}' workshop-db)" ]; then
  docker network connect workshop-db-metrics workshop-db || echo "WARNING: could not reattach workshop-db to workshop-db-metrics"
fi

RUNNING_IMAGE_ID=$(docker inspect --format '{{.Image}}' workshop-api)
if [ "${RUNNING_IMAGE_ID}" != "${EXPECTED_IMAGE_ID}" ]; then
  echo "workshop-api runs ${RUNNING_IMAGE_ID}, expected ${EXPECTED_IMAGE_ID} (${IMAGE_REF})" >&2
  exit 1
fi
echo "workshop-api runs ${IMAGE_REF}"
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
