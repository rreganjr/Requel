# AI Assistant Setup Guide

This guide explains how to turn on Requel's AI requirements‑review assistant, step by step. It
assumes **no prior experience** with API keys, environment variables, or Spring configuration —
every step is spelled out. If you already know a step, skip ahead.

Requel can use **one** AI provider at a time. This guide covers both supported providers —
**OpenAI** and **Anthropic (Claude)** — and you pick one. Everything is configured with
**environment variables**, so there is nothing to edit inside the application's configuration
files.

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

Follow **2a** for OpenAI **or** **2b** for Anthropic — whichever you'll use.

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

### 2b. Anthropic (Claude)

1. Go to **https://console.anthropic.com** and sign in or create an account.
2. Add a payment method and a spending limit under the **Billing / Limits** settings. This caps
   what Requel can ever spend.
3. Open the **API keys** page in the console and click **Create Key**. Name it `requel`.
4. **Copy the key immediately** (it starts with `sk-ant-`) and store it somewhere safe — the
   console shows it only once.
5. Pick a model id from **https://docs.claude.com/en/docs/about-claude/models** (a current Claude
   model). You'll use this as `REQUEL_AI_MODEL`.

---

## 3.Turn the assistant on with environment variables

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
| `REQUEL_AI_PROVIDER` | yes | `openai` or `anthropic` (or `noop` to test the plumbing with no network calls). |
| `REQUEL_AI_MODEL` | yes | The model id you chose in Section 2. |
| `REQUEL_AI_API_KEY` | yes | Your provider API key (the `sk-...` / `sk-ant-...` value). |
| `REQUEL_AI_ENDPOINT` | no | Leave unset — each provider uses its correct default URL. Set only for a proxy/gateway. |
| `REQUEL_AI_PROJECT_ALLOWLIST` | no | Comma‑separated project ids allowed to use AI. Unset = all projects. |

### macOS / Linux (Terminal)

Set the variables in the **same terminal session** where you'll start Requel. For **OpenAI**:

```bash
export REQUEL_AI_ENABLED=true
export REQUEL_AI_PROVIDER=openai
export REQUEL_AI_MODEL=<your-openai-model-id>
export REQUEL_AI_API_KEY="sk-your-openai-key"
```

For **Anthropic**, only the provider, model, and key change:

```bash
export REQUEL_AI_ENABLED=true
export REQUEL_AI_PROVIDER=anthropic
export REQUEL_AI_MODEL=<your-claude-model-id>
export REQUEL_AI_API_KEY="sk-ant-your-anthropic-key"
```

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

   The `provider` should match what you configured (`openai` or `anthropic`). A failed call writes
   **no** row, so a stale `noop` row from an earlier test is a sign the latest run errored — check
   the log.

Each provider module also has a live end‑to‑end test that is **skipped unless** its key is present:
`OpenAiAnalysisClientLiveIT` (gated on `OPENAI_API_KEY`) and `AnthropicAnalysisClientLiveIT` (gated
on `ANTHROPIC_API_KEY`). These let you exercise the real API from a build without affecting CI.

---

## 7. Troubleshooting

**The startup log shows `provider=noop`, or the usage row says `noop`.**
`REQUEL_AI_PROVIDER` wasn't set to `openai`/`anthropic` in the environment that launched Requel, so
the built‑in stub is active. Confirm the variable, then restart.

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

Set these as environment variables (the `requel.ai.*` form is the internal Spring property name).

| Environment variable | Property | Default | What it does |
| --- | --- | --- | --- |
| `REQUEL_AI_ENABLED` | `requel.ai.enabled` | `false` | Master switch. Must be `true` to register the assistant. |
| `REQUEL_AI_PROVIDER` | `requel.ai.provider` | `noop` | Selects the client: `openai`, `anthropic`, or `noop` (stub, no network). Exactly one is active. |
| `REQUEL_AI_MODEL` | `requel.ai.model` | `noop` | Model id for the chosen provider. |
| `REQUEL_AI_API_KEY` | `requel.ai.api-key` | *(empty)* | API key for the chosen provider. Read from this variable by default. |
| `REQUEL_AI_API_KEY_ENVIRONMENT_VARIABLE` | `requel.ai.api-key-environment-variable` | `REQUEL_AI_API_KEY` | Name of the env var the key is read from. Change only to reuse a vendor‑specific name. |
| `REQUEL_AI_ENDPOINT` | `requel.ai.endpoint` | *(blank)* | API URL. Blank = each provider's default (OpenAI Responses / Anthropic Messages). Set for a proxy. |
| `REQUEL_AI_TIMEOUT` | `requel.ai.timeout` | `30s` | Per‑request timeout. |
| `REQUEL_AI_MAX_RETRIES` | `requel.ai.max-retries` | `2` | Retries on a temporary (429/5xx) error. |
| `REQUEL_AI_MAX_INPUT_TOKENS` | `requel.ai.max-input-tokens` | `16000` | Safety cap on input size; oversize reviews are skipped with a warning. |
| `REQUEL_AI_MAX_OUTPUT_TOKENS` | `requel.ai.max-output-tokens` | `4000` | Cap on how much the model may write back. |
| `REQUEL_AI_PROJECT_ALLOWLIST` | `requel.ai.project-allowlist` | *(empty = all)* | CSV of project ids permitted to use AI. |

---

## Related documentation

- `doc/43-phase-5-plan.md` — implementation plan and exit criteria for the AI assistant.
- `doc/ai-assistance-plan.md` — overall AI‑assistance architecture (provider layer, context packs).
- `doc/AUTH_ARCH.md` — how `/api/**` authorization works.
