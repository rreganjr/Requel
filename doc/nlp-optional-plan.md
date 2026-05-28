# NLP Optionality Plan

Goal: allow Requel to start and function (in a degraded but safe mode) when the NLP implementation module is absent or intentionally disabled.

## Current State
- NLP interfaces (e.g., `NLPProcessorFactory`) and implementations live in `nlp-jpa`.
- Callers (assistants, some UI paths, legacy tests) obtain processors via `NLPProcessorFactory`.
- If `nlp-jpa` is absent, Spring context fails because required beans/types are missing.

## Design
1. **Conditional auto-config in `nlp-jpa`**
   - Add `@Configuration` class annotated with:
     - `@ConditionalOnClass(NLPProcessorFactory.class)`
     - `@ConditionalOnProperty(name = "requel.nlp.enabled", havingValue = "true", matchIfMissing = true)`
   - Register `NLPProcessorFactoryImpl` and processor beans under this config.
   - Remove/relax `@Component` on the impl classes if needed; let the config create them.

2. **No-op fallback factory**
   - In a neutral module (e.g., `platform-core` or `requel-app`), provide:
     ```java
     @Configuration
     @ConditionalOnMissingBean(NLPProcessorFactory.class)
     public class NoOpNlpConfig {
         @Bean
         public NLPProcessorFactory noOpNlpFactory() {
             return new NLPProcessorFactory() {
                 private <T> NLPProcessor<T> noop() {
                     return input -> input; // or return null for non-text outputs
                 }
                 public NLPText createNLPText(String text) { return null; }
                 public NLPText processText(String text) { return null; }
                 public NLPText appendText(NLPText... t) { return null; }
                 public NLPText appendText(List<NLPText> t) { return null; }
                 public NLPProcessor<String> getConstituentTreePrinter() { return noop(); }
                 public NLPProcessor<String> getDependencyPrinter() { return noop(); }
                 public NLPProcessor<String> getSemanticRolePrinter() { return noop(); }
                 public NLPProcessor<Integer> getConstituentTreeDepthFinder() { return noop(); }
                 public NLPProcessor<NLPText> getSentencizer() { return noop(); }
                 public NLPProcessor<NLPText> getTokenizer() { return noop(); }
                 public NLPProcessor<NLPText> getPosTagger() { return noop(); }
                 public NLPProcessor<NLPText> getParser() { return noop(); }
                 public NLPProcessor<NLPText> getLemmatizer() { return noop(); }
                 public NLPProcessor<NLPText> getSemanticRoleLabeler() { return noop(); }
                 public NLPProcessor<Collection<NLPText>> getNounPhraseFinder() { return noop(); }
                 public NLPProcessor<NLPText> getNamedEntityResolver() { return noop(); }
                 public NLPProcessor<NLPText> getPrimaryVerbFinder() { return noop(); }
                 public NLPProcessor<NLPText> getDictionizer() { return noop(); }
                 public NLPProcessor<Collection<NLPText>> getSimilarWordFinder() { return noop(); }
                 public NLPProcessor<Collection<NLPText>> getMoreSpecificWordSuggester() { return noop(); }
                 public NLPProcessor<Boolean> getSpellingChecker() { return noop(); }
                 public NLPProcessor<NLPText> getWordSenseDisambiguator() { return noop(); }
             };
         }
     }
     ```
   - This keeps injections satisfied; callers should handle null/identity returns gracefully.

3. **Guard assistants/UI wiring**
   - Wrap assistant auto-config in `@ConditionalOnBean(NLPProcessorFactory.class)` and/or check `requel.nlp.enabled`.
   - If assistants are invoked when NLP is disabled, return a user-friendly “NLP unavailable” response rather than throwing.

4. **Config flag**
   - Add `requel.nlp.enabled=true` (default) in `application.yml`.
   - Tests that do not need NLP can set `requel.nlp.enabled=false` and rely on the no-op factory.

5. **Failure modes**
   - NLP module absent + `requel.nlp.enabled=false` → app starts with no-op processors.
   - NLP module absent + `requel.nlp.enabled=true` → beans are skipped by `@ConditionalOnClass`; surface a clear startup error indicating NLP module missing.

6. **Module placement note**
   - Today `NLPProcessorFactory` lives in `nlp-jpa`. If we later split it into a tiny `nlp-core` API, the optional config stays the same but `@ConditionalOnClass` would target the interface in `nlp-core`. Assistants live in `requel-app`/`project-analysis`; they should depend only on the interface.

## Rollout Steps
1) Add the conditional config class in `nlp-jpa`; remove direct `@Component` if necessary.
2) Add the no-op config in `requel-app` (or a shared module) with `@ConditionalOnMissingBean`.
3) Gate assistant beans with `@ConditionalOnBean(NLPProcessorFactory.class)` or property checks.
4) Add a smoke test profile with `requel.nlp.enabled=false` to ensure the app boots and presents a graceful “NLP disabled” behavior.

## Resolved Decisions

Resolved during issue #43 walkthrough. See `43-comment.md` for the full record.

- **Default flag value.** `requel.nlp.enabled=true` remains the default (`matchIfMissing=true`). Lean builds opt out explicitly; existing deployments behave unchanged on upgrade.
- **Coexistence with AI assistants.** Legacy NLP is not deprecated when AI-backed assistants land. Both run in parallel; legacy NLP stays enabled by default. The flag exists for opt-out, not as a planned default flip.

