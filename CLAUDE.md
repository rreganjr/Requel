# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Requel is a web-based requirements management system supporting collaboration among stakeholders with automated NLP-based assistance. It models requirements as goals, stories, actors, scenarios, and use-cases with an IBIS-style annotation/discussion layer. Originally a 2009 Harvard ALM thesis project, now modernized to Spring Boot 3 / Java 17.


## Process Instructions

Always write commit messages to commit.md at the root of the project. Clear out existing content unless you are updating the message as told. In the file following the format:
```
https://github.com/rreganjr/Requel/issues/38

<short description, for example what tasks from what plan were implemented>

<details - file specific changes, including explaination when fixing bugs of what was broken and how the fix solves the problem>
```

Claude (re)writes `commit.md` to describe the current change before each commit. `commit.md` is
gitignored (`/commit.md`) and is **never committed** — it is the source for the commit message,
consumed via `git commit -F commit.md` (or copy/pasted into the VS Code commit box). When the PR
body should differ from the commit message (a PR summary, scope notes, a "Closes #<n>"), write it
to `pr.md` — also gitignored (`/pr.md`), also never staged — and pass it as `gh pr create
--body-file pr.md`. **Refresh `pr.md`/`commit.md` immediately before the command that consumes it**;
a stale reuse (e.g. last ticket's `pr.md`) is an easy and embarrassing mistake. Stage only the
changed source files — never `git add commit.md`/`pr.md`, and never `git add -A` (it would sweep
them in).

Never commit unless explicitly told to commit

Never push changes to the github repo

**"Let's commit" / "let's push" means prepare, then hand over the commands.** Claude does not run
`git` or `gh` commands that change state — not commit, push, branch, tag, PR, issue edit, or
comment — even when told to. On either phrase Claude:

1. Checks the working tree holds what it should and that the verification gate in step 3 of the
   Development Workflow has passed.
2. (Re)writes `commit.md` for the current change, in the format above.
3. Prints the exact commands to paste, with nothing left to fill in: `git add` naming each changed
   file, `git commit -F commit.md`, `git push -u origin <branch>`, `gh pr create ...`.

Read-only git — `log`, `show`, `status`, `diff`, `rev-parse` — is fine for Claude to run, but
always with `--no-optional-locks`. When Claude reaches the repo over the Cowork device bridge it
cannot delete files, so any command that takes `.git/index.lock` leaves it behind and blocks the
next git operation from the developer's own terminal.

All plans, reviews, notes and documentation go in the doc folder.

Never use `TL;DR` I hate that abbreviation, use `Summary`

### Development Workflow

Each release such as `release/2.0` has a GitHub project (**Requel 2.0**, `github.com/users/rreganjr/projects/2`); all issues for the release are added to it. We use the **Story Points** (estimate) and **Story Points (Retro)** (actual, filled after merge) custom fields to track effort.

Every change is tied to a GitHub issue and lands via a ticket branch and a PR — never commit straight onto `release/2.0`. **Steps that create or change Git/GitHub state (branch, commit, push, PR, issue edits, project-field edits, merges) are always run by the developer, never by Claude.** Claude prepares them — reviews, plans, writes `commit.md`, works out the exact `git`/`gh` invocation, checks the tree is in the state the command assumes — and hands the commands over to paste. See the "let's commit / let's push" rule above.

**The ticket lifecycle, every time:**

1. **Review the ticket for completeness** — before any code. Pull the issue and read it against the current state of the tree (ACs may have drifted as sibling tickets merged). Surface open questions and get decisions before planning. Claude does the review; the developer runs the read-only pull:
   ```bash
   gh issue view <n> --repo rreganjr/Requel --json number,title,body,labels,state > tmp/issue-<n>.json
   ```
   Report: is it still needed, have the ACs changed, any blockers/decisions needed. Ask the open questions and wait for answers.
2. **Write an implementation plan** — a house-style doc at `doc/<n>-<slug>-plan.md` (scope/locked decisions, contracts, step-by-step, test plan, out-of-scope, risks, AC mapping). All plans/reviews/notes live in `doc/`. Get a thumbs-up on the plan (and on any risky decisions, e.g. a route move) before coding. For a large ticket, split into stacked sub-PRs in the plan (§ "Stacked PRs" below).
3. **Branch (at the start of work)** — cut from `release/2.0`, named `<issue-number>-<short-slug>` (e.g. `142-route-groups`, `128-154-app-shell`). All edits on that branch. (Claude may create the branch over the device bridge with `git switch -c` / `git checkout -b` — those don't leave a lock — but never `git branch -D`, `git rebase`, `git stash`, or anything else that writes refs/index over the bridge; hand those to the developer.)
4. **Implement.** Keep a per-ticket verify script at `tmp/<n>-verify.sh` (gitignored) that runs the exact gates below, so the developer runs one command.
5. **Verify — the gate. All relevant suites must pass before committing:**
   - **Java (backend):** `mvn clean verify` green — whenever any `modules/**` changed.
   - **JS unit (frontend):** whenever `requel-angular/**` changed. Run **once, non-watch** — vitest defaults to an interactive watch that stops at a `q` prompt, so always pass `--watch=false` and export `CI=1`:
     ```bash
     cd requel-angular
     CI=1 npx ng test --watch=false --include='src/app/**/<the-specs-you-touched>.spec.ts'
     # or the whole suite: CI=1 npm test -- --watch=false
     ```
   - **Typecheck** (fast, catches template/wiring errors unit tests miss): `npx tsc -p tsconfig.app.json --noEmit && npx tsc -p tsconfig.spec.json --noEmit`, and for route/lazy/template changes a dev build: `npx ng build --configuration development`.
   - **e2e (Playwright):** required whenever routing, navigation, page objects, or user-visible flows changed — the unit suite will *not* catch a broken route or a page-object locator. e2e needs the full stack, so it normally runs in **CI**; read a failed run's report instead of guessing:
     ```bash
     # from the CI e2e coverage JSON, list the failing cases + locations
     python3 - <<'EOF'
     import json; d=json.load(open("e2ecoverage.json"))
     def walk(ns):
       for n in ns:
         if n.get('status')=='failed' and '.e2e.ts' in str(n.get('location','')):
           print(n['location'], '::', n.get('title'))
         for k in ('subs','rows','children'):
           if isinstance(n.get(k),list): walk(n[k])
     walk(d['rows'])
     EOF
     ```
     When the route/behavior changed **intentionally**, updating the e2e that encoded the old behavior is correct (not a regression); when the change was meant to be behavior-preserving, a red e2e is a real regression — fix the code, not the test.
   Do not commit until the relevant suites pass.
6. **Commit message** — (re)write `commit.md` in the format under **Process Instructions**, with a closing keyword (`Closes #<n>`). `commit.md` is gitignored and never staged.
7. **Commit + push** — Claude hands over `git add <changed files>` (name them; never `git add -A`, which would sweep in `commit.md`/`pr.md`), `git commit -F commit.md`, `git push -u origin <branch>`. When the PR body differs from the commit message, write it to `pr.md` (also gitignored) and refresh it *before* the `gh pr create`/`gh pr edit` — a stale `pr.md` is an easy mistake.
8. **PR** — Claude hands over `gh pr create --base release/2.0 --head <branch>`, always with `--title` (`<issue#>: <concise summary>`) and `--body-file pr.md`. PRs are squash-merged.
9. **Merge** — developer merges once CI (including e2e) is green. `--admin` overrides branch protection on `release/2.0` when needed; auto-merge is disabled on this repo.
10. **Close the issue** — `release/2.0` is not the default branch, so `Closes #<n>` does **not** auto-close on merge. After merge:
    ```bash
    gh issue close <n> --repo rreganjr/Requel --reason completed       --comment "Merged to release/2.0 via #<pr>."
    ```
11. **Sync the epic rollup** (for tickets under a rollup like #124):
    ```bash
    bash scripts/reorder-ui-ux-subissues.sh --sync-checks --comment
    ```
    It reconciles the rollup doc's checkboxes/progress from **issue-closed state** (not PR-merged state), so run it *after* step 10.
12. **Story Points (Retro)** — record actual effort in the project's **Story Points (Retro)** field so estimate-vs-actual stays honest:
    ```bash
    OWNER=rreganjr; NUM=2; REPO=rreganjr/Requel
    PROJECT_ID=$(gh project view "$NUM" --owner "$OWNER" --format json | jq -r '.id')
    RETRO_ID=$(gh project field-list "$NUM" --owner "$OWNER" --limit 100 --format json              | jq -r '.fields[] | select(.name=="Story Points (Retro)") | .id')
    ITEM_ID=$(gh project item-add "$NUM" --owner "$OWNER"              --url "https://github.com/$REPO/issues/<n>" --format json | jq -r '.id')
    gh project item-edit --project-id "$PROJECT_ID" --id "$ITEM_ID" --field-id "$RETRO_ID" --number <actual>
    ```
    (`--project-id` wants the `PVT_…` node ID; needs a `project`-scoped token — `gh auth refresh -s project` if `field-list` 403s.)

**Stacked PRs (large tickets split into sub-PRs).** Split a big ticket in the plan (e.g. `128-154-app-shell` → chrome, breadcrumb, resolver, workspace). Each sub-PR is its own branch; base each on the one below (`--base <lower-branch>`) or on `release/2.0` if the lower one already merged. **Merge bottom-up, and rebase the next branch after each squash-merge** — because squash rewrites SHAs, a plain `git rebase release/2.0` replays the already-merged commits and conflicts. Use `--onto` with the *old* tip of the branch that just merged:
```bash
# after the branch below (old tip <OLD_BASE_TIP>) squash-merged into release/2.0:
git checkout release/2.0 && git pull
git rebase --onto release/2.0 <OLD_BASE_TIP> <this-branch>
git push --force-with-lease
gh pr edit <pr> --repo rreganjr/Requel --base release/2.0
```
Prefer merging each sub-PR as it goes green rather than letting the stack grow deep.

**Device-bridge git caveat.** Over the Cowork bridge Claude cannot delete files, so any git command that takes `.git/*.lock` (notably `git status`, `git branch -D`, an interrupted `checkout`/`rebase`) leaves the lock behind and blocks the developer's terminal. Claude therefore: uses only read-only plumbing (`git show`, `git rev-parse`, `git ls-files`, `git log`) or `diff <(git show ref:path) path`, avoids `git status`, and hands **all** state-changing git to the developer. If a lock is stranded, the developer clears it: `rm -f .git/index.lock .git/*.lock .git/refs/**/*.lock`.

**Auto-close caveat:** the repo's default branch is `master`, but PRs target `release/2.0`. GitHub auto-closes an issue from `Closes #<n>` only when the PR merges into the **default** branch, so merging into `release/2.0` does **not** close the issue — always do step 10 explicitly.

Command reference — all run by the developer, in the developer's environment, not Claude's sandbox. Claude fills placeholders and hands them over:

```bash
# 0. Review:  gh issue view <n> --repo rreganjr/Requel --json number,title,body,labels,state > tmp/issue-<n>.json
# 1. Plan:    doc/<n>-<slug>-plan.md  (then get sign-off)
# 2. Branch:  git switch -c <issue#>-<slug> release/2.0
# 3. Verify (must pass — run what the change touched):
mvn clean verify                                              # backend (modules/**)
cd requel-angular && CI=1 npm test -- --watch=false           # frontend unit (requel-angular/**)
# e2e runs in CI; read a failed run's e2ecoverage.json (see step 5 above)
# 4. commit.md with a "Closes #<n>" line (gitignored — never `git add` it)
# 5. Commit + push (stage only changed source files):
git add <changed files> && git commit -F commit.md && git push -u origin <issue#>-<slug>
# 6. PR (always --title; body from pr.md, refreshed first):
gh pr create --repo rreganjr/Requel --base release/2.0 --head <issue#>-<slug>   --title "<issue#>: <concise summary>" --body-file pr.md
# 7. After squash-merge (release/2.0 is not default → close manually):
gh issue close <n> --repo rreganjr/Requel --reason completed --comment "Merged to release/2.0 via #<pr>."
# 8. Rollup + retro:
bash scripts/reorder-ui-ux-subissues.sh --sync-checks --comment
#    (then set Story Points (Retro) on project #2 — see step 12)
```

Recovery — work accidentally committed onto `release/2.0`:

- **Not yet pushed:** move the commits to a branch and rewind local `release/2.0`:
  `git branch <issue#>-<slug>` then `git reset --hard origin/release/2.0` then
  `git switch <issue#>-<slug>` and continue at step 5 above.
- **Already pushed to `origin/release/2.0`:** do **not** rewrite the shared branch. The work is
  integrated; just close the issue with a comment linking the commit
  (`gh issue close <n> --comment "Done in <sha> on release/2.0: ..."`).

## Build Commands

Requires **Java 17**, **Maven 3.6.3+**, and **Node 22+**. Set `JAVA_HOME` accordingly.

```bash
# Full build (Java + Angular, skips tests)
mvn -pl modules/requel-app -am package -DskipTests

# Fast iterative build (Java only, skips Angular and tests)
mvn -pl modules/requel-app -am package -DskipAngularBuild=true -DskipTests=true

# Run all tests
mvn test

# Run a single test class
mvn -pl modules/requel-app -am test -Dtest=ProjectXmlRoundTripIT

# Run a single module's tests
mvn -pl modules/project-jpa test

# Build Docker image
mvn -pl modules/requel-app -am package -Pdocker-image -DskipTests
```

## Running Locally

The jar is named for the reactor version in the root `pom.xml` (`requel-app-<version>.jar`), currently `2.0.0-dev` — update these commands when that version changes.

```bash
# With local MySQL (Angular served from the JAR at /)
java -jar modules/requel-app/target/requel-app-2.0.0-dev.jar \
  '--spring.datasource.url=jdbc:mysql://127.0.0.1:3306/requel?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC' \
  --spring.datasource.username=root --spring.datasource.password=password --server.port=8080

# Angular dev server (hot reload) + Spring Boot backend
# Start backend with dev profile so CORS allows localhost:4200:
java -jar modules/requel-app/target/requel-app-2.0.0-dev.jar \
  --spring.profiles.active=dev --server.port=8080 \
  '--spring.datasource.url=jdbc:mysql://127.0.0.1:3306/requel?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC' \
  --spring.datasource.username=root --spring.datasource.password=password
# Then in requel-angular/:
cd requel-angular && ng serve

# Or use docker-compose (MySQL + app)
docker-compose up
```

Default login: **admin** / **admin** at http://localhost:8080/

## Architecture

### Maven Monorepo (18 modules under `/modules/`)

DDD-inspired modular architecture with domain/persistence/UI separation:

**Foundation:**
- `platform-core` — shared exceptions, base abstractions (repository, command, validation)
- `platform-identity` — identity primitives, auth, password hashing

**Domain → JPA pairs** (interfaces in `-domain`, persistence in `-jpa`):
- `user-domain` / `user-jpa` — system users, roles
- `project-domain` / `project-jpa` — the core aggregate: projects, goals, actors, stories, scenarios, stakeholders
- `annotation-domain` / `annotation-jpa` — IBIS discussion layer (issues, positions, arguments) attached to any domain entity via Hibernate `@Any`/`@ManyToAny` discriminator pattern

**Features:**
- `nlp-jpa` — Stanford CoreNLP + OpenNLP integration; goal/scenario/lexical assistants
- `dictionary-jpa` — spell-check lexicon persistence
- `utils-jaxb` — JAXB import/export patchers (XML project serialization)

**UI:**
- `requel-angular/` — Angular 17+ SPA (outside the Maven module tree); built by `frontend-maven-plugin` during `mvn package` and served from `classpath:/static/`

**Assistants:**
- `assistant-core, assistant-api` - A key feature or Requel is background assistants that analyze the requirements and add annotations to elements such as notes and issues. issues have positions that may automate a fix to the requirements. There are legacy assistants that use NLP and custom logic to suggest fixes. The future is ai assistants `assistant-ai, assistant-anthropic, assistant-openai` and MCP support so external ai agents can build Requel models.

**Application:**
- `requel-app` — Spring Boot entry point, Flyway migrations, integration tests; serves the Angular SPA at `/` and the CQRS API at `/api/**`

### Key Patterns

- **Aggregate roots:** Project is the main aggregate containing goals, actors, stories, scenarios, stakeholders
- **Command pattern:** Domain mutations via command classes (separate from query repositories); `AnalysisInvokingCommandHandler` triggers NLP after writes
- **Repository pattern:** `AbstractRepository` → `AbstractJpaRepository`; interfaces in domain modules, implementations in JPA modules
- **Annotation registry:** `AnnotatableTypeRegistry` in `annotation-domain` maps discriminator strings to entity types; `project-jpa` registers its types via `ProjectAnnotatableRegistryConfiguration`
- **JAXB import/export:** Streaming importer with AggregateAssembler/ImportUnitOfWork pattern; patchers resolve cross-references post-unmarshal without repository access from entity constructors

### CQRS API Architecture

The Angular SPA is backed by a hybrid CQRS API:
- **Writes:** `POST /api/commands/{commandType}` — single dispatch endpoint, ~37 command types
- **Reads:** `GET /api/...` — conventional query endpoints, ~28 total
- **Composite CommandFactory:** per-domain factories (`ProjectCommandFactory`, `UserCommandFactory`, etc.) register their command types at startup; a top-level `CommandFactory` facade provides `newCommand(type, input)` entry point
- **Domain integration:** existing Commands implement `ApiCommand<T>` interface for input mapping
- **Authorization:** `AuthorizingCommandHandler` in handler chain checks `AuthorizableCommand.getAuthorizationRequirement()` before execute. See `doc/AUTH_ARCH.md`
- Full architecture diagram and endpoint inventory in `doc/UI_REFACTOR_PLAN.md` Section 3.1

### Database

- **Production:** MySQL 8.4, schema managed by Flyway (`modules/requel-app/src/main/resources/db/migration/`)
- **Tests:** H2 in-memory (MySQL mode), Hibernate DDL `create-drop`, Flyway disabled
- Test config: `modules/requel-app/src/test/resources/application-test.properties`

## Development Guardrails

- **Domain purity:** Keep domain code persistence-ignorant — no repository access from entity constructors or JAXB hooks
- **Aggregate boundaries:** Follow DDD terminology from `doc/unmarshalling_plan.md`; honour the AggregateAssembler/ImportUnitOfWork pattern for import logic
- **Annotation decoupling:** The annotation module must not import project implementation classes; use the registry pattern
- **Module dependencies flow downward:** domain modules never depend on JPA modules
- **Project XML compatibility:** Import/export must satisfy `doc/samples/project.xsd`; changes to JAXB mappings need round-trip regression tests

## Key Documentation

- `doc/20-release-plan.md` - New release with new Angular UI, CQRS API, SSE streaming, AI assistance, MCP spport
- `doc/AUTH_ARCH.md` — authorization architecture: AuthorizingCommandHandler, permission model, Angular PermissionService
- `doc/MODULARIZATION_PLAN.md` — module dependency graph, refactoring roadmap, package conventions
- `doc/unmarshalling_plan.md` — JAXB import strategy, aggregate assembly
- `doc/agents.md` — AI agent workflows, edit policies, guardrails
- `doc/USER_AND_STAKEHOLDER_MODEL.md` — identity/stakeholder coupling explanation
- `RELEASE.md` — release checklist, Docker build, GitHub Packages deploy

## Testing

- **JUnit 5 (Jupiter)** — fully migrated from JUnit 4; 59 test classes, all using `org.junit.jupiter.api`
- Tests in `modules/requel-app/src/test/` cover commands, REST API (MockMvc), repositories, authorization, and JAXB round-trips
- Surefire configured with `failIfNoTests=false` — modules without tests still build
- Key test classes: `AuthorizationIT` (28 authorization scenarios), `ProjectXmlStreamingRoundTripIT`, `ProjectUserCreationIT`, `AnnotationAnyMappingTest`
