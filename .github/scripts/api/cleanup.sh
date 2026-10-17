#!/bin/bash
set -e

# Same name deploy-api-prod.yml passes to deploy.sh and rollback.sh (not passed to this script)
LOCAL_IMAGE_NAME="${LOCAL_IMAGE_NAME:-workshop_rest_api-workshop_spring_app}"
PRUNE_AGE_HOURS="${PRUNE_AGE_HOURS:-168}"

echo ">>> Pruning dangling images..."
timeout 60 docker image prune -f

# Rollback images (:latest, *backup) and the PostgreSQL image, matched by ID: several tags can point to one image
PROTECTED_IDS=$(docker image ls --no-trunc --format '{{.ID}}' \
  --filter "reference=${LOCAL_IMAGE_NAME}:latest" \
  --filter "reference=${LOCAL_IMAGE_NAME}:*backup" \
  --filter "reference=workshop/postgres16-pgbackrest:latest")

# Images used by any container, running or stopped: removing one of its tags would break compose up and rollback
CONTAINER_IDS=$(docker ps -aq)
if [[ -n "${CONTAINER_IDS}" ]]; then
  PROTECTED_IDS+=$'\n'"$(xargs docker inspect --format '{{.Image}}' <<< "${CONTAINER_IDS}" 2>/dev/null || true)"
fi

echo ">>> Pruning unused tagged images older than ${PRUNE_AGE_HOURS}h..."
# Daemons without the 'until' image filter reject it: skip the step instead of listing every image
if ! docker image ls --no-trunc --filter "until=${PRUNE_AGE_HOURS}h" >/dev/null 2>&1; then
  echo "Skipped unused image pruning: 'until' image filter rejected by this Docker daemon"
else
  docker image ls --no-trunc --filter "until=${PRUNE_AGE_HOURS}h" --format '{{.ID}} {{.Repository}}:{{.Tag}}' \
    | while read -r image_id image_ref; do
        if [[ "${image_ref}" == *"<none>"* ]] || grep -qxF "${image_id}" <<< "${PROTECTED_IDS}"; then
          continue
        fi
        timeout 60 docker image rm "${image_ref}" >/dev/null || echo "Skipped ${image_ref} (in use or not removable)"
      done
fi



echo ">>> Docker disk usage:"
docker system df

echo "Cleanup done"
