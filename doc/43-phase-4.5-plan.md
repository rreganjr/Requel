# Phase 4.5 — Close the Loop

Issue #43. This phase exists because the review in `doc/43-opus48-review.md` found that
the Phase 1–4 modules (SPI contracts, dispatcher/worker, run persistence, context packs,
AI provider abstraction, partial MCP) are in place, but the runtime path they exist to
serve has never run end-to-end. No production code turns an assistant result into an
annotation, nothing dispatches an `AnalysisRequest`, and no `RequelAssistant` ships.

Phase 4.5 is the bridge between the foundation and Phase 5 (first AI assistant). Its goal
is a provably working **command → dispatch → assistant → annotation** loop on a single
legacy path, plus the bug fixes and the NLP-optional work the loop depends on.

The plans this phase implements against: `doc/assistant-spi-plan.md`,
`doc/ai-assistance-plan.md`, `doc/nlp-optional-plan.md`. Contract/structural prep already
done in this branch (provider module split, `AnnotationAction` reshape + `EvidenceRef`,
MCP naming, plan-doc updates) is **not** repeated here.

## Sequencing principle

Close the loop before breadth. Each step below is independently testable and leaves the
build green. Steps 1–6 are the critical path; steps 7–9 can proceed in parallel once the
applicator (step 3) exists.

## Work items

### 1. Fix `AnalysisRequest` round-trip in `JpaAssistantRunStore`

Problem (review concern #1): `JpaAssistantRunStore.rebuildRequest` fabricates
`EntityRef.of("Unknown", 0L)`, a placeholder `UserRef(0L, "unknown")`, and empty
attributes when reading a run back. The worker reads the record back via `findRun(runId)`
before invoking assistants, so in production the `AssistantContext` it builds has fake
user refs and **no attributes** — which silently breaks execute-as-triggering-user and
idempotency once the applicator is real.

- Persist the full `AnalysisRequest` faithfully: triggering user ref, assistant user ref,
  project ref, target ref, attributes, and locale (see step 2).
- Either store the request as a serialized column on `assistant_runs` or add the missing
  scalar columns; round-trip must reproduce `equals()`-equal refs and attributes.
- Test: queue a run with non-empty attributes + named users, read it back, assert the
  rebuilt `AnalysisRequest` equals the original (extend `JpaAssistantRunStoreTest`).

### 2. Add locale to `AnalysisRequest` and thread it through

Problem (review concern #2): `AssistantRunWorker` hardcodes `Locale.getDefault()`; the
plan lists locale as a context field.

- Add `Locale locale` to `AnalysisRequest`; populate `AssistantContext` from it in the
  worker instead of `Locale.getDefault()`.
- Default to the triggering user's locale (or `Locale.ROOT`) at request-construction time,
  not in the worker.

### 3. Implement the real `AssistantResultApplicator`

Problem (review gap #1): only `NoOpAssistantResultApplicator` exists; no `CommandHandler`
/ `AnnotationCommandFactory` use anywhere in `assistant-core`.

- New `CommandBackedAssistantResultApplicator` in `assistant-core` (keep the no-op as the
  test/disabled fallback).
- Map every `AnnotationAction.ActionType` to the factory method per the command-mapping
  table in `doc/assistant-spi-plan.md`, selecting lexical / change-spelling / add-word
  sub-variants from `metadata.kind`. `CREATE_OR_UPDATE_*` looks up the existing annotation
  by idempotency key; if found, populates the `Edit*Command` with the existing id +
  optimistic-lock version, else creates.
- Resolve `parentActionKey` references (position → in-result draft issue) to the parent's
  freshly-created annotation id before applying the child action.
- Execute every command as the **triggering user** through the existing
  `AuthorizingCommandHandler` chain (no new handler infrastructure). Stamp the new
  annotation `source = ASSISTANT:<assistantId>` and `assistant_idempotency_key`.
- Validate/cap action text length and reject unknown action types (untrusted-input rule).
- Increment `findings_count` on the run; write `AssistantFinding` rows.
- Map the two new annotation columns (`assistant_idempotency_key`, `source`) in the
  `annotation-jpa` entity — V8 adds them to the DB but no JPA field maps them yet
  (review concern #3). Add test-schema coverage (tests run Hibernate `create-drop` with
  Flyway off).
- Tests: idempotent re-apply (same key → update, not duplicate); unknown action rejected;
  over-length text capped; command executed as triggering user; SSE/audit fire.

> **Status (landed).** `CommandBackedAssistantResultApplicator` is implemented in
> `assistant-core` (annotation-jpa + user-domain added as deps; `@Primary` over the
> retained `NoOpAssistantResultApplicator`). It covers the **CREATE_OR_UPDATE family**
> (note, issue incl. lexical via `metadata.kind`, position incl. change-spelling /
> add-word, argument), resolves the target annotatable via `AssistantTargetLoader`, the
> grouping object via `ProjectOrDomainEntity.getProjectOrDomain()`, and the editedBy user
> by username (authorization keys off `getEditedBy()`, so no SecurityContext is needed on
> the worker thread). `parentActionKey` links a position/argument to an issue/position
> created earlier in the same result. Idempotency uses `AssistantFinding` (upsert by
> action key, touch on re-run) plus the repository's content-level find-by-text dedupe;
> text is length-capped; `findings_count` is bumped on the run.
>
> **Moved to Step 6** (needs the find-by-id/state-machine work): the `RESOLVE_ISSUE`,
> `DELETE_*`, and `REMOVE_ANNOTATION_FROM_ANNOTATABLE` action types (currently skipped with
> a log line, not applied), the SUPERSEDED / AUTO_RESOLVED / MANUALLY_RESOLVED transitions,
> and stamping the `assistant_idempotency_key` / `source` columns on the annotation row
> (idempotency currently lives entirely on `AssistantFinding`).

### 4. Wrap one legacy assistant as a `RequelAssistant<T>` adapter

Problem (review gap #4): no production `RequelAssistant`; the legacy assistants in
`project-jpa` still call `AnnotationCommandFactory` directly.

- Create the `assistant-legacy-nlp` module (per `doc/assistant-spi-plan.md` Target Module
  Shape). Start with the lexical/text path — `LexicalAssistant` / `TextEntityAssistant` —
  wrapped as `RequelAssistant<TextEntity>`.
- The adapter returns `AnnotationAction`s + `EvidenceRef`s instead of creating annotations
  itself. Register it with a cleanup policy (default `MARK_SUPERSEDED`).
- Register an `AssistantTargetLoader` for the chosen target type so the worker can reload
  it in the new transaction.

### 5. Route one `analyzeX` path through the dispatcher

Problem (review gap #3): nothing calls `dispatch(...)`; live analysis still flows through
`AssistantFacade` → `AssistantTaskRunner`.

- Pick one path (the lexical/text one from step 4). Have its `AnalyzableEditCommand`
  /`AssistantFacade` entry construct an `AnalysisRequest` and call
  `AssistantDispatcher.dispatch(...)` instead of the type-specific `analyzeX` method.
- Preserve the issue #39 transactional-proxy boundary (already correct in the
  dispatcher/worker split).
- **Integration test** (the keystone of this phase): edit an entity through the command,
  let dispatch run, assert the same observable annotations the legacy path produced —
  proving the loop end-to-end with audit/SSE intact.
- Leave the other `analyzeX` paths on the legacy path for now; they migrate in Phase 5+.

### 6. Finding state machine + per-assistant cleanup policy

Problem (review gap #2): `AssistantFindingState` and `AssistantFindingEntity` exist but no
transition code; no cleanup policy.

- Add cleanup policy to assistant registration metadata (`MANUAL` / `MARK_SUPERSEDED` /
  `AUTO_RESOLVE_IF_UNTOUCHED`, default `MARK_SUPERSEDED`).
- Implement the transitions owned by the applicator per `doc/assistant-spi-plan.md`
  (Finding State Machine): `ACTIVE → SUPERSEDED / AUTO_RESOLVED / MANUALLY_RESOLVED /
  DROPPED / ACTIVE(touched)`.
- Tests for each transition, including "untouched" detection (no human edits/replies/
  non-assistant positions on the linked annotation).

> **Status (largely landed).** Landed: `CleanupPolicy` enum + `RequelAssistant.cleanupPolicy()`
> default (`MARK_SUPERSEDED`); per-assistant failure isolation in `AssistantRunWorker` (one
> assistant erroring is logged and skipped rather than failing the run — this also subsumes the
> "per-assistant isolation" item the lexical adapters previously worked around with internal
> try/catch); the re-run idempotency integration test; the annotation
> `source` / `assistant_idempotency_key` column mapping + key-based dedupe; the
> `AUTO_RESOLVE_IF_UNTOUCHED` reconciliation in the applicator
> (`reconcileStaleFindings` / `autoResolveIfUntouched` / `isUntouched`), with the four lexical
> adapters opted in and end-to-end coverage; the `MANUALLY_RESOLVED` transition via the
> `FindingResolutionTrackingCommandHandler` chain decorator (a human resolving an
> assistant-raised issue moves its finding `ACTIVE → MANUALLY_RESOLVED`); the
> `SUPERSEDED` transition for the default `MARK_SUPERSEDED` policy (`reconcileStaleFindings`
> now stamps stale findings `SUPERSEDED` + `superseded_by_run_id`, leaving the annotation in
> place); and the RESOLVE / DELETE / REMOVE action types in the applicator
> (`applyCleanupAction`, transitioning the finding to `DROPPED` / `AUTO_RESOLVED`).
>
> The glossary-term-referer project edit also landed: `LexicalGlossaryTermAssistant` now emits
> an `ADD_GLOSSARY_TERM_REFERER` action when a phrase matches an existing glossary term, and
> the applicator applies it through `EditGlossaryTermCommand.setAddReferers` (idempotent at the
> domain level, so it records no finding). The `removeUnneededLexicalIssues` behaviour is
> covered by the `AUTO_RESOLVE_IF_UNTOUCHED` reconciliation the lexical adapters opted into.
> The finding state machine
> (`ACTIVE → SUPERSEDED / AUTO_RESOLVED / MANUALLY_RESOLVED / DROPPED / ACTIVE(touched)`) is
> complete, and Step 6 is done.
>
> **Known issue — cross-assistant lexical-word collision: RESOLVED.** Originally the
> applicator deduped `LEXICAL` issues by word, so glossary's 3-arg `findLexicalIssue` (no
> property) could match — and `setText(...)` overwrite — a spelling issue for the same word.
> This was fixed by switching idempotency to key-based lookup: each finding owns its
> annotation via `assistant_findings.applied_annotation_id` (`loadExistingAnnotation(actionKey,
> type)`), keyed by the assistant's `actionKey` rather than by content. Different assistants
> raising an issue for the same word now keep separate annotations, and the applicator never
> rewrites a human-authored annotation with matching text.

### 7. NLP-optional Scope 1 (`doc/nlp-optional-plan.md`)

Problem (review gap #7): unimplemented.

- Conditional auto-config in `nlp-jpa`: `@ConditionalOnClass(NLPProcessorFactory.class)` +
  `@ConditionalOnProperty(requel.nlp.enabled, matchIfMissing=true)`; relax the
  unconditional `@Component` on `NLPProcessorFactoryImpl`.
- `NoOpNLPProcessorFactory` + `@ConditionalOnMissingBean` config in `requel-app` (or a
  shared module) returning safe empty values (never null).
- `requel.nlp.enabled=true` default in `application.properties`.
- Smoke-test profile with `requel.nlp.enabled=false`: app boots, and an assistant run
  invoked under it records `status = SKIPPED` with an NLP-disabled `AssistantMessage` and
  an explicit empty `AssistantResult` (ties to the SPI no-op contract).

### 8. MCP authentication + dual identity (`doc/ai-assistance-plan.md`)

Problem (review gap #5): no security-context population, no session token, no
triggering-user vs assistant-pseudo-user handling, no audit stamping.

- Populate a Spring Security context as the triggering user for each MCP call so query
  authz mirrors REST.
- Carry both identities per session (triggering user + assistant pseudo-user); stamp
  `triggering_user_id`, `assistant_user_id`, `run_id` on an MCP audit row.
- Internal path: short-lived session token scoped to
  `(triggering_user_id, project_id, run_id, assistant_user_id)`.
- Contract tests with authenticated and unauthorized users.
- Lower priority: this gates *external* AI rollout, not the internal close-the-loop, so it
  can trail steps 1–6.

> **Status.** The concrete, non-speculative part landed: an `mcp_calls` audit table
> (`V10__mcp_calls.sql`) + `McpCallAudit` entity / `McpCallAuditRepository` /
> `McpCallAuditor`, wired into `McpJsonRpcHandler` so every JSON-RPC call records the
> triggering user (resolved from the security context), method, tool/resource name,
> outcome (OK / error code + summary), and duration. Auditing is best-effort (failures
> logged, never breaking the call). The MCP endpoint is already JWT-secured under
> `/api/**` and tools already run query authz in the caller's security context, so
> "authz mirrors REST" already holds. `assistant_user_id` / `run_id` columns exist but
> are nullable: the short-lived **session token + dual identity** (triggering user +
> assistant pseudo-user) exist to let the *internal AI runtime* call back into MCP during
> a run, and that AI assistant is deferred to Phase 5+ — so the token minting/validation
> and dual-identity stamping are **deferred to Phase 5+** alongside their first caller,
> rather than built as infrastructure ahead of any consumer.

### 9. MCP remaining read surfaces

Problem (review gap #6): ~3 of 12 surfaces implemented.

- Add resources: glossary, open-issues, entity annotations.
- Add tools: `requel.searchProjectEntities`, `requel.getProjectContext`,
  `requel.getEntity`, `requel.getEntityNeighbors`, `requel.getAnnotations`,
  `requel.draftAnnotation`. `requel.draftAnnotation` returns an `AnnotationAction` draft to
  the applicator (step 3) rather than persisting.
- Fix JSON-RPC error codes (`-32601` / `-32602` instead of `-32603`).

> **Status (in progress).** Landed: the `-32602` (Invalid params) error code via
> `McpInvalidParamsException`; and the read surfaces that reuse existing query controllers —
> glossary (`requel.getGlossary` + `requel://projects/{name}/glossary`), open-issues
> (`requel.getOpenIssues` + `requel://projects/{name}/open-issues`), and entity annotations
> (`requel.getAnnotations` tool). These delegate through new `ProjectQueryGateway` methods to
> `ProjectQueryController.listTerms` / `getOpenIssues` and `AnnotationQueryController`. Also
> landed: `requel.getEntity` — reads one entity (Goal / Story / Actor / UseCase / Scenario /
> GlossaryTerm) by type + id, routing through the gateway to the existing per-entity
> controller getters (unknown type -> `-32602`); and `requel.getEntityNeighbors`, which
> returns an entity's related entities grouped by relationship (as `EntityReferenceDto`s),
> derived in the gateway from the detail DTOs; and `requel.searchProjectEntities`, which
> name-matches across goals / stories / actors / use cases / scenarios / glossary terms by
> reusing the existing `list*` controllers and filtering (case-insensitive substring). Still
> to do: `requel.getProjectContext` (a composite context bundle), and `requel.draftAnnotation`
> (returns an `AnnotationAction` draft — needs an `assistant-api` dependency on `mcp-server`,
> which it does not have today).

## Deferred to Phase 5+ (explicitly out of scope here)

- The AI-backed `RequelAssistant` for `REQUIREMENTS_REVIEW` and wiring `assistant-ai` /
  `assistant-openai` into the dispatcher.
- Enforcing `requel.ai.maxInputTokens` as a real input cap and bounding AI output field
  lengths (review concern #5) — do this when the AI assistant is wired, since that is the
  first code path that actually sends/receives provider bodies.
- Migrating the remaining `analyzeX` paths and removing `AssistantFacade` /
  `AssistantTaskRunner`.
- `AssistantProjectSettings` persistence and the assistant-settings/run-history APIs.

## Exit criteria

1. Editing an entity on the migrated path dispatches an `AnalysisRequest`, the legacy
   lexical assistant runs through the SPI, and the applicator creates the expected
   annotations — verified by an integration test asserting parity with the old path.
2. Re-running on an unchanged target does not duplicate findings (idempotency holds).
3. `requel.nlp.enabled=false` boots cleanly and records `SKIPPED` assistant runs.
4. `mvn test` green across the reactor; no regressions in existing IT suites.
