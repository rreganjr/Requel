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
gitignored (`/commit.md`) and is **never committed** — it is only the source for the commit message
and PR body, consumed via `git commit -F commit.md` and `gh pr create --body-file commit.md` (or
copy/pasted into the VS Code commit box). So `commit.md` is never staged; stage only the changed
source files.

Never commit unless explicitly told to commit

Never push changes to the github repo

All plans, reviews, notes and documentation go in the doc folder.

Never use `TL;DR` I hate that abbreviation, use `Summary`

### Development Workflow

Each release such as `release/2.0` has a github project, all issues for the release get added to the project. we use 'Story Point' and 'Story Point Retro' custom fields to track effort. 

Every change is tied to a GitHub issue and lands via a ticket branch and a PR — never commit straight onto `release/2.0`. Steps that create or change Git/GitHub state (branch, commit, push, PR, issue edits) are performed by Claude ONLY when explicitly told; the "never commit/push unless told" rules above always apply. Claude drives GitHub with the `gh` CLI / `gh api` (issues, comments, PRs) when asked.

1. **Issue** — start from a GitHub issue. If none exists, create it (`gh issue create`) when told. Record decisions/progress with `gh issue comment`.
2. **Branch (at the start of work)** — cut a branch from `release/2.0` named
   `<issue-number>-<short-slug>` (e.g. `87-pat-delete`, matching the existing `73-api-tokens` /
   `77-spring-ai-provider-port` convention). Do all edits on that branch.
3. **Implement, then verify (manual gate — must pass before committing):**
   - Backend: `mvn clean verify` is green.
   - Frontend (when `requel-angular/` changed): `cd requel-angular && npm test -- --watch=false` is green.
     (`npm test` runs `ng test`; the `--` forwards `--watch=false`. Always give the npm form,
     not a bare `ng` command.)

   Do not commit until the relevant suite passes.
4. **Commit message** — write it to `commit.md` in the format above. Include a closing keyword
   (`Closes #<n>` / `Fixes #<n>`) so merging the PR closes the issue; reference related issues by URL.
5. **Commit + push** — only when told; commit on the ticket branch and push it.
6. **PR** — open with `gh pr create --base release/2.0` (when told), always passing a `--title`
   (`<issue#>: <concise summary>`) and using the `commit.md` content as the body via
   `--body-file`. PRs are squash-merged.

**Auto-close caveat:** the repo's default branch is `master`, but PRs target `release/2.0`. GitHub auto-closes an issue from `Closes #<n>` only when the PR merges into the **default** branch, so merging into `release/2.0` does **not** close the issue. Always close it explicitly after merge:
`gh issue close <n> --comment "Merged to release/2.0 via #<pr>."`

Command reference (Claude runs these only when told; `gh`/`mvn`/`ng` run in the developer's
environment, not Claude's sandbox):

```bash
# 1. Issue (if none): gh issue create --repo rreganjr/Requel --title "..." --body "..."
# 2. Branch from release/2.0 at the start of work:
git switch -c <issue#>-<slug> release/2.0
# 3. Verify before committing (must pass):
mvn clean verify                                 # backend
cd requel-angular && npm test -- --watch=false   # frontend, only if requel-angular/ changed
# 4. Write commit.md with a "Closes #<n>" line. commit.md is gitignored (/commit.md) — it is the
#    message source for the commit/PR body, NOT a committed file, so do not `git add` it.
# 5. Commit + push (stage only the changed source files):
git add <changed files> && git commit -F commit.md && git push -u origin <issue#>-<slug>
# 6. PR (always pass --title):
gh pr create --repo rreganjr/Requel --base release/2.0 --head <issue#>-<slug> \
  --title "<issue#>: <concise summary>" --body-file commit.md
# after squash-merge (release/2.0 is not default, so close manually):
gh issue close <n> --repo rreganjr/Requel --comment "Merged to release/2.0 via #<pr>."
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

```bash
# With local MySQL (Angular served from the JAR at /)
java -jar modules/requel-app/target/requel-app-1.2.0.jar \
  '--spring.datasource.url=jdbc:mysql://127.0.0.1:3306/requel?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC' \
  --spring.datasource.username=root --spring.datasource.password=password --server.port=8080

# Angular dev server (hot reload) + Spring Boot backend
# Start backend with dev profile so CORS allows localhost:4200:
java -jar modules/requel-app/target/requel-app-1.2.0.jar \
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
