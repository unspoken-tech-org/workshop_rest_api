# Releases and tags: API

Never create, move or delete a tag, or dispatch a deploy, unless the user asks:
a tag deploys. Deploy details: `.github/workflows/AGENTS.md`.

- Semver: minor = feature, patch = fix, major = breaking the front contract.
  Latest: `git tag --sort=-v:refname | head`.
- `vX.Y.Z-rc.N` (with a dot) on `main` or `release/X.Y` goes to QA. `vX.Y.Z` on
  the same commit as the approved rc goes to production. A hotfix patch skips
  the rc (`skip_rc`). The front uses the same suffixes.
- `git tag -a vX.Y.Z -m "<summary>" <sha> && git push origin vX.Y.Z`.
  Production needs approval in the `production` Environment.
- Migrations are forward-only: redeploying an older tag does not undo them.
  Warn when tagging a release that has one.
- `release/X.Y` (optional, on request): cut from `main`, or from the prod tag,
  to freeze a candidate or ship a hotfix while `main` is ahead. Tag there,
  merge fixes back into `main`, delete it at the next minor.
