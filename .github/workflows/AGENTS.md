# AGENTS.md — CI/CD (GitHub Actions + Tailscale)

Applies to `.github/` (workflows, composite actions, scripts). The root
`AGENTS.md` still applies.

## Rules
- **Naming:** workflows are `deploy-{service}-{environment}.yml`; composite
  actions live in `.github/actions/<group>/<action>/`; remote scripts live in
  `.github/scripts/<group>/` as `deploy.sh`, `rollback.sh`, `verify.sh`,
  `cleanup.sh` (plus `common.sh`, loaded with `source`, where needed).
- **Pinning:** third-party actions are pinned by full commit SHA with the
  version in a trailing comment. Upgrade every use of an action together, so
  one action never runs at two versions.
- **Transport:** SSH goes over Tailscale (ephemeral nodes via
  `tailscale/github-action`, `tags: tag:ci`, Tailscale SSH). Workflows hold no
  private SSH key. Host keys come from the `*_SSH_KNOWN_HOSTS` secret through
  `shared/setup-ssh-known-hosts`. The Cloudflare Tunnel `ssh.eletroluk.com` is
  retired; manual access is also Tailscale SSH.
- **Manual deploys** use `workflow_dispatch` with a `confirm` choice input that
  defaults to `CANCEL`; anything other than `DEPLOY` stops before any job that
  holds secrets or reaches a server. Terraform workflows use the same pattern
  with `PLAN`, `APPLY` or `DRILL`.
- **Approval:** production, observability and Terraform apply jobs run in a
  GitHub Environment with a required reviewer and a deployment ref policy (see
  the table). Do not relax a ref guard or an Environment policy to make a run
  pass.
- **Logs are public** (public repository): never print secrets, Terraform
  plans or `.env` values. Never pass a secret on the command line. The
  observability deploys already ship secrets as files under `.secrets/`
  instead of the `.env`; follow that for new secrets.
- **Permissions:** start from `permissions: contents: read` and add only what a
  job needs (e.g. `packages: write` for the image build).
- **Isolation:** each stack has its own `concurrency` group, deploy directory
  and Compose project; a deploy must not touch another stack's containers or
  images.
- **Archived pieces** (`deploy-observability-prod.yml`,
  `actions/observability/`, `scripts/observability/`) refuse to run. Do not
  revive them; extend `observability-control-plane` or `observability-edge`.

## Workflows

| Workflow | Trigger | Environment | Allowed refs |
| --- | --- | --- | --- |
| `ci.yml` | push, PR | — | — |
| `deploy-api-prod.yml` | tag `vX.Y.Z`, dispatch (`skip_rc`) | `production` | tag `vX.Y.Z` on the commit of a `vX.Y.Z-rc.N` (unless `skip_rc`) |
| `deploy-api-qa.yml` | tag `vX.Y.Z-rc.N`, dispatch | `qa` (no reviewer) | any |
| `deploy-gateway-prod.yml` | dispatch + `confirm` | `production` | tags `v*.*.*`, `release/*` |
| `deploy-gateway-qa.yml` | dispatch + `confirm` | — | any |
| `deploy-observability-control-plane-qa.yml` | dispatch + `confirm` | `qa-observability` | `main` (commit must be on `main`) |
| `deploy-observability-edge-prod.yml` | dispatch + `confirm` | `production-observability` | `main` (commit must be on `main`) |
| `terraform-cloudflare-observability.yml` | PR/push plan; dispatch `PLAN`/`APPLY` | `observability-terraform-apply` (apply) | `main` |
| `terraform-state-drill-observability.yml` | dispatch `DRILL` | `observability-terraform-apply` | `main` |
| `restore-test.yml` | dispatch, weekly, PR touching the DB image | — | any (synthetic data, no secrets) |
| `deploy-observability-prod.yml` | dispatch | — | archived, no deploy |

PRD deploys are dispatched with `--ref` pointing at the tag (e.g.
`gh workflow run deploy-api-prod.yml --ref v1.2.0 -f allow_migrations=true`).

## Secrets
- Names follow `PRD_*`/`PROD_*` and `QA_*` per environment, and `OBS_*` for
  observability and Terraform state. Check the workflow before adding a new one
  and reuse the existing prefix.
- Tailscale: `PRD_TS_OAUTH_CLIENT_ID`, `PRD_TS_OAUTH_SECRET`, `TS_TAILSCALE_IP`;
  `QA_TS_OAUTH_CLIENT_ID`, `QA_TS_OAUTH_SECRET`, `QA_TS_TAILSCALE_IP`.
- SSH: `PROD_SSH_USER`, `PROD_SSH_KNOWN_HOSTS`; `QA_SSH_USER`,
  `QA_SSH_KNOWN_HOSTS`. Target is `<SSH_USER>@<TS_TAILSCALE_IP>` (`100.x.x.x`).

## Skeleton technique for `workflow_dispatch`

GitHub only registers a `workflow_dispatch` workflow when its file exists on the
default branch (`main`). Without it, the UI does not list the workflow and
`gh workflow run` fails with `could not find any workflows named ...`.

When the workflow is dispatched with a ref (UI "Use workflow from",
`gh workflow run <file> --ref <branch>` or the REST API `ref` field), GitHub runs
the version of the file **from that ref**, not the one on `main`. The copy on
`main` only registers the trigger.

So a new manual workflow can be tested from its feature branch without merging
the whole branch:

1. Open a small PR to `main` with a skeleton that has:
   - the **same file name** as the real workflow;
   - the **same `name:`** and the **same `on.workflow_dispatch.inputs`**, so the
     dispatch form matches;
   - one no-op job that fails with a clear message, so a run from `main` never
     looks like a successful deploy;
   - `permissions: contents: read` and no secrets.
2. Keep the real implementation on the feature branch.
3. Dispatch from the feature branch:

   ```bash
   gh workflow run <workflow>.yml --ref <feature-branch> -f confirm=DEPLOY
   ```

4. When the feature branch is merged, its version replaces the skeleton. If the
   rebase or merge conflicts on the workflow file, keep the feature-branch side.

### Limits
- It does not work for workflows whose jobs are gated to `main`, by a
  `github.ref == 'refs/heads/main'` condition or by an Environment that only
  allows `main` (every row with `main` in the table above). Merge those
  workflows instead.
- Repository-level secrets are available to any branch dispatched by someone
  with write access. Prefer Environments with required reviewers for workflows
  that deploy or hold credentials.
- Do not keep skeletons indefinitely: track the feature PR that replaces each
  one. No skeleton is active today.
