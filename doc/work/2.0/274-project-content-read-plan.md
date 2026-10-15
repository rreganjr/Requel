# #274 A project content read — implementation plan

Issue: https://github.com/rreganjr/Requel/issues/274 (child 7 of epic #267)
Branch: `274-project-content-read`, cut from `release/2.0` @ `e5d06ad2`
Builds on: #270 (stale flag), #271 (severity), #272 P7 (no provenance in general reads), #273

## Summary

A new gateway read, `getProjectContent`, returns a project's whole normative content in one call.
That means every goal, story, actor, use case, scenario, step, glossary term and stakeholder, with
its text, relations, tags and (optionally) annotations. Entities point to each other by id, so
nothing is repeated. It is a JSON read for gateway callers (MCP, CLI, REST), and it has a
character cap. Over the cap it fails with a 422 that names the overflow by section.
`getProjectContext` is left alone except for its description, which wrongly says it reads "a
whole project in one call".

## Review against the tree (`release/2.0` @ `e5d06ad2`)

1. **The gap is real.** `InProcessQueryGateway.getProjectContext` is `ProjectDto` + the tree
   (id/type/name) + the glossary + open issues. No goal, story, actor, use case or scenario text,
   and no steps. `getProjectTree` is names only.
2. **`QueryDescriptions.GET_PROJECT_CONTEXT` is wrong.** It says "Reads a whole project in one
   call". Fixed here.
3. **#275's premise has drifted.** #273 found `GenerateReportCommandImpl` already runs the
   generator XSLT over the XML export, including `<sources>`. In-process emit does not need this
   read. Its consumers are gateway callers: the AI narrative half of emit, and external
   generators.
4. **The XML export cannot be exposed as this read.** It carries `externalSources` and
   `ignoredFindings`, and #272 P7 names "#274's content read" as a general read that never
   carries provenance.
5. **The detail DTOs cannot be reused as they are.** `UseCaseDto` embeds full `GoalDto`,
   `ActorDto`, `StoryDto` and `ScenarioDto` lists, so one read would repeat the same text many
   times. The read needs flat DTOs that point to each other by id.
6. **Shared steps already have an export shape.** `AbstractProjectOrDomain.getAllScenariosAndSteps`
   walks scenarios recursively and emits every step once, with scenarios referring to steps by
   id. The content read copies that shape.
7. **`getProjectEntities` walks only one level of scenario steps.** The content read uses
   `getAllScenariosAndSteps`. The counts test surfaces any case where a sub-scenario is not in
   `project.getScenarios()` (see Risks).
8. **`ProjectDto` has no step count.** AC4 ("counts match the project summary") covers seven
   types. Steps need their own assertion.
9. **REST errors lose their message.** `ResponseStatusException` reasons are not rendered by
   default (`server.error.include-message` is unset), and `RestQueryGateway` has no error-body
   mapping. The overflow needs a typed exception with an `ApiExceptionHandler` mapping, or
   "names the overflow" fails over REST/CLI.
10. **Client-side truncation.** Claude Code cuts off MCP tool output at about 25k tokens by
    default. A read under Requel's cap can still be truncated by the client. The tool
    description points callers at `annotations=open|none` and per-entity reads.

## Locked decisions (2026-09-29)

1. **Consumer:** gateway callers. #275 keeps XSLT over the export. A comment on #275 drops
   "using child 7's content read as input" (hand-over below).
2. **New tool** `getProjectContent`. `getProjectContext` stays a context bundle.
3. **Annotations:** notes and issues (open and resolved), with positions, arguments, severity,
   `source` and `stale`. An `annotations` parameter takes `none`, `open` or `all`, default `all`.
   `open` means notes plus unresolved issues.
4. **Cap:** total characters of the text fields as the read is built, the same measure as
   `ContextPackBudget`. Property `requel.gateway.content.max-characters`, default `400000`
   (about 100k tokens). Over the cap → 422 `CONTENT_TOO_LARGE`, and the message gives the
   total, the cap, and entity count + characters for each section.
5. **No provenance** (keeps #272 P7). The EntityProvenanceIT guard tests add the new read.
   Callers use `listSources` / `getEntitySources`.
6. **Defaults (unchallenged):**
   - Stakeholders carry name, type, text (non-user only) and goalIds. No username, email, phone,
     team or permissions.
   - Tags are included on every entity as sorted `category:value` tokens.
   - Report generators, dictionary words, ignored findings and assistant settings are excluded.
   - Entity `createdBy` is dropped. Annotation `createdBy` stays, because human vs assistant
     discussion depends on it.
7. **Fixed order:** every list is sorted by id; tags are sorted; positions and arguments keep
   `toIssueDto`'s natural order. Two reads of an unchanged project serialize identically.

## Contracts

### DTOs (`service-api`, `com.rreganjr.requel.service.api.dto`)

```java
record ProjectContentDto(
    ProjectDto project,
    String annotations,                 // the mode served: NONE | OPEN | ALL
    int characters, int maxCharacters,  // what this read counted, and the cap
    List<StakeholderContentDto> stakeholders,
    List<GoalContentDto> goals,
    List<StoryContentDto> stories,
    List<ActorContentDto> actors,
    List<UseCaseContentDto> useCases,
    List<ScenarioContentDto> scenarios,
    List<StepContentDto> steps,          // plain steps only; scenarios used as steps are in scenarios
    List<GlossaryTermContentDto> glossary)

record GoalContentDto(Long id, int version, String name, String text,
    List<GoalRelationContentDto> relations,   // outgoing only; incoming is the same edge seen from the other end
    List<String> tags, AnnotationsDto annotations)
record GoalRelationContentDto(String relationType, Long goalId)
record StoryContentDto(Long id, int version, String name, String text, String storyType,
    List<Long> goalIds, List<Long> actorIds, List<String> tags, AnnotationsDto annotations)
record ActorContentDto(Long id, int version, String name, String text,
    List<Long> goalIds, List<String> tags, AnnotationsDto annotations)
record UseCaseContentDto(Long id, int version, String name, String text,
    Long primaryActorId, Long primaryScenarioId, List<Long> additionalScenarioIds,
    List<Long> goalIds, List<Long> actorIds, List<Long> storyIds,
    List<String> tags, AnnotationsDto annotations)
record ScenarioContentDto(Long id, int version, String name, String text, String scenarioType,
    List<StepRefDto> steps,             // ordered; a shared step has the same id wherever it appears
    List<String> tags, AnnotationsDto annotations)
record StepRefDto(String type, Long id) // type: Step | Scenario
record StepContentDto(Long id, int version, String name, String text, String scenarioType,
    List<String> tags, AnnotationsDto annotations)
record GlossaryTermContentDto(Long id, int version, String name, String text,
    Long canonicalTermId, List<String> tags, AnnotationsDto annotations)
record StakeholderContentDto(Long id, int version, String name, String type, String text,
    List<Long> goalIds, List<String> tags, AnnotationsDto annotations)
```

`annotations` is an empty `AnnotationsDto` (both lists empty) under `NONE`. Notes and issues
come from `AnnotationCommandRegistrar.toNoteDto` / `toIssueDto` with the #270 stale flag. They
are sorted by id, not by the severity-first order `getAnnotations` uses, so the order depends
only on content.

### Service (`service-impl`, `com.rreganjr.requel.service.query.ProjectContentQueryService`)

- `@Service @Transactional(readOnly = true)`, following `ProvenanceQueryService`.
- `ProjectContentDto read(String projectName, String annotations)`:
  - `findProjectByName` → `NoSuchProjectException`;
  - `ProjectReadAccess.canRead` → `AuthorizationException`;
  - an `annotations` value that isn't `none`, `open` or `all` (case-insensitive) →
    `IllegalArgumentException`; null means `all`.
- Walks `getStakeholders`, `getGoals`, `getStories`, `getActors`, `getUseCases`,
  `getAllScenariosAndSteps` (split into scenarios and plain steps) and `getGlossaryTerms`.
- One `AnnotationFreshness.staleAnnotations(...)` lookup for all entities, as `getOpenIssues`
  does.
- Tags come from `TagExportProvider.exportAssignmentsFor(project)`, grouped by entity (optional
  bean: no tagging module → empty lists).
- Counts characters per section (entity names and texts; annotation, position and argument
  texts) into a `ContentBudget` holding the per-section tallies. It throws
  `ProjectContentTooLargeException` once the build finishes, so the message has every section's
  numbers.
- Cap: `@Value("${requel.gateway.content.max-characters:400000}")`. #293 removed the legacy
  placeholder, so `@Value` sees test properties now. `0` or less disables the cap.
- The `ProjectDto` comes from `ProjectQueryController`'s existing mapper (made reachable as a
  static, like `toReportGeneratorSummaryDto`).

### Overflow

- `com.rreganjr.requel.gateway.ProjectContentTooLargeException extends RuntimeException`
  (`gateway-api`, so both server and REST client see it). Fields: `characters`,
  `maxCharacters`, and `sections` (a map of section → count and characters).
- Message, for example: `Project 'PlatformQ Roundtable' content is 512,340 characters; the cap
  is 400,000 (requel.gateway.content.max-characters). goals 41 / 38,210; stories 6 / 9,400;
  actors 9 / 3,050; useCases 12 / 21,700; scenarios 18 / 14,020; steps 140 / 16,900; glossary
  30 / 8,400; stakeholders 5 / 900; annotations 214 / 399,760. Retry with annotations=open or
  annotations=none, or read entities one at a time with getEntity.`
- `ApiExceptionHandler`: → `422` with `ErrorResponse.of("CONTENT_TOO_LARGE", message)`.
- `RestQueryGateway.getProjectContent`: a 422 whose body code is `CONTENT_TOO_LARGE` is rethrown
  as `ProjectContentTooLargeException(message)`.
- MCP: the exception propagates through `McpReadService.callTool` like any read failure, and
  `RequelMcpToolCallback` passes its message on.

### Gateway / REST / MCP / CLI

- `QueryGateway`: `default ProjectContentDto getProjectContent(String projectName, String
  annotations)` throws `UnsupportedOperationException`, like the provenance reads, so the stub
  and in-memory gateways compile unchanged.
- `InProcessQueryGateway`: delegates to the service through the existing status-mapping helper
  (404 / 403 / 400). The helper is renamed from `provenance(...)` to `serviceRead(...)` now that
  it maps more than provenance. The overflow exception is not mapped; it passes through.
- `GatewayQueryController`: `GET /api/gateway/query/projects/{name}/content?annotations=all`.
- `RestQueryGateway`: the same URL with the `annotations` query parameter.
- MCP `getProjectContent`: schema `projectName` (required) plus `annotations` as an
  `enumProperty(["none","open","all"], ...)`. `callTool` passes `optionalText(arguments,
  "annotations")`.
- `QueryDescriptions`:
  - `GET_PROJECT_CONTENT`: what it returns; that entities point to each other by id; that shared
    steps share an id; the cap and the 422; that sources are not included (use `listSources` /
    `getEntitySources`); and the size hint about `annotations=open|none`.
  - `CONTENT_ANNOTATIONS` for the parameter.
  - `GET_PROJECT_CONTEXT` reworded: "Reads a project's summary, content tree (names only),
    glossary and open issues in one call. For entity text, scenarios and steps use
    getProjectContent."
- CLI: `requel content PROJECT [--annotations=none|open|all]` (`ContentCommand extends
  AbstractQueryCommand`, JSON output), registered in `RequelCli`.
- Docs: `doc/guides/local_mcp_bridge.md` tool list and the provenance paragraph (line ~273) name
  `getProjectContent`.

## Step by step

1. DTO records in `service-api`.
2. `ProjectContentTooLargeException` in `gateway-api`, and the `QueryGateway` default method.
3. `ProjectContentQueryService` with `ContentBudget`, plus the `ProjectDto` mapper made static.
4. `InProcessQueryGateway` wiring, the helper rename, and the `ApiExceptionHandler` 422 mapping.
5. `GatewayQueryController` endpoint and `RestQueryGateway` call with its 422 mapping.
6. `QueryDescriptions` (new constants, `GET_PROJECT_CONTEXT` reworded), MCP tool and schema.
7. CLI `ContentCommand`.
8. Guide update.
9. Tests (below), `tmp/274-verify.sh`, `mvn clean verify`.

## Test plan

- **`ProjectContentIT` (`requel-app`)**. Fixture: two goals with a relation; a story linked to a
  goal and an actor; a use case with a primary actor, a primary scenario and an additional
  scenario; one plain step shared by both scenarios; a sub-scenario used as a step; a glossary
  term with a canonical term; a non-user stakeholder with a goal; one tag; an open issue; a
  resolved issue with a position and an argument; a note.
  - Counts: stakeholders, goals, stories, actors, useCases, scenarios and glossary each equal the
    matching `ProjectDto` count. Steps equal the distinct plain steps from
    `getAllScenariosAndSteps` (AC4).
  - Reconstruction: every fixture entity's name and text is present. Every id referenced
    anywhere (`goalIds`, `primaryActorId`, `StepRefDto`, `canonicalTermId`, relations) resolves
    to an entity in the read (AC1).
  - Shared steps: the shared step's id is in both scenarios' `steps` and exactly once in
    `steps`. The sub-scenario shows up as `StepRefDto("Scenario", id)` and in `scenarios` (AC2).
  - Annotation modes: `all` has the resolved issue with its position and argument; `open` drops
    it and keeps the note; `none` has empty lists everywhere; an unknown mode → 400.
  - Fixed order: two reads serialize to identical JSON.
  - Access: a non-member → 403, an unknown project → 404.
- **`ProjectContentCapIT`** (own context, `requel.gateway.content.max-characters` set small via
  `@DynamicPropertySource`): over the cap, `ProjectContentTooLargeException` is thrown, and its
  message names the total, the cap and every section. Over REST it is a 422 with code
  `CONTENT_TOO_LARGE` and the same message. `annotations=none` on the same project succeeds when
  the entities alone fit (AC3).
- **`EntityProvenanceIT`**: both guard tests (`noGeneralReadOrContextPackCarriesTheSourceOrItsLocator`,
  `noGeneralReadOrContextPackCarriesAReferenceItsNoteOrPrecedence`) add `getProjectContent` to
  the reads checked.
- **`McpReadServiceTest`**: the tool is listed, every schema property has a description (the
  existing #296 check), `annotations` is an enum, and `callTool` passes `null` when it is absent.
- **`RestQueryGatewayTest`**: the URL and query parameter; a 422 `CONTENT_TOO_LARGE` body →
  `ProjectContentTooLargeException` with the server's message.
- **`QueryCommandsTest`**: `content PROJECT --annotations=open` calls the gateway with `open`.
- **`RequelMcpEndToEndIT`**: one `getProjectContent` call over the real transport returns
  goals with text.

## Out of scope

- Streaming or pagination (issue text).
- Changing `getProjectTree` (issue text), or `getProjectContext`'s payload.
- Provenance, references or authority in the read (#272 P7).
- #275's generator and its input. #275 stays on XSLT over the export.
- A content hash or "project version". That belongs to #275 if its AC needs one.
- The Angular UI. Nothing in the app uses this read.

## Risks

- **Sub-scenarios outside `project.getScenarios()`.** If one exists, the scenario count test
  fails. That is a real finding: `getProjectEntities` (and so `getOpenIssues`) would be missing
  its steps' issues too. Fold the fix in if it is a one-liner; otherwise file it with no
  milestone.
- **Default cap.** 400,000 characters is a guess at "fits a large context". The roundtable
  project should fit with `annotations=all`. The IT prints its size so the default can be
  checked against it.
- **Lazy loading cost.** One read touches every entity's annotations and tags. It is the same
  walk `getOpenIssues` and the export already do, inside one read-only transaction. No new fetch
  graphs (#323).
- **Client truncation** (review point 10). This is covered only by the description; Requel
  cannot see the client's limit.

## AC mapping

| Acceptance criterion | Where |
|---|---|
| One call returns enough to reconstruct the project's normative content | `ProjectContentDto`; reconstruction and id-resolution assertions |
| Scenarios come back with their steps; shared steps identifiable | `ScenarioContentDto.steps` + flat `steps`; shared-step assertion |
| Over the cap → clear message naming the overflow, no truncation | `ProjectContentTooLargeException`, 422 `CONTENT_TOO_LARGE`; `ProjectContentCapIT` |
| Test: entity counts match the project summary | counts assertion (seven `ProjectDto` types + steps) |
| Added: fixed order | two-read equality |
| Added: no provenance | `EntityProvenanceIT` guard tests |
| Added: same access as other reads | 403 / 404 assertions |

## Revised issue body (for `gh issue edit`)

In `tmp/issue-274-body.md`. Changes from the filed text: "normative content" is defined; the
decision on the tool is recorded; the annotation modes and cap unit are stated; three ACs are
added (fixed order, no provenance, access); and the steps assertion is added.

## Implementation notes (2026-09-29)

Where the build departed from the text above, and why:

1. **The cap test shares `ProjectContentIT`'s context.** The plan had a separate
   `ProjectContentCapIT` with its own context and a small cap set by property. Instead the test
   builds a second `ProjectContentQueryService` with a small cap, from the same beans. That covers
   the same behaviour without paying for another Spring context on CI. The REST 422 is covered by
   `GatewayQueryControllerContentTest` (standalone MockMvc + `ApiExceptionHandler`).
2. **Risk 1 did not show up.** With a sub-scenario used as a step, `ProjectDto.scenarioCount`
   equals the scenarios the read reaches. A sub-scenario is a project scenario.
3. **The fixture is 815 characters (685 without annotations)**, logged by the cap test. The
   400,000 default still needs checking against roundtable (project 621) with a real read.
4. **The CLI prints the overflow message.** `AbstractQueryCommand` catches
   `ProjectContentTooLargeException` and prints its message, exit code `REQUEST_ERROR`. Otherwise
   a 422 would have printed only the status line.
5. **Tags come from `TagRepository.findTagsOnEntity`**, one query per entity, as the XML export's
   `TagExportProviderImpl` does. Tokens are `category:value` without the colour.
6. **`local_mcp_bridge.md` step 3** ("load existing goals") now points at `getProjectContent`.
   `getProjectContext` never carried goal text.
