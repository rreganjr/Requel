3.2 API and command errors are surfaced inconsistently

Part of the UI/UX remediation epic #124. Source: `doc/UI_UX_REVIEW.md` Finding 3.2. Priority: High.

The field-violation -> control adapter (`applyCommandErrors`) shipped with #132, and #176 made command violations report input-DTO field names. This ticket makes error/success *rendering* consistent: all form, command, and nested-action feedback renders inline (no toasts for errors), blocking errors announce assertively, and network failures are retryable.

## Acceptance criteria
- Blocking form/command/page errors render inline via a shared component (`app-submit-error`) with `role="alert"` (assertive).
- Success/info confirmations render inline with `role="status"` / `aria-live="polite"`; brief transient confirmations (e.g. "Saved", "Note added") may remain toasts.
- Nested-widget action errors (tag assignment, annotation add) render inline, not as toasts.
- Network failures (`status === 0`) present a retryable inline alert that re-runs the failed action; list-load failures offer Retry.
- Tests cover the shared component (incl. a11y), `isNetworkError`, and the editor retry wiring.
- Delivered by #132/#176, not re-done here: field-violation -> control mapping and DTO field names.
