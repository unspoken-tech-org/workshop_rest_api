#!/bin/bash
set -euo pipefail

echo ">>> Pruning dangling images..."
docker image prune -f

echo ">>> Removing GHCR credentials from server..."
rm -f ~/.docker/config.json

echo ">>> Docker disk usage:"
docker system df

echo "Cleanup done"
