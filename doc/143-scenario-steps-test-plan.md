# Manual smoke test — #143 scenario step-state → FormArray

Companion to `doc/143-scenario-steps-formarray-plan.md`. Covers what automation only
approximates (real drag feel, SSE-during-typing, the native unsaved-changes dialog) and acts as
the pre-/post-refactor parity gate. Run against a local dev stack. No QA team — this is the
human pass before the PR merges.

Automated coverage already in place (don't re-do by hand):
- Unit (`scenario-editor.spec.ts`): add / remove / reorder / inline-edit / dialog-apply /
  id-backfill / SSE-keeps-node / 409-keeps-edit / empty-name-submitted. Run: `bash tmp/143-verify.sh`.
- e2e (`e2e/scenario-steps-143.e2e.ts`): drag-reorder persistence + Back-guard cancel.
  Run: `bash tmp/143-verify.sh --e2e`.

## Gate — run once BEFORE the refactor, once AFTER; behavior must match

| # | Flow | Steps | Expected (unchanged by #143) |
|---|------|-------|------------------------------|
| 1 | Create w/ steps | New Scenario → name + type → Next → add 3 steps, name them → Done | Scenario saved; all 3 steps present in order; no empty-name step silently created |
| 2 | Inline edit | Open a saved scenario → edit a step name inline → Tab out | Save enables; Back now prompts (see #6) |
| 3 | Dialog edit | Open step ✎ dialog → change name/type/notes → Apply → Save → reload | Edited values persist; dialog is scratch-only until Apply |
| 4 | Reorder | Drag a step by its ⠿ handle above another → Save → reload | New order persists; ids not swapped/duplicated |
| 5 | Add-below / add-at | Use +below on a middle step; +top | New blank step lands in the right position; Save persists order |
| 6 | Unsaved guard | Make any step change → click Back / navigate away | Native confirm "You have unsaved changes…" — Cancel keeps edit & page; OK discards & leaves |
| 7 | Empty name (AFTER only) | Add a step, leave name blank → try Save | **Changes with #143:** Save disabled / step name shows required error (today it saves blank) |
| 8 | Sub-scenario ref | Add sub-scenario via selector | Row renders as a link (not an input); Save persists; not counted as an editable-name step |

## Cross-cutting / race cases (the reason FormArray reconciliation is delicate)

- **SSE during create refetch:** create a scenario with a new step; while the post-save refetch
  runs, immediately keep typing a second step. The typed node must survive and later get its
  server id (no duplicate on the next Save). (Unit-pinned, but eyeball once for real timing.)
- **SSE while dialog open:** open the step ✎ dialog on one client; edit the scenario from another
  tab. The open dialog must not lose its node / jump. (Unit-pinned.)
- **Reorder-then-immediate-save before refetch:** reorder, Save, and confirm no step takes the
  wrong id (the known position-match gap — see plan §4; behavior must be *no worse* than today).
- **Guard false-positives:** open a scenario, change nothing, click Back → no prompt.

## Regression sweep (adjacent, untouched, must still pass)

- `use-case-editor`: create/edit still works after the dead-field removal (AC-5).
- Other editors' Save/guard behavior unchanged (goal, story, actor, user, settings).
- `bash tmp/143-verify.sh` green; full `npx ng test` green; `npx playwright test e2e/scenarios.e2e.ts` green.
