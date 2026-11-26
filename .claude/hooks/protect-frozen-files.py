#!/usr/bin/env python3
"""Claude Code PreToolUse hook to protect frozen fixtures and existing migrations.

Blocks Edit/Write/MultiEdit on:
- apps/api/src/test/resources/jsons/** (create or edit; deletion via rm/git rm stays allowed)
- existing files in apps/api/src/main/resources/db/migration/postgresql/** (new migrations allowed)
"""

import json
import os
import sys


def main() -> int:
    try:
        payload = json.load(sys.stdin)
    except json.JSONDecodeError as exc:
        print(f"protect-frozen-files: invalid JSON payload: {exc}", file=sys.stderr)
        return 1

    tool_name = payload.get("tool_name", "")
    if tool_name not in {"Edit", "Write", "MultiEdit"}:
        return 0

    tool_input = payload.get("tool_input") or {}
    file_path = tool_input.get("file_path") or ""
    if not file_path:
        return 0

    normalized = os.path.normpath(file_path).replace("\\", "/")

    if "/apps/api/src/test/resources/jsons/" in normalized or normalized.startswith(
        "apps/api/src/test/resources/jsons/"
    ):
        print(
            "BLOCKED: editing or creating JSON snapshots under "
            "apps/api/src/test/resources/jsons/ is forbidden (CONVENTIONS.md #5). "
            "Use direct assertions instead; deleting legacy snapshots is allowed via rm.",
            file=sys.stderr,
        )
        return 2

    if (
        "/apps/api/src/main/resources/db/migration/postgresql/" in normalized
        or normalized.startswith("apps/api/src/main/resources/db/migration/postgresql/")
    ):
        if tool_name in {"Edit", "MultiEdit"} or os.path.exists(file_path):
            print(
                "BLOCKED: Flyway migrations are forward-only; modifying an existing "
                "migration under db/migration/postgresql/ is forbidden (AGENTS.md). "
                "Create a new versioned migration file instead.",
                file=sys.stderr,
            )
            return 2

    return 0


if __name__ == "__main__":
    sys.exit(main())
