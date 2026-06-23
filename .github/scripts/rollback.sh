#!/bin/bash
set -e
cd /home/workshop/deploy/workshop_rest_api

LOCAL_IMAGE_NAME="${LOCAL_IMAGE_NAME:-workshop_rest_api-workshop_spring_app}"

if docker image inspect "${LOCAL_IMAGE_NAME}:backup" >/dev/null 2>&1; then
  docker tag "${LOCAL_IMAGE_NAME}:backup" "${LOCAL_IMAGE_NAME}:latest"
  docker compose -f docker-compose-production.yml up -d --force-recreate workshop_spring_app
  echo "Rollback executed"
else
  echo "No backup image found"
  exit 1
fi
