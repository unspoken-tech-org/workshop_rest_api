#!/bin/bash
set -e

echo ">>> Pruning dangling images..."
docker image prune -f

echo ">>> Docker disk usage:"
docker system df

echo "Cleanup done"
