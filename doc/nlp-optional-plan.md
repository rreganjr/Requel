# NLP Optionality Plan

Goal: allow Requel to start and function (in a degraded but safe mode) when NLP is intentionally disabled, with a clear path to also allowing the NLP implementation module to be physically absent from the classpath.

## Scope

Two related but distinct scopes, with different prerequisites:

1. **Short-term — disabled with `nlp-jpa` still on the classpath.** `requel.nlp.enabled=false` causes the conditional auto-config to skip the real NLP beans; a no-op factory takes their place. This is the scope that ships first and does not require any module-boundary changes.
2. **Longer-term — absent-module startup.** Removing `nlp-jpa` from the classpath also requires extracting `NLPProcessorFactory` (and the small set of value types it returns) into a tiny `nlp-api` (or `nlp-core`) module so the no-op factory in `requel-app` can depend on the interface without pulling `nlp-jpa` back in. The `@ConditionalOnClass` target then becomes the interface in `nlp-api`. This is a prerequisite — without it, removing `nlp-jpa` from the classpath also removes the interface the no-op factory implements. Treat the `nlp-api` extraction as Step 0 if and when this scope becomes a real requirement.

## Current State
- NLP interfaces (e.g., `NLPProcessorFactory`) and implementations live in `nlp-jpa`.
- Callers (assistants, some UI paths, legacy tests) obtain processors via `NLPProcessorFactory`.
- If `nlp-jpa` is absent, Spring context fails because required beans/types are missing.
- After issue #39, NLP is exercised through `AssistantTaskRunner` rather than direct calls from `AssistantFacade`. The runner takes `NLPProcessorFactory` as a constructor dependency, so guarding NLP via conditional auto-config implicitly guards the assistant runner that depends on it.

## Design
1. **Conditional auto-config in `nlp-jpa`**
   - Add `@Configuration` class annotated with:
     - `@ConditionalOnClass(NLPProcessorFactory.class)`
     - `@ConditionalOnProperty(name = "requel.nlp.enabled", havingValue = "true", matchIfMissing = true)`
   - Register `NLPProcessorFactoryImpl` and processor beans under this config.
   - Remove/relax `@Component` on the impl classes if needed; let the config create them.

2. **No-op fallback factory**
   - In a neutral module (e.g., `platform-core` or `requel-app`), provide a `NoOpNlpConfig` with `@ConditionalOnMissingBean(NLPProcessorFactory.class)` that registers a `NoOpNLPProcessorFactory` bean.
   - The no-op factory must return **safe empty values**, not `null`. Concretely:
     - `createNLPText(...)` / `processText(...)` / `appendText(...)` return an empty `NLPText` (zero sentences, zero tokens) rather than `null`.
     - All `NLPProcessor<T>` getters return a processor that produces a type-appropriate empty result for `T`: empty collections for collection-typed processors, the input `NLPText` unchanged for `NLPText`-typed processors (so token/POS/lemma walks degrade to no-ops), `Boolean.FALSE` for `Boolean`, `0` for `Integer`, and `""` for `String`-typed printers.
     - This guarantees callers never need null-guards. The existing `LexicalAssistant` and entity assistants then degrade to "no findings" rather than NPE-ing when NLP is disabled.
   - Once the assistant SPI from `doc/assistant-spi-plan.md` lands, this no-op contract is also what backs the SPI's "explicit empty `AssistantResult`" rule for disabled assistants.

     ```java
     @Configuration
     @ConditionalOnMissingBean(NLPProcessorFactory.class)
     public class NoOpNlpConfig {
         @Bean
         public NLPProcessorFactory noOpNlpFactory() {
             return new NoOpNLPProcessorFactory();
         }
     }
     ```

3. **Guard assistants/UI wiring**
   - Wrap assistant auto-config in `@ConditionalOnBean(NLPProcessorFactory.class)` and/or check `requel.nlp.enabled`.
   - Because `AssistantTaskRunner` (post issue #39) constructor-injects `NLPProcessorFactory`, guarding the factory via conditional config is sufficient — the no-op factory satisfies the dependency.
   - If assistants are invoked when NLP is disabled, return an explicit empty result and record the `AssistantRun` with `status = SKIPPED` and an `AssistantMessage` noting that NLP is disabled, rather than throwing.

4. **Config flag**
   - Add `requel.nlp.enabled=true` (default) in `application.yml`.
   - Tests that do not need NLP can set `requel.nlp.enabled=false` and rely on the no-op factory.

5. **Failure modes**
   - NLP module absent + `requel.nlp.enabled=false` → app starts with no-op processors.
   - NLP module absent + `requel.nlp.enabled=true` → beans are skipped by `@ConditionalOnClass`; surface a clear startup error indicating NLP module missing.

6. **Module placement note**
   - Today `NLPProcessorFactory` lives in `nlp-jpa`. Disabling NLP with the module still on the classpath (Scope 1) works against the interface as it is.
   - For Scope 2 (absent-module startup), extract `NLPProcessorFactory` and the small set of value types it returns into a tiny `nlp-api` (or `nlp-core`) module. The optional config stays the same but `@ConditionalOnClass` would target the interface in `nlp-api`. Assistants and the no-op factory then depend only on `nlp-api`. This extraction is a prerequisite, not a later nicety, if `nlp-jpa` is to be removable from the classpath.

## Rollout Steps
1) Add the conditional config class in `nlp-jpa`; remove direct `@Component` if necessary.
2) Add the no-op config in `requel-app` (or a shared module) with `@ConditionalOnMissingBean`, returning a `NoOpNLPProcessorFactory` that produces safe empty values everywhere (no nulls).
3) Gate assistant beans with `@ConditionalOnBean(NLPProcessorFactory.class)` or property checks. `AssistantTaskRunner`'s `NLPProcessorFactory` dependency is satisfied by the no-op factory when NLP is disabled.
4) Add a smoke test profile with `requel.nlp.enabled=false` to ensure the app boots and that assistant runs invoked under it persist `AssistantRun` rows with `status = SKIPPED` and an NLP-disabled `AssistantMessage`.
5) (Only if Scope 2 becomes a requirement) Extract `NLPProcessorFactory` and its return types into `nlp-api`; repoint `@ConditionalOnClass` and the no-op factory at the new interface; verify the app starts with `nlp-jpa` removed from the classpath.

## Resolved Decisions

Resolved during issue #43 walkthrough. See the full record at <https://github.com/rreganjr/Requel/issues/43#issuecomment-4560006774>.

- **Default flag value.** `requel.nlp.enabled=true` remains the default (`matchIfMissing=true`). Lean builds opt out explicitly; existing deployments behave unchanged on upgrade.
- **Coexistence with AI assistants.** Legacy NLP is not deprecated when AI-backed assistants land. Both run in parallel; legacy NLP stays enabled by default. The flag exists for opt-out, not as a planned default flip.
- **Initial scope.** Phase 1 of this plan delivers Scope 1 (in-module disable via the property flag). Scope 2 (absent-module startup) is contingent on a later `nlp-api` extraction and is not pursued as part of issue #43.
- **No-op behavior.** Safe empty values, not `null`. The no-op factory is the single source of truth for the "disabled" contract that the assistant SPI's no-op assistants also rely on.

