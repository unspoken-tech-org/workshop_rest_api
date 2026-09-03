#!/bin/bash
set -eo pipefail
cd "${DEPLOY_DIR:-/home/workshop/deploy/observability_stack}"

echo "=== Cleanup Observability Prod ==="
rm -rf "${DEPLOY_DIR}/.tmp"
docker image prune -f || true
echo "Cleanup completed"
