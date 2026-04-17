# Java Test Implementation Review

Reviewed against `doc/RELEASE_20_TEST_PLAN.md` and the current Java test suite under:

- `modules/requel-app/src/test/java`
- `modules/project-jpa/src/test/java`

Validation run:

- `mvn -pl modules/requel-app,modules/project-jpa -am -DskipAngularBuild=true test`
- Result: `344` tests run, `0` failures, `0` errors, `29` skipped

## Highest-priority issues

### 1. `*IT` integration tests are not part of the default Maven test run

This is the biggest gap in the current implementation.

- The build config only wires `maven-surefire-plugin` in [pom.xml](/Users/rregan_platformq/gh-acc/rreganjr/Requel/pom.xml:45).
- There is no `maven-failsafe-plugin` configuration, and the `mvn test` run did not produce Surefire reports for:
  - [ProjectXmlStreamingRoundTripIT.java](/Users/rregan_platformq/gh-acc/rreganjr/Requel/modules/requel-app/src/test/java/com/rreganjr/requel/ProjectXmlStreamingRoundTripIT.java:107)
  - [ProjectXmlRoundTripIT.java](/Users/rregan_platformq/gh-acc/rreganjr/Requel/modules/requel-app/src/test/java/com/rreganjr/requel/ProjectXmlRoundTripIT.java:101)
  - [ProjectUserCreationIT.java](/Users/rregan_platformq/gh-acc/rreganjr/Requel/modules/requel-app/src/test/java/com/rreganjr/requel/user/ProjectUserCreationIT.java:42)
  - [AuthorizationIT.java](/Users/rregan_platformq/gh-acc/rreganjr/Requel/modules/requel-app/src/test/java/com/rreganjr/requel/service/AuthorizationIT.java:84)

Impact:

- Some of the most important release tests exist, but are effectively outside the normal automated suite.
- The current `344` passing tests overstate release confidence because they exclude these `*IT` classes entirely.

Recommended fix:

- Either add `maven-failsafe-plugin` for `*IT` classes, or configure Surefire includes explicitly if you want them in `test`.

### 2. `AuthControllerTest` does not verify the actual anonymous login contract

- The class is annotated with `@WithMockUser` at [AuthControllerTest.java](/Users/rregan_platformq/gh-acc/rreganjr/Requel/modules/requel-app/src/test/java/com/rreganjr/requel/service/auth/AuthControllerTest.java:72).
- That means the login tests at lines [87](/Users/rregan_platformq/gh-acc/rreganjr/Requel/modules/requel-app/src/test/java/com/rreganjr/requel/service/auth/AuthControllerTest.java:87), [109](/Users/rregan_platformq/gh-acc/rreganjr/Requel/modules/requel-app/src/test/java/com/rreganjr/requel/service/auth/AuthControllerTest.java:109), and [123](/Users/rregan_platformq/gh-acc/rreganjr/Requel/modules/requel-app/src/test/java/com/rreganjr/requel/service/auth/AuthControllerTest.java:123) are all executed as authenticated requests.
- The suite also has no unauthenticated `/api/auth/me` test; only the happy path exists at [140](/Users/rregan_platformq/gh-acc/rreganjr/Requel/modules/requel-app/src/test/java/com/rreganjr/requel/service/auth/AuthControllerTest.java:140).

Impact:

- The tests prove DTO/error-shape behavior, but not the intended security contract:
  - `POST /api/auth/login` should work anonymously
  - `GET /api/auth/me` should return `401` when unauthenticated

Tests to add:

- Anonymous `POST /api/auth/login` success
- Anonymous `POST /api/auth/login` bad-credentials case
- Unauthenticated `GET /api/auth/me` returns `401`

### 3. `ProjectQueryControllerTest` covers only a subset of the controller surface

Current coverage stops at:

- projects list/detail
- goals list/detail
- actors list/detail
- stories list
- use-cases list
- scenarios list
- stakeholders list

See [ProjectQueryControllerTest.java](/Users/rregan_platformq/gh-acc/rreganjr/Requel/modules/requel-app/src/test/java/com/rreganjr/requel/service/query/ProjectQueryControllerTest.java:130).

But `ProjectQueryController` exposes additional endpoints:

- `/api/projects/stakeholder-permissions` at [ProjectQueryController.java:150](/Users/rregan_platformq/gh-acc/rreganjr/Requel/modules/service-impl/src/main/java/com/rreganjr/requel/service/query/ProjectQueryController.java:150)
- `/api/projects/{name}/my-permissions` at [179](/Users/rregan_platformq/gh-acc/rreganjr/Requel/modules/service-impl/src/main/java/com/rreganjr/requel/service/query/ProjectQueryController.java:179)
- `/api/projects/{name}/tree` at [213](/Users/rregan_platformq/gh-acc/rreganjr/Requel/modules/service-impl/src/main/java/com/rreganjr/requel/service/query/ProjectQueryController.java:213)
- `/api/projects/{name}/export` at [239](/Users/rregan_platformq/gh-acc/rreganjr/Requel/modules/service-impl/src/main/java/com/rreganjr/requel/service/query/ProjectQueryController.java:239)
- stakeholder detail at [285](/Users/rregan_platformq/gh-acc/rreganjr/Requel/modules/service-impl/src/main/java/com/rreganjr/requel/service/query/ProjectQueryController.java:285)
- story detail at [372](/Users/rregan_platformq/gh-acc/rreganjr/Requel/modules/service-impl/src/main/java/com/rreganjr/requel/service/query/ProjectQueryController.java:372)
- scenario detail at [456](/Users/rregan_platformq/gh-acc/rreganjr/Requel/modules/service-impl/src/main/java/com/rreganjr/requel/service/query/ProjectQueryController.java:456)
- use-case detail at [497](/Users/rregan_platformq/gh-acc/rreganjr/Requel/modules/service-impl/src/main/java/com/rreganjr/requel/service/query/ProjectQueryController.java:497)
- terms list/detail at [797](/Users/rregan_platformq/gh-acc/rreganjr/Requel/modules/service-impl/src/main/java/com/rreganjr/requel/service/query/ProjectQueryController.java:797) and [817](/Users/rregan_platformq/gh-acc/rreganjr/Requel/modules/service-impl/src/main/java/com/rreganjr/requel/service/query/ProjectQueryController.java:817)
- reports list/detail/run at [865](/Users/rregan_platformq/gh-acc/rreganjr/Requel/modules/service-impl/src/main/java/com/rreganjr/requel/service/query/ProjectQueryController.java:865), [885](/Users/rregan_platformq/gh-acc/rreganjr/Requel/modules/service-impl/src/main/java/com/rreganjr/requel/service/query/ProjectQueryController.java:885), and [906](/Users/rregan_platformq/gh-acc/rreganjr/Requel/modules/service-impl/src/main/java/com/rreganjr/requel/service/query/ProjectQueryController.java:906)
- open issues at [957](/Users/rregan_platformq/gh-acc/rreganjr/Requel/modules/service-impl/src/main/java/com/rreganjr/requel/service/query/ProjectQueryController.java:957)

Impact:

- The current suite does not cover several controller methods that have their own mapping and DTO logic.

### 4. Release-critical coverage is also diluted by disabled suites

Skipped tests are concentrated in legacy JAXB/NLP paths:

- [ImportProjectCommandTest.java](/Users/rregan_platformq/gh-acc/rreganjr/Requel/modules/requel-app/src/test/java/com/rreganjr/requel/project/impl/command/ImportProjectCommandTest.java:36)
- [ProjectXmlRoundTripIT.java](/Users/rregan_platformq/gh-acc/rreganjr/Requel/modules/requel-app/src/test/java/com/rreganjr/requel/ProjectXmlRoundTripIT.java:97)
- [ProjectJAXBTest.java](/Users/rregan_platformq/gh-acc/rreganjr/Requel/modules/requel-app/src/test/java/com/rreganjr/requel/project/ProjectJAXBTest.java:47)
- [ProjectAssistantTest.java](/Users/rregan_platformq/gh-acc/rreganjr/Requel/modules/requel-app/src/test/java/com/rreganjr/requel/project/impl/assistant/ProjectAssistantTest.java:34)
- [DictionaryRepositoryTest.java](/Users/rregan_platformq/gh-acc/rreganjr/Requel/modules/requel-app/src/test/java/com/rreganjr/nlp/dictionary/DictionaryRepositoryTest.java:40)
- [LemmatizerTests.java](/Users/rregan_platformq/gh-acc/rreganjr/Requel/modules/requel-app/src/test/java/com/rreganjr/nlp/dictionary/LemmatizerTests.java:34)
- [NERTests.java](/Users/rregan_platformq/gh-acc/rreganjr/Requel/modules/requel-app/src/test/java/com/rreganjr/nlp/dictionary/NERTests.java:38)
- [SemanticRoleLabelerTests.java](/Users/rregan_platformq/gh-acc/rreganjr/Requel/modules/requel-app/src/test/java/com/rreganjr/nlp/SemanticRoleLabelerTests.java:36)
- [VerbNetImporterTests.java](/Users/rregan_platformq/gh-acc/rreganjr/Requel/modules/requel-app/src/test/java/com/rreganjr/nlp/VerbNetImporterTests.java:55)
- [NLPConstituentParseTests.java](/Users/rregan_platformq/gh-acc/rreganjr/Requel/modules/requel-app/src/test/java/com/rreganjr/nlp/NLPConstituentParseTests.java:51)

Impact:

- The release suite is healthy for core CRUD/API work, but not for NLP or the removed JAXB path.
- These disabled tests should not be counted as current release coverage.

## Other correctness and completeness issues

### 5. `EditProjectCommandImplTest` only tests creation

- The file is named [EditProjectCommandImplTest.java](/Users/rregan_platformq/gh-acc/rreganjr/Requel/modules/requel-app/src/test/java/com/rreganjr/requel/project/impl/command/EditProjectCommandImplTest.java:36).
- It contains exactly one test, `testProjectCreation`, at line [38](/Users/rregan_platformq/gh-acc/rreganjr/Requel/modules/requel-app/src/test/java/com/rreganjr/requel/project/impl/command/EditProjectCommandImplTest.java:38).

Missing:

- edit existing project
- duplicate-name rejection
- any delete-related behavior if that path exists elsewhere

### 6. `ReportGeneratorCommandTest` only covers create and delete

- Create is at [80](/Users/rregan_platformq/gh-acc/rreganjr/Requel/modules/requel-app/src/test/java/com/rreganjr/requel/project/impl/command/ReportGeneratorCommandTest.java:80).
- Delete is at [101](/Users/rregan_platformq/gh-acc/rreganjr/Requel/modules/requel-app/src/test/java/com/rreganjr/requel/project/impl/command/ReportGeneratorCommandTest.java:101).

Missing:

- edit/update existing report generator
- duplicate-name rejection
- invalid XSLT / validation failure path if the command validates content

### 7. `CopyCommandTest` does not fully cover the copy-command matrix and explicitly works around one known bug

- The scenario-step test documents a known bug instead of asserting the intended behavior at [296](/Users/rregan_platformq/gh-acc/rreganjr/Requel/modules/requel-app/src/test/java/com/rreganjr/requel/project/impl/command/CopyCommandTest.java:296).
- Only `CopyGoal` has explicit-name coverage at [159](/Users/rregan_platformq/gh-acc/rreganjr/Requel/modules/requel-app/src/test/java/com/rreganjr/requel/project/impl/command/CopyCommandTest.java:159).

Missing:

- explicit new-name coverage for actor/story/use-case/scenario copies
- duplicate-name auto-generation coverage for scenario-step after the bug is fixed
- copy behavior around annotations / glossary-term references, which the plan calls out

### 8. `EditUserCommandTest` proves the current implementation differs from the plan

The test class explicitly documents that:

- there is no separate `DeleteUser` command
- there is no separate `ChangePassword` command

See [EditUserCommandTest.java](/Users/rregan_platformq/gh-acc/rreganjr/Requel/modules/requel-app/src/test/java/com/rreganjr/requel/user/impl/command/EditUserCommandTest.java:36).

This is not a failing test, but it means the test plan is stale in this area. Do not schedule `DeleteUser` test work unless the command is actually being added.

### 9. `CommandControllerTest` does not cover the multipart upload dispatch path

- `CommandController` has a multipart handler at [CommandController.java:92](/Users/rregan_platformq/gh-acc/rreganjr/Requel/modules/service-impl/src/main/java/com/rreganjr/requel/service/command/CommandController.java:92).
- [CommandControllerTest.java](/Users/rregan_platformq/gh-acc/rreganjr/Requel/modules/requel-app/src/test/java/com/rreganjr/requel/service/command/CommandControllerTest.java:102) tests only JSON POSTs.

Missing:

- multipart happy path
- multipart bad command type / validation / command-exception mapping
- import command file wiring

### 10. `ProjectRepositoryTest` only covers a subset of repository methods, and one assertion is too broad

Current coverage is limited to:

- `findProjectByName`
- `findGoalByProjectOrDomainAndName`
- `findActorByProjectOrDomainAndName`
- stakeholder-permission lookup/catalog
- duplicate project name

See [ProjectRepositoryTest.java](/Users/rregan_platformq/gh-acc/rreganjr/Requel/modules/requel-app/src/test/java/com/rreganjr/requel/project/ProjectRepositoryTest.java:94).

But `ProjectRepository` also exposes untested methods such as:

- `findUseCaseByProjectOrDomainAndName` at [ProjectRepository.java:71](/Users/rregan_platformq/gh-acc/rreganjr/Requel/modules/project-jpa/src/main/java/com/rreganjr/requel/project/ProjectRepository.java:71)
- `findStoryByProjectOrDomainAndName` at [83](/Users/rregan_platformq/gh-acc/rreganjr/Requel/modules/project-jpa/src/main/java/com/rreganjr/requel/project/ProjectRepository.java:83)
- `findScenarioByProjectOrDomainAndName` at [94](/Users/rregan_platformq/gh-acc/rreganjr/Requel/modules/project-jpa/src/main/java/com/rreganjr/requel/project/ProjectRepository.java:94)
- stakeholder lookups at [113](/Users/rregan_platformq/gh-acc/rreganjr/Requel/modules/project-jpa/src/main/java/com/rreganjr/requel/project/ProjectRepository.java:113) and [126](/Users/rregan_platformq/gh-acc/rreganjr/Requel/modules/project-jpa/src/main/java/com/rreganjr/requel/project/ProjectRepository.java:126)
- glossary lookups at [159](/Users/rregan_platformq/gh-acc/rreganjr/Requel/modules/project-jpa/src/main/java/com/rreganjr/requel/project/ProjectRepository.java:159) and [167](/Users/rregan_platformq/gh-acc/rreganjr/Requel/modules/project-jpa/src/main/java/com/rreganjr/requel/project/ProjectRepository.java:167)
- report generator lookup at [207](/Users/rregan_platformq/gh-acc/rreganjr/Requel/modules/project-jpa/src/main/java/com/rreganjr/requel/project/ProjectRepository.java:207)

Also:

- `duplicateProjectNameThrowsEntityException` uses `assertThrows(Exception.class, ...)` at [209](/Users/rregan_platformq/gh-acc/rreganjr/Requel/modules/requel-app/src/test/java/com/rreganjr/requel/project/ProjectRepositoryTest.java:209), which is too broad to catch regression in exception mapping.

### 11. There is still no dedicated annotation repository test for `Issue -> Position -> Argument` persistence across entity types

- [AnnotationAnyMappingTest.java](/Users/rregan_platformq/gh-acc/rreganjr/Requel/modules/requel-app/src/test/java/com/rreganjr/requel/annotation/AnnotationAnyMappingTest.java:51) verifies only a single `Note -> Actor` `@Any/@ManyToAny` load path.

Missing:

- issue/position/argument chain persistence
- multiple annotatable entity types
- save/reload/delete behavior for shared annotations

### 12. `AuthorizationIT` is good coverage, but it still misses some release-plan scenarios even if it gets wired into the build

Current HTTP coverage focuses on command authorization for:

- goals
- stories
- actors
- stakeholders
- projects
- admin editing another user

See [AuthorizationIT.java](/Users/rregan_platformq/gh-acc/rreganjr/Requel/modules/requel-app/src/test/java/com/rreganjr/requel/service/AuthorizationIT.java:227).

Still missing:

- user editing their own account via HTTP
- report-generator and glossary-term command authorization
- annotation command authorization
- query-endpoint authorization/forbidden behavior

## Tests to write next

Recommended order:

1. Make `*IT` tests execute in CI/local by default.
2. Add `AuthController` anonymous-login and unauthenticated-`/me` tests.
3. Expand `ProjectQueryControllerTest` to cover:
   - stakeholder permissions
   - my permissions
   - tree
   - export
   - stakeholder detail
   - story/use-case/scenario detail
   - terms list/detail
   - reports list/detail/run
   - open issues
4. Add multipart `CommandController` tests for import/file-upload commands.
5. Extend `EditProjectCommandImplTest` with edit and duplicate-name cases.
6. Extend `ReportGeneratorCommandTest` with update and duplicate-name cases.
7. Expand `ProjectRepositoryTest` for story/use-case/scenario/stakeholder/glossary/report-generator finders and tighten exception assertions.
8. Add a true annotation repository integration test for `Issue -> Position -> Argument` on multiple annotatable types.
9. After the `CopyScenarioStep` auto-naming bug is fixed, replace the workaround in `CopyCommandTest` with a regression test for the intended behavior.
10. Add HTTP authorization tests for self-edit, glossary/report commands, and annotation commands.

## Bottom line

The current Java test suite is materially stronger than the plan’s original “current state” section suggests, but it still has three release-significant problems:

- some of the most valuable tests are not actually run by default
- controller coverage is still incomplete for several shipped endpoints
- a meaningful slice of legacy/NLP coverage remains disabled
