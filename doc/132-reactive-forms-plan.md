# Implementation Plan — #132 3.1 Reactive forms + consistent validation

Part of the UI/UX remediation epic #124. Source: `doc/UI_UX_REVIEW.md` Finding 3.1. Phase 3.
Follows **N5 #158** (`app-field` + `app-form-wizard` + `form-errors`), which merged as PR #170.

Grounded in `requel-angular` on `release/2.0` at **`4ac9b73`** — i.e. *after* the N5 primitives
and the Goal/Story pilots landed. The current body of issue #132 was written before that and is
stale in ways that change the work; §10 lists the ticket edits.

This plan rolls the N5 pattern out to the eleven remaining form surfaces, and takes on three
things #132's body left ambiguous: the create-flow wizards, the command-error adapter, and a
two-column row group.

## Decisions (locked)

1. **Wizard where create hides authorable content; rows everywhere else.** The rule is
   structural: if a create form gates *authorable* fields or add/remove affordances behind
   `@if (!isNew())`, the user is being asked to save a half-configured entity before they can
   finish configuring it — that becomes an `app-form-wizard`. If every input is already visible
   on create, a wizard adds chrome and buys nothing. §1 classifies all eleven; five convert.
2. **Read-only derived sections are not gated authorable content and do not become steps.**
   "Referenced By", "Alternate Terms", and `app-annotations-section` render *against* a
   persisted entity and take no input. They stay gated and stay outside the wizard, exactly as
   #158 left annotations. This is why `term-editor` and `report-editor` — which do have
   `!isNew()` blocks — are rows-only, not wizards. §1.3 records that call explicitly so it
   isn't re-litigated in review.
3. **Backend `@Size` constraints land first, as a blocker.** There is exactly **one** bean
   validation size constraint in all of `modules/*/src/main/java` — `UserImpl:385`,
   `@Size(min = 1)` on roles. No `@Size` and no `@Column(length=…)` on any artifact `name` or
   `text`; `EditGoalInput` carries only `@NotBlank`. Rather than invent client-side caps,
   **#171** adds the real constraints and #132 mirrors them. §2.3 specifies that issue and §9
   covers the sequencing consequence.
4. **Password rules are `minLength(1)` + `maxLength(128)`, sourced from
   `UserImpl.MAX_PASSWORD_LENGTH`.** That constant is the only password bound the server
   enforces (`isValidPassword` = non-blank and `≤ MAX_PASSWORD_LENGTH`). The client mirrors it
   rather than inventing a stronger policy, so the form never rejects a password the server
   would accept. A real password policy is a product decision and out of scope.
5. **#132 ships the command-error adapter; #133 keeps the inline-vs-toast policy.** #132 is the
   ticket holding all eleven forms, so it is the cheapest place to build `applyCommandErrors`
   and the name mapping it needs. #133 narrows to where errors *render* (inline for blocking,
   toast for non-blocking confirmations only) and the `aria-live` behavior. §4 draws the line
   and §4.2 handles the property-name problem, which is the real work.
6. **`app-field` gains a two-column group variant.** `project-editor` and `user-editor` stay
   dense rather than collapsing to a long single-column stack. This amends a primitive #158
   shipped one commit ago; §5 keeps the change additive so no existing caller changes behavior.
7. **Dirty checking is derived from the form; every hand-rolled `trackChanges()` is deleted.**
   All eleven editors implement `DirtyCheckable` with a bespoke change-tracker. Once a control
   owns the value, `hasUnsavedChanges()` is `this.form.dirty`, and the
   `(ngModelChange)="trackChanges()"` wiring, the `hasChanges()` signals, and the
   `detectChanges()` timing workarounds (`user-editor.ts:155-156`, `settings.ts:122-125`) go
   with them.
8. **`settings.ts` and `report-editor.ts` are in scope.** Neither appears in #158's
   out-of-scope enumeration, and `settings.ts:122-125` is cited in #132's own Problems list.
   They are small and no other ticket would pick them up.
9. **Split three ways; #132 stays at 8.** `doc/158-form-wizard-field.md` §7 repointed #132 to
   8 assuming a rows-and-validators sweep, and that number is right — for that scope. The five
   wizards (§1.1), the two-column variant (§5), and the version-contract hazard the Goal/Story
   pilots did not face (§3) move to their own issues. #132 keeps the helpers and the six
   rows-only editors. §9 has the table.

## 1. Inventory and classification

Eleven surfaces remain on `[(ngModel)]`. Applying decisions 1 and 2:

### 1.1 Wizards — create gates authorable content

| Editor | Gated behind `!isNew()` | Steps |
|---|---|---|
| `project-editor.ts` | Tags (`app-tag-selector`) | 2 · Details → Tags |
| `actor-editor.ts` | Goals (add/remove via `app-entity-selector-dialog`) | 2 · Details → Goals |
| `stakeholder-editor.ts` | Goals | 2 · Details (incl. Permissions) → Goals |
| `scenario-editor.ts` | **Steps** (add / edit / remove / sub-scenario) | 2 · Details → Steps |
| `use-case-editor.ts` | Primary Scenario, Additional Scenarios, Goals, Stories, Additional Actors | 4 · Details → Scenarios → Goals & Stories → Actors |

Notes:

- `scenario-editor` is the highest-value conversion in the ticket. Steps *are* the scenario;
  gating them means today's create flow produces an empty scenario and forces the user to
  navigate back in to write it.
- `use-case-editor` is the largest (749 lines, five gated sections) and warrants its own PR.
- `stakeholder-editor` has two modes (`isUserType()`, user-backed / non-user) and Permissions
  is *already* visible on create, so only the Goals step is new chrome. The mode selector is
  `[disabled]="!isNew()"` and must stay on step 1.
- `actor-editor`'s "Referenced By" tables (use cases, stories) are derived and read-only; they
  stay edit-only per decision 2.

### 1.2 Rows only — all inputs visible on create

| Editor | Controls | Validators |
|---|---|---|
| `term-editor.ts` | Term, Definition, Canonical Term (`p-select`) | name required |
| `report-editor.ts` | Name, XSLT Template (textarea) | name required |
| `user-editor.ts` | Username, Name, Email, Phone, Organization, Password, Confirm, Roles + per-role Permissions | username & name required, email format, password 1–128, confirm matches, ≥1 role |
| `edit-account.ts` | Username (disabled), Name, Email, Phone, Organization, New Password, Confirm | name required, email format, password 1–128 when non-blank, confirm matches |
| `settings.ts` | Sidebar Project Limit (number), Staleness Threshold (`p-select`) | limit required, integer, ≥1 |
| `login.ts` | Username, Password | both required |

### 1.3 Why `term-editor` and `report-editor` are not wizards

Both have `!isNew()` blocks, so they look like wizard candidates on a grep. They aren't:

- `term-editor` gates **Alternate Terms** and **Referenced By** — both derived from
  `term()?.alternateTerms` / `term()?.referers`, both additionally gated on `.length`, and
  neither editable. Its three inputs are all on screen at create.
- `report-editor` gates the **Run** button and annotations. Run is an action on a saved
  template, not a creation step. Its two inputs are both on screen at create.

Wrapping either in wizard chrome would produce a one-step wizard, which is a worse form.

## 2. The validation contract

### 2.1 What `form-errors.ts` gains

N5 shipped the message map for `required | minlength | maxlength | email | pattern` with
per-field overrides. #132 adds the two cross-field validators the user surfaces need, as
exported factories so no editor hand-rolls them:

```ts
/** Cross-field: confirm must equal the password control. Error key: `passwordMismatch`. */
export function passwordsMatch(passwordKey: string, confirmKey: string): ValidatorFn;

/** At least one selection in a multi-select / checkbox-group control. Error key: `atLeastOne`. */
export function atLeastOne(): ValidatorFn;
```

Both need entries in `DEFAULT_FORM_ERRORS` and in `ERROR_PRECEDENCE`.

`passwordMismatch` is a **group-level** error, and `app-field.showError` reads
`control.invalid` — a group error does not mark its children invalid, so a naive group
validator renders no message anywhere. Attach the validator to the group *and* have it set the
error onto the confirm control, which keeps `app-field` unchanged and puts the message under
the field the user has to fix. Cover this in `form-errors.spec.ts` — it's the failure mode most
likely to ship silently.

### 2.2 The per-field validator table

| Field | Validator | Source of truth |
|---|---|---|
| Any artifact `name` | `required` + `maxLength(n)` | `@NotBlank` today; `n` from **#171** (§2.3) |
| Any artifact `text` / description | `maxLength(n)` | from **#171** (§2.3) |
| `emailAddress` | `email` | `type="email"` today; §2.3 should add `@Email` |
| `password` | `minLength(1)`, `maxLength(128)` | `UserImpl.MAX_PASSWORD_LENGTH` |
| `repassword` | `passwordsMatch` | client-only concern |
| Roles | `atLeastOne` | **`UserImpl:385` `@Size(min = 1)`** — already backend-backed |
| `sidebarProjectLimit` | `required`, `min(1)`, integer | UI concern |

`edit-account`'s password is *optional* ("leave blank to keep current"), so its validators are
conditional: apply `minLength`/`maxLength`/match only when the control is non-blank.

Keep the numbers in one exported constants module (e.g. `shared/validation-limits.ts`) that
names its backend source per entry, so the next person can diff it against the annotations
rather than guessing which numbers are real.

### 2.3 Blocker: backend `@Size` constraints — **#171**

**#132 is blocked by #171.** Scope:

- Add `@Size(max = …)` to the `name` and `text` fields on the `Edit*Input` records in
  `modules/service-api/src/main/java/com/rreganjr/requel/service/api/dto/` and to the
  corresponding JPA entity fields in `project-jpa` / `user-jpa`.
- Add `@Email` to the user/account email inputs so the client `email` validator mirrors
  something.
- Audit `@Column(length)` on the affected columns and add a Flyway migration wherever a column
  needs widening to match the chosen `@Size`.
- Confirm the chosen limits round-trip through XML import/export (`doc/samples/project.xsd`)
  and the MCP command gateway, both of which accept the same DTOs.

Note for whoever picks it up: `VerbNetFrame` already uses `length = 16277215` for text columns,
so the artifact text fields are likely effectively unbounded already and most of the real work
is on `name`. Pick limits deliberately rather than inheriting whatever the DDL happens to say.

Once #171 merges, #132 reads the values into `validation-limits.ts`. The
`maxlength` message wording already exists in `DEFAULT_FORM_ERRORS`, so no message work is
needed.

## 3. The version contract for the new wizards — this differs from Goal/Story

`doc/158-form-wizard-field.md` §2 established the rule: capture `id` **and** `version` from
`result.entity` on the step-1 commit, treat a held version as spent on use, and never carry one
across two mutations. §2 then narrowed the risk by verifying that the Goal/Story enrichment
commands *don't* touch the parent's `@Version` — `AssignTagCommandImpl` mutates the `Tag`, and
`EditGoalRelationCommandImpl` mutates a standalone `GoalRelationImpl`.

**That verification does not carry over.** `AddGoalToGoalContainerCommandImpl.execute()`
(`modules/project-jpa/.../command/`) ends with:

```java
addingContainer.getGoals().add(addedGoal);
addingContainer = getRepository().merge(addingContainer);
```

The container **is** the actor / stakeholder / use-case being created. Merging it bumps its
`@Version`. So for three of the five new wizards the Goals step *does* spend the parent's
version, and the belt-and-braces refetch that #158 deliberately dropped is **required here**.

Requirements for the wizard hosts:

- After **every** step commit — not just step 1 — refresh the held `version` from the command
  result, or refetch when the command does not return the parent. `AddGoalToGoalContainer`
  returns the container, so prefer the result.
- Verify per command rather than assuming. The ones this ticket touches and must be checked:
  `AddGoalToGoalContainer` / `RemoveGoalFromGoalContainer` (bumps — confirmed),
  `AddScenarioToUseCase` / `RemoveScenarioFromUseCase`, `SetPrimaryScenarioOnUseCase`,
  `AddStoryToStoryContainer`, `AddActorToActorContainer`, `EditScenarioStep` /
  `DeleteScenarioStep` / `CopyScenarioStep` / `ConvertStepToScenario`, and `AssignTag`
  (verified clean in #158).
- Keep each editor's existing `if (fromSSE && this.hasUnsavedChanges()) return;` guard intact
  inside the wizard — an SSE reload must refresh `version` without clobbering the active step.
  `story-editor` had no such guard until #158 added one, so check each of these five rather
  than assuming.
- A 409 keeps the step, refetches, and renders the "changed elsewhere — version refreshed"
  message in the wizard's `role="alert"` region. Never a silent overwrite, never a dead end.

**Required test per wizard:** create → advance to the last step → mutate an association →
navigate **back** to step 1 → edit the name → Continue. Asserts success, not 409. This is the
test that catches the `AddGoalToGoalContainer` bump.

## 4. The command-error adapter (owned here)

### 4.1 The adapter

Add to `src/app/shared/form-errors.ts`:

```ts
/**
 * Applies a failed CommandResult's field violations onto a form.
 *
 * Violations whose field resolves to a control set `{ server: message }` on that control.
 * Anything unresolved is returned so the caller can render it page-level — nothing is
 * ever dropped.
 */
export function applyCommandErrors(
  form: FormGroup,
  violations: FieldViolation[] | null | undefined,
  map?: Record<string, string>
): string[];
```

- `server` joins `ERROR_PRECEDENCE` last, so a live client-side complaint outranks a stale
  server one.
- A `server` error must clear on the next change to that control, or the user is stuck staring
  at an error they've already fixed with a disabled Save.

  **Implemented as a validator, not `setErrors` — and this matters more than it looks.**
  Angular's `updateValueAndValidity` reassigns `errors` from the validator result, so a
  directly-written key is dropped the next time validation runs. That gives the clear-on-edit
  behaviour for free, but validation also runs on **render**: `setUpControl` revalidates
  whenever a `[formControl]` directive initialises. Measured during slice 5 — a server
  violation mapped onto `user-editor`'s `roleNames` (bound to its role checkboxes via
  `[formControl]`) read back as `null` after a single `detectChanges()`, i.e. the user would
  never have seen it. So `applyCommandErrors` attaches a validator closed over a snapshot of
  the rejected value: it reports while the value is unchanged, returns null once edited, and
  survives revalidation either way. `clearServerErrors` detaches it.
- `FieldViolation.field` is nullable; a null field is command-level and goes straight into the
  returned array.
- Return type is deliberately `string[]` rather than `void`: the caller feeds it to
  `submitError`, which is how nothing gets silently swallowed.

### 4.2 The name-mapping problem — the actual work

`CommandController:136-157` builds `FieldViolation.field` from
`BeanValidationException.getEntityPropertyNames()` — **JPA entity property names**. The form's
control names come from the input DTO. They coincide often (`name`, `text`) and diverge exactly
where it matters (`emailAddress` vs the `email` control id, `encryptedPassword` vs `password`,
`selectedRoleNames` vs `roles`).

Two ways to close it. Do both, in this order:

1. **Now, in #132:** the optional `map` parameter — a small per-editor
   `{ entityProperty: controlName }` object living next to that editor's form definition.
   Eleven small maps, most of them empty. Unresolvable names fall through to page-level, so a
   missing entry degrades rather than breaks.
2. **Follow-up: #176.** Change `CommandController` to emit input-DTO field names instead of
   entity property names, then delete the maps. One backend change replacing eleven client
   maps. Worth filing now with a pointer back here, but not worth blocking on — the fallback
   path makes #132 correct either way.

### 4.3 What stays with #133

- Inline-vs-toast policy: inline `role="alert"` for blocking form/page errors, toasts reserved
  for non-blocking confirmations, `aria-live="polite"` for success.
- Retryable inline alert for network failures.
- Auditing the existing toast sites (`goal-editor.ts:361`, `tag-selector.ts:183`,
  `annotations-section.ts:311`) against that policy.

The existing `violations` **concatenation** in `project-editor.ts:260-264`,
`user-editor.ts:273-277`, and `edit-account.ts:178-181` is *replaced* by `applyCommandErrors` in
this ticket — those three sites are the adapter's first callers.

409 keeps its own path, using the `status` field `CommandService` already carries (see §3).

## 5. The two-column `app-field` group

`project-editor` and `user-editor` are the only two editors on `grid-template-columns: 1fr 1fr`.
They stay dense (decision 6), which means extending the N5 primitive.

Design constraint: **additive only**. `app-field` shipped one commit ago and `goal-editor` /
`story-editor` depend on its current behavior. Nothing here changes an existing caller.

```html
<app-field-group [columns]="2">
  <app-field label="Username" [control]="form.controls.username"> … </app-field>
  <app-field label="Name" [control]="form.controls.name"> … </app-field>
</app-field-group>
```

- `app-field-group` is a layout-only wrapper: a CSS grid of `columns` tracks that lays out
  child `app-field` hosts. It owns no label, no error, no ARIA.
- Each `app-field` keeps its own internal label/control grid, so a cell still reads label-left.
  Requires a `--rq-field-label-w` narrower inside a group — add a token rather than a literal.
- `app-field`'s existing `@container (max-width: 30rem)` collapse already stacks label above
  control per cell. The group needs its own container query to collapse `columns` → 1 first, so
  a narrow viewport degrades two-column → one-column → stacked, in that order.
- `divider` interacts badly with multi-column: a hairline under one cell of a two-cell row
  looks like a mistake. Either the group suppresses child dividers and draws its own per row,
  or callers pass `[divider]="false"` inside groups. Prefer the former — the group knows where
  its rows are, the caller shouldn't have to.
- Needs `app-field-group.spec.ts` and `app-field-group.a11y.spec.ts`. The a11y case that
  matters: reading order in a screen reader must follow visual order, and each control must
  still resolve to exactly one label.

If review finds the group fighting the row contract, the documented fallback is single column
with `app-card` section grouping (Identity / Contact / Password / Roles). Don't hold the
editor migrations on the group landing — sequence it first (§9) so they aren't blocked.

## 6. Per-editor work plan

Suggested slicing, smallest-risk first, each independently reviewable:

1. **`app-field-group`** (§5) — shared primitive, lands before its consumers.
2. **`applyCommandErrors` + `passwordsMatch` + `atLeastOne`** (§2.1, §4) — shared helpers with
   specs, no editor changes yet.
3. **`login.ts`, `settings.ts`** — 2 controls each, no wizard, no `DirtyCheckable` on login.
   Establishes the rows-only recipe end-to-end including the adapter.
4. **`term-editor.ts`, `report-editor.ts`** — rows only; deletes the imperative name checks at
   `term-editor.ts:277-280` and `report-editor.ts:199-202` that `required` now covers.
5. **`edit-account.ts`, `user-editor.ts`** — rows + `app-field-group`; the full validator table
   from §2.2 including conditional password and `atLeastOne` roles; deletes the
   `detectChanges()` workaround at `user-editor.ts:155-156` and both `1fr 1fr` grids.
6. **`project-editor.ts`, `actor-editor.ts`** — first two wizards, 2 steps each; `actor-editor`
   is where the §3 version bump first bites.
7. **`stakeholder-editor.ts`** — wizard, plus the `isUserType()` mode branch on step 1.
8. **`scenario-editor.ts`** — wizard with the Steps step, including the step-edit dialog.
9. **`use-case-editor.ts`** — wizard, 4 steps, largest surface. Own PR.

Every editor PR deletes that editor's local `.form-grid` block (they range from 120px to 160px
label columns, plus the two `1fr 1fr`) and its `trackChanges()` machinery.

## 7. Tests

Inherits the N5 harness — Vitest via `@angular/build:unit-test`, `TestBed`, and
`expectNoAxeViolations` from `src/app/shared/testing/a11y.ts`. Per migrated editor:

- **`*.spec.ts`** — validators fire as specified; Save/Continue disabled per decision;
  `hasUnsavedChanges()` tracks `form.dirty`; the command payload is byte-identical to what the
  `ngModel` version sent. That last one is the regression that matters: an entity created
  through the migrated form must equal one created before.
- **`*.a11y.spec.ts`** — axe clean with a field in its error state, not just at rest. Copy
  `goal-editor.a11y.spec.ts`.
- **Per wizard** — the §3 back-navigate-and-re-Continue test. Non-negotiable.
- **`form-errors.spec.ts`** — extend for `passwordsMatch` (including the group-vs-control
  placement in §2.1), `atLeastOne`, and `applyCommandErrors`: resolved field → control error,
  unresolved field → returned string, null field → returned string, `server` error clears on
  next edit, `server` loses to a live client error.
- **`app-field-group`** — §5.
- Remember `p-button` puts `data-testid` on the host: query `[data-testid="x"] button`.

Existing Playwright e2e driving these forms need selector updates where `app-field` generates
ids (`rq-field-{n}`). Pass `controlId` to keep stable ids on any control an e2e test targets.

**The concrete list, found while migrating `login`.** `#username` and `#password` are shared
across three page objects — `e2e/pages/LoginPage.ts:11`, `e2e/pages/UserEditorPage.ts:55,90,128,132`
and `e2e/account.e2e.ts:31,32,47,56` — so those two ids are a contract, not an implementation
detail. `login` therefore keeps `controlId="username"` / `controlId="password"` rather than
taking prefixed ids, and no e2e change was needed.

`user-editor` and `edit-account` (slice 5) have a subtler version of the same problem: today
`#password` is the **`p-password` host** and the tests reach the real input with
`.locator('#password').locator('input')`. Once those editors pass `controlId="password"` +
`inputId="password"`, `#password` becomes the inner input itself and that chained locator
resolves to nothing. Update `UserEditorPage.ts:90` and `account.e2e.ts:56` to
`.locator('#password')` in the same PR, or the e2e failure will look like a form bug.

## 8. Sequencing

```
#171 backend @Size (§2.3) ─┐
      #172 app-field-group ─┤
              #158 ✅ ─────┴─→ #132 ─┬─→ #173 wizards ─┐
                                     │                 ├─→ #138 (label/error sweep)
                                     └─────────────────┘
                                     └─→ #143, #144
```

- #132 is **blocked by #171** (decision 3), **#172**, and #158 (merged).
- #133 no longer blocks #132 — the dependency inverts, since #132 now ships the adapter and
  #133 consumes the rendering policy question. Update `scripts/update-ui-ux-subissues.sh`
  accordingly; it currently encodes only `add_blocked_by 132 158` (line 583). That dependency
  already exists on the issue, and `add_blocked_by` is idempotent, so the line is a no-op —
  #171 needs adding to the script for the next fresh run.
- #138 stays blocked by #132, and should gain **#173** as a blocker too.
- **#176** (§4.2, backend emits DTO field names) is independent and can land any time
  after #132.

## 9. Points and splitting

Split three ways, decided 2026-08-04. #132 keeps its existing 8 and the scope that number was
set for; the two pieces that were never in it get their own issues.

| Issue | Scope | Points |
|---|---|---|
| **#171** | Backend `@Size` / `@Email` / column audit + Flyway (§2.3) | 3 |
| **#172** | `app-field-group` two-column variant (§5) | 3 |
| **#132** | Helpers (§2.1, §4) + the six rows-only editors (§1.2) | 8 |
| **#173** | The five create-flow wizards (§1.1, §3) | 8 |

Dependency shape after the split:

- **#172 blocks #132** — `user-editor` and `edit-account` are in #132 and both need the
  two-column group. It is the smallest piece and should land first.
- **#132 blocks #173** — the wizards consume `applyCommandErrors`, `passwordsMatch`,
  `atLeastOne`, and `validation-limits.ts`, all built in #132.
- **#171 blocks #132** only for the `name`/`text` max-length values. Everything else in #132 is
  independent of it, so if #171 stalls, ship #132 with `validation-limits.ts` covering only what
  is already backend-backed (password from `UserImpl.MAX_PASSWORD_LENGTH`, roles from
  `UserImpl:385`) and add the `name`/`text` entries when #171 merges. One file changes.
- **#138** should wait on #173 as well as #132, since it sweeps labels and errors across all
  eleven surfaces.

§6's nine slices map to the split as: slice 1 → #172; slices 2–5 → #132; slices 6–9 → #173.

## 10. Ticket hygiene

### 10.1 #132 as it stands on GitHub

- **"What exists today" is stale.** It cites `goal-editor.ts:76-87` and
  `story-editor.ts:79-113` as `div.form-grid` + `[(ngModel)]`; both are reactive + `app-field`
  as of `4ac9b73`. The "few fields have native validators" framing predates N5 too.
- **No acceptance criteria on the issue.** `scripts/update-ui-ux-subissues.sh:417` defines
  `append_acceptance 132` with six criteria and none are on the issue — that script has not been
  run against it, though `create-ui-ux-lookandfeel.sh`'s `append_to_issue 132` *has* (the
  "Target look-and-feel" paragraph is present). Replace that heredoc with §10.2 before running
  it. Note the guard at line 82 is body-based: once criteria exist, re-running skips the issue,
  so the heredoc has to be right before the first run.
- **Title and body should narrow to the split scope** — the wizards and the group primitive now
  live in #173 and #172.
- **Blocked-by links.** #158 and #171 are linked (#158 was already there; re-adding returns
  HTTP 422 "Target issue has already been taken"). #172 still needs adding.
- **Story Points: 8**, via `scripts/set-points.sh 132 8`.
- **The violations bullet stays** in Recommendations — #132 owns it (decision 5) — but should
  name `applyCommandErrors` and point at §4.2 for the name-mapping caveat.

### 10.2 Acceptance criteria — #132 (helpers + rows-only editors)

- [ ] `login`, `settings`, `term-editor`, `report-editor`, `edit-account`, and `user-editor` use
      reactive forms and `app-field` rows; no `[(ngModel)]` and no local `.form-grid` remains in
      any of them.
- [ ] `form-errors.ts` gains `passwordsMatch`, `atLeastOne`, and `applyCommandErrors`, all with
      specs; no validation message text lives in a component.
- [ ] `passwordsMatch` renders its message under the confirm field — the group-level error is
      also set on the confirm control, since `app-field.showError` reads `control.invalid` and a
      group error does not mark children invalid. Covered by a spec.
- [ ] `applyCommandErrors` maps field violations onto controls via the per-editor
      `{ entityProperty: controlName }` map, returns anything unresolved for page-level display,
      drops `server` errors on the next edit to that control, loses to a live client-side error
      in `ERROR_PRECEDENCE`, and replaces the semicolon-joining in `project-editor`,
      `user-editor`, and `edit-account`.
- [ ] Validators match the §2.2 table for these six editors, with limits read from
      `shared/validation-limits.ts` — not hard-coded per editor. Password bounds come from
      `UserImpl.MAX_PASSWORD_LENGTH`; `name`/`text` bounds come from #171.
- [ ] `edit-account`'s password validators apply only when the control is non-blank.
- [ ] Save is disabled when `form.invalid || form.pristine || saving()`.
- [ ] Inline field errors render via `app-field`, associated by `aria-describedby` /
      `aria-invalid`; axe clean with fields in the error state, not just at rest.
- [ ] `hasUnsavedChanges()` derives from `form.dirty` in all six; every `trackChanges()`,
      `hasChanges()`, and `detectChanges()` change-tracking workaround in them is deleted —
      including `user-editor.ts:155-156` and `settings.ts:122-125`.
- [ ] Command-level failures render inline with `role="alert"`, not as a toast.
- [ ] An entity created through each migrated form is identical to one created by the previous
      form.
- [ ] All styling from `--rq-*` tokens; no literals.

### 10.3 Acceptance criteria — #173 (create-flow wizards)

- [ ] `project-editor`, `actor-editor`, `stakeholder-editor`, `scenario-editor`, and
      `use-case-editor` create flows are `app-form-wizard` flows with the steps in §1.1; their
      `@if (!isNew())` gates on authorable content are gone.
- [ ] Read-only derived sections ("Referenced By", annotations) stay gated and stay outside the
      wizard, per decision 2.
- [ ] Edit mode for all five uses `app-field` rows with no wizard chrome.
- [ ] `stakeholder-editor`'s `isUserType()` mode selector stays on step 1 and stays
      `[disabled]="!isNew()"`.
- [ ] Held `version` is refreshed from the command result after **every** step commit, not just
      step 1 — `AddGoalToGoalContainerCommandImpl` merges the container and bumps its
      `@Version`, so the Goal/Story precedent in `doc/158-form-wizard-field.md` §2 does not
      carry over.
- [ ] Each command listed in §3 is verified for whether it bumps the parent's `@Version`, rather
      than assumed.
- [ ] Each editor's `if (fromSSE && this.hasUnsavedChanges()) return;` guard survives inside the
      wizard, so an SSE reload refreshes `version` without clobbering the active step.
- [ ] A 409 keeps the step, refetches, and renders "changed elsewhere — version refreshed" in
      the wizard's `role="alert"` region. No silent overwrite, no dead end.
- [ ] **Per wizard:** create → advance to the last step → mutate an association → navigate back
      to step 1 → edit the name → Continue succeeds with no 409. This is the test that catches
      the `AddGoalToGoalContainer` bump.
- [ ] `hasUnsavedChanges()` derives from `form.dirty`; `trackChanges()` machinery in all five is
      deleted.
- [ ] Validators, error rendering, and `applyCommandErrors` reuse the #132 helpers — nothing
      re-implemented locally.
- [ ] An entity created through each wizard is identical to one created by the previous form.
- [ ] axe clean per editor with fields in the error state.

### 10.4 Acceptance criteria — #172 (`app-field-group`)

- [ ] `app-field-group` exists with a `columns` input, is layout-only (no label, no error, no
      ARIA of its own), and lays out child `app-field` hosts in a CSS grid.
- [ ] No existing `app-field` caller changes behavior — `goal-editor` and `story-editor` render
      identically before and after.
- [ ] Each cell still reads label-left, via a narrower `--rq-field-label-w` token inside a
      group rather than a literal.
- [ ] Responsive collapse degrades two-column → one-column → label-above-control, in that
      order: the group's own container query collapses `columns` first, then `app-field`'s
      existing `@container (max-width: 30rem)` stacks.
- [ ] The group suppresses child `divider` and draws its own per row, so a hairline never
      appears under one cell of a two-cell row.
- [ ] `app-field-group.spec.ts` and `app-field-group.a11y.spec.ts` exist; the a11y spec asserts
      screen-reader reading order follows visual order and each control resolves to exactly one
      label.
- [ ] Documented fallback if the group fights the row contract: single column with `app-card`
      section grouping. #132 is not held on the group landing.

### 10.5 Out of scope (all three)

- Mini-forms in annotations, tags, admin tools, and dialogs — **#134**.
- Inline-vs-toast policy and the toast-site audit — **#133** (§4.3).
- The broader form-labelling sweep — **#138**.
- Signal/state hygiene beyond the form itself — **#143**.
- Changing `CommandController` to emit DTO field names — **#176** (§4.2).
