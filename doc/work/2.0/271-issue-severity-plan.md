# #271 Issue severity is accepted by `draftAnnotation`, then silently dropped on persist — implementation plan

Issue: https://github.com/rreganjr/Requel/issues/271 (child 4 of epic #267)
Branch: `271-issue-severity`, cut from `release/2.0` @ `8fa39c91`

## Summary

`Issue` has no severity, so every downstream reader has one bit of priority, `mustBeResolved`,
which is `true` for a spelling false positive and for a possible authorization hole alike. This
ticket adds a closed `LOW | MEDIUM | HIGH` severity to issues. It is persisted, exported,
imported, settable through `EditIssue`, written by the assistant applicator from the finding's
severity, shown and picked in the UI, and used to order every issue list. Lexical issues default
to `LOW` and every other issue to `MEDIUM`, both for new issues and in the V22 backfill, so the
roundtable project's real findings move above the lexical noise as soon as this merges. The
ticket also fixes an existing bug in the same mapper: an `EditIssue` update that leaves out
`mustBeResolved` resets it to `false`.

#296 (`@CommandDescription` across the gateway catalog) waits on this, so `EditIssue`'s
description is written once, after severity exists.

## Review against the tree (`release/2.0` @ `8fa39c91`)

**`draftAnnotation` never persists.** `McpReadService.draftAnnotation` builds an
`AnnotationAction` and returns it to the caller; its javadoc says "never persisted (the applicator
applies it later)". Only `AssistantRunWorker` feeds actions to the applicator. So the AC "a
severity supplied to `draftAnnotation` survives to the persisted issue" has no server path to
test. Severity can reach an `Issue` in two ways, and this ticket covers both:

- **Assistant runs.** `CommandBackedAssistantResultApplicator.applyIssue` (≈ line 520) has
  `action.severity()` and writes it only to `assistant_findings.severity` (line 773), never to
  the issue. This is the real drop.
- **MCP and CLI callers.** A caller persists a draft by calling `EditIssue`. `EditIssueInput`
  has `projectName, entityType, entityId, issueId, text, mustBeResolved` and no severity.

`draftAnnotation` currently accepts any string for `severity`, and its schema says only
`{"type":"string"}`.

**The vocabulary already exists.** `assistant-ai/.../ai/schemas/requirements-review-output.v1.json`
declares severity as `["LOW", "MEDIUM", "HIGH", null]`, the review prompt's example uses
`"MEDIUM"`, and `assistant_findings.severity` is `VARCHAR(20)` (V8). The legacy NLP assistants
send no severity at all.

**Single-table annotations.** `AbstractAnnotation` is `SINGLE_TABLE` on `annotations` with
discriminator `annotation_type`. Notes share the table with issues; positions and arguments have
their own tables (`positions`, `arguments`). Two issue
discriminators exist: `com.rreganjr.requel.annotation.Issue` (`IssueImpl`) and
`com.rreganjr.requel.annotation.impl.LexicalIssue`, the only subclass. The new column has to be
nullable in the database. "Never null" applies to issue rows only, and is enforced by the
backfill, the constructors and a getter fallback.

**Write paths that create or edit an issue:**

| Path | Where | Today |
|---|---|---|
| Gateway / UI `EditIssue` | `AnnotationCommandRegistrar` ≈ line 122 | `setMustBeResolved(Boolean.TRUE.equals(i.mustBeResolved()))`, so an update that leaves it out clears it |
| Assistant applicator | `CommandBackedAssistantResultApplicator.applyIssue` | `EditIssueCommand` or `EditLexicalIssueCommand`; severity not passed |
| Legacy `AbstractAssistant.addSimpleIssue` | `project-jpa/.../assistant/AbstractAssistant.java:92` | copies text and `mustBeResolved` when re-attaching an existing issue |
| `EditIssueCommandImpl.execute` | `annotation-jpa` | with no issue set, it first looks for an issue with the same text (`findIssue`) and reuses it **without touching its properties** |
| `EditLexicalIssueCommandImpl.execute` | `annotation-jpa` | the same lookup by word (and property); on update it sets only text and word |
| XML import | `AnnotationImportXml` → `AnnotationImportXmlMapper` → `AnnotationImportDraft` → `AnnotationAssembler` | constructs `IssueImpl` / `LexicalIssue` directly |

**Read paths:**

| Path | Where | Order today |
|---|---|---|
| `GET /api/projects/{name}/open-issues`, `getOpenIssues` (gateway, MCP, CLI) and `getProjectContext.openIssues` | `ProjectQueryController.getOpenIssues` ≈ line 1103; `InProcessQueryGateway` delegates | entityType, entityName, issueText |
| Per-entity annotations | `AnnotationQueryController` line 90 | issue id |
| `IssueDto` | `AnnotationCommandRegistrar.toIssueDto` line 260 | — |
| CLI `open-issues` | `requel-cli/.../OpenIssuesCommand.renderText` | server order |
| Angular Open Issues | `features/open-issues/open-issues.ts` | table sorts client-side, `sortField="entityType"` |
| Angular annotations list | `shared/annotations-section.ts` | server order |
| XML export | `IssueImpl` JAXB; `doc/samples/project.xsd` `issue` type | — |

**Schema tooling.** `CommandInputSchema` (gateway-api) maps an enum-typed field to a bare
`string` and a `String` field to `string`. An enum-typed input field would fail in Jackson
before bean validation, so the caller would get a binding error rather than a field-level one.
`CommandInputValidator` raises `BeanValidationException` with per-field entries for constraint
violations on the input DTO.

**Migrations.** The latest is `V21__ignored_findings.sql`, so this ticket adds V22. No local or
remote branch has a V22+, `master` stops at V2, and no PRs are open.

## Locked decisions

From the review on 2026-09-25:

1. **The AC becomes two paths.** The applicator writes the action's severity onto the issue,
   and `EditIssue` accepts severity so a caller that persists a draft keeps it. No
   persist-a-draft command.
2. **The vocabulary is `LOW | MEDIUM | HIGH`**, the same as the AI output schema. The default
   is `MEDIUM`, except `LexicalIssue`, which defaults to `LOW` both for new issues and in the
   backfill. This is a fixed default by issue kind, not assistant analysis, so it stays within
   the ticket's "no automatic severity assignment by the assistants".
3. **The applicator is lenient.** An assistant severity outside the vocabulary is stored as the
   default and logged at WARN, so one bad model reply doesn't drop a real finding. Caller-facing
   paths (`EditIssue`, `draftAnnotation`) reject it.
4. **UI: show and pick.** A severity column on Open Issues, a severity tag on each issue in the
   annotations section, both lists ordered by severity, and a severity picker on the add-issue
   form.
5. **Fold in the `mustBeResolved` partial-update fix.** On an `EditIssue` update, a missing
   `mustBeResolved` means "leave it unchanged", following #316's contract. On create, a missing
   value still means `false`, as today.

Defaults this plan takes (object in review if any is wrong):

6. **`IssueSeverity` enum in `annotation-domain`**, stored as `VARCHAR(20)` with
   `@Enumerated(STRING)`, not a MySQL `ENUM` column, so adding a value later is not a column
   change. It has a `rank()` (`HIGH` 3, `MEDIUM` 2, `LOW` 1) for ordering and a case-insensitive
   `IssueSeverity.parse(String) : Optional<IssueSeverity>`.
7. **Strings at the module boundaries.** `AnnotationAction.severity`, `EditIssueInput.severity`,
   `IssueDto.severity`, `OpenIssueDto.severity` and the Angular models stay `String`.
   `service-api` and `assistant-api` gain no dependency on `annotation-domain`. Values are
   parsed where the command is built, and DTOs always carry the upper-case name.
8. **Case-insensitive input, upper-case storage.** `"high"` is accepted and stored as `HIGH`,
   matching #320's case-insensitive ignores.
9. **`EditIssue` severity: null means "leave unchanged" on update and "use the default" on
   create.** The same rule applies inside `EditIssueCommand`, so the applicator and
   `AbstractAssistant` get it too.
10. **Order everywhere: severity rank descending, then the list's current order.** Open issues
    keep entityType → entityName → issueText as the tie-break; per-entity annotations keep issue
    id. `mustBeResolved` does not enter the order: it is a gate, not a priority.
11. **`draftAnnotation`'s schema advertises the vocabulary** (`"enum": ["LOW","MEDIUM","HIGH"]`).
    `CommandInputSchema` is left alone: teaching it enums is a gateway-wide change and #296's
    description text covers the `EditIssue` caller.

12. **A text match applies a supplied severity** (approved in plan review, 2026-09-25). When
    `EditIssueCommandImpl` has no issue set and reuses one with the same text, a supplied
    non-null severity is written to the matched issue, because the caller asked for it.
    `mustBeResolved` on that path stays untouched, as today. `EditLexicalIssueCommandImpl`'s
    match by word (and property) follows the same rule.

## Contracts

### Domain (`annotation-domain`)

```java
public enum IssueSeverity {
    LOW(1), MEDIUM(2), HIGH(3);
    public int rank();
    /** Case-insensitive; blank or unknown is empty. */
    public static Optional<IssueSeverity> parse(String value);
}

public interface Issue extends Annotation {
    ...
    /** Never null for a persisted issue. */
    IssueSeverity getSeverity();
}

public interface EditIssueCommand extends EditAnnotationCommand {
    ...
    /** Null: default on create, unchanged on update. */
    void setSeverity(IssueSeverity severity);
    /** Null: false on create, unchanged on update. */
    void setMustBeResolved(Boolean mustBeResolved);
}
```

`setMustBeResolved(boolean)` stays as an overload so the existing call sites compile unchanged.

### Persistence (`annotation-jpa`)

- `IssueImpl`: `private IssueSeverity severity;` mapped `@Enumerated(EnumType.STRING)
  @Column(name = "severity", length = 20)`. Constructors set `defaultSeverity()`, which is
  `MEDIUM` on `IssueImpl` and overridden to `LOW` on `LexicalIssue`. `getSeverity()` returns
  `severity != null ? severity : defaultSeverity()`, so a row written by a path this ticket
  missed still reads non-null.
- JAXB: `@XmlAttribute(name = "severity")` on `IssueImpl`. `LexicalIssue` inherits it.
- `EditIssueCommandImpl`: a `severity` field and a nullable `mustBeResolved`; create, update
  and text-match follow decisions 5, 9 and 12. `EditLexicalIssueCommandImpl`
  does the same for severity. Its update path still ignores `mustBeResolved`, as today: every
  caller passes an explicit value, so starting to apply it would change behavior.

### Schema (`V22__issue_severity.sql`)

```sql
-- Issue #271: severity on issues. annotations is single-table and notes share it with
-- issues, so the column is nullable and only issue rows are backfilled. Lexical issues are the
-- legacy spell-check / vague-word / glossary-phrase output and default LOW; every other issue
-- defaults MEDIUM.
ALTER TABLE `annotations` ADD COLUMN `severity` varchar(20) DEFAULT NULL;

UPDATE `annotations` SET `severity` = 'LOW'
 WHERE `annotation_type` = 'com.rreganjr.requel.annotation.impl.LexicalIssue';

UPDATE `annotations` SET `severity` = 'MEDIUM'
 WHERE `annotation_type` = 'com.rreganjr.requel.annotation.Issue';
```

H2 tests build the column from the mapping (`create-drop`).

### XML (`doc/samples/project.xsd`, import)

- `issue` type: `<xs:attribute name="severity" type="tns:issueSeverity"/>`, optional, with a
  new `issueSeverity` simple type restricting `xs:string` to the three values. Older files have
  no attribute and stay valid.
- `AnnotationImportXml` gets a `severity` attribute; `AnnotationImportDraft` gets a nullable
  `severity` (a `String`, since `platform-core` is below `annotation-domain`);
  `AnnotationAssembler` parses it. Missing means the kind's default. An unknown value logs a WARN
  and uses the default rather than failing the import: an import restores what was exported and
  shouldn't fail on one attribute.

### API (`service-api`, `service-impl`)

```java
public record EditIssueInput(
        String projectName,
        @NotBlank String entityType,
        @NotNull Long entityId,
        Long issueId,
        @NotBlank String text,
        Boolean mustBeResolved,
        @Pattern(regexp = "LOW|MEDIUM|HIGH", flags = Pattern.Flag.CASE_INSENSITIVE,
                 message = "must be one of LOW, MEDIUM, HIGH")
        String severity) { }

public record IssueDto(..., boolean mustBeResolved, String severity, boolean resolved, ...) { }
public record OpenIssueDto(Long issueId, String issueText, boolean mustBeResolved, String severity,
        String entityType, Long entityId, String entityName) { }
```

- The registrar passes `IssueSeverity.parse(i.severity()).orElse(null)` and
  `i.mustBeResolved()` (nullable) to the command. The `@Pattern` has already rejected an
  unknown value with a `severity` field error.
- `ProjectQueryController.getOpenIssues`: the comparator is severity rank descending, then the
  current chain.
- `AnnotationQueryController`: issues are ordered by severity rank descending, then id.
- `WriteInputDtoConstructionTest` / `McpTestCatalog` gain the new component.

### MCP (`mcp-server`)

- `draftAnnotation`: a present `severity` is parsed. Unknown throws `McpInvalidParamsException`
  ("Unsupported severity: urgent (expected LOW, MEDIUM or HIGH)"); a known value goes into the
  draft upper-cased. The schema gets the enum list.

### Assistants (`assistant-core`, `project-jpa`)

- `CommandBackedAssistantResultApplicator.applyIssue`: parse `action.severity()`. A blank or
  null value passes `null` (the kind's default on create, unchanged on update). An unknown value
  logs `WARN "assistant {} sent unknown severity {} for action {}; using default"` and passes
  `null`. Both branches, lexical and general, call `setSeverity`. The finding's own
  `setSeverity(action.severity())` is unchanged and keeps the raw value.
- `AbstractAssistant.addSimpleIssue`: the re-attach branch leaves severity `null` (unchanged) and
  keeps its explicit `mustBeResolved` copy.

### CLI (`requel-cli`)

`OpenIssuesCommand.renderText` prefixes each line with the severity:
`HIGH   [Goal Checkout] ... (must resolve)`. JSON output carries the DTO field.

### Angular

- `models/annotation.ts` `IssueDto` and `open-issues.ts` `OpenIssueDto`:
  `severity: 'LOW' | 'MEDIUM' | 'HIGH'`. A shared `ISSUE_SEVERITY_OPTIONS` and
  `severityRank()` live beside `SUPPORT_LEVEL_OPTIONS`.
- `annotation.service.ts` `addIssue(..., mustBeResolved, severity)` sends `severity`.
- `annotations-section.ts`: the add-issue form gets a `p-select` (`data-testid="annotation-issue-severity"`,
  label "Severity", default `MEDIUM`). Each issue row gets an `app-tag`
  (`data-testid="annotation-issue-severity-badge"`, tone danger / warning / info for
  HIGH / MEDIUM / LOW). The server order is kept.
- `open-issues.ts`: a sortable "Severity" column rendered as the same tag. The table's default
  sort switches from `entityType` to a derived `severityRank` field, descending, so a
  client-side re-sort never undoes the server order. Sorting by the column uses the rank, not
  the string (alphabetical order would put HIGH < LOW < MEDIUM).
- `project-workspace.ts` only counts `mustBeResolved` and needs no change.

## Step by step

1. `IssueSeverity` and the `Issue` / `EditIssueCommand` interface changes (`annotation-domain`).
2. `IssueImpl` / `LexicalIssue` mapping, defaults, JAXB attribute; `EditIssueCommandImpl` /
   `EditLexicalIssueCommandImpl` semantics.
3. `V22__issue_severity.sql`.
4. XML: xsd, `AnnotationImportXml`, mapper, draft, assembler.
5. `service-api` DTOs, the registrar mapping and the `mustBeResolved` fix, both query orderings.
6. `draftAnnotation` validation and schema.
7. Applicator and `AbstractAssistant`.
8. CLI rendering.
9. Angular models, service, annotations section, open issues.
10. Tests per the plan below; `tmp/271-verify.sh` runs `mvn clean verify`, the Angular unit
    suite (`CI=1 npm test -- --watch=false`), both `tsc` checks and a development build.

## Test plan

**Unit**

- `IssueSeverityTest`: `parse` is case-insensitive, trims, and returns empty for blank, null and
  unknown values; ranks are ordered.
- `McpReadServiceTest`: `draftAnnotation` upper-cases `"high"`; rejects `"urgent"` with
  `McpInvalidParamsException`; omitting severity leaves it null; the schema lists the enum.
- `CommandBackedAssistantResultApplicatorTest`:
  - a general issue action with `HIGH` persists a `HIGH` issue;
  - a lexical action with no severity persists `LOW`, and a general one `MEDIUM`;
  - `"urgent"` persists the default and logs one WARN;
  - a re-run of the same action key with `MEDIUM` → `HIGH` updates the issue; a re-run with null
    leaves it alone;
  - the finding still records the raw value.
- `WriteInputDtoConstructionTest`: `EditIssueInput` builds with the new component.
- `OpenIssuesCommand` rendering (CLI `QueryCommandsTest`): severity prefix present, server order
  kept.

**Integration (H2, `requel-app`)**

- `AnnotationCommandTest`:
  - create `Issue` → `MEDIUM`; create `LexicalIssue` → `LOW`; create with `HIGH` → `HIGH`;
  - update with null severity → unchanged; update with `LOW` → `LOW`;
  - **`mustBeResolved` fix:** create with `true`, update text only → still `true` (fails on the
    current tree);
  - text-match reuse: an existing `MEDIUM` issue matched by text with `HIGH` supplied becomes
    `HIGH`, and its `mustBeResolved` is unchanged; the same for a lexical issue matched by word.
- New `IssueSeverityIT` (gateway write surface, the `GatewayPositionTypeIT` pattern):
  - `EditIssue` with `"high"` → the DTO says `HIGH`, and the issue re-read says `HIGH`;
  - `"urgent"` → rejected with a `severity` field error and nothing written;
  - `getOpenIssues` and `getProjectContext.openIssues` come back `HIGH`, `MEDIUM`, `LOW`,
    tie-broken by the existing order; no entry has a null severity;
  - per-entity `getAnnotations` issues are in severity-then-id order.
- `ProjectXmlStreamingRoundTripIT` and `ProjectJAXBTest`:
  - `HIGH` issue and `LOW` lexical issue round-trip;
  - a file with no `severity` attribute imports as `MEDIUM` / `LOW`;
  - the export validates against `project.xsd`.

**MySQL (Testcontainers)**

- `IssueSeverityMySqlIT extends IssueSeverityIT` (the `EditPositionDedupMySqlIT` pattern), so
  the gateway cases run on the Flyway schema.
- `IssueSeverityMigrationMySqlIT`: Flyway to `target=21`; insert an issue, a lexical issue and a
  note over JDBC; migrate to 22. Result: issue `MEDIUM`, lexical `LOW`,
  note `NULL`.

**Angular unit**

- `annotations-section.spec.ts`: the picker defaults to `MEDIUM` and sends the chosen value;
  the badge renders each tone; the list keeps the server order. The `.a11y.spec` still passes
  with the new control.
- `open-issues.spec.ts`: severity column present; the default order is `severityRank`
  descending; clicking the column sorts by rank, not alphabetically.
- `annotation.service` spec: `addIssue` posts `severity`.

**e2e (CI)**

- `annotations.e2e.ts`: add an issue with `HIGH`; the badge reads High and the issue lists
  above an existing `MEDIUM` one.
- `open-issues.e2e.ts`: a `HIGH` issue appears first.
- After the run, grep the app log per `CLAUDE.md`'s Testing section.

## Out of scope

- Automatic severity assignment by the assistants, beyond the fixed per-kind default (ticket).
  #268 decides how the lexical assistants score what they raise.
- A command that persists a `draftAnnotation` result.
- `EditIssue`'s `@CommandDescription`: #296 writes it once this lands.
- Teaching `CommandInputSchema` to emit enum values for gateway input fields.
- Severity on notes, positions or arguments.
- Filtering by severity in the UI, gateway, MCP and CLI; this ticket only orders. Filed as a v2.1
  follow-on (draft: `271-followup-severity-filter-issue.md`).
- Showing issue severity to the AI in `IssueSnapshot` / the entity context pack. Filed as a
  v2.1 follow-on (draft: `271-followup-severity-ai-context-issue.md`).
- `mustBeResolved` semantics on other annotation commands.

## Risks

- **`mustBeResolved` behavior change.** Any caller that relied on "update without the field
  clears it" changes. The Angular client only creates issues through `EditIssue`, and the
  applicator and `AbstractAssistant` always set the field, so no known caller relies on it.
- **Open Issues default sort changes** from Type to Severity. Intentional (decision 4). An e2e
  that asserted the Type order would be updating encoded old behavior, not a regression.
- **The `IssueDto` / `OpenIssueDto` constructor order changes.** Every construction site is in
  this tree (`toIssueDto`, `getOpenIssues`, and the test stubs `StubProjectQueryGateway`
  and `QueryCommandsTest`), and the compiler finds them all.
- **The getter fallback hides a missed write path.** `getSeverity()` never returns null, so a path
  that forgets to set severity reads as the default instead of failing. The MySQL migration IT and
  the "no null on read" assertions cover the known paths. The fallback is the price of the AC's
  "no read path returns null".
- **AI prompt drift.** A model that starts returning `"CRITICAL"` degrades to `MEDIUM` with a WARN
  rather than failing. The WARN is how you'd notice.

## AC mapping

| #271 acceptance criterion (as amended) | Covered by |
|---|---|
| A severity supplied to `draftAnnotation` survives to the persisted issue: via the applicator path, and via `EditIssue` for a caller persisting a draft | applicator tests; `IssueSeverityIT` `EditIssue` cases |
| It comes back on read | `IssueSeverityIT` re-read, `IssueDto` / `OpenIssueDto` |
| `getProjectContext` returns open issues in severity order | `IssueSeverityIT` ordering case |
| An unrecognised severity is a field-level validation error, not a silent drop | `IssueSeverityIT` `"urgent"` case (`EditIssue`); `McpReadServiceTest` (`draftAnnotation`) |
| Existing issues carry a defaulted severity after migration, and no read path returns null | `IssueSeverityMigrationMySqlIT`; getter fallback; `IssueSeverityIT` non-null assertions |
| Order issues by severity wherever they are listed (work item) | query ITs, CLI test, Angular specs, e2e |
| Folded in: `mustBeResolved` omitted on update is unchanged | `AnnotationCommandTest` regression case |
