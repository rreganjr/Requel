# #266 Corpus analyses: final eval

`score.py --corpus --runs 3` on the eval fixture, 2026-10-03, cli/claude, the shipped
`ai-corpus-relationships` (version 1). Raw runs: `266-ai-eval-final-raw.json`. Tuning:
`266-tuning-log.md`.

## Summary

- **Find overlaps** flags both planted number conflicts in every run and none of the pairs that
  must stay silent: 7 pairs per run on 53 entities.
- **The AI analysis** raises 78% of the planted relationships (14/18): four of the six in every
  run. It raises nothing on the silent pairs, and it never duplicates an issue across runs. All
  its evidence was found in the participants' text, and no run was partial.
- The one finding counted spurious per three runs is defensible: the reservations goal and the
  reserve-and-pay story say the same thing at two levels, without a link.

## Find overlaps (no AI)

| Expected pairs found | Silent pairs raised | Pairs per run |
|---|---|---|
| 100% (6/6) | 0 | 7.0 |

## Analysis (AI)

| Runs ok/total | Hit rate | Exact participants | Spurious per run | Also valid per run | Silent pairs raised | Duplicate issues | Unverified evidence | Partial runs |
|---|---|---|---|---|---|---|---|---|
| 3/3 | 78% (14/18) | 79% (11/14) | 0.3 | 3.3 | 0 | 0 | 0 | 0 |

| Expectation | Hits |
|---|---|
| contradiction-loan-length (14 vs 21 days) | 3/3 |
| renewal-breaks-loan-length (a renewal past the goal's 14 days) | 3/3 |
| hold-two-meanings | 3/3 |
| non-staff-member | 3/3 |
| sms-vs-privacy | 1/3 |
| duplicate-search | 1/3 |

- **Not exact:** the renewal finding names the goal, the use case, the renewal step and the patron
  story. Every one is part of it; the expectation names two.
- **Also valid** (real, not targeted): checkout never checks the suspended-member rule; "suspended"
  and "on hold" as two names for one account state; a hold, a reservation and a pickup date as one
  concept; the patron story repeating the renewal use case; after-hours returns against the
  late-returns flow.
- **The misses share no wording** with their pair: SMS reminders against member privacy is
  debatable (only the carrier is outside the library), and the two search goals are a capability
  and a speed target. Both come from the index alone, one run in three.

## Against the acceptance criteria

| Criterion | Where |
|---|---|
| A contradiction between two well-formed requirements, naming both | 3/3 runs (contradiction-loan-length, exact); `CorpusAnalysisIT` |
| A term with two meanings raised once | 3/3 runs (hold-two-meanings, one issue); `CorpusAnalysisIT` (three participants, one issue) |
| Every finding names two or more refs that resolve | `CorpusAnalysisIT` (strays and single participants dropped); the read lists each participant |
| No duplicates on an unchanged re-run | 0 duplicate issues; `CorpusFinderIT`, `CorpusAnalysisIT` |
| Over-budget index refused with the overflow named | `CorpusPackBuilderTest`, `CorpusAnalysisIT` |
| Never from the post-edit path | `CorpusFinderIT.anEditNeverStartsACorpusRun`; the registry matches corpus assistants by task |
| Review and corpus findings coexist | `CorpusAnalysisIT.corpusAndReviewFindingsCoexistAndTheFindersIssueIsReplaced` |
| One issue on every participant, stale on all | `CorpusFinderIT`, `FindingFreshnessTest` |
| Find overlaps flags the number conflict and duplicate goals | number conflicts 6/6; the duplicate goals score 0.25, below Find overlaps' 0.35 and sent to the AI analysis instead (checkpoint) |
