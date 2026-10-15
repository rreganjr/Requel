# #259 Dev-only `cli` AI provider — implementation plan

Issue: https://github.com/rreganjr/Requel/issues/259 (child 1 of epic #258)
Branch: `259-cli-ai-provider`, cut from `release/2.0` @ `4eaded1b`
Next in the epic: #262 (redaction), which classes this client as remote

## Summary

A `CliAiAnalysisClient` drives a locally authenticated `claude` or `codex` CLI as the AI
provider, for development only. The prompt goes on stdin, the command is an argv list, and the
child runs with tools off, no MCP servers, an allowlisted environment and an empty temp directory.
It runs under a timeout and a byte cap. Validation and mapping of the reply move into one public
`ReviewResultMapper` that both this client and `SpringAiAnalysisClient` use. Provider failures stop
being swallowed: a review whose only assistant failed is recorded as `FAILED`, for every provider.

## Review against the tree (`release/2.0` @ `4eaded1b`)

1. **Failures are swallowed.** `RequirementsReviewAssistant.analyze` catches `AiAnalysisException`
   and returns a result, so `AssistantRunWorker` marks the run `SUCCEEDED`. This holds for every
   provider, so the ticket's "records the failure on the run" needs a fix outside the new client.
2. **Package-private can't span subpackages.** The clients live in `.spring` and `.cli`, so the
   mapper in `assistant.ai` has to be `public`.
3. **The doc moved** to `doc/guides/AI_ASSISTANT_SETUP.md`.
4. **`POST /api/ai/reviews` returns 202** and runs asynchronously. The findings show up as
   annotations written by the run.
5. **The Spring AI prompt omits `outputSchema`,** because the converter adds it. The CLI client
   appends the schema itself.
6. **User-level CLI config leaks in.** Even in an empty directory the CLI loads the developer's MCP
   servers, which may include Requel's own `/api/mcp`.
7. **`dataHandlingFlags` is always `Map.of()`** today. Redaction is #262. Until it lands the client
   sends project text unredacted, so use it only on test projects.

The issue body was updated on 2026-09-30 with items 1–6 and the decisions below.

## Locked decisions (2026-09-30, signed off 2026-09-30)

- **Failure status:** the worker marks a run `FAILED` when every matching assistant threw, and
  `PARTIAL` when only some did (unchanged). `RequirementsReviewAssistant` rethrows
  `AiAnalysisException` as `AssistantException`.
- **Codex:** two output modes, `claude-json` and `raw`. Both CLIs are checked end to end by hand.
- **Environment:** allowlist `PATH`, `HOME`, `USER`, `LANG`, `TMPDIR`, plus names listed in
  `requel.ai.cli.env`.
- **Structured output:** the schema is appended to the prompt for both CLIs. A native flag
  (`claude --json-schema`, `codex exec --output-schema`) is optional and goes through `args`.
- **Misconfiguration fails at boot.** `provider=cli` with a blank, relative or non-executable
  `command` fails the context, the same rule #288 used for configuration that was asked for.
- **Usage on failure** is not recorded, which is unchanged from the Spring AI path.

## Contracts

### `ReviewResultMapper` (`com.rreganjr.requel.assistant.ai`, public, final)

```java
public final class ReviewResultMapper {
    public record ReviewResult(String summary, List<Finding> findings, List<String> warnings) {
        public record Finding(String findingType, String severity, Double confidence,
            List<String> evidenceReferences, String suggestedIssueText,
            String suggestedNoteText, List<String> suggestedPositions) {}
    }
    public ReviewResultMapper(ObjectMapper objectMapper) { … }
    /** Parse a JSON reply (fence-stripped) into a ReviewResult; malformed → AiAnalysisException. */
    public ReviewResult parse(String json) throws AiAnalysisException;
    public static void validate(ReviewResult r) throws AiAnalysisException;   // moved as-is
    public AiAnalysisResponse toResponse(ReviewResult r, AiUsage usage, Map<String, Object> metadata);
    static String stripCodeFence(String reply);
}
```

`SpringAiAnalysisClient.ReviewResult` becomes the mapper's record. Spring AI's
`responseEntity(...)` binds to it the same way. The client keeps only `usage(ChatResponse, …)`
and `providerMetadata(ChatResponse)`, which are Spring AI types, and passes their output in.
Validation messages drop the "Spring AI" prefix, e.g. "structured output missing summary".

### `CliProcessRunner` (the process seam; tests fake it)

```java
interface CliProcessRunner {
    CliProcessResult run(CliInvocation invocation) throws IOException, InterruptedException;
}
record CliInvocation(List<String> argv, Map<String, String> environment, Path workingDirectory,
                     byte[] stdin, Duration timeout, int maxOutputBytes, int maxErrorBytes) {}
record CliProcessResult(int exitCode, byte[] stdout, boolean stdoutTruncated,
                        String stderr, boolean timedOut, Duration elapsed) {}
```

`ProcessBuilderCliProcessRunner` (the default implementation):

- `new ProcessBuilder(argv)`, `environment().clear()` then `putAll(invocation.environment())`,
  `directory(workingDirectory)`, no `redirectErrorStream`.
- Three daemon threads (Java 17, so no virtual threads): one writes stdin and closes it, and one each drains stdout and stderr up
  to their cap and then **keeps reading and discarding**, so the child never blocks on a full pipe.
- `waitFor(timeout)`. On expiry it calls `destroyForcibly()` on the process and its descendants
  (`toHandle().descendants()`) and returns `timedOut=true`.

### `CliAiAnalysisClient` (`com.rreganjr.requel.assistant.ai.cli`)

`analyze(request)`:

1. Build the prompt: the Spring AI system text (task instructions, task type, locale), then the
   same JSON body as `SpringAiAnalysisClient.prompt(...)`, then
   `"Reply with a single JSON object matching this JSON Schema and nothing else:\n" + outputSchema`.
   The prompt builder moves to a shared package-level helper so the two prompts can't drift.
2. Create a temp dir (`Files.createTempDirectory("requel-ai-cli-")`), run, and delete it in
   `finally`.
3. Map the result: timed out, non-zero exit, stdout truncated or empty stdout each throw
   `AiAnalysisException`. The message names the cause, the exit code and the first 2 KB of stderr,
   and never includes the prompt.
4. Decode by `output-format`:
   - `claude-json`: parse the envelope. `is_error=true` or a missing `result` throws. The reply
     string is `result`. `usage.input_tokens`, `usage.output_tokens`,
     `usage.cache_read_input_tokens` and `total_cost_usd` feed `AiUsage`.
   - `raw`: the reply is stdout decoded as UTF-8. Usage is null apart from latency.
5. `mapper.parse(reply)`, then `validate`, then `toResponse` with `AiUsage(provider, model, …)`
   from `AiProperties`. The metadata carries `provider`, `model`, `outputFormat`, `exitCode`.

### Configuration — `requel.ai.cli.*` (`CliAiProperties`)

| Property | Default | Notes |
|---|---|---|
| `command` | *(none)* | Absolute path; validated at boot when `provider=cli` |
| `args` | `[]` | argv after the command; the profile supplies the `claude` set |
| `output-format` | `claude-json` | `claude-json` \| `raw` |
| `env` | `[]` | Extra environment variable names passed through when set |
| `timeout` | `180s` | |
| `max-output-bytes` | `1048576` | stdout cap; over it → rejected |
| `max-error-bytes` | `65536` | stderr capture cap |

`CliAiClientConfiguration` registers the client under
`@ConditionalOnProperty(prefix="requel.ai", name="provider", havingValue="cli")`, so it is
mutually exclusive with noop and Spring AI by construction.

### Profile — `application-ai-cli.properties`

```properties
requel.ai.enabled=true
requel.ai.provider=cli
requel.ai.model=${REQUEL_AI_MODEL:claude-cli}
spring.ai.model.chat=none
requel.ai.cli.command=${REQUEL_AI_CLI_COMMAND:}
requel.ai.cli.output-format=claude-json
# claude: print mode, JSON envelope, no tools, no MCP servers, one turn.
requel.ai.cli.args=-p,--output-format,json,--tools,,--strict-mcp-config,--max-turns,1
# codex (set these instead):
# requel.ai.cli.output-format=raw
# requel.ai.cli.args=exec,--sandbox,read-only,--skip-git-repo-check,-
```

The flag spellings are verified against the installed `claude` and `codex` on the developer's Mac
at step 6 and corrected here if they differ. A Spring list with an empty element (`--tools ""`) has
to survive binding. If it doesn't, the empty value moves into a `--tools=` form.

### Worker (`AssistantRunWorker`)

`Analysis` gains `thrownCount`. After phase 1, if `thrownCount == assistants.size()` (every
matching assistant threw), the run is `runStore.markFailed(runId, new
AssistantWorkerException(String.join("; ", problems)))` and phase 2 is skipped: there is nothing
to apply. It returns without rethrowing, because the assistant already logged a WARN. An
*incomplete* result (`isIncomplete`) still counts as `PARTIAL`, not failed.

This also moves a post-edit run whose lexical assistants all threw from `PARTIAL` to `FAILED`,
which is the intended reading.

## Step by step

1. **Mapper.** Add `ReviewResultMapper` and move `ReviewResult`, `validate`, `findings`, `messages`,
   plus the prompt helper. `SpringAiAnalysisClient` delegates. Move the validate/toResponse cases
   to `ReviewResultMapperTest`. Spring AI tests green.
2. **Failure path.** `RequirementsReviewAssistant` rethrows as `AssistantException`, and the worker
   gets the all-failed rule. Add the tests below.
3. **Process runner.** `CliProcessRunner`, the records, and `ProcessBuilderCliProcessRunner` with
   its tests.
4. **Client and config.** `CliAiAnalysisClient`, `CliAiProperties`, `CliAiClientConfiguration` with
   boot validation, and `application-ai-cli.properties`.
5. **ITs in requel-app** (fake CLI script, see the test plan) and the shipped-properties guard.
6. **By-hand check** on the Mac with the real `claude`, then `codex`: correct the args in the
   profile and record the versions in the doc.
7. **Doc.** A development-only section in `doc/guides/AI_ASSISTANT_SETUP.md`: what it is, the
   setup, both invocations, the warnings (not a deployment provider, not in Docker, not for other
   users, unredacted until #262), and troubleshooting (auth, timeouts, flags).
8. **`tmp/259-verify.sh`**: `mvn clean verify` (only `modules/**` changes; no Angular).

## Test plan

**assistant-ai (unit)**

- `ReviewResultMapperTest`: the moved validate/toResponse cases, `parse` of valid, fenced
  (```` ```json ````) and malformed JSON, and a missing summary.
- `SpringAiAnalysisClientTest`: the remaining cases, still green, with the Spring AI types only.
- `CliAiAnalysisClientTest` over a capturing fake runner:
  - argv is exactly `command + args`, and no element contains prompt text;
  - a context pack holding `$(touch /tmp/pwned)`, backticks, `'"; | & > <`, `\n` and unicode
    arrives byte-for-byte in stdin;
  - the environment is exactly the allowlist plus configured extras present in the parent, and
    unset extras are absent;
  - the working directory is a fresh empty temp dir, gone afterwards, success or failure;
  - `claude-json`: result parsed and usage/cost mapped; `is_error=true` → exception;
  - `raw`: stdout parsed and usage null;
  - non-zero exit, timeout, truncated stdout, empty stdout, non-JSON and schema-invalid each →
    `AiAnalysisException` with no prompt text in the message.
- `ProcessBuilderCliProcessRunnerTest` (`@EnabledOnOs({LINUX, MAC})`, OS binaries only, never a
  CLI): `/bin/cat` round-trips 1 MB of stdin; `/bin/sh -c 'exit 3'` → exit 3; `sleep 30` with a
  200 ms timeout → `timedOut` and the process gone; 5 MB of stdout with a 1 KB cap → truncated and
  no hang; 5 MB of stderr with small stdout → no hang.
- `CliAiClientConfigurationTest` (`ApplicationContextRunner`): `provider=cli` → the CLI client is
  the only `AiAnalysisClient`; a missing provider → noop only; `cli` with a blank, relative or
  non-executable command → the context fails with a message naming `requel.ai.cli.command`.
- `RequirementsReviewAssistantTest`: the client throws → `analyze` throws `AssistantException`, and
  no usage row.

**assistant-core (unit)**

- `AssistantRunWorkerTest`: the only assistant throws → `FAILED`, applicator not called; one of
  two throws → `PARTIAL` (existing behaviour); an incomplete result → `PARTIAL`.

**requel-app (IT)**

- `ShippedAiPropertiesTest`: the classpath `application.properties` leaves `requel.ai.provider`
  unset or `noop`, never `cli`.
- `CliProviderReviewIT` (`@ActiveProfiles({"test","ai-cli"})`, Linux/Mac only): at class setup
  write an executable fake-CLI script to a temp dir, and point `requel.ai.cli.command` at it with
  `@DynamicPropertySource`. Mind #293: `CliAiProperties` binds through `@ConfigurationProperties`,
  which sees dynamic properties, not `@Value`.
  - A script printing a canned `claude-json` envelope with one finding → `POST /api/ai/reviews` for
    a Goal returns 202, the run reaches `SUCCEEDED`, one issue annotation is on the Goal, and one
    `AssistantUsage` row has the tokens and cost.
  - A script that exits 2 → the run is `FAILED` with the error kind recorded, no annotations, no
    usage row.
- Existing `SpringAiProviderContextLoadsTest` / `SpringAiAnthropicProviderContextLoadsTest` green.

**By hand (step 6), on a throwaway project, not roundtable:**

```bash
java -jar modules/requel-app/target/requel-app-2.0.0-dev.jar \
  --spring.profiles.active=dev,ai-cli --server.port=8080 \
  --requel.ai.cli.command="$(which claude)" \
  '--spring.datasource.url=jdbc:mysql://127.0.0.1:3306/requel?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC' \
  --spring.datasource.username=root --spring.datasource.password=password
```

Request a review of one Goal with `POST /api/ai/reviews` (the UI has no review button), then
check the annotations plus the run and usage rows. Then repeat with
`codex`, using the profile's codex settings.

## Out of scope

- Any deployment, Docker or CI use of this provider.
- Streaming, multi-turn, or tool-using CLI sessions.
- Redaction and `dataHandlingFlags` (#262).
- Per-definition prompts or per-assistant identities (#260).
- Recording usage for failed calls.

## Risks

- **CLI flags drift between releases.** The args are configuration, and step 6 pins the verified
  set and versions in the doc.
- **The restricted environment breaks auth.** `claude` on macOS reads the keychain, which needs
  `HOME`/`USER`, and codex reads `~/.codex`. Either can need an extra variable, which goes in
  `requel.ai.cli.env`. This is caught at step 6.
- **A slow review holds an executor thread** for up to `timeout`. Acceptable for a dev-only
  provider.
- **The FAILED rule changes post-edit runs** where every lexical assistant throws. That case is
  rare and was already an error, and `FAILED` is the more accurate status.
- **Empty list element binding** (`--tools ""`). See the profile note.

## AC mapping

| AC | Where |
|---|---|
| `dev,ai-cli` review writes findings, run and usage rows (claude and codex) | `CliProviderReviewIT` (fake CLI) + step 6 by hand |
| noop is the only client with no provider; shipped properties don't select `cli` | `CliAiClientConfigurationTest`, `ShippedAiPropertiesTest` |
| Metacharacters round-trip into stdin and execute nothing | `CliAiAnalysisClientTest` |
| Only allowlisted environment reaches the child | `CliAiAnalysisClientTest`, `ProcessBuilderCliProcessRunnerTest` |
| Non-zero, timeout, non-JSON → exception, no annotations, run `FAILED` | `CliAiAnalysisClientTest`, `AssistantRunWorkerTest`, `CliProviderReviewIT` |
| Over `maxOutputBytes` truncated and rejected | `ProcessBuilderCliProcessRunnerTest`, `CliAiAnalysisClientTest` |
| `ReviewResultMapper` is the only validation/mapping, Spring AI tests green | `ReviewResultMapperTest`, `SpringAiAnalysisClientTest` |
| `mvn verify` green with the client unselected, no CLI binary needed | `tmp/259-verify.sh` |

## Implementation notes (2026-09-30)

- The shared prompt helper is `AiPromptBuilder` (public, `assistant.ai`). `DEFAULT_GUIDANCE` moved
  there from `SpringAiAnalysisClient`.
- `SpringAiAnalysisClientTest` keeps its two `toResponse` cases (they cover the client's own
  usage/metadata half). The three validate cases moved to `ReviewResultMapperTest`.
- **Empty list element:** the indexed form `requel.ai.cli.args[4]=` binds to `""`, which
  `CliAiClientConfigurationTest` asserts. The profile uses indexed args throughout.
- **codex:** the profile ships the claude args. codex is selected on the command line, because an
  indexed list given in one property source replaces the profile's list entirely (see the doc).
- `CliProviderReviewIT` uses `@TestPropertySource`, not `@ActiveProfiles("ai-cli")`. The base
  class pins `requel.ai.provider=noop` in a `@TestPropertySource`, which outranks a profile file.
  `ShippedAiPropertiesTest` covers the profile file's content instead.
- Verified in a sandbox build (JDK 17 toolchain): assistant-core and assistant-ai unit tests,
  plus `ShippedAiPropertiesTest`, `CliProviderReviewIT`, `AiReviewDispatchIT` and
  `SpringAiProviderContextLoadsTest`. The full `mvn clean verify` is `tmp/259-verify.sh`.
- **Step 6, claude (2.1.198):** the profile flags are accepted as-is. The first run failed with
  "Not logged in": the CLI had never been logged in, which surfaced only after the non-zero-exit
  path learned to read claude's stdout error envelope (claude reports its own errors there, stderr
  empty). After `/login` a review of Goal 960 succeeded: 4 findings, a usage row with tokens and cost.
  Its `input_tokens` of 2 (beside 17,500 cache-creation tokens) led to recording input as the sum of
  the uncached, cache-creation and cache-read counts.
- `assistant_runs.provider` / `model` stay null. Nothing sets them on any path, Spring AI
  included; the usage row carries the attribution.
- **Step 6, codex:** a `raw`-mode review of the same Goal, with the command-line args override,
  succeeded: 2 findings, a usage row with provider `cli`, model `codex-cli`, latency only.
