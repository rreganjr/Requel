# AI review evaluation

A fixture project, the findings a competent reviewer should raise on it, and a script that scores
Requel's AI review against them (issue #355). The score is the yardstick for the AI-assistant work
in epic #258: #260 must reproduce the baseline, and #261 and #263 report their change against it.
The baseline is in `doc/work/2.0/355-ai-eval-baseline.md`; the per-type definitions' result
(#263) is in `doc/work/2.0/263-ai-eval-final.md`.

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

### Tuning the review definitions without a rebuild (#263)

Each reviewable type has a bundled definition in
`modules/assistant-ai/src/main/resources/ai/definitions/`. To tune them:

1. Copy the files to a working directory, e.g. `tmp/263-defs/`.
2. Start Requel with `--requel.ai.definitions.dir=<that directory>` (dev only). At startup, after
   seeding, the files there replace the bundled definitions with the same keys, whatever their
   version.
3. Edit a file, then run the script with `--reload-definitions`: it calls
   `POST /api/dev/ai/definitions/reload` (present only when the property is set) before
   scoring. An invalid file is refused with its problems listed, and nothing changes.
4. Copy the tuned files back to the resource directory.

A start without the property puts the shipped files back over any dev override.

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
| `--reload-definitions` | off | first reload `requel.ai.definitions.dir` (#263, dev only) |

Each run imports the fixture under a new name (`AI Eval YYYYMMDD-HHMMSS`), so runs never collide.
Entities are reviewed one at a time. With `--runs 3` and the `cli` provider a full run takes
about an hour.

## How a run is scored

For each review of an entity the script posts `POST /api/ai/reviews`, then polls
`GET /api/ai/reviews` until a new run finishes, and scores the findings that run reported.

1. A finding **matches** an expected item when its text matches one of the item's `patterns`
   (case-insensitive regular expressions) or its `findingType` is one of the item's
   `acceptTypes` (#263), and, if the item lists `types`, its `findingType` is one of them.
2. Findings of an accepted type claim their items first, then the rest match on wording, so a
   finding that only shares words with an item can't take it from the finding that names the
   flaw (#263).
3. Each finding matches at most one item (first in file order), and each item counts once per run.
4. A finding left over that matches one of the entity's `alsoValid` items (same rules) is **also
   valid**: a real flaw the fixture doesn't target. It is neither a hit nor spurious (#263).
5. A leftover extraction finding (`EXTRACT_*`) is a **suggestion**: advisory, not spurious (#263).
6. Anything else is **spurious**. On a `silent` entity everything except suggestions and also
   valid findings is spurious.
7. **Policies (#265).** A review request also dispatches the policy pass when a policy applies;
   the script waits for it and scores its findings against the entity's `policyExpect` items
   (same matching). An entity without `policyExpect` is policy-silent; its `policyAlsoValid`
   items are real flaws the policy may raise without breaking that. The report adds a
   "Policies" table.
8. On the `trap` entity, a finding or the run's summary matching a `confirmPatterns` entry counts
   as **confirmed**: the reviewer vouched for figures nobody checked. A text that also matches the
   trap item's patterns ("the figures add up, but no source is given") questions the figures and
   does not confirm (#263).

| Column | Meaning |
|---|---|
| Hit rate | expected items matched / (expected items x successful runs) |
| Spurious per run | findings matching nothing, per successful run |
| Silent kept silent | successful runs on silent entities with no hit and nothing spurious |
| Schema failures | runs that failed because the reply was not the output schema's shape |
| Other failures | runs that failed for any other reason (provider error, refusal) |
| Confirmed (trap) | runs on the trap that said its figures are right |
| Unverified evidence | findings citing text that is not in what the model was sent: the entity and its context sections (#260, widened in #263); the header also lists the `definition@version` the runs used |
| Off-vocabulary types | findings whose type is not in the definition's vocabulary (#263) |
| Suggestions per run | extraction findings that matched no expected item (#263) |
| Also valid per run | findings matching an `alsoValid` item (#263) |

A type the server can't review is reported as skipped.

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

## Corpus analyses (#266)

`python3 scripts/ai-eval/score.py --corpus --runs 3 --label "cli/claude"` runs Find overlaps and
then the AI corpus analysis over the whole fixture project, `--runs` times, and writes
`target/ai-eval/corpus-<timestamp>/report.md` and `raw.json`.

`expectations.json` has a `corpus` section:

| Key | What |
|---|---|
| `expect` | relationships the analysis should raise: `participants` (type and name of each), `acceptTypes`, and `finder: true` when Find overlaps should flag the pair too |
| `alsoValid` | real relationships the fixture doesn't target; a match is neither a hit nor spurious |
| `silent` | pairs that must not appear together in a finding |

A relationship is all the findings of a run that share one issue. It hits an expectation when it
names every expected participant with an accepted type; it is *exact* when it names no others.
The report also counts *duplicate issues*: a relationship two consecutive runs reported under
different issues, which idempotency should prevent. `--rescore` works on a corpus `raw.json` too.

