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
  echo "No release switch was recorded; leaving the edge collector untouched"
  if [[ "$(current_release_dir || true)" != "${failed_dir}" ]]; then
    rm -rf -- "${failed_dir}"
  fi
  exit 0
fi

previous_dir="$(cat -- "${DEPLOY_MARKER}")"

# Empty marker: the first deployment had no known-good release. Keep the failed
# one running for diagnosis instead of tearing the project down.
if [[ -z "${previous_dir}" ]]; then
  rm -f -- "${DEPLOY_MARKER}"
  echo "No previous edge release exists; the failed first release stays for diagnosis" >&2
  exit 0
fi

# A named previous release must be intact. If it is gone or lacks a file needed
# to run, that is a real failure: stop before switching, so the failed release
# keeps running and nothing is restored half-way.
[[ "${previous_dir}" == "${RELEASES_DIR}/"* ]] || die "marker points outside the releases directory"
[[ -d "${previous_dir}" ]] || die "previous edge release is missing: $(basename -- "${previous_dir}")"
for required in .env "${COMPOSE_FILE}" .secrets/qa-otlp-endpoint .secrets/api-scrape-password; do
  test -s "${previous_dir}/${required}" || die "previous edge release has no ${required}; it cannot be restored"
done

echo "Restoring edge release $(basename -- "${previous_dir}")"
# The failed deploy may have rotated the monitoring role password; the restored
# release brings its own. A release from before the postgres-exporter has none.
if [[ -s "${previous_dir}/.secrets/pg-monitor-password" ]]; then
  pg_monitor_role "${previous_dir}/.secrets/pg-monitor-password"
fi
switch_current_to "${previous_dir}"
# Same project, previous files; recreate everything so secrets are
# remounted from the restored release (Compose does not hash them).
compose_in "${previous_dir}" up -d --remove-orphans --force-recreate
compose_in "${previous_dir}" ps
rm -rf -- "${failed_dir}"
rm -f -- "${DEPLOY_MARKER}"
wait_until_ready "${previous_dir}"
