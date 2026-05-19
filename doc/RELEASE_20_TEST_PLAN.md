# Requel 2.0 Test Plan

**Goal:** Establish a comprehensive, automated test suite that gives confidence in a 2.0 release
across three layers — Java unit/integration, Angular component, and full-stack browser automation.

---

## 1. Current State

### Java

Tests live in `modules/requel-app/src/test/`. Framework is **JUnit 4** via the JUnit Vintage
engine. H2 in-memory database (MySQL mode) is used for integration tests; Flyway is disabled in
test profile and Hibernate drops/recreates the schema on each run.

| Existing test class | What it covers |
|---|---|
| `ProjectXmlRoundTripIT` | Full JAXB import → DB → export round-trip |
| `ProjectXmlStreamingRoundTripIT` | Streaming importer variant of the above |
| `ProjectUserCreationIT` | Admin user bootstrap, project creation via commands |
| `AnnotationAnyMappingTest` | Hibernate `@Any`/`@ManyToAny` discriminator mapping |
| `EditProjectCommandImplTest` | EditProject command happy path |
| `ImportProjectCommandTest` / `ImportProjectStreamingCommandTest` | Import commands |
| `ProjectJAXBTest` | JAXB marshalling/unmarshalling |
| `UserImplTest` / `UserCollectionImplTest` / `UserImplMarshallingTest` | User domain |
| `GoalAssistantTest` / `ProjectAssistantTest` | NLP assistant integration |
| NLP tests (`NLPTests`, `LemmatizerTests`, etc.) | Stanford CoreNLP / OpenNLP |

**Gaps:** No command tests for goals, stories, actors, use-cases, scenarios, stakeholders; no REST
API (MockMvc) tests; no authorization tests; no audit log tests.

### Angular

Framework: **Angular 21** with `@angular/build:unit-test` (backed by **Vitest** + **jsdom** —
the Angular-native replacement for Karma as of Angular 19+). `jsdom` is already in
`devDependencies`.

**Gaps:** Zero spec files exist. No E2E tooling installed.

---

## 2. Java Test Strategy

### 2.0 Migrate existing tests to JUnit 5 first ✓ DONE

The existing test suite is small (14 files) and self-contained enough that a full migration to
JUnit 5 before writing any new tests is the right call. Mixing JUnit 4 and JUnit 5 via the Vintage
engine works, but it adds engine overhead, produces inconsistent annotation styles in the same
codebase, and means new tests can't use JUnit 5 features (`@ParameterizedTest`, `@Nested`,
`assertThrows`, etc.) alongside old ones without confusion.

**Migration steps:**

1. **Update `requel-app/pom.xml`:**
   - Remove the `junit:junit` and `org.junit.vintage:junit-vintage-engine` dependencies.
   - Spring Boot's `spring-boot-starter-test` already includes `junit-jupiter` (JUnit 5) and
     `mockito-junit-jupiter`; no explicit version management needed.

2. **Update each existing test file** — the mechanical changes are:

   | JUnit 4 | JUnit 5 equivalent |
   |---|---|
   | `import org.junit.Test` | `import org.junit.jupiter.api.Test` |
   | `import org.junit.Before` | `import org.junit.jupiter.api.BeforeEach` |
   | `import org.junit.After` | `import org.junit.jupiter.api.AfterEach` |
   | `import org.junit.Assert.*` | `import org.junit.jupiter.api.Assertions.*` |
   | `@RunWith(SpringRunner.class)` | `@ExtendWith(SpringExtension.class)` (or implicit via `@SpringBootTest`) |
   | `@RunWith(MockitoJUnitRunner.class)` | `@ExtendWith(MockitoExtension.class)` |
   | `expected = SomeException.class` on `@Test` | `assertThrows(SomeException.class, () -> ...)` |
   | Public test methods required | Package-private is fine |

3. **Verify** all 14 existing tests still pass with `mvn test` before writing anything new.

This is a one-time cost — the existing test classes are straightforward Spring integration tests
and plain unit tests with no JUnit 4-specific rules or runners beyond what the table above covers.

### 2.1 Framework and conventions (post-migration)

- All tests use **JUnit 5 (Jupiter)**.
- Use **Spring Boot Test** (`@SpringBootTest`, `@DataJpaTest`, `@WebMvcTest`) to get managed
  context injection without manual wiring.
- Use **Mockito** with `@ExtendWith(MockitoExtension.class)` for unit tests that isolate a
  single class.
- Integration tests that need a database use the existing `application-test.properties` (H2,
  `spring.jpa.hibernate.ddl-auto=create-drop`, Flyway disabled).
- Naming convention: `*Test.java` for unit tests (no Spring context), `*IT.java` for integration
  tests (Spring context, H2).
- Prefer `@Nested` inner classes to group related scenarios within a single test class.
- Use `@ParameterizedTest` with `@MethodSource` for data-driven cases (e.g., testing multiple
  entity types against the same command pattern).
- Use **JaCoCo** for Java code coverage reporting; JUnit runs tests, but coverage comes from
  JaCoCo instrumentation.

### 2.1.1 Java coverage reporting (JaCoCo)

Add **`jacoco-maven-plugin`** to the **root POM's `<pluginManagement>`** (not per-module) so every
module inherits it automatically. Bind two goals:

- `jacoco:prepare-agent` — instruments the JVM before tests run (binds to `initialize` phase by default)
- `jacoco:report` — generates the report in the `verify` phase

Recommended outputs:

- HTML report for local developer use
- XML report for CI / quality tools

Expected report locations in this multi-module build:

- Per-module reports under `target/site/jacoco/`
- For example: `modules/requel-app/target/site/jacoco/index.html`

Recommended initial policy:

- Start by generating reports only
- After the suite matures, add coverage thresholds for line and branch coverage in CI
- Keep reports **per module by default** so a change in one Maven module can be reviewed against its
  own local coverage report quickly
- Optionally evaluate **PIT mutation testing** later for critical business logic once baseline
  JaCoCo coverage is in place

### 2.2 Command tests ✓ DONE

Each edit command should have an integration test that:
1. Creates the minimum prerequisite data (project, user)
2. Executes the command via the `CommandHandler` (respects the full handler chain: authorization,
   analysis invocation, audit)
3. Asserts the result entity reflects the input
4. For edit commands, asserts name uniqueness is enforced (duplicate name throws)
5. For delete commands, asserts the entity is removed from its project

| Test class to create | Commands covered |
|---|---|
| `EditGoalCommandImplTest` | `EditGoal`, `CopyGoal`, `DeleteGoal` |
| `EditStoryCommandImplTest` | `EditStory` (incl. `primaryActorName`), `CopyStory`, `DeleteStory` |
| `EditActorCommandImplTest` | `EditActor`, `CopyActor`, `DeleteActor` |
| `EditUseCaseCommandImplTest` | `EditUseCase` (incl. `primaryActorName`), `CopyUseCase`, `DeleteUseCase` |
| `EditScenarioCommandImplTest` | `EditScenario`, `EditScenarioStep`, `CopyScenario`, `DeleteScenario` |
| `EditStakeholderCommandImplTest` | `EditUserStakeholder`, `EditNonUserStakeholder`, `DeleteStakeholder` |
| `EditGlossaryTermCommandImplTest` | `EditGlossaryTerm`, `DeleteGlossaryTerm` |
| `EditReportGeneratorCommandImplTest` | `EditReportGenerator`, `DeleteReportGenerator` |
| `ActorContainerCommandImplTest` | `AddActorToActorContainer`, `RemoveActorFromActorContainer` for Project, UseCase, Story |
| `GoalContainerCommandImplTest` | `AddGoalToGoalContainer`, `RemoveGoalFromGoalContainer` for Project, UseCase, Story |
| `StoryContainerCommandImplTest` | `AddStoryToStoryContainer`, `RemoveStoryFromStoryContainer` |
| `ScenarioContainerCommandImplTest` | `SetPrimaryScenarioOnUseCase`, `AddScenarioToUseCase`, `RemoveScenarioFromUseCase` |
| `GoalRelationCommandImplTest` | `EditGoalRelation`, `RemoveGoalRelation` |
| `AnnotationCommandImplTest` | `EditIssue`, `EditPosition`, `EditArgument`, `DeleteIssue`, `DeletePosition`, `DeleteArgument` |
| `EditUserCommandImplTest` | `EditUser` for user create/edit plus password-change flows |
| `AuditingCommandHandlerTest` | Background commands skipped; API commands write row; `projectId` resolved correctly |

### 2.2.1 Copy command tests (identified from JaCoCo 0% coverage)

JaCoCo analysis showed all six Copy commands at 0% instruction coverage. Each shares the same
pattern: copy the original entity's content into a new entity, auto-generate a unique name if the
original name is already taken, preserve text and type fields, and share annotations and glossary
term references.

| Test class | Commands covered | Key scenarios |
|---|---|---|
| `CopyCommandTest` | `CopyGoal`, `CopyActor`, `CopyStory`, `CopyScenario`, `CopyScenarioStep`, `CopyUseCase` | copy preserves content; auto-generated name when original name is taken; explicit new name used when provided; `CopyUseCase` also copies the primary scenario |

### 2.2.2 Additional commands — partially complete

| Command | Status | Test class | Notes |
|---|---|---|---|
| `DeleteReportGeneratorCommandImpl` | ✓ DONE | `ReportGeneratorCommandTest` | Standard delete pattern; also covers `EditReportGeneratorCommand` create |
| `ReplaceGlossaryTermCommandImpl` | ✓ DONE | `GlossaryTermCommandTest` | **Bug noted:** the `else if (entity instanceof Story)` branch casts to `ActorImpl` — dead code (copy-paste error); Actor text replacement never fires |
| `ResolveIssueWithAddActorPositionCommandImpl` | ✓ DONE | `AnnotationCommandTest` | Wired manually without NLP: create LexicalIssue + AddActorPosition, resolve, verify actor appears in project |
| `ResolveIssueWithAddGlossaryTermPositionCommandImpl` | ✓ DONE | `AnnotationCommandTest` | Same pattern; verifies GlossaryTerm appears in project |
| `ConvertStepToScenarioCommandImpl` | ✓ DONE | `ScenarioStepCommandTest` | Create scenario with step, convert step, verify new Scenario replaces Step at same index |
| `ExportProjectCommandImpl` | — deferred | — | Exercised by `ProjectXmlStreamingRoundTripIT`; 0% in module report is cross-module attribution artefact |
| `GenerateReportCommandImpl` | — deferred | — | Requires XSLT template + file I/O; complex setup; low regression risk |

### 2.2.3 Annotation command tests (identified from JaCoCo 0% coverage)

The existing `AnnotationCommandTest` covers the IBIS hierarchy create/edit/delete for Issue,
Position, and Argument. JaCoCo shows five annotation commands still at 0%:

| Class | Instructions | What it does |
|---|---|---|
| `ResolveIssueWithChangeSpellingPositionCommandImpl` | 199 | Resolves a `LexicalIssue` by replacing a misspelled word in the annotatable entity's text field via reflection; delegates to `ResolveIssueCommandImpl.execute()` |
| `RemoveAnnotationFromAnnotatableCommandImpl` | 127 | Detaches a single annotation from one annotatable; if no annotatables remain, deletes the annotation via `DeleteIssue` or `DeleteNote` |
| `ResolveIssueCommandImpl` | 90 | Marks a position as the resolution for an issue (`issue.isResolved()` becomes true) |
| `ResolveIssueWithAddWordToDictionaryPositionCommandImpl` | 48 | Resolves a `LexicalIssue` by adding the flagged word to the dictionary via `EditDictionaryWordCommand`, then delegates to base resolve |
| `DeleteNoteCommandImpl` | 42 | Removes a `Note` annotation from all its annotatables and deletes it |
| `EditNoteCommandImpl` | (already partially hit via existing tests) | Create/edit a note annotation |

Tests added to `AnnotationCommandTest`:

| Test method | Commands exercised | What it verifies |
|---|---|---|
| `createNote` | `EditNoteCommand` | Note persisted; appears in goal annotations |
| `editNote` | `EditNoteCommand` | Note text updated |
| `deleteNote` | `EditNoteCommand` + `DeleteNoteCommand` | Note removed from goal annotations |
| `resolveIssueWithPosition` | `ResolveIssueCommand` | `issue.isResolved()` true; `getResolvedByPosition()` returns the resolving position |
| `removeAnnotationFromAnnotatableKeepsAnnotationWhenShared` | `RemoveAnnotationFromAnnotatableCommand` | Issue detached from one of two goals; still present on the other |
| `removeAnnotationFromAnnotatableDeletesIssueWhenLastAnnotatable` | `RemoveAnnotationFromAnnotatableCommand` + `DeleteIssueCommand` | Issue removed from sole annotatable; issue itself deleted |
| `resolveIssueWithChangeSpellingFixesTextInAnnotatable` | `EditLexicalIssueCommand` + `EditChangeSpellingPositionCommand` + `ResolveIssueWithChangeSpellingPositionCommand` | Misspelled word in goal text replaced with corrected spelling; issue marked resolved |
| `resolveIssueWithAddWordToDictionaryResolvesIssue` | `EditLexicalIssueCommand` + `EditAddWordToDictionaryPositionCommand` + `ResolveIssueWithAddWordToDictionaryPositionCommand` | Word added to dictionary; issue marked resolved |

### 2.3 REST API tests (MockMvc) ✓ DONE

Use `@WebMvcTest` + `MockMvc` to test the HTTP layer in isolation, with the command/query services
mocked. This layer verifies: routing, request deserialization, authorization header enforcement,
error response shapes, and HTTP status codes.

| Test class to create | Controllers covered |
|---|---|
| `CommandControllerTest` | `POST /api/commands/{type}` — happy path, unknown type 400, unauthorized 403, validation 422, conflict 409 |
| `ProjectQueryControllerTest` | `GET /api/projects` (list), `GET /api/projects/{name}` (detail), goals, stories, actors, use-cases, scenarios, stakeholders, open-issues, `GET /api/projects/{name}/tree`, `GET /api/projects/stakeholder-permissions`, `GET /api/projects/{name}/terms`, `GET /api/projects/{name}/reports`, `GET /api/projects/{name}/reports/{reportId}/run` |
| `UserQueryControllerTest` | `GET /api/users`, `GET /api/users/{id}`, `GET /api/users/organizations`, `GET /api/users/roles` |
| `AuthControllerTest` | `POST /api/auth/login` (success, bad credentials), `GET /api/auth/me` (returns current user; 401 when not authenticated), JWT token structure |
| `AnnotationQueryControllerTest` | `GET /api/annotations` — verifies response shape and authorization |
| `EventStreamControllerTest` | `GET /api/events/stream` (SSE content-type, subscription registered), `POST /api/events/subscriptions` (add subscription), `DELETE /api/events/subscriptions` (remove subscription), `DELETE /api/events/connection` (close connection) |
| `UserPreferencesControllerTest` | `GET`/`PUT /api/user-preferences` |

### 2.4 Repository tests ✓ DONE

Use `@DataJpaTest` with H2. These verify the JPA mappings and named queries without running the
full application context.

| Test class to create | What to verify |
|---|---|
| `ProjectRepositoryTest` | `findProjectByName`, `findGoalById`, `findStoryById`, `findActorByProjectOrDomainAndName`, uniqueness constraints |
| `AnnotationRepositoryTest` | `@Any`/`@ManyToAny` load and save for Issue → Position → Argument chains on each entity type |
| `StoryPrimaryActorMappingTest` | `primary_actor_id` FK — set, clear, verify lazy load |
| `UserRepositoryTest` | `findByUsername`, role-based queries |

### 2.5 Authorization tests ✓ DONE

Use the full Spring context with `@SpringBootTest` + `MockMvc`. Log in as different user types and
assert permission enforcement.

| Scenario | Expected |
|---|---|
| Unauthenticated `POST /api/commands/EditGoal` | 401 |
| Authenticated user without edit permission on project | 403 |
| Admin editing any project entity | 200 |
| User editing their own account | 200 |
| User editing another account | 403 |

### 2.5.1 Test user setup and the permission model

#### Built-in users

Two users are created by Spring initializers (`AdminUserInitializer`, `ProjectUserInitializer`) at
context startup. Because the test profile uses H2 with `create-drop`, both initializers run before
every test class that loads the Spring context. These users are available without any test setup:

| Username | Password | System role | Can create projects |
|---|---|---|---|
| `admin` | `admin` | `SystemAdminUserRole` | yes (admin bypass) |
| `project` | `project` | `ProjectUserRole` | yes |

Use these for broad happy-path and negative-path tests (unauthenticated, wrong credentials,
admin-bypasses-all). Do not use them for permission-boundary tests — their system-level roles
make them hard to scope narrowly.

#### The stakeholder permission model

Project-level access control is separate from system roles. A `UserStakeholder` links a system
`User` to a `Project` and carries a set of `StakeholderPermission` objects. A permission is
the combination of an entity type and a permission type:

**Permission types** (`StakeholderPermissionType` enum):

| Type | Meaning |
|---|---|
| `Edit` | Create and edit this entity type on the project |
| `Delete` | Delete this entity type from the project |
| `Grant` | Grant Edit/Delete/Grant permissions for this entity type to other stakeholders |

**Entity types covered** (each supports all three permission types):

`Project`, `Annotation`, `Goal`, `Actor`, `Stakeholder`, `GlossaryTerm`, `Story`, `UseCase`,
`Scenario`, `ReportGenerator`

The `StakeholderPermissionsInitializer` populates the permission catalog (29 rows: 9 entity types ×
3 permission types + `Project` × 2 — Project has no Delete permission) at startup. The catalog
rows exist in the H2 test database automatically.

**Permission key format:** `{simpleClassName}[{permissionType}]`  
Example: `Goal[Edit]`, `Story[Delete]`, `Actor[Grant]`

#### Test-specific users (for permission-boundary tests)

Create named test users via the `EditUser` command inside a `@BeforeAll` method. Use
`@TestInstance(Lifecycle.PER_CLASS)` so the fixture is built once per test class, not once per
test method (user + project creation is expensive in a Spring context).

Recommended test personas:

| Test user | Password | Stakeholder permissions to grant |
|---|---|---|
| `test-editor` | `test-editor` | `Project[Edit]`, `Goal[Edit]`, `Actor[Edit]`, `Story[Edit]`, `UseCase[Edit]`, `Scenario[Edit]`, `GlossaryTerm[Edit]`, `Stakeholder[Edit]`, `ReportGenerator[Edit]`, `Annotation[Edit]` |
| `test-deleter` | `test-deleter` | `Goal[Delete]`, `Actor[Delete]`, `Story[Delete]`, `UseCase[Delete]`, `Scenario[Delete]`, `GlossaryTerm[Delete]`, `Stakeholder[Delete]`, `ReportGenerator[Delete]`, `Annotation[Delete]` (no Edit) |
| `test-granter` | `test-granter` | `Goal[Grant]` only |
| `test-noaccess` | `test-noaccess` | no stakeholder on the project at all |

**Do not hardcode passwords as plain strings in test source.** Define them as constants in a shared
`TestUsers` class (e.g., `modules/requel-app/src/test/java/.../TestUsers.java`) that is excluded
from production builds.

#### Recommended test fixture pattern

```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(Lifecycle.PER_CLASS)
class AuthorizationIT {

    @Autowired CommandHandler commandHandler;
    @Autowired UserRepository userRepository;
    @Autowired ProjectRepository projectRepository;

    private String editorToken;
    private String deleterToken;
    private String noAccessToken;

    @BeforeAll
    void setUpFixture() throws Exception {
        // 1. Create test project via EditProject command (as admin)
        //    Store project name for later lookups.

        // 2. Create test users via EditUser command.

        // 3. Add editor as UserStakeholder on the test project via EditUserStakeholder command.
        //    Grant Project[Edit], Goal[Edit], Actor[Edit], Story[Edit], UseCase[Edit],
        //    Scenario[Edit], GlossaryTerm[Edit], Stakeholder[Edit], ReportGenerator[Edit],
        //    Annotation[Edit].

        // 4. Add deleter as UserStakeholder; grant all [Delete] permissions — Goal, Actor, Story,
        //    UseCase, Scenario, GlossaryTerm, Stakeholder, ReportGenerator, Annotation (no Edit).

        // 5. Add granter as UserStakeholder; grant Goal[Grant] only.

        // 6. Do NOT add test-noaccess to the project.

        // 7. Log each user in via POST /api/auth/login and store the JWT token
        //    for use in MockMvc Authorization headers in @Test methods.
    }
}
```

The JWT tokens are captured in `@BeforeAll` and reused across `@Test` methods. Each `@Test` builds
its `MockMvc` request with `.header("Authorization", "Bearer " + token)`.

#### Permission matrix to test

The authorization tests should cover at least the following cells. Each row is a command against
the test project created in `@BeforeAll`. The `test-editor` has all `[Edit]` permissions;
`test-deleter` has all `[Delete]` permissions but no `[Edit]`; `test-granter` has only
`Goal[Grant]`; `test-noaccess` has no stakeholder on the project.

**Project-level commands:**

| Command | `admin` | `test-editor` | `test-deleter` | `test-granter` | `test-noaccess` | Unauthenticated |
|---|---|---|---|---|---|---|
| `EditProject` | 200 | 200 | **403** | **403** | **403** | **401** |

> Note: `Project` has no Delete permission — `DeleteProject` is not a supported command.

**Goal commands:**

| Command | `admin` | `test-editor` | `test-deleter` | `test-granter` | `test-noaccess` | Unauthenticated |
|---|---|---|---|---|---|---|
| `EditGoal` (create) | 200 | 200 | **403** | **403** | **403** | **401** |
| `DeleteGoal` | 200 | **403** | 200 | **403** | **403** | **401** |
| `AddGoalToGoalContainer` | 200 | 200 | **403** | **403** | **403** | **401** |
| `RemoveGoalFromGoalContainer` | 200 | **403** | 200 | **403** | **403** | **401** |
| `EditGoalRelation` | 200 | 200 | **403** | **403** | **403** | **401** |
| `RemoveGoalRelation` | 200 | 200 | **403** | **403** | **403** | **401** |

> Goal relation commands require `Goal[Edit]` (relations are a structural property of the goal,
> not a standalone entity type).

**Actor commands:**

| Command | `admin` | `test-editor` | `test-deleter` | `test-granter` | `test-noaccess` | Unauthenticated |
|---|---|---|---|---|---|---|
| `EditActor` (create) | 200 | 200 | **403** | **403** | **403** | **401** |
| `DeleteActor` | 200 | **403** | 200 | **403** | **403** | **401** |
| `AddActorToActorContainer` | 200 | 200 | **403** | **403** | **403** | **401** |
| `RemoveActorFromActorContainer` | 200 | **403** | 200 | **403** | **403** | **401** |

**Story commands:**

| Command | `admin` | `test-editor` | `test-deleter` | `test-granter` | `test-noaccess` | Unauthenticated |
|---|---|---|---|---|---|---|
| `EditStory` (create) | 200 | 200 | **403** | **403** | **403** | **401** |
| `DeleteStory` | 200 | **403** | 200 | **403** | **403** | **401** |
| `AddStoryToStoryContainer` | 200 | 200 | **403** | **403** | **403** | **401** |
| `RemoveStoryFromStoryContainer` | 200 | **403** | 200 | **403** | **403** | **401** |

**UseCase commands:**

| Command | `admin` | `test-editor` | `test-deleter` | `test-granter` | `test-noaccess` | Unauthenticated |
|---|---|---|---|---|---|---|
| `EditUseCase` (create) | 200 | 200 | **403** | **403** | **403** | **401** |
| `DeleteUseCase` | 200 | **403** | 200 | **403** | **403** | **401** |

**Scenario commands:**

| Command | `admin` | `test-editor` | `test-deleter` | `test-granter` | `test-noaccess` | Unauthenticated |
|---|---|---|---|---|---|---|
| `EditScenario` (create) | 200 | 200 | **403** | **403** | **403** | **401** |
| `DeleteScenario` | 200 | **403** | 200 | **403** | **403** | **401** |
| `EditScenarioStep` | 200 | 200 | **403** | **403** | **403** | **401** |
| `DeleteScenarioStep` | 200 | **403** | 200 | **403** | **403** | **401** |
| `SetPrimaryScenarioOnUseCase` | 200 | 200 | **403** | **403** | **403** | **401** |
| `AddScenarioToUseCase` | 200 | 200 | **403** | **403** | **403** | **401** |
| `RemoveScenarioFromUseCase` | 200 | **403** | 200 | **403** | **403** | **401** |

> Scenario step commands require `Scenario[Edit]` / `Scenario[Delete]`.

**GlossaryTerm commands:**

| Command | `admin` | `test-editor` | `test-deleter` | `test-granter` | `test-noaccess` | Unauthenticated |
|---|---|---|---|---|---|---|
| `EditGlossaryTerm` (create) | 200 | 200 | **403** | **403** | **403** | **401** |
| `DeleteGlossaryTerm` | 200 | **403** | 200 | **403** | **403** | **401** |

**Stakeholder commands:**

| Command | `admin` | `test-editor` | `test-deleter` | `test-granter` | `test-noaccess` | Unauthenticated |
|---|---|---|---|---|---|---|
| `EditUserStakeholder` (add) | 200 | 200 | **403** | **403**¹ | **403** | **401** |
| `EditNonUserStakeholder` (add) | 200 | 200 | **403** | **403**¹ | **403** | **401** |
| `DeleteStakeholder` | 200 | **403** | 200 | **403** | **403** | **401** |

> ¹ `test-granter` holds `Goal[Grant]` which allows granting Goal permissions to others, but
> `EditUserStakeholder` itself requires `Stakeholder[Edit]`. Without that, the command is rejected
> before the Grant permission is consulted.

**ReportGenerator commands:**

| Command | `admin` | `test-editor` | `test-deleter` | `test-granter` | `test-noaccess` | Unauthenticated |
|---|---|---|---|---|---|---|
| `EditReportGenerator` (create) | 200 | 200 | **403** | **403** | **403** | **401** |
| `DeleteReportGenerator` | 200 | **403** | 200 | **403** | **403** | **401** |

**Annotation commands:**

| Command | `admin` | `test-editor` | `test-deleter` | `test-granter` | `test-noaccess` | Unauthenticated |
|---|---|---|---|---|---|---|
| `EditIssue` (create) | 200 | 200 | **403** | **403** | **403** | **401** |
| `DeleteIssue` | 200 | **403** | 200 | **403** | **403** | **401** |
| `EditPosition` (create) | 200 | 200 | **403** | **403** | **403** | **401** |
| `DeletePosition` | 200 | **403** | 200 | **403** | **403** | **401** |
| `EditArgument` (create) | 200 | 200 | **403** | **403** | **403** | **401** |
| `DeleteArgument` | 200 | **403** | 200 | **403** | **403** | **401** |

> Annotation commands use the `groupingObject` (typically the project) as the project context for
> the `ProjectScopedCommand` interface. All annotation types share the `Annotation[Edit]` /
> `Annotation[Delete]` permission.

**User account commands:**

| Command | `admin` | `test-editor` | `test-deleter` | `test-granter` | `test-noaccess` | Unauthenticated |
|---|---|---|---|---|---|---|
| `EditUser` (own account) | 200 | 200 | 200 | 200 | 200 | **401** |
| `EditUser` (other account) | 200 | **403** | **403** | **403** | **403** | **401** |
| `EditUser` (own password change) | 200 | 200 | 200 | 200 | 200 | **401** |

> Own-account edits require only authentication (no system role or project permission). Editing
> another user's account requires `SystemAdminUserRole`.

Bold **403/401** cells are the boundary conditions most likely to regress; prioritize these.

### 2.6 XML import/export ✓ DONE

`ProjectXmlStreamingRoundTripIT` covers:

- Story `primaryActor` captured in `StorySummary` and verified in `assertSnapshotsEquivalent`
- Use-case `primaryActor` captured in `UseCaseSummary` and verified in `assertSnapshotsEquivalent`
- Exported XML validated against `project.xsd` in both round-trip tests
- Sample `Requel.xml` import verified for canonical glossary term preservation

`ProjectXmlRoundTripIT` is `@Disabled` — superseded by the streaming variant.

---

## 3. Angular Test Strategy

### 3.1 Framework and tooling ✓ DONE

Angular 21's `@angular/build:unit-test` builder uses **Vitest** under the hood with **jsdom** as
the browser environment. This replaces the old Karma/Jasmine setup. Tests are `.spec.ts` files
co-located with the source files they test.

**Required packages to add:**

```bash
npm install --save-dev @testing-library/angular @testing-library/user-event @testing-library/jest-dom
```

`@testing-library/angular` wraps Angular's `TestBed` with a DOM-first API that encourages testing
behavior rather than implementation. It pairs well with Vitest.

**Configure `vitest` in `angular.json`** (the `@angular/build:unit-test` builder accepts a
`vitest` config key):

```json
"test": {
  "builder": "@angular/build:unit-test",
  "options": {
    "include": ["src/**/*.spec.ts"],
    "setupFiles": ["src/test-setup.ts"]
  }
}
```

> **Note:** The `@angular/build:unit-test` builder in Angular 19+ uses Vitest with jsdom by
> default — no extra `browser: false` flag is needed. Verify the exact option names against the
> Angular 21 release notes if behaviour differs from what is described here.

`src/test-setup.ts`:
```ts
import '@testing-library/jest-dom';
```

**Angular coverage** requires an additional package — Vitest does not bundle a coverage provider:

```bash
npm install --save-dev @vitest/coverage-v8
```

Once installed, `npm test -- --coverage` generates coverage reports. Recommended outputs:

- HTML report for local developer use
- LCOV / text summary for CI

Recommended initial policy:

- Start with unit-test coverage reporting only
- Review coverage per frontend app rather than trying to combine it with Java coverage

### 3.2 Service tests ✓ DONE

Services are the backbone of the Angular app. Tests mock `HttpClient` using
`provideHttpClientTesting()` and verify the request/response shape.

| Spec file | What to verify |
|---|---|
| `auth.service.spec.ts` | `login()` sends `POST /api/auth/login`; stores JWT; `logout()` clears token; `isAuthenticated` computed signal reflects token state |
| `command.service.spec.ts` | `execute()` sends `POST /api/commands/{type}`; returns `CommandResult`; propagates 400/422/409 errors as structured `CommandResult.error` |
| `project.service.spec.ts` | `listProjects()` returns array; `notifyTreeChanged()` emits on the internal event subject (`treeChanged$`) |
| `goal.service.spec.ts` | `listGoals()`, `getGoal()` — correct URL construction and response mapping |
| `story.service.spec.ts` | Same; verify `primaryActorName` is included in `StoryDto` |
| `actor.service.spec.ts` | `listActors()`, `getActor()` |
| `use-case.service.spec.ts` | `listUseCases()`, `getUseCase()` |
| `scenario.service.spec.ts` | `listScenarios()`, `getScenario()` |
| `stakeholder.service.spec.ts` | `listStakeholders()`, `getStakeholder()` — user and non-user variants |
| `term.service.spec.ts` | `listTerms()`, `getTerm()` — correct URL and response mapping |
| `report.service.spec.ts` | `listReports()`, `getReport()`, `runReport()` — verify request shapes and response mapping |
| `user.service.spec.ts` | `listUsers()`, `getUser()`, `listOrganizations()`, `listRoles()` |
| `preferences.service.spec.ts` | `getPreferences()` loads from `GET /api/user-preferences`; `savePreferences()` sends `PUT`; signal reflects saved values |
| `annotation.service.spec.ts` | `listAnnotations()` sends `GET /api/annotations`; annotation types (issue, position, argument) are correctly mapped |
| `permission.service.spec.ts` | `canEdit()` / `canDelete()` return correct values after `loadForProject()`; permissions cached per project |
| `event-stream.service.spec.ts` | `addSubscription()` registers; `removeSubscription()` deregisters; `events$` emits parsed SSE envelope |
| `auth.guard.spec.ts` | Redirects unauthenticated user to `/login`; passes authenticated user through |
| `dirty-check.guard.spec.ts` | Returns `true` when no unsaved changes; shows confirm dialog when `hasUnsavedChanges()` is true |
| `auth.interceptor.spec.ts` | Attaches `Authorization: Bearer <token>` header; redirects to `/login` on 401 response |

### 3.3 Shared component tests ✓ DONE

| Spec file | What to verify |
|---|---|
| `list-page.spec.ts` | Renders `[title]` in header; shows `[actions]` slot; emits `(search)` event on input; renders `<ng-content>` in body |
| `entity-selector-dialog.spec.ts` | Hidden when `[visible]="false"`; shows list when visible; emits `(selected)` with correct `EntityReferenceDto` on row click; emits `(closed)` on cancel |
| `annotations-section.spec.ts` | Renders no annotations when empty; renders issue/position/argument tree; Add Issue button visible when `[canEdit]="true"`, hidden otherwise |
| `sidebar-nav.spec.ts` | Renders project list; project nodes expand to show entity types; active route is highlighted; `notifyTreeChanged()` triggers reload |
| `scenario-selector-dialog.spec.ts` | Filters by `[excludeIds]`; hidden when `[visible]="false"`; shows list when visible; emits `(selected)` on row click |

### 3.4 Feature page tests (editors) ✓ DONE

For editor components, the key behaviors to test are: loading state, save behavior, dirty tracking,
and error display. Mock the injected services.

| Spec file | Key scenarios |
|---|---|
| `login.spec.ts` | Submit with valid credentials → navigates to `/projects`; invalid credentials → shows error message |
| `project-list.spec.ts` | Renders project rows; search filters visible rows; New Project button navigates to `/projects/new` |
| `project-editor.spec.ts` | Loads existing project by name; Save sends `EditProject` command; delete confirmation dialog |
| `goal-editor.spec.ts` | New goal form is empty; save sends `EditGoal` with correct payload; dirty flag set on input change; dirty flag cleared after save; hasChanges blocks navigation when dirty; Add/Remove Goal relationship works |
| `story-editor.spec.ts` | Primary actor `p-select` is populated with actors; selecting actor sets `primaryActorName` in payload; clearing actor sends `null`; Additional Actors section shows/hides Add button per permissions |
| `actor-editor.spec.ts` | Loads actor; saves; Copy button triggers confirm + `CopyActor` command + navigation |
| `use-case-editor.spec.ts` | Primary actor dropdown populated; primary scenario create/select flows; Additional Scenarios add/remove; Goals and Stories tables show associated entities |
| `scenario-editor.spec.ts` | Step table renders; Add Step appends row; drag-to-reorder (if implemented); Save sends steps array |
| `stakeholder-editor.spec.ts` | User stakeholder: user selector shown; Non-user stakeholder: name input shown; correct command type sent for each |
| `term-editor.spec.ts` | Loads term; displays term text; Save sends `EditGlossaryTerm` command; Delete confirmation sends `DeleteGlossaryTerm` |
| `report-editor.spec.ts` | Loads report generator config; Save sends `EditReportGenerator`; Run button triggers report run and shows download link |
| `settings.spec.ts` | Loads current preferences from `GET /api/user-preferences`; Save sends `PUT`; values persist across reload |
| `account-editor.spec.ts` | User can edit own display name and email; password change form validates confirm-match; Save sends `EditUser` command |
| `admin-user-list.spec.ts` | Admin sees user list; click navigates to admin user editor; non-admin cannot reach admin route |
| `user-editor.spec.ts` | Admin sees all fields including role assignment; non-admin editing self sees limited fields; password change form validation |
| `open-issues.spec.ts` | Issues render with type badge; click navigates to annotated entity editor |

### 3.5 List page tests ✓ DONE

List pages are simpler — verify data loading, search filtering, and navigation.

| Spec file | Key scenarios |
|---|---|
| `goal-list.spec.ts` | Goals loaded on init; search filters by name; row click navigates to editor |
| `story-list.spec.ts` | Same pattern; storyType column visible |
| `actor-list.spec.ts` | Same |
| `use-case-list.spec.ts` | No search bar (confirm `[showSearch]="false"` in template) |
| `scenario-list.spec.ts` | Same |
| `stakeholder-list.spec.ts` | Both user and non-user stakeholders shown |

---

## 4. End-to-End Browser Automation

### 4.1 Tool recommendation: Playwright

**[Playwright](https://playwright.dev/)** is the recommended E2E tool for Requel 2.0.

Why Playwright over alternatives:
- **Multi-browser** — Chromium, Firefox, and WebKit from one test suite
- **Headless by default**, full browser available with `--headed` flag for debugging
- **TypeScript-native** — test files are `.ts`, no extra config
- **Built-in waiting** — auto-waits for elements to be visible, no `sleep()` calls
- **Angular-aware** — Playwright's `page.getByRole()` / `page.getByLabel()` APIs work well with Angular's accessibility attributes
- **Tracing and screenshots** on failure built in
- **API request interception** — useful for testing error states without a real backend

The main alternative is **Cypress**, which has a more polished developer UI but runs only in
Chromium and has architectural limitations around multiple tabs (relevant for SSE multi-session
tests).

### 4.2 Installation

```bash
cd requel-angular
npm install --save-dev @playwright/test
npx playwright install chromium firefox webkit
```

Add to `package.json`:
```json
"scripts": {
  "e2e": "playwright test",
  "e2e:headed": "playwright test --headed",
  "e2e:report": "playwright show-report"
}
```

`playwright.config.ts` at repo root (or `requel-angular/`):

**Packaged-app mode** (recommended for CI and production-parity): the Spring Boot JAR serves both the
Angular SPA and the API. Both run on the same port.

```ts
import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,          // Requel has shared DB state; run serially
  retries: 1,
  use: {
    baseURL: 'http://localhost:8080',   // Spring Boot serves Angular + API on :8080
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
  },
  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
    { name: 'firefox',  use: { ...devices['Desktop Firefox'] } },
  ],
});
```

**Dev-server mode** (optional, for hot-reload during E2E authoring): run the Angular dev server on
`:4200` with `proxy.conf.json` forwarding `/api` to Spring Boot on `:8081`. Change `baseURL` to
`http://localhost:4200` in this mode. Do not mix the two — pick one per environment.

### 4.2.1 Dev-only reset endpoints for E2E

E2E tests need built-in users (`admin`, `project`, …) in known-canonical state at the start
of every run, regardless of what happened to those records during prior debugging or test
runs on a developer's local database. To support this, the server exposes dev-only reset
endpoints under `/api/dev/*` that re-seed each built-in user. The endpoints are registered
only when the matching `requel.dev.reset-*.enabled` flag is `true`, so they are absent from
production builds.

| Endpoint | Property flag | What it resets |
|---|---|---|
| `POST /api/dev/reset-admin` | `requel.dev.reset-admin.enabled` | admin's password (default `admin`) |
| `POST /api/dev/reset-project` | `requel.dev.reset-project.enabled` | project user's name, password (`project`), and roles (forces back to `ProjectUserRole` only — drops any drift such as a manually-added `SystemAdminUserRole`) |

`e2e/global-setup.ts` calls both endpoints before every E2E run; if either is absent (404),
the call is silently skipped with a console warning.

The simplest way to enable both endpoints (and a couple of other dev-only conveniences like
CORS for the Angular dev server) is to activate the `dev` Spring profile when running the
JAR locally:

```bash
java -jar modules/requel-app/target/requel-app-2.0.0-dev.jar \
  --spring.profiles.active=dev \
  --server.port=8080
```

This activates `modules/requel-app/src/main/resources/application-dev.properties` which
flips both reset-endpoint flags. Equivalent explicit-flag form (useful for opting in to
only one endpoint, or for non-`dev` profile situations):

```bash
java -jar modules/requel-app/target/requel-app-2.0.0-dev.jar \
  --requel.dev.reset-admin.enabled=true \
  --requel.dev.reset-project.enabled=true \
  --server.port=8080
```

Docker-compose runs (`docker-compose up`) start with a fresh database every time and don't
activate the `dev` profile — those endpoints are useful primarily for developers running
e2e against a persistent local install.

### 4.3 Test generation approach

Writing Playwright tests from scratch is slow and produces fragile selectors. The recommended
workflow combines three tools: **`playwright codegen`** for recording happy paths, the
**Page Object Model** pattern for keeping selector logic out of tests, and **AI-assisted
generation** (Claude Code) for scenarios that are hard to record (error states, permission
boundaries, multi-tab SSE).

#### Step 0 — Verify and add ARIA roles to Angular components

Playwright's preferred selectors (`getByRole`, `getByLabel`) require that components have
correct ARIA roles and label associations. Before writing any E2E tests, audit the templates and
fix gaps.

A component-by-component audit of the codebase was performed and all gaps were fixed before this
plan was finalised. The table below records what was found and corrected:

| Component | Original status | Fix applied |
|---|---|---|
| `goal-editor`, `story-editor`, `actor-editor`, `use-case-editor`, `scenario-editor`, `project-editor`, `login` | **GOOD** | No changes needed |
| All list pages (`goal-list`, `story-list`, etc.) | **GOOD** | Rely on `list-page` wrapper; see below |
| `list-page` | **PARTIAL** | Added `aria-label="Search"` to the search input |
| `entity-selector-dialog` | **PARTIAL** | Added `aria-label="Search"` to search input; added `ariaLabel="Filter by type"` to type-filter `p-select` |
| `scenario-selector-dialog` | **PARTIAL** | Added `id`/`for` pairs on create-form inputs; added `aria-label="Search scenarios"` to toolbar search input |
| `annotations-section` | **PARTIAL** | Added `aria-label="Note text"` and `aria-label="Issue text"` to form textareas |
| `sidebar-nav` | **NEEDS WORK** | Added `ariaLabel="New project"` / `ariaLabel="Import project"` to action buttons; added `aria-label` to sidebar links |

If a future component is added without correct labels, the rule is: prefer `aria-label` or `<label for>` first;
use `data-testid` only when no semantic approach is possible. Every `data-testid` is a maintenance commitment.

#### Step 1 — Record happy paths with `playwright codegen`

`playwright codegen` opens a browser and an inspector side-panel. Every interaction you perform
(click, fill, select) is translated into Playwright TypeScript code in real time. Start the app,
then run:

```bash
# packaged-app mode
npx playwright codegen http://localhost:8080

# dev-server mode
npx playwright codegen http://localhost:4200
```

Use codegen to record one complete flow per E2E file (e.g., create project → create goal → save →
verify). Copy the generated code into the corresponding `e2e/*.e2e.ts` file, then clean it up:
- Replace fragile CSS selectors (`locator('.p-select-option')`) with ARIA-role selectors
  (`getByRole('option', { name: 'Alice' })`)
- Extract repeated selector logic into the Page Object for that feature

PrimeNG components have a predictable DOM structure that codegen handles well, but the generated
selectors sometimes target internal implementation divs. Prefer:

| PrimeNG component | Preferred Playwright selector |
|---|---|
| `p-select` / `p-dropdown` | `getByRole('combobox', { name: 'Label' })` to open; `getByRole('option', { name: 'value' })` to pick |
| `p-table` row | `getByRole('row', { name: 'row text' })` |
| `p-dialog` | `getByRole('dialog')` |
| `p-button` | `getByRole('button', { name: 'Save' })` |
| `p-inputtext` | `getByLabel('Field label')` |

When no ARIA role or label is available (e.g., a custom component with no label), add a
`data-testid` attribute to the Angular template and select with `getByTestId('...')`. Keep
`data-testid` attributes minimal — only add them where the ARIA approach genuinely fails.

#### Step 2 — Page Object Model

Wrap each major page or dialog in a Page Object class. Tests call methods on the POM; selectors
live in the POM, not in the test. This means a UI change only requires updating one file.

Recommended structure:

```
requel-angular/e2e/
  pages/
    LoginPage.ts
    ProjectListPage.ts
    GoalEditorPage.ts
    StoryEditorPage.ts
    ... (one file per editor/list page)
  fixtures/
    auth.ts          # shared login fixture
    test-data.ts     # project/entity setup helpers via API
  auth.e2e.ts
  goals.e2e.ts
  stories.e2e.ts
  ...
```

Minimal POM example:

```ts
// e2e/pages/GoalEditorPage.ts
import { Page } from '@playwright/test';

export class GoalEditorPage {
  constructor(private page: Page) {}

  async navigate(projectName: string, goalId: string) {
    await this.page.goto(`/projects/${projectName}/goals/${goalId}`);
  }

  nameInput()    { return this.page.getByLabel('Name'); }
  saveButton()   { return this.page.getByRole('button', { name: 'Save' }); }
  deleteButton() { return this.page.getByRole('button', { name: 'Delete' }); }

  async save() {
    await this.saveButton().click();
    await this.page.waitForResponse(r => r.url().includes('/api/commands/') && r.status() === 200);
  }
}
```

Tests then read like documentation:

```ts
test('rename goal persists after reload', async ({ page }) => {
  const editor = new GoalEditorPage(page);
  await editor.navigate('TestProject', goalId);
  await editor.nameInput().fill('Renamed Goal');
  await editor.save();
  await page.reload();
  await expect(editor.nameInput()).toHaveValue('Renamed Goal');
});
```

#### Step 3 — Shared authentication fixture

Logging in via the UI for every test is slow and flaky. Instead, use Playwright's `storageState`
to capture the browser's auth cookies/localStorage after one login, then reuse the saved state
for the rest of the suite.

`e2e/fixtures/auth.ts`:

```ts
import { test as base, expect } from '@playwright/test';

type AuthFixtures = { adminPage: Page; editorPage: Page };

export const test = base.extend<AuthFixtures>({
  adminPage: async ({ browser }, use) => {
    const ctx = await browser.newContext({ storageState: 'e2e/.auth/admin.json' });
    const page = await ctx.newPage();
    await use(page);
    await ctx.close();
  },
  // add editorPage, deleterPage etc. as needed
});
```

Generate the saved state once in a `globalSetup` script (`e2e/global-setup.ts`) that logs in as
each test user and writes `e2e/.auth/<username>.json`. Add `e2e/.auth/` to `.gitignore`.

```ts
// e2e/global-setup.ts
import { chromium } from '@playwright/test';

async function saveAuthState(username: string, password: string) {
  const browser = await chromium.launch();
  const page = await browser.newPage();
  await page.goto('http://localhost:8080/login');
  await page.getByLabel('Username').fill(username);
  await page.getByLabel('Password').fill(password);
  await page.getByRole('button', { name: 'Login' }).click();
  await page.waitForURL('**/projects');
  await page.context().storageState({ path: `e2e/.auth/${username}.json` });
  await browser.close();
}

export default async function globalSetup() {
  await saveAuthState('admin', 'admin');
  await saveAuthState('project', 'project');
  // add test-specific users once they are created via the Java API in a setup step
}
```

In `playwright.config.ts`:
```ts
globalSetup: './e2e/global-setup.ts',
```

#### Step 4 — AI-assisted generation for scenarios that cannot be recorded

Some scenarios cannot be captured by `playwright codegen` because they require:
- A specific server-side state (expired JWT, a goal that exists only under certain permissions)
- Multi-browser-context coordination (SSE live refresh test)
- API-level request interception (simulating a 401 mid-session)

For these, use Claude Code with the prompt pattern:

> "Here is the GoalEditorPage POM (`e2e/pages/GoalEditorPage.ts`). Here is the goal service
> (`requel-angular/src/app/core/goal.service.ts`). Write a Playwright test in `goals.e2e.ts`
> that verifies the dirty guard fires when a user navigates away with unsaved changes — the
> confirm dialog appears, and cancelling leaves the user on the editor page."

Provide the POM, the relevant service, and the specific scenario from section 4.4. The output
will need review — verify selectors against the live DOM with `npx playwright codegen` if in doubt.

#### Step 5 — Test data setup via API, not UI

For tests that need a project with existing goals/actors before the scenario starts, prefer
creating the data via the REST API (using Playwright's `request` fixture) rather than
navigating through the UI to create it. This keeps tests independent of UI flows they are not
testing.

```ts
test.beforeEach(async ({ request }) => {
  // Create a goal via API before the test that exercises goal editing
  await request.post('/api/commands/EditGoal', {
    headers: { Authorization: `Bearer ${adminToken}` },
    data: { projectName: 'TestProject', name: 'Fixture Goal', text: 'Setup goal' }
  });
});
```

### 4.4 E2E test scenarios

Tests live in `requel-angular/e2e/`. The Spring Boot backend + MySQL must be running before
the E2E suite runs (use `docker-compose up` for CI).

#### Authentication ✓ done

| Status | File | Scenario |
|---|---|---|
| ✓ | `auth.e2e.ts` | Login with valid credentials → lands on projects page |
| ✓ | | Login with bad credentials → error message shown, no navigation |
| ✓ | | Accessing `/projects` while logged out → redirected to `/login` |
| ✓ | | JWT expiry → subsequent API call redirects to login. Covered by `expired JWT on API call triggers interceptor logout and redirects to /login`, which logs in, intercepts every `/api/**` (except `/auth/login`) with a 401 to simulate token expiry, reloads the page, and asserts both the redirect to `/login` and that `localStorage.requel_token` is cleared. Same test covers the "Token expires mid-session" row in Forbidden-state UX below. |

Extra tests implemented (not in original plan): admin sees admin section in sidebar; project user does not see admin section.

#### Project management ✓ done

| Status | File | Scenario |
|---|---|---|
| ✓ | `projects.e2e.ts` | Create new project → appears in sidebar and project list |
| ✓ | | Edit project name → new name shown in header and sidebar |
| ✓ | | Import project XML → all imported entities appear in the correct lists. Covered by both the mocked-API banner test (`import project success shows banner and refreshed list entry`) and the real round-trip test below, which seeds a project with a goal/actor/story via API, exports via the UI, and re-imports the resulting XML, then asserts the goal/actor/story names appear in the imported project's goal-, actor-, and story-list pages. Negative branches: `import project failure shows error banner`, `import change with no selected file shows warning and does not call import`. |
| ✓ | | Export project XML → file downloads; re-import round-trips cleanly. Covered by `export project XML and re-import round-trips goals, actors, and stories` — captures the browser download triggered by the project-editor Export button (Content-Disposition: attachment), validates the exported XML contains the seeded entity names, then feeds the same file back through the project-list import input and verifies the imported project (auto-named `Imported Project` via `resolveProjectName()`) holds those entities. |

Extra tests implemented: cancel on project editor navigates back; dirty guard — navigate away with unsaved changes; restricted user without `createProjects` permission hides New Project and Import; project save validation/generic/network failures all show error banner; import-with-no-file warning branch.

**Out of scope for 2.0 — Delete project.** No `DeleteProject` command exists in the backend, no soft-delete infrastructure (`deletedAt`, `@SQLDelete`, `Project[Delete]` permission row) exists, and the Authorization section above explicitly notes "`Project` has no Delete permission — `DeleteProject` is not a supported command." The `e2e/fixtures/api-helper.ts` `deleteProject` helper is a documented no-op. Adding project delete is a feature change (multi-aggregate cascade across goals, stories, actors, use-cases, scenarios, stakeholders, terms, reports, plus annotations attached to all of them); if/when it's prioritized, capture the design in a dedicated `doc/project-delete-support-plan.md` rather than treating it as a missing E2E test row.

#### Goals ✓ done

| Status | File | Scenario |
|---|---|---|
| ✓ | `goals.e2e.ts` | Create goal → appears in goal list |
| ✓ | | Rename goal → new name persists after save and page reload |
| ✓ | | Add/remove goal relation → relation visible on both goals |
| ✓ | | Delete goal → removed from list |
| ✓ | `use-cases.e2e.ts` | Navigate to goal from use-case goals table → opens correct editor. Covered by `click goal link in use-case goals table → navigates to goal editor`, which seeds a use case + goal, links them via `addGoalToUseCase`, then clicks the `use-case-goal-link` testid in the goals sub-table and asserts the URL matches `/goals/\d+$` and the goal-editor name field is populated correctly. |
| ✓ | `dirty-guard.e2e.ts` | Dirty guard → navigate away with unsaved changes → cancel dialog stays on editor; confirm dialog navigates away |

Extra tests implemented: back button navigates to goal list.

#### Stories ✓ done

| Status | File | Scenario |
|---|---|---|
| ✓ | `stories.e2e.ts` | Create story → appears in list |
| ✓ | | Set primary actor via dropdown → actor name shown in form after reload |
| ✓ | | Clear primary actor → field shows placeholder after reload |
| ✓ | | Add/remove additional actor |
| ✓ | | Change story type → persists after reload |

Extra tests implemented: rename story; delete story.

#### Actors ✓ done

| Status | File | Scenario |
|---|---|---|
| ✓ | `actors.e2e.ts` | Create actor → appears in list |
| ✓ | | Rename actor → new name persists after save and reload |
| ✓ | | Copy actor → new actor with modified name appears in list |
| ✓ | | Delete actor → removed from list |
| ✓ | | Actor appears in primary actor dropdown for story and use-case. Covered by `newly-created actor appears in the primary-actor dropdown for both story and use-case editors`, which creates a fresh actor via API, then opens `/stories/new` and `/use-cases/new` and asserts via the new `expectActorInPrimaryActorDropdown` page-object helper that the actor is one of the visible options in each editor's primary-actor `p-select`. |

#### Use Cases ✓ done

| Status | File | Scenario |
|---|---|---|
| ✓ | `use-cases.e2e.ts` | Create use case → appears in list |
| ✓ | | Set primary actor → actor name shown in form after reload |
| ✓ | | Primary scenario shown in card; Open in Editor navigates to scenario editor |
| ✓ | | SetPrimaryScenarioOnUseCase → links scenario and name shown in card |
| ✓ | | Add/remove additional scenario, goal, story, actor |

Note: EditUseCase always auto-creates a Primary scenario on new use case creation, so the "Create New" / "Select Existing" buttons (shown only when no primary scenario exists) are not reachable via the normal test setup and are not covered by E2E tests.

Extra tests implemented: rename use case; delete use case; copy use case.

#### Scenarios ✓ done

| Status | File | Scenario |
|---|---|---|
| ✓ | `scenarios.e2e.ts` | Create scenario → appears in list |
| ✓ | | Add step → name persists after save and reload |
| ✓ | | Step order change (remove + re-add) → persists after save and reload |
| ✓ | | Delete step → gone after save and reload |
| ✓ | | Edit step via popup → name and text persist after save and reload |
| ✓ | | Add Sub-scenario via selector dialog: pick existing scenario → appears as sub-scenario step (added 2026-05-07 to lift `scenario-selector-dialog.ts` E2E coverage from 36 %) |
| ✓ | | Add Sub-scenario via selector dialog: inline `New Scenario` form + `Create & Add` dispatches `EditScenario` from inside the dialog and appends the new scenario as a step |
| ✓ | | Add Sub-scenario via selector dialog: Escape dismisses the dialog without adding a step (covers `onHide` no-op exit) |

Extra tests implemented: rename scenario; change scenario type; copy scenario.

#### Annotations (IBIS) ✓ done

| Status | File | Scenario |
|---|---|---|
| ✓ | `annotations.e2e.ts` | Add issue to a goal → issue appears in annotations section |
| ✓ | | Add position to issue → position nested under issue |
| ✓ | | Add argument to position → argument nested |
| ✓ | | Resolve issue → status changes |
| ✓ | | Open-issues page shows unresolved issues; click navigates to annotated entity |
| ✓ | | Add note to a goal → note appears in annotations section |
| ✓ | | Delete note → note removed from annotations section |
| ✓ | | Delete issue → issue removed from annotations section |
| ✓ | | Delete position → position removed (and its arguments cascade) |
| ✓ | | Delete argument → argument removed from position |

**Coverage gap closed 2026-05-06.** Pre-existing audit of
`requel-angular/coverage/lcov.info` flagged
`src/app/core/annotation.service.ts` at 7/12 lines hit (58%) and 6/11
functions hit (55%) — the lowest-coverage core service in the report.
The five then-uncovered functions (`addNote`, `deleteNote`,
`deleteIssue`, `deletePosition`, `deleteArgument`) are now each
exercised by the five new rows above. A coverage rerun after these
tests land should bring `annotation.service.ts` above 90% line/function
coverage.

**Implementation notes (2026-05-06):**
- Added six prerequisite testids to `shared/annotations-section.ts`
  (`annotation-note`, `annotation-note-badge`, `annotation-delete-note`,
  `annotation-delete-issue`, `annotation-delete-position`,
  `annotation-delete-argument`) — they match the existing
  `annotation-issue` / `annotation-position` / `annotation-argument`
  naming pattern.
- Added `addNote(api, projectName, entityType, entityId, text)` and
  `addArgument(api, projectName, positionId, text, supportLevel?)`
  helpers to `e2e/fixtures/api-helper.ts` so the delete tests can seed
  annotations cheaply via `EditNote` / `EditArgument` commands instead
  of clicking through the UI.
- Each delete test uses `hasText` filtering to scope to the
  specifically-seeded annotation row, since the NLP assistant
  auto-creates lexical issues on every new goal — tests must never rely
  on being the only annotation in the section.

#### Glossary terms ✓ done

| Status | File | Scenario |
|---|---|---|
| ✓ | `terms.e2e.ts` | Create glossary term → appears in term list |
| ✓ | | Edit term text → persists after save and page reload |
| ✓ | | Delete term → removed from list |
| ✓ | | Save with empty name → `Term name is required.` validation message; no API call fires (added 2026-05-07 to lift `term-editor.ts` E2E coverage from 75 % toward 90 %) |
| ✓ | | Alternate-terms section: term B with `canonicalTermId = A` appears under A's "Alternate Terms" table; clicking the row navigates to B's editor (covers the `navigateToTerm()` and the `@if alternateTerms?.length` branch) |
| ✓ | | Setting a canonical term via the UI p-select → save → reload → the chosen canonical persists (verified via the canonical's "Alternate Terms" section showing the updated term back-pointer) |
| ✓ | | Back button → returns to term list (covers `onBack()`) |

#### Reports ✓ done

| Status | File | Scenario |
|---|---|---|
| ✓ | `reports.e2e.ts` | Create report generator → appears in report list |
| ✓ | | Edit report configuration → persists |
| ✓ | | Run report → output or download link appears |
| ✓ | | Delete report generator → removed from list |

#### Settings and preferences ✓ done

| Status | File | Scenario |
|---|---|---|
| ✓ | `settings.e2e.ts` | Change sidebar project limit and staleness filter → values persist after page reload |
| ✓ | | Reset to defaults → saved and reflected in UI (`reset to defaults → limit=10 and staleness=3 months after reload`) |

#### Account self-edit ✓ done

| Status | File | Scenario |
|---|---|---|
| ✓ | `account.e2e.ts` | Edit own display name → new name shown in header |
| ✓ | | Change password → can log out and log back in with new password |

Extra tests implemented: username is pre-filled and disabled on self-edit form.

#### Administration ✓ done

| Status | File | Scenario |
|---|---|---|
| ✓ | `admin.e2e.ts` | Create user and set roles |
| ✓ | | Verify newly created user can log in |
| ✓ | | Edit user account as admin → changes persist after reload |
| ✓ | | Non-admin cannot see admin nav link in sidebar (link-visibility layer; direct-URL navigation to admin routes is gated separately by `adminGuard` — see Forbidden-state UX below) |
| ✓ | | Change own password → can log in with new password |

#### Sidebar and project tree ✓ done

| Status | File | Scenario |
|---|---|---|
| ✓ | `sidebar.e2e.ts` | Import project XML via sidebar → project appears in list; entities visible in tree |
| ✓ | | Edit a goal → project tree refreshes without full page reload (SSE event triggers update) |

#### Forbidden-state UX (401 / 403) ✓ done

| Status | File | Scenario |
|---|---|---|
| ✓ | `forbidden.e2e.ts` | Accessing any protected route while logged out → redirected to `/login` |
| ✓ | `forbidden.e2e.ts` | Authenticated user accessing admin route without admin role → redirected to dashboard `/` by `adminGuard`. The new `core/admin.guard.ts` is wired onto both `/users` and `/users/:username` in `app.routes.ts` (in addition to the parent layout's `authGuard`, so unauthenticated users still bounce to `/login`). Two e2e tests in `forbidden.e2e.ts` cover the project-user paths to `/users` and `/users/:username`; both assert a redirect to `/` (not `/login`, since the user IS authenticated, just not authorized). The previous "no admin route guard" test was flipped to assert the new behavior. Unit coverage in `core/admin.guard.spec.ts` mirrors `auth.guard.spec.ts` — admin → `true`, non-admin → `UrlTree('/')`, unauthenticated user → `UrlTree('/')`. The sidebar admin-link visibility check in `admin.e2e.ts:84` is independent and remains the UX-hint layer. |
| ✓ | | Token expires mid-session → next API call returns 401 → interceptor redirects to `/login`. Covered by the JWT expiry test in `auth.e2e.ts` (same scenario as the "JWT expiry" row in Authentication above — one test covers both). |

#### SSE live refresh ✓ done

| Status | File | Scenario |
|---|---|---|
| ✓ | `sse-refresh.e2e.ts` | Open goal editor in browser context A; edit and save the same goal via API call (or second browser context B); goal editor in A reloads automatically without manual refresh |

The implemented test simulates the "second context" via an authenticated API
call rather than a second browser context — from the SSE pipeline's
perspective both look identical, since both produce backend command
broadcasts that the already-open editor reacts to. A sentinel pinned in
`window.__sseRefreshSentinel` is checked at the end of the test to prove
no full page reload happened during the refresh.

### 4.5 CI integration

For GitHub Actions or similar:

```yaml
- name: Start Requel
  run: docker-compose up -d
  
- name: Wait for health
  run: |
    for i in {1..30}; do
      curl -sf http://localhost:8080/actuator/health && break || sleep 2
    done

- name: Run E2E
  run: cd requel-angular && npm run e2e    # baseURL in playwright.config.ts → :8080

- name: Upload Playwright report
  if: always()
  uses: actions/upload-artifact@v4
  with:
    name: playwright-report
    path: requel-angular/playwright-report/
```

---

## 5. Coverage Priorities

Not everything needs to be written at once. Recommended order:

| Priority | Layer | What to write first |
|---|---|---|
| 0 | Java | **Migrate all 14 existing tests from JUnit 4 to JUnit 5** — do this before writing any new tests so the whole suite speaks one language |
| 1 | Java | Command tests for all entity CRUD operations — these are the most business-critical paths and the hardest to debug through the UI |
| 2 | Java | `CommandControllerTest` (MockMvc) — verifies the HTTP contract that the Angular app depends on |
| 3 | Angular | ~~Fix ARIA roles on shared components~~ — **done** (applied before this plan was committed; see section 4.3 Step 0 for the audit record). Any newly added component should follow the same labelling rules. |
| 4 | Angular | Service specs (`auth.service`, `command.service`, `permission.service`) — services used by every component |
| 5 | Angular | `dirty-check.guard.spec.ts`, `auth.guard.spec.ts`, `auth.interceptor.spec.ts` — cross-cutting concerns |
| 6 | Angular | Editor component specs for the main entity types (goal, story, use-case) |
| 7 | E2E | Auth flow + full project lifecycle (create project → goal → story → use-case → scenario) |
| 8 | Java | Repository tests and authorization tests |
| 9 | E2E | Annotations, SSE live refresh, admin flows |
| 10 | Angular | List page specs, remaining editor specs |

---

## 6. Running the test suites

```bash
# Java unit + integration tests
mvn test

# Java unit + integration tests with JaCoCo report
mvn verify

# Java integration tests only
mvn -pl modules/requel-app test -Dtest="*IT"

# Angular unit tests (Vitest)
cd requel-angular && npm test

# Angular unit tests with coverage
cd requel-angular && npm test -- --coverage

# E2E tests (packaged-app mode: Spring Boot + Angular on :8080 via docker-compose)
cd requel-angular && npm run e2e

# E2E headed (debug mode)
cd requel-angular && npm run e2e:headed
```

JaCoCo HTML coverage reports are generated under `target/site/jacoco/` for each Maven module.
