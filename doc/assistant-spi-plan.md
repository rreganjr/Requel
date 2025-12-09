# Assistant SPI Plan

## Goals
- Allow third parties (or future internal modules) to plug in new “assistants” that analyze project data and perform some action by having a simple SPI
- Actions may be 
  - to directly alter project state. This is not ideal but should be possible.
  - Add notes and issues to project entities where issues have positions that will change project state, i.e. a user reviews and chooses what should happen.
  - interaction with an external service, for example schedule a meeting, send an email or Slack message, create a Jira ticket or github issue
- Keep existing annotation-based assistants working, but make the SPI general enough for non-annotation actions (e.g., schedule meetings, call external services).
- Support various kinds of analysis, not just NLP/Lexical
- Enable swapping the current NLP-backed `LexicalAssistant` analysis for newer AI backends (e.g., ChatGPT/Codex) without changing the rest of the app.

## Current State
- Assistants live in `project-jpa` (`AbstractAssistant`, `LexicalAssistant`, `GoalAssistant`, etc.).
- `AbstractAssistant` is the root of all the assistants with support for adding annotations: note, issue, and positions on issues. It has a `createMessage` helper for generating messages from a resource bundle of message templates.
- Public interface is effectively `TypedAssistant<T>` (get/set entity + analyze). Everything else is concrete. Note nothing implements this interface
- `TypedAssistant<T>` is effectively dead code today—nothing references it.
- `AssistantFacade` constructs `LexicalAssistant` and wires it into other assistants; assistants directly depend on `LexicalAssistant`, `AnnotationCommandFactory`, `NLPProcessorFactory`, etc.
- Feedback path is primarily annotations (issues/notes/positions) created via the annotation command factory.
- `AssistantFacade` is invoked from project commands after edits/imports; it manually instantiates a `LexicalAssistant` and the concrete assistants per entity type (goal, story, actor, use case, scenario/step, project).
- `ProjectOrDomainEntityAssistant` is the root of the assistants for project entities: goals, actors, etc.
- `TextEntityAssistant` extends `ProjectOrDomainEntityAssistant` and is the basis for all the other assistants, it basically converts text data to an NLPText for analysis
- the entity assistants only deal with NLP processing of text, `ProjectOrDomainEntityAssistant` does the primary work in `analyze()` using a `LexicalAssistant` to analyze various text properties on project entities
- The `LexicalAssistant` doesn't really follow the Assistant interface, it is more of a facade for various NLP analysis
- 
## Proposed SPI
  1) Define a stable assistant contract in a neutral module (e.g., `assistant-api`) replacing the unused `TypedAssistant<T>`:
     - `AssistantContext` (project/domain reference, identity `User` from `platform-identity`, resource bundle/locale, optional services map, optional `AssistantRole` for auth).
       - `AnnotationAPI` maybe the methods are directly on the context, maybe the grouping object is taken from the context project/domain reference and not passed in
       ```
          public interface AnnotationAPI {
            Note addNote(Object groupingObject, User assistantUser, Annotatable thingBeingAnalyzed, String noteText) throws Exception
            void addIssue(ProjectOrDomain projectOrDomain, User assistantUser, ProjectOrDomainEntity thingBeingAnalyzed, String issueText) throws Exception
            Position addSimplePositionToIssue(ProjectOrDomain projectOrDomain, User assistantUser, Issue issue, String positionText) throws Exception
            void removeAnnotation(Annotation annotation, Annotatable annotatable) throws Exception       
       ```
     - `RequelAssistant<T>` (name chosen to avoid clashes with other libraries) with methods:
       - `Class<T> targetType()`
       - `void setContext(AssistantContext ctx)`
       - `void setTarget(T target)`
       - `AssistantResult analyze()`
     - `AssistantResult` carrying:
       - `List<AnnotationAction>`: normalized requests to create/update/remove annotations/positions (applied by the façade via `AnnotationCommandFactory`).
       - `List<AssistantMessage>`: human-readable findings for UI/logging when no annotation is desired (e.g., “Glossary candidate: ‘data lake’”).
       - `List<ExternalAction>` (optional): side effect intents such as “POST to webhook” or “schedule meeting”; execution is up to the app/approver.
       - Metadata: severity, summary, source assistant id.
     - Example:
       ```
       AssistantResult result = AssistantResult.builder()
         .summary("Lexical analysis completed")
         .severity(INFO)
         .annotationAction(AnnotationAction.createIssue(issueText="Unknown word 'datalaek'",
                                                       mustResolve=true,
                                                       annotatable=goalId))
         .annotationAction(AnnotationAction.createPosition(positionText="Add 'data lake' to glossary",
                                                           issueRef=issueId))
         .message(AssistantMessage.info("Candidate glossary term: data lake"))
         .build();
       ```
       The façade applies `AnnotationAction`s through existing command handlers; messages can be rendered in UI logs; `ExternalAction`s can be queued for approval.

2) Integration in the app:
   - `AssistantRegistry` (or reuse `AssistantFacade`) discovers all `Assistant<?>` beans.
   - Dispatch: for a given target entity, pick assistants where `targetType().isInstance(target)`.
   - Apply actions: annotation actions go through the existing `AnnotationCommandFactory`; external actions are handed to pluggable executors.

3) AI/NLP pluggability:
   - Provide an `Analyzers` helper interface (e.g., `TextAnalysisService` with methods like `findGlossaryCandidates(String text)`, `checkSpelling(...)`, `summarize(...)`).
   - Current `LexicalAssistant` becomes an implementation of `Assistant<TextEntity>` that delegates to an injected `TextAnalysisService`.
   - A ChatGPT-based assistant can implement the same `Assistant<TextEntity>` but call OpenAI APIs instead of `NLPProcessorFactory`.

4) Non-annotation assistants:
   - Assistants may return no annotation actions but produce `AssistantMessage` or `ExternalAction` items (e.g., “Found 12 open issues; scheduling a meeting”).
   - Execution of external actions is optional; app can present them for user approval.

5) Backward compatibility:
   - Keep existing assistants but adapt them to the new SPI (wrapping their current annotation writes into `AnnotationAction` results).
   - `AssistantFacade` becomes a thin dispatcher; construction of assistants should be via DI, not direct `new`.

## Minimal incremental steps
1) Create `assistant-api` module with `Assistant<T>`, `AssistantContext`, `AssistantResult`, `AssistantMessage`, `AnnotationAction`, `ExternalAction`.
2) Refactor `AssistantFacade` to resolve assistants from the Spring context by type, not new them.
3) Wrap current assistants to implement the SPI and emit `AnnotationAction` instead of directly invoking command handlers (facade applies the actions).
4) Introduce `TextAnalysisService` interface; adapt `LexicalAssistant` to delegate to it. Provide current NLP-backed impl and leave room for a ChatGPT-backed impl later.
5) (Optional) Add a sample “AIAssistant” that uses the SPI but returns only `AssistantMessage` to demonstrate non-annotation usage.

## Open questions
- Where to host `assistant-api`? Likely a small module above `platform-core` but below `project-jpa`. Requires `User`/locale context; use `platform-identity` `User` (or a `UserRef`) to avoid `user-jpa` dependency.
  -  should it be assistant-api or assistant-spi? the assistant context will need access to annotations, for user we can use the identity User instead of the full user
- How to serialize/display `AssistantResult` in the UI (Echo)? Possibly adapt existing annotation views, and add a simple message list for non-annotation results.
  - for the most part we don't display assistant results, errors get logged, the only way you know an assistant did anything is via the annotations added to the project
  - maybe we should have an assistant-activity table
- Governance of external actions: do we execute automatically or require user approval?
  - currently the only approval is the checkbox when importing, because assistants add issues with positions, the user takes an action via the posistion, but that doesn't account for sending emails, scheduling things or creating data in other systems, adding user approval of individual actions will be tedious, and as there are no external assistants yet, it's not a big concern, but maybe after we migrate the UI we can have an assistant management panel.
- Rate limits/cost for ChatGPT/OpenAI integrations (need config flags and error handling).
  - let's worry about this when we get to it.

---
## Architectural critique / questions
- **Module boundaries:** Confirm whether `assistant-api` can depend only on `platform-core` + a minimal `identity` abstraction; pulling in `user-jpa` would reintroduce cycles. Do we need a `UserRef` type to avoid JPA entities in the SPI?
- **Assistant roles/authorization:** Define an `AssistantRole` concept (akin to `ProjectRole`) in the SPI to drive which assistants can run and what data they can emit; tie this into the security/privacy controls.
- **Discovery/Wiring:** Plan assumes Spring bean discovery. Add a container-agnostic path too (e.g., tiny in-memory registry + `ServiceLoader` hook) so CLI/batch tools can register or load assistants without a Spring context, while Spring apps can still auto-register beans into the same registry.
- **Result application:** Applying `AnnotationAction` via command handlers preserves rules/transactions, but we need idempotency/dup detection to avoid duplicate issues/positions on repeated runs. Ideas: give each action a stable key (e.g., `assistantId + targetId + findingSignature`), persist that on the annotation/issue/position, and have handlers perform create-or-update by key (or at least check same text/severity on same annotatable). If a later run omits a previous key, optionally auto-resolve/remove that annotation.
- **External actions safety:** How will we authorize/confirm `ExternalAction`s (webhooks, calendar updates)? Consider an approval queue and audit trail.
- **Performance/async:** `AssistantFacade` currently spawns threads per analysis. Plan: keep the SPI synchronous (assistants return `AssistantResult`), but have the facade run assistants on an `ExecutorService` and return a `CompletionStage<AssistantResult>` (or small handle) to callers. Callers can `join()` for blocking flows or attach listeners for UI/event use. Keeps threading in one place, no leaking thread mgmt into assistants.
- **AI error handling:** For AI-backed assistants, define behavior on API failures/timeouts (partial results? warnings in `AssistantMessage`? retries?).
- **Testing hooks:** Provide fakes for `TextAnalysisService` and `RequelAssistant` that return deterministic `AssistantResult`s for tests (no real NLP/API calls). Include a `NoopAssistant` that emits nothing so pipelines can run in environments without assistants.
- **Security/privacy:** If assistants call external services with project text, we need opt-in flags, redaction rules, and clear data-handling docs.
- **UI surfacing:** How will Echo render `AssistantMessage` and present approval for `ExternalAction`s? Do we need a standard DTO for the client?
- **Versioning/compatibility:** Keep the SPI stable, but when a breaking change is unavoidable, follow the AWS style: ship a new artifact/namespace (e.g., `assistant-api-v2` package or new Maven coordinates) instead of renaming classes with V1/V2. Use semantic versioning, default methods for additive changes, and a small adapter layer to bridge old assistants where possible. Deprecate before removing to give consumers time to move.
- **Analysis trigger pattern:** Prefer a central dispatcher over `invokeAnalysis()` hooks in commands. Patterns: (a) commands return `AnalysisRequest`s describing changed entities; handler dispatches post-commit; (b) publish domain/application events (`EntityChangedEvent`, `ProjectImportedEvent`) and let the assistant façade listen; (c) handler decorator reads a `ProvidesAnalysisTargets` interface from command results. Avoids reflection and empty stubs, keeps commands focused on state changes.
