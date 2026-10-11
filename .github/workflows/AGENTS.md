# AGENTS.md — GitHub Actions workflows

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
   gh workflow run deploy-observability-control-plane-qa.yml --ref feature/observability-otlp -f confirm=DEPLOY -f ref=feature/observability-otlp
   ```

4. When the feature branch is merged, its version replaces the skeleton. If the
   rebase or merge conflicts on the workflow file, keep the feature-branch side.

### Limits

- Guards in the real workflow still apply. Jobs restricted to
  `refs/heads/main` (for example `plan` and `apply` in
  `terraform-cloudflare-observability.yml`) do not run from a feature branch.
  Do not relax those guards to use this technique; merge that workflow instead.
- Repository-level secrets are available to any branch dispatched by someone
  with write access. Prefer Environments with required reviewers for workflows
  that deploy or hold credentials.
- The repository is public, so Actions logs are public. A run from a feature
  branch must follow the same rule: never print secrets, plans or `.env` values.
- Do not keep skeletons indefinitely: track the feature PR that replaces each one.
