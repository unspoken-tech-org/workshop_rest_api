#!/bin/bash
set -e
cd "${DEPLOY_DIR}"

# Ensure pgbackrest directories and config exist (idempotent — safe to run repeatedly)
# Read DB_USERNAME from .env to match the PostgreSQL user
DB_USER=$(grep '^DB_USERNAME=' .env | cut -d'=' -f2)
PGBACKREST_CONF="/srv/pgbackrest/conf/pgbackrest.conf"

# Create required directories with correct permissions
mkdir -p /srv/pgbackrest/conf
mkdir -p /srv/pgbackrest/repo
mkdir -p /srv/pgbackrest/logs

# Ensure postgres user (UID 999) can write to repo and logs dirs
sudo chown -R 999:999 /srv/pgbackrest/repo /srv/pgbackrest/logs 2>/dev/null || true

cat > "${PGBACKREST_CONF}" << EOF
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
pg1-user=${DB_USER}
EOF

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
