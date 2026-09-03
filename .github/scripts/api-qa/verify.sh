#!/bin/bash
set -eo pipefail
cd "${DEPLOY_DIR}"

PG_CID=$(docker compose -f "${COMPOSE_FILE}" ps -q workshop_db_qa)
if [ -z "$PG_CID" ]; then
  echo "Postgres container not found"
  exit 1
fi

echo ">>> Checking PostgreSQL connectivity and health..."
STATUS=$(docker inspect --format="{{json .State.Health.Status}}" "$PG_CID" 2>/dev/null | tr -d '"')
echo "PostgreSQL health: ${STATUS:-unknown}"

if [ "${STATUS}" != "healthy" ]; then
  echo "ERROR: PostgreSQL container is not healthy"
  exit 1
fi

echo ">>> Checking Spring Boot API health..."
API_CID=$(docker compose -f "${COMPOSE_FILE}" ps -q workshop_spring_app_qa)
if [ -n "${API_CID}" ]; then
  API_STATUS=$(docker inspect --format="{{json .State.Health.Status}}" "${API_CID}" 2>/dev/null | tr -d '"')
  echo "API health: ${API_STATUS:-unknown}"
fi

echo "=== FINAL CONTAINERS STATUS ==="
docker compose -f "${COMPOSE_FILE}" ps
