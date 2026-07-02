#!/bin/bash
set -e
cd "${DEPLOY_DIR}"

PG_CID=$(docker compose -f "${COMPOSE_FILE}" ps -q workshop_db_qa)
if [ -z "$PG_CID" ]; then
  echo "Postgres container not found"
  exit 1
fi

echo ">>> Checking stanza..."
if docker exec -u postgres -e PGPASSWORD="${DB_PASSWORD}" "$PG_CID" \
    pgbackrest --stanza=workshop --log-level-console=info check; then
  echo "stanza OK"
else
  echo "stanza not found, creating..."
  docker exec -u postgres -e PGPASSWORD="${DB_PASSWORD}" "$PG_CID" \
    pgbackrest --stanza=workshop --log-level-console=info stanza-create
  docker exec -u postgres -e PGPASSWORD="${DB_PASSWORD}" "$PG_CID" \
    pgbackrest --stanza=workshop --log-level-console=info check
  echo "stanza created and OK"
fi

docker exec -u postgres -e PGPASSWORD="${DB_PASSWORD}" "$PG_CID" \
  pgbackrest info || true

STATUS=$(docker inspect --format="{{json .State.Health.Status}}" "$PG_CID" 2>/dev/null | tr -d '"')
echo "PostgreSQL health: ${STATUS:-unknown}"

echo "=== FINAL CONTAINERS STATUS ==="
docker compose -f "${COMPOSE_FILE}" ps
