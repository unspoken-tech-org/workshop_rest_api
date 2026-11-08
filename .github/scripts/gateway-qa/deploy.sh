#!/bin/bash
set -eo pipefail
cd "${DEPLOY_DIR}"

# Cleanup secrets/credentials on exit (success or failure)
trap 'rm -rf "${DEPLOY_DIR}/.tmp"' EXIT

# Setup GHCR auth (config.json copied to secure .tmp/docker/ by composite action)
chmod 600 "${DEPLOY_DIR}/.tmp/docker/config.json" 2>/dev/null || true

# The tunnel token is now a file secret: drop the .env that held it.
rm -f .env

# Setup file secrets (Compose mounts them at /run/secrets/ in the gateway)
mkdir -p secrets
chmod 700 secrets
# cloudflared reads its token only at startup and Compose does not hash secret
# sources: a rotated token needs the tunnel recreated explicitly.
RECREATE_SERVICES=(caddy-gateway-qa)
if ! cmp -s "${DEPLOY_DIR}/.tmp/cloudflare_tunnel_token" secrets/cloudflare_tunnel_token; then
  RECREATE_SERVICES+=(cloudflared-qa)
fi
for secret_name in cf_api_token admin_email; do
  mv "${DEPLOY_DIR}/.tmp/${secret_name}" "secrets/${secret_name}"
  chmod 400 "secrets/${secret_name}"
done
# cloudflared runs as non-root and Compose bind-mounts the file with host
# permissions: it must be world-readable; the 700 directory keeps it private.
mv "${DEPLOY_DIR}/.tmp/cloudflare_tunnel_token" secrets/cloudflare_tunnel_token
chmod 444 secrets/cloudflare_tunnel_token

# Backup the CURRENT image before pulling the new one
if docker image inspect "${LOCAL_IMAGE_NAME}:latest" >/dev/null 2>&1; then
  docker tag "${LOCAL_IMAGE_NAME}:latest" "${LOCAL_IMAGE_NAME}:backup"
  echo "Backup created: ${LOCAL_IMAGE_NAME}:backup"
else
  echo "No previous image found, skipping backup"
fi

# Pull the new image from GHCR and retag to the local compose name
docker --config "${DEPLOY_DIR}/.tmp/docker" pull "${REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG}"
docker tag "${REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG}" "${LOCAL_IMAGE_NAME}:latest"

# Restart the service. --force-recreate como em PRD: o Caddyfile é montado por bind,
# então uma mudança só nele não troca a imagem e o compose manteria o container antigo.
docker compose -f "${COMPOSE_FILE}" up -d --force-recreate --remove-orphans "${RECREATE_SERVICES[@]}"
docker compose -f "${COMPOSE_FILE}" ps

# Health check (R2): Caddy and the tunnel must both report healthy. A container
# without a healthcheck never counts as ready.
health_status() {
  docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' "$1" 2>/dev/null || echo missing
}
ATTEMPTS=30
SLEEP=5
for i in $(seq 1 $ATTEMPTS); do
  CADDY_STATUS=$(health_status caddy-gateway-qa)
  TUNNEL_STATUS=$(health_status cloudflared-gateway-qa)
  echo "[$i/$ATTEMPTS] caddy-gateway-qa health: ${CADDY_STATUS}, cloudflared-gateway-qa health: ${TUNNEL_STATUS}"
  if [ "$CADDY_STATUS" = "none" ] || [ "$TUNNEL_STATUS" = "none" ]; then
    echo "A gateway container has no healthcheck: it cannot be verified (R2)" >&2
    break
  fi
  if [ "$CADDY_STATUS" = "healthy" ] && [ "$TUNNEL_STATUS" = "healthy" ]; then
    echo "Gateway QA is healthy"
    exit 0
  fi
  sleep $SLEEP
done
echo "Gateway QA did not become healthy"
docker logs --tail=200 caddy-gateway-qa || true
exit 1
