# We Need These Stinkin' Badges — README Badge & Coverage Plan

## Summary

Add a row of status/identity badges to `README.md` (build, tests, license, latest
release, Java/Spring versions, Docker pulls) and publish line-level coverage from
the existing CI workflow so a coverage badge can render alongside them. The
backend (JaCoCo) coverage badge is reachable with a small change to the existing
`.github/workflows/ci.yml`. The frontend/E2E coverage badge requires scaffolding
that the repo does not yet have (Playwright config, an `e2e/` test directory,
and a CI job that boots the app), so it is split into a later phase.

This plan complements `20-release-plan.md` (CI orchestration) and
`e2e-coverage-improvement-plan.md` (existing JS/E2E coverage targets). It does
not change the test strategy itself.

---

## 1. Goals

The release effort needs to deliver four things, in order of effort:

1. A row of static/auto-rendered badges at the top of `README.md` covering
   license, latest release, last commit, Docker pulls, Java/Spring versions,
   and the existing CI workflow status — none of these require any change to
   the build, only a `LICENSE` file at the repo root and a paste into the
   README.
2. A **backend coverage badge** sourced from the JaCoCo XML report that
   `ci.yml` already produces but currently throws away after 14 days as an
   artifact. Achieved by adding a `codecov/codecov-action@v4` upload step to
   the existing `Build & test` job.
3. A **frontend/E2E coverage badge** sourced from a V8-coverage report
   produced by Playwright + `monocart-reporter`. Requires creating the
   missing `playwright.config.ts` / `playwright.coverage.config.ts`, an
   `e2e/` test directory with at least one happy-path spec, and a new CI job
   that boots the Spring Boot app against a MySQL service container and runs
   `npm run e2e:coverage`.
4. A reproducible badge set — every badge URL in the README either reads
   directly from GitHub's API (release, license, last commit), reads from a
   service we deliberately publish to (Codecov), or is a static SPDX/version
   chip. No screenshots, no hand-maintained numbers.

Out of scope: changing the existing test inventory, the Maven build, or
the `release/2.0` branching model.

---

## 2. Current State

### 2.1. What's already in place

- `.github/workflows/ci.yml` (single workflow, `CI`):
  - Runs on push and PR to `master` and `release/**`.
  - Sets up JDK 17 + Node 22.
  - Runs `mvn -pl modules/requel-app -am verify` (so Surefire unit tests and
    Failsafe integration tests both run).
  - Runs `npm test -- --no-watch` in `requel-angular/` (Vitest unit tests).
  - Uploads `**/target/site/jacoco/` as a 14-day artifact named
    `jacoco-coverage`.
  - Uploads `surefire-reports` / `failsafe-reports` on failure.
- The Maven build is already producing JaCoCo XML at
  `**/target/site/jacoco/jacoco.xml` (per the workflow comment on lines
  76–86). No POM change needed for backend coverage.
- `requel-angular/package.json` declares:
  - `@playwright/test`, `monocart-reporter`, `@vitest/coverage-v8`.
  - Scripts: `e2e`, `e2e:headed`, `e2e:report`, `e2e:coverage` (which
    references `playwright.coverage.config.ts`).

### 2.2. What's missing

- No `LICENSE` file at the repo root. Without one, the GitHub shields.io
  license badge renders as "no license" and downstream "Used by" data is
  blocked.
- No `playwright.config.ts` or `playwright.coverage.config.ts` in
  `requel-angular/`. The `e2e` and `e2e:coverage` scripts will fail
  immediately if run today.
- No `requel-angular/e2e/` (or equivalent) directory with Playwright specs.
- No CI step that publishes the JaCoCo XML anywhere shields.io or a coverage
  service can read it.
- No CI job that runs Playwright at all. The `monocart-reporter` dependency
  is currently dead weight.

---

## 3. Phase 1 — README Badge Row (no CI changes)

Add a single block of Markdown badges immediately under the `## Requel 2.0`
heading. The badges fall into two buckets: auto-rendered from GitHub
metadata, and static identity chips for the stack.

### 3.1. Add a `LICENSE` file

Pick a license (the existing thesis materials in `doc/` are Harvard-published
and unrelated to the source-code license; the codebase itself has no declared
license today). Apache-2.0 or MIT are the conventional choices for a project
of this shape. Drop the standard SPDX template at `LICENSE` in the repo
root. GitHub auto-detects the SPDX id and the badge starts rendering on the
next push.

### 3.2. README badge block

Paste under the `## Requel 2.0` heading (Markdown order matters for
shields.io layout — keep build/tests first so the eye lands on green/red):

```markdown
[![CI](https://github.com/rreganjr/Requel/actions/workflows/ci.yml/badge.svg)](https://github.com/rreganjr/Requel/actions/workflows/ci.yml)
[![Coverage](https://codecov.io/gh/rreganjr/Requel/branch/master/graph/badge.svg)](https://codecov.io/gh/rreganjr/Requel)
[![License](https://img.shields.io/github/license/rreganjr/Requel)](LICENSE)
[![Release](https://img.shields.io/github/v/release/rreganjr/Requel)](https://github.com/rreganjr/Requel/releases)
[![Last commit](https://img.shields.io/github/last-commit/rreganjr/Requel)](https://github.com/rreganjr/Requel/commits/master)
[![Docker Pulls](https://img.shields.io/docker/pulls/rreganjr/requel)](https://hub.docker.com/r/rreganjr/requel)
[![Java](https://img.shields.io/badge/Java-17-007396?logo=openjdk)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-6DB33F?logo=springboot)](https://spring.io/projects/spring-boot)
[![Angular](https://img.shields.io/badge/Angular-21-DD0031?logo=angular)](https://angular.dev/)
```

Notes:

- The CI badge points at the existing `ci.yml`. It is green only when both
  `mvn verify` and Vitest pass — no separate "tests" badge needed.
- The Coverage badge is included in this block but does not render until
  Phase 2 lands. Leaving the line in place avoids a follow-up README edit.
- The Release badge says "no releases" until a v2.0.0 GitHub Release is
  cut. That happens at the end of the cycle per `RELEASE_GUIDE.md`.

### 3.3. Acceptance criteria

- `LICENSE` exists at the repo root with a recognised SPDX id.
- `README.md` shows the badge block at the top.
- CI, license, last commit, Docker pulls, and the three identity chips
  render correctly on the public README.

---

## 4. Phase 2 — Backend Coverage via Codecov

Goal: turn the JaCoCo XML that `ci.yml` already produces into a live coverage
percentage that shields.io can render, and surface PR-level coverage diffs.

### 4.1. Why Codecov

Three candidate services were considered:

| Option | Effort | Cost | PR diffs | Notes |
| --- | --- | --- | --- | --- |
| **Codecov** | Add one step to `ci.yml`. No token needed for public repos. | Free for OSS | Yes | Native JaCoCo XML support. Sensible default for the Maven + Angular split via "flags". |
| Coveralls | Same shape as Codecov but expects LCOV; needs a JaCoCo→LCOV converter step. | Free for OSS | Yes | Older API, less polished JaCoCo path. |
| Self-hosted (`gh-pages` + shields.io dynamic JSON) | Author a shell step to parse `jacoco.xml`, compute %, push to a `gh-pages` JSON file. | Free, no third party. | No | Adds a second workflow that pushes to `gh-pages`; ongoing maintenance for the parsing step. |

Codecov is the lowest-cost path that also gets PR coverage comments —
worth keeping because `e2e-coverage-improvement-plan.md` already tracks
coverage targets by file, and Codecov surfaces those numbers in PR reviews.

### 4.2. `ci.yml` modification

Add one step at the end of the existing `build-and-test` job, after the
JaCoCo artifact upload:

```yaml
      - name: Upload backend coverage to Codecov
        if: always()
        uses: codecov/codecov-action@v4
        with:
          files: '**/target/site/jacoco/jacoco.xml'
          flags: backend
          name: jacoco-backend
          fail_ci_if_error: false
```

Rationale for each option:

- `if: always()` matches the surrounding `Upload JaCoCo coverage` step so
  coverage is uploaded even when later steps fail (e.g. Vitest flakes).
  Treating coverage as observational, not a gate.
- `flags: backend` tags this upload as the Java/Spring side so Phase 3 can
  add a `frontend-e2e` flag without overwriting it.
- `fail_ci_if_error: false` matches the project's existing posture that
  coverage telemetry should never break a build.

### 4.3. Codecov repo setup

For a public repo, Codecov requires only that the repo be linked from
[https://codecov.io/](https://codecov.io/) under the GitHub org. No secret
or token has to be added to GitHub Actions. The maintainer should:

1. Sign in to Codecov with the GitHub identity that owns `rreganjr/Requel`.
2. Click "Add repository" and accept the default settings.
3. Optionally add a `codecov.yml` at the repo root to set target thresholds
   (recommended: do **not** add this in Phase 2; let real numbers settle
   for one or two CI runs first).

### 4.4. Codecov configuration walkthrough

The Codecov onboarding wizard at [codecov.io](https://codecov.io/) is a
tutorial, not a configuration gate. The actual coverage pipeline is
defined by the `codecov/codecov-action@v4` step in `ci.yml` (§4.2) — the
wizard exists only to install the GitHub App, generate an example upload
snippet, and surface a per-repo upload token if one is needed. Treat
every wizard prompt as cosmetic except "install the GitHub App" and
"copy the repo upload token."

**Account creation.** Sign in at [codecov.io](https://codecov.io/) with
the GitHub identity that owns `rreganjr/Requel`. Codecov requests the
standard GitHub OAuth scopes (read user, read org membership, read
repository metadata). Accept and authorise the Codecov GitHub App
against the `rreganjr` account. For a public repo, no further
permissions are required; private-repo coverage would also need the
"act on behalf" scope, which does not apply here.

**Plan selection.** Pick the "Open Source" plan. Codecov's Pro and Team
plans are private-repo-only features (carry-forward flags across
branches, larger upload limits); the OSS tier is unlimited for public
repos and matches everything this project needs.

**Add the repository.** From the dashboard, click "Add new repository"
and select `rreganjr/Requel` from the list. If it does not appear,
revisit the GitHub App installation and grant it access to the repo
explicitly — the App defaults to "all repos" but org-scoped accounts
sometimes default to a curated list. Once linked, the repo lands on a
"setup" screen with the framework selector.

**Framework selector — pick anything.** The wizard currently offers
Jest, Vitest, Pytest, and Go as preset choices. None of these match
JaCoCo, which is what the backend produces. This does not matter: the
selector only changes the example shell command shown on the next
screen, it does not constrain what report formats Codecov will accept.
The Codecov uploader auto-detects JaCoCo XML, Cobertura XML, LCOV, and
several other formats at upload time. Recommended pick: **Vitest**,
because the project already uses Vitest for Angular unit tests (per
§2.1) and the generated example snippet will be the closest match to
the frontend half of the pipeline. The Java half is covered by the
codecov-action step in `ci.yml`, not by anything the wizard generates.

**Upload token.** The wizard prints a `CODECOV_TOKEN` value. For a
**public** repo this token is **not required** — `codecov-action@v4`
will authenticate via the GitHub OIDC token bound to the workflow run.
Skip the "add this secret to GitHub Actions" step. If a future need
arises (e.g. uploading from a fork or a self-hosted runner that does
not have OIDC), save the token to `secrets.CODECOV_TOKEN` in the repo's
GitHub Actions settings and add `token: ${{ secrets.CODECOV_TOKEN }}`
to the action step. Keep it out of the workflow file regardless.

**Confirm the first upload.** After §4.2's workflow change lands on
`master`, the next CI run pushes a JaCoCo XML report. Codecov processes
it within ~60 seconds. The "Add repository" wizard collapses
automatically once the first report is ingested and the repo page
starts showing a coverage percentage instead of the "waiting for first
upload" placeholder. If the upload step in CI shows green but the
Codecov page still says "waiting," the most common cause is the action
not finding the XML file — re-check the `files:` glob in §4.2 against
the actual JaCoCo output path (`mvn verify` writes to
`modules/*/target/site/jacoco/jacoco.xml`, one per module).

**Where the badge URL comes from.** From the Codecov repo page, open
Settings → Badges & Graphs. The Markdown snippet there is the source of
truth for the badge URL in §3.2. The pattern is stable
(`https://codecov.io/gh/<owner>/<repo>/branch/<branch>/graph/badge.svg`),
so the URL already pasted into the Phase 1 badge block will work
without editing once the first upload completes.

**Flags strategy.** Codecov "flags" tag each upload so multiple reports
can roll up into one project without overwriting each other. The
planned shape for this repo:

- `backend` — JaCoCo XML from `mvn verify` (Phase 2, §4.2).
- `frontend-unit` — Vitest V8 coverage from `npm test -- --coverage`,
  optional follow-on to Phase 2 if a unit-coverage report is desired
  separately from E2E. The current Vitest step in `ci.yml` does not
  emit coverage; adding `--coverage` to the existing `npm test`
  invocation produces an LCOV report at
  `requel-angular/coverage/lcov.info` that uploads cleanly with this
  flag.
- `frontend-e2e` — Monocart/Playwright V8 coverage (Phase 3, §5.3).

Codecov's default badge aggregates all flags into a single number. To
keep the README simple, leave the badge unflagged (§3.2). If reviewers
ever want a flag-scoped chip, the URL pattern is in §5.4.

**`codecov.yml` — defer.** The Codecov UI nudges new users toward
adding a `codecov.yml` at the repo root with target thresholds (e.g.
"fail PR if coverage drops more than 1%"). Skip this in Phase 2. The
first few uploads are needed to establish a realistic baseline,
especially given the 30–60% branch coverage on lowest files flagged in
`e2e-coverage-improvement-plan.md`. Once the baseline is stable, add a
minimal `codecov.yml` along the lines of:

```yaml
coverage:
  status:
    project:
      default:
        target: auto
        threshold: 1%
    patch:
      default:
        target: 80%
```

This lets the baseline drift only with explicit reviewer awareness
(`auto` = "do not regress vs. base") and holds new code on a PR to a
higher bar than the project average. Land this as a follow-up PR after
the first Codecov dashboard for `master` is visible.

### 4.5. Acceptance criteria

- The CI badge and the Coverage badge both render green on `master`.
- A PR against `master` receives a Codecov comment with backend coverage
  delta within ~3 minutes of CI completion.
- The `jacoco-coverage` artifact upload step is unchanged (still useful
  for offline inspection of the HTML report).

---

## 5. Phase 3 — Frontend/E2E Coverage via Playwright + Monocart

Goal: produce a V8 coverage report from a headless Playwright run against a
booted Spring Boot app, upload it to Codecov under a `frontend-e2e` flag,
and either show a combined Codecov badge or add a second flagged badge.

This phase is materially larger than Phase 2 because the supporting
scaffolding does not exist yet (see §2.2).

### 5.1. Author Playwright configs

Two files in `requel-angular/`:

- `playwright.config.ts` — base config used by `npm run e2e` /
  `npm run e2e:headed`. Defines `baseURL: http://localhost:8081`, the
  test directory (`./e2e`), a single Chromium project, and a JSON +
  HTML reporter pair.
- `playwright.coverage.config.ts` — extends the base config, adds
  `monocart-reporter` with V8 coverage collection turned on, and writes
  the report to `requel-angular/coverage/`.

These configs should mirror what `e2e-coverage-improvement-plan.md`
already assumes is running. The fact that the plan exists, refers to
specific coverage numbers per feature, and still passes review means
there is an external/working copy of the configs somewhere — recover them
from whichever developer ran the 2026-05-01 coverage rerun rather than
re-deriving from scratch.

### 5.2. Add an `e2e/` test directory

Minimum viable shape:

```
requel-angular/
  e2e/
    auth.spec.ts          # admin / admin login, lands on project list
    project-create.spec.ts # create a project, see it in the sidebar
    project-edit.spec.ts   # open the project, edit a goal, save
```

Three specs is enough to exercise the JWT auth flow, a write command via
the CQRS API, and the SSE refresh path. The feature-level coverage
already in `e2e-coverage-improvement-plan.md` implies many more specs
exist somewhere — fold them in if/when recovered.

### 5.3. Add an `e2e` job to `ci.yml`

The job runs after `build-and-test` succeeds, on the same workflow run.
Reuses the cached Maven and npm dependencies via `actions/setup-*`.

```yaml
  e2e:
    name: E2E + frontend coverage
    needs: build-and-test
    runs-on: ubuntu-latest
    timeout-minutes: 30
    services:
      mysql:
        image: mysql:8.4
        env:
          MYSQL_ROOT_PASSWORD: password
          MYSQL_DATABASE: requel
        ports:
          - 3306:3306
        options: >-
          --health-cmd "mysqladmin ping -ppassword"
          --health-interval 10s
          --health-timeout 5s
          --health-retries 10
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: temurin, java-version: '17', cache: maven }
      - uses: actions/setup-node@v4
        with:
          node-version: '22'
          cache: npm
          cache-dependency-path: requel-angular/package-lock.json
      - name: Write Maven toolchains.xml
        run: |  # same heredoc as build-and-test
          ...
      - name: Build app jar (skip tests, already covered)
        run: >
          mvn -pl modules/requel-app -am package
          -DskipTests --batch-mode --no-transfer-progress
      - name: Boot Requel
        run: |
          java -jar modules/requel-app/target/requel-app-2.0.0.jar \
            --spring.profiles.active=dev \
            --server.port=8081 \
            --spring.datasource.url='jdbc:mysql://127.0.0.1:3306/requel?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC' \
            --spring.datasource.username=root \
            --spring.datasource.password=password &
          npx --yes wait-on http://localhost:8081/api/health -t 120000
      - name: Install Playwright browsers
        working-directory: requel-angular
        run: |
          npm ci
          npx playwright install --with-deps chromium
      - name: Run Playwright with coverage
        working-directory: requel-angular
        run: npm run e2e:coverage
      - name: Upload E2E coverage to Codecov
        if: always()
        uses: codecov/codecov-action@v4
        with:
          files: requel-angular/coverage/lcov.info
          flags: frontend-e2e
          name: monocart-frontend-e2e
          fail_ci_if_error: false
      - name: Upload Playwright report on failure
        if: failure()
        uses: actions/upload-artifact@v4
        with:
          name: playwright-report
          path: requel-angular/playwright-report/
          retention-days: 14
```

Open questions:

- The reset endpoints `/api/dev/reset-admin` and `/api/dev/reset-project`
  documented in `README.md` are gated on the `dev` profile and are exactly
  what Playwright global-setup expects. The job above runs with
  `--spring.profiles.active=dev` for that reason. Confirm with the team
  before assuming this in CI on `master`.
- `wait-on http://localhost:8081/api/health` assumes an actuator health
  endpoint. If `management.endpoints.web.exposure.include` does not
  publish `health`, swap to a known-good HTTP 200 path (e.g. `/login`).

### 5.4. Coverage badge after Phase 3

The Coverage badge URL added in Phase 2 already aggregates across all
flags by default, so once `frontend-e2e` uploads land, the same badge
reflects backend + frontend together.

To show two separate chips, add a flag-scoped second badge:

```markdown
[![Backend coverage](https://codecov.io/gh/rreganjr/Requel/branch/master/graph/badge.svg?flag=backend)](https://codecov.io/gh/rreganjr/Requel)
[![Frontend coverage](https://codecov.io/gh/rreganjr/Requel/branch/master/graph/badge.svg?flag=frontend-e2e)](https://codecov.io/gh/rreganjr/Requel)
```

Recommendation: ship the single combined badge unless reviewers ask for
the split. One green chip is easier to scan than two.

### 5.5. Acceptance criteria

- `requel-angular/playwright.config.ts` and
  `requel-angular/playwright.coverage.config.ts` exist and run locally.
- At least one Playwright spec under `requel-angular/e2e/` passes locally
  against a booted `--spring.profiles.active=dev` server.
- The `e2e` job in `ci.yml` completes green on `master`.
- Codecov shows both `backend` and `frontend-e2e` flag uploads on the
  latest `master` commit, and the Coverage badge reflects the combined
  number.

---

## 6. Risks & Trade-offs

- **Codecov flakiness.** The `codecov/codecov-action` has had occasional
  upload failures in late 2025 when GitHub's API rate-limited the runner.
  `fail_ci_if_error: false` keeps this from breaking the build. If
  flakiness becomes a regular complaint, fall back to the self-hosted
  shields.io dynamic-JSON pattern.
- **Coverage % regressions on the badge.** Per
  `e2e-coverage-improvement-plan.md`, branch coverage on the lowest files
  is in the 30–60% range. The first Codecov publish will set the public
  baseline; consider whether that number is one you want pinned at the
  top of the README for the v2.0 launch. If not, defer Phase 2 until at
  least the items in §"Phase 3" of the existing E2E plan are closed.
- **E2E run time.** A clean Maven build plus Playwright with browser
  install adds ~6–8 minutes to total CI time. The split into two jobs
  (`build-and-test` then `e2e`) lets PRs see Java test results without
  waiting for the E2E lane.
- **Dev profile in CI.** Phase 3 requires running the production JAR
  with `--spring.profiles.active=dev` so the Playwright reset endpoints
  are reachable. This is fine for ephemeral GitHub-hosted runners but
  must never be how the published Docker image runs.

---

## 7. Sequencing

1. **PR 1 (Phase 1).** Add `LICENSE`. Add badge block to `README.md`.
   No CI changes. Reviewable in 5 minutes.
2. **PR 2 (Phase 2).** Add Codecov upload step to `ci.yml`. Link the
   repo on Codecov before merging so the first publish lands cleanly.
3. **PR 3 (Phase 3).** Recover/author Playwright configs and specs.
   Add the `e2e` job to `ci.yml`. Verify both flags appear on Codecov.

Phases 1 and 2 can ship before the v2.0.0 tag. Phase 3 can ship after
v2.0.0 if E2E scaffolding work is not ready in time — the README badge
block is forward-compatible and will pick up the second flag without a
README edit.

---

## 8. References

- `.github/workflows/ci.yml` — existing CI workflow
- `doc/20-release-plan.md` — release orchestration and CI rationale
- `doc/RELEASE_20_TEST_PLAN.md` — backend/frontend/E2E test inventory
- `doc/e2e-coverage-improvement-plan.md` — current frontend coverage targets
- `doc/RELEASE_GUIDE.md` — manual release runbook
- [shields.io](https://shields.io/) — badge URL reference
- [Codecov GitHub Action](https://github.com/codecov/codecov-action) — v4 docs
- [Playwright + Monocart V8 coverage](https://github.com/cenfun/monocart-reporter) — V8 coverage reporting
