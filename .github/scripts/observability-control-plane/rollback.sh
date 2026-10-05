#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_NAME=rollback
# shellcheck source=common.sh
source "$(dirname -- "${BASH_SOURCE[0]}")/common.sh"

RELEASE_ID="${RELEASE_ID:?RELEASE_ID is required}"

validate_contract
validate_release_id "${RELEASE_ID}"

failed_dir="${RELEASES_DIR}/${RELEASE_ID}"
rm -rf -- "${failed_dir}.staging" "${TMP_DIR}/payload.tar.gz"

# Without the marker the failed run never switched releases: the running
# project was not touched and must stay as it is.
if [[ ! -f "${DEPLOY_MARKER}" ]]; then
  echo "No release switch was recorded; leaving the control plane untouched"
  if [[ "$(current_release_dir || true)" != "${failed_dir}" ]]; then
    rm -rf -- "${failed_dir}"
  fi
  exit 0
fi

previous_dir="$(cat -- "${DEPLOY_MARKER}")"

if [[ -n "${previous_dir}" && -d "${previous_dir}" ]]; then
  [[ "${previous_dir}" == "${RELEASES_DIR}/"* ]] || die "marker points outside the releases directory"
  echo "Restoring control-plane release $(basename -- "${previous_dir}")"
  switch_current_to "${previous_dir}"
  # Same project, previous files: Compose recreates only what differs.
  compose_in "${previous_dir}" up -d --remove-orphans
  compose_in "${previous_dir}" ps
  rm -rf -- "${failed_dir}"
  rm -f -- "${DEPLOY_MARKER}"
  wait_until_ready "${previous_dir}"
else
  # First deployment: there is no known-good release. Keep the failed one
  # running for diagnosis instead of tearing the project down.
  rm -f -- "${DEPLOY_MARKER}"
  echo "No previous control-plane release exists; the failed first release stays for diagnosis" >&2
fi
