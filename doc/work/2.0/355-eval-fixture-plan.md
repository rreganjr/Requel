# #355 Evaluation fixture, scoring script and baseline — implementation plan

Issue: https://github.com/rreganjr/Requel/issues/355 (child 5a of epic #258, third in its build order)
Branch: `355-eval-fixture`, cut from `release/2.0` @ `c8bea0fb`
Builds on: #259 (the `cli` provider, run FAILED on provider failure), #262 (redaction), #274 (project reads)

## Summary

A checked-in fixture project of deliberately flawed (and deliberately clean) entities, an
expectations file saying what a competent reviewer should raise on each, a small read that returns
a review run's outcome, and a Python scoring script that reviews every fixture entity a few times
through the real API and reports per-type scores. The first run of it against today's
`RequirementsReviewAssistant` on the `cli` provider is recorded as the baseline that #260, #261 and
#263 are measured against.

## Review against the tree (`release/2.0` @ `c8bea0fb`)

1. **Step is already reviewable.** `AiReviewService.REVIEWABLE_TYPES` is Goal, Story, Actor,
   UseCase, Scenario, Step. Only GlossaryTerm is not. The issue said otherwise (fixed).
2. **No read-back.** `POST /api/ai/reviews` returns a bare 202 (`AnalysisRequestDispatcher.dispatch`
   is `void`, submitted after commit since #259). Nothing exposes a run's status or findings.
3. **The model's run summary is dropped.** `assistant_runs` has `error_summary` but no result
   summary; the trap needs it. V30 adds one.
4. **Schema failures are already visible.** `ReviewResultMapper.parse/validate` throws
   `AiAnalysisException` on a non-JSON reply, missing `summary` or missing `findingType`; #259 turns
   that into a FAILED run with `error_summary` set. The script classifies those messages.
5. **Findings are stored per action.** Issue and note actions become `assistant_findings` rows with
   `finding_type`, `summary` (the issue or note text, ≤500 chars), `last_seen_run_id`. A finding with
   neither issue nor note text is dropped before it is stored and can't be scored.
6. **Import is scriptable.** `ImportProject` takes multipart (`input` + `file`) on `/api/commands`,
   accepts a `name` override, and leaves analysis off unless `enableAnalysis` is true, so importing
   the fixture starts no lexical runs. `DeleteProject` exists for cleanup.
7. **`findingType` is free-form today** (AMBIGUOUS, INCOMPLETE, UNTESTABLE, …), so matching on #263's
   vocabulary codes alone would score the baseline at about zero.

## Locked decisions (2026-10-01)

- **Read-back:** `GET /api/ai/reviews?entityType=&entityId=` returns the target's latest
  `REQUIREMENTS_REVIEW` run. The script records the time, posts, then polls.
- **Matching:** an expectations file beside the fixture XML. An expected finding lists keyword
  patterns and, optionally, accepted `findingType`s. Unmatched findings are spurious and listed in
  the report for review by eye.
- **Runs per entity:** 3 by default (`--runs`), scored as a hit rate.
- **Trap:** a finding flagging the figures as unsourced is a hit; a finding or the run summary saying
  they are consistent/correct is counted in a separate **confirmed** column.
- **Types:** seven in the fixture (adds Scenario); only GlossaryTerm reported as skipped.
- **Script:** Python 3, standard library only. A dev tool in `scripts/`, never in the jar, not run
  by CI.

## Contracts

### Read — `GET /api/ai/reviews?entityType=&entityId=`

Same type check and project-access rule as the POST (`AiReviewService`). 404 for an unknown entity,
403 without access, 400 for a non-reviewable type, 204 when the target has no review run yet.

```json
{
  "runId": "…", "status": "QUEUED|RUNNING|SUCCEEDED|FAILED",
  "createdAt": "…", "completedAt": "…", "latencyMs": 41234,
  "errorKind": null, "errorSummary": null,
  "summary": "model's one-paragraph summary, or null",
  "redactionCount": 0,
  "findings": [
    { "findingType": "AMBIGUOUS", "kind": "ISSUE|NOTE", "severity": "MEDIUM",
      "confidence": 0.8, "text": "…", "state": "OPEN", "annotationId": 6210 }
  ]
}
```

- "Latest" = newest `created_at` among runs with `task_type = REQUIREMENTS_REVIEW` for the target.
- `findings` = rows with `last_seen_run_id = runId` (everything that run reported, new or re-seen),
  issue and note kinds only.
- Read-only; no gateway/MCP exposure in this ticket (the gateway allowlist is commands).

### Run summary — V30

`ALTER TABLE assistant_runs ADD COLUMN result_summary VARCHAR(2000) NULL;` Written when a run
completes (SUCCEEDED or partial) from the `AssistantResult` summaries of its assistants, joined and
truncated like `error_summary`. `AssistantRunRecord` and both stores carry it.

### Fixture — `scripts/ai-eval/`

| File | What |
|---|---|
| `fixture-project.xml` | importable project XML, all entities synthetic |
| `expectations.json` | per entity: what a reviewer should raise |
| `score.py` | the scoring script |
| `test_score.py` | `unittest` for the matching and scoring logic, run by hand |
| `README.md` | how to run it, how to read the report |

Fixture domain: an invented "community tool-library" system (lending tools, members, late returns),
so nothing resembles a real specification. Roughly 35 entities:

| Type | Flawed cases | Clean (silent) |
|---|---|---|
| Goal | unmeasurable aspiration; one per #263 starting vocabulary entry (11) | 1 |
| Story | missing benefit clause; conjunctive story; the **trap** | 1 |
| UseCase | no precondition and no error path; actor is a named person | 1 |
| Scenario | no failure branch; step order contradicts the use case | 1 |
| Step | ambiguous actor ("the system or staff"); two actions in one step | 1 |
| Actor | named individual instead of a role; role defined only by exclusion | 1 |
| GlossaryTerm | circular definition; term defined by its own name (skipped) | 1 (skipped) |

The trap: a Story stating "the old review raised 32 findings (24 high, 8 low)" as justification,
with no source; the arithmetic agrees and the figures are unsourced.

### Expectations file

```json
{
  "fixture": "fixture-project.xml",
  "entities": [
    { "type": "Goal", "name": "Members love the library",
      "expect": [
        { "id": "goal-unmeasurable", "patterns": ["measur", "verif", "testab", "criteri"],
          "types": [] }
      ] },
    { "type": "Goal", "name": "Members can reserve a tool up to 7 days ahead",
      "silent": true, "expect": [] },
    { "type": "Story", "name": "Halve review findings", "trap": true,
      "expect": [ { "id": "trap-unsourced", "patterns": ["source", "cite", "where .* come", "unverif", "evidence"] } ],
      "confirmPatterns": ["consistent", "correct", "accurate", "adds up", "verified", "checks out"] }
  ]
}
```

Matching, per run of an entity:

1. A finding matches an expected item when its text matches one of the item's patterns
   (case-insensitive regex) and, if the item lists `types`, its `findingType` is one of them.
2. Each finding matches at most one item (first in file order); each item counts once per run.
3. A finding that matches no item is **spurious**. On a `silent` entity every finding is spurious.
4. On a `trap` entity, a finding or the run summary matching a `confirmPatterns` entry counts as
   **confirmed**.
5. An entity's name in the expectations must exist in the imported fixture with that type (the CI
   test enforces this).

### Script — `scripts/ai-eval/score.py`

```
python3 scripts/ai-eval/score.py [--base-url http://localhost:8080] [--user admin] [--password admin]
    [--runs 3] [--timeout 300] [--type Goal ...] [--out target/ai-eval] [--delete-after]
```

1. Log in (`/api/auth/login`; `REQUEL_USER` / `REQUEL_PASSWORD` env override the flags).
2. Import the fixture under a timestamped name (`AI Eval YYYYMMDD-HHMMSS`) so runs never collide.
3. Resolve each expectations entity to an id through the project reads.
4. For each reviewable entity × `--runs`: read the latest run id, POST, poll the GET every 2 s
   until a different run reaches a terminal status or `--timeout` passes. A POST answered 400
   marks the type skipped. Entities run one at a time
   (the `cli` provider is one process per call).
5. Score and write `report.md` and `raw.json` under `--out/<timestamp>/`. `--delete-after` deletes the
   imported project.

Report, per type: entities, runs, hit rate (hits ÷ (expected items × runs)), spurious per run,
silent kept silent (runs with zero findings ÷ runs on silent entities), schema failures, other
failures, timeouts, confirmed (trap). Then every spurious finding and every miss, with its text, for
review by eye. Schema failure = FAILED with `error_summary` starting "model reply" or "structured
output"; any other FAILED is "other".

### Baseline

`doc/work/2.0/355-ai-eval-baseline.md`: the date, commit, provider/model, `--runs`, the report's
per-type table, and short notes on what the misses and spurious findings look like. Run on your Mac
with the `cli` provider (claude), and codex too if time allows.

## Step by step

1. V30 migration, `AssistantRunEntity.resultSummary`, `AssistantRunRecord`, both stores, set on
   completion in `AssistantRunWorker`.
2. Store read: latest run for target + task type; findings by `last_seen_run_id` (JPA + InMemory).
3. `AiReviewService.latestReview` + `AiReviewController` GET + `AiReviewDto` records.
4. Fixture XML + expectations (written by hand, imported locally to check).
5. `FixtureExpectationsIT`: import the XML through `ImportProject`, assert every expectations entity
   exists by type and name, and every type has at least one flawed and one silent case.
6. `score.py`, `test_score.py`, `README.md`.
7. Run the baseline on your Mac; record it.
8. Doc: a "Measuring review quality" section in `doc/guides/AI_ASSISTANT_SETUP.md` pointing at the
   README.

## Test plan

- `AiReviewControllerIT` (H2, fake CLI from `CliProviderReviewIT`): POST then GET returns the
  SUCCEEDED run with its summary and findings; a second run makes GET return the newer run with only
  its findings; 204 with no run; 404 unknown id; 400 GlossaryTerm; 403 for a non-stakeholder
  (`AuthorizationIT`); FAILED run returns `errorSummary`; lexical runs on the target are ignored.
- Store tests: `resultSummary` round-trip and truncation; latest-run selection by task type.
- `FixtureExpectationsIT` as above.
- `test_score.py`: pattern + type matching, one finding per item, spurious on silent entities,
  confirmed on the trap, schema-failure classification, hit-rate arithmetic.
- `mvn verify` needs no provider.

## Out of scope

- Per-type definitions, prompt tuning, extending `REVIEWABLE_TYPES` (#263).
- Returning a run id from the POST (would change `AnalysisRequestDispatcher`'s contract).
- A UI for runs, an MCP read for runs, CI quality gates.

## Risks

- **Pattern matching is generous.** A pattern like "measur" can match an unrelated finding. The
  report lists every match so a bad pattern shows up; patterns get tightened as #263 tunes.
- **Wall time.** About 30 reviewable entities × 3 runs at 30–60 s per CLI call is roughly an hour.
  `--type` runs a subset.
- **Polling race.** Clock skew between the script and server can't occur (same machine), but a
  lexical run can land on the target; the GET filters by task type.
- **Fixture drift.** Renaming a fixture entity without updating the expectations fails CI, by design.

## AC mapping

| AC | Where |
|---|---|
| Fixture imports, seven types, negatives, trap | `fixture-project.xml`, `FixtureExpectationsIT` |
| Synthetic only | invented domain; reviewed in PR |
| GET returns latest run | `AiReviewController`, `AiReviewControllerIT` |
| Script end to end, per-type report | `score.py`, `test_score.py` |
| Baseline recorded | `355-ai-eval-baseline.md` |
| Not CI-gated; `mvn verify` needs no provider | script outside the build; IT uses the fake CLI |

## Implementation notes (2026-10-01)

- **Polling by run id, not time.** The script reads the latest run id before posting and waits for
  a different one, so it never compares its clock with the server's.
- **`created_at` gets microseconds (V30).** It was whole seconds, so two runs queued in the same
  second tied and "latest" fell to the random UUID; an end-to-end run with an instant fake CLI hung
  on exactly that. `TIMESTAMP(6)` makes the order real.
- **No provider or model in the read.** `assistant_runs.provider` / `model` are never written
  (they're on the usage rows), so the read would always return null. Dropped; `--label` records
  the provider in the report.
- **Folded in: the AI review controller's 400/403 were 500s.** `ApiExceptionHandler` is scoped to
  `com.rreganjr.requel.service`, so it never handled `AiReviewController` (in `com.rreganjr.requel.ai`).
  The controller now maps `AuthorizationException` → 403 and `IllegalArgumentException` → 400 for
  both the POST and the GET; `AuthorizationIT.reviewReadNeedsProjectAccess` covers both.
- **Folded in: flaky `RedactionEgressIT`.** A goal name carried a 13-digit millisecond time, which
  is Luhn-valid one time in ten and was masked as a card, so the redaction count was sometimes 2.
  The name now uses base 36.
- **Kind from the action key.** `assistant_findings` has no kind column; the read takes ISSUE/NOTE
  from the AI action key (`…:issue:…` / `…:note:…`).
- **Fixture:** 34 entities (14 goals including the duplicate pair, 5 stories including the trap,
  3 use cases, 3 scenarios, 3 steps, 3 actors, 3 glossary terms). The steps sit in a fourth,
  unscored scenario ("Log a broken tool").
- **Smoke-tested end to end** in the cloud sandbox against MySQL 8 with a fake CLI: import,
  name → id resolution (steps through the scenario reads), review, poll, report, `--project`,
  `--delete-after`, GlossaryTerm skipped.
- **`--rescore`.** The first baseline scoring showed generous patterns making false hits (72%).
  `score.py --rescore raw.json` re-scores saved findings against the current expectations with no
  server, so the patterns were tightened and the same 93 reviews re-scored (65%). The original
  raw findings are committed as `doc/work/2.0/355-ai-eval-baseline-raw.json` so later tickets
  re-score the baseline with their own patterns and compare like with like.
- **Baseline recorded** in `doc/work/2.0/355-ai-eval-baseline.md` (cli/claude, 3 runs, 93/93
  succeeded, 65% hit rate, 4.2 spurious per run, 0/18 silent runs quiet, trap confirmed once).

