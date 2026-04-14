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

Never commit unless explicitly told to commit

Never push changes to the github repo

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
  --spring.datasource.username=root --spring.datasource.password=password --server.port=8081

# Angular dev server (hot reload) + Spring Boot backend
# Start backend with dev profile so CORS allows localhost:4200:
java -jar modules/requel-app/target/requel-app-1.2.0.jar \
  --spring.profiles.active=dev --server.port=8081 \
  '--spring.datasource.url=jdbc:mysql://127.0.0.1:3306/requel?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC' \
  --spring.datasource.username=root --spring.datasource.password=password
# Then in requel-angular/:
cd requel-angular && ng serve

# Or use docker-compose (MySQL + app)
docker-compose up
```

Default login: **admin** / **admin** at http://localhost:8081/

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

- `doc/UI_REFACTOR_PLAN.md` — Echo2→Angular migration plan, CQRS API, SSE streaming, phases
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
