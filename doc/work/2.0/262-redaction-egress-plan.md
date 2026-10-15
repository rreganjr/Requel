# #262 Redaction and data-handling flags — implementation plan

Issue: https://github.com/rreganjr/Requel/issues/262 (child 4 of epic #258, second in its build order)
Branch: `262-redaction-egress`, cut from `release/2.0` @ `083618d8`
Builds on: #259 (the `cli` provider, run FAILED on provider failure), #268 (the per-project settings store)

## Summary

A deterministic default `RedactionPolicy` masks credentials, emails, phones, US SSNs and payment
cards in every text *and name* field of a context pack. Usernames are replaced by roles. Each category,
and whether the project may use a remote provider at all, is a per-project switch in #268's store,
with toggles on the project overview page. Every AI request carries real `dataHandlingFlags`, and every
remote client refuses a request whose flags don't allow it. The run records what was masked.

## Review against the tree (`release/2.0` @ `083618d8`)

1. **Redaction already runs before truncation** (`ContextPackTextUtils.prepareText`); the AC only
   needs a test.
2. **Names bypass the policy.** Only `*.text` fields are redacted; project, goal, story, actor, use
   case, step and glossary-term names are sent as-is.
3. **Usernames are sent.** `ContextPackTextUtils.username` puts annotation authors and the project
   creator into the pack.
4. **Settings home exists.** `project_assistant_settings` (#268) is a per-project on/off per string key.
   `disabledAssistants` returns every key switched off, so the new keys need their own read.
5. **The run has no redaction fields.** A V29 migration is needed.
6. **`RedactionPolicy` has no project context.** Per-project categories need one; the builders know
   the target's project.
7. **`cli` is remote** (#259). `openai-compat` can be either (Ollama vs Gemini).
8. **Doc path** is `doc/guides/AI_ASSISTANT_SETUP.md`.

## Locked decisions (2026-09-30, signed off 2026-09-30)

- **Egress default: allowed.** No row for `egress.external` = allowed, as today. Remote requests are
  always redacted.
- **Usernames → roles.** `assistant` for the assistant user; everyone else `user-1`, `user-2`, …
  numbered by first appearance, stable within one pack.
- **Toggles on the overview page**, beside #268's assistant toggles, with the command and read API
  behind them.
- **Categories:** `credentials`, `email`, `phone`, `ssn` (US only), `card` (Luhn). All on by default.
- **Names are redacted too** (review item 2), folded in.
- **Remote classification:** `cli`, `openai`, `anthropic` are remote. `openai-compat` is remote
  unless its base-url host is `localhost`, `127.*`, `::1` or `host.docker.internal`. `noop` is local.
- **Fail closed:** a remote client refuses when `externalProviderAllowed` is false *or missing*.
- **Mask format:** `[REDACTED:<CATEGORY>]`.
- **Not in project XML**, like #268's settings.

## Contracts

### Setting keys (`DataHandlingSettings`, project-domain)

| Key | Meaning when on (no row = on) |
|---|---|
| `egress.external` | the project may use a remote AI provider |
| `redaction.credentials` | mask credential shapes |
| `redaction.email` | mask email addresses |
| `redaction.phone` | mask phone numbers |
| `redaction.ssn` | mask US SSNs |
| `redaction.card` | mask Luhn-valid card numbers |

`ProjectAssistantSettingsStore` gains `Map<String, Boolean> settings(Long projectId)` (every stored
row). `disabledAssistants` is unchanged. `DataHandlingSettings.of(projectId, store)` resolves the six
keys with defaults.

### Command and read

- `EditProjectDataHandlingSetting { projectName, key, enabled }`: the key must be one of the six
  (anything else is a 400 with the list), and it requires `Project[Edit]`, like
  `EditProjectAssistantSetting`. It goes on the gateway allowlist with a record input DTO (#296).
- `GET /api/projects/{name}/data-handling` → `{ externalProviderAllowed, redaction: { credentials:
  true, … } }`, requiring project access like `/assistants`.

### Redaction SPI (assistant-core)

```java
public interface RedactionPolicy {
    String redact(String fieldPath, String value, List<String> notes);          // unchanged
    /** The policy to use for one project's pack; the default ignores the project. */
    default RedactionPolicy forProject(Long projectId) { return this; }
}
```

- `DefaultRedactionPolicy` (`@Component`, which displaces `NoOpRedactionPolicy` via the existing
  `@ConditionalOnMissingBean`):
  - `forProject` returns a view restricted to the project's enabled categories;
  - `redact` applies each enabled category's patterns in a fixed order (credentials first, so a key
    inside a URL is masked whole);
  - it appends one note per field, `goal.name: EMAIL x1`.
- The builders call `redactionPolicy.forProject(projectId)` once per pack. They also send names through
  the policy as `<type>.name` / `<type>[id].name`, and `ContextPackTextUtils.username` becomes a
  per-pack `AuthorPseudonyms`.
- `ContextPackMetadata` gains `redactionCount` and `redactionCategories` (derived from the notes),
  with existing constructors kept.

### Flags and guard (assistant-ai)

- `AiProviderLocality` (bean): `LOCAL` | `REMOTE` from `requel.ai.provider` and
  `spring.ai.openai.base-url`.
- `RequirementsReviewAssistant` builds `dataHandlingFlags` = `{ externalProviderAllowed,
  providerLocality, provider, redactionCategories }` from the project's settings.
- `DataHandlingGuard.requireAllowed(request, locality)`: for `REMOTE`, a false or missing
  `externalProviderAllowed` → `AiAnalysisException("project <id> does not allow sending text to an
  external AI provider (setting egress.external)")`. It is the first statement in
  `SpringAiAnalysisClient.analyze` and `CliAiAnalysisClient.analyze`. `NoopAiAnalysisClient` doesn't
  call it.

### Run record — V29

```sql
ALTER TABLE assistant_runs ADD COLUMN redaction_count INT NOT NULL DEFAULT 0;
ALTER TABLE assistant_runs ADD COLUMN redaction_categories VARCHAR(200) NULL;
```

`AssistantRunStore.recordRedactions(runId, count, categories)` (best effort, like usage) is called by
`RequirementsReviewAssistant` after building the pack, before the provider call, so a refused run
still shows what would have been masked.

### UI

The project-assistants panel on the overview page gets a *Data handling* section: an **External AI
providers** toggle and one toggle per redaction category. It calls the new command and re-reads.

## Step by step

1. `DataHandlingSettings` keys, the store read, the command, the read endpoint and the gateway allowlist.
2. `DefaultRedactionPolicy` with per-category detectors, and `forProject`.
3. Builders: names through the policy, `AuthorPseudonyms`, `forProject`, metadata counts.
4. V29, `recordRedactions` on both run stores, and `AssistantRunEntity` fields.
5. `AiProviderLocality`, flags in `RequirementsReviewAssistant`, `DataHandlingGuard` in both remote
   clients.
6. Angular: the data-handling section in `project-assistants-panel`, the service call, and a spec.
7. Doc section; `tmp/262-verify.sh` (`mvn clean verify` + the Angular unit specs touched + typecheck).

## Test plan

- **`DefaultRedactionPolicyTest`** (table-driven):
  - each category's positives (several shapes each) and near-misses: a version string, an invoice
    number, a Luhn-failing 16-digit number, `000-12-3456`;
  - masking keeps the surrounding sentence;
  - a key inside a URL is masked once;
  - `forProject` with a category off leaves it alone.
- **Builder tests** (existing three stay on `NoOpRedactionPolicy`), plus new cases with the default
  policy:
  - names are redacted and noted;
  - usernames become `assistant` / `user-N`, stable across two annotations by the same author;
  - redaction before truncation: a 4,000-character field whose email straddles the cap comes out masked,
    never half an address.
- **`CliAiAnalysisClientTest` / `SpringAiAnalysisClientTest`**: refused when
  `externalProviderAllowed` is false or missing, with the runner / ChatClient never invoked; allowed
  when true.
- **`AiProviderLocalityTest`**: each provider and base-url case.
- **`RequirementsReviewAssistantTest`**: the flags are populated from settings; `recordRedactions` is
  called with the pack's counts.
- **`EditProjectDataHandlingSettingIT`**: an unknown key is refused; permission is required; the read
  reflects writes.
- **`RedactionEgressIT`** (requel-app, the `cli` provider with the fake script from #259, which
  captures stdin to a file):
  - a goal with a key and an email in its text and an email in its name → stdin has three
    placeholders and none of the raw values;
  - no username in stdin;
  - the run has `redaction_count = 3` and the categories;
  - with `egress.external` off → the run is FAILED with the egress message and the fake CLI never ran
    (no stdin file).
- **Angular:** a panel spec for the toggles.

## Out of scope

AI-based PII detection (#265), redacting stored data, org-wide policy, project XML carriage, and
non-US national IDs.

## Risks

- **False positives mask requirement text** (e.g. a version or model number shaped like a phone).
  Mitigation: conservative patterns, near-miss tests, and per-category switches.
- **Pseudonymising usernames hides who raised what** from the model. Acceptable; roles keep the
  structure.
- **`forProject` needs a settings read per pack.** One query, cheap next to a model call.

## AC mapping

| AC | Where |
|---|---|
| Key, email in text and email in name masked at the client boundary | `RedactionEgressIT` |
| No username reaches the provider | `RedactionEgressIT`, builder tests |
| `redactedFields` names every path; run records count and categories | builder tests, `RedactionEgressIT` |
| `egress.external` off → fails closed, no request; local still runs | `RedactionEgressIT`, client tests, `AiProviderLocalityTest` |
| Missing flag refused by a remote client | client tests |
| Flags non-empty and reflect the provider | `RequirementsReviewAssistantTest` |
| Category switched off is not masked | `DefaultRedactionPolicyTest`, `RedactionEgressIT` |
| Redaction before size capping | builder test |
| No context-pack regressions; NoOp still available | existing builder tests |

## Implementation notes (2026-09-30)

- **No new store read.** The store's existing `disabledAssistants` already returns every key that is
  switched off. Because no row means on, that set alone determines the settings
  (`DataHandlingSettings.fromDisabledKeys`); the `settings()` method in the plan wasn't needed.
- **`NoOpRedactionPolicy` is no longer a component.** It was `@ConditionalOnMissingBean`, which a
  component scan doesn't order reliably against another component, so `DefaultRedactionPolicy` is
  the only bean. `NoOpRedactionPolicy` stays for tests.
- **Counts come from the notes.** `ContextPackMetadata` derives `redactionCount()` and
  `redactionCategories()` from the policy's notes (`goal.text: EMAIL x1`), so the record's shape
  and constructors are unchanged.
- **Locality is a bean.** `AiProviderLocality` is created in `AiConfiguration` from the Environment
  (#293). The Spring AI client gets it through `setLocality`; without it the provider name alone
  decides, which is conservative for `openai-compat`. The CLI client is always remote.
- **All three builders call `forProject`.** Project and Issue packs don't reach a model yet, but
  they get the same name and author treatment so #266 starts from it.
- Verified in a sandbox build: unit tests in project-jpa, assistant-core and assistant-ai;
  `RedactionEgressIT`, `CliProviderReviewIT`, `AuthorizationIT`, `ProjectAssistantSettingsIT`,
  `AiReviewDispatchIT`; the panel spec (13 tests) and both typechecks on the device. The full
  gate is `tmp/262-verify.sh`.
