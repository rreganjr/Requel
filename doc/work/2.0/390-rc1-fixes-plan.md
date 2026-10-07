# #390: rc1 acceptance fixes, plan

Issue: https://github.com/rreganjr/Requel/issues/390 · branch `390-rc1-fixes` · epic #375 · found by #378.

## 1. Hide sidebar

- `layout.ts`: add `.sidebar[hidden] { display: none; }` beside the `.sidebar` rule.
- Auto-collapse (his call, 2026-10-06): below 768px wide the sidebar starts collapsed, whatever the stored preference, and collapses when the window narrows past 768px (`matchMedia`). A toggle while narrow isn't persisted, so the wide-screen preference survives.
- Tests: `layout.spec.ts`: a narrow start collapses without persisting the toggle; crossing the breakpoint collapses and widening restores the preference. e2e (`sidebar.e2e.ts`): Hide sidebar hides the aside; a 390px window starts collapsed and the page gets the width. jsdom doesn't apply component styles, so the `display` check is e2e only.

## 2. Dark-mode form fields

- Root cause wider than form fields: Aura's dark tokens (197 of them: form fields, select/popover/modal overlays, lists, menus, and the component dark schemes: buttons, toasts, messages, ...) reference `{surface.N}` assuming surface.0 is white. Requel's dark ramp is inverted so app CSS reading `--p-surface-0` flips, which turned all of them inside out.
- Fix: `requel-preset.ts` derives a preset from Aura's dark tokens with `{surface.N}` rewritten to `{slate.N}` (the colour Aura intended) and layers it between Aura and Requel's own preset, so Requel's explicit dark tokens still win. Light mode is untouched.
- Test: `requel-preset.spec.ts`: form fields and select overlay get slate-950/slate-0/slate-900; no Aura dark token still references the surface ramp; Requel's dark surface/content/text tokens are unchanged. Checked by hand in dark mode.

## 3. Stakeholder editor locks after save

- `PermissionService.refresh()`: keep one in-flight promise. A second caller while a refresh is running gets the same promise instead of finding `_loadedProject` cleared and returning at once.
- Same fix for `loadForProject` while a load is pending.
- `refresh()` also no longer clears first: the current permissions stay until the new ones land, and a `clear()` (project change) during the fetch drops its result.
- Tests: `permission.service.spec.ts`: overlapping `refresh()` calls share one fetch, keep the old permissions while it runs, and both resolve with the new ones; a refresh finishing after `clear()` is dropped. e2e (`stakeholders.e2e.ts`): a save leaves the grantable boxes enabled.

## 4. Non-admin with Stakeholder Edit: users

- Decided 2026-10-06: new `GET /api/projects/{name}/stakeholder-candidates`: users who aren't already user stakeholders of the project, as `{username, name}` only (no email). Requires `Stakeholder[Edit]` on the project (403 otherwise, 404 for an unknown project). Admin passes like anyone holding the permission.
- Editor: create mode loads candidates from it instead of `GET /api/users`; edit mode loads no list and shows the stakeholder's own user in the disabled select.
- `/api/users/**` stays admin-only.
- Candidates are users with ProjectUserRole (EditUserStakeholder needs it), excluding assistant identities (AssistantUserRole and the legacy `assistant`).
- Tests: `ProjectQueryControllerTest` (holder gets the filtered list without emails, non-holder 403, unknown project 404); editor spec for both modes; e2e as the non-admin `project` user: Add User offers the candidate and not an existing stakeholder, with no load error.

## 5. Review `latencyMs`

- `JpaAssistantRunStore`: wherever a terminal update sets `completedAt`, also set `latencyMs = completedAt - startedAt` when `startedAt` is set (SUCCEEDED, PARTIAL, FAILED, SKIPPED).
- Test: `JpaAssistantRunStoreTest`: succeeded and failed runs get their latency; a run that never started has none.

## 6. INSTALL.md Docker run

- Add `--restart unless-stopped` to the Requel `docker run` in "Docker without Compose", with one line saying why (Requel exits if MySQL isn't accepting connections yet, and Docker restarts it).

## Not in scope

- Deleting users (the #378 test users stay).
