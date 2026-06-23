#!/bin/bash
set -e
cd /home/workshop/deploy/workshop_rest_api

PG_CID=$(docker compose -f docker-compose-production.yml ps -q workshop_db)
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
docker compose -f docker-compose-production.yml ps
