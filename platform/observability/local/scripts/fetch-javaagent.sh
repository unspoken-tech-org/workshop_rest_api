#!/usr/bin/env bash
# Baixa, uma única vez, o OpenTelemetry Java Agent usado pelo perfil `api`
# e confere o checksum fixado (.javaagent/ fica fora do Git). Um arquivo
# existente com checksum divergente é recusado, nunca usado.
#
# Uso, na raiz de workshop_rest_api:
#   platform/observability/local/scripts/fetch-javaagent.sh
set -euo pipefail

AGENT_DIR="${AGENT_DIR:-platform/observability/local/.javaagent}"
AGENT_VERSION="2.32.0"
# Conferido no digest do asset da release no GitHub e no .sha256 do Maven Central.
AGENT_SHA256="f787eb6c7f3d18e69a431e108a15278d25ee37f83d68b678f621e063f3988f82"
AGENT_URL="https://repo1.maven.org/maven2/io/opentelemetry/javaagent/opentelemetry-javaagent/${AGENT_VERSION}/opentelemetry-javaagent-${AGENT_VERSION}.jar"
AGENT_JAR="$AGENT_DIR/opentelemetry-javaagent.jar"

verify() {
	echo "$AGENT_SHA256  $1" | sha256sum -c --quiet -
}

mkdir -p "$AGENT_DIR"

if [[ -s "$AGENT_JAR" ]]; then
	verify "$AGENT_JAR" || { echo "checksum divergente: $AGENT_JAR" >&2; exit 1; }
	echo "mantido: $AGENT_JAR ($AGENT_VERSION)"
	exit 0
fi

tmp="$(mktemp "$AGENT_DIR/.download.XXXXXX")"
trap 'rm -f "$tmp"' EXIT
curl -fsSL --proto '=https' --tlsv1.2 -o "$tmp" "$AGENT_URL"
verify "$tmp"
# Lido pelo usuário não-root do container da API.
chmod 644 "$tmp"
mv "$tmp" "$AGENT_JAR"
echo "criado:  $AGENT_JAR ($AGENT_VERSION)"
