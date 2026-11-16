#!/usr/bin/env bash
# ==============================================================================
# Backup and restore test of the PRD database image, on synthetic data.
#
# Builds the PRD image (apps/api/docker/Dockerfile.pgbackrest: same PostgreSQL and
# pgBackRest versions), starts it with WAL archiving to a throwaway posix
# repository, writes synthetic rows, takes a full backup, writes more rows (only
# in the archived WAL), destroys the data volume, restores it with pgBackRest and
# checks that every row came back. Needs only Docker and jq: no secrets, no R2,
# no PRD or QA access, no published ports. Everything is removed on exit.
#
# Environment:
#   RESTORE_TEST_PREFIX           name prefix of image, containers and volumes
#                                 (default workshop-restore-test)
#   RESTORE_TEST_TIMEOUT_SECONDS  wait for startup and recovery (default 180)
# ==============================================================================
set -Eeuo pipefail

ROOT="$(cd "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
PREFIX="${RESTORE_TEST_PREFIX:-workshop-restore-test}"
TIMEOUT="${RESTORE_TEST_TIMEOUT_SECONDS:-180}"
STANZA=restore-test
IMAGE="${PREFIX}:local"
NAME="${PREFIX}-$(date +%s)"
DATA_VOLUME="${NAME}-data"
REPO_VOLUME="${NAME}-repo"
PGDATA=/var/lib/postgresql/data
REPO_PATH=/var/lib/pgbackrest
DB_NAME=restore_test

cleanup() {
  docker rm -f "${NAME}" "${NAME}-restored" >/dev/null 2>&1 || true
  docker volume rm -f "${DATA_VOLUME}" "${REPO_VOLUME}" >/dev/null 2>&1 || true
  echo "Restore test resources removed (${NAME}*)"
}
trap cleanup EXIT

# pgBackRest reads PGBACKREST_* variables as options, inside every container.
PGBACKREST_ENV=(
  -e PGBACKREST_STANZA="${STANZA}"
  -e PGBACKREST_REPO1_TYPE=posix
  -e PGBACKREST_REPO1_PATH="${REPO_PATH}"
  -e PGBACKREST_REPO1_RETENTION_FULL=1
  -e PGBACKREST_PG1_PATH="${PGDATA}"
  -e PGBACKREST_START_FAST=y
  -e PGBACKREST_LOG_LEVEL_CONSOLE=warn
  -e PGBACKREST_LOG_LEVEL_FILE=off
)
VOLUMES=(-v "${DATA_VOLUME}:${PGDATA}" -v "${REPO_VOLUME}:${REPO_PATH}")

# Starts the cluster in the data volume and waits until it accepts TCP connections
# (the entrypoint's init server listens on the socket only) and leaves recovery.
start_db() {
  local container=$1; shift
  docker run -d --name "${container}" "${PGBACKREST_ENV[@]}" "${VOLUMES[@]}" \
    -e POSTGRES_PASSWORD="restore-test-$(date +%s)" -e POSTGRES_DB="${DB_NAME}" \
    "${IMAGE}" postgres "$@" >/dev/null
  local deadline=$(( $(date +%s) + TIMEOUT ))
  until [ "$(docker exec -u postgres "${container}" psql -h 127.0.0.1 -d "${DB_NAME}" -Atc \
      'SELECT pg_is_in_recovery()' 2>/dev/null)" = "f" ]; do
    if [ "$(docker inspect -f '{{.State.Running}}' "${container}" 2>/dev/null)" != "true" ] \
        || [ "$(date +%s)" -ge "${deadline}" ]; then
      echo "${container} did not become ready; last log lines:" >&2
      docker logs --tail 30 "${container}" >&2 || true
      exit 1
    fi
    sleep 1
  done
}

psql_in() {
  docker exec -u postgres "$1" psql -v ON_ERROR_STOP=1 -h 127.0.0.1 -d "${DB_NAME}" -Atc "$2"
}

# Row counts and a content checksum of both synthetic tables.
fingerprint() {
  psql_in "$1" "SELECT (SELECT count(*) FROM probe_customers) || ' ' || (SELECT count(*) FROM probe_devices)
    || ' ' || md5(coalesce((SELECT string_agg(c.id || c.name || d.id || d.serial, ',' ORDER BY d.id)
       FROM probe_devices d JOIN probe_customers c ON c.id = d.customer_id), ''))"
}

echo ">>> Building ${IMAGE} from the PRD database Dockerfile"
docker build -q -t "${IMAGE}" -f "${ROOT}/apps/api/docker/Dockerfile.pgbackrest" "${ROOT}/apps/api/docker" >/dev/null
docker run --rm "${IMAGE}" sh -c 'postgres --version; pgbackrest version'

echo ">>> Starting PostgreSQL with WAL archiving to a throwaway posix repository"
docker volume create "${DATA_VOLUME}" >/dev/null
docker volume create "${REPO_VOLUME}" >/dev/null
start_db "${NAME}" -c wal_level=replica -c archive_mode=on \
  -c "archive_command=pgbackrest --stanza=${STANZA} archive-push %p"
docker exec -u postgres "${NAME}" pgbackrest stanza-create
docker exec -u postgres "${NAME}" pgbackrest check

echo ">>> Writing synthetic data and taking a full backup"
psql_in "${NAME}" "CREATE TABLE probe_customers (id int PRIMARY KEY, name text NOT NULL);
  CREATE TABLE probe_devices (id int PRIMARY KEY, customer_id int NOT NULL REFERENCES probe_customers, serial text NOT NULL);
  INSERT INTO probe_customers SELECT g, 'customer-' || g FROM generate_series(1, 500) g;
  INSERT INTO probe_devices SELECT g, 1 + g % 500, md5(g::text) FROM generate_series(1, 2000) g;" >/dev/null
docker exec -u postgres "${NAME}" pgbackrest --type=full backup

# Rows written after the backup exist only in the archived WAL: the restore must
# replay it. pgbackrest check switches WAL and waits for the segment to be archived.
psql_in "${NAME}" "INSERT INTO probe_customers SELECT g, 'customer-' || g FROM generate_series(501, 600) g;
  INSERT INTO probe_devices SELECT g, 501 + g % 100, md5(g::text) FROM generate_series(2001, 2300) g;
  UPDATE probe_customers SET name = name || '-updated' WHERE id <= 10;" >/dev/null
docker exec -u postgres "${NAME}" pgbackrest check
EXPECTED="$(fingerprint "${NAME}")"
docker exec -u postgres "${NAME}" pgbackrest --output=json info \
  | jq -r '.[0].backup[] | "  backup \(.type) \(.label) error=\(.error)"'

echo ">>> Destroying the cluster and its data volume"
docker rm -f "${NAME}" >/dev/null
docker volume rm "${DATA_VOLUME}" >/dev/null
docker volume create "${DATA_VOLUME}" >/dev/null

echo ">>> Restoring from the repository (latest backup + archived WAL)"
docker run --rm -u postgres "${PGBACKREST_ENV[@]}" "${VOLUMES[@]}" "${IMAGE}" pgbackrest restore
STARTED="$(date +%s)"
start_db "${NAME}-restored" -c archive_mode=off
echo ">>> Recovery finished and promoted in $(( $(date +%s) - STARTED ))s"

echo ">>> Checks"
ACTUAL="$(fingerprint "${NAME}-restored")"
echo "  expected: ${EXPECTED}"
echo "  restored: ${ACTUAL}"
if [ "${ACTUAL}" != "${EXPECTED}" ]; then
  echo "Restore test FAILED: restored data differs from the data before the destroy" >&2
  exit 1
fi
UPDATED="$(psql_in "${NAME}-restored" "SELECT count(*) FROM probe_customers WHERE name LIKE '%-updated'")"
if [ "${UPDATED}" != "10" ]; then
  echo "Restore test FAILED: ${UPDATED} updated rows after restore, expected 10 (WAL replay)" >&2
  exit 1
fi
echo "Restore test OK: counts and checksum match, post-backup WAL replayed"
