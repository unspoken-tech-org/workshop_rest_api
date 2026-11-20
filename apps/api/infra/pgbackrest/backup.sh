#!/bin/bash
# Runs a pgBackRest backup or expire from the crontab that deploy-api-prod installs.
# Healthchecks gets the success ping only after `pgbackrest info` shows a new
# backup of the requested type started by this run; any other outcome sends
# /fail. A missing ping (host down, cron stopped, run stuck) is caught by the
# check's own schedule, outside PRD and QA.
set -Eeuo pipefail

TYPE="${1:-}"
case "${TYPE}" in
  full|diff|expire|metrics) ;;
  *) echo "usage: $0 full|diff|expire|metrics" >&2; exit 2 ;;
esac

pgbackrest_info() {
  docker exec -u postgres workshop-db pgbackrest --stanza=workshop --output=json info
}

# Textfiles for the node-exporter of the PRD edge (--collector.textfile), complementing
# Healthchecks: under /srv because /home/workshop is closed to the exporter's user (nobody).
# A failed info leaves pgbackrest.prom in place, so the age alerts in QA keep counting.
METRICS_DIR="${METRICS_DIR:-/srv/pgbackrest/metrics}"

atomic_write_prom() {
  local dest="$1"
  local tmp="${dest}.tmp"
  mkdir -p "${METRICS_DIR}" && chmod 755 "${METRICS_DIR}" || return 1
  if cat > "${tmp}" && chmod 644 "${tmp}" && mv "${tmp}" "${dest}"; then
    return 0
  fi
  rm -f "${tmp}"
  echo "metrics not written: ${dest}" >&2
  return 1
}

read_existing_metric() {
  local file="$1" metric="$2"
  if [[ -f "${file}" ]]; then
    awk -v m="${metric}" '$1 == m { print $2; exit }' "${file}" || true
  fi
}

write_metrics() {
  local info
  info="$(pgbackrest_info)" || { echo "pgbackrest info failed: metrics not updated" >&2; return 1; }
  jq -er '
    .[0] as $s
    | def last_ok($t): [$s.backup[] | select(.type == $t and (.error | not))] | last;
    def gauge($name; $help; f):
      "# HELP \($name) \($help)", "# TYPE \($name) gauge",
      (("full", "diff") as $t | last_ok($t) | select(. != null)
        | "\($name){type=\"\($t)\"} \(f)");
    gauge("workshop_pgbackrest_last_backup_timestamp_seconds";
      "Stop time of the latest backup without error, by type."; .timestamp.stop),
    gauge("workshop_pgbackrest_last_backup_duration_seconds";
      "Duration of the latest backup without error, by type."; .timestamp.stop - .timestamp.start),
    gauge("workshop_pgbackrest_last_backup_repository_bytes";
      "Bytes the latest backup without error added to the repository, by type."; .info.repository.delta),
    "# HELP workshop_pgbackrest_stanza_status_code Status code of the stanza in pgbackrest info (0 is ok).",
    "# TYPE workshop_pgbackrest_stanza_status_code gauge",
    "workshop_pgbackrest_stanza_status_code \($s.status.code)"
  ' <<<"${info}" | atomic_write_prom "${METRICS_DIR}/pgbackrest.prom"
}

write_backup_status() {
  local exit_code="$1" attempt_ts="$2"
  local status_file="${METRICS_DIR}/pgbackrest_backup_status.prom"
  local last_success
  last_success="$(read_existing_metric "${status_file}" "workshop_pgbackrest_last_backup_success_timestamp_seconds")"
  if [[ "${exit_code}" -eq 0 ]]; then
    last_success="${attempt_ts}"
  fi
  {
    printf '%s\n' \
      "# HELP workshop_pgbackrest_last_backup_exit_code Exit code of the latest pgbackrest backup run (0 is ok)." \
      "# TYPE workshop_pgbackrest_last_backup_exit_code gauge" \
      "workshop_pgbackrest_last_backup_exit_code ${exit_code}" \
      "# HELP workshop_pgbackrest_last_backup_attempt_timestamp_seconds Unix timestamp of the latest pgbackrest backup attempt." \
      "# TYPE workshop_pgbackrest_last_backup_attempt_timestamp_seconds gauge" \
      "workshop_pgbackrest_last_backup_attempt_timestamp_seconds ${attempt_ts}"
    if [[ -n "${last_success}" ]]; then
      printf '%s\n' \
        "# HELP workshop_pgbackrest_last_backup_success_timestamp_seconds Unix timestamp of the latest successful pgbackrest backup run." \
        "# TYPE workshop_pgbackrest_last_backup_success_timestamp_seconds gauge" \
        "workshop_pgbackrest_last_backup_success_timestamp_seconds ${last_success}"
    fi
  } | atomic_write_prom "${status_file}"
}

write_expire_status() {
  local exit_code="$1" run_ts="$2"
  local expire_file="${METRICS_DIR}/pgbackrest_expire.prom"
  local last_success
  last_success="$(read_existing_metric "${expire_file}" "workshop_pgbackrest_expire_last_success_timestamp_seconds")"
  if [[ "${exit_code}" -eq 0 ]]; then
    last_success="${run_ts}"
  fi
  {
    printf '%s\n' \
      "# HELP workshop_pgbackrest_expire_last_exit_code Exit code of the latest pgbackrest expire run (0 is ok)." \
      "# TYPE workshop_pgbackrest_expire_last_exit_code gauge" \
      "workshop_pgbackrest_expire_last_exit_code ${exit_code}" \
      "# HELP workshop_pgbackrest_expire_last_run_timestamp_seconds Unix timestamp of the latest pgbackrest expire attempt." \
      "# TYPE workshop_pgbackrest_expire_last_run_timestamp_seconds gauge" \
      "workshop_pgbackrest_expire_last_run_timestamp_seconds ${run_ts}"
    if [[ -n "${last_success}" ]]; then
      printf '%s\n' \
        "# HELP workshop_pgbackrest_expire_last_success_timestamp_seconds Unix timestamp of the latest successful pgbackrest expire run." \
        "# TYPE workshop_pgbackrest_expire_last_success_timestamp_seconds gauge" \
        "workshop_pgbackrest_expire_last_success_timestamp_seconds ${last_success}"
    fi
  } | atomic_write_prom "${expire_file}"
}

if [[ "${TYPE}" == metrics ]]; then
  write_metrics
  exit
fi

if [[ "${TYPE}" == expire ]]; then
  RUN_TS="$(date +%s)"
  if docker exec -u postgres workshop-db pgbackrest --stanza=workshop expire; then
    EXPIRE_EXIT=0
  else
    EXPIRE_EXIT=$?
  fi
  write_expire_status "${EXPIRE_EXIT}" "${RUN_TS}" || true
  exit "${EXPIRE_EXIT}"
fi

SECRETS_DIR="${SECRETS_DIR:-$(cd "$(dirname "$0")/.." && pwd)/config/secrets}"
ATTEMPT_TS="$(date +%s)"
BACKUP_EXIT_CODE=1

# Every run refreshes both run-status and info metrics, whatever its outcome;
# this never changes the exit code.
trap 'write_backup_status "${BACKUP_EXIT_CODE}" "${ATTEMPT_TS}" || true; write_metrics || true' EXIT

PING_URL="$(cat "${SECRETS_DIR}/healthchecks-backup-${TYPE}-url")"

# Ping failures are logged and never change the exit code: Healthchecks alerts on its own.
ping_hc() {
  curl -fsS -m 10 --retry 3 -o /dev/null --data-raw "${2:-}" "${PING_URL}$1" \
    || echo "healthchecks ping $1 failed" >&2
}

fail() {
  echo "${TYPE} backup failed: $1" >&2
  ping_hc /fail "$1"
  exit 1
}

ping_hc /start

if ! BEFORE="$(pgbackrest_info | jq -er --arg t "${TYPE}" \
  '[.[0].backup[] | select(.type == $t)] | last | .label // ""')"; then
  fail "pgbackrest info failed before the backup"
fi

STARTED="$(date +%s)"
if ! docker exec -u postgres workshop-db pgbackrest --stanza=workshop --type="${TYPE}" backup; then
  fail "pgbackrest backup exited non-zero"
fi

# A diff with no full to build on is silently promoted to full: the type check catches it.
if ! LABEL="$(pgbackrest_info | jq -er --arg t "${TYPE}" --arg before "${BEFORE}" \
  --argjson started "${STARTED}" '
    .[0] as $s
    | ([$s.backup[] | select(.type == $t)] | last) as $b
    | select($s.status.code == 0 and $b != null and $b.label != $before
        and $b.timestamp.start >= $started and ($b.error | not))
    | $b.label')"; then
  fail "pgbackrest info shows no new ${TYPE} backup from this run"
fi

BACKUP_EXIT_CODE=0
echo "${TYPE} backup OK: ${LABEL}"
ping_hc "" "${LABEL}"
