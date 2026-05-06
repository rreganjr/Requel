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
 *
 * Planned (not yet implemented — see RELEASE_20_TEST_PLAN.md §4.4):
 *   - Edit a goal in one tab → sidebar tree refreshes via SSE without a
 *     full page reload.
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

});
