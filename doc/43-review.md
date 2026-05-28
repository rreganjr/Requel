# Issue 43 Plan Review

Reviewed against:

- GitHub issue #43: https://github.com/rreganjr/Requel/issues/43
- First issue comment: https://github.com/rreganjr/Requel/issues/43#issuecomment-4560006774
- `doc/assistant-spi-plan.md`
- `doc/ai-assistance-plan.md`
- `doc/nlp-optional-plan.md`

## Summary

The plans are directionally solid, but they are not yet internally consistent enough to implement from without a cleanup pass. The issue comment records important decisions that are only partially reflected in the main document bodies. A few sections also predate issue #39 work and still describe the old `AssistantFacade` implementation.

## Findings

### Current state is stale after issue #39

`doc/assistant-spi-plan.md` and `doc/ai-assistance-plan.md` still describe `AssistantFacade` as manually constructing `LexicalAssistant` and concrete entity assistants. On the issue #39 branch, that responsibility moved into `AssistantTaskRunner`, with `AssistantFacade` acting as an async submission and notification facade.

Before issue #43 implementation starts, update the current-state sections so the migration plan starts from the actual code shape.

### Resolved decisions are not propagated into the main plan text

The first issue comment resolves several open decisions:

- module shape is `assistant-api` plus `assistant-core` from day one
- MCP belongs in a new `mcp-server` module, bundled in-process with `requel-app`
- first AI task is `REQUIREMENTS_REVIEW` only
- project-scoped assistant settings come first
- each external MCP client gets its own assistant pseudo-user
- provider body retention is metadata by default, with failure retention and bounded opt-in capture modes

The main sections still contain older ambiguous wording such as `assistant-api` or `assistant-spi`, `assistant-core` or `assistant-impl`, `mcp-server` or `service-impl`, and first two AI tasks. These should be rewritten to match the resolved decisions instead of relying on a later section to override them.

### MCP placement conflicts

`doc/ai-assistance-plan.md` currently presents an in-process MCP endpoint in `service-impl` as the first useful path, while the resolved decision says to create a new `mcp-server` Maven module bundled in-process with `requel-app`.

The MCP section should be rewritten around the decided shape:

- `mcp-server` owns MCP protocol/resources/tools.
- It delegates through a `ProjectQueryGateway` style abstraction.
- The first implementation is in-process and uses existing service/query beans.
- A future standalone/local bridge can reuse the same tool contracts while replacing the gateway with auth exchange plus REST calls.

### Provider request/response retention conflicts

The security sections still say provider request/response bodies are stored only behind an explicit debug flag. The resolved decision is more specific:

- metadata only by default
- failed run bodies are always stored
- per-project capture windows are allowed
- per-run user opt-in is allowed up to a configured ceiling
- success sampling defaults to 1% and is off in dev/test
- all bodies have a hard TTL and are encrypted with a separate key
- `AssistantRun` records whether bodies exist and why

The security, data model, and testing sections should reflect that actual policy.

### Broken `43-comment.md` references

All three docs refer to `43-comment.md`, but that file no longer exists locally. Replace those references with the issue comment URL:

`https://github.com/rreganjr/Requel/issues/43#issuecomment-4560006774`

Alternatively, restore a local copy under `doc/43-comment.md`, but the current references are broken.

### NLP optionality has a module-boundary prerequisite

`doc/nlp-optional-plan.md` says Requel should start when `nlp-jpa` is absent, but the fallback factory still depends on `NLPProcessorFactory`, which currently lives in `nlp-jpa`.

That means true optional absence requires extracting the NLP interfaces to a small `nlp-api` or `nlp-core` module first. The plan currently treats that split as a later possibility, but it is a prerequisite if `nlp-jpa` can be removed from the classpath.

If the short-term goal is only `requel.nlp.enabled=false` with `nlp-jpa` still present, the plan should say that explicitly. If the goal is absent-module startup, add `nlp-api` extraction as step 1.

## Missing Implementation Details

Before implementation, the plans should specify:

- exact `AssistantRun`, `AssistantFinding`, `AssistantUsage`, and annotation source/idempotency fields
- stale-finding states and transitions: active, superseded, auto-resolved, manually resolved
- exact commands used by `AssistantResultApplicator` for notes, issues, positions, and annotation updates
- ownership of context pack DTOs: `assistant-core`, `service-api`, or a new shared DTO module
- MCP authentication/session flow for internal hosted-model calls versus external local clients
- no-op NLP behavior: prefer safe empty results or explicit "unavailable" results over mostly `null`
- migration sequence from `AssistantTaskRunner` to `AssistantDispatcher` so issue #39 session handling is preserved

## Recommended Cleanup Order

1. Update current-state sections for the issue #39 `AssistantTaskRunner` change.
2. Replace all `43-comment.md` references with the GitHub issue comment URL or restore a local doc copy.
3. Fold resolved decisions into the main body and rollout plan, not only the `Resolved Decisions` sections.
4. Rewrite MCP sections around the decided `mcp-server` module and `ProjectQueryGateway`.
5. Rewrite provider retention/security sections to match the resolved retention policy.
6. Clarify NLP optionality scope and add `nlp-api` extraction if absent-module startup is still a requirement.
7. Add concrete data model and applicator command details before starting the first implementation branch.

## Recommendation

Do one documentation cleanup pass before coding issue #43. The desired architecture is clear, but the current docs contain enough drift and contradictory wording that implementation could easily choose the wrong module boundary or rollout sequence.

## Second Pass

Reviewed the updated versions of:

- `doc/assistant-spi-plan.md`
- `doc/ai-assistance-plan.md`
- `doc/nlp-optional-plan.md`

### What improved

The main first-pass issues are now addressed:

- Current-state sections now account for issue #39 and explicitly preserve the `AssistantTaskRunner` transactional-proxy boundary during the migration to `AssistantDispatcher`.
- The resolved decisions from issue #43 are now folded into the main plan text, not only listed at the bottom.
- `43-comment.md` references were replaced with the GitHub issue comment URL.
- MCP placement is now consistently a new `mcp-server` module bundled in-process with `requel-app`, with `ProjectQueryGateway` as the future standalone seam.
- Provider body retention policy now matches the resolved decision: metadata by default, failed-body retention, capture windows, user opt-in, sampling, TTL, and separate encryption.
- NLP optionality now correctly distinguishes short-term disabled-in-place behavior from longer-term absent-module startup, and treats `nlp-api` extraction as a prerequisite for the absent-module case.
- The SPI plan now includes concrete run/finding/usage fields, finding states, DTO ownership, and no-op behavior.

Overall, the documents are now coherent enough to serve as implementation guidance.

### Remaining issues to fix before coding

#### Annotation command names are illustrative, not current API

`doc/assistant-spi-plan.md` lists applicator mappings such as:

- `AnnotationCommandFactory.newAddNoteCommand(...)`
- `newAddIssueCommand(...)`
- `newAddPositionCommand(...)`
- `newAddArgumentCommand(...)`
- `newEditAnnotationTextCommand(...)`
- `newRemoveAnnotationCommand(...)`

The current `AnnotationCommandFactory` exposes edit/delete/remove commands instead:

- `newEditNoteCommand()`
- `newEditIssueCommand()`
- `newEditLexicalIssueCommand()`
- `newEditPositionCommand()`
- `newEditArgumentCommand()`
- `newResolveIssueCommand(Position position)`
- `newRemoveAnnotationFromAnnotatableCommand()`
- delete commands for notes/issues/positions/arguments

Either update the plan to use the actual factory methods, or explicitly state that issue #43 will introduce a new applicator-facing command abstraction that maps onto the existing factory. As written, a worker could implement against method names that do not exist.

#### Dispatcher transaction and async boundaries need one more precise sentence

The docs say `AssistantDispatcher` owns async execution and is also reached through a transactional proxy. That is directionally right, but the implementation needs a clear split to avoid self-invocation or transaction-on-the-wrong-thread mistakes.

Recommended wording:

`AssistantDispatcher.dispatch(...)` enqueues work and returns an `AssistantRunHandle`; the executor invokes a separate Spring bean method such as `AssistantRunWorker.runInNewTransaction(runId)` annotated with `@Transactional(REQUIRES_NEW)`. That worker reloads targets and calls assistants.

Or, if `AssistantFacade` keeps owning the executor during migration, say:

`AssistantFacade` submits a Runnable that calls `AssistantDispatcher.dispatchInTransaction(...)` through the Spring proxy; the transactional method must run inside the executor thread, not before queuing.

This matters because issue #39 was specifically about the thread/session boundary.

#### Assistant-core dependency on service-api should be made explicit

The SPI plan says context pack DTOs live in `assistant-core` and the builders consume `service-api` projection DTOs. That may be fine, but the target module shape does not explicitly say whether `assistant-core` may depend on `service-api`.

Before implementation, choose one:

- `assistant-core` may depend on `service-api` DTOs and query interfaces.
- `assistant-core` owns assistant DTOs and consumes a small gateway interface; `service-impl` or `mcp-server` adapts existing service DTOs into those shapes.

The second option keeps assistant-core less coupled to REST/API DTO evolution.

#### Authorization model may require infrastructure beyond existing commands

The plan says commands execute as the assistant user, but authorization checks use the triggering user's permissions. That is the right policy, but it may not match the current command-handler assumptions if authorization is derived from `editedBy` or current security context.

Before coding the applicator, identify the exact mechanism:

- extend command metadata with `triggeredBy` plus `executedAs`
- add an assistant-aware authorization context
- or run commands as the triggering user while storing assistant attribution separately on `AssistantRun` and annotation source fields

Without that decision, applicator behavior could either over-authorize the assistant user or fail writes the triggering user should be allowed to request.

#### AI use-case list still looks broader than Phase 5

The plan correctly says Phase 5 starts with `REQUIREMENTS_REVIEW` only, but the `AI Assistant Use Cases` section still lists scenario review, glossary extraction, project summary, and open issue triage without clearly labeling them as later candidates.

This is not contradictory anymore, but it would be safer to rename that section to `Candidate AI Assistant Use Cases` and add one line: `Only REQUIREMENTS_REVIEW is in scope for the first AI assistant. The rest are backlog candidates.`

#### Current-state wording around MCP-facing services is slightly misleading

`doc/ai-assistance-plan.md` still says `service-api` / `service-impl` are "the right place to expose MCP-facing query/application services." Since MCP is now owned by `mcp-server`, this should probably say those modules provide the authenticated query/application services that `mcp-server` delegates to.

Small wording issue, but it prevents a future reader from putting protocol handlers back into `service-impl`.

### Updated recommendation

The updated docs are now close enough to proceed after the small clarifications above. I would fix the command factory mapping and dispatcher transaction wording before opening the implementation branch; the other notes can be cleaned up as part of the first implementation PR description or early module scaffolding.
