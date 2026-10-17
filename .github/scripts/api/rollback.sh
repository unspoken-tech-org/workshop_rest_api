#!/bin/bash
set -euo pipefail
DEPLOY_DIR="${DEPLOY_DIR:-/home/workshop/deploy/workshop_rest_api}"
cd "${DEPLOY_DIR}"

LOCAL_IMAGE_NAME="${LOCAL_IMAGE_NAME:-workshop_rest_api-workshop_spring_app}"
COMPOSE_FILE="${COMPOSE_FILE:-docker-compose-production.yml}"
OVERRIDE_FILE="${COMPOSE_FILE%.yml}.override.yml"
PREVIOUS_OVERRIDE="${OVERRIDE_FILE}.previous"
export IMAGE_TAG="${IMAGE_TAG:-latest}"

# deploy.sh writes its run id to this marker only when it swaps the API. A marker from
# another run (or no run id) is not ours: exit without touching it.
if [ ! -f .api-swapped ] || [ -z "${DEPLOY_RUN_ID:-}" ] || [ "$(cat .api-swapped)" != "${DEPLOY_RUN_ID}" ]; then
  echo "API not swapped by this run: nothing to roll back"
  exit 0
fi

# Without a backup nothing can be reverted: fail before changing anything
if ! docker image inspect "${LOCAL_IMAGE_NAME}:backup" >/dev/null 2>&1; then
  echo "API was swapped but no backup image exists: cannot roll back" >&2
  exit 1
fi

# Restore the compose file that runs the backup image: the snapshot paired with it
# (cp, not mv: the pair lives as long as the backup tag does). .previous is only the
# fallback for backups made before paired snapshots; with neither, refuse rather than
# pair the backup image with the new compose file (it needs the agent image).
BACKUP_SNAPSHOT="${COMPOSE_FILE}.backup"
if [ -f "${BACKUP_SNAPSHOT}" ]; then
  cp -p "${BACKUP_SNAPSHOT}" "${COMPOSE_FILE}"
  echo "Compose file restored from the snapshot paired with the backup"
elif [ -f "${COMPOSE_FILE}.previous" ]; then
  mv "${COMPOSE_FILE}.previous" "${COMPOSE_FILE}"
  echo "Compose file restored from .previous (backup without paired snapshot)"
else
  echo "No compose snapshot for the backup image: cannot roll back" >&2
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

# A workshop-db recreated by this compose run loses the workshop-db-metrics network
# that the edge deploy attaches (see deploy.sh); reattach it when the network exists.
if docker network inspect workshop-db-metrics >/dev/null 2>&1 && \
  [ -z "$(docker inspect --format '{{if index .NetworkSettings.Networks "workshop-db-metrics"}}yes{{end}}' workshop-db)" ]; then
  docker network connect workshop-db-metrics workshop-db || echo "WARNING: could not reattach workshop-db to workshop-db-metrics"
fi

# Health check pos-rollback (parity with deploy.sh)
ATTEMPTS=30
SLEEP=5
for i in $(seq 1 $ATTEMPTS); do
  STATUS=$(docker inspect --format='{{json .State.Health.Status}}' workshop-api 2>/dev/null | tr -d '"')
  echo "[$i/$ATTEMPTS] workshop-api health: ${STATUS:-unknown}"
  if [ "$STATUS" = "healthy" ]; then
    # The swap is reverted, so the marker and snapshot no longer apply
    rm -f .api-swapped "${COMPOSE_FILE}.previous"
    echo "API is healthy after rollback"
    exit 0
  fi
  sleep $SLEEP
done

rm -f .api-swapped "${COMPOSE_FILE}.previous"
echo "API did not become healthy after rollback"
docker logs --tail=200 workshop-api || true
exit 1
