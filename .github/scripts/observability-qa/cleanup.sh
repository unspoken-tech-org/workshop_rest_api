#!/bin/bash
set -eo pipefail

echo ">>> Pruning dangling images..."
timeout 60 docker image prune -f

echo ">>> Pruning tagged images older than 7 days..."
timeout 60 docker image prune -a -f --filter "until=168h"

echo ">>> Docker disk usage:"
docker system df

echo "Cleanup done"
