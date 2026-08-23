# Implementation Plan — #134 3.3 Mini-form validation contract

Part of the UI/UX remediation epic **#124**. Source: `doc/UI_UX_REVIEW.md` Finding 3.3. Phase 3.
Branch: `134-mini-form-validation` off `release/2.0` (after #200 merged).
Blocked by #132, #133 — both merged. Priority: Medium, effort Medium (4-6 days).

> Like #133/#138, part of 3.3 is already delivered; §5 drafts the narrowed issue body/AC
> (posted separately for approval).

## 0. Already delivered — NOT in scope
- Inline error *surfacing* for tag-selector and annotations-section (server/action errors now
  render inline via `app-submit-error`, cleared on reload) — #133.
- Fieldset/legend grouping + per-input `aria-label`s on the mini-form add-rows — #138.
- The validation contract itself (`app-field`: required marker, `aria-invalid`,
  `aria-describedby`, error message via `form-errors.ts`) — #158/#132.

## 1. Scope (locked decisions)
1. **Convert all mini-forms from `[(ngModel)]` to reactive forms** (FormGroup + validators) so
   they carry a real validation contract.
2. **Use `app-field`** for the vertical dialog / textarea forms (it renders a visible label +
   the error/aria wiring for free): api-tokens create, scenario-selector create, annotations
   note & issue.
3. **Compact inline add-rows keep their #138 layout** (fieldset + inline row) and drive the
   *same* contract from the reactive control — `aria-invalid` + `aria-describedby` on the input
   and an inline required message from the shared `form-errors` messages — without app-field's
   two-column row chrome. Covers tag-selector, global-tags, tag-categories, and the annotation
   position/argument inline inputs.
4. **Dialog create-errors render inside the dialog via `app-submit-error`**: move the
   api-tokens create error in from the parent message-slot; standardize scenario-selector's
   `<p class="create-error">` to `app-submit-error`.
5. **Narrow the issue AC** and note what #133/#138/#158 already delivered.

## 2. The inline contract (for compact add-rows, decision 3)
For a required control on a compact add-row:
- bind `[formControl]`, `[attr.aria-invalid]="submitted() && ctrl.invalid"`,
  `[attr.aria-describedby]="ctrl.invalid ? errId : null"`;
- render `@if (submitted() && ctrl.invalid) { <p [id]="errId" class="app-field-error"
  role="alert">{{ requiredMessage }}</p> }` beside the control;
- reuse `resolveErrorMessage` / `DEFAULT_FORM_ERRORS` from `form-errors.ts` so the wording
  ("… is required") matches app-field, rather than hard-coding strings;
- a `submitted` signal per form set true in the save handler; blank submit no longer returns
  silently — it marks the control touched, sets `submitted`, and shows the message.
If this repeats more than ~3 times, extract a tiny `app-inline-field` directive that stamps the
aria wiring (mirroring `appFieldControl`); decide during Step 1.

## 3. Inventory & per-surface approach

| Surface | Form → | Required field(s) | Approach |
|---|---|---|---|
| `tag-selector` add-row | reactive `{category, value}` | value | inline contract; keep fieldset |
| `global-tags` add-row | reactive `{category, value}` | value | inline contract |
| `tag-categories` add-row | reactive `{name, exclusive, allowedTypes, values, color}` | name | inline contract |
| `annotations` note form | reactive `{text}` | text | app-field (textarea) |
| `annotations` issue form | reactive `{text, mustResolve}` | text | app-field (textarea) + checkbox |
| `annotations` position/argument | reactive `{text[, supportLevel]}` | text | inline contract |
| `scenario-selector-dialog` create | reactive `{name}` | name | app-field; error → app-submit-error in dialog |
| `api-tokens` create dialog | reactive `{name}` | name | app-field; **move** error into the dialog via app-submit-error |

Server/action errors already shown by #133 stay; this adds *client-side required* validation
and, for the two dialogs, relocates/standardizes the create error.

## 4. Step-by-step (each step its own PR to `release/2.0`, squash-merged)
- **Step 1 — pattern + tag-selector.** Establish the inline contract on the tag-selector
  add-row (value required, inline message, aria wiring); extract a helper/directive if
  warranted. Unit + a11y specs.
- **Step 2 — admin add-rows.** global-tags, tag-categories reactive + inline required on the
  key field.
- **Step 3 — annotations-section.** note/issue via app-field; position/argument inline. Keep
  the existing success toasts and #133 action-error banner.
- **Step 4 — dialogs.** scenario-selector-dialog and api-tokens create forms via app-field +
  `app-submit-error` inside the dialog; move the PAT error out of the parent slot.
- **Step 5 — tests sweep + AC.** Cross-surface a11y assertions; post the revised #134 AC.

## 5. Revised #134 issue body / AC
Posted to https://github.com/rreganjr/Requel/issues/134 (body + scope comment). AC narrows to:
reactive mini-forms with a required-on-blank contract (touched + inline message via
aria-describedby / aria-invalid); vertical forms use app-field, compact rows use the inline
contract; dialog create-errors render inside the dialog via app-submit-error. Delivered-
elsewhere items (error surfacing #133, fieldsets/labels #138, the app-field contract #158/#132)
are called out as done.

## 6. Risks / notes
- app-field imposes a labelled two-column row, so it is used only where a vertical labelled
  field is the right shape; the compact toolbars keep the #138 look (decision 3) — this is the
  reconciliation of "reactive + app-field" with the compact add-row design.
- annotation note/issue currently sit in a fieldset with a visually-hidden legend (#138); when
  app-field adds a visible label, drop the now-redundant legend (keep the fieldset only where it
  groups >1 control, e.g. the issue text + mustResolve checkbox).
