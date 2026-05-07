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
    // (SSE connection initiated by auth-layout's connect(['Project:0'])).
    // Without the SSE request being in flight, a backend broadcast that
    // fires before the client subscribes would be lost — observing the
    // request guarantees the connection is at least established.
    const sseRequestPromise = page.waitForRequest('**/events/stream**');
    await Promise.all([
      page.waitForResponse(r => r.url().includes('/api/projects') && r.status() === 200),
      page.goto('/projects'),
    ]);
    await sseRequestPromise;
    await expect(sidebar.tree()).toBeVisible();
    await sidebar.expectProjectInTree(projectName);

    // Detect the SSE-driven /api/projects reload that fires AFTER the
    // EditGoal command is dispatched. Setting up the listener BEFORE the
    // edit is what scopes it to the post-edit reload (Playwright's
    // waitForResponse only matches responses that arrive after the
    // listener is installed).
    const sseRefreshPromise = page.waitForResponse(
      r => r.url().includes('/api/projects') && r.status() === 200,
      { timeout: 15_000 }
    );

    // Trigger an EditGoal via the API — this is the "edit a goal in one
    // context" simulation. The goal is created, the backend runs both
    // publishEntityChangedIfPresent (Goal:newId — sidebar isn't subscribed
    // to that, but the editor in the multi-context test below is) and
    // publishProjectChangedIfScoped (Project:0 — the sidebar IS).
    await createGoal(request, projectName, goalName, 'SSE refresh test goal');

    // The SSE event must arrive within the 15s timeout. Awaiting the
    // second /api/projects response is the strongest signal we can take
    // from outside the app — we never asked Playwright to navigate or
    // reload, so this response could only have come from the sidebar's
    // events$ subscriber firing on the Project broadcast.
    await sseRefreshPromise;

    // Re-render of the tree with `expanded: false` collapses any nodes
    // the user (or earlier test step) had open, so we expand explicitly
    // here before asserting the freshly-loaded count. The data inside
    // the project node now reflects the new goal — Goals (1) — without
    // the test ever issuing a navigation or page.reload().
    await sidebar.expandProject(projectName);
    await sidebar.expectEntityGroup(projectName, /^Goals \(1\)$/);

    await page.close();
  });

});
