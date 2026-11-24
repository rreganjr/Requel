# #527 — Definition editor: field rows, readable finding types, no overflow when narrow

https://github.com/rreganjr/Requel/issues/527

Branch `527-definition-editor` from `release/2.2`, PR to `release/2.2`. Size 2.

## Summary

Found in the #519 UI review (F21): the AI definition editor put its labels above the fields while
the other editors use the label-column rows of `app-field`; each finding type was two single-line
inputs, so a long code (`DUPLICATE_AT_DIFFERENT_ABSTRACTION`) and every description were cut off;
and on a narrow window the finding-type rows ran past the right edge of the card.

## Locked decisions (2026-11-24)

- **Single fields use `app-field`.** Kind and Key (new definition), Name and Instructions get the
  shared label column, width and dividers. Server errors stay under their control inside the row.
- **Checkbox groups keep `fieldset` + `legend`.** Entity types / Sets, Context it reads, Finding
  types and the new "Where it runs" group (Local only) have no single control for a `<label for>`,
  so they stay fieldsets, drawn without the box: a divider below and the legend in label styling,
  so they read as rows of the same form.
- **Finding-type description is a textarea between two and six lines.** It starts at two lines
  (`rows="2"`, `min-height: 3.5em`) even when the text is short, grows with the text
  (`autoResize`) up to `max-height: 9em` (about six lines), then scrolls inside the box; the
  reader can still drag it taller (`resize: vertical`).
- **Finding-type code is monospace and wide enough for the longest bundled code** (34 characters
  plus the input's padding). On a narrow window the row wraps: code above, description below.
- The instructions character count moves under the textarea ("N / max characters").
- The form takes the shared editor width (`--rq-editor-max`) instead of its own 52rem.

## Steps

1. `project-definition-editor.ts`: import `AppFieldComponent`/`AppFieldControlDirective`; wrap
   Kind, Key, Name and Instructions in `<app-field label=… controlId=…>` (the controls keep their
   own ids, so `controlId` points the label at them); finding-type row becomes `input.vocab-type`
   + `textarea.vocab-desc` with `aria-label`s and a `vocab-desc-<i>` test id; Local only moves into
   a "Where it runs" fieldset; styles as above.

## Test plan

- Spec: the `app-field` labels are Name and Instructions and the Name label's `for` resolves to the
  name input; the first finding description is a two-row textarea holding its text; "Finding
  types" is still a legend.
- Spec: no axe violations on a populated definition (this caught the label `for` mismatch before
  `controlId` was passed).
- Rendered check (dev build, mocked API, headless Chrome at 1440 and 390 wide): short descriptions
  are two lines high, a 400-word one stops at the six-line cap and scrolls, the longest code shows
  in full, nothing extends past the card.
- Whole unit suite, typecheck, development build. No e2e covers this page.

## Out of scope

- The page's "All AI definitions" link and intro text styling.
- A shared component for checkbox groups; four fieldsets on one page don't justify it yet.

## Acceptance criteria → where

| AC | Covered by |
|---|---|
| Labels sit in the label column like the other editors | Step 1 (`app-field`); spec |
| Finding-type code and description are readable in full | Monospace code sized to the longest code; description textarea, 2–6 lines then scroll |
| Rows don't overflow the card on a narrow window | Wrapping row with `min-width: 0`; rendered check at 390 wide |
| Existing tests pass | Unit suite |
