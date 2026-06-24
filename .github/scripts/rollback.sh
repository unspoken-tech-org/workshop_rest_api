#!/bin/bash
set -e
cd /home/workshop/deploy/workshop_rest_api

LOCAL_IMAGE_NAME="${LOCAL_IMAGE_NAME:-workshop_rest_api-workshop_spring_app}"
COMPOSE_FILE="docker-compose-production.yml"
OVERRIDE_FILE="${COMPOSE_FILE%.yml}.override.yml"
PREVIOUS_OVERRIDE="${OVERRIDE_FILE}.previous"

if ! docker image inspect "${LOCAL_IMAGE_NAME}:backup" >/dev/null 2>&1; then
  echo "No backup image found"
  exit 1
fi

docker tag "${LOCAL_IMAGE_NAME}:backup" "${LOCAL_IMAGE_NAME}:latest"

if [ -f "${PREVIOUS_OVERRIDE}" ]; then
  mv "${PREVIOUS_OVERRIDE}" "${OVERRIDE_FILE}"
  docker compose -f "${COMPOSE_FILE}" -f "${OVERRIDE_FILE}" up -d --force-recreate workshop_spring_app
  echo "Rollback executed (override restored)"
else
  rm -f "${OVERRIDE_FILE}"
  docker compose -f "${COMPOSE_FILE}" up -d --force-recreate workshop_spring_app
  echo "Rollback executed (alias :latest)"
fi
