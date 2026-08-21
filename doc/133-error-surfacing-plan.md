# Implementation Plan — #133 3.2 Consistent API & command error surfacing

Part of the UI/UX remediation epic **#124**. Source: `doc/UI_UX_REVIEW.md` Finding 3.2. Phase 3.
Branch: `133-error-surfacing` off `release/2.0` at `3f7e6fa`.
Follows **#132** (reactive forms + `applyCommandErrors`) and **#176** (DTO field-name violations),
both merged. #133 blocks **#138** (4.4 label/error associations) and #134 (3.3 mini-forms).

> Status of the ticket body: the #133 AC was written before #132/#176 landed and is partly
> stale — it still asks for "a shared command-error adapter." That adapter now exists. This plan
> narrows #133 to the render/announcement policy, and §7 drafts the corrected issue body for
> approval before it is posted.

## 0. What #132 / #176 already delivered — NOT in scope here

- `applyCommandErrors(form, violations, map?)` and `clearServerErrors(root)` live in
  `src/app/shared/form-errors.ts` and are already called by all eleven editors
  (login, project, term, stakeholder, use-case, edit-account, user, actor, scenario, report).
- Field violations map to per-control `{ server }` errors and clear on edit (validator-based).
- #176 made `CommandController` emit input-DTO field names, so the per-editor name maps are gone.
- The old semicolon-concatenation of violations (project/user/edit-account) is already removed.

So field-violation → control mapping is done. #133 is about **where and how errors render and
are announced**, and making that uniform.

## 1. The policy (what "consistent" means)

**Decision: all form/command feedback renders inline. Toasts are not used for form, command, or
nested-action feedback.** (Chosen over the review's "toasts for confirmations" wording: inline
messages stay put, don't auto-dismiss, and are easier for screen-reader/keyboard users.)

1. **Blocking errors** — a failed submit/command, or a page/list action that failed — render
   **inline** as a `role="alert"` region (assertive) that persists until resolved or dismissed.
2. **Field-level violations** render inline under the field (already handled by `app-field` +
   `applyCommandErrors`). Unchanged.
3. **Success / info confirmations** render **inline** as a polite region (`role="status"`,
   `aria-live="polite"`). Inline success `p-message` sites stay inline; nested success **toasts**
   are converted to inline (see §3).
4. **Network failures** (`CommandService.handleError` → `error: 'Network error'`, no `status`)
   render a **retryable** inline alert (the alert exposes a Retry action).
5. **409 optimistic-lock** keeps its existing dedicated path (it already carries `status: 409`);
   not re-touched here.

Net effect: `<p-toast>` in `layout.ts` is left in place for any genuinely transient/background
notice, but no form/command/nested-action path uses it after this ticket.

## 2. New shared component — `app-submit-error`

A small inline alert primitive, sibling to `error-state.ts` (#131) but for a different job:
`error-state` **replaces a whole panel** when a load fails; `app-submit-error` is a **banner that
coexists with a still-usable form/list**.

```
selector: app-submit-error
@Input() message: string | null      // null => nothing rendered
@Input() retryable = false           // true => show Retry (network failures)
@Input() retryLabel = 'Retry'
@Input() testid = 'submit-error'
@Output() retry = new EventEmitter<void>()
host/template: role="alert" (assertive), error styling consistent with error-state
```

- Mirrors `error-state`'s a11y conventions (role, icon, testid) and can share its SCSS tokens.
- Deliberately separate from `error-state` (decision: shared inline-alert component, not an
  extension) so load-failure vs submit-failure stay semantically distinct and independently
  testable.
- Ships with `app-submit-error.spec.ts` and `app-submit-error.a11y.spec.ts` before any caller.
- Inline **success** stays as the existing `<p-message severity="success">`, standardized to carry
  `role="status"` / `aria-live="polite"`; no new success component is introduced.

## 3. Inventory (grounded in the current tree)

- **Blocking error render sites:** 26 `<p-message severity="error">` across `src/app/features`
  (editors + list pages). Each becomes `<app-submit-error [message]="errorMessage()">`, wired
  `[retryable]` + `(retry)` where the failing action is a load/network call.
- **Success render sites:** `successMessage()` `<p-message severity="success">` in the editors and
  list pages **stay inline**, standardized to `role="status"` / `aria-live="polite"`.
- **Toast sites to convert (all → inline):**
  - `tag-selector.ts:191,200` — "Failed to create/assign tag" error toasts → inline
    `role="alert"` inside the selector widget.
  - `annotations-section.ts` — error toasts (336,353,391) → inline `role="alert"`; **success
    toasts (332,349,386) → inline** polite region within the section (this is the one spot that
    changes a currently-working success toast; called out for review).
  - `goal-editor.ts` toast site — audit against the policy; convert to inline.
- **Network detection:** add a tiny helper (e.g. `isNetworkError(result)` = `error === 'Network
  error'` or `status == null`) so callers set `[retryable]` uniformly rather than string-matching.

## 4. Step-by-step (each step = its own PR to `release/2.0`, squash-merged)

- **Step 1 — `app-submit-error` primitive.** Component + unit + a11y specs. No callers. Small,
  reviewable in isolation.
- **Step 2 — error/network contract.** Add `isNetworkError` helper (+ spec) and a thin
  `submitError` signal convention so every editor wires the same shape.
- **Step 3 — editor forms (part A).** Migrate the six rows-only editors (term, report, user,
  edit-account, settings, login): error `p-message` → `app-submit-error`, success `p-message`
  standardized inline, plus network retry.
- **Step 4 — editor forms (part B).** Migrate the five wizard/large editors (project, actor,
  stakeholder, scenario, use-case). `use-case-editor` (largest) may warrant its own commit.
- **Step 5 — list/action pages.** project-list and the other `*-list` pages that surface
  import/action errors → `app-submit-error` (+ retry for load failures); success standardized inline.
- **Step 6 — nested widgets.** tag-selector, annotations-section, goal-editor → all inline.
- **Step 7 — tests + issue AC.** Command-level fallback + network-retry coverage; a11y assertions;
  post the updated #133 AC (§7) once approved.

Steps 3–6 can land independently; 1–2 are prerequisites. If you'd rather compress, 3+4 can be one
PR and 5+6 another, giving four PRs total.

## 5. Testing

- `app-submit-error.spec.ts` / `.a11y.spec.ts`: renders on non-null message, `role="alert"`
  present, Retry emits only when `retryable`.
- Extend `form-errors.spec.ts`: command-level (null-field) violations fall through to the submit
  error; nothing dropped.
- Network path: `isNetworkError` true → alert is retryable and Retry re-invokes the command.
- a11y: assertive region for blocking errors, polite region for success; reuse the
  `@axe-core/playwright` smoke pattern from Phase 1.

## 6. Risks / assumptions to confirm on review

- The only currently-working pattern this changes is the **annotations-section success toasts**,
  which become inline polite regions for consistency (§3). Flag if you'd rather leave those as toasts.
- `p-message` (error) is being replaced (not just annotated) because it does not reliably expose
  `role="alert"`; `app-submit-error` guarantees the announcement. Inline success `p-message` is
  kept but gets explicit `role="status"`/`aria-live="polite"`.

## 7. Proposed #133 issue body / AC rewrite (draft — post only on approval)

**What remains (adapter delivered in #132, field names fixed in #176):**
- Introduce a shared inline alert (`app-submit-error`, `role="alert"`) for blocking form/command errors.
- Apply the all-inline policy across all editors, list pages, and nested widgets.
- Success/info confirmations render inline as polite regions.
- Network failures render a retryable inline alert.

**Acceptance criteria (revised):**
- Blocking form/command/page errors render inline with `role="alert"` (assertive) via a shared component.
- Success/info confirmations render inline with `role="status"` / `aria-live="polite"`.
- Form, command, and nested-action feedback no longer uses toasts.
- Network failures present a retryable inline alert.
- Tests cover the shared component (incl. a11y), command-level fallback, and network retry.
- Note: field-violation→control mapping was delivered by #132 (`applyCommandErrors`) and DTO
  field names by #176; not re-done here.
