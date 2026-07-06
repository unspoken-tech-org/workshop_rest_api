#!/bin/bash
set -euo pipefail
cd /home/workshop/deploy/workshop_rest_api

LOCAL_IMAGE_NAME="${LOCAL_IMAGE_NAME:-workshop_rest_api_prod}"
COMPOSE_FILE="docker-compose-production.yml"
OVERRIDE_FILE="${COMPOSE_FILE%.yml}.override.yml"
PREVIOUS_OVERRIDE="${OVERRIDE_FILE}.previous"
export IMAGE_TAG="${IMAGE_TAG:-latest}"

if ! docker image inspect "${LOCAL_IMAGE_NAME}:backup" >/dev/null 2>&1; then
  echo "No backup image found"
  exit 1
fi

docker tag "${LOCAL_IMAGE_NAME}:backup" "${LOCAL_IMAGE_NAME}:${IMAGE_TAG}"

if [ -f "${PREVIOUS_OVERRIDE}" ]; then
  mv "${PREVIOUS_OVERRIDE}" "${OVERRIDE_FILE}"
  docker compose -f "${COMPOSE_FILE}" -f "${OVERRIDE_FILE}" up -d --force-recreate workshop_spring_app
  echo "Rollback executed (override restored)"
else
  rm -f "${OVERRIDE_FILE}"
  docker compose -f "${COMPOSE_FILE}" up -d --force-recreate workshop_spring_app
  echo "Rollback executed (alias :${IMAGE_TAG})"
fi

# Health check pos-rollback (parity with deploy.sh)
ATTEMPTS=30
SLEEP=5
for i in $(seq 1 $ATTEMPTS); do
  STATUS=$(docker inspect --format='{{json .State.Health.Status}}' workshop-api 2>/dev/null | tr -d '"')
  echo "[$i/$ATTEMPTS] workshop-api health: ${STATUS:-unknown}"
  if [ "$STATUS" = "healthy" ]; then
    echo "API is healthy after rollback"
    exit 0
  fi
  sleep $SLEEP
done

echo "API did not become healthy after rollback"
docker logs --tail=200 workshop-api || true
exit 1
