#!/bin/bash
set -e
cd "${DEPLOY_DIR}"

# Login GHCR
echo "${GH_TOKEN}" | docker login ghcr.io -u "${GH_USER}" --password-stdin

# Pull da nova imagem do GHCR e retag para nome local do compose
docker pull "${REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG}"
docker tag "${REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG}" "${API_IMAGE}:latest"

# Backup da imagem atual
if docker image inspect "${API_IMAGE}:latest" >/dev/null 2>&1; then
  docker tag "${API_IMAGE}:latest" "${API_IMAGE}:backup"
  echo "Backup created: ${API_IMAGE}:backup"
else
  echo "No previous image found, skipping backup"
fi

# Restart do servico
docker compose -f "${COMPOSE_FILE}" up -d --remove-orphans workshop_spring_app
docker compose -f "${COMPOSE_FILE}" ps

# Health check
ATTEMPTS=90
SLEEP=5
for i in $(seq 1 $ATTEMPTS); do
  STATUS=$(docker inspect --format='{{json .State.Health.Status}}' workshop-api 2>/dev/null | tr -d '"')
  echo "[$i/$ATTEMPTS] workshop-api health: ${STATUS:-unknown}"
  if [ "$STATUS" = "healthy" ]; then
    echo "API is healthy"
    exit 0
  fi
  sleep $SLEEP
done
echo "API did not become healthy"
docker logs --tail=200 workshop-api || true
exit 1
