#!/bin/bash
# ==============================================================================
# Script: restore_from_r2_qa.sh
# Purpose: Stop QA API, wipe data volume, restore physical pgBackRest backup 
#          from Cloudflare R2, apply role/db adjustments in Single-User Mode, 
#          run data masking (LGPD), and dynamically bulk-insert all QA API keys 
#          from Infisical path /qa-api-keys.
# ==============================================================================
set -euo pipefail

DEPLOY_DIR="/home/workshop/deploy/workshop_rest_api_qa"
COMPOSE_FILE="${DEPLOY_DIR}/docker-compose-qa.yml"
QA_ENV_FILE="${DEPLOY_DIR}/.env"
MASK_SQL_FILE="${DEPLOY_DIR}/scripts/sql/mask_data_qa.sql"

# Fail-Closed error handler to destroy unmasked volume on premature abort
cleanup_on_error() {
  echo ">>> [ALERT] QA Restore failed! Destroying unmasked data volume..."
  docker compose -f "${COMPOSE_FILE}" down -v || true
  exit 1
}
trap cleanup_on_error ERR

if [ ! -f "${QA_ENV_FILE}" ]; then
  echo "ERROR: QA environment file not found: ${QA_ENV_FILE}"
  exit 1
fi

# 1. Read QA and R2 environment variables from .env
QA_DB_USER=$(grep '^DB_USERNAME=' "${QA_ENV_FILE}" | cut -d'=' -f2-)
QA_DB_PASS=$(grep '^DB_PASSWORD=' "${QA_ENV_FILE}" | cut -d'=' -f2-)
QA_DB_NAME=$(grep '^DB_NAME=' "${QA_ENV_FILE}" | cut -d'=' -f2-)

PGBACKREST_S3_BUCKET=$(grep '^PGBACKREST_S3_BUCKET=' "${QA_ENV_FILE}" | cut -d'=' -f2-)
PGBACKREST_S3_ENDPOINT=$(grep '^PGBACKREST_S3_ENDPOINT=' "${QA_ENV_FILE}" | cut -d'=' -f2-)
PGBACKREST_S3_REGION=$(grep '^PGBACKREST_S3_REGION=' "${QA_ENV_FILE}" | cut -d'=' -f2-)
PGBACKREST_S3_KEY=$(grep '^PGBACKREST_S3_KEY=' "${QA_ENV_FILE}" | cut -d'=' -f2-)
PGBACKREST_S3_KEY_SECRET=$(grep '^PGBACKREST_S3_KEY_SECRET=' "${QA_ENV_FILE}" | cut -d'=' -f2-)

# 2. Export QA Secrets from Infisical path /qa-api-keys
INFISICAL_TOKEN_ENV=$(grep '^INFISICAL_TOKEN=' "${QA_ENV_FILE}" | cut -d'=' -f2- || echo "")

SECRETS_JSON="[]"

if command -v infisical >/dev/null 2>&1; then
  echo ">>> Authenticating and exporting secrets from Infisical path /qa-api-keys..."
  if [ -n "${INFISICAL_TOKEN_ENV}" ]; then
    export INFISICAL_TOKEN="${INFISICAL_TOKEN_ENV}"
  fi
  SECRETS_JSON=$(infisical export --env=staging --path=/qa-api-keys --format=json 2>/dev/null || infisical export --env=qa --path=/qa-api-keys --format=json 2>/dev/null || echo "[]")
fi

if [ "${SECRETS_JSON}" != "[]" ] && ! echo "${SECRETS_JSON}" | python3 -m json.tool >/dev/null 2>&1; then
  echo "WARNING: Infisical returned invalid JSON. Falling back to empty key set."
  SECRETS_JSON="[]"
fi

KEY_COUNT=$(echo "${SECRETS_JSON}" | python3 -c "import sys,json; print(len(json.loads(sys.stdin.read())))" 2>/dev/null || echo "0")
echo ">>> Infisical returned ${KEY_COUNT} key(s) from /qa-api-keys"

# Default production database name restored in physical dump
PRD_DB_NAME_DEFAULT="work_shop_db"

echo ">>> [1/6] Stopping QA API and destroying existing database volume..."
docker compose -f "${COMPOSE_FILE}" down -v || true

echo ">>> [2/6] Executing pgBackRest physical restore from Cloudflare R2..."
docker run --rm \
  -v workshop_rest_api_qa_db_data:/var/lib/postgresql/data \
  -v /srv/pgbackrest/conf:/etc/pgbackrest:ro \
  -v /srv/pgbackrest/repo:/var/lib/pgbackrest \
  -v /srv/pgbackrest/logs:/var/log/pgbackrest \
  -e PGBACKREST_REPO1_TYPE=s3 \
  -e PGBACKREST_REPO1_S3_BUCKET="${PGBACKREST_S3_BUCKET}" \
  -e PGBACKREST_REPO1_S3_ENDPOINT="${PGBACKREST_S3_ENDPOINT}" \
  -e PGBACKREST_REPO1_S3_REGION="${PGBACKREST_S3_REGION}" \
  -e PGBACKREST_REPO1_S3_KEY="${PGBACKREST_S3_KEY}" \
  -e PGBACKREST_REPO1_S3_KEY_SECRET="${PGBACKREST_S3_KEY_SECRET}" \
  workshop/postgres16-pgbackrest:latest \
  pgbackrest --stanza=workshop --log-level-console=info restore

echo ">>> [3/6] Adjusting roles and DB name in Single-User Mode (Zero Prod Secrets)..."
docker run --rm \
  -v workshop_rest_api_qa_db_data:/var/lib/postgresql/data \
  workshop/postgres16-pgbackrest:latest \
  postgres --single -D /var/lib/postgresql/data postgres <<EOF
ALTER DATABASE ${PRD_DB_NAME_DEFAULT} RENAME TO ${QA_DB_NAME};
CREATE ROLE ${QA_DB_USER} WITH SUPERUSER LOGIN PASSWORD '${QA_DB_PASS}';
DROP ROLE IF EXISTS work_shop_prd;
EOF

echo ">>> [4/6] Starting QA PostgreSQL container for recovery and anonymization..."
docker compose -f "${COMPOSE_FILE}" up -d workshop_db_qa

echo ">>> Waiting for QA PostgreSQL to be ready..."
ATTEMPTS=45
DB_READY=false
for i in $(seq 1 $ATTEMPTS); do
  if docker exec -i workshop-db-qa pg_isready -U "${QA_DB_USER}" -d "${QA_DB_NAME}" >/dev/null 2>&1; then
    echo "PostgreSQL QA is ready!"
    DB_READY=true
    break
  fi
  echo "[$i/$ATTEMPTS] Waiting for database..."
  sleep 2
done

if [ "$DB_READY" = false ]; then
  echo "ERROR: PostgreSQL failed to start in time."
  docker logs --tail=100 workshop-db-qa || true
  exit 1
fi

echo ">>> [5/6] Executing data anonymization (Data Masking - LGPD)..."
if [ -f "${MASK_SQL_FILE}" ]; then
  docker cp "${MASK_SQL_FILE}" workshop-db-qa:/tmp/mask_data_qa.sql
  docker exec -i -e PGPASSWORD="${QA_DB_PASS}" workshop-db-qa psql -U "${QA_DB_USER}" -d "${QA_DB_NAME}" -f /tmp/mask_data_qa.sql
  docker exec -i workshop-db-qa rm -f /tmp/mask_data_qa.sql
  echo "Data Masking executed successfully!"
else
  echo "WARNING: File ${MASK_SQL_FILE} not found. Skipping PII anonymization."
fi

# 5.1 Dynamically construct multi-row INSERT SQL for all API Keys exported from Infisical path /qa-api-keys
echo ">>> [5.1/6] Dynamically processing all Infisical API Keys from /qa-api-keys into a single INSERT..."
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DYNAMIC_INSERT_SQL=$(python3 "${SCRIPT_DIR}/sql/insert_api_keys.py" "${SECRETS_JSON}" 2>/tmp/insert_api_keys.err || echo "")

if [ -n "${DYNAMIC_INSERT_SQL}" ]; then
  echo ">>> Injecting all Infisical API Keys from /qa-api-keys in a single SQL command..."
  docker exec -i -e PGPASSWORD="${QA_DB_PASS}" workshop-db-qa psql -U "${QA_DB_USER}" -d "${QA_DB_NAME}" -c "${DYNAMIC_INSERT_SQL}"
  echo "Infisical API Keys inserted successfully!"
else
  echo ">>> No API Keys found or generated. Skipping injection."
  if [ -f /tmp/insert_api_keys.err ] && [ -s /tmp/insert_api_keys.err ]; then
    echo ">>> Script output: $(cat /tmp/insert_api_keys.err)"
  fi
fi

# Disable error trap upon successful completion
trap - ERR

echo ">>> [6/6] Restarting QA Spring Boot API..."
if [ -z "${IMAGE_TAG:-}" ]; then
  DETECTED_TAG=$(docker images --format "{{.Tag}}" workshop_rest_api_qa | grep -v "backup" | head -n 1 || echo "latest")
  export IMAGE_TAG="${DETECTED_TAG}"
fi
docker compose -f "${COMPOSE_FILE}" up -d workshop_spring_app_qa

echo ">>> Restore, sanitization, and startup executed successfully!"
