#!/usr/bin/env bash
# apply-docker-daemon-config.sh
# Mescla as chaves gerenciadas de platform/host/docker/daemon.json no
# /etc/docker/daemon.json do host, preservando as chaves não gerenciadas.
#
# Uso:
#   scripts/host/apply-docker-daemon-config.sh                  # dry-run (padrão)
#   sudo scripts/host/apply-docker-daemon-config.sh --apply     # grava (com backup)
#   sudo scripts/host/apply-docker-daemon-config.sh --rollback [ARQUIVO_BACKUP]
#
# NÃO reinicia o Docker. Runbook: platform/host/docker/runbook-default-address-pools.md

set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MANAGED_FILE="${MANAGED_FILE:-$SCRIPT_DIR/../../platform/host/docker/daemon.json}"
TARGET_FILE="${TARGET_FILE:-/etc/docker/daemon.json}"

MODE="dry-run"
ROLLBACK_SRC=""

usage() {
  sed -n '2,11p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'
  exit "${1:-0}"
}

while [ $# -gt 0 ]; do
  case "$1" in
    --dry-run) MODE="dry-run" ;;
    --apply) MODE="apply" ;;
    --rollback)
      MODE="rollback"
      if [ $# -gt 1 ] && [[ "$2" != --* ]]; then
        ROLLBACK_SRC="$2"
        shift
      fi
      ;;
    -h | --help) usage 0 ;;
    *)
      echo "Opção desconhecida: $1" >&2
      usage 1
      ;;
  esac
  shift
done

log() { echo "[docker-daemon] $*"; }
die() {
  echo "[docker-daemon] ERRO: $*" >&2
  exit 1
}

validate_json() {
  local file="$1"
  jq -e . "$file" >/dev/null || die "JSON inválido: $file"
  if command -v dockerd >/dev/null 2>&1; then
    local out
    out="$(dockerd --validate --config-file "$file" 2>&1)" ||
      die "dockerd --validate rejeitou $file: $out"
    log "dockerd --validate: configuração OK ($file)"
  else
    log "AVISO: dockerd não encontrado; só o JSON foi validado."
  fi
}

print_restart_warning() {
  cat <<'MSG'

IMPORTANTE: nada foi reiniciado. Para a configuração valer:
  sudo systemctl restart docker
ATENÇÃO: o restart derruba TODOS os containers do host (live-restore ainda
está desligado no daemon em execução). Faça só na janela da release, conforme
platform/host/docker/runbook-default-address-pools.md.
MSG
}

command -v jq >/dev/null 2>&1 || die "jq é necessário (apt install jq)."

if [ "$MODE" = "rollback" ]; then
  if [ -z "$ROLLBACK_SRC" ]; then
    # shellcheck disable=SC2012
    ROLLBACK_SRC="$(ls -1t "$TARGET_FILE".bak-* 2>/dev/null | head -n1 || true)"
  fi
  [ -n "$ROLLBACK_SRC" ] && [ -f "$ROLLBACK_SRC" ] || die "nenhum backup encontrado (${TARGET_FILE}.bak-*)."
  [ "$(id -u)" -eq 0 ] || die "rollback exige root (use sudo)."
  validate_json "$ROLLBACK_SRC"
  log "Restaurando $ROLLBACK_SRC -> $TARGET_FILE"
  cp -p "$ROLLBACK_SRC" "$TARGET_FILE"
  log "Restaurado. Reinicie o Docker (mesmo aviso de impacto) para voltar à configuração anterior."
  print_restart_warning
  exit 0
fi

[ -f "$MANAGED_FILE" ] || die "arquivo gerenciado não encontrado: $MANAGED_FILE"
validate_json "$MANAGED_FILE"

CURRENT_TMP="$(mktemp)"
MERGED_TMP="$(mktemp)"
trap 'rm -f "$CURRENT_TMP" "$MERGED_TMP"' EXIT

if [ -f "$TARGET_FILE" ]; then
  jq -S . "$TARGET_FILE" >"$CURRENT_TMP" || die "JSON atual inválido: $TARGET_FILE"
else
  echo '{}' >"$CURRENT_TMP"
fi

# Chaves gerenciadas substituem as atuais (objetos mesclados em profundidade,
# arrays como default-address-pools substituídos); as demais são preservadas.
jq -S -s '.[0] * .[1]' "$CURRENT_TMP" "$MANAGED_FILE" >"$MERGED_TMP"
validate_json "$MERGED_TMP"

if cmp -s "$CURRENT_TMP" "$MERGED_TMP"; then
  log "$TARGET_FILE já contém as chaves gerenciadas. Nada a fazer."
  exit 0
fi

log "Diff (atual -> proposto):"
diff -u --label "$TARGET_FILE" --label "proposto" "$CURRENT_TMP" "$MERGED_TMP" || true

if [ "$MODE" = "dry-run" ]; then
  log "Dry-run: nada foi gravado. Use --apply para gravar."
  print_restart_warning
  exit 0
fi

[ "$(id -u)" -eq 0 ] || die "--apply exige root (use sudo)."

if [ -f "$TARGET_FILE" ]; then
  BACKUP="${TARGET_FILE}.bak-$(date +%Y%m%d-%H%M%S)"
  cp -p "$TARGET_FILE" "$BACKUP"
  log "Backup: $BACKUP"
fi

install -m 0644 -o root -g root "$MERGED_TMP" "$TARGET_FILE"
log "Gravado: $TARGET_FILE"
log "Rollback: sudo $0 --rollback ${BACKUP:-}"
print_restart_warning
