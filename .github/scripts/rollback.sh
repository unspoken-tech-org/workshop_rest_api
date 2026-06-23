#!/bin/bash
set -e
cd /home/workshop/deploy/workshop_rest_api

API_IMAGE="workshop_rest_api-workshop_spring_app"

if docker image inspect "${API_IMAGE}:backup" >/dev/null 2>&1; then
  docker tag "${API_IMAGE}:backup" "${API_IMAGE}:latest"
  docker compose -f docker-compose-production.yml up -d --force-recreate workshop_spring_app
  echo "Rollback executed"
else
  echo "No backup image found"
  exit 1
fi
