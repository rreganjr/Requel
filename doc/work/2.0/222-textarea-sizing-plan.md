# #222 — Textarea sizing default — plan

Ticket: [#222](https://github.com/rreganjr/Requel/issues/222) (I5, Phase 2 of the
Post-#124 UI polish epic #219). Punch-list item **F1**. Branch: `222-textarea-sizing`
off `release/2.0`. Frontend-only.

## Scope / locked decisions

- **Goal (F1):** form textareas render narrow/short and always have to be hand-resized; `rows`
  is inconsistent (2/4/5/6/8) and full-width is set per-editor, so some fall back to the narrow
  browser default. Standardize textarea defaults centrally.
- **Height behavior (confirmed): auto-grow.** Prose textareas get PrimeNG `[autoResize]="true"`
  — start at a consistent height, grow with content, no manual resize needed. All textareas get
  full width.

## Changes

### Global (the central default) — `src/styles.scss`

Add one rule for the app's textareas (PrimeNG stamps `p-textarea` on every `pTextarea`):
```
textarea[pTextarea] {
  width: 100%;
  box-sizing: border-box;
  min-height: <comfortable floor, ~ 4-row equivalent>;
}
```
This makes every textarea full width with a consistent floor, replacing the per-editor width
rules and fixing the ones that currently fall back to the narrow default. `min-height` is a
floor under autoResize (and gives the non-autoResize XSLT field a sane minimum too).

### Prose editor textareas — add auto-grow + normalize the initial height

Add `[autoResize]="true"` and normalize `rows` to a single default (proposed **4**) on the
prose description/text textareas: goal, project (already autoResize; normalize rows), stakeholder,
use-case, story, actor, scenario (×2 — details + step-edit), term. With autoResize the field
grows as you type, so a uniform small start is fine.

### Left as-is

- **report-editor `.xslt-textarea`** (rows=20, monospace "Paste XSLT here") — a code field, NOT
  prose. No autoResize (it shouldn't grow unbounded); it still picks up the global full-width.
- **annotations quick-add textareas** (note/issue, rows=2): add `[autoResize]="true"` for
  consistency but keep the compact rows=2 start; global rule already gives them full width (drop
  the now-redundant local `.add-textarea { width:100% }`).
- Per-editor input/select width rules (e.g. term-editor's `app-field input/textarea/p-select`)
  are left alone — input/select sizing is out of scope for F1; only the textarea part becomes
  redundant and is harmless.

## Test plan (the verify gate)

- **Unit:** existing editor specs stay green (rows/autoResize are attributes, not asserted).
  Add a light spec only if a shared component gains logic — none expected (this is CSS + template
  attrs), so likely no new unit test; rely on typecheck + build + visual.
- **Typecheck:** `tsc` app + spec.
- **Dev build (AOT):** compiles all editors.
- **Visual (the real proof):** open a couple of editors (goal, term, project) — textareas are full
  width, start at a consistent comfortable height, and grow as you type; the XSLT field on a report
  is unchanged; annotation quick-add is full width.
- **e2e:** editor flows run in CI; textarea attribute changes are low-risk for locators.

## Out of scope

- Input/select width normalization (F1 is textareas). The broader app-field control-width sweep
  can be its own follow-up if desired.

## Risks

- Global `textarea[pTextarea]` width must win over PrimeNG's theme width — confirm in-browser; bump
  specificity only if needed.
- autoResize + a CSS `min-height` can fight if min-height is large; keep the floor modest.
- Low overall: template-attr + one global CSS rule, no logic.

## AC mapping

- Textareas default to full width with a comfortable start, no manual resize → global rule +
  autoResize (visual).
- Consistent across editors → single `rows` default + one global rule; per-editor divergence removed.
