# Requel Modularization Plan

## 1. Goals
- Convert the legacy single-module build into a Maven monorepo composed of independently buildable Java modules.
- Isolate the core domain entities (project, user, annotation, etc.) from UI, NLP analysis, and infrastructure code so those layers can evolve or be replaced independently (e.g., swap Echo2 UI).
- Avoid cyclic package/module dependencies by reshaping class responsibilities and introducing bridging modules only where strictly necessary.
- Preserve Spring Boot 3 + Hibernate 6 behaviour and existing database schema while improving testability and CI build times.

## 2. Current Architecture Snapshot
- **Build**: One Maven module (`Requel`) that packages everything, including Spring Boot application, domain, repositories, NLP utilities, and Echo2 UI support.
- **Package map**: Root packages under `com.rreganjr` (`command`, `validator`, `repository`, `nlp`, `requel`, etc.) and Echo2 sources under `nextapp`.
- **Domain coupling hot spots**:
  - `com.rreganjr.requel.project` ↔ `com.rreganjr.requel.user`: cross-imports through `ProjectUserRole`, `UserStakeholderImpl`, numerous commands, and repository initializers.
  - `com.rreganjr.requel.project.impl.assistant` depends on `com.rreganjr.nlp` and injects NLP types into entities and commands; this ties analysis to the domain model.
  - `com.rreganjr.requel.annotation.impl.AbstractAnnotation` hard-codes entity implementations (`ProjectImpl`, `ScenarioImpl`, etc.), pulling annotation logic into the project module.
  - `com.rreganjr.requel.utils.jaxb` mixes general-purpose helpers with project/user aware JAXB patchers, creating back edges into the domain.
  - `com.rreganjr.repository.jpa.DomainObjectWrapper` references `UserSetImpl`, producing a repository → user-impl dependency.
- **Infrastructure**: `SystemInitializer` lives at the root package and is implemented by project, user, and NLP initializers. The Spring Boot `Application` bootstraps Echo servlets and scans `com.rreganjr.requel` and `com.rreganjr.nlp`.
- **Tests**: Concentrated in `src/test/java/com/rreganjr/requel/...`, tightly coupled to concrete implementations.

## 3. Target Module Map
The monorepo will grow a `/modules` directory with one Maven module per package slice plus a Spring Boot application module. All modules inherit from a new root `pom.xml` that acts as an aggregator.

| Layer | Module (proposed artifactId) | Packages/Refactoring Scope | Depends on |
|-------|------------------------------|-----------------------------|------------|
| Foundation | `platform-core` | Move shared exceptions (`RequelException`, `EntityValidationException`, `EntityLockException`, `NoSuchEntityException`), `Describable`, `NamedEntity`, `OrganizedEntity` (after decoupling from `User`), `CreatedEntity` (adjusted to reference a lightweight `UserReference`). Also relocate `SystemInitializer` & `AbstractSystemInitializer` to new `com.rreganjr.platform.bootstrap`. | none |
| Foundation | `validation-core` | Current `com.rreganjr.validator`. | `platform-core` |
| Foundation | `command-core` | Current `com.rreganjr.command`. Replace direct references to `user` exceptions with platform equivalents or callback interfaces. | `platform-core`, `validation-core` |
| Foundation | `repository-core` | `com.rreganjr.repository` & `com.rreganjr.repository.jpa` minus domain-specific adapters, refactored to depend on interfaces (`UserSet`, etc.) instead of impl classes. | `platform-core`, `command-core` |
| Domain | `user-domain` | `com.rreganjr.requel.user` interfaces, DTOs, exceptions. Convert `CreatedEntity` dependency to the new `UserReference` abstraction. | `platform-core` |
| Domain | `user-jpa` | `com.rreganjr.requel.user.jpa` (renamed from `.impl`) entities and repository adapters. Depends on `user-domain`, `repository-core`. | `user-domain`, `repository-core` |
| Domain | `project-domain` | `com.rreganjr.requel.project` interfaces + entities stripped of NLP/assistant hooks. Introduce new `com.rreganjr.requel.project.access` subpackage for `ProjectUserRole` & stakeholder bridges that depend on `user-domain`. | `platform-core`, `user-domain` |
| Domain | `project-jpa` | `com.rreganjr.requel.project.jpa` (renamed from `.impl`) persistence implementations, command implementations, and repository initializers that sit on top of `project-domain` & `repository-core`. | `project-domain`, `repository-core`, `user-jpa` |
| Domain | `annotation-domain` | `com.rreganjr.requel.annotation` & `impl` reorganised so that entity references go through interfaces or a registry; move project-specific discriminators to `project-domain`. | `platform-core`, `project-domain` |
| Domain | `utils-core` | Generic utilities (`DateUtils`, `Untar`). Introduce `utils-jaxb` module for patchers that rely on project/user/annotation interfaces. | `platform-core` (+ optional `annotation-domain` for `utils-jaxb`) |
| Feature | `project-analysis` | Extract `com.rreganjr.requel.project.analysis` (from `impl.assistant`, `impl.command.RemoveUnneedLexicalIssuesCommand*`, etc.) and other NLP-heavy classes from `project-jpa`. Depends on `project-domain`, `annotation-domain`, `nlp-core`. | `project-domain`, `annotation-domain`, `nlp-core` |
| Feature | `nlp-core` | Entire `com.rreganjr.nlp` tree. Replace direct `NoSuchEntityException` usage with `platform-core` type. | `platform-core` |
| Feature | `ui-echo2` | `com.rreganjr.requel.ui` plus vendored `nextapp.*` sources. Depends only on `project-analysis` (for assistants), `project-domain` (for DTOs), and service APIs. | `project-analysis`, `service-api` |
| Feature | `service-api` | Split `com.rreganjr.requel.service` into API contracts (controllers, DTOs) and implementation module (`service-impl`). API should not depend on UI. | `platform-core`, `project-domain`, `command-core` |
| Feature | `service-impl` | Spring MVC controllers & wiring (current `ProjectXmlController`, future REST endpoints). | `service-api`, `project-jpa`, `command-core` |
| Application | `requel-app` | New Spring Boot module containing `Application`, configuration, main resources, Docker build, and integration tests. This is the only module depending on UI. | All above as needed |

### 3.1 New package conventions
- Move cross-cutting types from `com.rreganjr.requel` root into `com.rreganjr.platform` (for core) and `com.rreganjr.requel.bootstrap` (for bootstrapping).
- Introduce `com.rreganjr.requel.project.access` to host `ProjectUserRole`, stakeholder-specific initializers, and other bridge classes between project & user modules.
- Introduce `com.rreganjr.requel.project.analysis` (or `assistant`) within `project-analysis` for NLP dependent logic.
- Shift JAXB patchers into `com.rreganjr.requel.utils.jaxb` with clear dependency direction: patchers depend on domain APIs, domains never import patchers.
- UI module retains `com.rreganjr.requel.ui` but becomes isolated from domain implementation packages (only import service interfaces).

### 3.2 Package naming guidelines
- **Module directories** live under `modules/<artifactId>` (e.g., `modules/user-domain`), but Java packages always use dot-separated names—no hyphens.
- **Platform core**: `com.rreganjr.platform.core` for shared abstractions; bootstrap helpers in `com.rreganjr.platform.bootstrap`.
- **Command and repository**: retain `com.rreganjr.command` and `com.rreganjr.repository` with subpackages (`.jpa`, `.spring`) as needed.
- **User modules**: public APIs remain in `com.rreganjr.requel.user`; persistence implementations relocate to `com.rreganjr.requel.user.jpa`.
- **Project modules**: interfaces and aggregates in `com.rreganjr.requel.project`; persistence code in `com.rreganjr.requel.project.jpa`; cross-module bridges in `com.rreganjr.requel.project.access`; analysis extensions in `com.rreganjr.requel.project.analysis`.
- **Annotation**: keep `com.rreganjr.requel.annotation` for APIs and add `com.rreganjr.requel.annotation.jpa` if we split persistence bindings.
- **Utilities**: `com.rreganjr.requel.utils` for generic helpers; `com.rreganjr.requel.utils.jaxb` for bridge utilities.
- **Service layer**: reserve `com.rreganjr.requel.service.api` for controller/service contracts and `com.rreganjr.requel.service.impl` (Spring MVC wiring) within the `service-impl` module.
- **Application module**: `com.rreganjr.requel.app` (or retain `com.rreganjr.requel`) for Spring Boot entrypoints and configuration classes.

## 4. Dependency Refactors & Cycle Breaks
### 4.1 User ↔ Project cycle
- Extract `ProjectUserRole`, `DomainAdminUserRole`, `ProjectUserInitializer`, etc. into `project-access` submodule that sits _above_ both `user-domain` and `project-domain`.
- Replace direct `UserImpl` references in project entities with the `User` interface; only `project-access` and `user-jpa` need to know about `UserImpl`.
- Update repository initializers to depend on interfaces (e.g., inject `Supplier<User>` instead of `UserRepository` where possible). Relocate initializers to dedicated module to avoid repository-core ↔ domain cycles.
- Ensure `user-domain` never imports `project` packages once bridges are extracted.

### 4.2 Annotation ↔ Project coupling
- Introduce an `AnnotatableTypeRegistry` in `annotation-domain` that maps discriminator strings to entity interfaces. Register JPA implementations (`ProjectImpl`, `ScenarioImpl`, etc.) from `project-jpa` via Spring configuration instead of static annotations inside `AbstractAnnotation`.
- Limit annotation entities to depend on `ProjectOrDomainEntity` interfaces only. Validation logic that needs concrete classes moves to `project-jpa`.

### 4.3 NLP leakage into core
- Migrate NLP-dependent assistants and commands into `project-analysis`. Core `project-domain` should restrict itself to NLP-free entities and value objects.
- Introduce interfaces for lexical assistance that live in `project-domain` (`LexicalAnalysisService`), implemented inside `project-analysis`. UI and commands call the interface, keeping the domain agnostic of NLP implementation.
- Package each analysis bundle (current NLP plus future variants) as a Spring Boot optional starter (`requel-project-analysis-nlp-starter`, `requel-project-analysis-ml-starter`, etc.). Starters expose auto-configuration that registers the analysis interfaces only when the module is present on the classpath, allowing `requel-app` or other consumers to include/exclude analysis capabilities through dependency management rather than code changes.
- Provide conditional beans so that the application fails fast or falls back gracefully when no analysis starter is present; dev profile can include the NLP starter by default while other deployments choose alternatives.

### 4.4 Utilities cleanup
- Split `utils` into `utils-core` (pure utilities) and `utils-jaxb` (bridge). Update domains to depend only on `utils-core`; bridges live in higher modules to prevent back edges.

### 4.5 System initializer alignment
- Create `com.rreganjr.platform.bootstrap` for `SystemInitializer` abstractions. Domain-specific initializers (user, project, NLP) move into their respective modules, contributing Spring beans exposed to the application module via auto-configuration.

### 4.6 Echo2 isolation
- Move all Echo2-specific Java packages (`com.rreganjr.requel.ui`, `net.sf.echopm`, `nextapp`) into `ui-echo2`.
- Publish a service-level API (e.g., `ProjectPresentationService`) that the UI calls. This lets us swap the frontend without touching domain modules.

## 5. Repository & Build Restructure
1. **Introduce aggregator POM**: Rename current `pom.xml` to `modules/pom.xml` for the new root and create a top-level aggregator (packaging `pom`) that lists each submodule.
2. **Create skeleton modules**: Generate module directories with minimal `pom.xml` files inheriting from the new parent. Start with foundation modules (`platform-core`, `validation-core`, `command-core`, `repository-core`) and `requel-app`.
3. **Relocate source sets**: Move Java and resource directories into the corresponding module (`modules/platform-core/src/main/java/...`, etc.). Adjust IDE module definitions.
4. **Rewire dependencies**: Update POMs to express the dependency graph from Section 3. Use Maven `dependencyManagement` in the root parent for consistent versions.
5. **Adjust Spring Boot configuration**: In `requel-app`, configure component scanning and `@EntityScan` to import from new module packages. Add auto-configuration modules where necessary (e.g., a `module-jpa-spring` module exporting `@Configuration` beans).
6. **Move scripts & transforms**: Keep the Echo transformer scripts in the application module or a dedicated `build-tools` module referenced by `requel-app`.
7. **Refactor tests**: Co-locate unit tests with their modules. For cross-module integration tests, add a `requel-it` module or keep them in `requel-app`.
8. **Update Docker/build pipeline**: Adjust Dockerfile and CI scripts to build the root aggregator (`mvn clean install`) and then package the Spring Boot jar from `requel-app`.

## 6. Migration Phases
- **Phase 0** – *Foundation extraction*: Create `platform-core`, `validation-core`, and `command-core`. Remove direct references from `command-core` to user-specific exceptions by introducing general-purpose hooks or relocating the exceptions into platform.
- **Phase 1** – *User & project split*: Move user interfaces into `user-domain`, implementations into `user-jpa`, extract project entities into `project-domain`, and create `project-jpa`. Extract bridging roles into `project-access`.
- **Phase 2** – *Annotation & utils cleanup*: Introduce registry pattern, relocate JAXB utilities, update project entities to depend on the registry.
- **Phase 3** – *NLP isolation*: Carve out `project-analysis` and ensure `project-domain` no longer imports NLP packages.
- **Phase 4** – *Service/UI modules*: Introduce `service-api`, `service-impl`, and `ui-echo2`. Update controller wiring to depend on service interfaces.
- **Phase 5** – *Application consolidation*: Finalise `requel-app` module, update packaging, and retire legacy single-module build.
- **Phase 6** – *Cleanup & documentation*: Remove deprecated imports, update README/DEPLOY docs, and ensure tooling (IDEA, Docker, CI) reflects new structure.

## 7. Testing & Verification Strategy
- Run unit tests within each module (enforced via Maven `failsafe`/`surefire` configs). Add module-specific test fixtures for repository and NLP components.
- Introduce integration tests in `requel-app` that start the Spring context with the modular beans. Focus on the project/user bootstrap path and Echo2 UI registration.
- Validate Hibernate mappings after moving entities by executing schema generation or migration scripts in CI.
- Track performance impact: modularisation should enable selective builds (`mvn -pl module-name test`) and speed up feedback loops.

## 8. Decisions & Clarifications
- **Package renaming scope**: Renaming packages and modules is acceptable. Update JAXB annotations, discriminator values, and configuration to reference the new names while keeping binary compatibility where external integrations depend on class names.
- **Project XML compatibility**: Project export/import must continue to satisfy `doc/samples/project.xsd` and the sample data in `doc/samples/Requel.xml`. Any refactoring that touches JAXB mappings needs regression tests that round-trip the sample project.
- **Echo2 horizon**: Replacement timeline is uncertain. Keep `ui-echo2` maintainable but avoid deep coupling so that an eventual alternative UI can reuse service APIs without destabilising the application module.
- **Analysis modules**: NLP remains optional and acts as the first concrete Spring Boot starter. Future analysis modules (e.g., ML, rules engines) should implement the same extension points outlined in §4.3 so they can be added/removed via dependency management.
- **Database migration strategy**: Evaluate introducing Flyway alongside (or instead of) the current `SystemInitializer` beans. Flyway brings versioned, repeatable migrations with checksum validation, easier CI drift detection, and module-specific migration folders. Initializers still cover non-SQL bootstrap tasks (seeding default users, dynamic permission wiring), so a hybrid approach may be appropriate—Flyway handles schema and static data, while lightweight initializers orchestrate programmatic seeding that depends on services.

## 9. Immediate Next Steps
- Add regression coverage that exports/imports `doc/samples/Requel.xml` against the refactored modules to guard the XML contract.
- Prototype `platform-core` extraction (move exceptions, `SystemInitializer`) and ensure existing tests still pass.
- Prepare Maven parent/child POM skeletons to unblock gradual module migration.
- Draft architectural diagrams (plantuml) illustrating the intended dependency graph for inclusion in project docs.
- Decide whether to pilot Flyway alongside the existing initializers in an early module (e.g., `user-jpa`) to validate the hybrid migration approach before committing project-wide.
