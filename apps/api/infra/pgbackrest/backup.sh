#!/bin/bash
# Runs a pgBackRest backup from the crontab that deploy-api-prod installs.
# Healthchecks gets the success ping only after `pgbackrest info` shows a new
# backup of the requested type started by this run; any other outcome sends
# /fail. A missing ping (host down, cron stopped, run stuck) is caught by the
# check's own schedule, outside PRD and QA.
set -uo pipefail

TYPE="${1:-}"
case "${TYPE}" in
  full|diff|metrics) ;;
  *) echo "usage: $0 full|diff|metrics" >&2; exit 2 ;;
esac

pgbackrest_info() {
  docker exec -u postgres workshop-db pgbackrest --stanza=workshop --output=json info
}

# Textfile for the node-exporter of the PRD edge (--collector.textfile), complementing
# Healthchecks: the latest backup without error of each type, read from `pgbackrest
# info`. Under /srv because /home/workshop is closed to the exporter's user (nobody).
# A failed info leaves the old file in place, so the age alerts in QA keep counting.
METRICS_DIR="/srv/pgbackrest/metrics"
write_metrics() {
  local info
  info="$(pgbackrest_info)" || { echo "pgbackrest info failed: metrics not updated" >&2; return 1; }
  mkdir -p "${METRICS_DIR}" && chmod 755 "${METRICS_DIR}" || return 1
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
  ' <<<"${info}" > "${METRICS_DIR}/pgbackrest.prom.tmp" \
    && chmod 644 "${METRICS_DIR}/pgbackrest.prom.tmp" \
    && mv "${METRICS_DIR}/pgbackrest.prom.tmp" "${METRICS_DIR}/pgbackrest.prom" \
    || { rm -f "${METRICS_DIR}/pgbackrest.prom.tmp"; echo "backup metrics not written" >&2; return 1; }
}

if [[ "${TYPE}" == metrics ]]; then
  write_metrics
  exit
fi

SECRETS_DIR="$(cd "$(dirname "$0")/.." && pwd)/config/secrets"
PING_URL="$(cat "${SECRETS_DIR}/healthchecks-backup-${TYPE}-url")" || exit 1

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

# Every run refreshes the metrics, whatever its outcome; this never changes the exit code.
trap 'write_metrics || true' EXIT

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

echo "${TYPE} backup OK: ${LABEL}"
ping_hc "" "${LABEL}"
