# Release process

How Requel goes from a planned version to something users download, and how a fix reaches
users between planned versions. Requel ships as a jar and a Docker image that people install
and may stay on, so every release is a version someone might run for a long time: releases
are deliberate, versioned and tagged, not continuous.

The per-ticket lifecycle (review, plan, branch, verify, commit, PR, retro) is in `CLAUDE.md`
under **Development Workflow**. This guide covers the release around it.

## Summary

- **`release/x.y` is where a planned version is built.** One branch per planned minor
  release. Ticket branches PR into it and are squash-merged.
- **`master` is the latest shipped release.** A release branch merges into `master` when its
  version ships. `master` is the default branch, so the repo landing page and README describe
  what users download.
- **Hotfixes come off `master`.** Ticket branch from `master`, PR to `master`, tag `vx.y.z`
  there, then merge `master` forward into the active release branch.
- **A milestone is a concrete version** (`v2.1`, `v2.0.1`), created once its scope is
  decided. There is no `v2.x` bucket. An issue with no milestone is in the backlog.
- **One project board per minor release** ("Requel 2.1"). Patch milestones share their
  minor's board.
- **Only the latest shipped minor gets hotfixes.**

## Versions

Requel follows [semantic versioning](https://semver.org/):

| Bump | What it may contain |
|---|---|
| Patch (`2.0.1`) | Fixes to shipped behavior. No new features. No change to `doc/samples/project.xsd`, the command/query API, or configuration properties. A Flyway migration only if the fix needs one (see [Database migrations](#database-migrations)). |
| Minor (`2.1.0`) | New features. Schema changes that only add things. New commands, queries and properties. Anything a 2.0 user can upgrade to without changing their data or integrations. |
| Major (`3.0.0`) | Breaking changes: project XML a previous version can't import or export, a changed or removed API command or query (the Angular UI, the MCP gateway and `requel-cli` all consume it), removed properties, a migration path that drops data. |

The version in the root `pom.xml` says where a branch is:

| Version | Where |
|---|---|
| `2.1.0-dev` | `release/2.1` during development |
| `2.1.0-rc1`, `-rc2`, … | `release/2.1` while release candidates are being tested |
| `2.1.0` | the final commit on `release/2.1`, then `master` once promoted |
| `2.1.1`, `2.1.2`, … | `master`, one per hotfix release |

Tags are `v<major>.<minor>.<patch>[-rcN]`. Pushing a `v*` tag is what publishes a release:
see [What CI does](#what-ci-does).

## Branches

```
release/2.1   ●──●──●── rc1 ──● v2.1.0
             /                 \  promote (merge commit)
master   ───●───────────────────●────●── v2.1.1 (hotfix PR, squash)
                                 \    \
release/2.2                       ●──●─●──●──   cut from master; forward merge of v2.1.1
```

| Branch | Holds | Lands by |
|---|---|---|
| `master` | the latest shipped release, plus hotfixes | hotfix PRs (squash), promotion of a release branch (merge commit) |
| `release/x.y` | the next planned release | ticket PRs (squash), forward merges from `master` (merge commit) |
| `<issue>-<slug>` | one ticket | cut from `release/x.y` for planned work, from `master` for a hotfix |

Ticket PRs are squash-merged. **Merges between long-lived branches are merge commits,
never squashes**: a forward merge of `master` into `release/x.y`, and the promotion of
`release/x.y` into `master`. A squash drops the ancestry, so the next merge between the
same two branches conflicts on everything the last one already brought across. The repo
settings (Settings → General → Pull Requests) must allow both merge commits and squash
merging; the method is picked per PR.

`release/x.y+1` is cut from `master` after `x.y.0` ships. When the next version has to
start before that, cut it from the current release branch instead and merge the current
branch forward into it until it ships.

## Milestones, boards and the backlog

| Release | Branch | Milestone | Board | Work artifacts |
|---|---|---|---|---|
| 2.1.0 | `release/2.1` | `v2.1` | Requel 2.1 | `doc/work/2.1/` |
| 2.1.1 | `master` | `v2.1.1` | Requel 2.1 | `doc/work/2.1/` |

- **Triage.** A new issue gets no milestone by default. It is milestoned when that
  decision is made:
  - a defect in the shipped release that users will hit, fixable within the patch rules
    above → the next patch milestone (`v2.1.1`), created then if it doesn't exist;
  - planned work for the release in progress → that release's milestone (`v2.2`);
  - anything else stays in the backlog (`no:milestone`).
- **Create a milestone when its scope is decided**, not in advance. The next minor's
  milestone exists once there is agreed work for it; a patch milestone exists once there
  is a fix to ship.
- **Before a release ships**, every open issue on its milestone is closed or moved: to
  the next milestone if it is still planned, or back to the backlog.
- **Boards.** The scripts derive everything from the release number: `2.1` → milestone
  `v2.1` → board "Requel 2.1". A patch number maps to its minor's board, so `2.1.1` →
  milestone `v2.1.1` → board "Requel 2.1". `./scripts/setup-project.sh 2.2` creates a new
  minor's board; `./scripts/add-milestone-issues-to-project.sh 2.1.1` puts a patch
  milestone's issues on the parent board.
- **Retros.** `backfill-points.sh` selects by milestone, so run it once per milestone
  (`2.1` and `2.1.1`). `audit-retros.sh 2.1` walks the board, so it already covers the
  patch issues on it.
- **Work artifacts** go in `doc/work/<major.minor>/` from the `pom.xml` version of the
  branch the work is on. A hotfix on `master` at `2.1.0` files under `doc/work/2.1/`.

## Commits that skip the PR

Two kinds of change go straight onto a release branch or `master` with no ticket branch
and no PR. Line 1 of the commit is still the issue URL, so the retro counts it.

- **Documentation only**: `doc/**/*.md` and markdown files at the repo root (`README.md`,
  `CLAUDE.md`). CI skips these pushes (see [What CI does](#what-ci-does)), so a PR would
  only add review overhead.
- **Release commits**: a version bump and nothing else, as part of cutting a release
  below. CI runs on the push.

Everything else goes through a ticket branch and a PR.

## Cutting a release (x.y.0)

The steps below cut 2.1.0; substitute the version. Open a **Release 2.1.0** issue on the
`v2.1` milestone first: the version-bump commits carry its URL, and it is a place for
notes that don't belong to one ticket.

1. **Close the scope.** Nothing open is left on the milestone:
   ```bash
   gh issue list --repo rreganjr/Requel --milestone v2.1 --state open
   ```
2. **Bring `master` in.** If `master` has commits the release branch doesn't (hotfixes,
   or for 2.0 the work that landed on `master` directly), merge them forward first, so the
   release candidates test exactly what will be promoted. See
   [Forward merges](#forward-merges). Skip this step when this prints nothing:
   ```bash
   git fetch origin
   git log --oneline origin/release/2.1..origin/master
   ```
3. **Release candidate.** Set the version, commit, tag:
   ```bash
   git switch release/2.1
   git pull origin release/2.1
   mvn versions:set -DnewVersion=2.1.0-rc1 -DgenerateBackupPoms=false
   git ls-files '*pom.xml' | xargs git add
   git commit -m "https://github.com/rreganjr/Requel/issues/<release-issue>" -m "Version 2.1.0-rc1"
   git push origin release/2.1
   git tag -a v2.1.0-rc1 -m "Requel 2.1.0-rc1"
   git push origin v2.1.0-rc1
   ```
   The tag publishes a pre-release on GitHub and the `rreganjr/requel:2.1.0-rc1` image.
   `:latest` does not move.
4. **Test the candidate** from Docker Hub, not a local build (see
   [Verifying a published image](#verifying-a-published-image)). A problem is fixed by a
   normal ticket PR into `release/2.1`, then `-rc2`.
5. **Final release.** Same as step 3 with `-DnewVersion=2.1.0`, the message
   `Version 2.1.0` and the tag `v2.1.0`. This publishes the GitHub Release with the jar
   attached, and pushes `rreganjr/requel:2.1.0` and `:latest`.
6. **Promote to `master`** with a merge commit:
   ```bash
   gh pr create --repo rreganjr/Requel --base master --head release/2.1 --title "Release 2.1.0" --body "Promote release/2.1 (v2.1.0) to master."
   gh pr merge <pr> --repo rreganjr/Requel --merge
   ```
   Step 2 already brought `master` into the release branch, so this has no conflicts and
   leaves `master` with the same tree as `v2.1.0`.
7. **Close out.** Close the Release issue and the `v2.1` milestone (Issues → Milestones →
   Close), record retros (`./scripts/backfill-points.sh 2.1`), regenerate the doc index
   (`./scripts/gen-doc-index.sh 2.1`). Nothing lands on `release/2.1` after this; delete
   it once the promotion is merged.
8. **Start the next release** once there is agreed work for it: cut `release/2.2` from
   `master`, set `2.2.0-dev`, create the `v2.2` milestone and run
   `./scripts/setup-project.sh 2.2`. `doc/work/2.2/` is created by the first plan filed
   there.

## Hotfixes (x.y.z)

A hotfix patches the latest shipped release. The steps below ship 2.1.1.

1. **Issue and milestone.** Put the issue on `v2.1.1` (create the milestone if needed) and
   run `./scripts/add-milestone-issues-to-project.sh 2.1.1`, which adds it to the Requel 2.1
   board.
2. **Branch from `master`**, then follow the normal ticket lifecycle:
   ```bash
   git fetch origin
   git switch -c <issue>-<slug> origin/master
   ```
   The PR targets `master` (`gh pr create --base master ...`) and is squash-merged. Because
   `master` is the default branch, `Closes #<n>` closes the issue on merge.
3. **Version bump.** Fold `mvn versions:set -DnewVersion=2.1.1 -DgenerateBackupPoms=false`
   into the last fix's PR, so the release costs no extra CI run. When several fixes ship
   together, the bump rides with whichever merges last.
4. **Tag on `master`:**
   ```bash
   git switch master
   git pull origin master
   git tag -a v2.1.1 -m "Requel 2.1.1"
   git push origin v2.1.1
   ```
5. **Merge forward** into the active release branch, if there is one (next section).
6. **Close out.** Close the `v2.1.1` milestone and run `./scripts/backfill-points.sh 2.1.1`.

## Forward merges

After each hotfix, and before cutting a release, merge `master` into the active release
branch so the fix is not lost from the next version:

```bash
git fetch origin
git switch -c merge-master-into-2.2 origin/release/2.2
git merge --no-ff origin/master
# resolve conflicts; the pom version always keeps the release branch's value (2.2.0-dev)
git push -u origin merge-master-into-2.2
gh pr create --repo rreganjr/Requel --base release/2.2 --head merge-master-into-2.2 --title "Merge master (v2.1.1) into release/2.2" --body "Forward merge of master into release/2.2."
gh pr merge <pr> --repo rreganjr/Requel --merge
```

The PR is merged with **Create a merge commit**, never squash (see [Branches](#branches)).
Every pom conflicts on each forward merge, because `master` says `2.1.1` and the release
branch says `2.2.0-dev`; the release branch's version always wins. Merge commits have no
issue URL on line 1, which is fine: the retro counts the fix's own commit.

## Database migrations

Flyway applies migrations in version order and refuses to start if an applied migration
changed, so versions are allocated with more than one branch in mind:

- **Planned work** takes the next integer on its release branch (`V21__`, `V22__`).
- **A hotfix migration** takes a dotted version after the last migration in the release it
  patches: `V20.1__` when `v2.1.0` shipped with `V20` and `release/2.2` may already have
  `V21`. The forward merge carries it into the release branch.
- Upgrades then work from any supported version: a 2.1.0 database upgrading to 2.2.0 runs
  `V20.1` and then `V21`, and a 2.1.1 database already has `V20.1`.
- The only database that sees `V20.1` arrive after `V21` is a development database on
  `release/2.2`. Flyway refuses it as out of order: recreate the database, or start once
  with `--spring.flyway.out-of-order=true`.
- The standing rule still holds: never edit or renumber an applied migration
  (`CLAUDE.md`, **Development Guardrails**).

## Support policy

Only the latest shipped minor release gets hotfixes. Once 2.2.0 ships, a 2.1 user upgrades
to 2.2.x to get a fix; there is no 2.1.2. That is also why hotfixes tag on `master`:
container publishing moves `:latest` on every final tag, and `:latest` should only ever
move forward.

## What CI does

| Workflow | Runs on | Does |
|---|---|---|
| `ci.yml` | every push and PR to `master` and `release/**`, except ones that only touch `doc/**/*.md` or root `*.md` | full Maven build, unit + integration tests, Angular lint + unit tests, e2e |
| `release.yml` | a `v*` tag | full build + tests, then a GitHub Release with generated notes and the jar attached; `-rc` tags are pre-releases |
| `container-publish.yml` | a `v*` tag | build + tests, docker-compose smoke test, push `rreganjr/requel:<version>`; final tags (no `-` suffix) also push `:latest` |
| `pages.yml` | pushes touching `website/**` | publishes `website/`, which hosts the project XSD that exports point to |

Markdown under `doc/` is not skipped in general: only `*.md`. `doc/samples/project.xsd` is
read by the XML round-trip tests, so a change there runs CI.

If `Build & test` is a required status check on a branch, a docs-only PR never gets that
check (the workflow doesn't start), so it waits forever. That is one more reason docs-only
changes are committed directly; if one does go through a PR, merge it with `--admin`.

The smoke test in `container-publish.yml` re-tags the image as `2.0.0-dev` because
`docker-compose.yml` names that tag. When the version in `docker-compose.yml` changes, change
that workflow step to match.

## Verifying a published image

Pull the image from Docker Hub rather than using a local build, so the test covers what
users get. From a directory outside the repo, save this as `compose.yml`, set the tag, and
run `docker compose up`:

```yml
services:
  db:
    image: mysql:8.4
    environment:
      - "MYSQL_ROOT_PASSWORD=pa33w0rd"
      - "MYSQL_DATABASE=requel"
      - "MYSQL_ROOT_HOST=%"
    healthcheck:
      test: ["CMD-SHELL", "mysqladmin ping -h localhost -u root -p\"$MYSQL_ROOT_PASSWORD\" --silent"]
      interval: 10s
      timeout: 5s
      retries: 10
      start_period: 20s

  web:
    depends_on:
      db:
        condition: service_healthy
    image: rreganjr/requel:2.1.0-rc1
    pull_policy: always
    ports:
      - "8080:8080"
    environment:
      - "_JAVA_OPTIONS=-Xms2g -Xmx2g -XX:MaxMetaspaceSize=512m -XX:+UseG1GC"
      - "SPRING_DATASOURCE_URL=jdbc:mysql://db:3306/requel?createDatabaseIfNotExist=true&allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC"
      - "SPRING_DATASOURCE_USERNAME=root"
      - "SPRING_DATASOURCE_PASSWORD=pa33w0rd"
```

Then check that you can log in at http://localhost:8080/, the UI assets load, and you can
open a project and move between its pages.

Also test the upgrade path: start the previous release's image against a database, then
the candidate against the same database. Flyway migrates it on startup, and a failure there
is exactly what users would hit.

## Manual fallback

When GitHub Actions is unavailable, the same release can be built and published from a
workstation (JDK 17, Docker, and a Docker Hub login):

```bash
mvn -pl modules/requel-app -am clean verify
docker build --build-arg JAR_FILE=modules/requel-app/target/requel-app-2.1.0.jar -t rreganjr/requel:2.1.0 -t rreganjr/requel:latest .
docker login --username rreganjr
docker push rreganjr/requel:2.1.0
docker push rreganjr/requel:latest
```

Then create the GitHub Release by hand (Releases → Draft a new release, pick the pushed
tag, generate notes, attach the jar). Maven artifacts are not published to GitHub Packages;
the old v1.2 setup for that is in the history of the removed `RELEASE.md`.
