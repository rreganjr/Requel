# Release Plan — Requel 2.1

Branch `release/2.1`, cut from `master` at `v2.0.2`, version `2.1.0-dev`. Milestone `v2.1`,
board "Requel 2.1". Process: `doc/guides/RELEASE_PROCESS.md`; each ticket follows the lifecycle
in `CLAUDE.md` (review, plan, branch from `release/2.1`, verify, one PR, retro).

## Summary

17 open issues on `v2.1` as of 2026-10-27. A minor release may add features, commands,
queries, properties and additive schema; it may not break 2.0 data or integrations
(`RELEASE_PROCESS.md`, "Versions"). Two items already moved here from 2.0.2 because they change
the API: NOT_FOUND for unknown names and removing `EditProjectInput.id` (both in #345).

The order puts process and CI first (they make every later ticket cheaper), then the API error
contract (so every new gateway tool after it inherits the right error kinds), then bugs, then
features grouped by area. Size is a rough guess in working days, before each ticket's review.

## Order

### Phase 1 — process and CI

| # | Issue | Size | Notes |
|---|---|---|---|
| 1 | #315 CLAUDE.md: derive the default branch | 1 | Doc-only, straight onto `release/2.1`. `CLAUDE.md` names `release/2.0` 19 times; this cycle needs them right. The issue deferred "make the whole file release-agnostic" until 2.1 was in sight: it is. |
| 2 | #300 CI: GitHub Actions to Node 24 majors | 1 | The default-branch prerequisite is moot (`master` is default and carries the workflows). Ubuntu 26.04 AC added 2026-10-23. |
| 3 | #321 Coverage aggregate across modules | 2 | Also `ci.yml`; land after #300 to avoid conflicts. Runs `requel-cli` and `gateway-rest-client` tests in CI for the first time. |

### Phase 2 — API error contract

| # | Issue | Size | Notes |
|---|---|---|---|
| 4 | #345 Error kinds: CONFLICT / NOT_FOUND, no retry on stale version, remove `EditProjectInput.id` | 2 | Decide: a new `CONFLICT` kind (HTTP 409, MCP code) or `INVALID_INPUT`. Every later gateway ticket's tests assert kinds, so this goes first. |
| 5 | #331 `name` required only on create | 2 | Mechanism (class-level constraint vs validation group) settled in its plan. |

### Phase 3 — bugs

| # | Issue | Size | Notes |
|---|---|---|---|
| 6 | #341 DeleteScenario on a primary scenario; orphan steps; CopyScenario flattens nested scenarios | 3 | Decide: deep copy or link for nested scenarios. MySQL Testcontainers IT required. |

### Phase 4 — security and install

| # | Issue | Size | Notes |
|---|---|---|---|
| 7 | #381 Force the built-in admin to change its password | 3 | Every install starts as admin/admin. Touches login, the API filter, MCP and tokens, and the UI. |
| 8 | #380 In-place upgrade from 1.0.x | 5 | Prototype works. Stays on 2.1 (decided 2026-10-27; "Backlog" in its body predates the milestone). |

### Phase 5 — gateway and MCP features

| # | Issue | Size | Notes |
|---|---|---|---|
| 9 | #330 ResolveIssue over the gateway | 2 | Uses #345's kinds; #342's same-project checks already cover the binder. |
| 10 | #338 Filter open issues by minimum severity | 3 | REST, gateway, MCP, CLI and the Angular Open Issues page. |
| 11 | #387 Request an AI review from the editor and over MCP | 3 | |

### Phase 6 — NLP and glossary

| # | Issue | Size | Notes |
|---|---|---|---|
| 12 | #349 Primary verb for imperative text | 2 | Decide at review: actor check on scenario names or not. |
| 13 | #348 Glossary rename keeps a synonym and offers a referer rewrite | 5 | UI dialog plus gateway result change. |

### Phase 7 — AI assistants

| # | Issue | Size | Notes |
|---|---|---|---|
| 14 | #337 Severity in AI context packs | 1 | Small; independent of the rest. |
| 15 | #371 Assistant definitions in project XML | 3 | New schema version (additive). Decide: switches and data-handling too; arrive switched off; fail or skip invalid. |
| 16 | #370 Organization-level assistant definitions | 8 | Largest item. Decide: who authors, what a fork forks from, locking. Consider splitting at review. |
| 17 | #365 Re-check AI definitions on a local Ollama model | 2 | Hardware: Apple M3 Pro, 36 GB. No model yet: start with a 14B (`qwen3:14b`, ~9 GB at Q4), and try a 27B (Qwen 3.6 27B, ~17 GB at Q4) if it fits beside MySQL and Requel; final choice at the ticket's review. Run Ollama natively on macOS (Metal), not in the compose container, which gets no GPU on a Mac. Over the OpenAI-compatible endpoint Requel's schema is prompt-embedded, not enforced, so schema failures depend on the model; whether to use Ollama's enforced `format` is a review question. |

Rough total: about 48 days of work.

## Decisions needed before the ticket starts

| Issue | Decision |
|---|---|
| #345 | `CONFLICT` kind vs `INVALID_INPUT` for uniqueness and stale-version errors |
| #341 | Nested scenario on copy: deep copy or link |
| #349 | Actor check on a scenario's own name |
| #371 | Switches/data-handling travel; imported definitions switched off; fail vs skip |
| #370 | Author role; fork base; locking. Split? |
| #365 | Which local model (by the Mac's memory); native Ollama vs the compose container; enforced JSON schema or prompt-embedded |

## Release mechanics

- **Migrations:** planned work takes the next integer after `V38` on `release/2.1`. A 2.0.x
  hotfix that needs one takes `V38.1` on `master`.
- **Hotfixes during 2.1:** branch from `master`, then merge `master` forward into
  `release/2.1` with a merge commit (`RELEASE_PROCESS.md`, "Forward merges").
- **Release candidates:** `2.1.0-rc1` once every issue is closed or moved; acceptance findings
  go in one ticket per rc, as in 2.0.
- **Promotion:** `release/2.1` merges into `master` with a merge commit at `2.1.0`.
