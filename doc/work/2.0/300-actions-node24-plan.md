# #300 — GitHub Actions to their Node 24 majors

https://github.com/rreganjr/Requel/issues/300

Lands on `master` through the hotfix lane (branch `300-actions-node24` from `origin/master`, PR to
`master`), then merges forward into `release/2.1`. CI only: no version bump, no tag. Filed under
`doc/work/2.0/` because `master`'s POM is `2.0.2`.

## Summary

Every workflow pins actions whose `runs.using` is `node20`; GitHub forces them onto Node 24 with a
warning today and will stop. `setup-java@v4` is also frozen. Bump each to its current major
(checked against each action's `action.yml` on 2026-10-27), add a non-publishing
`workflow_dispatch` to the two tag-only workflows so they can be proven without a tag, and verify on
the Ubuntu 26.04 runner image.

## Locked decisions

1. **On `master`, then forward.** `workflow_dispatch` works only from the default branch, and
   `master`'s CI (hotfixes) needs the bump as much as `release/2.1`.
2. **Docker actions folded in.** `docker/setup-qemu-action` and `docker/setup-buildx-action` v3
   are `node20` and were missing from the issue; v4 is `node24`.
3. **Keep `ubuntu-latest`.** Ubuntu 26.04 needs no workflow change (comment on the issue,
   2026-10-23); the PR proves it.
4. **Stale `2.0.0-dev` re-tag dropped** from `container-publish.yml`: `docker-compose.yml` follows
   the POM version since #376.

## Changes

| Workflow | Bumps | Other |
|---|---|---|
| `ci.yml` | checkout v4→v7 (2), setup-java v4→v6 (2), setup-node v4→v7 (2), upload-artifact v4→v7 (3), codecov-action v5→v7 (5) | none |
| `release.yml` | checkout→v7, setup-java→v6, setup-node→v7, action-gh-release v2→v3 | `workflow_dispatch` with boolean `publish` (default false); `Create GitHub Release` runs only when `github.event_name == 'push' \|\| inputs.publish` |
| `container-publish.yml` | checkout→v7, setup-java→v6, setup-node→v7, setup-qemu v3→v4, setup-buildx v3→v4 | same dispatch input; `VERSION` = POM version on dispatch; tag-derived re-tag only on a tag push; login, push and logout gated; `2.0.0-dev` re-tag removed |
| `pages.yml` | checkout→v7, configure-pages v5→v6, upload-pages-artifact v3→v5, deploy-pages v4→v5 | none (already dispatchable) |

Inputs we pass are unchanged in every new major (issue, "Compatibility"); the docker v4 release
notes are checked during implementation and any input change recorded in the PR.

## Verification

- **PR gate (on `master`):** both CI jobs green with no Node 20 / setup-java annotations; one run on
  `ubuntu-26.04` if the rollout hasn't reached the PR; Codecov uploads and comment; artifacts.
- **After merge:** dispatch `release.yml` and `container-publish.yml` from `master` with `publish`
  false (no Release, no push; image built and smoke-tested as `2.0.2`), and `pages.yml`.
- **Next real tag:** `v2.1.0-rc1` publishes as before.

## Out of scope

- #321 (coverage aggregate) also edits `ci.yml`; it lands after this, on `release/2.1`.
- Pinning actions to SHAs.

## Risks

- **A required-check name change.** Branch protection requires `Build & test`; job names are not
  touched, so the check keeps its name.
- **codecov v7 and OIDC.** Unchanged per its release notes; the PR's Codecov comment proves it.
