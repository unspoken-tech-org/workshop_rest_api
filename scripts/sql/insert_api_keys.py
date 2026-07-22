#!/usr/bin/env python3
"""
Dynamically generate multi-row INSERT SQL for QA API keys from Infisical JSON.
Reads tags[].name to determine platform (fallback: name parsing).
"""
import sys
import json


def detect_platform(item):
    """Detect platform from Infisical tags first, then fallback to name parsing."""
    for tag in item.get("tags", []):
        name = tag.get("name", "").upper()
        if name:
            return name

    key_name = item.get("key", "").upper()
    for p in ["WEB", "DESKTOP", "SERVER", "SERVICE"]:
        if p in key_name:
            return p
    return "MOBILE"


def main():
    try:
        data = json.loads(sys.argv[1]) if len(sys.argv) > 1 else []
    except (json.JSONDecodeError, IndexError) as e:
        print(f"ERROR: Invalid JSON input: {e}", file=sys.stderr)
        sys.exit(1)

    if not data:
        sys.exit(0)

    rows = []
    for item in data:
        key_val = item.get("value", "")
        comment = item.get("comment", "")

        if not key_val:
            continue

        platform = detect_platform(item)
        client_name = f"qa_{platform.lower()}_app"
        desc = comment if comment else f"Infisical QA Key"

        safe_val = key_val.replace("'", "''")
        safe_desc = desc.replace("'", "''")

        rows.append(
            f"('{safe_val}', '{client_name}', 'qa_admin', "
            f"'{platform}', 'ADMIN', '{safe_desc}', true, NOW())"
        )

    if not rows:
        sys.exit(0)

    sql = "INSERT INTO api_keys (key_value, client_name, user_identifier, platform, role, description, active, created_at) VALUES\n  "
    sql += ",\n  ".join(rows)
    sql += "\nON CONFLICT (key_value) DO UPDATE SET active = true;"
    print(sql)


if __name__ == "__main__":
    main()
