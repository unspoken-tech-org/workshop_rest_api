#!/bin/bash
set -e

echo ">>> Pruning dangling images..."
docker image prune -f

echo ">>> Pruning tagged images older than 7 days..."
docker image prune -a -f --filter "until=168h"

echo ">>> Removing GHCR credentials from server..."
rm -f ~/.docker/config.json

echo ">>> Docker disk usage:"
docker system df

echo "Cleanup done"
