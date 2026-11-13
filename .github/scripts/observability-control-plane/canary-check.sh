#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_NAME=canary-check
# shellcheck source=common.sh
source "$(dirname -- "${BASH_SOURCE[0]}")/common.sh"

# Recupera no release ativo o canário enviado por outro ambiente (o verify da
# borda da PRD), sem alterar nada na QA.
CANARY_ID="${CANARY_ID:?CANARY_ID is required}"

validate_contract
release_dir="$(current_release_dir)" || die "no active release"
echo "Looking for canary ${CANARY_ID} in the active control-plane release"
canary_query "${release_dir}" "${CANARY_ID}"
