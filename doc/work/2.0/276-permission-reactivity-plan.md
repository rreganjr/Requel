# 276 — a granted permission does not surface its control until reload (plan)

Issue: https://github.com/rreganjr/Requel/issues/276

## Corrections to the ticket's causes

Causes 1 and 3 hold. `PermissionService.clear()` has exactly one reference in the tree, in
`permission.service.spec.ts:61`. Every consumer snapshots — and there are ten, not nine:
`use-case-editor.ts:528` does it too. `stakeholder-editor.ts:582` already reads live
(`return this.permissionService.canEdit('Goal')`), so the shape this ticket wants exists in the
codebase already.

**The snapshot was deliberate, and converting does not undo it.** `git log -S` across all refs finds
no `computed` permission ever existing here — the snapshot arrived in `569164f6` under #38, whose
message includes "fix permission button timing". Declaring `canEdit = signal(false)` and setting it
once the fetch resolves is what stopped controls rendering before permissions were known. A
`computed` keeps that: before the load `_permissions()` is null, `hasPermission` returns false, so
the control reads false exactly as it does now, then follows the signal afterwards.

One behaviour genuinely changes: a control can now disappear while someone is looking at the form,
because a revoke elsewhere propagates immediately instead of waiting for navigation. That is the
point of the revocation AC, but it is new, and worth watching for in an editor mid-edit.

**Cause 2 is already solved.** `clear()` sets `_loadedProject = null`, so the `loadForProject` guard
falls through after a clear. No `force` argument is needed — only a caller that clears first.

**Cause 4 is wrong, and it makes the expensive item cheap.** Two SSE events already fire on every
permission grant. `EditUserStakeholderCommandImpl` implements both `ProjectScopedCommand` and
`EditProjectOrDomainEntityCommand`, so `CommandEventPublisher.publishProjectChangedIfScoped`
broadcasts `Project:0` and `publishTargeted` publishes a `Stakeholder:<id>` refresh. `layout.ts:344`
already calls `connect(['Project:0'])`, so the shell is subscribed. The wire and the subscriber both
exist; nothing maps either event to permission invalidation. Cross-session propagation is therefore
mostly a client-side change, and stays in this ticket rather than splitting out.

## Not a security boundary

Authorization is enforced at the command layer regardless of what the UI shows —
`AuthorizableCommand`, with `CommandGatewayIT` asserting `UNAUTHORIZED` on a permission the caller
does not hold. A control left visible after a revoke is a usability defect, not privilege
escalation. The revocation AC is about the UI telling the truth, not about closing a hole.

## Decision: a dedicated permission event, not the Project broadcast

Reusing `Project:0` would work and cost nothing to publish, but that broadcast fires on **every**
project-scoped command — every goal edit, every scenario save — so every session viewing the project
would re-fetch its permissions on every write by anyone. A round trip per session per command, to
catch an event that happens rarely.

So: a dedicated target. `CommandEventPublisher` publishes
`publishTargetUpdate("Permissions", PERMISSIONS_BROADCAST_ID, {"type":"refresh"})` when the command
it just ran was a stakeholder-permission write, and the shell subscribes to `Permissions:0` alongside
`Project:0`.

Broadcast rather than `Permissions:<projectId>` because the shell connects before a project is
chosen, and permission writes are rare enough that waking every session costs nothing measurable —
unlike the project broadcast, whose volume is the whole problem. A client with no active project
ignores it. If that ever becomes noisy, `/events/stream/subscriptions` already supports dynamic
subscription and the target can be narrowed without touching the publisher.

Publishing stays in `CommandEventPublisher` (service-impl), inspecting the command, rather than
injecting the publisher into `EditUserStakeholderCommandImpl` (project-jpa). That matches how every
other event is published and keeps SSE out of the domain module.

## Changes

**`modules/service-impl/.../command/CommandEventPublisher.java`** — publish the permission event when
the command is an `EditUserStakeholderCommand`. Nothing about the payload identifies a user or a
permission: it says "permissions changed", and each client re-fetches its own. No permission data on
the wire.

**`requel-angular/src/app/core/permission.service.ts`** — expose the permissions signal (or a
`computed` over it) so consumers can derive. Keep `clear()` and the existing guard as they are; add
a `refresh(projectName)` that clears and re-loads, which is what both the write path and the event
listener call.

**`requel-angular/src/app/core/event-stream.service.ts`** — subscribe `Permissions:0` at connect
time (`layout.ts:344` passes the list).

**A listener** — on a `Permissions` event, call `permissionService.refresh(activeProject)`. Home is
`layout.ts`, which already owns the connection and the `Project:0` subscription.

**The consumers — eighteen files, not the ten first counted.** My earlier figure came from a
truncated grep; the ticket's nine and my ten both undercounted. The full set adds `story-editor.ts`,
`story-list.ts`, `actor-list.ts`, `actor-editor.ts`, `scenario-editor.ts`, `scenario-list.ts`,
`report-list.ts` and `report-editor.ts` to `goal-editor.ts`, `goal-list.ts`, `project-editor.ts`,
`project-workspace.ts` (`loadCanDelete`), `term-list.ts`, `term-editor.ts`, `stakeholder-list.ts`,
`stakeholder-editor.ts`, `use-case-list.ts` and `use-case-editor.ts`. Converting only the first ten
would have left a grant updating goals and terms while stories and scenarios stayed stale — a worse
state than the bug, because it would look fixed. Convert `this.canX.set(permissionService.canX('T'))` to
`computed(() => permissionService.canX('T'))`. `hasPermission` reads the signal, so a `computed`
wrapping it tracks correctly with no service change. Templates calling `canX()` stay
source-compatible; the writable-signal declarations become computed and any other `.set()` on them
has to go.

**Same-session write path** — after a stakeholder-permission write succeeds, call `refresh()`
directly rather than waiting for the event to come back round. `stakeholder-editor.ts` is where the
grant happens.

## Tests

- `permission.service.spec.ts` — `refresh()` re-fetches where a bare `loadForProject()` would no-op.
- A component spec — change the service's permissions and assert a control's visibility follows
  without re-running the component's load path. That is the assertion that would have caught this.
- `CommandEventPublisherTest` — an `EditUserStakeholder` command publishes the permission event; an
  unrelated project-scoped command does not.
- `event-stream.service.spec.ts` / layout spec — a `Permissions` event triggers a refresh.
- Revocation in both directions, since the AC names it specifically.

## Out of scope

- Narrowing `Permissions:0` to a per-project target. Noted above as the escape hatch if needed.
- The `Project:0` broadcast's own volume. It is loud, but that is a separate question from this bug.
- Any change to server-side authorization. It is already correct and is not what this fixes.

## Gate

`mvn clean verify` for the publisher change, and `npm test` in `requel-angular` for the rest —
most of this ticket is frontend, so the Maven gate alone proves almost none of it.
