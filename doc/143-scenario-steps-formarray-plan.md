# Implementation Plan — #143 5.2 Scenario step state → FormArray

Part of the UI/UX remediation epic **#124**. Source: `doc/UI_UX_REVIEW.md` Finding 5.2. Phase 3.
Branch: `143-scenario-steps-formarray` off `release/2.0`.
Blocked by #132 (merged), builds on #158/#170 (reactive forms), #185 (dirty-check guard),
#171 (`ARTIFACT_NAME_MAX`). Priority: High, effort Medium (3–5 days).

> Finding 5.2's remaining surface is a single component. §0 records why everything else in the
> finding is already done; §10 restates the narrowed AC posted on the issue.

## 0. Already delivered — NOT in scope

The four editors named in Finding 5.2 already have reactive **main** forms, with no
`detectChanges()` and no hand-rolled `trackChanges()`:

- `goal-editor`, `use-case-editor`, `user-editor`, `settings` → `FormGroup` + validators, Save
  gated on `form.invalid || form.pristine || saving()`, `DirtyCheckable` guard from #185.
- Server entity / permission / async state correctly stays in signals.

`scenario-editor`'s **details** form is likewise reactive (`detailsForm`, `scenario-editor.ts:442`).
What is *not* is its **step list**. That, plus a small dead-field cleanup, is this ticket.

## 1. Scope (locked decisions)

1. **Model the step list as a `FormArray` of per-step `FormGroup`s** (Option A). The server
   contract makes this the natural fit: `EditScenario` submits the whole list and rebuilds
   `scenario.getSteps()` server-side — there is no per-step command (`scenario-editor.ts:809`,
   `saveDetails()` → `buildStepInputs()`).
2. **Sibling array, not nested.** Keep the steps array as its own `stepsForm`, a sibling of
   `detailsForm` — *not* nested inside `detailsForm`. The create wizard gates the Details step on
   `detailsForm` validity (`scenario-editor.ts:146`, `[form]="detailsForm"`); nesting steps would
   make an empty step-2 name block advancing past step 1.
3. **Delete `stepsSaveNeeded`.** Dirty comes from the forms:
   `hasUnsavedChanges = detailsForm.dirty || stepsForm.dirty`.
4. **Per-step name validation** (`required` + `ARTIFACT_NAME_MAX_LENGTH`), matching every other
   editor — today an empty step name is submitted to the server.
5. **Step-detail edit dialog stays as-is** (its `editingName/Type/Text` `[(ngModel)]` scratch
   buffer) and is migrated to a reactive mini-form in the **follow-on** (#134 family). This ticket
   only rewires `applyStepEdit()` to write into the target group.
6. **Dead-field cleanup:** remove `name`, `text`, `primaryActorName` from `use-case-editor.ts:559`
   (unreferenced since its reactive migration).

## 2. The FormArray model (the contract)

Each step is a `FormGroup` that carries **both** editable fields and metadata, so reordering a
step moves its identity with it (no parallel array to keep in sync):

```ts
type StepGroup = FormGroup<{
  name: FormControl<string>;          // required + maxLength(ARTIFACT_NAME_MAX_LENGTH)
  scenarioType: FormControl<string>;  // nonNullable, default = detailsForm.scenarioType
  text: FormControl<string | null>;
  // metadata — no validators, not rendered as inputs, included in getRawValue():
  stepId: FormControl<number | null>;
  isScenario: FormControl<boolean>;
  isNew: FormControl<boolean>;
}>;

readonly stepsForm = new FormArray<StepGroup>([]);
```

- `newStepGroup()` replaces `newStepNode()` (`scenario-editor.ts:671`) — same defaults
  (`stepId:null, isScenario:false, isNew:true`, `scenarioType` from `detailsForm`).
- **Structural changes must `markAsDirty()` explicitly.** Angular does *not* dirty a `FormArray`
  on `push`/`removeAt`/`insert`. Add/remove/reorder call a single
  `private markStepsDirty() { this.stepsForm.markAsDirty(); }` — the one-line replacement for the
  ~10 scattered `stepsSaveNeeded.set(true)` calls. Inline field edits dirty the control natively.
- **`isScenario` rows** (sub-scenario references added by `onSubScenarioSelected`,
  `scenario-editor.ts:746`) render as a read-only link, not an input. Their `name` control is
  populated from the ref and its validators are irrelevant; **disable the `name` control** on those
  groups (`{ nonNullable: true }` + `.disable()`) so a reference row can never make `stepsForm`
  invalid. Disabled controls are still emitted by `getRawValue()`, so the submit payload is
  unchanged.

Guard wiring (replaces `scenario-editor.ts:526`–`533`):

```ts
hasUnsavedChanges(): boolean { return this.detailsForm.dirty || this.stepsForm.dirty; }
canSave(): boolean {
  return this.detailsForm.valid && this.stepsForm.valid
      && this.hasUnsavedChanges() && !this.saving();
}
```

## 3. Behavior-parity map (current → new)

| Path | Today (`stepNodes` signal + `stepsSaveNeeded`) | After (FormArray) |
|---|---|---|
| `addStep` / `addStepAt` / `addStepBelow` | splice node, `stepsSaveNeeded.set(true)` | `insert(i, newStepGroup())` + `markStepsDirty()` |
| `removeStep` | filter node, flag | `removeAt(i)` + `markStepsDirty()` |
| `onDrop` reorder | `moveItemInArray(nodes)`, flag | `const g = at(prev); removeAt(prev); insert(cur, g)` + `markStepsDirty()` |
| inline name edit | `[(ngModel)]="step.name"` + `onStepNameChange()` flag | `[formControl]="g.controls.name"` (dirties natively) |
| `applyStepEdit` (dialog Apply) | mutate node, flag | `g.patchValue({name,scenarioType,text})` + `markStepsDirty()` |
| `onSubScenarioSelected` | push isScenario node, flag | `push(refGroup)` (name disabled) + `markStepsDirty()` |
| `buildStepInputs` | map `stepNodes()` | map `stepsForm.getRawValue()` → `EditStepInput[]` |
| `excludeScenarioIds` | read `stepNodes()` | read `stepsForm.getRawValue()` |
| dirty for guard/Save | `detailsForm.dirty || stepsSaveNeeded()` | `detailsForm.dirty || stepsForm.dirty` |

## 4. Load / refresh reconciliation (the careful part)

Two existing branches in `loadScenario` (`scenario-editor.ts:585`–`606`) must be preserved
exactly; only the storage changes.

- **Clean reload** (no unsaved edits): today `detailsForm.reset(...)` +
  `stepNodes.set(stepsToNodes(...))` + `stepsSaveNeeded.set(false)`. New: rebuild `stepsForm` from
  the server steps (`clear()` then `push(...)`, or `setControl` per index), then
  `stepsForm.markAsPristine()`. A rebuilt-from-server array is pristine and valid by construction.
- **Dirty/saving/editing reload** (`mergeStepIds`, `scenario-editor.ts:636`): the post-create
  refetch backfills server ids onto id-less nodes **by position**, without clobbering what the user
  is typing. New: for each group with `stepId == null`, `patchValue({ stepId, isNew:false }, { emitEvent:false })`
  and **do not** dirty (the array must stay dirty if it already was, but this reconciliation itself
  is not a user edit — use `emitEvent:false` and do not touch pristine/dirty here).
- The **spent-on-use version** flow and the no-skeleton post-save refetch
  (`scenario-editor.ts:846`–`851`) are unchanged. Note the ordering that makes id-backfill work:
  `saveDetails()` awaits `loadScenario(false)` **before** `finally { saving.set(false) }`, so during
  the refetch `saving()` is still true → the `mergeStepIds` branch runs. Keep that ordering.
- Keep the **known position-matching gap** comment (`scenario-editor.ts:625`) verbatim — FormArray
  does not change it (still no client key for a step the server has never seen).

## 5. Template changes (`scenario-editor.ts` inline template)

- Steps list `@for` iterates `stepsForm.controls` (`let g = $implicit; let i = $index`), wrapped in
  `[formGroup]="g"` (or `formArrayName` + `formGroupName="i"`). Track by a stable key — prefer
  `g.controls.stepId.value ?? g` (object identity for new rows) so DnD does not thrash the DOM.
- Inline name input (`scenario-editor.ts:296`): `[(ngModel)]="step.name"` → `[formControl]="g.controls.name"`,
  adopting the **#134 inline contract** (`[attr.aria-invalid]`, `[attr.aria-describedby]`, inline
  `role="alert"` required message from `form-errors.ts`) rather than app-field's two-column chrome.
- Read sites (`step.name`, `step.scenarioType`, `step.isScenario`) → `g.controls.*.value`.
- CdkDropList `[cdkDropListData]` and `onDrop` operate on the controls array.
- Drop `FormsModule` from `imports` **only if** no other `ngModel` remains — the step dialog still
  uses it, so `FormsModule` stays until the follow-on lands.

## 6. Step-by-step (each step its own PR to `release/2.0`, squash-merged)

- **Step 0 — characterization tests (no behavior change).** Fill the gaps in
  `scenario-editor.spec.ts` so the refactor is provably behavior-preserving: inline name edit →
  dirty, dialog **Apply** persists + dirty, **drag-reorder** → dirty + order, unsaved-changes guard
  true on dirty steps with a pristine details form, and `onSave` still sends the reordered/edited
  list. Land these first, green on the current `stepNodes` implementation.
- **Step 1 — introduce `stepsForm`, port state + handlers.** Replace `stepNodes`/`stepsSaveNeeded`
  with the FormArray; port §3 handlers and §4 reconciliation; rewire guard/`canSave`. Update the
  Step-0 specs from `stepNodes()`/`stepsSaveNeeded()` assertions to `stepsForm`.
- **Step 2 — template + inline validation.** Move the list template to `[formGroup]`/`formArrayName`
  and the #134 inline contract; per-step required-name message.
- **Step 3 — `use-case-editor` dead-field removal** (`name`/`text`/`primaryActorName`). Trivial,
  separable; can ride Step 1's PR if preferred.

## 7. Test plan

- Unit (`scenario-editor.spec.ts`): the Step-0 characterization set, re-pointed at `stepsForm`,
  plus new per-step required-name validity and the `isScenario` "name disabled, still submitted"
  case.
- a11y (`scenario-editor.a11y.spec.ts`): unchanged — `openStepEdit({...STEP})` /
  `onStepDialogVisibleChange` still drive `editingStep()`; the dialog is untouched here.
- Manual: create flow (wizard) → add several steps, reorder, save, confirm ids backfill and no
  duplicate steps server-side; edit flow → inline rename + dialog edit + reorder, Back triggers the
  unsaved-changes guard, Save clears it.
- `npm run lint` / `npm test` in `requel-angular`; no `detectChanges`/`trackChanges` reintroduced.

## 8. Out of scope → follow-on

Step-detail **edit dialog** reactive-mini-form migration (`editingName/Type/Text` →
`FormGroup`, inline validation, Apply patches the group). Tracked as the 5.2 follow-on, blocked by
this ticket. This plan leaves the dialog working via the existing scratch buffer, with
`applyStepEdit()` already writing into the FormArray group so the follow-on is a pure dialog-local
change.

## 9. Risks & edge cases

- **Structural dirty:** easy to forget `markAsDirty()` on add/remove/reorder — Step-0 tests cover
  it. Single `markStepsDirty()` helper keeps it one call site per op.
- **Disabled `name` on reference rows:** verify `getRawValue()` still emits it (it does) so the
  submit payload and `excludeScenarioIds()` are unchanged.
- **Id backfill during refetch:** must use `{ emitEvent:false }` and must not flip dirty/pristine,
  or a mid-refetch edit gets lost / a clean form looks dirty. Mirror the current guard branch exactly.
- **Track-by on reorder:** a bad `track` expression causes DnD flicker or lost focus — use control
  identity.
- **First FormArray in the app:** no house precedent (user-editor uses a dynamic `FormGroup`, not a
  `FormArray`). This establishes the pattern; keep it local, no shared abstraction yet.

## 10. Narrowed acceptance criteria (as posted on #143)

1. `scenario-editor` step state no longer relies on a manual dirty flag; unsaved-step detection
   derives from form state (`stepsSaveNeeded` removed).
2. Add / edit / remove / reorder mark the form dirty through the form itself; Save enable/disable
   and the unsaved-changes guard behave identically to today.
3. Step name is validated by the form (`required` + `ARTIFACT_NAME_MAX`) instead of an empty name
   being submitted to the server.
4. Server entity / permission / async state continues to use signals.
5. Dead legacy fields removed from `use-case-editor.ts`.
6. Characterization tests cover inline edit, dialog Apply, drag-reorder, and the guard on dirty
   steps — passing before and after.
7. Step-detail edit dialog migration is explicitly out of scope (follow-on).
