#!/usr/bin/env bash
# Gera, uma única vez, os valores locais e descartáveis usados pela stack de
# observabilidade (.secrets/ fica fora do Git). Não sobrescreve arquivos
# existentes e nunca imprime os valores.
#
# Uso, na raiz de workshop_rest_api:
#   platform/observability/local/scripts/init-secrets.sh
set -euo pipefail

SECRETS_DIR="${SECRETS_DIR:-platform/observability/local/.secrets}"
# Imagem usada só para gerar o hash bcrypt, sem rede.
HTPASSWD_IMAGE="httpd:2.4.65-alpine@sha256:07b2fabb7029a0b8aeb2e0fd02651c28fe22c21c5b5a59d6ff5b022791fcd89e"

random_value() {
	head -c 32 /dev/urandom | base64 | tr -d '/+=\n' | cut -c1-"$1"
}

write_once() {
	local file="$SECRETS_DIR/$1"
	if [[ -s "$file" ]]; then
		echo "mantido: $file"
		return
	fi
	printf '%s' "$2" > "$file"
	# Lido por containers com usuários não-root diferentes.
	chmod 644 "$file"
	echo "criado:  $file"
}

mkdir -p "$SECRETS_DIR"

write_once api-scrape-password "$(random_value 32)"

# A API guarda só o hash; a senha vai ao htpasswd por stdin.
if [[ ! -s "$SECRETS_DIR/api-scrape-password-hash" ]]; then
	hash="$(docker run --rm -i --network none "$HTPASSWD_IMAGE" \
		htpasswd -niB -C 10 metrics-scraper < "$SECRETS_DIR/api-scrape-password" | cut -d: -f2-)"
	write_once api-scrape-password-hash "$hash"
else
	echo "mantido: $SECRETS_DIR/api-scrape-password-hash"
fi

# Base sintética (R23): senha do PostgreSQL descartável e API key semeada.
write_once synthetic-db-password "$(random_value 32)"
write_once synthetic-api-key "sk_server_LOCAL_SYNTHETIC_$(random_value 30)"
