# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Requel is a web-based requirements management system supporting collaboration among stakeholders with automated NLP-based assistance. It models requirements as goals, stories, actors, scenarios, and use-cases with an IBIS-style annotation/discussion layer. Originally a 2009 Harvard ALM thesis project, now modernized to Spring Boot 3 / Java 17.

## Build Commands

Requires **Java 17** and **Maven 3.6.3+**. Set `JAVA_HOME` accordingly.

```bash
# Full build (includes Echo2 javax→jakarta transform on first run)
mvn -pl modules/requel-app -am clean verify

# Fast iterative build (skips Echo transform and tests)
mvn -pl modules/requel-app -am package -DskipEchoTransform=true -DskipTests=true

# Run all tests
mvn test

# Run a single test class
mvn -pl modules/requel-app -am test -Dtest=ProjectXmlRoundTripIT

# Run a single module's tests
mvn -pl modules/project-jpa test

# Build Docker image
mvn -pl modules/requel-app -am package -Pdocker-image -DskipTests -DskipEchoTransform=true
```

## Running Locally

```bash
# With local MySQL
java -jar modules/requel-app/target/requel-app-1.2.0.jar \
  '--spring.datasource.url=jdbc:mysql://127.0.0.1:3306/requel?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC' \
  --spring.datasource.username=root --spring.datasource.password=password --server.port=8081

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

**UI (Echo2 framework — legacy Java RIA):**
- `ui-core` — UI framework abstractions
- `ui-assets` — static resources
- `project-ui`, `user-ui`, `annotation-ui`, `nlp-ui` — panel/tree-based UI components

**Application:**
- `requel-app` — Spring Boot entry point, Echo2 servlet registration, Flyway migrations, integration tests

### Key Patterns

- **Aggregate roots:** Project is the main aggregate containing goals, actors, stories, scenarios, stakeholders
- **Command pattern:** Domain mutations via command classes (separate from query repositories); `AnalysisInvokingCommandHandler` triggers NLP after writes
- **Repository pattern:** `AbstractRepository` → `AbstractJpaRepository`; interfaces in domain modules, implementations in JPA modules
- **Annotation registry:** `AnnotatableTypeRegistry` in `annotation-domain` maps discriminator strings to entity types; `project-jpa` registers its types via `ProjectAnnotatableRegistryConfiguration`
- **JAXB import/export:** Streaming importer with AggregateAssembler/ImportUnitOfWork pattern; patchers resolve cross-references post-unmarshal without repository access from entity constructors

### CQRS API Architecture (Planned — see `doc/UI_REFACTOR_PLAN.md`)

The Echo2 UI is being replaced with an Angular SPA backed by a hybrid CQRS API:
- **Writes:** `POST /api/commands/{commandType}` — single dispatch endpoint, ~37 command types
- **Reads:** `GET /api/...` — conventional query endpoints, ~28 total
- **Composite CommandFactory:** per-domain factories (`ProjectCommandFactory`, `UserCommandFactory`, etc.) register their command types at startup; a top-level `CommandFactory` facade provides `newCommand(type, input)` entry point
- **Domain integration:** existing Commands implement `ApiCommand<T>` interface for input mapping
- Full architecture diagram and endpoint inventory in `doc/UI_REFACTOR_PLAN.md` Section 3.1

### Database

- **Production:** MySQL 8.4, schema managed by Flyway (`modules/requel-app/src/main/resources/db/migration/`)
- **Tests:** H2 in-memory (MySQL mode), Hibernate DDL `create-drop`, Flyway disabled
- Test config: `modules/requel-app/src/test/resources/application-test.properties`

### Echo2 Transform

Echo2 JARs ship with `javax.*` namespaces and must be transformed to `jakarta.*` for Spring Boot 3. The `exec-maven-plugin` runs `/scripts/java17-transform.sh` during build. Skip with `-DskipEchoTransform=true` after the first successful transform (JARs are cached in local Maven repo).

## Development Guardrails

- **Domain purity:** Keep domain code persistence-ignorant — no repository access from entity constructors or JAXB hooks
- **Aggregate boundaries:** Follow DDD terminology from `doc/unmarshalling_plan.md`; honour the AggregateAssembler/ImportUnitOfWork pattern for import logic
- **Annotation decoupling:** The annotation module must not import project implementation classes; use the registry pattern
- **Module dependencies flow downward:** domain modules never depend on JPA modules; UI depends on domain interfaces, not implementations
- **Project XML compatibility:** Import/export must satisfy `doc/samples/project.xsd`; changes to JAXB mappings need round-trip regression tests

## Key Documentation

- `doc/MODULARIZATION_PLAN.md` — module dependency graph, refactoring roadmap, package conventions
- `doc/unmarshalling_plan.md` — JAXB import strategy, aggregate assembly
- `doc/agents.md` — AI agent workflows, edit policies, guardrails
- `doc/USER_AND_STAKEHOLDER_MODEL.md` — identity/stakeholder coupling explanation
- `RELEASE.md` — release checklist, Docker build, GitHub Packages deploy

## Testing

- **JUnit 4** with JUnit Vintage engine (legacy test runner)
- Tests in `modules/requel-app/src/test/` cover integration (Spring context, JAXB round-trips, Hibernate mappings)
- Surefire configured with `failIfNoTests=false` — modules without tests still build
- Key integration tests: `ProjectXmlRoundTripIT`, `ProjectUserCreationIT`, `AnnotationAnyMappingTest`
