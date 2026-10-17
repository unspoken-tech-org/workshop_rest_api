#!/bin/bash
set -e
cd /home/workshop/deploy/workshop_rest_api

# Counts ERROR records per logger name. Messages are never printed: they can carry data.
logger_counts() {
  sed -n 's/.*"logger"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' | sort | uniq -c
}

# API canary runs first, on the server so no new connection is opened from CI.
# Exit 42 is reserved for canary failures: the workflow rolls back on that code only.
echo ">>> Checking API canary..."
API_HEALTH=$(docker inspect --format='{{json .State.Health.Status}}' workshop-api 2>/dev/null | tr -d '"') || true
if [ "${API_HEALTH}" != "healthy" ]; then
  echo "ERROR: workshop-api health is ${API_HEALTH:-unknown}, expected healthy" >&2
  exit 42
fi

# busybox wget ships in the JRE image (the compose healthcheck uses it too).
if ! API_BODY=$(docker exec workshop-api wget -q -T 10 -O - http://localhost:8080/actuator/health); then
  echo "ERROR: GET /actuator/health failed inside workshop-api" >&2
  exit 42
fi
if ! printf '%s' "${API_BODY}" | grep -Eq '^\{[[:space:]]*"status"[[:space:]]*:[[:space:]]*"UP"'; then
  echo "ERROR: /actuator/health does not report status UP" >&2
  exit 42
fi

# Production logs are JSON. ERROR records since the current start fail the canary,
# unless they carry a requestId (set per HTTP request): those only warn, because
# some client errors are logged as ERROR.
if ! API_STARTED_AT=$(docker inspect --format='{{.State.StartedAt}}' workshop-api); then
  echo "ERROR: could not inspect workshop-api" >&2
  exit 42
fi
if ! API_LOGS=$(docker logs --since "${API_STARTED_AT}" workshop-api 2>&1); then
  echo "ERROR: could not read workshop-api logs" >&2
  exit 42
fi
API_ERRORS=$(printf '%s\n' "${API_LOGS}" | grep -E '"level"[[:space:]]*:[[:space:]]*"ERROR"' || true)
if [ -n "${API_ERRORS}" ]; then
  REQUEST_ERRORS=$(printf '%s\n' "${API_ERRORS}" | grep -E '"requestId"[[:space:]]*:' || true)
  OUTSIDE_ERRORS=$(printf '%s\n' "${API_ERRORS}" | grep -vE '"requestId"[[:space:]]*:' || true)
  if [ -n "${REQUEST_ERRORS}" ]; then
    echo "::warning::workshop-api logged $(printf '%s\n' "${REQUEST_ERRORS}" | wc -l) ERROR record(s) inside requests since start"
  fi
  if [ -n "${OUTSIDE_ERRORS}" ]; then
    echo "ERROR: workshop-api logged $(printf '%s\n' "${OUTSIDE_ERRORS}" | wc -l) ERROR record(s) outside requests since start, by logger:" >&2
    printf '%s\n' "${OUTSIDE_ERRORS}" | logger_counts >&2
    exit 42
  fi
fi
echo "API canary OK"

# pgBackRest and PostgreSQL checks run after the canary: their failures exit 1 and do not roll back.
PG_CID=$(docker compose -f docker-compose-production.yml ps -q workshop_db)
if [ -z "$PG_CID" ]; then
  echo "Postgres container not found"
  exit 1
fi

echo ">>> Checking stanza..."
if docker exec -u postgres "$PG_CID" bash -c 'pgbackrest --stanza=workshop --log-level-console=info check'; then
  echo "stanza OK"
else
  echo "stanza not found, creating..."
  docker exec -u postgres "$PG_CID" bash -c 'pgbackrest --stanza=workshop --log-level-console=info stanza-create'
  docker exec -u postgres "$PG_CID" bash -c 'pgbackrest --stanza=workshop --log-level-console=info check'
  echo "stanza created and OK"
fi

docker exec -u postgres "$PG_CID" bash -c 'pgbackrest info' || true

STATUS=$(docker inspect --format="{{json .State.Health.Status}}" "$PG_CID" 2>/dev/null | tr -d '"')
echo "PostgreSQL health: ${STATUS:-unknown}"

echo "=== FINAL CONTAINERS STATUS ==="
docker compose -f docker-compose-production.yml ps

# Everything passed: the snapshot and the swap marker are no longer needed for rollback.
rm -f docker-compose-production.yml.previous .api-swapped
