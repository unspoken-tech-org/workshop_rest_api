#!/bin/bash
set -eo pipefail
cd "${DEPLOY_DIR}"

echo ">>> Rolling back observability deployment..."
docker compose -f "${COMPOSE_FILE}" up -d --remove-orphans || true
docker compose -f "${COMPOSE_FILE}" ps || true
echo "Rollback executed"
