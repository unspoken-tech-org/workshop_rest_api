#!/usr/bin/env bash
set -euo pipefail

# Claude Code PostToolUse hook: format and run ArchUnit when a Java file under apps/api is edited.
payload="$(cat)"
file_path="$(python3 -c 'import json,sys; d=json.load(sys.stdin); print((d.get("tool_input") or {}).get("file_path",""))' <<< "$payload" 2>/dev/null || true)"

if [[ -z "$file_path" || "$file_path" != *.java ]]; then
  exit 0
fi

if [[ "$file_path" != *"/apps/api/"* && "$file_path" != apps/api/* ]]; then
  exit 0
fi

project_dir="${CLAUDE_PROJECT_DIR:-$(pwd)}"
cd "$project_dir"

./gradlew :apps:api:spotlessApply :apps:api:test --tests "com.tproject.workshop.architecture.*" --quiet
