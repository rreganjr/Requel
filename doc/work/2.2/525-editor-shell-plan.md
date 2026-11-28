# #525 — Shared editor shell for the entity editors, with a not-found state

https://github.com/rreganjr/Requel/issues/525

Branch `525-editor-shell` from `release/2.2`, PR to `release/2.2`. Size 5.

## Summary

The eight entity editors (goal, story, actor, use case, scenario, step, stakeholder, glossary term,
7,581 lines between them) each repeat the same frame. The frame is:

- a header with `app-page-header` and an actions row;
- the save-error banner and the changed-elsewhere banner;
- loading, load-error and form states;
- the Annotations and Sources sections.

A shared `app-entity-editor` shell takes over that frame and adds a not-found state. The editors
keep their forms, wizards, dialogs and logic, and move onto the shell one at a time in this
ticket's single PR.

Review against the tree: still needed, and the ACs stand.

- `app-page-header` exists, from #519. It is the title block only; the actions row and the
  narrow-window wrap are still copied into each editor.
- A missing id answers 404, for example `GET /api/projects/{name}/goals/{goalId}`. Every editor
  catches it as a generic "Failed to load ..." with a Retry that can't help, and still shows Copy,
  Delete, an empty `<h1>` and the two sections.
- The editors differ a little. The step editor has no changed-elsewhere banner. The stakeholder
  editor has no Annotations section. The use case and stakeholder editors carry their own test
  ids on Back. The shell keeps each of these as it is today ("behaviour unchanged otherwise").

## Locked decisions

1. **The shell renders Annotations and Sources itself.** The editor passes `projectName`,
   `entityType` and `entityId`, and gets `(issueResolved)` for the annotations section's
   `(resolved)`. So section order and the "only for a saved, found entity" rule live in one
   place. #501's approval controls can later go into this header, not eight. `showAnnotations`
   (default true) lets the stakeholder editor keep its current layout.
2. **One PR, eight editors.** It is mostly moving template code, and splitting it would cost
   extra CI runs and review cycles for little gain. Each editor's own unit and e2e tests are the
   check that nothing else changed.
3. **The not-found state comes from a 404 on the entity read.** It is shown with no actions, no
   heading text other than the not-found title, and no sections. Any other failure keeps today's
   error state with Retry.
4. **Editors keep their test ids.** The shell takes a `testid` prefix (for example `goal-editor`)
   and keeps the ids existing specs and e2e use: `-loading`, `-load-error`, `-error`. Action
   buttons stay in each editor's actions slot with their own ids (`goal-back` and so on).

## Contract

`app-entity-editor` (shared, standalone, OnPush):

- **Inputs:**
  - `title`: the entity's name, or "New Goal" and so on;
  - `entityLabel`: "Goal", "Use case", ... used by the not-found and loading text;
  - `state`: `'loading' | 'not-found' | 'error' | 'ready'`;
  - `loadError`: the error-state message;
  - `listLink`: the router link back to the entity's list;
  - `saveError`, `saveRetryable`: the submit-error banner;
  - `updateAvailable`, `updateMessage`: the changed-elsewhere banner, omitted for the step
    editor;
  - `projectName`, `entityType`, `entityId`, `showAnnotations`, `showSources`: the sections,
    rendered when `state === 'ready'` (each renders nothing while `entityId` is null, so a create
    wizard shows them once its first step has saved, as today). `showSources` lets the
    stakeholder editor keep Sources to a saved non-user stakeholder;
  - `testid`.
- **Outputs:** `retryLoad`, `retrySave`, `reloadUpdate`, `dismissUpdate`, `issueResolved`.
- **Slots:**
  - `[actions]`: Back, Copy, Delete, Review with AI. Not shown when not found.
  - The body: the form or wizard, the relationship sections, tags. It is an
    `<ng-template rqEditorBody>`, not plain projected content, because Angular builds projected
    content whether or not its `<ng-content>` is rendered; as a template it is built only when
    `state === 'ready'`.
  - `[after]`: dialogs and `p-confirmDialog`, rendered in every state, so a dialog's own
    lifecycle doesn't change.
- **Not-found:**
  - the header shows "‹Entity› not found";
  - the body says the item may have been deleted, with a link back to the list, testid
    `<testid>-not-found`;
  - no actions, no banners, no sections.
- **Header CSS** (the #519 narrow-window wrap) lives in the shell once and is removed from the
  eight editors.
- **`isNotFound(error)`** helper in `core/http-errors.ts`: an `HttpErrorResponse` with status
  404. Editors use it in their load `catch` to choose `not-found` over `error`.

## Steps

1. The shell and its spec, with axe on each state, especially not-found, plus `isNotFound` and
   its spec.
2. Move the editors one at a time, running that editor's spec after each:
   1. term (726 lines; no wizard);
   2. step (409; no banner);
   3. goal;
   4. actor;
   5. story;
   6. use case;
   7. stakeholder (no annotations; user and named variants);
   8. scenario (1,544).

   In each editor:
   - its header markup, banner and state branches become the shell's inputs and slots;
   - its copied header styles are removed;
   - its load `catch` sets the not-found state on 404;
   - it gains a "missing id shows not-found" spec.
3. e2e `editor-not-found-525`: open a goal id that doesn't exist. Expect the not-found state with
   its link to the list, and no Copy or Delete.

## Test plan

- **Shell spec:**
  - each state shows what it should and nothing else;
  - not-found has no actions, banners or sections, and its link goes to the list;
  - sections show only when ready with an id, and Annotations is hidden when `showAnnotations`
    is false;
  - outputs fire;
  - axe on the ready, not-found and error states.
- **Each editor's spec:**
  - a new case: the entity read answers 404 → `<testid>-not-found` is shown, and there's no
    Copy, Delete, `app-annotations-section` or `app-sources-section`;
  - all existing cases keep passing unchanged, as the check on behaviour.
- **e2e:** the new not-found test. The existing editor e2e suites run in CI as the regression
  check.

## Out of scope

- Redesigning the editors' forms.
- Any new action. #501 adds approval controls next, into the shell's header.

## Risks

- **Size.** This touches eight files of 400 to 1,500 lines. Moving them one at a time, with that
  editor's spec run after each, keeps a mistake local.
- **Projected content.** A dialog inside a state branch would be destroyed and re-created when
  the state changes. Dialogs go in `[after]`, which is always rendered, as they are now outside
  the state branches.
- **Test ids.** Specs and e2e page objects find elements by test id. The shell builds its ids
  from the editor's prefix, and the editors keep their action ids, so no locator changes.

## Acceptance criteria → where

| AC | Covered by |
|---|---|
| Each editor renders through the shell; header, states and section order from one place | Step 2; each editor spec, shell spec |
| A missing id shows the not-found state in every editor | a spec per editor; e2e `editor-not-found-525` |
| No axe violations on the not-found state | shell spec |
| Existing editor unit and e2e tests pass | the whole unit suite; CI e2e |
