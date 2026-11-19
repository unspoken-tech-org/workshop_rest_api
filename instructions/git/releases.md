# Releases and tags: API

Never create, move or delete a tag, or dispatch a deploy, unless the user asks:
a tag deploys. Deploy details: `.github/workflows/AGENTS.md`.

- Semver: minor = feature, patch = fix, major = breaking the front contract.
  Latest: `git tag --sort=-v:refname | head`.
- `vX.Y.Z-rc.N` (with a dot) on `main` or `release/X.Y` goes to QA. `vX.Y.Z` on
  the same commit as the rc goes to production once that rc's QA deploy
  succeeded; the commit must be in `main` or `release/*`. A hotfix patch skips
  the rc and QA checks (`skip_rc`), not the branch check. The front uses the same suffixes.
- `git tag -a vX.Y.Z -m "<summary>" <sha> && git push origin vX.Y.Z`.
  Production needs approval in the `production` Environment.
- After a tag deploys, the workflow publishes a GitHub Release with notes
  generated from the PRs since the last final tag (an rc becomes a
  pre-release). PR titles are those notes: keep them in commit format.
- Migrations are forward-only: redeploying an older tag does not undo them.
  Warn when tagging a release that has one.
- `release/X.Y` (optional, on request): cut from `main`, or from the prod tag,
  to freeze a candidate or ship a hotfix while `main` is ahead. Tag there,
  merge fixes back into `main`, delete it at the next minor.
