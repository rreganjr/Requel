# #146 — 5.5 Shared UI layer: architecture & adoption doc — Implementation Plan

Issue: https://github.com/rreganjr/Requel/issues/146
Part of the look-and-feel remediation epic (`doc/124-remediation-rollup.md`, Phase 4).
Blocked-by #154/#155/#156/#157/#158/#131 — all merged.

## Summary

#146 is the consolidation/ratification capstone: its ACs call for an architecture doc, documented
adoption + a11y contracts, and explicitly **no new primitives** (AC4), with "≥1 editor + list page
refactored onto primitives" (AC2) already true app-wide. A survey found **zero adoption gaps** —
every editor and list page already uses the shared primitives (page-header, list-page /
app-data-table, app-field / app-field-group, editor-actions, app-relationship-section,
app-submit-error, app-card, the empty/loading/error state trio). So the remaining work is the one
missing piece: **the shared-UI-layer architecture & adoption document.** No production code changes.

## Deliverable: `doc/SHARED_UI_COMPONENTS.md`

1. **Purpose & the layer boundary** — what belongs in `requel-angular/src/app/shared/` (reusable,
   domain-agnostic UI) vs a feature (`features/**`, domain-specific). Where cross-cutting services
   live (`core/`, e.g. `AnnouncerService`).
2. **Principles the layer enforces** — standalone + `OnPush`; theme tokens only (no hard-coded
   color); `data-testid` forwarded via inputs so e2e/unit selectors survive refactors; an a11y
   spec per component; no domain/service logic inside shared components (they emit events, hosts
   own commands).
3. **Component inventory** — grouped tables (Shell & layout / Form primitives / Data display /
   Feedback & state / Dialogs & selectors / Domain sections / Utilities), each row: selector,
   one-line purpose, "when to use," and a link to its plan doc where one exists. Built from the
   26-item shared/ inventory + `core/announcer.service`.
4. **Adoption guidelines** — a "reach for X, not hand-rolled Y" table: page title → `app-page-header`;
   list surface → `app-list-page` + `app-data-table`; form field → `app-field` / `app-field-group`;
   multi-step create → `app-form-wizard`; linked-entity list → `app-relationship-section`; save/API
   error → `app-submit-error`; field error → `app-inline-error`; empty/loading/failed → the state
   trio; pick-an-entity → `app-entity-selector-dialog` / `app-scenario-selector-dialog`; async
   status → `AnnouncerService` (+ `app-update-banner`).
5. **Accessibility contract (AC3)** — the rule: every shared UI component ships a `*.a11y.spec.ts`
   asserting no axe violations (via `shared/testing/a11y.ts` `expectNoAxeViolations`) and accessible
   names for icon-only controls. Lists the 13 existing a11y specs as the enforced baseline.
6. **Worked examples (AC2)** — walk `goal-editor` and `goal-list` and name every primitive each
   composes, as the canonical "this is how a page is built now" reference.
7. **Adding a new shared component** — a short checklist (standalone/OnPush, tokens, testids, a11y
   spec, doc entry) so the layer stays consistent.

## Test plan

Doc-only; no code changes → existing tests remain green (AC5). Verify by rendering the Markdown and
checking every referenced selector/plan-doc path resolves. No `tsc`/`ng` gate needed beyond a
sanity check that nothing in `src/**` changed.

## Out of scope

Any new primitive or code change (AC4); refactoring pages (already adopted); the visual
`UI_DESIGN_GUIDE.md` (this doc is the *component-layer* companion, cross-linked, not a rewrite).

## AC mapping

| AC | Where satisfied |
|----|-----------------|
| 1 — architecture doc: boundaries + adoption | `SHARED_UI_COMPONENTS.md` §1–4 |
| 2 — ≥1 editor + list page on primitives | §6 worked examples (goal-editor, goal-list — already refactored) |
| 3 — a11y contracts documented | §5 a11y contract + the 13 a11y specs |
| 4 — no new primitives | Doc-only, by construction |
| 5 — existing tests pass | No code change; suites unchanged |
