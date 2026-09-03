#!/bin/bash
# ==============================================================================
# Script: reset_qa_db.sh
# Purpose: Idempotent reset of QA database:
#          1. Stops QA Spring Boot API
#          2. Drops and recreates schema public (with permissions & extensions)
#          3. Starts API for Flyway migrations to run cleanly
#          4. Injects synthetic seed dataset (qa_seed_data.sql)
#          5. Dynamically injects QA API keys from Infisical path /qa-api-keys
# ==============================================================================
set -euo pipefail

DEPLOY_DIR="${DEPLOY_DIR:-/home/workshop/deploy/workshop_rest_api_qa}"
COMPOSE_FILE="${DEPLOY_DIR}/docker-compose-qa.yml"
QA_ENV_FILE="${DEPLOY_DIR}/.env"
SEED_FILE="${DEPLOY_DIR}/scripts/sql/qa_seed_data.sql"
INSERT_KEYS_SCRIPT="${DEPLOY_DIR}/scripts/sql/insert_api_keys.py"

if [ ! -f "${QA_ENV_FILE}" ]; then
  echo "ERROR: QA environment file not found: ${QA_ENV_FILE}"
  exit 1
fi

DB_NAME=$(grep '^DB_NAME=' "${QA_ENV_FILE}" | cut -d'=' -f2-)
DB_USER=$(grep '^DB_USERNAME=' "${QA_ENV_FILE}" | cut -d'=' -f2-)
DB_PASS=$(grep '^DB_PASSWORD=' "${QA_ENV_FILE}" | cut -d'=' -f2-)

PGPASS_FILE=$(mktemp /tmp/pgpass_reset.XXXXXX)
chmod 600 "${PGPASS_FILE}"
echo "workshop-db-qa:5432:${DB_NAME}:${DB_USER}:${DB_PASS}" > "${PGPASS_FILE}"

cleanup() {
  rm -f "${PGPASS_FILE}" 2>/dev/null || true
  docker exec -i workshop-db-qa rm -f /tmp/pgpass_qa 2>/dev/null || true
}
trap cleanup EXIT ERR

echo ">>> [1/5] Stopping QA API temporarily to release active database connections..."
docker compose -f "${COMPOSE_FILE}" stop workshop_spring_app_qa || true

echo ">>> [2/5] Resetting schema public, permissions and extensions in QA DB..."
docker exec -i workshop-db-qa sh -c 'cp /dev/stdin /tmp/pgpass_qa && chmod 600 /tmp/pgpass_qa' < "${PGPASS_FILE}"

docker exec -i -e PGPASSFILE=/tmp/pgpass_qa workshop-db-qa psql -U "${DB_USER}" -d "${DB_NAME}" << 'INNER_EOF'
DROP SCHEMA public CASCADE;
CREATE SCHEMA public;
GRANT ALL ON SCHEMA public TO CURRENT_USER;
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS unaccent;
INNER_EOF

echo ">>> [3/5] Starting QA API for Flyway migrations..."
docker compose -f "${COMPOSE_FILE}" start workshop_spring_app_qa

echo ">>> Waiting for Flyway migrations to complete..."
ATTEMPTS=45
MIGRATIONS_READY=false
for i in $(seq 1 $ATTEMPTS); do
  if docker exec -i -e PGPASSFILE=/tmp/pgpass_qa workshop-db-qa psql -U "${DB_USER}" -d "${DB_NAME}" \
      -c "SELECT 1 FROM flyway_schema_history" >/dev/null 2>&1; then
    echo "Flyway migrations completed successfully!"
    MIGRATIONS_READY=true
    break
  fi
  echo "[$i/$ATTEMPTS] Waiting for Flyway migrations..."
  sleep 2
done

if [ "$MIGRATIONS_READY" = false ]; then
  echo "ERROR: Flyway migrations timed out."
  docker logs --tail=100 workshop-api-qa || true
  exit 1
fi

echo ">>> [4/5] Injecting synthetic business dataset (qa_seed_data.sql)..."
if [ -f "${SEED_FILE}" ]; then
  docker exec -i -e PGPASSFILE=/tmp/pgpass_qa workshop-db-qa psql -U "${DB_USER}" -d "${DB_NAME}" -v ON_ERROR_STOP=1 < "${SEED_FILE}"
  echo "Synthetic dataset populated successfully!"
else
  echo "ERROR: Seed file not found: ${SEED_FILE}"
  exit 1
fi

echo ">>> [5/5] Injecting QA API keys from Infisical path /qa-api-keys..."
SECRETS_JSON="[]"
if command -v infisical >/dev/null 2>&1; then
  SECRETS_JSON=$(infisical export --env=staging --path=/qa-api-keys --format=json 2>/dev/null || infisical export --env=qa --path=/qa-api-keys --format=json 2>/dev/null || echo "[]")
fi

if [ "${SECRETS_JSON}" != "[]" ] && [ -f "${INSERT_KEYS_SCRIPT}" ]; then
  DYNAMIC_INSERT_SQL=$(python3 "${INSERT_KEYS_SCRIPT}" "${SECRETS_JSON}" 2>/dev/null || echo "")
  if [ -n "${DYNAMIC_INSERT_SQL}" ]; then
    docker exec -i -e PGPASSFILE=/tmp/pgpass_qa workshop-db-qa psql -U "${DB_USER}" -d "${DB_NAME}" -c "${DYNAMIC_INSERT_SQL}"
    echo "QA API keys injected successfully!"
  else
    echo "WARNING: Could not generate SQL for API keys from Infisical."
  fi
else
  echo "NOTICE: Infisical keys not available or empty. Skipping key injection."
fi

echo ">>> QA Database reset and seed completed successfully!"
