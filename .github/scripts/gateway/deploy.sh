#!/bin/bash
set -euo pipefail
cd "${DEPLOY_DIR}"

# Cleanup secrets/credentials ao sair (sucesso ou falha)
# Padrao v1.24 do deploy.sh (Spring)
trap 'rm -f /tmp/.env ~/.docker/config.json' EXIT

# Setup GHCR auth (config.json copiado pelo composite deploy-via-ssh-gateway)
mkdir -p ~/.docker
if [ -f ~/.docker/config.json.tmp ]; then
  mv ~/.docker/config.json.tmp ~/.docker/config.json
  chmod 600 ~/.docker/config.json
fi

# Setup .env (movido do composite; padrao v1.21 do Spring)
if [ -f /tmp/.env ]; then
  mv /tmp/.env ./.env
  chmod 600 .env
fi

LOCAL_IMAGE_NAME="${LOCAL_IMAGE_NAME:-workshop/caddy-gateway}"
COMPOSE_FILE="${COMPOSE_FILE:-docker-compose-gateway.yml}"
OVERRIDE_FILE="${COMPOSE_FILE%.yml}.override.yml"
PREVIOUS_OVERRIDE="${OVERRIDE_FILE}.previous"

# Pull da nova imagem do GHCR
docker pull "${REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG}"

# Retag GHCR -> nome local do compose
docker tag "${REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG}" "${LOCAL_IMAGE_NAME}:latest"

# Backup da imagem ANTERIOR em producao (padrao v1.36 do Spring)
# Backup ANTES do retag garante que :backup aponta para a imagem anterior
# (caso a nova imagem falhe o health check, rollback restaura a versao saudavel)
if docker image inspect "${LOCAL_IMAGE_NAME}:latest" >/dev/null 2>&1; then
  # Verifica se a tag :latest atual e diferente da nova (evita sobrescrever :backup se a imagem ja existe)
  if [ "$(docker inspect --format='{{index .RepoDigests 0}}' "${LOCAL_IMAGE_NAME}:latest" 2>/dev/null || echo "")" != "${REGISTRY}/${IMAGE_NAME}@$(docker inspect --format='{{index .RepoDigests 0}}' "${REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG}" 2>/dev/null | cut -d'@' -f2)" ]; then
    docker tag "${LOCAL_IMAGE_NAME}:latest" "${LOCAL_IMAGE_NAME}:backup"
    echo "Backup created: ${LOCAL_IMAGE_NAME}:backup"
  else
    echo "Image already up-to-date, skipping backup"
  fi
fi

# Override file: expor a tag real no docker ps (padrao v1.39 do Spring)
# Snapshot do override atual antes de sobrescrever (rollback-gateway.sh restaura o snapshot)
if [ -f "${OVERRIDE_FILE}" ]; then
  cp "${OVERRIDE_FILE}" "${PREVIOUS_OVERRIDE}"
fi

cat > "${OVERRIDE_FILE}" <<EOF
services:
  caddy-gateway:
    image: ${REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG}
EOF

# Restart do servico
docker compose -f "${COMPOSE_FILE}" -f "${OVERRIDE_FILE}" up -d --force-recreate caddy-gateway
docker compose -f "${COMPOSE_FILE}" -f "${OVERRIDE_FILE}" ps

# Health check via Docker healthcheck (Adicionado no docker-compose-gateway.yml na Onda 0)
# Usa a Admin API do Caddy em :2019/health (nao depende dos backends)
ATTEMPTS=30
SLEEP=5
for i in $(seq 1 $ATTEMPTS); do
  STATUS=$(docker inspect --format='{{json .State.Health.Status}}' caddy-gateway 2>/dev/null | tr -d '"')
  echo "[$i/$ATTEMPTS] caddy-gateway health: ${STATUS:-unknown}"
  if [ "$STATUS" = "healthy" ]; then
    echo "Caddy is healthy"
    exit 0
  fi
  sleep $SLEEP
done

echo "Caddy did not become healthy"
docker logs --tail=200 caddy-gateway || true
exit 1
