#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_NAME=deploy
# shellcheck source=common.sh
source "$(dirname -- "${BASH_SOURCE[0]}")/common.sh"

RELEASE_ID="${RELEASE_ID:?RELEASE_ID is required}"
PAYLOAD="${TMP_DIR}/payload.tar.gz"

# Image of the scrape check: the busybox pinned for the edge probe in compose.yml.
# Keep the digest equal to the busybox image in platform/observability/edge/compose.yml.
SCRAPE_CHECK_IMAGE="busybox:1.37.0@sha256:bdf57e528e45e4433820e045b29b4597825a1c9e38353532d90a01445013f82e"

# The API keeps only the bcrypt hash and the edge keeps the password, so rotating
# one without the other would fail only at scrape time. A one-shot container on
# the metrics network checks it: the API must answer 200 with the credential and
# 401 without it. The password is read from the mounted file inside the container
# and never reaches this script's output; it is not in the docker command line.
# The Basic header must not reach any process's argv, since /proc/<pid>/cmdline is
# readable from the host. The raw HTTP request is therefore written with printf (a
# shell builtin in ash) and sent with nc, which gets only host and port as
# arguments. The base64 comes from stdin for the same reason.
scrape_credential_check() {
  local env_file="$1"
  local secret_file="$2"
  local scrape_user scrape_target check result

  scrape_user="$(sed -n 's/^API_SCRAPE_USERNAME=//p' "${env_file}")"
  scrape_target="$(sed -n 's/^API_SCRAPE_TARGET=//p' "${env_file}")"
  [[ -n "${scrape_user}" ]] || die "API_SCRAPE_USERNAME must be set in .env"
  [[ "${scrape_target}" =~ ^[A-Za-z0-9._-]+:[0-9]{1,5}$ ]] || die "API_SCRAPE_TARGET must be host:port"

  # Runs inside the container, where $1 is the secret path. Prints status codes only.
  # shellcheck disable=SC2016  # the variables expand inside the container, not here
  check='
    host="${SCRAPE_TARGET%:*}"
    port="${SCRAPE_TARGET##*:}"
    auth="$({ printf "%s:" "${SCRAPE_USER}"; cat -- "$1" | tr -d "\r\n"; } | base64 | tr -d "\n")"
    status() { sed -n "1s/^HTTP\/[0-9.]* \([0-9][0-9][0-9]\).*/\1/p"; }
    with="$(printf "GET /actuator/prometheus HTTP/1.0\r\nHost: %s\r\nAuthorization: Basic %s\r\n\r\n" "${SCRAPE_TARGET}" "${auth}" | nc -w 10 "${host}" "${port}" 2>/dev/null | status)"
    without="$(printf "GET /actuator/prometheus HTTP/1.0\r\nHost: %s\r\n\r\n" "${SCRAPE_TARGET}" | nc -w 10 "${host}" "${port}" 2>/dev/null | status)"
    echo "with=${with:-none} without=${without:-none}"
  '

  # The values reach the container through the environment (-e NAME), so the
  # docker command line only carries names.
  if ! result="$(SCRAPE_USER="${scrape_user}" SCRAPE_TARGET="${scrape_target}" docker run --rm \
    --network "${METRICS_NETWORK}" \
    -e SCRAPE_USER -e SCRAPE_TARGET \
    -v "${secret_file}:/run/secrets/api-scrape-password:ro" \
    "${SCRAPE_CHECK_IMAGE}" sh -c "${check}" check /run/secrets/api-scrape-password)"; then
    die "scrape credential check could not run"
  fi
  if [[ "${result}" == "with=none without=none" ]]; then
    die "the API did not answer /actuator/prometheus on the metrics network (${result})"
  fi
  [[ "${result}" == "with=200 without=401" ]] || \
    die "unexpected scrape check result (${result}); expected with=200 without=401 (with=401 means the password does not match the API hash)"
  echo "API scrape credential: 200 with it, 401 without it"
}

# Checks that run before the active release is switched. The external network
# must exist (the PRD API Compose creates it), the scrape password must match the
# API hash, and the free disk must cover the queue budget: one queue per signal,
# 3 x COLLECTOR_QUEUE_BYTES. The database network and the monitoring role are
# additive and idempotent, so they are applied here too: the previous release does
# not use them, and a rollback keeps them.
preflight() {
  local env_file="$1"
  local secret_file="$2"
  local pg_secret_file="$3"
  local queue_bytes budget docker_root free_bytes

  docker network inspect "${METRICS_NETWORK}" >/dev/null 2>&1 || \
    die "external network ${METRICS_NETWORK} does not exist; deploy the PRD API Compose first, it creates the network"

  scrape_credential_check "${env_file}" "${secret_file}"

  ensure_db_metrics_network
  pg_monitor_role "${pg_secret_file}"

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
# as UID 10001 (the pg-monitor password, as nobody) and cannot be chowned without
# root, so these files are readable by others while their directory stays 700:
# only this SSH user can reach them on the host. The QA endpoint is read only by
# this script (600).
chmod 700 -- "${staging_dir}/.secrets"
test -s "${staging_dir}/.secrets/api-scrape-password" || die "payload has no api-scrape-password secret file"
test -s "${staging_dir}/.secrets/qa-otlp-endpoint" || die "payload has no qa-otlp-endpoint secret file"
test -s "${staging_dir}/.secrets/pg-monitor-password" || die "payload has no pg-monitor-password secret file"
chmod 444 -- "${staging_dir}/.secrets/api-scrape-password"
chmod 444 -- "${staging_dir}/.secrets/pg-monitor-password"
chmod 600 -- "${staging_dir}/.secrets/qa-otlp-endpoint"

preflight "${staging_dir}/.env" "${staging_dir}/.secrets/api-scrape-password" \
  "${staging_dir}/.secrets/pg-monitor-password"

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
