#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_NAME=deploy
# shellcheck source=common.sh
source "$(dirname -- "${BASH_SOURCE[0]}")/common.sh"

RELEASE_ID="${RELEASE_ID:?RELEASE_ID is required}"
PAYLOAD="${TMP_DIR}/payload.tar.gz"

validate_contract
validate_release_id "${RELEASE_ID}"
test -s "${PAYLOAD}" || die "missing payload: ${PAYLOAD}"

release_dir="${RELEASES_DIR}/${RELEASE_ID}"
[[ ! -e "${release_dir}" ]] || die "release already exists: ${RELEASE_ID}"

mkdir -p -- "${RELEASES_DIR}"
chmod 700 -- "${RELEASES_DIR}"

# Extract into a staging directory and rename it, so a release directory is
# either complete or absent.
staging_dir="${release_dir}.staging"
rm -rf -- "${staging_dir}"
mkdir -m 700 -- "${staging_dir}"
tar -xzf "${PAYLOAD}" -C "${staging_dir}" --no-same-owner
rm -f -- "${PAYLOAD}"
test -s "${staging_dir}/${COMPOSE_FILE}" || die "payload has no ${COMPOSE_FILE}"
test -s "${staging_dir}/.env" || die "payload has no .env"
chmod 600 -- "${staging_dir}/.env"
mv -T -- "${staging_dir}" "${release_dir}"

compose_in "${release_dir}" config --quiet

# The marker keeps the previous release until verify confirms the new one;
# rollback restores it whenever deploy or verify fails.
previous_dir="$(current_release_dir || true)"
umask 077
printf '%s\n' "${previous_dir}" > "${DEPLOY_MARKER}"

switch_current_to "${release_dir}"
echo "Activated control-plane release ${RELEASE_ID}"

compose_in "${release_dir}" up -d --remove-orphans
compose_in "${release_dir}" ps
wait_until_ready "${release_dir}"
