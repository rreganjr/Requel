# Release Plan — Requel 2.0

This document describes how to combine in-flight work from multiple GitHub issues into a single 2.0 release, and how to automate the build, container publishing, test execution, and documentation generation in GitHub Actions.

It complements two existing documents:

- `RELEASE_GUIDE.md` — the manual local-machine runbook for cutting a release. This plan replaces most of those manual steps with CI automation, but the runbook remains the authoritative reference for the underlying Maven/Docker commands.
- `RELEASE_20_TEST_PLAN.md` — the test strategy and Java/Angular/E2E test inventory. The CI workflows below run the test suites that document defines.

This plan is concerned only with orchestration: branching, integration, CI, container publishing, and doc generation.

---

## 1. Goals

The release effort needs to deliver four things:

1. A single integration branch where the Angular UI migration (#38) and dependent tickets (#39, #43, and others added during the cycle) can be merged, tested together, and stabilized before promotion to `master`.
2. A GitHub Actions pipeline that builds and tests every push and pull request so the integration branch never drifts into an unbuildable state.
3. An automated container publishing pipeline so that any tagged release produces a `rreganjr/requel:<version>` image on Docker Hub, matching the manual flow described in `README.md` and the `docker-image` Maven profile.
4. Generated, hosted documentation (Javadoc, Angular component docs, and the existing Markdown plans) published to GitHub Pages on every release so the docs stay in sync with the code.

Out of scope: changes to the test strategy itself (covered in `RELEASE_20_TEST_PLAN.md`), Echo2 cleanup (already merged on the #38 branch), and database migration design (covered in `README.md` and `UPGRADE_PLAN.md`).

---

## 2. Issues in Scope

The release targets the following tickets at minimum. Others can be added during the cycle by appending them to the table and opening a PR against the release branch.

| Issue | Title (paraphrased) | Role in the release |
|-------|---------------------|---------------------|
| #38 | Migrate UI from Echo2 to Angular | Foundation. Introduces the Angular SPA, CQRS API, JWT auth, and SSE refresh. All other tickets in this release sit on top of this work. |
| #39 | (Build-on-#38 ticket) | Depends on #38. Merged into the release branch after #38 lands. |
| #43 | (Build-on-#38 ticket) | Depends on #38. Merged into the release branch after #38 lands. |
| TBD | Additional tickets added during stabilization | Merged via PR into the release branch as they complete. |

The defining property of this group is that #39 and #43 (and any later additions) are not viable on `master` alone — they assume the Angular SPA and CQRS API from #38. That is the reason for a long-lived integration branch rather than merging each ticket straight to `master`.

---

## 3. Branching Strategy

The proposed model is a single long-lived **release branch** off `master`, with short-lived **feature branches** for each ticket merged in via pull request.

```
master ────────────────────────────────────────●──────●─→  (tagged v2.0.0 after merge)
              │                                ▲      ▲
              └─→ release/2.0 ─●──●──●──●──●───┘ rc1  rc2
                               ▲  ▲  ▲  ▲  ▲
                               │  │  │  │  │
                            #38  #39 #43 #44 #45  ← feature branches merged via PR
```

### 3.1 Create the release branch

Cut `release/2.0` from the current state of the #38 feature branch. The #38 branch already contains the Echo→Angular migration that everything else depends on, so it forms the natural base.

```bash
git checkout 38-migrate-ui-from-echo2-to-angular
git pull origin 38-migrate-ui-from-echo2-to-angular
git checkout -b release/2.0
git push -u origin release/2.0
```

Once `release/2.0` exists, treat the original `38-migrate-ui-from-echo2-to-angular` branch as frozen — further work on #38 itself happens via small fix-up PRs into `release/2.0`.

### 3.2 Configure branch protection on `release/2.0`

In **Settings → Branches → Branch protection rules**, add a rule for `release/2.0`:

- Require a pull request before merging (1 approval).
- Require status checks to pass: `build-and-test` (the CI workflow added in §5.1).
- Require branches to be up to date before merging.
- Restrict who can push directly (admins only, for emergency fixes).

Apply the same protection to `master`, but with a stricter rule: `build-and-test` and `container-publish` (smoke test) must both pass.

### 3.3 Feature-branch workflow per ticket

For each new ticket, branch from `release/2.0`, work, and PR back:

```bash
git checkout release/2.0
git pull
git checkout -b 39-some-feature
# ...do the work, commit...
git push -u origin 39-some-feature
gh pr create --base release/2.0 --title "#39 Some feature" --body "Closes #39"
```

PR titles should start with the issue number (`#39 …`) so they correlate with `commit.md` per the project's existing convention (see `CLAUDE.md`).

Rebasing onto `release/2.0` is preferred over merging — it keeps the integration history linear and makes the eventual squash-merge to `master` clean. If a feature branch is long-lived, schedule a weekly rebase against `release/2.0`.

### 3.4 Promotion to `master`

When the release branch is feature-complete and green:

1. Bump the Maven version from `2.0.0-dev` to `2.0.0-rc1` (see §4).
2. Tag `v2.0.0-rc1` from the tip of `release/2.0`. CI publishes the RC container and docs.
3. Run the smoke checks from `RELEASE_GUIDE.md` §6–7 against the RC image.
4. Iterate on `release/2.0` if needed (more PRs, more RC tags) until stable.
5. Bump to `2.0.0` (no suffix) and tag `v2.0.0`.
6. Open a PR `release/2.0 → master`. Use a **merge commit** (not squash) so the integration history is preserved on `master`.
7. CI publishes `rreganjr/requel:2.0.0` and `:latest`, the GitHub Release with notes, and the docs site.
8. Delete `release/2.0` or leave it for back-porting 2.0.x patches.

### 3.5 Why not merge each ticket straight to `master`?

A direct-to-`master` model would force #39 and #43 to either include their own copy of the #38 changes (huge merge conflicts) or wait for #38 to land first (serial, not parallel). The release branch lets the three streams stabilize together and lets the team validate cross-ticket interactions before any of it reaches `master`. The cost is one extra long-lived branch — acceptable for a release of this size.

---

## 4. Versioning

The root POM is currently `2.0.0-dev`. Adopt the following sequence:

- `2.0.0-dev` — current development version on `release/2.0`. Stays unchanged for the bulk of the cycle.
- `2.0.0-rc1`, `-rc2`, … — release-candidate tags from `release/2.0` once feature-complete. Each RC produces a real container and docs site so they can be exercised by stakeholders.
- `2.0.0` — the final tag on `master` after the release branch merges back.
- `2.0.1`, `2.0.2`, … — patches, branched from `master` (or back-ported through `release/2.0` if still alive).

Version bumps are done with the Maven Versions plugin so the change touches every module pom in one commit:

```bash
mvn versions:set -DnewVersion=2.0.0-rc1 -DgenerateBackupPoms=false
mvn versions:commit
# update references in README.md, RELEASE_GUIDE.md, docker-compose.yml as needed
git commit -am "Bump to 2.0.0-rc1"
git tag -a v2.0.0-rc1 -m "Release candidate 1"
git push origin release/2.0 v2.0.0-rc1
```

Tag format is `v<MAJOR>.<MINOR>.<PATCH>[-rcN]`. The CI workflows below trigger on tags matching `v*`.

---

## 5. GitHub Actions Workflows

Four workflows cover the automation needs. All YAML lives in `.github/workflows/`. Skeletons are in §10; this section explains what each one does and when it fires.

### 5.1 `ci.yml` — build and test on every push and PR

Fires on push to any branch and on pull requests targeting `master` or `release/*`. Responsible for:

- Setting up JDK 17 (Temurin) and Node 22.
- Caching `~/.m2` and `requel-angular/node_modules`.
- Running `mvn -pl modules/requel-app -am verify` — this builds the full Angular SPA, builds every Java module, and runs the JUnit 5 + IT suites described in `RELEASE_20_TEST_PLAN.md`.
- Uploading the JaCoCo HTML report and Surefire/Failsafe reports as workflow artifacts.
- Running the Angular Vitest suite (`cd requel-angular && npm ci && npm test -- --run`).
- Optionally running Playwright E2E against a docker-compose stack (matrix job, can be gated on a `[e2e]` label so it doesn't run on every PR).

The job must complete in roughly ten minutes for the integration branch model to be usable; the Maven and npm caches are critical to that target.

### 5.2 `container-publish.yml` — Docker Hub image on every tag

Fires on tags matching `v*`. Responsible for:

- Re-running the full Maven build with `-Pdocker-image` to produce the JAR and build the local image, matching the `README.md` flow.
- Logging in to Docker Hub using `DOCKERHUB_USERNAME` / `DOCKERHUB_TOKEN` secrets.
- Tagging and pushing `rreganjr/requel:<tag-without-v>` (e.g. `2.0.0-rc1`, `2.0.0`).
- For non-RC tags, additionally tagging and pushing `rreganjr/requel:latest`.
- Running a smoke test: bring up `docker-compose.yml` against the just-built image, `curl` the health endpoint, then tear down. Fail the workflow if the container doesn't respond.

This workflow replaces steps 4–8 of `RELEASE_GUIDE.md` end-to-end. The manual runbook stays valid as a fallback for emergency releases when Actions is unavailable.

### 5.3 `docs-publish.yml` — generate and publish documentation

Fires on tags matching `v*` and on pushes to `master`. Responsible for:

- Generating aggregate Javadoc with the Maven Javadoc plugin (see §7.1).
- Generating Angular component/service documentation with Compodoc (see §7.2).
- Rendering the Markdown plans in `doc/` to HTML with MkDocs Material (see §7.3).
- Optionally rendering DocBook content under `doc/docbook/en-US/` if that pipeline is still active.
- Publishing the combined site to GitHub Pages from `gh-pages` branch using `peaceiris/actions-gh-pages` or the official `actions/deploy-pages` flow.

The output URL becomes `https://rreganjr.github.io/Requel/`, with `/javadoc/`, `/angular/`, and `/guide/` subpaths for the three doc sets. Link to it from `README.md` once it's live.

### 5.4 `release.yml` — GitHub Release with notes

Fires on tags matching `v*`. Responsible for:

- Auto-generating release notes from PRs merged into `release/2.0` since the previous tag, using `actions/create-release` or `softprops/action-gh-release` with the `generate_release_notes: true` option.
- Attaching the built JAR (`modules/requel-app/target/requel-app-<version>.jar`) and the Angular bundle as release assets.
- Marking the release as pre-release if the tag contains `-rc`.

This replaces RELEASE_GUIDE.md step 10 (manual GitHub tag/release creation).

### 5.5 Required secrets

Configure these under **Settings → Secrets and variables → Actions**:

- `DOCKERHUB_USERNAME` — the Docker Hub account name (e.g. `rreganjr`).
- `DOCKERHUB_TOKEN` — Docker Hub access token with `Read, Write, Delete` scope.
- `GITHUB_TOKEN` — provided automatically; needs `contents: write` and `pages: write` permissions, which are granted per-job in the workflow YAML.

No secrets are needed for Maven publishing in this plan because there is no current need to publish artifacts to GitHub Packages; if that changes later, add `GITHUB_TOKEN` write-packages scope and a `settings.xml` step (see the v1.2 `RELEASE.md`).

---

## 6. Container Build & Push — Mechanics

The current `docker-image` Maven profile in `modules/requel-app/pom.xml` invokes `docker build` via `exec-maven-plugin` and produces `rreganjr/requel:${project.version}`. The CI workflow uses that same profile so local-vs-CI builds stay byte-for-byte equivalent.

### 6.1 Build steps in CI

```yaml
- name: Build JAR and Docker image
  run: mvn -pl modules/requel-app -am package -Pdocker-image -DskipTests

- name: Tag for Docker Hub
  run: |
    VERSION="${GITHUB_REF_NAME#v}"
    docker tag rreganjr/requel:${VERSION} rreganjr/requel:${VERSION}
    if [[ ! "$VERSION" =~ -rc ]]; then
      docker tag rreganjr/requel:${VERSION} rreganjr/requel:latest
    fi

- name: Login & push
  run: |
    echo "${{ secrets.DOCKERHUB_TOKEN }}" | docker login -u "${{ secrets.DOCKERHUB_USERNAME }}" --password-stdin
    docker push rreganjr/requel:${VERSION}
    if [[ ! "$VERSION" =~ -rc ]]; then docker push rreganjr/requel:latest; fi
```

### 6.2 Optional: multi-arch images

The current Dockerfile produces a single-architecture image (whatever the runner uses — usually amd64). If multi-arch is needed for Apple Silicon hosts, switch to `docker/build-push-action@v5` with `platforms: linux/amd64,linux/arm64`. That requires moving image construction out of the Maven profile and into a dedicated workflow step. Defer this until there's a concrete need.

### 6.3 Smoke test before pushing

To avoid publishing broken images, run the existing `docker-compose.yml` against the freshly built local image and verify the server responds before the push step:

```yaml
- name: Smoke test the image
  run: |
    docker compose up -d
    for i in {1..30}; do
      curl -fsS http://localhost:8080/actuator/health && break
      sleep 5
    done
    docker compose down
```

If the health probe never succeeds, the workflow fails and nothing is pushed.

---

## 7. Documentation Generation

The repository ships a lot of hand-written Markdown under `doc/` plus DocBook XML, PDFs, and architecture diagrams. None of it is currently auto-generated. The goal here is to add three generated doc sets and publish all of it as a single site.

### 7.1 Javadoc for the Java modules

Add the Maven Javadoc plugin to the root POM, configured for aggregate output across all modules:

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-javadoc-plugin</artifactId>
  <version>3.6.3</version>
  <configuration>
    <doclint>none</doclint>
    <failOnError>false</failOnError>
    <quiet>true</quiet>
  </configuration>
</plugin>
```

Then in CI:

```bash
mvn -DskipTests javadoc:aggregate
# output ends up in target/site/apidocs/
```

`doclint=none` is important because the original 2009 codebase has Javadoc warnings that would otherwise fail the build. `failOnError=false` lets Javadoc finish even when individual modules have issues; the build log surfaces them for cleanup over time.

### 7.2 Angular docs with Compodoc

Compodoc generates a navigable site from TypeScript JSDoc comments, component templates, and module structure. Add it to `requel-angular/package.json`:

```json
"scripts": {
  "docs": "compodoc -p tsconfig.app.json -d ../target/site/angular --hideGenerator"
},
"devDependencies": {
  "@compodoc/compodoc": "^1.1.24"
}
```

Then in CI: `cd requel-angular && npm ci && npm run docs`.

Compodoc output lands alongside the Javadoc under `target/site/`, ready to be combined.

### 7.3 Markdown plans rendered with MkDocs

The existing `doc/*.md` files are valuable architecture references but are awkward to read on GitHub for new contributors. MkDocs Material renders them as a searchable static site. Add a top-level `mkdocs.yml`:

```yaml
site_name: Requel Documentation
docs_dir: doc
site_dir: target/site/guide
theme:
  name: material
nav:
  - Overview: index.md            # symlink or copy of README.md
  - Architecture:
    - Modularization: MODULARIZATION_PLAN.md
    - UI refactor: UI_REFACTOR_PLAN.md
    - Authorization: AUTH_ARCH.md
    - Unmarshalling: unmarshalling_plan.md
  - Releases:
    - Release plan: 20-release-plan.md
    - Release guide: RELEASE_GUIDE.md
    - Test plan: RELEASE_20_TEST_PLAN.md
  - User and stakeholder model: USER_AND_STAKEHOLDER_MODEL.md
```

CI build step: `pip install mkdocs-material && mkdocs build`.

### 7.4 Combined site layout

After all three generators run, the final site looks like:

```
target/site/
├── index.html          # landing page linking the three sub-sites
├── apidocs/            # Javadoc
├── angular/            # Compodoc
└── guide/              # MkDocs Material
```

A trivial `index.html` (committed to `doc/_site_index.html`) gets copied to `target/site/index.html` as the first step of `docs-publish.yml`. The whole `target/site/` directory is then deployed to GitHub Pages.

### 7.5 DocBook (optional)

`doc/docbook/en-US/` contains the original 2009 thesis and user-guide sources. If the team wants those re-rendered as part of the build, add a `pandoc` step or restore the original `docbook-maven-plugin`. This is low priority — the PDFs already exist in `doc/` — so it's listed here as an option, not a requirement.

---

## 8. Cutover Plan

Concrete sequence of steps to move from where the repo is today to the full automated pipeline. None of these steps require code changes outside the release branch.

1. **Land the integration branch.** Cut `release/2.0` from the tip of `38-migrate-ui-from-echo2-to-angular`. Push and add branch protection (§3.2). Open small PRs against `release/2.0` for any in-flight fixups on #38 itself.
2. **Add `ci.yml`.** Submit it as a PR to `release/2.0`. Verify it goes green on the PR, then merge. From this point forward, every PR into `release/2.0` is gated on a clean build.
3. **Merge #39 and #43.** Each as a feature branch off `release/2.0`, PR back in, CI gate, merge.
4. **Add Javadoc and Compodoc configuration.** Separate PR to `release/2.0`. Verify locally that `mvn javadoc:aggregate` and `npm run docs` produce output. Don't add `docs-publish.yml` yet.
5. **Add `mkdocs.yml` and the site-index page.** Verify `mkdocs build` produces a usable site locally.
6. **Add `docs-publish.yml` and enable GitHub Pages.** Set the Pages source to `gh-pages` branch in Settings. Push a throwaway tag (`v2.0.0-pre1` deleted afterward) and verify the docs site renders at `https://rreganjr.github.io/Requel/`.
7. **Add `container-publish.yml` and `release.yml`.** Configure Docker Hub secrets first. Push `v2.0.0-rc1`. Verify the image appears on Docker Hub and the GitHub Release page lists the tag with auto-generated notes.
8. **Stabilize, iterate on RCs.** Cut additional RC tags as needed. Each one exercises the full pipeline end-to-end.
9. **Promote to `master`.** Bump to `2.0.0`, tag, PR to `master` with merge commit. CI publishes `:latest` and the final docs.

---

## 9. Risks and Trade-offs

A few things to watch during the cycle:

The release branch can drift from `master` if `master` receives unrelated commits. Mitigation: forbid direct pushes to `master` during the 2.0 cycle except for documentation fixes, and rebase `release/2.0` onto `master` weekly. If `master` is genuinely frozen until 2.0 ships, this isn't an issue.

The CI build cost grows quickly once Angular + Playwright are in the mix. Caching is essential, and the E2E suite should be opt-in (label-gated) until the test base settles. The numbers in `RELEASE_20_TEST_PLAN.md` §4 suggest the full Playwright suite will be substantial.

Docker Hub rate limits anonymous pulls. The smoke test in `container-publish.yml` pulls MySQL on every run; if rate limits start biting, switch to a GitHub Container Registry mirror or authenticate the pulls.

The `2.0.0-dev` → `2.0.0-rc1` → `2.0.0` version bumps each require a commit that touches dozens of POMs. Use the Maven Versions plugin (§4) rather than hand-editing, and keep these commits separate from feature work so they're easy to revert if needed.

Documentation generation introduces three new toolchains (Javadoc, Compodoc, MkDocs Material). Each can fail independently. Configure `docs-publish.yml` so a single generator's failure fails the whole workflow loudly — half-published docs are worse than no docs.

---

## 10. Appendix — Workflow Skeletons

These are starting points, not finished workflows. They need adjustment based on the actual cache hit rate and the final E2E configuration. Drop them into `.github/workflows/` and refine on the first run.

### 10.1 `.github/workflows/ci.yml`

```yaml
name: CI
on:
  push:
    branches: [master, "release/**"]
  pull_request:
    branches: [master, "release/**"]

jobs:
  build-and-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: "17"
          cache: maven
      - uses: actions/setup-node@v4
        with:
          node-version: "22"
          cache: npm
          cache-dependency-path: requel-angular/package-lock.json
      - name: Maven build + tests
        run: mvn -pl modules/requel-app -am verify
      - name: Angular tests
        run: |
          cd requel-angular
          npm ci
          npm test -- --run
      - name: Upload coverage
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: jacoco-report
          path: "**/target/site/jacoco/"
      - name: Upload surefire reports
        if: failure()
        uses: actions/upload-artifact@v4
        with:
          name: surefire-reports
          path: "**/target/surefire-reports/"
```

### 10.2 `.github/workflows/container-publish.yml`

```yaml
name: Container Publish
on:
  push:
    tags: ["v*"]

jobs:
  publish:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: "17"
          cache: maven
      - uses: actions/setup-node@v4
        with:
          node-version: "22"
          cache: npm
          cache-dependency-path: requel-angular/package-lock.json
      - name: Build JAR and local Docker image
        run: mvn -pl modules/requel-app -am package -Pdocker-image -DskipTests
      - name: Smoke test
        run: |
          docker compose up -d
          for i in {1..30}; do
            curl -fsS http://localhost:8080/actuator/health && exit 0 || sleep 5
          done
          docker compose logs
          exit 1
      - name: Tear down
        if: always()
        run: docker compose down
      - name: Login to Docker Hub
        run: echo "${{ secrets.DOCKERHUB_TOKEN }}" | docker login -u "${{ secrets.DOCKERHUB_USERNAME }}" --password-stdin
      - name: Tag and push
        run: |
          VERSION="${GITHUB_REF_NAME#v}"
          docker push rreganjr/requel:${VERSION}
          if [[ ! "$VERSION" =~ -rc ]]; then
            docker tag rreganjr/requel:${VERSION} rreganjr/requel:latest
            docker push rreganjr/requel:latest
          fi
```

### 10.3 `.github/workflows/docs-publish.yml`

```yaml
name: Docs Publish
on:
  push:
    branches: [master]
    tags: ["v*"]

permissions:
  contents: read
  pages: write
  id-token: write

jobs:
  build-and-publish:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: "17"
          cache: maven
      - uses: actions/setup-node@v4
        with:
          node-version: "22"
          cache: npm
          cache-dependency-path: requel-angular/package-lock.json
      - uses: actions/setup-python@v5
        with:
          python-version: "3.12"
      - name: Generate Javadoc
        run: mvn -DskipTests javadoc:aggregate
      - name: Generate Compodoc
        run: |
          cd requel-angular
          npm ci
          npm run docs
      - name: Build MkDocs site
        run: |
          pip install mkdocs-material
          mkdocs build
      - name: Combine site
        run: |
          mkdir -p target/site
          cp doc/_site_index.html target/site/index.html
          mv target/site/apidocs target/site/apidocs || true
      - uses: actions/upload-pages-artifact@v3
        with:
          path: target/site
      - uses: actions/deploy-pages@v4
```

### 10.4 `.github/workflows/release.yml`

```yaml
name: GitHub Release
on:
  push:
    tags: ["v*"]

permissions:
  contents: write

jobs:
  release:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: "17"
          cache: maven
      - uses: actions/setup-node@v4
        with:
          node-version: "22"
          cache: npm
          cache-dependency-path: requel-angular/package-lock.json
      - name: Build JAR
        run: mvn -pl modules/requel-app -am package -DskipTests
      - name: Create GitHub Release
        uses: softprops/action-gh-release@v2
        with:
          generate_release_notes: true
          prerelease: ${{ contains(github.ref_name, '-rc') }}
          files: |
            modules/requel-app/target/requel-app-*.jar
```

---

## 11. Tracking the Release in GitHub: Milestones, Projects, Releases

GitHub gives three primitives for organizing release work, and they are often confused as alternatives. They aren't — each one solves a different problem, and a healthy release uses all three together.

### 11.1 Milestone — track scope and completion

A milestone is the right primitive for "what work belongs in this release." It is a named bucket that issues and PRs can be assigned to, and GitHub renders the bucket as a single page at `/milestones/<name>` with a progress bar based on open vs. closed items.

For Requel 2.0, create a milestone called `v2.0` and assign every in-scope issue to it — `#38`, `#39`, `#43`, plus anything added during the cycle. Set a target date (it's only a hint, easy to adjust). From that point on, the milestone page is the canonical answer to "how close is 2.0 to done?"

Milestones also feed the auto-generated release notes in §5.4 — items closed under a milestone can be filtered into the changelog without manual curation.

Create one milestone per release: `v2.0`, `v2.0.1`, `v2.1`. Don't reuse them.

### 11.2 Project (Projects v2) — track workflow

A milestone tells you what's in scope. A Project tells you where each item is in the pipeline — Backlog, In Progress, In Review, Merged to `release/2.0`, Verified on RC, Done. PRs and issues can live on the same board, and custom fields (priority, assignee, target RC) make filtering easy.

A Project is useful when there are enough parallel work streams that the milestone's flat list stops being readable. For a release with three contributors and ten issues, a board is genuinely helpful. For a release with one contributor and three issues, a board is overhead.

When a Project does make sense, prefer **one Project that spans multiple releases**, with a `Milestone` filter. That way the same board carries through `v2.0` → `v2.0.1` → `v2.1` without recreating columns each time.

### 11.3 Release — the published artifact

A GitHub Release is the *output* of the cycle, not a planning tool. It lives at `/releases`, is tied to a Git tag, and shows the changelog plus any attached binaries (the JAR, in our case — see §10.4). Users who land on the repo find Releases first; that's where they get download links and the matching Docker image tag.

Releases are created automatically by the `release.yml` workflow (§5.4) when a `v*` tag is pushed. Don't create them by hand in the GitHub UI — the workflow attaches the JAR, generates the notes, and marks `-rc` tags as pre-releases, all of which are easy to forget manually.

### 11.4 How they connect

The clean flow ties all three together:

1. An issue is filed and assigned to the `v2.0` milestone.
2. The issue is added to the Project board in the Backlog column (if a Project is in use).
3. A feature branch is opened and a PR references the issue (`Closes #39`).
4. The PR moves across the Project board as it progresses.
5. The PR merges into `release/2.0`, which closes the issue; the milestone progress bar advances; the Project board moves the card to Done.
6. When the milestone is at or near 100%, the version is bumped and `v2.0.0-rc1` is tagged on `release/2.0`. The Release workflow publishes a pre-release with auto-generated notes pulling from the merged PRs.
7. After RC validation, the version is bumped to `v2.0.0`, the branch merges to `master`, and the final Release is published.

Each primitive plays its part: the milestone proves the work is in scope, the Project shows it moved through the pipeline, and the Release is what the world sees.

### 11.5 Recommendation for Requel

Start with **just the milestone**. Create `v2.0` today, assign `#38`, `#39`, and `#43` to it, set a tentative target date. That alone gives a real release dashboard with no extra ceremony.

Add a Project only if scope grows past roughly ten issues or if multiple people start working in parallel and need a shared status view. Until then, the milestone page plus the PR list is enough signal.

The Release itself is automatic via the workflow in §10.4 — no GitHub UI work is needed.

---

## 12. Open Questions

These need decisions before the plan can be executed end-to-end:

- Should `release/2.0` be cut from `38-migrate-ui-from-echo2-to-angular` (as proposed) or from `master` with #38 cherry-picked in? The former is faster but carries the full history of #38; the latter is cleaner but is a larger one-time merge. Recommendation: keep #38's history.
- Are #39 and #43 already in flight as their own branches, and if so, do they need rebasing onto `release/2.0` first? Answer determines step 3 of the cutover plan.
- Should the E2E Playwright suite run on every PR, or only on `release/*` branches and nightly? Answer drives `ci.yml` matrix configuration. Recommendation: nightly + `[e2e]` label until the suite stabilizes.
- Are there existing Docker Hub credentials configured for `rreganjr`, or do we need to provision a new automation token? Required before `container-publish.yml` can run.
- Should documentation deploy to `https://rreganjr.github.io/Requel/` (project Pages) or a custom domain? Affects only the Pages settings — workflow is identical either way.
