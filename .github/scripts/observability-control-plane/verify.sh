#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_NAME=verify
# shellcheck source=common.sh
source "$(dirname -- "${BASH_SOURCE[0]}")/common.sh"

RELEASE_ID="${RELEASE_ID:?RELEASE_ID is required}"

validate_contract
validate_release_id "${RELEASE_ID}"

release_dir="$(current_release_dir)" || die "no active release"
[[ "${release_dir}" == "${RELEASES_DIR}/${RELEASE_ID}" ]] || \
  die "active release is not ${RELEASE_ID}"

compose_in "${release_dir}" config --quiet
compose_in "${release_dir}" ps
if ! all_services_ready "${release_dir}"; then
  echo "control-plane service is not ready: ${NOT_READY_SERVICE}" >&2
  compose_in "${release_dir}" logs --tail=200 "${NOT_READY_SERVICE}" || true
  exit 1
fi

echo "Observability control plane verification passed"
