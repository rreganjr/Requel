# AI Assistant Setup Guide

This guide explains how to turn on Requel's AI requirements‑review assistant, step by step. It
assumes **no prior experience** with API keys, environment variables, or Spring configuration —
every step is spelled out. If you already know a step, skip ahead.

By the end you will have:

1. An OpenAI account and an API key.
2. That key stored safely on the machine that runs Requel.
3. Requel configured to use the AI assistant.
4. A working test that confirms the assistant runs.

---

## 1. What this feature does (and what it costs)

Requel can ask an external AI model (OpenAI) to **review a requirement** — a goal, story, actor,
use case, scenario, or step — and report quality problems such as ambiguity, missing detail, or
untestable wording. Those findings are attached to the entity as discussion **issues** and
**notes**, exactly like the findings the built‑in checks produce.

A few things to understand before you start:

- **It is off by default.** Requel ships with the AI assistant disabled. Nothing leaves your
  network until you deliberately turn it on.
- **It only runs when explicitly asked.** The assistant does **not** run automatically every time
  someone edits a requirement. It runs only when a "requirements review" is requested for a
  specific entity (see [Section 6](#6-using-the-assistant)).
- **It sends data to OpenAI.** When a review runs, the text of the entity (and a small amount of
  surrounding context) is sent to OpenAI's servers to be analyzed. Do not enable this for projects
  whose contents may not leave your organization without approval.
- **It costs money.** OpenAI charges per use, based on the amount of text sent and received. Costs
  are usually small per review, but they are not zero. You control spending limits in your OpenAI
  account, and Requel records how many tokens each review used.

---

## 2. Create an OpenAI account and an API key

An **API key** is a long secret password that lets a program (Requel) talk to OpenAI on your
behalf. Treat it like a password: anyone who has it can spend money on your account.

1. Go to **https://platform.openai.com** in a web browser and sign in, or create an account if you
   don't have one. Note that this is the *developer platform*, which is separate from the ChatGPT
   consumer product — you need a platform account here.
2. Add a payment method and, ideally, a spending limit: open **Settings → Billing**, add a card,
   and under **Limits** set a monthly budget you're comfortable with. This caps what Requel can
   ever spend.
3. Open the **API keys** page (find it under your account menu, usually **Dashboard → API keys**,
   or go directly to **https://platform.openai.com/api-keys**).
4. Click **Create new secret key**. Give it a name like `requel` so you remember what it's for.
5. **Copy the key immediately** and paste it somewhere safe (a password manager is ideal). OpenAI
   shows the full key **only once**. It looks like `sk-...` followed by a long string of letters
   and numbers. If you lose it, you can't see it again — you'd just create a new one.

You'll also need to pick a **model** — the specific AI engine that does the work. Different models
trade off cost, speed, and quality. You can see the available models and their prices on the
OpenAI platform under **Docs → Models** (or **https://platform.openai.com/docs/models**). Pick a
current general‑purpose model id; you'll paste that id into Requel's configuration in
[Section 5](#5-configure-requel). If you're not sure, start with one of OpenAI's smaller, cheaper
models — you can change it later by editing one line of configuration.

> **Keep the key secret.** Never paste your API key into the Requel UI, a chat message, a Git
> commit, a screenshot, or anywhere public. The steps below store it as an *environment variable*,
> which keeps it out of Requel's configuration files.

---

## 3. What an environment variable is

An **environment variable** is a named value that the operating system holds in memory and hands
to programs when they start. It's a standard way to give a program a secret (like an API key)
*without writing the secret into any file the program reads*.

Requel expects your key in an environment variable named **`OPENAI_API_KEY`**. When Requel starts,
it looks for that variable and uses whatever value it finds.

The next section shows how to set it on each operating system.

---

## 4. Store your API key as an environment variable

Pick the section for your operating system. In every example, replace
`sk-your-key-goes-here` with the actual key you copied in Section 2.

### macOS / Linux (Terminal)

The most reliable approach is to set the variable in the **same terminal session** where you'll
start Requel, right before you start it:

```bash
export OPENAI_API_KEY="sk-your-key-goes-here"
```

This lasts only for that terminal window. To make it permanent, add the same line to your shell
startup file — `~/.zshrc` on modern macOS, or `~/.bashrc` on most Linux systems:

```bash
echo 'export OPENAI_API_KEY="sk-your-key-goes-here"' >> ~/.zshrc
source ~/.zshrc
```

Confirm it worked (this should print your key):

```bash
echo $OPENAI_API_KEY
```

### Windows (PowerShell)

For the current PowerShell window only:

```powershell
$env:OPENAI_API_KEY = "sk-your-key-goes-here"
```

To set it permanently for your user account (takes effect in *new* windows):

```powershell
setx OPENAI_API_KEY "sk-your-key-goes-here"
```

Close and reopen PowerShell, then confirm:

```powershell
echo $env:OPENAI_API_KEY
```

### Docker / docker‑compose

If you run Requel with `docker-compose up`, pass the key into the container by adding it under the
app service's `environment:` section in `docker-compose.yml`:

```yaml
services:
  app:
    environment:
      - OPENAI_API_KEY=${OPENAI_API_KEY}
      - REQUEL_AI_ENABLED=true
      - REQUEL_AI_PROVIDER=openai
      - REQUEL_AI_MODEL=<the-model-id-you-chose>
```

The `${OPENAI_API_KEY}` syntax pulls the value from your shell's environment (set as in the macOS/
Linux steps above) so the secret still isn't written into the file. Spring maps the uppercase
`REQUEL_AI_*` variables onto the `requel.ai.*` properties described next.

> **Advanced (optional).** If you can't use an environment variable, you can instead set
> `requel.ai.apiKey` directly (see the next section). This is **not recommended** because the key
> ends up in a configuration file. If you do, never commit that file to Git.

---

## 5. Configure Requel

Requel's settings live in
`modules/requel-app/src/main/resources/application.properties`. The AI block is already there,
disabled, with comments. Open the file in any text editor and set these three values:

```properties
requel.ai.enabled=true
requel.ai.provider=openai
requel.ai.model=<the-model-id-you-chose-in-section-2>
```

That's the minimum. With `OPENAI_API_KEY` set (Section 4) and these three lines, the assistant is
ready.

### All available settings

Every setting has a sensible default; you only need to override the ones you care about.

| Property | Default | What it does |
| --- | --- | --- |
| `requel.ai.enabled` | `false` | Master switch. Must be `true` to register the assistant at all. |
| `requel.ai.provider` | `noop` | `openai` uses the real OpenAI API. `noop` (the default) is a built‑in stub that returns no findings — useful for testing the plumbing without spending money. |
| `requel.ai.model` | `noop` | The OpenAI model id (e.g. the id you chose in Section 2). Required when `provider=openai`. |
| `requel.ai.apiKeyEnvironmentVariable` | `OPENAI_API_KEY` | The name of the environment variable Requel reads the key from. Change only if you stored the key under a different name. |
| `requel.ai.apiKey` | *(empty)* | A direct key value. Leave empty and use the environment variable instead (see the note above). |
| `requel.ai.endpoint` | `https://api.openai.com/v1/responses` | The OpenAI API URL. Change only for a proxy or compatible gateway. |
| `requel.ai.timeout` | `30s` | How long to wait for a response before giving up. |
| `requel.ai.maxRetries` | `2` | How many times to retry on a temporary network/server error. |
| `requel.ai.maxInputTokens` | `16000` | Safety cap on how much text is sent. If an entity's context is larger, the review is skipped with a warning instead of running up a large bill. |
| `requel.ai.maxOutputTokens` | `4000` | Cap on how much the model may write back. |
| `requel.ai.projectAllowlist` | *(empty = all projects)* | An optional list of project ids permitted to use AI. Empty means every project may. To restrict it, list ids comma‑separated, e.g. `requel.ai.projectAllowlist=12,34`. A project not on a non‑empty list has its reviews skipped. |

> **Where the key actually comes from.** With the defaults, Requel reads
> `requel.ai.apiKeyEnvironmentVariable` (which is `OPENAI_API_KEY`) and looks up that environment
> variable at runtime. So setting the `OPENAI_API_KEY` environment variable in Section 4 is what
> supplies the key — you do not put the key in `application.properties`.

After editing the file, rebuild and restart Requel for the changes to take effect (see the project
README / `CLAUDE.md` for the exact build and run commands).

---

## 6. Using the assistant

The assistant runs only when a review is **explicitly requested** for one entity. Reviews are
triggered through the API endpoint `POST /api/ai/reviews`.

Reviewable entity types are: **Goal, Story, Actor, UseCase, Scenario, Step**.

Because this endpoint lives under `/api/**`, it requires you to be logged in. Logging in returns a
**token** that you include on the review request. Here is the full sequence with `curl` (a command
line tool for making web requests; it's preinstalled on macOS and Linux, and available on Windows):

1. **Log in** to get a token. Replace the username/password with your Requel credentials (the
   default development login is `admin` / `admin`):

   ```bash
   curl -X POST http://localhost:8080/api/auth/login \
     -H "Content-Type: application/json" \
     -d '{"username":"admin","password":"admin"}'
   ```

   The response includes a `token` field — a long string. Copy its value.

2. **Request a review** for a specific entity, pasting the token after `Bearer `. Set
   `entityType` to one of the reviewable types and `entityId` to the entity's numeric id:

   ```bash
   curl -X POST "http://localhost:8080/api/ai/reviews?entityType=Goal&entityId=123" \
     -H "Authorization: Bearer PASTE_YOUR_TOKEN_HERE"
   ```

   A successful request returns **HTTP 202 Accepted**, meaning the review was accepted and runs in
   the background. If the entity id doesn't exist you'll get **404 Not Found**; if you lack access
   to that entity's project you'll get **403 Forbidden**.

3. **See the results** in Requel: open the goal (or other entity) in the UI and look at its
   discussion / annotations. The AI‑generated issues and notes appear there once the background
   review finishes.

**Who is allowed to run a review.** You can review an entity only if you're either a system
administrator or a stakeholder on that entity's project. This is enforced on top of the optional
`projectAllowlist` setting.

---

## 7. Verifying your setup

Work through these in order:

1. **App starts cleanly.** Start Requel. If `requel.ai.enabled=true` but the key is missing or the
   model is wrong, the app still starts — failures surface only when a review is actually run.
2. **Trigger a review** on a known entity using the steps in Section 6 and confirm you get
   `202 Accepted`.
3. **Check the entity's annotations** in the UI for new AI‑sourced issues/notes after a few
   seconds.
4. **Confirm a usage record was written.** Each successful review records a row in the
   `assistant_usages` table (provider, model, token counts, estimated cost, latency). Seeing a row
   there confirms the call reached OpenAI and returned.

There is also an automated end‑to‑end test that calls the live OpenAI API:
`OpenAiAnalysisClientLiveIT` in the `assistant-openai` module. It is **skipped unless**
`OPENAI_API_KEY` is present in the environment, so it never runs (or fails) in CI without a key.
To run it yourself, set the key (and optionally `OPENAI_MODEL` to override the default model) and
run the module's tests with `mvn clean verify`.

---

## 8. Troubleshooting

**Reviews return 202 but no annotations appear.**
The review runs in the background, so give it a few seconds. If nothing ever appears, check the
application logs for an AI error, and confirm `requel.ai.provider=openai` (the default `noop`
provider intentionally produces no findings).

**Log mentions a missing API key.**
The `OPENAI_API_KEY` environment variable isn't visible to the running app. Confirm it with
`echo $OPENAI_API_KEY` (macOS/Linux) or `echo $env:OPENAI_API_KEY` (Windows) **in the same
session/context that launched Requel**. A common mistake is setting it in one terminal but starting
Requel from another. For Docker, confirm the variable is passed into the container.

**Reviews are skipped with a "maxInputTokens" warning.**
The entity's context was larger than `requel.ai.maxInputTokens`. This is the cost guardrail doing
its job. Raise the limit only if you understand it means sending (and paying for) more text.

**403 Forbidden when requesting a review.**
Either your user isn't a stakeholder/admin on that entity's project, or the project isn't on a
non‑empty `requel.ai.projectAllowlist`. Check both.

**401 Unauthorized.**
Your login token is missing, malformed, or expired. Log in again (Section 6, step 1) and reuse the
fresh token. Tokens expire after the configured `requel.jwt.expiry-hours` (8 hours by default).

**Authentication / OpenAI billing errors in the log.**
The key may be revoked, or your OpenAI account may have hit its spending limit or have no payment
method. Check your account at **https://platform.openai.com**.

---

## 9. Turning it back off

Set `requel.ai.enabled=false` in `application.properties` and restart. The assistant is no longer
registered and no data is sent to OpenAI. You can leave the `OPENAI_API_KEY` environment variable
in place; it's simply unused while the feature is off.

---

## Related documentation

- `doc/43-phase-5-plan.md` — implementation plan and exit criteria for the AI assistant.
- `doc/ai-assistance-plan.md` — overall AI‑assistance architecture (provider layer, context packs).
- `doc/AUTH_ARCH.md` — how `/api/**` authorization works.
