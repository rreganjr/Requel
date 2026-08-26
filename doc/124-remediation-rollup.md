## Remediation rollup by phase

Grouped from `doc/UI_UX_REVIEW.md` findings 1.1–5.6 plus the look-and-feel items (N1–N6, see `doc/124-lookandfeel-plan.md`). Also includes the follow-on tickets split out of #132 (see `doc/132-reactive-forms-plan.md`) — #171, #172, #173, #176. Some of those are server-side, but each one exists only to make a UI finding correct, so they are tracked here. Closed sub-issues are auto-checked; the rest are checked as each PR squash-merges to `release/2.0`.

**Execution order matters — do issues top-to-bottom.** The list below is the intended build order, and the epic's native sub-issue list is sorted to match. Key dependency notes:

- Phase 1 closes app-wide accessibility blockers that later components must not reintroduce.
- Phase 2 creates tokens and shared primitives. N4 #157 owns `app-data-table`; N5 #158 owns `app-field`/wizard primitives only, not full form migration. #172 (`app-field-group`) is an additive layout extension of the N5 primitive and blocks #132.
- Phase 3 migrates forms using the N5 primitives. #132 is blocked by #158 and #172; #138 is blocked by #132 and the command-error adapter #133.
- Phase 3 server-side backing: #171 supplies the real `@Size`/`@Email` constraints that #132's client-side validation is supposed to mirror, and #176 makes command field violations report input-DTO field names so #133's `applyCommandErrors` stops needing per-editor rename maps — #176 is ordered ahead of #133 because #173 grew those maps to nine, and #133 should audit error rendering on a base that no longer carries the workaround. #173 (3.1b create-flow wizards) is blocked by #132 and consumes its helpers.
- Phase 4 applies the primitives to IA/workflow tickets. #146 is an adoption/integration ticket after N1–N5, not a catch-all primitive build.
- Phase 5 handles lower-level Angular/SSE/bundle work plus optional dark-mode config.

### Phase 1 — Accessibility blockers

- [x] #135 — 4.1 Skip navigation and heading structure are incomplete
- [x] #136 — 4.2 Several interactive elements are mouse-only or not real links/buttons
- [x] #137 — 4.3 Icon-only buttons often lack accessible names
- [x] #139 — 4.5 Custom dialogs and overlays miss modal accessibility guarantees

### Phase 2 — Design system and shared primitives

- [x] #125 — 1.1 App uses stock Aura with no Requel brand layer
- [x] #126 — 1.2 Component-local CSS fights PrimeNG and fragments visual consistency
- [x] #127 — 1.3 Typography and hierarchy are too flat
- [x] #141 — 4.7 Color contrast, color-only meaning, reduced motion, and target size need policy
- [x] #156 — N3 Card / content-surface primitive (app-card)
- [x] #155 — N2 Tag & Chip severity system as shared primitives
- [x] #131 — 2.4 Loading, empty, and failure states are under-specified
- [x] #157 — N4 Data-table pattern component (app-data-table)
- [x] #158 — N5 Multi-step entity-create wizard (app-form-wizard + app-field)
- [x] #172 — 3.1a `app-field-group` two-column row layout variant for `app-field` (additive N5 extension; blocks #132)

### Phase 3 — Forms and validation remediation

- [x] #132 — 3.1 Forms are mostly template-driven and lack consistent validation
- [x] #171 — 3.1 server backing: bean validation `@Size`/`@Email` constraints on artifact name/text and user email inputs (blocks #132 — client caps must mirror real constraints)
- [x] #173 — 3.1b Create-flow wizards for project, actor, stakeholder, scenario, use-case (blocked by #132)
- [x] #176 — 3.2 server backing: `CommandController` should report field violations using input-DTO field names, not JPA entity property names (do this before #133 — it deletes the nine per-editor rename maps and the map parameter on `applyCommandErrors`)
- [x] #133 — 3.2 API and command errors are surfaced inconsistently
- [x] #134 — 3.3 Mini-forms (annotations, tags, admin, dialogs) need the same validation contract
- [x] #138 — 4.4 Form labels and error associations are incomplete
- [x] #143 — 5.2 Signals are used, but form/state hygiene is mixed
  - **Scope narrowed (2026-08):** the four editors named in Finding 5.2 (goal, use-case, user, settings) already have reactive main forms via #132/#158, with the shared dirty-check guard from #185. The only remaining 5.2 instance is `scenario-editor` step state — mutable `stepNodes` objects plus a manual `stepsSaveNeeded` flag living outside the reactive form — plus dead legacy fields (`name`/`text`/`primaryActorName`) in `use-case-editor`. The step-detail **edit dialog** is split to a follow-on (mini-form, #134 family).
- [x] #202 — 5.2 follow-on: scenario step-detail edit dialog reactive mini-form (blocked by #143)

### Phase 4 — Information architecture and workflow adoption

- [x] #142 — 5.1 Standalone/lazy routes are good, but route groups need structure
- [x] #154 — N1 App shell: top bar + grouped collapsible sidebar
- [x] #128 — 2.1 Navigation is complete but project context is hidden in the sidebar
- [x] #129 — 2.2 List/detail patterns are inconsistent and over-rely on row selection
- [ ] #130 — 2.3 Dialog and relationship flows need clearer progression
- [ ] #140 — 4.6 Async and SSE updates are not announced
- [ ] #146 — 5.5 Shared components exist but are too thin for the app's repeated patterns

### Phase 5 — Deeper Angular architecture and optional polish

- [ ] #144 — 5.3 Change detection and subscriptions are not modernized
- [ ] #145 — 5.4 SSE service is thoughtful but disconnected from UX and app-level state
- [ ] #147 — 5.6 Bundle and dependency posture is reasonable but should be measured
- [ ] #159 — N6 Theme switcher + dark mode via config panel (optional)

**Progress: 27 / 34 complete** (Phase 1 ✅, Phase 2 ✅, Phase 3 in progress).
