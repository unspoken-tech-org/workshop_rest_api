#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_NAME=deploy
# shellcheck source=common.sh
source "$(dirname -- "${BASH_SOURCE[0]}")/common.sh"

RELEASE_ID="${RELEASE_ID:?RELEASE_ID is required}"
PAYLOAD="${TMP_DIR}/payload.tar.gz"

# Checks that run before the active release is switched. The external network
# must exist (the PRD API Compose creates it), and the free disk must cover the
# queue budget: one queue per signal, 3 x COLLECTOR_QUEUE_BYTES.
preflight() {
  local env_file="$1"
  local queue_bytes budget docker_root free_bytes

  docker network inspect "${METRICS_NETWORK}" >/dev/null 2>&1 || \
    die "external network ${METRICS_NETWORK} does not exist; deploy the PRD API Compose first, it creates the network"

  queue_bytes="$(sed -n 's/^COLLECTOR_QUEUE_BYTES=//p' "${env_file}")"
  [[ "${queue_bytes}" =~ ^[1-9][0-9]*$ ]] || die "COLLECTOR_QUEUE_BYTES in .env must be a positive integer"
  budget=$((3 * queue_bytes))

  # The queue volume lives under the Docker root directory, so that disk is measured.
  docker_root="$(docker info --format '{{.DockerRootDir}}' 2>/dev/null || true)"
  [[ -n "${docker_root}" ]] || docker_root="${DEPLOY_DIR}"
  free_bytes="$(df -PB1 -- "${docker_root}" | awk 'NR == 2 {print $4}')"
  echo "Free disk at ${docker_root}: ${free_bytes} bytes; queue budget (3 x COLLECTOR_QUEUE_BYTES): ${budget} bytes"
  (( free_bytes >= budget )) || die "free disk is below the queue budget"
}

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

# Secret modes. The scrape password is bind-mounted into a container that runs
# as UID 10001 and cannot be chowned without root, so the file is readable by
# others while its directory stays 700: only this SSH user can reach it on the
# host. The QA endpoint is read only by this script (600).
chmod 700 -- "${staging_dir}/.secrets"
test -s "${staging_dir}/.secrets/api-scrape-password" || die "payload has no api-scrape-password secret file"
test -s "${staging_dir}/.secrets/qa-otlp-endpoint" || die "payload has no qa-otlp-endpoint secret file"
chmod 444 -- "${staging_dir}/.secrets/api-scrape-password"
chmod 600 -- "${staging_dir}/.secrets/qa-otlp-endpoint"

preflight "${staging_dir}/.env"

mv -T -- "${staging_dir}" "${release_dir}"

compose_in "${release_dir}" config --quiet

# The marker keeps the previous release until verify confirms the new one;
# rollback restores it whenever deploy or verify fails.
previous_dir="$(current_release_dir || true)"
umask 077
printf '%s\n' "${previous_dir}" > "${DEPLOY_MARKER}"

switch_current_to "${release_dir}"
echo "Activated edge release ${RELEASE_ID}"

# Compose does not hash secret sources, so a kept container would still read
# the previous release's secrets (or none, after a failed creation). The queue
# volume is not recreated: buffered data survives the switch.
compose_in "${release_dir}" up -d --remove-orphans --force-recreate
compose_in "${release_dir}" ps
wait_until_ready "${release_dir}"
