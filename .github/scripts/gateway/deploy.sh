#!/bin/bash
set -euo pipefail
cd "${DEPLOY_DIR}"

# Cleanup secrets/credentials on exit (success or failure)
trap 'rm -f /tmp/.env /tmp/cf_api_token /tmp/admin_email /tmp/cloudflare_tunnel_token ~/.docker/config.json' EXIT

# Setup GHCR auth (config.json copied by the deploy-via-ssh-gateway composite)
mkdir -p ~/.docker
if [ -f ~/.docker/config.json.tmp ]; then
  mv ~/.docker/config.json.tmp ~/.docker/config.json
  chmod 600 ~/.docker/config.json
fi

# Setup .env (moved from the composite)
if [ -f /tmp/.env ]; then
  mv /tmp/.env ./.env
  chmod 600 .env
fi

# Setup file secrets (Compose mounts them at /run/secrets/ in the gateway)
mkdir -p secrets
chmod 700 secrets
# cloudflared reads its token only at startup and Compose does not hash secret
# sources: a rotated token needs the tunnel recreated explicitly.
RECREATE_SERVICES=(caddy-gateway)
if ! cmp -s /tmp/cloudflare_tunnel_token secrets/cloudflare_tunnel_token; then
  RECREATE_SERVICES+=(cloudflared-prod)
fi
for secret_name in cf_api_token admin_email; do
  mv "/tmp/${secret_name}" "secrets/${secret_name}"
  chmod 400 "secrets/${secret_name}"
done
# cloudflared runs as non-root and Compose bind-mounts the file with host
# permissions: it must be world-readable; the 700 directory keeps it private.
mv /tmp/cloudflare_tunnel_token secrets/cloudflare_tunnel_token
chmod 444 secrets/cloudflare_tunnel_token

LOCAL_IMAGE_NAME="${LOCAL_IMAGE_NAME:-workshop/caddy-gateway}"
COMPOSE_FILE="${COMPOSE_FILE:-docker-compose-gateway.yml}"
OVERRIDE_FILE="${COMPOSE_FILE%.yml}.override.yml"
PREVIOUS_OVERRIDE="${OVERRIDE_FILE}.previous"

# Backup the CURRENT image before pulling the new one
# (rollback.sh restores :backup, ensuring a safe fallback)
OLD_DIGEST=""
if docker image inspect "${LOCAL_IMAGE_NAME}:latest" >/dev/null 2>&1; then
  OLD_DIGEST=$(docker inspect --format='{{index .RepoDigests 0}}' "${LOCAL_IMAGE_NAME}:latest" 2>/dev/null || echo "")
  docker tag "${LOCAL_IMAGE_NAME}:latest" "${LOCAL_IMAGE_NAME}:backup"
  echo "Backup created: ${LOCAL_IMAGE_NAME}:backup"
else
  echo "No previous image found, skipping backup"
fi

# Pull the new image from GHCR
docker pull "${REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG}"

# Retag GHCR -> local compose name
docker tag "${REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG}" "${LOCAL_IMAGE_NAME}:latest"

# Override file: expose the real tag in docker ps
# Snapshot the current override before overwriting (rollback-gateway.sh restores the snapshot)
if [ -f "${OVERRIDE_FILE}" ]; then
  cp "${OVERRIDE_FILE}" "${PREVIOUS_OVERRIDE}"
fi

cat > "${OVERRIDE_FILE}" <<EOF
services:
  caddy-gateway:
    image: ${REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG}
EOF

# Restart the service
docker compose -f "${COMPOSE_FILE}" -f "${OVERRIDE_FILE}" up -d --force-recreate "${RECREATE_SERVICES[@]}"
docker compose -f "${COMPOSE_FILE}" -f "${OVERRIDE_FILE}" ps

# Health check via Docker healthcheck (R2): Caddy (Admin API, does not depend on
# backends) and the tunnel (/ready, needs an edge connection) must both report
# healthy. A container without a healthcheck never counts as ready.
health_status() {
  docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' "$1" 2>/dev/null || echo missing
}
ATTEMPTS=30
SLEEP=5
for i in $(seq 1 $ATTEMPTS); do
  CADDY_STATUS=$(health_status caddy-gateway)
  TUNNEL_STATUS=$(health_status cloudflared-gateway-prod)
  echo "[$i/$ATTEMPTS] caddy-gateway health: ${CADDY_STATUS}, cloudflared-gateway-prod health: ${TUNNEL_STATUS}"
  if [ "$CADDY_STATUS" = "none" ] || [ "$TUNNEL_STATUS" = "none" ]; then
    echo "A gateway container has no healthcheck: it cannot be verified (R2)" >&2
    break
  fi
  if [ "$CADDY_STATUS" = "healthy" ] && [ "$TUNNEL_STATUS" = "healthy" ]; then
    echo "Caddy and tunnel are healthy"
    exit 0
  fi
  sleep $SLEEP
done

echo "Gateway did not become healthy"
docker logs --tail=200 caddy-gateway || true
exit 1
