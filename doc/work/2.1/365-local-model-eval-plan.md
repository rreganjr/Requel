# #365 — Re-check the tuned AI review definitions against a local Ollama model

https://github.com/rreganjr/Requel/issues/365

Branch `365-local-model-eval` from `release/2.1`, PR to `release/2.1`. Size 3.

## Summary

The per-type review definitions (#263) were tuned on a frontier model through the `cli` provider.
This scores them on two local models through the `ai-ollama` profile, compares the results with
#263's, documents the Ollama setup a real review needs, and records which instructions a smaller
model needs changed.

## Locked decisions (2026-11-03)

1. **Models:** `qwen3.6:27b` (dense, ~17 GB) and `gemma4:26b` (mixture-of-experts, 4B active,
   ~18 GB), both scored and compared. Both fit a 36 GB M3 Pro with room for a 32K context.
2. **Context length is documented as required.** Ollama's default context is 4,096 tokens and cuts a
   longer prompt without an error; a review prompt can reach `requel.ai.max-input-tokens` (16,000).
   The setup guide says to start Ollama with `OLLAMA_CONTEXT_LENGTH=32768`.
3. **Prompt-embedded JSON first.** The `ai-ollama` profile goes through Ollama's OpenAI-compatible
   endpoint, where Spring AI embeds the output schema in the prompt rather than enforcing it. The
   first runs use that as it is, which tests whether the instructions hold up. Schema failures, if
   any, are explained; enforcing the schema (Ollama's `format` / a JSON-schema `response_format`)
   is tried only if they need it, and recorded beside the first result.
4. **Ron runs the scoring on his Mac** from step-by-step instructions
   (`tmp/365-run-instructions.md`). Claude reads the reports from `target/ai-eval/` over the
   device bridge and writes up the results.

## State of the tree

- Definitions are at version 2 since #337 (one sentence on existing-issue severity, inert on the
  fixture, whose entities have no human issues). The #263 comparison ran version 1.
- Requel keeps no copy of what it sends a provider or what comes back, so a local model's reply
  can't be inspected after a run.

## Changes

- `scripts/ai-eval/capture-proxy.py` (new, dev tool, standard library only): sits between Requel
  and the OpenAI-compatible server, writes each request and reply to the capture directory, and
  prints one line per exchange: model, status, time, prompt and completion tokens, finish reason,
  reasoning length, and whether the reply's content parses as JSON.
- `scripts/ai-eval/README.md`: "Local models and the capture proxy".
- `doc/guides/AI_ASSISTANT_SETUP.md`: section 4c, a local model big enough for real reviews: the
  two models, `OLLAMA_CONTEXT_LENGTH`, checking `ollama ps`, thinking, the proxy.
- `doc/work/2.1/365-local-model-eval.md` (after the runs): both models against #263's `cli`
  result, per type and per entity, schema failures explained, timing, and the instruction notes
  the AC asks for. Raw findings as `365-local-model-eval-<model>-raw.json` beside it.
- Definition changes, only if a run shows an instruction the smaller model can't follow and a
  simpler one doesn't cost the `cli` result; otherwise recorded as why not.

## Steps

1. Proxy, README, setup guide (done on the branch).
2. Ron: Ollama setup, smoke checks, a one-type trial, then a full run per model.
3. Read the reports and captures; write the results doc and the notes; any definition change and
   a re-score.
4. `tmp/365-verify.sh` (the docs and script change nothing in the build; the gate is the backend
   build, for the record), commit.

## Test plan

- `capture-proxy.py` checked against a fake upstream (forwarding, capture files, summary line).
- The runs themselves are the measurement. Each report is re-scorable from its raw JSON.

## Out of scope

- Changing the bundled Ollama default model or the profile's defaults beyond the documentation.
- Making the proxy part of the product (body capture in `assistant_usages` stays as it is).

## AC mapping

| AC | Covered by |
|---|---|
| A recorded `score.py` run on a local model, beside the #263 `cli` results | 365-local-model-eval.md, both models |
| Zero schema failures, or each explained | the results doc, from the capture lines |
| Notes in `doc/` on instructions that needed changing | the results doc's notes section |
