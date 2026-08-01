## Remediation rollup by phase

Grouped from `doc/UI_UX_REVIEW.md` findings 1.1–5.6 plus the look-and-feel items (N1–N6, see `doc/124-lookandfeel-plan.md`). Closed sub-issues are auto-checked; the rest are checked as each PR squash-merges to `release/2.0`.

**Execution order matters — do issues top-to-bottom.** The list below is the intended build order, and the epic's sub-issue list is sorted to match (see `scripts/reorder-ui-ux-subissues.sh`). Key dependencies:

- **N5 #158 (`app-field` + wizard) leads Phase 3.** The reactive-forms migration (3.1 #132), the command-error adapter (3.2 #133), the mini-form contract (3.3 #134), and the label/error-association work (4.4 #138) all bind to the shared `app-field` primitive, so it must land first.
- **4.4 #138 depends on 3.1 #132**, which in turn depends on **N5 #158**. (4.4 is *not* blocked by 4.5 #139 — dialogs are independent and ship in Phase 1.)
- Phase 2 (design-system tokens) precedes Phase 3 because `app-field` styles consume the theme tokens.

### Phase 1 — Quick wins & accessibility blockers

- [ ] #135 — 4.1 Skip navigation and heading structure are incomplete
- [ ] #136 — 4.2 Several interactive elements are mouse-only or not real links/buttons
- [ ] #137 — 4.3 Icon-only buttons often lack accessible names
- [ ] #139 — 4.5 Custom dialogs and overlays miss modal accessibility guarantees
- [ ] #141 — 4.7 Color contrast, color-only meaning, reduced motion, and target size need policy

### Phase 2 — Design-system foundation

- [ ] #125 — 1.1 App uses stock Aura with no Requel brand layer
- [ ] #126 — 1.2 Component-local CSS fights PrimeNG and fragments visual consistency
- [x] #127 — 1.3 Typography and hierarchy are too flat
- [ ] #146 — 5.5 Shared components exist but are too thin for the app's repeated patterns
- [ ] #155 — N2 Tag & Chip severity system as shared primitives
- [ ] #156 — N3 Card / content-surface primitive (app-card)
- [ ] #159 — N6 Theme switcher + dark mode via config panel (optional)

### Phase 3 — Forms & validation remediation

- [ ] #158 — N5 Multi-step entity-create wizard (app-form-wizard + app-field) — **prerequisite for the rest of this phase**
- [ ] #132 — 3.1 Forms are mostly template-driven and lack consistent validation
- [ ] #133 — 3.2 API and command errors are surfaced inconsistently
- [ ] #134 — 3.3 Mini-forms (annotations, tags, admin, dialogs) need the same validation contract
- [ ] #138 — 4.4 Form labels and error associations are incomplete
- [ ] #143 — 5.2 Signals are used, but form/state hygiene is mixed

### Phase 4 — Information architecture & workflow polish

- [ ] #128 — 2.1 Navigation is complete but project context is hidden in the sidebar
- [ ] #129 — 2.2 List/detail patterns are inconsistent and over-rely on row selection
- [ ] #130 — 2.3 Dialog and relationship flows need clearer progression
- [ ] #131 — 2.4 Loading, empty, and failure states are under-specified
- [ ] #140 — 4.6 Async and SSE updates are not announced
- [ ] #154 — N1 App shell: top bar + grouped collapsible sidebar
- [ ] #157 — N4 Data-table pattern component (app-data-table)

### Phase 5 — Deeper Angular architecture refactors

- [ ] #142 — 5.1 Standalone/lazy routes are good, but route groups need structure
- [ ] #144 — 5.3 Change detection and subscriptions are not modernized
- [ ] #145 — 5.4 SSE service is thoughtful but disconnected from UX and app-level state
- [ ] #147 — 5.6 Bundle and dependency posture is reasonable but should be measured
