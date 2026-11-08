# #478 — rc2: one button on a step row: the pencil, or the triangle when it has issues

https://github.com/rreganjr/Requel/issues/478

Branch `478-step-row-button` from `release/2.1` (at `2.1.0-rc2`), PR to `release/2.1`. Size 1.
Found in the rc2 acceptance test (#420); the fix ships in rc3.

## Summary

#476 put a warning triangle beside the pencil on a step row with open issues. Both open the step
page, so the row has two controls for one thing. A plain step's row now shows one: the pencil
when the step has no open issues, the triangle when it has.

## Locked decisions (2026-11-06)

1. **The triangle replaces the pencil** on a saved plain step with open issues:
   - It is colored by the highest open severity, as now.
   - Its tooltip and accessible name give the count and that severity ("2 open issues, highest
     MEDIUM").
   - It opens the step page.
2. **It follows the pencil's rules.** It is disabled while the scenario has unsaved changes, with
   the tooltip "Save the scenario first".
3. **Without Scenario edit permission** there is no pencil, but the triangle still shows and opens
   the step page read-only. A read-only user can't make the scenario dirty, so it is never
   disabled for them.
4. **Sub-scenario rows are unchanged.** They have no pencil, so their triangle stays beside the
   name, as a link to the sub-scenario's page.
5. **A new, unsaved step** has no issues, so it always shows the pencil, which opens the popup.

## Changes

requel-angular
- `scenario-editor.ts`:
  - On a plain row, the triangle is a `p-button` in the pencil's place (`data-testid`
    `scenario-step-issues`, `data-severity`), with the pencil's click handler and disabled rule.
    Its color comes from the button's `severity`, mapped from the issue tone by a new
    `issueButtonSeverity` (danger → `danger`, warning → `warn`, info → `info`). A component
    style can't reach the button inside PrimeNG's own view.
  - The pencil shows only when the row has no open issues.
  - The sub-scenario row keeps the `#stepIssuesIcon` link.
- `scenario-editor.spec.ts`:
  - A row with issues has the triangle and no pencil.
  - A clean row has the pencil and no triangle.
  - The triangle navigates to the step page, and is disabled while the scenario is dirty.
  - The triangle shows without edit permission.
  - The sub-scenario row keeps its link.
- `e2e/step-page-476.e2e.ts`:
  - The first test clicks the triangle, and checks the row has no pencil.
  - The dirty test takes whichever of the two buttons the row shows, since analysis can put an
    issue on the step.

website
- `guide/requirements.html`: the triangle takes the pencil's place.

## Test plan

`tmp/478-verify.sh` runs:
- the Angular typechecks;
- a development build;
- the full Angular unit suite.

No backend change. CI runs the e2e.

## Out of scope

- The step page itself, and Open Issues.

## Risks

- **Analysis can add an issue to a step after a test's page load.** The row reflects the last
  scenario read, so a test that needs the pencil takes either button.

## AC mapping

| AC | Covered by |
|---|---|
| A clean saved step shows the pencil only | scenario-editor.spec |
| A step with issues shows the triangle only, colored, with the count; it opens the step page | scenario-editor.spec, e2e step-page-476 |
| The triangle is disabled while the scenario is dirty | scenario-editor.spec, e2e step-page-476 |
| A new step shows the pencil, which opens the popup | scenario-editor.spec, scenarios.e2e |
| A sub-scenario row keeps its triangle | scenario-editor.spec |
| Specs and e2e cover the one-button row | the above |
