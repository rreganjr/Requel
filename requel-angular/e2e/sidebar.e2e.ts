import { test, expect } from './fixtures/auth';
import {
  createProject, createGoal, createActor, createStory,
  exportProjectXml,
} from './fixtures/api-helper';
import { SidebarPage } from './pages/SidebarPage';
import * as fs from 'fs';

/**
 * Tests for the global app sidebar (`app-sidebar-nav`).
 *
 * Currently covers:
 *   - Import project XML via the sidebar Import button → the new project
 *     appears in the sidebar tree, and its entity-group children render
 *     once the node is expanded.
 *   - SSE-driven tree refresh: when a project-scoped command fires in
 *     the same context (or any other context sharing the backend), the
 *     sidebar reloads `/api/projects` without a manual page refresh.
 *     Backend wiring: `CommandController.publishProjectChangedIfScoped`
 *     broadcasts `Project:0` (PROJECT_BROADCAST_ID); auth-layout's
 *     `connect(['Project:0'])` is the matching client subscription;
 *     `SidebarNavComponent.ngOnInit` re-runs `loadProjects()` on every
 *     `targetType === 'Project'` envelope.
 *
 *     The SSE-readiness wait is on the GET `/events/stream` *response*
 *     (not the request) — the URL-param subscription is registered
 *     server-side before headers are returned, so once we've seen the
 *     200 we know any subsequent `Project` broadcast will reach this
 *     client. Waiting only on the request being sent left a race where
 *     the `EditGoal` broadcast could fire before subscription
 *     registration completed.
 */
test.describe('Sidebar', () => {

  test('import project XML via sidebar → project appears in tree with entity groups', async ({ adminContext, request }, testInfo) => {
    test.setTimeout(60_000);

    // ----- Build a populated source project, then export it to XML so we
    // have a real, validate-able file to feed the sidebar import input. We
    // can't use the placeholder import-project.xml fixture here because the
    // assertion explicitly cares about entity-group children rendering with
    // non-zero counts after import.
    const nonce = Date.now();
    const sourceName = `e2e-sidebar-import-${nonce}`;
    const goalName = `Sidebar Import Goal ${nonce}`;
    const actorName = `Sidebar Import Actor ${nonce}`;
    const storyName = `Sidebar Import Story ${nonce}`;

    await createProject(request, sourceName, 'Sidebar import source project');
    await createGoal(request, sourceName, goalName, 'goal text');
    await createActor(request, sourceName, actorName, 'actor text');
    await createStory(request, sourceName, storyName, 'Success', 'story text', actorName);

    const xml = await exportProjectXml(request, sourceName);
    const exportPath = testInfo.outputPath('sidebar-import.xml');
    fs.writeFileSync(exportPath, xml, 'utf-8');

    // ----- Drive the sidebar import via the UI -----
    // The sidebar is visible on any authenticated route — /projects is the
    // standard landing page after login, so we use it as our entry point.
    const page = await adminContext.newPage();
    const sidebar = new SidebarPage(page);

    await Promise.all([
      page.waitForResponse(r => r.url().includes('/api/projects') && r.status() === 200),
      page.goto('/projects'),
    ]);
    await expect(sidebar.tree()).toBeVisible();

    // Wait for the ImportProject command response so we can read back the
    // server-assigned project name (the backend may rename on collision).
    const importResponsePromise = page.waitForResponse(response =>
      response.url().includes('/api/commands/ImportProject') &&
      response.request().method() === 'POST'
    );
    await sidebar.importProjectFromFile(exportPath);
    const importResponse = await importResponsePromise;
    expect(importResponse.ok(), 'sidebar import command returns HTTP success').toBeTruthy();
    const importResult = await importResponse.json() as { entity?: { name?: string } };
    const importedName = importResult.entity?.name;
    expect(importedName, 'import command returns the imported project name').toBeTruthy();

    // ----- "project appears in list" — sidebar tree shows the new project -----
    await sidebar.expectProjectInTree(importedName!);

    // ----- "entities visible in tree" — expand the imported project and
    // verify its entity-group children render with non-zero counts that
    // match what we exported (1 goal, 1 actor, 1 story). The sidebar
    // formats counts inline as "Goals (N)", "Actors (N)", "Stories (N)".
    await sidebar.expandProject(importedName!);
    await sidebar.expectEntityGroup(importedName!, /^Goals \(1\)$/);
    await sidebar.expectEntityGroup(importedName!, /^Actors \(1\)$/);
    await sidebar.expectEntityGroup(importedName!, /^Stories \(1\)$/);

    await page.close();
  });

  test('edit a goal via API → sidebar tree refreshes counts via SSE without a full page reload', async ({ adminContext, request }) => {
    test.setTimeout(30_000);

    // Project starts with zero goals so the SSE-driven refresh is observable
    // as a clean "Goals (0)" → "Goals (1)" count change.
    const nonce = Date.now();
    const projectName = `e2e-sse-sidebar-${nonce}`;
    const goalName = `SSE Sidebar Goal ${nonce}`;
    await createProject(request, projectName, 'SSE-driven sidebar refresh source');

    const page = await adminContext.newPage();
    const sidebar = new SidebarPage(page);

    // Wait for both /api/projects (initial sidebar load) AND /events/stream
    // RESPONSE (not just the request — see comment below). The Project:0
    // subscription rides in the URL params of `GET /events/stream`, so by
    // the time the server returns 200 OK headers it has parsed the param,
    // created the session, and registered the subscription. From that point
    // on any Project broadcast is queued/flushed to this client.
    //
    // Waiting only for the request being SENT (waitForRequest) is the
    // source of a known race: the test could fire `createGoal` before the
    // server had finished registering the subscription, in which case the
    // `Project:0` broadcast emitted by the EditGoal command would be lost
    // to this client and the sidebar refresh would never happen.
    await Promise.all([
      page.waitForResponse(r => r.url().includes('/api/projects') && r.status() === 200),
      page.waitForResponse(
        r => r.url().includes('/events/stream') &&
             !r.url().includes('/events/stream/subscriptions') &&
             r.status() === 200
      ),
      page.goto('/projects'),
    ]);
    await expect(sidebar.tree()).toBeVisible();
    await sidebar.expectProjectInTree(projectName);

    // Trigger an EditGoal via the API — this is the "edit a goal in one
    // context" simulation. The goal is created, the backend runs both
    // publishEntityChangedIfPresent (Goal:newId — sidebar isn't subscribed
    // to that, but the editor in the multi-context test in
    // sse-refresh.e2e.ts is) and publishProjectChangedIfScoped (Project:0
    // — the sidebar IS). By the time `await createGoal()` resolves, the
    // server has already emitted the broadcast on its way to writing the
    // HTTP response.
    await createGoal(request, projectName, goalName, 'SSE refresh test goal');

    // Poll the UI for the count update rather than catching a specific
    // `/api/projects` reload response. Two reasons:
    //   1. The user-facing outcome IS the count change. Asserting on the
    //      DOM is what we actually care about; the network request is a
    //      means to that end.
    //   2. Network-listener-based proofs are fragile around SSE timing.
    //      `expect(...).toPass()` retries the whole closure until it passes
    //      OR the timeout elapses, so SSE latency just delays the success
    //      of an iteration rather than killing the test on first attempt.
    //
    // We re-expand inside the closure on every iteration because
    // `projectTreeNodes` rebuilds with `expanded: false` whenever
    // `loadProjects()` re-runs (which is exactly what we're asserting
    // happened) — if we expanded once before the SSE refresh, the new
    // render would have collapsed the node, hiding the children we want
    // to inspect. The test never calls `page.reload()` or navigates, so
    // the only thing that could possibly drive a count change here is the
    // sidebar's `events$` subscriber firing on the Project broadcast.
    await expect(async () => {
      await sidebar.expandProject(projectName);
      await sidebar.expectEntityGroup(projectName, /^Goals \(1\)$/);
    }).toPass({ timeout: 15_000, intervals: [250, 500, 1_000, 2_000] });

    await page.close();
  });

});
