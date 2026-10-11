#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_NAME=cleanup
# shellcheck source=common.sh
source "$(dirname -- "${BASH_SOURCE[0]}")/common.sh"

validate_contract

# Called only after verify passed: the active release is confirmed.
rm -f -- "${DEPLOY_MARKER}" "${TMP_DIR}/payload.tar.gz"

# Keep the active release and the most recent ones for rollback; older
# releases are removed together with their .env.
active_dir="$(current_release_dir || true)"
if [[ -d "${RELEASES_DIR}" ]]; then
  mapfile -t releases < <(
    find "${RELEASES_DIR}" -mindepth 1 -maxdepth 1 -type d ! -name '*.staging' -printf '%T@ %p\n' \
      | sort -rn | cut -d' ' -f2-
  )
  kept=0
  for release in "${releases[@]}"; do
    if [[ "${release}" == "${active_dir}" ]] || ((kept < RELEASES_TO_KEEP - 1)); then
      [[ "${release}" == "${active_dir}" ]] || kept=$((kept + 1))
      continue
    fi
    rm -rf -- "${release}"
    echo "Removed old control-plane release $(basename -- "${release}")"
  done
fi

echo "Observability control-plane deployment confirmed"
