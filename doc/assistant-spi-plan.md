# Assistant SPI Plan

## Goals
- Allow third parties (or future internal modules) to plug in new “assistants” that analyze Requel data and optionally emit feedback.
- Keep existing annotation-based assistants working, but make the SPI general enough for non-annotation actions (e.g., schedule meetings, call external services).
- Enable swapping the current NLP-backed `LexicalAssistant` for newer AI backends (e.g., ChatGPT/Codex) without changing the rest of the app.

## Current State
- Assistants live in `project-jpa` (`AbstractAssistant`, `LexicalAssistant`, `GoalAssistant`, etc.).
- Public interface is effectively `TypedAssistant<T>` (get/set entity + analyze). Everything else is concrete.
- `AssistantFacade` constructs `LexicalAssistant` and wires it into other assistants; assistants directly depend on `LexicalAssistant`, `AnnotationCommandFactory`, `NLPProcessorFactory`, etc.
- Feedback path is primarily annotations (issues/notes/positions) created via the annotation command factory.
- `TypedAssistant<T>` is effectively dead code today—nothing references it.
- `AssistantFacade` is invoked from project commands after edits/imports; it manually instantiates a `LexicalAssistant` and the concrete assistants per entity type (goal, story, actor, use case, scenario/step, project).

## Proposed SPI
1) Define a stable assistant contract in a neutral module (e.g., `assistant-api`) replacing the unused `TypedAssistant<T>`:
   - `AssistantContext` (project/domain reference, user performing analysis, resource bundle/locale, optional services map).
   - `RequelAssistant<T>` (name chosen to avoid clashes with other libraries) with methods:
     - `Class<T> targetType()`
     - `void setContext(AssistantContext ctx)`
     - `void setTarget(T target)`
     - `AssistantResult analyze()`
   - `AssistantResult` carrying:
     - Optional list of `AnnotationAction` (create/update/remove annotations/positions) OR a generic `List<AssistantMessage>` for UI/logging.
     - Optional `List<ExternalAction>` (e.g., webhook calls, calendar events).
     - Severity/summary metadata.

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
- Where to host `assistant-api`? Likely a small module above `platform-core` but below `project-jpa`. Requires `User`/locale context; consider a lightweight `UserRef` to avoid pulling `user-jpa`.
- How to serialize/display `AssistantResult` in the UI (Echo)? Possibly adapt existing annotation views, and add a simple message list for non-annotation results.
- Governance of external actions: do we execute automatically or require user approval?
- Rate limits/cost for ChatGPT/OpenAI integrations (need config flags and error handling).
