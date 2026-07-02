#!/bin/bash
set -euo pipefail
cd "${DEPLOY_DIR}"

LOCAL_IMAGE_NAME="${LOCAL_IMAGE_NAME:-workshop_rest_api-workshop_spring_app_qa}"
COMPOSE_FILE="${COMPOSE_FILE:-docker-compose-qa.yml}"

if ! docker image inspect "${LOCAL_IMAGE_NAME}:backup" >/dev/null 2>&1; then
  echo "No backup image found"
  exit 1
fi

docker tag "${LOCAL_IMAGE_NAME}:backup" "${LOCAL_IMAGE_NAME}:latest"
docker compose -f "${COMPOSE_FILE}" up -d --force-recreate workshop_spring_app_qa
echo "Rollback executed (alias :latest)"

# Health check post-rollback (parity with deploy.sh)
ATTEMPTS=30
SLEEP=5
for i in $(seq 1 $ATTEMPTS); do
  STATUS=$(docker inspect --format='{{json .State.Health.Status}}' workshop-api-qa 2>/dev/null | tr -d '"')
  echo "[$i/$ATTEMPTS] workshop-api-qa health: ${STATUS:-unknown}"
  if [ "$STATUS" = "healthy" ]; then
    echo "QA API is healthy after rollback"
    exit 0
  fi
  sleep $SLEEP
done

echo "QA API did not become healthy after rollback"
docker logs --tail=200 workshop-api-qa || true
exit 1
