#!/bin/bash
set -euo pipefail
cd "${DEPLOY_DIR}"

LOCAL_IMAGE_NAME="${LOCAL_IMAGE_NAME:-workshop_rest_api_qa}"
COMPOSE_FILE="${COMPOSE_FILE:-docker-compose-qa.yml}"
IMAGE_TAG="${IMAGE_TAG:-latest}"

# deploy.sh writes its run id to this marker only when it swaps the API. A marker from
# another run (or no run id) is not ours: exit without touching it.
if [ ! -f .api-swapped ] || [ -z "${DEPLOY_RUN_ID:-}" ] || [ "$(cat .api-swapped)" != "${DEPLOY_RUN_ID}" ]; then
  echo "API not swapped by this run: nothing to roll back"
  exit 0
fi

# Without a backup nothing can be reverted: fail before changing anything
if ! docker image inspect "${LOCAL_IMAGE_NAME}:${IMAGE_TAG}-backup" >/dev/null 2>&1; then
  echo "API was swapped but no backup image exists: cannot roll back" >&2
  exit 1
fi

# Restore the compose file deploy.sh replaced
if [ -f "${COMPOSE_FILE}.previous" ]; then
  mv "${COMPOSE_FILE}.previous" "${COMPOSE_FILE}"
  echo "Compose file restored"
fi

docker tag "${LOCAL_IMAGE_NAME}:${IMAGE_TAG}-backup" "${LOCAL_IMAGE_NAME}:${IMAGE_TAG}"
docker compose -f "${COMPOSE_FILE}" up -d --force-recreate workshop_spring_app_qa
echo "Rollback executed (alias :${IMAGE_TAG})"

# Health check post-rollback (parity with deploy.sh)
ATTEMPTS=30
SLEEP=5
for i in $(seq 1 $ATTEMPTS); do
  STATUS=$(docker inspect --format='{{json .State.Health.Status}}' workshop-api-qa 2>/dev/null | tr -d '"')
  echo "[$i/$ATTEMPTS] workshop-api-qa health: ${STATUS:-unknown}"
  if [ "$STATUS" = "healthy" ]; then
    # The swap is reverted, so the marker and snapshot no longer apply
    rm -f .api-swapped "${COMPOSE_FILE}.previous"
    echo "QA API is healthy after rollback"
    exit 0
  fi
  sleep $SLEEP
done

rm -f .api-swapped "${COMPOSE_FILE}.previous"
echo "QA API did not become healthy after rollback"
docker logs --tail=200 workshop-api-qa || true
exit 1
