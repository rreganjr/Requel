# AI Assistant Setup Guide

This guide explains how to turn on Requel's AI requirements‑review assistant, step by step. It
assumes **no prior experience** with API keys, environment variables, or Spring configuration —
every step is spelled out. If you already know a step, skip ahead.

Requel can use **one** AI provider at a time. You have two choices today: **OpenAI**, or a
**local model you run yourself in Docker** (no account, no API key, nothing leaves your machine —
see [Section 4](#4-run-a-local-ai-model-in-docker-no-api-key)). Everything is configured with
**environment variables**, so there is nothing to edit inside the application's configuration files.

> **Spring AI migration (issue #77).** Requel's provider layer was rebuilt on Spring AI's
> `ChatClient`, replacing the hand-rolled OpenAI/Anthropic/OpenAI-compatible clients with one
> adapter. What changed for configuration:
> - **Anthropic is temporarily unavailable** — the OpenAI starter shipped first; Anthropic returns
>   as a fast-follow (it's a starter swap). For now use `openai`, `openai-compat`, or `noop`.
>   Setting `REQUEL_AI_PROVIDER=anthropic` is not supported yet.
> - **`REQUEL_AI_BASE_URL` replaces `REQUEL_AI_ENDPOINT`** for local/OpenAI-compatible servers, and
>   it is the server **root** (e.g. `http://localhost:11434`) — Spring AI appends
>   `/v1/chat/completions` itself. The old full-URL `REQUEL_AI_ENDPOINT` is no longer read.
> - **`REQUEL_AI_STRUCTURED_OUTPUT_MODE` is gone** — Spring AI requests structured output and Requel
>   validates it.
> - **Timeout/retries** are now Spring AI's (`spring.ai.retry.*`); `REQUEL_AI_MAX_RETRIES` is still
>   bridged. A configurable request timeout for slow local models is a known follow-up.

By the end you will have:

1. An account and an API key with your chosen provider (OpenAI or Anthropic).
2. That key stored safely as an environment variable on the machine that runs Requel.
3. A small set of `REQUEL_AI_*` environment variables that turn the assistant on.
4. A working test that confirms the assistant runs.

---

## 1. What this feature does (and what it costs)

Requel can ask an external AI model to **review a requirement** — a goal, story, actor, use case,
scenario, or step — and report quality problems such as ambiguity, missing detail, or untestable
wording. Those findings are attached to the entity as discussion **issues** and **notes**, exactly
like the findings the built‑in checks produce.

A few things to understand before you start:

- **It is off by default.** Requel ships with the AI assistant disabled. Nothing leaves your
  network until you deliberately turn it on.
- **It only runs when explicitly asked.** The assistant does **not** run automatically every time
  someone edits a requirement. It runs only when a "requirements review" is requested for a
  specific entity (see [Section 5](#5-using-the-assistant)).
- **It sends data to your AI provider.** When a review runs, the text of the entity (and a small
  amount of surrounding context) is sent to OpenAI's or Anthropic's servers to be analyzed. Do not
  enable this for projects whose contents may not leave your organization without approval.
- **It costs money.** Providers charge per use, based on the amount of text sent and received.
  Costs are usually small per review, but they are not zero. You control spending limits in your
  provider account, and Requel records how many tokens each review used.

---

## 2. Pick a provider, then create an account and API key

**Keep the key secret.** Never paste your API key into the Requel UI, a chat message, a Git
commit, a screenshot, or anywhere public. The steps below store it as an *environment variable*,
which keeps it out of Requel's configuration files.

Follow **2a** for OpenAI. (Anthropic — **2b** — is temporarily unavailable pending the Anthropic
starter fast-follow; see the migration note at the top.) If you'd rather run a local model with no
key at all, skip this section and go to [Section 4](#4-run-a-local-ai-model-in-docker-no-api-key).

### 2a. OpenAI

1. Go to **https://platform.openai.com** and sign in or create an account. This is the *developer
   platform*, separate from the ChatGPT consumer product — you need a platform account here.
2. Add a payment method and a spending limit under **Settings → Billing → Limits**. This caps what
   Requel can ever spend.
3. Open **https://platform.openai.com/api-keys** and click **Create new secret key**. Name it
   `requel`.
4. **Copy the key immediately** (it starts with `sk-`) and store it somewhere safe — OpenAI shows
   it only once.
5. Pick a model id from **https://platform.openai.com/docs/models** (start with a smaller, cheaper
   general‑purpose model if unsure). You'll use this as `REQUEL_AI_MODEL`.

### 2b. Anthropic (Claude) — *not yet available*

> Anthropic support was removed in the Spring AI port and returns as a fast-follow (adding the
> `spring-ai-starter-model-anthropic` starter and routing `REQUEL_AI_PROVIDER=anthropic` to the same
> adapter). The account steps below are retained for when it lands; they do nothing today.

1. Go to **https://console.anthropic.com** and sign in or create an account.
2. Add a payment method and a spending limit under the **Billing / Limits** settings. This caps
   what Requel can ever spend.
3. Open the **API keys** page in the console and click **Create Key**. Name it `requel`.
4. **Copy the key immediately** (it starts with `sk-ant-`) and store it somewhere safe — the
   console shows it only once.
5. Pick a model id from **https://docs.claude.com/en/docs/about-claude/models** (a current Claude
   model). You'll use this as `REQUEL_AI_MODEL`.

---

## 3. Turn the assistant on with environment variables

Requel reads all of its AI settings from environment variables whose names start with
**`REQUEL_AI_`**. When Requel starts, it looks them up and configures itself accordingly. (Spring,
the framework underneath Requel, automatically maps an environment variable like
`REQUEL_AI_API_KEY` onto its internal setting `requel.ai.api-key` — you don't need to know the
internal names.)

Environment variables are only read **when the app starts**, so after changing any of them you
must **restart** Requel.

Set the variables below, then restart Requel. Use the block for your chosen provider; the variable
*names* are identical for both — only `REQUEL_AI_PROVIDER`, `REQUEL_AI_MODEL`, and the key value
differ. That's the whole point of the unified `REQUEL_AI_API_KEY`: switching providers means
changing three values, not learning a new variable name.

### The variables

| Variable | Required? | What to set it to |
| --- | --- | --- |
| `REQUEL_AI_ENABLED` | yes | `true` to turn the assistant on. |
| `REQUEL_AI_PROVIDER` | yes | `openai`, `openai-compat` (local/self-hosted, see Section 4), or `noop` (test plumbing, no network). (`anthropic` is a fast-follow — not yet available.) |
| `REQUEL_AI_MODEL` | yes | The model id you chose in Section 2 (or the local model name for `openai-compat`). |
| `REQUEL_AI_API_KEY` | yes\* | Your provider API key (`sk-...`). \*Optional for a local `openai-compat` server that doesn't require one (any non-blank value works). |
| `REQUEL_AI_BASE_URL` | no | Leave unset for hosted `openai` (the default `https://api.openai.com` is used). **Set for `openai-compat`** — the server **root** (e.g. `http://localhost:11434`); Spring AI appends `/v1/chat/completions`. |
| `REQUEL_AI_PROJECT_ALLOWLIST` | no | Comma‑separated project ids allowed to use AI. Unset = all projects. |

### macOS / Linux (Terminal)

Set the variables in the **same terminal session** where you'll start Requel. For **OpenAI**:

```bash
export REQUEL_AI_ENABLED=true
export REQUEL_AI_PROVIDER=openai
export REQUEL_AI_MODEL=<your-openai-model-id>
export REQUEL_AI_API_KEY="sk-your-openai-key"
```

(Anthropic would be the same three values with `REQUEL_AI_PROVIDER=anthropic`, but it is not wired
up yet — see the migration note at the top.)

To make them permanent, add the same `export` lines to `~/.zshrc` (modern macOS) or `~/.bashrc`
(most Linux), then run `source ~/.zshrc`. Confirm with:

```bash
echo $REQUEL_AI_PROVIDER $REQUEL_AI_MODEL
echo $REQUEL_AI_API_KEY
```

### Windows (PowerShell)

For the current window (OpenAI shown; swap the three values for Anthropic):

```powershell
$env:REQUEL_AI_ENABLED = "true"
$env:REQUEL_AI_PROVIDER = "openai"
$env:REQUEL_AI_MODEL = "<your-openai-model-id>"
$env:REQUEL_AI_API_KEY = "sk-your-openai-key"
```

To persist for your user account (takes effect in *new* windows), use `setx` for each, e.g.
`setx REQUEL_AI_API_KEY "sk-your-openai-key"`, then reopen PowerShell.

### Docker / docker‑compose

Add the variables under the app service's `environment:` section in `docker-compose.yml`:

```yaml
services:
  app:
    environment:
      - REQUEL_AI_ENABLED=true
      - REQUEL_AI_PROVIDER=openai            # or: anthropic
      - REQUEL_AI_MODEL=${REQUEL_AI_MODEL}
      - REQUEL_AI_API_KEY=${REQUEL_AI_API_KEY}
```

The `${...}` syntax pulls each value from your shell's environment so the secret isn't written
into the file.

> You do **not** need to edit `application.properties`. The defaults there leave the assistant off;
> these environment variables override them at startup. (If you ever must keep a vendor‑specific
> key variable name like `OPENAI_API_KEY`, set `REQUEL_AI_API_KEY_ENVIRONMENT_VARIABLE` to that
> name and Requel will read the key from it instead.)

---

## 4. Run a local AI model in Docker (no API key)

If you can't get a cloud API key (or don't want your data leaving the building), you can run a
model **locally** and point Requel at it. The `openai-compat` provider talks to any server that
implements the OpenAI‑style `POST /v1/chat/completions` API — including
[Ollama](https://ollama.com), LM Studio, vLLM, and LocalAI. This guide uses **Ollama** because it
runs in one Docker command.

> **Structured‑output note.** Spring AI requests structured output (forcing guaranteed‑shape JSON)
> and Requel validates it; there is no longer a `REQUEL_AI_STRUCTURED_OUTPUT_MODE` knob. Cloud
> OpenAI enforces a strict schema natively; local servers vary in how strictly they honor it, but
> Spring AI also embeds the schema in the prompt so the model returns schema‑shaped JSON either way.
> Smaller local models are less precise than the big cloud ones — expect rougher findings. For
> *proving the pipeline*, it's perfect.

### 4a. Ollama in Docker, Requel running on your machine

Use this when you start Requel from the jar (or `mvn spring-boot:run`) directly on your computer.

1. Start Ollama and pull a model (one time):

   ```bash
   docker run -d -v ollama:/root/.ollama -p 11434:11434 --name ollama ollama/ollama
   docker exec -it ollama ollama pull qwen2.5:3b
   ```

   > **Pick a model that fits your RAM.** A small model like `qwen2.5:3b` (~2 GB, and notably
   > better at structured JSON than similarly sized models) or `llama3.2:3b` runs comfortably and
   > is fine for proving the pipeline. Big models (e.g. `llama3.1` 8B) need 6–8 GB+ or Ollama gets
   > killed mid-run — see the OOM note in Section 7.

2. Point Requel at it (same terminal you start Requel from):

   ```bash
   export REQUEL_AI_ENABLED=true
   export REQUEL_AI_PROVIDER=openai-compat
   export REQUEL_AI_BASE_URL=http://localhost:11434   # server ROOT; Spring AI adds /v1/chat/completions
   export REQUEL_AI_MODEL=llama3.1
   export REQUEL_AI_API_KEY=ollama          # ignored by Ollama; any non-blank value
   ```

3. Start Requel and use it as normal (Section 5).

### 4b. Everything in Docker (Requel + database + Ollama together)

For a one‑command, fully self‑contained setup, use the bundled compose file
**`docker-compose.local-ai.yml`** at the repo root. It runs MySQL, Requel, and Ollama on one
network and wires the AI variables for you (the app reaches Ollama by the service name `ollama`,
not `localhost`).

```bash
# start all three containers
docker compose -f docker-compose.local-ai.yml up -d

# pull the model into the Ollama container (one time; cached in a named volume)
# this must match REQUEL_AI_MODEL in the compose file (default: qwen2.5:3b)
docker compose -f docker-compose.local-ai.yml exec ollama ollama pull qwen2.5:3b
```

Then open Requel at http://localhost:8080/ and trigger a review (Section 5).

The AI settings in this compose file are **hardcoded** (provider, endpoint, key, model) — it's
tied to the bundled Ollama service, so they never vary, and fixing the model means a stray
`REQUEL_AI_*` in your shell can't leak into the container. To use a different model, edit the
`REQUEL_AI_MODEL=qwen2.5:3b` line in `docker-compose.local-ai.yml` to a name you've pulled, then
recreate the app container:

```bash
docker compose -f docker-compose.local-ai.yml exec ollama ollama pull llama3.1   # pull it first
# (after editing REQUEL_AI_MODEL in the compose file)
docker compose -f docker-compose.local-ai.yml up -d --force-recreate web
```

> **Why `http://ollama:11434` and not `localhost`?** Inside Docker, each container's `localhost`
> is itself. Containers reach each other by service name on the shared network, so the compose
> file sets `REQUEL_AI_BASE_URL=http://ollama:11434` (the server root). If you instead run Ollama
> in Docker but Requel on the host (Section 4a), use `http://localhost:11434`; if Requel is in
> Docker but Ollama is on the host, use `http://host.docker.internal:11434`.

After setting the variables, **restart Requel**. On startup you should see a log line confirming
the assistant is active, including the provider and model (never the key):

```
AI requirements-review assistant enabled (provider=openai, model=..., projectAllowlist=all projects)
```

---

## 5. Using the assistant

The assistant runs only when a review is **explicitly requested** for one entity, via the API
endpoint `POST /api/ai/reviews`. Reviewable entity types are: **Goal, Story, Actor, UseCase,
Scenario, Step**.

Because this endpoint lives under `/api/**`, it requires you to be logged in. Logging in returns a
**token** that you include on the review request. Full sequence with `curl`:

1. **Log in** to get a token (default development login is `admin` / `admin`):

   ```bash
   curl -X POST http://localhost:8080/api/auth/login \
     -H "Content-Type: application/json" \
     -d '{"username":"admin","password":"admin"}'
   ```

   Copy the `token` value from the response.

2. **Request a review** for a specific entity. Set `entityType` to one of the reviewable types and
   `entityId` to the entity's numeric id:

   ```bash
   curl -i -X POST "http://localhost:8080/api/ai/reviews?entityType=UseCase&entityId=2" \
     -H "Authorization: Bearer PASTE_YOUR_TOKEN_HERE"
   ```

   A successful request returns **HTTP 202 Accepted**; the review then runs in the background.
   `404 Not Found` means no such entity; `403 Forbidden` means you lack access to that entity's
   project; `401 Unauthorized` means the token is missing or expired.

3. **See the results** in Requel: open the entity in the UI and look at its discussion /
   annotations. The AI‑generated issues and notes appear there once the review finishes.

**Who is allowed to run a review.** You can review an entity only if you're a system administrator
or a stakeholder on that entity's project. This is enforced on top of the optional
`REQUEL_AI_PROJECT_ALLOWLIST`.

---

## 6. Verifying your setup

1. **App starts and logs the assistant line** (Section 4). If you don't see it, `REQUEL_AI_ENABLED`
   isn't `true` in the environment that launched Requel.
2. **Trigger a review** (Section 5) and confirm **202 Accepted**.
3. **Check the entity's annotations** in the UI for new AI‑sourced issues/notes after a few
   seconds.
4. **Confirm the provider call succeeded** by checking the `assistant_usages` table — each
   successful review records a row with the provider, model, token counts, estimated cost, and
   latency:

   ```sql
   SELECT provider, model, input_tokens, output_tokens, cost_estimate, latency_ms
   FROM assistant_usages ORDER BY id DESC LIMIT 1;
   ```

   The `provider` should match what you configured (`openai` or `openai-compat`). A failed call
   writes **no** row, so a stale `noop` row from an earlier test is a sign the latest run errored —
   check the log.

The provider‑specific live end‑to‑end ITs were removed in the Spring AI port (the per‑provider
HTTP plumbing they exercised is now Spring AI's). CI runs no‑network by default (`provider=noop`);
to exercise a real provider, configure `REQUEL_AI_*` for OpenAI (or a local `openai-compat` server)
and run a review against a live instance as in Section 5. A reusable opt‑in smoke test over the
Spring AI adapter is a follow‑up.

---

## 7. Troubleshooting

**The startup log shows `provider=noop`, or the usage row says `noop`.**
`REQUEL_AI_PROVIDER` wasn't set to `openai`/`openai-compat` in the environment that launched Requel,
so the built‑in stub is active. Confirm the variable, then restart.

**Reviews return 202 but no annotations appear.**
The review runs in the background — give it a few seconds. If nothing ever appears, check the
application log for a `RequirementsReviewAssistant` warning.

**Log mentions a missing API key.**
`REQUEL_AI_API_KEY` isn't visible to the running app. Confirm with `echo $REQUEL_AI_API_KEY`
(macOS/Linux) **in the same session that launched Requel** — a common mistake is setting it in one
terminal but starting Requel from another. For Docker, confirm it's passed into the container.

**HTTP 429 `insufficient_quota` / quota or billing errors.**
Your provider account is out of credits, has no payment method, or the key was disabled. Check your
provider's billing page. This is not a Requel problem.

**HTTP 500 `llama-server ... signal: killed` (local Ollama).**
The model server was killed for running out of memory. The model is too large for the RAM Docker
has. Either raise Docker Desktop's memory (Settings → Resources → Memory) well above the model
size, or use a smaller model (e.g. `llama3.2:3b` or `qwen2.5:3b` instead of an 8B). Remember to
`ollama pull` the new model and point `REQUEL_AI_MODEL` at it. Local CPU inference is also slow; the
request timeout is now governed by Spring AI's HTTP client defaults rather than the former
`REQUEL_AI_TIMEOUT` knob. A configurable timeout for slow local models is a tracked follow-up — if
reviews time out on a big model today, prefer a smaller/faster model.

**HTTP 404 `model '<name>' not found` (local Ollama).**
`REQUEL_AI_MODEL` names a model the server hasn't pulled. Run `ollama pull <name>` and confirm with
`ollama list`; the value must match exactly.

**Reviews are skipped with a "maxInputTokens" warning.**
The entity's context was larger than the input cap (`REQUEL_AI_MAX_INPUT_TOKENS`, default 16000).
This is the cost guardrail working. Raise it only if you accept sending (and paying for) more text.

**403 Forbidden when requesting a review.**
Either your user isn't a stakeholder/admin on that entity's project, or the project isn't on a
non‑empty `REQUEL_AI_PROJECT_ALLOWLIST`.

**401 Unauthorized.**
Your login token is missing or expired (tokens last `requel.jwt.expiry-hours`, 8h by default). Log
in again and reuse the fresh token.

**Wrong model id.**
If `REQUEL_AI_MODEL` isn't a real model for the selected provider, the call returns a provider
error (a `warn` in the log) rather than a usage row. Re‑check the id against the provider's model
list.

---

## 8. Turning it back off

Set `REQUEL_AI_ENABLED=false` (or unset it) and restart. The assistant is no longer registered and
no data is sent to any provider. You can leave the other variables in place; they're simply unused
while the feature is off.

---

## 9. Full settings reference

Since the Spring AI port (issue #77), Requel keeps only its governance settings under
`requel.ai.*`; transport (key, base-url, model, output cap, retries) is owned by Spring AI under
`spring.ai.*`. The familiar `REQUEL_AI_*` environment variables still work — `application.properties`
bridges them to the matching `spring.ai.*` setting.

**Requel governance (`requel.ai.*`):**

| Environment variable | Property | Default | What it does |
| --- | --- | --- | --- |
| `REQUEL_AI_ENABLED` | `requel.ai.enabled` | `false` | Master switch. Must be `true` to register the assistant. |
| `REQUEL_AI_PROVIDER` | `requel.ai.provider` | `noop` | Selects the client: `openai`, `openai-compat` (local/self-hosted), or `noop` (stub, no network). `anthropic` is a fast-follow — not yet available. Exactly one is active. |
| `REQUEL_AI_MODEL` | `requel.ai.model` | `noop` | Model id; also bridged to `spring.ai.openai.chat.options.model` so the call and the usage report stay in sync. |
| `REQUEL_AI_MAX_INPUT_TOKENS` | `requel.ai.max-input-tokens` | `16000` | App-side safety cap on input size; oversize reviews are skipped with a warning. |
| `REQUEL_AI_PROJECT_ALLOWLIST` | `requel.ai.project-allowlist` | *(empty = all)* | CSV of project ids permitted to use AI. |

**Transport, bridged to Spring AI (`application.properties`):**

| Environment variable | Bridged to | Default | What it does |
| --- | --- | --- | --- |
| `REQUEL_AI_API_KEY` | `spring.ai.openai.api-key` | `not-set` | Provider API key. The placeholder default lets the app boot with AI disabled; set a real key for `openai`. Optional (any non-blank value) for a local `openai-compat` server. |
| `REQUEL_AI_BASE_URL` | `spring.ai.openai.base-url` | `https://api.openai.com` | Server **root** for `openai-compat` (e.g. `http://localhost:11434`); Spring AI appends `/v1/chat/completions`. Replaces the old full-URL `REQUEL_AI_ENDPOINT`. |
| `REQUEL_AI_MAX_OUTPUT_TOKENS` | `spring.ai.openai.chat.options.max-tokens` | `4000` | Cap on how much the model may write back. |
| `REQUEL_AI_MAX_RETRIES` | `spring.ai.retry.max-attempts` | `3` | Retries on a temporary (429/5xx) error. |

> **Removed in the port:** `REQUEL_AI_ENDPOINT` (use `REQUEL_AI_BASE_URL`),
> `REQUEL_AI_STRUCTURED_OUTPUT_MODE` (Spring AI owns structured output),
> `REQUEL_AI_API_KEY_ENVIRONMENT_VARIABLE`, and `REQUEL_AI_TIMEOUT` (HTTP timeout is now Spring AI's;
> a configurable knob is a tracked follow-up). Setting them has no effect.

---

## Related documentation

- `doc/43-phase-5-plan.md` — implementation plan and exit criteria for the AI assistant.
- `doc/ai-assistance-plan.md` — overall AI‑assistance architecture (provider layer, context packs).
- `doc/AUTH_ARCH.md` — how `/api/**` authorization works.
