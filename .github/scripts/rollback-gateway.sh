#!/bin/bash
set -euo pipefail
cd "${DEPLOY_DIR:-/home/workshop/deploy/gateway_stack}"

LOCAL_IMAGE_NAME="${LOCAL_IMAGE_NAME:-workshop/caddy-gateway}"
COMPOSE_FILE="${COMPOSE_FILE:-docker-compose-gateway.yml}"
OVERRIDE_FILE="${COMPOSE_FILE%.yml}.override.yml"
PREVIOUS_OVERRIDE="${OVERRIDE_FILE}.previous"

if ! docker image inspect "${LOCAL_IMAGE_NAME}:backup" >/dev/null 2>&1; then
  echo "No backup image found"
  exit 1
fi

docker tag "${LOCAL_IMAGE_NAME}:backup" "${LOCAL_IMAGE_NAME}:latest"

if [ -f "${PREVIOUS_OVERRIDE}" ]; then
  mv "${PREVIOUS_OVERRIDE}" "${OVERRIDE_FILE}"
  docker compose -f "${COMPOSE_FILE}" -f "${OVERRIDE_FILE}" up -d --force-recreate caddy-gateway
  echo "Rollback executed (override restored)"
else
  rm -f "${OVERRIDE_FILE}"
  docker compose -f "${COMPOSE_FILE}" up -d --force-recreate caddy-gateway
  echo "Rollback executed (alias :latest)"
fi
