#!/usr/bin/env bash
# audit-action-shas.sh
# Audita todos os SHAs de actions em um workflow do GitHub Actions
# Uso: ./scripts/audit-action-shas.sh [.github/workflows/deploy.yml]

set -euo pipefail

WORKFLOW="${1:-.github/workflows/deploy.yml}"

if [ ! -f "$WORKFLOW" ]; then
  echo "Arquivo não encontrado: $WORKFLOW"
  exit 1
fi

if ! command -v gh >/dev/null 2>&1; then
  echo "gh CLI não instalado. Instale: https://cli.github.com/"
  exit 1
fi

echo "=== Auditing SHAs in $WORKFLOW ==="
echo ""

matches=$(grep -cE "uses:.*@[a-f0-9]{40}" "$WORKFLOW" || true)

if [ "$matches" -eq 0 ]; then
  echo "Nenhum SHA encontrado em $WORKFLOW"
  exit 0
fi

errors=0
checked=0

grep -E "uses:.*@[a-f0-9]{40}" "$WORKFLOW" | while IFS= read -r line; do
  action=$(echo "$line" | sed -E 's/.*uses: ([^@]+)@.*/\1/')
  sha=$(echo "$line" | sed -E 's/.*@([a-f0-9]{40}).*/\1/')

  echo "Action: $action"
  echo "SHA:    $sha"

  real_sha=$(gh api "repos/$action/commits/$sha" --jq '.sha' 2>/dev/null || echo "")

  if [ -z "$real_sha" ]; then
    echo "Status: SHA INVÁLIDO (404, rate limit ou action inexistente)"
    errors=$((errors + 1))
  elif [ "$real_sha" = "$sha" ]; then
    echo "Status: SHA válido"
  else
    echo "Status: SHA divergente (tag pode ter sido movida)"
    echo "Real:   $real_sha"
    errors=$((errors + 1))
  fi
  echo ""
  checked=$((checked + 1))
done

if [ "$errors" -gt 0 ]; then
  echo "=== Resultado: $errors SHA(s) com problema de $checked verificados ==="
  exit 1
else
  echo "=== Resultado: Todos os SHAs válidos ==="
fi
