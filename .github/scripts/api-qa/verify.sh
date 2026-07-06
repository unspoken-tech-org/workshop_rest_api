#!/bin/bash
set -eo pipefail
cd "${DEPLOY_DIR}"

PG_CID=$(docker compose -f "${COMPOSE_FILE}" ps -q workshop_db_qa)
if [ -z "$PG_CID" ]; then
  echo "Postgres container not found"
  exit 1
fi

# pgbackrest.conf is mounted via bind mount in /etc/pgbackrest/pgbackrest.conf:ro

echo ">>> Checking stanza..."
if docker exec -u postgres -e PGPASSWORD="\$POSTGRES_PASSWORD" "$PG_CID" \
    pgbackrest --stanza=workshop --log-level-console=info check; then
  echo "stanza OK"
else
  echo "stanza not found, creating..."
  docker exec -u postgres -e PGPASSWORD="\$POSTGRES_PASSWORD" "$PG_CID" \
    pgbackrest --stanza=workshop --log-level-console=info stanza-create
  docker exec -u postgres -e PGPASSWORD="\$POSTGRES_PASSWORD" "$PG_CID" \
    pgbackrest --stanza=workshop --log-level-console=info check
  echo "stanza created and OK"
fi

docker exec -u postgres -e PGPASSWORD="\$POSTGRES_PASSWORD" "$PG_CID" \
  pgbackrest info || true

# Verify backups exist (protect against data loss)
echo ">>> Verifying backups..."
BACKUP_COUNT=$(docker exec -u postgres "$PG_CID" \
  pgbackrest --stanza=workshop info --output=json 2>/dev/null | \
  grep -c '"type":"full"' || echo "0")

if [ "$BACKUP_COUNT" -eq 0 ]; then
  echo "No backups found, creating initial full backup..."
  docker exec -u postgres "$PG_CID" \
    pgbackrest --stanza=workshop --log-level-console=info backup --type=full
  echo "Initial backup created"
else
  echo "Found $BACKUP_COUNT backup(s)"
fi

STATUS=$(docker inspect --format="{{json .State.Health.Status}}" "$PG_CID" 2>/dev/null | tr -d '"')
echo "PostgreSQL health: ${STATUS:-unknown}"

echo "=== FINAL CONTAINERS STATUS ==="
docker compose -f "${COMPOSE_FILE}" ps
