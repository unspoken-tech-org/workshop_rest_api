#!/bin/bash
# Runs a pgBackRest backup from the crontab that deploy-api-prod installs.
# Healthchecks gets the success ping only after `pgbackrest info` shows a new
# backup of the requested type started by this run; any other outcome sends
# /fail. A missing ping (host down, cron stopped, run stuck) is caught by the
# check's own schedule, outside PRD and QA.
set -uo pipefail

TYPE="${1:-}"
case "${TYPE}" in
  full|diff) ;;
  *) echo "usage: $0 full|diff" >&2; exit 2 ;;
esac

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

pgbackrest_info() {
  docker exec -u postgres workshop-db pgbackrest --stanza=workshop --output=json info
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

echo "${TYPE} backup OK: ${LABEL}"
ping_hc "" "${LABEL}"
