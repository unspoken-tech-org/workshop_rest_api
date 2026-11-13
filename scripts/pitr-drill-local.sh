#!/usr/bin/env bash
# ==============================================================================
# Point-in-time restore drill of the PRD pgBackRest repository, on this machine.
#
# Restores the R2 repository into a throwaway container and volume, up to a
# target time, checks the result and removes everything on exit (also on error).
# It never touches PRD, QA, the local dev DB or the workshop_rest_api project:
# its own image tag, container and volume, no published ports, WAL archiving off.
#
# The restored data is real customer data: run it only on a trusted machine.
#
# Credentials (read-only R2 token scoped to the backup bucket), one value per file
# under SECRETS_DIR (default .secrets):
#   pgbackrest-r2-ro-access-key  pgbackrest-r2-ro-secret-key
#   pgbackrest-r2-bucket         pgbackrest-r2-endpoint
# Environment:
#   PITR_DB_USER, PITR_DB_NAME   PostgreSQL role and database of PRD (required)
#   PITR_TARGET                  target time, e.g. "2026-11-12 18:00:00-03"; default:
#                                stop time of the latest backup minus 4 hours
#   PITR_REPO_PATH               repo1-path in the bucket (default /var/lib/pgbackrest)
#   PITR_TIMEOUT_SECONDS         wait for recovery to finish (default 900)
# ==============================================================================
set -Eeuo pipefail

ROOT="$(cd "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
SECRETS_DIR="${SECRETS_DIR:-${ROOT}/.secrets}"
DB_USER="${PITR_DB_USER:?PITR_DB_USER is required}"
DB_NAME="${PITR_DB_NAME:?PITR_DB_NAME is required}"
REPO_PATH="${PITR_REPO_PATH:-/var/lib/pgbackrest}"
TIMEOUT="${PITR_TIMEOUT_SECONDS:-900}"
STANZA=workshop
IMAGE=workshop-pitr-drill:local
NAME="workshop-pitr-drill-$(date +%s)"
VOLUME="${NAME}-data"
PGDATA=/var/lib/postgresql/data

read_secret() {
  local file="${SECRETS_DIR}/$1"
  [ -s "${file}" ] || { echo "missing ${file}" >&2; exit 1; }
  tr -d '\r\n' < "${file}"
}

WORK="$(mktemp -d)"
chmod 700 "${WORK}"
cleanup() {
  docker rm -f "${NAME}" >/dev/null 2>&1 || true
  docker volume rm -f "${VOLUME}" >/dev/null 2>&1 || true
  rm -rf -- "${WORK}"
  echo "Drill resources removed (${NAME}, ${VOLUME})"
}
trap cleanup EXIT

# pgBackRest reads PGBACKREST_* variables as options; the file stays 600 and is
# read by the Docker CLI, never mounted into the container.
umask 077
{
  echo "PGBACKREST_REPO1_TYPE=s3"
  echo "PGBACKREST_REPO1_PATH=${REPO_PATH}"
  echo "PGBACKREST_REPO1_S3_REGION=auto"
  echo "PGBACKREST_REPO1_S3_BUCKET=$(read_secret pgbackrest-r2-bucket)"
  ENDPOINT="$(read_secret pgbackrest-r2-endpoint)"
  echo "PGBACKREST_REPO1_S3_ENDPOINT=${ENDPOINT#https://}"
  echo "PGBACKREST_REPO1_S3_KEY=$(read_secret pgbackrest-r2-ro-access-key)"
  echo "PGBACKREST_REPO1_S3_KEY_SECRET=$(read_secret pgbackrest-r2-ro-secret-key)"
  echo "PGBACKREST_PG1_PATH=${PGDATA}"
  echo "PGBACKREST_LOG_LEVEL_CONSOLE=info"
  echo "PGBACKREST_LOG_LEVEL_FILE=off"
} > "${WORK}/pgbackrest.env"

pgbackrest() {
  docker run --rm -u postgres --env-file "${WORK}/pgbackrest.env" \
    -v "${VOLUME}:${PGDATA}" "${IMAGE}" pgbackrest --stanza="${STANZA}" "$@"
}

echo ">>> Building ${IMAGE} from the PRD database Dockerfile"
docker build -q -t "${IMAGE}" -f "${ROOT}/apps/api/docker/Dockerfile.pgbackrest" "${ROOT}/apps/api/docker" >/dev/null
docker volume create "${VOLUME}" >/dev/null

echo ">>> Reading the repository"
INFO="$(pgbackrest --output=json info)"
jq -er '.[0].status.code == 0' <<<"${INFO}" >/dev/null || {
  echo "stanza ${STANZA} is not ok: $(jq -r '.[0].status.message' <<<"${INFO}")" >&2; exit 1; }
jq -r '.[0].backup[] | "  \(.type) \(.label) stop=\(.timestamp.stop | todate) error=\(.error)"' <<<"${INFO}"

if [ -z "${PITR_TARGET:-}" ]; then
  LAST_STOP="$(jq -er '[.[0].backup[] | select(.error | not) | .timestamp.stop] | max' <<<"${INFO}")"
  PITR_TARGET="$(date -u -d "@$(( LAST_STOP - 4 * 3600 ))" '+%Y-%m-%d %H:%M:%S+00')"
fi
TARGET_EPOCH="$(date -d "${PITR_TARGET}" +%s)"
echo ">>> Target time: ${PITR_TARGET}"

echo ">>> Restoring (pgBackRest picks the latest backup before the target)"
STARTED="$(date +%s)"
pgbackrest --type=time --target="${PITR_TARGET}" --target-action=promote restore

# The restore_command written by pgBackRest fetches WAL from R2, so the container
# needs egress; archive_mode=off keeps the restored cluster from pushing WAL anywhere.
echo ">>> Starting the restored cluster and replaying WAL"
docker run -d --name "${NAME}" --env-file "${WORK}/pgbackrest.env" \
  -v "${VOLUME}:${PGDATA}" "${IMAGE}" postgres -c archive_mode=off >/dev/null

psql_drill() {
  docker exec -u postgres "${NAME}" psql -v ON_ERROR_STOP=1 -U "${DB_USER}" -d "${DB_NAME}" -Atc "$1"
}
deadline=$(( $(date +%s) + TIMEOUT ))
until [ "$(psql_drill 'SELECT pg_is_in_recovery()' 2>/dev/null)" = "f" ]; do
  if [ "$(docker inspect -f '{{.State.Running}}' "${NAME}" 2>/dev/null)" != "true" ]; then
    echo "restored cluster stopped during recovery; last log lines:" >&2
    docker logs --tail 30 "${NAME}" >&2 || true
    echo "hint: 'recovery ended before configured recovery target was reached' means no commit after the target; pick an earlier PITR_TARGET" >&2
    exit 1
  fi
  if [ "$(date +%s)" -ge "${deadline}" ]; then
    echo "recovery did not finish within ${TIMEOUT}s" >&2
    docker logs --tail 30 "${NAME}" >&2 || true
    exit 1
  fi
  sleep 5
done
echo ">>> Recovery finished and promoted in $(( $(date +%s) - STARTED ))s"

echo ">>> Checks"
FAIL=0
LAST_TOKEN="$(psql_drill 'SELECT coalesce(extract(epoch FROM max(created_at))::bigint, 0) FROM refresh_tokens')"
if [ "${LAST_TOKEN}" -gt "${TARGET_EPOCH}" ]; then
  echo "  FAIL refresh_tokens has rows after the target"; FAIL=1
else
  echo "  OK   latest refresh_tokens.created_at is $(( (TARGET_EPOCH - LAST_TOKEN) / 60 )) min before the target"
fi
echo "  OK   latest Flyway version: $(psql_drill 'SELECT max(version::int) FROM flyway_schema_history WHERE success')"
echo "  Row counts:"
for table in customers devices payments device_history technicians api_keys refresh_tokens; do
  echo "    ${table}: $(psql_drill "SELECT count(*) FROM ${table}")"
done
for table in customers devices; do
  [ "$(psql_drill "SELECT count(*) FROM ${table}")" -gt 0 ] || { echo "  FAIL ${table} is empty"; FAIL=1; }
done

if [ "${FAIL}" -ne 0 ]; then
  echo "PITR drill FAILED" >&2
  exit 1
fi
echo "PITR drill passed: target ${PITR_TARGET}"
