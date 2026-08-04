# Implementation Plan — #158 N5 Multi-step entity-create wizard (`app-form-wizard` + `app-field`)

Part of the look-and-feel adoption epic #124 (see `doc/124-lookandfeel-plan.md`, item N5),
and §1.2 "Form wizard" of `doc/UI_UX_REVIEW.md`. Phase 3. Concretizes #132 + #146.

Build the two primitives the app's create/edit forms compose from — `app-field` (the form
row) and `app-form-wizard` (the multi-step shell) — plus the shared `form-errors` message
map, and prove all three on the **Goal** and **Story** create/edit flows.

This plan supersedes the N5 wording in `doc/124-lookandfeel-plan.md` §4 and the current
body of issue #158, both of which were written without looking at the actual editors.
Grounded in `requel-angular` at `909cc49` (Angular 21.2, PrimeNG 21.1.3, `@angular/cdk`
21.2.3 with `/a11y`, **Vitest** via `@angular/build:unit-test` + Playwright 1.59).

## Decisions (locked)

1. **Commit on step 1, not one submit at the end.** Tags, Relations, Goals and Actors are
   separate `CommandService` commands that need a real entity `id`, so step 1 commits
   `EditGoal` / `EditStory` on **Continue** and the later steps enrich the saved entity.
   The cost — abandoning the wizard at step 2 leaves a saved, valid, named entity — is
   accepted. See §2 for the version-handling this forces.
2. **Version is server-owned from the moment of the step-1 commit.** The wizard never
   reuses a version it has already spent. See §2 — this is the main correctness risk in
   the whole ticket.
3. **Both Goal and Story pilot the wizard**, not Goal alone. They exercise different step
   shapes (Goal: Tags + Relations; Story: Goals + Additional Actors) and different control
   types (Story adds a `p-select` and an actor picker), which is what proves `app-field` is
   actually control-agnostic.
4. **The `form-errors` helper lands here, not in #132.** N5 owns the helper and the first
   two migrations; #132 extends the helper (cross-field, password confirmation) and rolls
   the pattern out to the remaining editors.
5. **Edit mode is not a wizard.** The Goal and Story editors keep their single-card layout
   and swap `div.form-grid` for `app-field` rows. Same components, no wizard chrome.
6. **Story's Primary Actor stays on step 1.** It's a scalar property of the story, not an
   association, so it belongs with Name / Type / Text rather than with the Additional
   Actors step. This makes the actor picker a **step-1 `app-field` requirement**, and it is
   the control most likely to fight the row contract — build `app-field` against it early
   rather than discovering the friction late.
7. **The step nav is hand-rolled, not a `p-stepper` wrapper.** This replaces the
   original "wrap PrimeNG the way `app-data-table` wraps `p-table`" decision, whose
   justification was inheriting the library's keyboard nav and ARIA — and the ARIA is
   exactly what is broken (§2a). It is also the better semantic: tabs are peer views
   you browse, wizard steps are a linear process with progress state, so the nav is a
   `<nav>` + `<ol>` of buttons with `aria-current="step"`, which carries no tablist
   child requirements. Keyboard behaviour was already an acceptance criterion, so it
   was owned here either way; native buttons give Enter/Space, leaving roving
   tabindex as the only real work. No points change — a small hand-rolled nav for a
   wrapper is a wash.
8. **Points: 8 → 13** for #158, with #132 repointed to 8. See §7.

## 1. Ordering — the plan is right, the acceptance wording is wrong

`scripts/epic-rollup-comment.sh` puts `3.1` (#132) and `N5` (#158) both in **Phase 3** as
peers; nothing in it orders them. What *reads* as a dependency is one line in the N5
acceptance:

> Built on reactive forms (#132).

Combined with #158's out-of-scope bullet ("comprehensive reactive forms migration belongs
to #132"), N5 depends on a thing it declares out of scope. That's the circularity — not the
phasing.

Ground truth from the tree: **there are zero reactive forms in the app today.**
`ReactiveFormsModule` / `FormBuilder` / `FormGroup` appear in no file; 22 files under
`src/app` use `[(ngModel)]`. Somebody has to introduce reactive forms, and doing it inside
N5 — where the two components that consume them are being built — is the cheaper order.
`scripts/update-ui-ux-subissues.sh` already encodes exactly this (`add_blocked_by 132 158`,
and line 142: "#132 is blocked by #158"), so the tooling and this plan now agree.

Fix: N5's acceptance line becomes "Introduces reactive forms; #132 rolls the pattern out."
Phase 3 grouping stays as-is.

**Also stale:** #158 says "Blocked by #125, #126, #127." All three are merged (#126 → PR
#162, #127 → `bac80b1`, rollup → `06795d4`). Delete that line. The real upstream is
**N3 / #156 (`app-card`)**, which is already in.

## 2. The version contract (why commit-on-step-1 needs care)

Committing on step 1 means the wizard holds a persisted entity across steps 2–3 while
issuing *further* commands against it. #108 wired caller-supplied optimistic-lock
`version` through every `Edit*` command: on an update the command reloads the entity,
compares the caller's `version` to the persisted one, and throws
`EntityLockException.staleEntity(...)` on mismatch, which `CommandController` maps to
**HTTP 409**. Creates are never version-checked.

Today that never bites, because `goal-editor.onSave()` navigates away immediately after a
create:

```ts
if (this.version != null) input['version'] = this.version;
const result = await this.commandService.execute('EditGoal', input);
if (result.success) {
  if (this.isNew()) { /* ... */ this.router.navigate([...,'goals', saved.id]); }
  else { await this.loadGoal(); }   // reload refreshes this.version
}
```

A wizard stays on the page. So the failure mode is real and easy to hit: capture `version`
on step 1, add a relation on step 3, go back to step 1 to fix a typo, hit Continue → the
held version is stale → 409, and the user loses the edit for a reason the UI can't explain.

**Requirements.**

- `result.entity` from `EditGoal` / `EditStory` is a `GoalDto` / `StoryDto`, both of which
  carry `id` **and** `version`. The step-1 commit captures **both** from `result.entity` —
  never just the id.
- Treat the held version as **spent on use**. After *every* `EditGoal` / `EditStory`,
  refresh the held version from that command's returned entity. Never carry a version
  across two mutations of the same entity.
- **The enrichment steps do not bump the parent's version — verified, so no refetch is
  needed after steps 2–3.** Checked against the command impls:
  - `AssignTagCommandImpl.execute()` (`modules/tagging-jpa`) mutates the **Tag**, not the
    entity: `tagImpl.getTaggables().add(managedTaggable); merge(tagImpl)`. The Goal/Story
    is loaded as a `Taggable` and never modified, so its `@Version` is untouched.
  - `EditGoalRelationCommandImpl.execute()` (`modules/project-jpa`) persists or merges a
    standalone `GoalRelationImpl` and version-checks the **relation's** own version. It
    never touches the `Goal`.

  So the parent's version changes on `EditGoal` / `EditStory` and nowhere else. The bug
  surface is narrower than it first looked: it is confined to a *second* step-1 commit.
  That is exactly the back-navigate case below, and it is still a real defect — the
  simplification removes the belt-and-braces refetch, not the requirement.
- The editors already subscribe to the SSE event stream and reload on notification, with an
  explicit `if (fromSSE && this.hasUnsavedChanges()) return;` guard. Keep that guard intact
  inside the wizard: an SSE reload must refresh `version` without clobbering fields the
  user is editing in the active step.
- A 409 on a step commit is **not** a generic error. The wizard keeps the step, refetches
  the entity, and renders "This <entity> was changed elsewhere — your version has been
  refreshed, please review and continue" in the `role="alert"` region. It must not silently
  overwrite and must not dead-end.

**Required test (this is the one that would have caught it):** create through the wizard,
advance to step 3, add a relation, navigate **back** to step 1, edit the name, Continue —
asserts success, not 409.

## 2a. Verified platform facts

Checked against the installed tree so the spec above isn't written against a
remembered API:

| Assumption | Reality |
|---|---|
| `p-stepper` exists | ✅ PrimeNG **21.1.3**, exported at `primeng/stepper`; `StepperModule`, `Stepper`, `Step`, `StepItem`, `StepList`, `StepPanel`, `StepPanels`, `StepperSeparator` |
| `orientation="vertical"` input | ❌ **Does not exist.** Inputs are `value`, `linear`, `transitionOptions`, `motionOptions`. Vertical is a composition (`StepItem` + `StepperSeparator`), horizontal is `StepList` + `StepPanels` |
| Step selection is keyed | ❌ `value` is `ModelSignal<number \| undefined>` — numeric. Wizard maps key ↔ index |
| `[linear]` gating available | ✅ `InputSignalWithTransform<any, boolean>` |
| Reactive forms available | ✅ `@angular/forms` **21.2.2**; and confirmed **zero** current usages of `ReactiveFormsModule` / `FormBuilder` / `FormGroup`, against 22 files using `ngModel` |
| `DirtyCheckable` / `dirtyCheckGuard` | ✅ `app/core/dirty-check.guard.ts`, already implemented by both target editors |
| `expectNoAxeViolations` | ✅ `app/shared/testing/a11y.ts`; `empty-state.a11y.spec.ts` is the pattern to copy |
| `app-card`, `app-data-table` | ✅ both in `app/shared/` — N3 and N4 have landed |
| `--rq-editor-max` | ✅ `styles.scss:76`, but it is a **max-width (48rem)**, not a media-query breakpoint |
| `p-stepper` is axe-clean | ❌ **No — and this killed the wrapper approach.** `p-stepper` hard-codes `role="tablist"` on itself while its children are `role="presentation"` (`p-step`) and `role="tabpanel"` (`p-step-panel`), never `role="tab"`. axe reports a **critical** `aria-required-children`. Verified against **both** compositions — vertical (`p-step-item`) and horizontal (`p-step-list` + `p-step-panels`) — and `p-step-list` carries no role at all, so the tablist is on the wrong element. A component's host binding cannot be overridden from outside. See locked decision 7 |
| `p-button` carries our `data-testid` | ❌ It lands on the `p-button` **host**; the real `<button>` is inside. Specs must query `[data-testid="x"] button` (as `empty-state.spec.ts` / `error-state.spec.ts` already do) |
| `app-field` can host `p-select` | ✅ **Confirmed on Story's Type and Primary Actor rows.** Pass `app-field`'s `controlId` matching the select's own `inputId`; the `<label for>` then resolves to the input PrimeNG renders *inside* the wrapper, verified by asserting the target is a descendant of the wrapper and not the wrapper itself. Do not rely on the directive's DOM probe for wrappers that render their input asynchronously |
| `story-editor` guarded SSE reloads | ❌ It had **no** `fromSSE` guard at all (unlike `goal-editor`), so an SSE notification silently overwrote whatever the user was typing. Fixed as part of the migration; both editors now keep local edits *and* adopt the incoming version |
| `formControlName` works in a projected step body | ❌ It resolves its parent `formGroup` from the injector at the **insertion** point, where there is none. Step templates must use `[formControl]` |
| Unit tests are Karma/Jasmine | ❌ **Vitest.** `angular.json` uses `@angular/build:unit-test` with `setupFiles: src/test-setup.ts`; `tsconfig.spec.json` types are `vitest/globals` + `@testing-library/jest-dom`. No karma/jasmine dependency exists. Specs use global `describe`/`it`/`expect` and `TestBed` — copy `app-card.spec.ts` |
| `@angular/cdk` for step-nav keyboard | ✅ 21.2.3, `@angular/cdk/a11y` exported — `FocusKeyManager` available if `p-stepper`'s own nav proves insufficient |

## 3. What the pilot actually looks like

Both the issue ("project editor") and the plan ("e.g. new Goal or Story") were written
without looking at the forms.

### Goal — `goal-editor.ts`

The form is **two fields**:

```html
<div class="form-grid">
  <label for="name">Name</label>
  <input id="name" pInputText [(ngModel)]="name" />
  <label for="text">Description</label>
  <textarea id="text" pTextarea [(ngModel)]="text" rows="6"></textarea>
</div>
```

You cannot split two fields across a multi-step wizard. But the editor already has three
more sections **gated behind `@if (!isNew())`** — Tags, Relations, Annotations — because
they need a persisted `goalId`. *That gate* is the create-flow problem worth solving, and
it's what makes the Goal create flow genuinely multi-step:

| Step | Content | Required | Commits |
|---|---|---|---|
| 1 · Details | Name, Description | yes | `EditGoal` on **Continue** — creates the goal, captures `id` + `version` |
| 2 · Tags | existing `app-tag-selector`, now reachable pre-first-save | no (skippable) | per-tag `TagService` call, as today |
| 3 · Relations | existing `app-entity-selector-dialog` + relation-type flow | no (skippable) | `EditGoalRelation`, as today |

Done navigates to `/projects/:name/goals/:id`.

### Story — `story-editor.ts`

Story has a richer step 1 (four controls, two of them not plain inputs) and two gated
sections — Goals and Additional Actors:

| Step | Content | Required | Commits |
|---|---|---|---|
| 1 · Details | Name, Type (`p-select`), Primary Actor (picker — locked to step 1), Text | yes (Name, Type) | `EditStory` on **Continue** — captures `id` + `version` |
| 2 · Goals | existing `app-entity-selector-dialog` (`entityType="Goal"`, `excludeIds`) | no (skippable) | as today |
| 3 · Additional Actors | existing `app-entity-selector-dialog` (`entityType="Actor"`) | no (skippable) | as today |

Story has no tag-selector; don't add one here. `app-annotations-section` stays outside the
wizard in both editors (it renders against a persisted `entityId`).

Story's step 1 is what makes `app-field` honest: it must host `pInputText`, `pTextarea`,
`p-select` and the actor picker without per-control special-casing. Since Primary Actor is
locked to step 1 (decision 6), the actor picker is a step-1 requirement — so build
`app-field` against Story's four controls, not Goal's two, or the row contract will look
finished while still being input-only.

### Edit mode

`/projects/:name/goals/:goalId` and `/projects/:name/stories/:storyId` keep their single
`app-card` and swap `div.form-grid` for `app-field` rows. No wizard chrome. Both editors
currently redefine the identical `.form-grid { grid-template-columns: 120px 1fr; ... }`
block locally — both copies get deleted.

## 4. Replacement for `doc/124-lookandfeel-plan.md` §4, N5

> ### N5 — Entity-create form wizard (`app-form-wizard` + `app-field`) · priority:medium
>
> Build the two primitives the app's create/edit forms compose from, and prove them on the
> Goal and Story create/edit flows.
>
> **`app-field`** — one form row: label + helper text on the left, control on the right,
> hairline divider below, inline error under the control. Replaces the per-editor
> `div.form-grid { grid-template-columns: 120px 1fr }` that #126's descope left in place
> (nine editors currently redefine it at varying widths). The row owns
> label↔control↔error association, so #138 is satisfied structurally rather than
> per-caller.
>
> **`app-form-wizard`** — two-column card: left = vertical step nav with per-step
> completion state, right = the active step's fields, footer = subtle **Cancel** +
> primary **Continue** (**Done** on the last step). The nav is a `<nav>` + `<ol>` of
> buttons with `aria-current="step"` and a roving tabindex, *not* a `p-stepper` wrapper —
> `p-stepper` fails axe's `aria-required-children` in every composition (§2a).
>
> Introduces reactive forms and a shared `form-errors` message map, and migrates the Goal
> and Story flows; #132 rolls both out to the remaining editors.
>
> **Acceptance.**
> - Goal create and Story create migrated end-to-end as 3-step wizards, replacing the
>   `@if (!isNew())` gate on their enrichment sections.
> - Goal edit and Story edit use `app-field` rows (no wizard chrome); `div.form-grid`
>   deleted from both editors.
> - Reactive forms; wizard Continue disabled while `form.invalid || saving()`, edit-form
>   Save additionally disabled while `form.pristine`.
> - Entity `version` refreshed after every step commit; a stale-version 409 keeps the step
>   and refetches rather than failing opaquely.
> - Labels and errors associated via `aria-describedby` / `aria-invalid` (#138); step nav
>   keyboard-operable; `axe` clean.
> - All styling from `--rq-*` tokens; no literals.
>
> *Concretizes #132 + #146. Points: 13.*

## 5. Replacement body for issue #158

> ## N5 — Entity-create form wizard (`app-form-wizard` + `app-field`)
>
> Part of the design-system adoption in `doc/124-lookandfeel-plan.md` §4 (N5) and §1.2
> "Form wizard". Full plan: `doc/158-form-wizard-field.md`. Phase 3. Concretizes #132 +
> #146.
>
> ### Why
>
> Nine editors each redefine their own `div.form-grid` with a different label column width,
> none associate errors with fields, and create flows hide enrichment sections behind
> `@if (!isNew())` — so a user must save a half-configured entity before they can finish
> configuring it. #126 was descoped to the mechanical `::ng-deep`/inline-style pass and did
> **not** ship a shared form layout, so this issue is the first real answer to that
> finding.
>
> ### In scope
>
> **1. `app-field` — `src/app/shared/app-field.ts`**
>
> One form row. Label + helper on the left, control on the right, hairline divider below,
> inline error beneath the control.
>
> ```ts
> @Input({ required: true }) label!: string;
> @Input() helper = '';
> @Input() control?: AbstractControl;   // drives error / required / aria state
> @Input() divider = true;
> @Input() errorMessages?: Record<string, string>;  // per-field override
> ```
>
> - The control is content-projected and carries an `appFieldControl` directive.
>   `app-field` queries it with `contentChild` and sets `id`, `aria-describedby` (helper id
>   + error id), and `aria-invalid` on the host element. Callers never wire ARIA by hand —
>   this is what makes #138 structural.
> - Must host `pInputText`, `pTextarea`, `p-select` and the actor picker with no
>   per-control special-casing (Story's step 1 is the proof).
> - Ids are generated (`rq-field-{n}`) unless the caller supplies one.
> - Required marker derives from the control's validators, not a separate input.
> - Errors render only when `control.invalid && (control.touched || submitted)`.
> - Label column width is a single token, not a per-editor literal.
> - Layout collapses to stacked label-above-control on narrow viewports. Note
>   `--rq-editor-max` (`styles.scss:76`) is a **max-width token (48rem)**, not a breakpoint
>   — the collapse needs a container query or a new `--rq-bp-*` token rather than reusing
>   it.
>
> **2. `form-errors` helper — `src/app/shared/form-errors.ts`**
>
> Maps `required | minlength | maxlength | email | pattern` to messages, with per-field
> override. Owned by this ticket; #132 extends it (password confirmation, cross-field)
> during rollout.
>
> **3. `app-form-wizard` — `src/app/shared/app-form-wizard.ts`**
>
> Two-column card. Left: vertical step nav with completion state. Right: the active
> step's `app-field` rows. Footer: subtle **Cancel** + primary **Continue** / **Done**.
>
> The nav is hand-rolled — a `<nav aria-label="Steps">` wrapping an `<ol>` of buttons
> carrying `aria-current="step"`, `aria-disabled` on locked steps, and a roving tabindex.
> It does **not** wrap `p-stepper`, which hard-codes `role="tablist"` on itself and so
> fails axe's `aria-required-children` in both its vertical and horizontal compositions
> (§2a). Locked steps use `aria-disabled` rather than the `disabled` attribute, so screen
> reader users can still reach a step and hear that it is not yet available.
>
> Steps are declared as child elements rather than passed as an array, so each step's
> body can be an `<ng-template>` the caller owns:
>
> ```html
> <app-form-wizard [(activeKey)]="step" (stepCommit)="onCommit($event)"
>                  (cancelled)="onCancel()" (finished)="onFinish()">
>   <app-wizard-step key="details" label="Details" [form]="detailsForm">
>     <ng-template>
>       <app-field label="Name" [control]="detailsForm.controls.name">
>         <input appFieldControl [formControl]="detailsForm.controls.name" />
>       </app-field>
>     </ng-template>
>   </app-wizard-step>
>   <app-wizard-step key="tags" label="Tags" [optional]="true"> ... </app-wizard-step>
> </app-form-wizard>
> ```
>
> `app-wizard-step` inputs: `key`, `label`, `helper?`, `form?` (omitted for
> association-only steps), `optional`. Inside the template use `[formControl]`, **not**
> `formControlName` — the body is projected, and `formControlName` looks for its parent
> `formGroup` at the insertion point, where there is none.
>
> Keys are the only step identity in the public API and the route fragment; indexes never
> leak into caller code or URLs.
>
> The commit handshake is a request object, so the wizard owns the busy state rather than
> the host having to mirror it back:
>
> ```ts
> interface WizardCommitRequest {
>   readonly step: AppWizardStepComponent;
>   complete(): void;              // advance, or emit finished on the last step
>   fail(message: string): void;   // stay on the step, show message in role="alert"
> }
> ```
>
> The wizard stays busy until one of the two fires, so a host that forgets to respond
> visibly hangs on its own step instead of silently advancing past a failed save. A
> second response after the first is ignored.
>
> Behavior:
> - **Linear forward, free backward.** Continue is disabled while `form.invalid` or a
>   commit is in flight; completed steps stay clickable. Deliberately **not** gated on
>   `pristine`: with commit-on-step-1 the user can return to a step whose values are
>   already saved and valid, and a pristine guard would trap them there with no way
>   forward. (The plan's original `form.pristine` clause survives for the *edit* forms,
>   where Save-on-unchanged is the thing worth preventing.)
> - **Step state**: incomplete / complete / invalid / active, derived from the step's
>   `FormGroup`. Optional steps can be skipped without blocking.
> - **Commit model**: `stepCommit` fires on Continue; the host decides whether that hits
>   the API. Wizard shows a busy state until the host resolves.
> - **Version handling**: the host reports the entity's current `version` back after each
>   commit (from `result.entity`) and the wizard never reuses a spent version. A 409
>   (`EntityLockException.staleEntity` → HTTP 409 via `CommandController`) keeps the step,
>   refetches the entity, and explains that it was changed elsewhere. Never a silent
>   overwrite.
> - **Failure**: on any rejected commit the wizard stays on the step and renders the
>   host-supplied message in a `role="alert"` region above the fields. Mapping backend
>   constraint violations to specific fields stays with #133.
> - **Routing**: active step reflected as a route fragment so back/refresh land on the
>   right step.
> - **Unsaved changes**: implements `DirtyCheckable` and composes with the existing
>   `dirtyCheckGuard`.
>
> **4. Pilots — Goal and Story create/edit**
>
> - `/projects/:name/goals/new` becomes a 3-step wizard: **Details** (Name, Description —
>   required, commits `EditGoal` on Continue) → **Tags** (optional) → **Relations**
>   (optional). Done navigates to the saved goal.
> - `/projects/:name/stories/new` becomes a 3-step wizard: **Details** (Name, Type,
>   Primary Actor, Text — commits `EditStory`) → **Goals** (optional) → **Additional
>   Actors** (optional). Primary Actor stays on Details: it is a scalar property of the
>   story, not an association, and the Additional Actors step covers only the association.
> - Both edit routes keep their single card and swap `div.form-grid` for `app-field` rows.
>   No wizard chrome.
> - `@if (!isNew())` gates removed from the migrated enrichment sections.
> - `app-annotations-section` stays outside the wizard in both editors.
>
> ### Out of scope
>
> - Migrating the *remaining* editors (project, actor, stakeholder ×2, scenario, use-case,
>   term, user, auth) to reactive forms / `app-field` — **#132**.
> - Mapping backend constraint violations to field-level errors — **#133**.
> - The broader form-labelling sweep — **#138**.
> - Dark-mode variants — **N6**.
>
> ### Acceptance criteria
>
> - [ ] Goal create runs end-to-end as a 3-step wizard; a goal created through it is
>       identical to one created by the current form.
> - [ ] Story create runs end-to-end as a 3-step wizard; same equivalence check.
> - [ ] Enrichment sections reachable during create (no `isNew()` gate).
> - [ ] Goal edit and Story edit render via `app-field`; `div.form-grid` gone from both.
> - [ ] All four flows use reactive forms; wizard Continue disabled while
>       `form.invalid || saving()`, edit-form Save also disabled while `form.pristine`.
> - [ ] Step nav is a `<nav>`/`<ol>` of buttons with `aria-current="step"`; locked steps
>       are `aria-disabled` and still focusable.
> - [ ] `version` is captured from `result.entity` on the step-1 commit and refreshed after
>       every subsequent mutation; no held version is reused.
> - [ ] Back-navigate-and-re-Continue after a later-step commit succeeds (no 409).
> - [ ] A stale-version 409 keeps the step, refetches, and surfaces an explanatory message;
>       no silent overwrite.
> - [ ] The SSE reload guard (`fromSSE && hasUnsavedChanges()`) still prevents clobbering
>       in-flight step edits.
> - [ ] Every `app-field` control has an associated `<label>`, and `aria-describedby` /
>       `aria-invalid` update with validation state.
> - [ ] `app-field` hosts `pInputText`, `pTextarea`, `p-select` and the actor picker with no
>       per-control special-casing.
> - [ ] Step nav is keyboard-operable (Up/Down/Home/End, Enter/Space); focus moves to the
>       step panel heading on change.
> - [ ] No color, radius, spacing, or type literals — all `--rq-*` tokens.
> - [ ] `app-field.spec.ts` and `app-form-wizard.spec.ts` cover required/error states,
>       disable policy, linear gating, keyboard nav, and version refresh + 409 handling.
> - [ ] `app-field.a11y.spec.ts` and `app-form-wizard.a11y.spec.ts` pass
>       `expectNoAxeViolations` (matching `empty-state.a11y.spec.ts`).
> - [ ] Playwright: create a goal and a story through the wizard, each including one
>       skipped optional step, plus the back-navigate-and-re-Continue case.
>
> ### Notes
>
> - Depends on **#156** (`app-card`) — merged. The #125/#126/#127 block is cleared; that
>   line has been removed.
> - New `data-testid`s follow the existing convention (`goal-wizard-step-tags`,
>   `story-wizard-step-goals`, `wizard-continue`, `wizard-cancel`).
> - Points: **13**.

## 6. What this moves out of #132

#132 (3.1 "Forms are mostly template-driven and lack consistent validation") keeps the
rollout and loses the foundations:

| Moves to #158 | Stays in #132 |
|---|---|
| `form-errors` helper (base message map) | Helper extensions: cross-field, password confirmation |
| Reactive forms for Goal + Story | Reactive forms for the remaining 8 editors |
| `app-field` adoption in Goal + Story | `app-field` adoption everywhere else |
| — | Save-button disable policy applied app-wide |
| — | Deleting the remaining 7 copies of `.form-grid` |

`scripts/update-ui-ux-subissues.sh` already records `add_blocked_by 132 158`, so no
dependency edit is needed — only the scope/acceptance text on #132 and the two point
values.

## 7. Story points

| Issue | Was | Now | Why |
|---|---|---|---|
| **#158** (N5) | 8 | **13** | Two pilots instead of one, plus the `form-errors` helper, plus the version/409 contract |
| **#132** (3.1) | unset | **8** | Loses the helper and two editors; still 8 editors + the app-wide disable policy |

Nothing in `scripts/` currently points #132 — `set-lookandfeel-points.sh` only covers
#154–#159 and `backfill-points.sh` only touches closed issues. Verify with
`bash scripts/probe-project.sh` before writing.

The repo already has the tooling; **prefer it over raw `gh`**, because it resolves the
project by title, adds the item if missing, and refuses to set a Retro value on an open
issue:

```bash
bash scripts/set-points.sh 158 13
bash scripts/set-points.sh 132 8
```

Also update the here-doc in `scripts/set-lookandfeel-points.sh` so a re-run doesn't
reset #158 to 8:

```
158 13  # N5 Form wizard + app-field + form-errors helper + Goal & Story migrations
```

<details>
<summary>Raw <code>gh</code> equivalent (project #2, <code>rreganjr</code>)</summary>

```bash
OWNER=rreganjr
NUM=2                     # github.com/users/rreganjr/projects/2
REPO=rreganjr/Requel

PROJECT_ID=$(gh project view "$NUM" --owner "$OWNER" --format json | jq -r '.id')
SP_ID=$(gh project field-list "$NUM" --owner "$OWNER" --format json \
        | jq -r '.fields[] | select(.name=="Story Points") | .id')

set_points() {   # set_points <issue#> <points>
  ITEM_ID=$(gh project item-add "$NUM" --owner "$OWNER" \
              --url "https://github.com/$REPO/issues/$1" --format json | jq -r '.id')
  gh project item-edit --project-id "$PROJECT_ID" --id "$ITEM_ID" \
                       --field-id "$SP_ID" --number "$2"
}

set_points 158 13
set_points 132 8
```

Notes: `--project-id` wants the project's **node ID** (`PVT_…`), not the number `2`;
`item-add` is idempotent and returns the existing item id if the issue is already on the
board. Needs a token with `project` scope — `gh auth refresh -s project` if
`gh project field-list` 403s.

</details>

## 8. Applying the rest

- Update `doc/124-lookandfeel-plan.md` §4 N5 with §4 above, and the §7 points line
  (`N5 = 8` → `N5 = 13`).
- Replace the body of #158 with §5 (`gh issue edit 158 --body-file …`).
- Update #132's scope/acceptance per §6.
- Regenerate the epic rollup comment in place:

  ```bash
  DRY_RUN=1 bash scripts/epic-rollup-comment.sh                  # preview
  COMMENT_ID=5113771759 bash scripts/epic-rollup-comment.sh       # edit in place
  ```

Per `CLAUDE.md`, none of the GitHub-state steps run without an explicit go-ahead.

## 8a. Carried out of scope, filed elsewhere

Both raised as comments on their owning issues rather than absorbed here:

- **#139** — `p-confirmDialog` marks its *host* `role="alertdialog"` even while idle, so
  axe reports an unnamed dialog (**serious**) on every page that mounts one. Not specific
  to these forms. `expectNoAxeViolations` gained an `exclude` parameter and the pilots pass
  `['p-confirmdialog']`; grep that string to find the exclusions to remove once fixed.
- **#132** — `goal-editor`'s relation-type `p-select` still binds with `ngModel`. It is a
  transient dialog control rather than part of the goal form, so it stays for #132's sweep,
  which also inherits `form-errors` (to extend) and `app-field` (to roll out). Verified
  counts at hand-off: `.form-grid` still defined locally in **8** editors (actor, project,
  report, scenario, stakeholder, term, use-case, user — goal's and story's are deleted).

## 9. Still open

- ~~Whether the tag and relation commands bump the **parent** entity's `@Version`.~~
  **Closed** — they don't; see §2. Neither `AssignTagCommandImpl` nor
  `EditGoalRelationCommandImpl` touches the parent entity.
- ~~Whether `p-stepper` supports `orientation="vertical"`.~~ **Closed** — it doesn't; the
  spec now uses the vertical composition. See §2a.
- ~~Whether Story's **Primary Actor** should stay on step 1 or move to the Actors step.~~
  **Closed — step 1.** See locked decision 6.
- The narrow-viewport collapse needs either a container query or a new `--rq-bp-*` token,
  since `--rq-editor-max` is a width not a breakpoint (§2a). Decide when styling
  `app-field`; adding one token is in scope.

Nothing above blocks starting. The remaining item is a styling choice made while writing
`app-field`'s CSS.
