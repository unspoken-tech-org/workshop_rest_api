#!/bin/bash
set -eo pipefail
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

# Setup GHCR auth (config.json copied to secure .tmp/docker/ by composite action)
chmod 600 "${DEPLOY_DIR}/.tmp/docker/config.json" 2>/dev/null || true

# Setup .env
mv "${DEPLOY_DIR}/.tmp/.env" ./ 2>/dev/null || true
chmod 600 .env

# Setup JWT keys
mkdir -p config/qa_keys
mv "${DEPLOY_DIR}/.tmp/private-pkcs8.pem" config/qa_keys/ 2>/dev/null || true
chmod 644 config/qa_keys/private-pkcs8.pem
mv "${DEPLOY_DIR}/.tmp/public.pem" config/qa_keys/ 2>/dev/null || true
chmod 644 config/qa_keys/public.pem

# Setup scrape identity hash (file secret; the API runs as a non-root user, so
# the file is world-readable inside the private config/observability directory)
mkdir -p config/observability
chmod 700 config/observability
mv "${DEPLOY_DIR}/.tmp/scrape_password_hash" config/observability/scrape_password_hash
chmod 444 config/observability/scrape_password_hash

# Setup DB password (file secret, never container env; the containers run as
# non-root users, so the file is world-readable inside the private
# config/secrets directory)
mkdir -p config/secrets
chmod 700 config/secrets
mv "${DEPLOY_DIR}/.tmp/db_password" config/secrets/db_password
chmod 444 config/secrets/db_password

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

# Pull by digest; the local compose tag is set only after the Flyway gate
docker --config "${DEPLOY_DIR}/.tmp/docker" pull "${IMAGE_REF}"
EXPECTED_IMAGE_ID=$(docker image inspect --format '{{.Id}}' "${IMAGE_REF}")

# Backup the image the running API container uses, not the tag: the tag may have
# moved since. rollback.sh restores :IMAGE_TAG-backup only when this run swaps the API.
PREV_ID=$(docker inspect --format '{{.Image}}' workshop-api-qa 2>/dev/null || true)
# Redeploying the running image (e.g. a re-run) keeps the existing backup
if [ -n "${PREV_ID}" ] && [ "${PREV_ID}" = "${EXPECTED_IMAGE_ID}" ]; then
  echo "workshop-api-qa already runs ${IMAGE_REF}, keeping the existing backup"
elif [ -n "${PREV_ID}" ]; then
  docker tag "${PREV_ID}" "${LOCAL_IMAGE_NAME}:${IMAGE_TAG}-backup"
  echo "Backup created from workshop-api-qa: ${LOCAL_IMAGE_NAME}:${IMAGE_TAG}-backup"
elif docker image inspect "${LOCAL_IMAGE_NAME}:${IMAGE_TAG}" >/dev/null 2>&1; then
  docker tag "${LOCAL_IMAGE_NAME}:${IMAGE_TAG}" "${LOCAL_IMAGE_NAME}:${IMAGE_TAG}-backup"
  echo "Backup created: ${LOCAL_IMAGE_NAME}:${IMAGE_TAG}-backup"
else
  echo "No previous image found for tag ${IMAGE_TAG}, skipping backup"
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
  docker exec workshop-db-qa sh -c 'psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Atc "$1"' sh "$1"
}
if docker container inspect workshop-db-qa >/dev/null 2>&1; then
  DB_RUNNING=$(docker inspect --format '{{.State.Running}}' workshop-db-qa)
else
  DB_RUNNING=missing
fi
case "${DB_RUNNING}" in
  true) HAS_HISTORY=$(db_query "SELECT to_regclass('public.flyway_schema_history') IS NOT NULL") ;;
  missing) echo "workshop-db-qa not found: first deploy, no Flyway history"; HAS_HISTORY=f ;;
  *) echo "workshop-db-qa is not running; cannot verify Flyway history" >&2; exit 1 ;;
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

# Retag after the Flyway gate, so a failed gate never moves the :IMAGE_TAG alias
docker tag "${IMAGE_REF}" "${LOCAL_IMAGE_NAME}:${IMAGE_TAG}"

# Mark that this run swapped the API: rollback.sh reverts only when the marker holds this run id
printf '%s\n' "${DEPLOY_RUN_ID}" > .api-swapped

# Swap in the staged compose file (rollback.sh restores the snapshot)
if [ -f "${COMPOSE_FILE}" ]; then
  cp -p "${COMPOSE_FILE}" "${COMPOSE_FILE}.previous"
fi
mv "${DEPLOY_DIR}/.tmp/${COMPOSE_FILE}" "${COMPOSE_FILE}"

# Restart the service
docker compose -f "${COMPOSE_FILE}" up -d --remove-orphans workshop_spring_app_qa

RUNNING_IMAGE_ID=$(docker inspect --format '{{.Image}}' workshop-api-qa)
if [ "${RUNNING_IMAGE_ID}" != "${EXPECTED_IMAGE_ID}" ]; then
  echo "workshop-api-qa runs ${RUNNING_IMAGE_ID}, expected ${EXPECTED_IMAGE_ID} (${IMAGE_REF})" >&2
  exit 1
fi
echo "workshop-api-qa runs ${IMAGE_REF}"
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
