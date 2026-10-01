# AI review evaluation

A fixture project, the findings a competent reviewer should raise on it, and a script that scores
Requel's AI review against them (issue #355). The score is the yardstick for the AI-assistant work
in epic #258: #260 must reproduce the baseline, and #261 and #263 report their change against it.
The baseline is in `doc/work/2.0/355-ai-eval-baseline.md`.

This is a dev tool. It needs a running Requel with an AI provider, it is not part of the build,
and CI never runs it. `FixtureExpectationsIT` is the only part CI runs: it imports the fixture and
checks every entity named in the expectations exists.

## Files

| File | What |
|---|---|
| `fixture-project.xml` | importable project XML: an invented community tool library |
| `expectations.json` | per entity, what a reviewer should raise |
| `score.py` | imports the fixture, reviews each entity, writes the report |
| `test_score.py` | unit tests for the matching and scoring: `python3 scripts/ai-eval/test_score.py` |

Every entity is synthetic. Do not add text from a real specification: this repository is public.

## Run it

1. Start Requel with an AI provider, for example the `cli` provider
   (`doc/guides/AI_ASSISTANT_SETUP.md`, section 10).
2. From the repository root:

   ```bash
   python3 scripts/ai-eval/score.py --label "cli/claude"
   ```

3. Read `target/ai-eval/<timestamp>/report.md`. `raw.json` beside it has every run's findings.

Useful options:

| Option | Default | What |
|---|---|---|
| `--base-url` | `http://localhost:8080` | the server |
| `--user`, `--password` | `admin` / `admin` (or `REQUEL_USER` / `REQUEL_PASSWORD`) | who to log in as |
| `--runs` | 3 | reviews per entity; the output is nondeterministic, so scores are rates |
| `--type Goal` | all | only these types; repeatable |
| `--timeout` | 300 | seconds to wait for one review |
| `--project NAME` | import a new one | re-score an already imported fixture |
| `--delete-after` | off | delete the imported project at the end |
| `--label` | none | free text in the report, e.g. the provider and model |
| `--rescore RAW_JSON` | off | no server: re-score a saved `raw.json` against the current expectations |

Each run imports the fixture under a new name (`AI Eval YYYYMMDD-HHMMSS`), so runs never collide.
Entities are reviewed one at a time. With `--runs 3` and the `cli` provider a full run takes
about an hour.

## How a run is scored

For each review of an entity the script posts `POST /api/ai/reviews`, then polls
`GET /api/ai/reviews` until a new run finishes, and scores the findings that run reported.

1. A finding **matches** an expected item when its text matches one of the item's `patterns`
   (case-insensitive regular expressions) and, if the item lists `types`, its `findingType` is
   one of them.
2. Each finding matches at most one item (first in file order), and each item counts once per run.
3. A finding that matches nothing is **spurious**. On a `silent` entity every finding is spurious.
4. On the `trap` entity, a finding or the run's summary matching a `confirmPatterns` entry counts
   as **confirmed**: the reviewer vouched for figures nobody checked.

| Column | Meaning |
|---|---|
| Hit rate | expected items matched / (expected items x successful runs) |
| Spurious per run | findings matching nothing, per successful run |
| Silent kept silent | successful runs on silent entities that raised nothing |
| Schema failures | runs that failed because the reply was not the output schema's shape |
| Other failures | runs that failed for any other reason (provider error, refusal) |
| Confirmed (trap) | runs on the trap that said its figures are right |
| Unverified evidence | findings citing text that is not in the entity (#260); the header also lists the `definition@version` the runs used |

A type the server can't review (GlossaryTerm, today) is reported as skipped.

Read the matches in the report's details before trusting a number: a pattern that matches the
wrong finding inflates the hit rate. Fix it in `expectations.json`, then re-score the saved
findings instead of reviewing again:

```bash
python3 scripts/ai-eval/score.py --rescore doc/work/2.0/355-ai-eval-baseline-raw.json
```

Re-score the baseline the same way whenever the patterns change, so a new run and the baseline
are always scored by the same patterns.

## The trap

`Halve the repair backlog` states its figures two ways that agree (32 = 24 + 8) and cites no
source. The right review asks where the figures come from. Saying they are consistent is worse than
saying nothing: consistency checking cannot detect a shared wrong premise.

## Changing the fixture

Edit `fixture-project.xml` and `expectations.json` together, keeping names identical.
`FixtureExpectationsIT` fails the build if an expected entity is missing, a type has no flawed or
no silent case, or there isn't exactly one trap. Re-run the baseline when a change alters what is
measured.
