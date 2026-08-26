import { test, expect } from './fixtures/auth';
import {
  createActor,
  createGoal,
  createProject,
  createStory,
  createUser,
  deleteProject,
  exportProjectXml,
} from './fixtures/api-helper';
import { LoginPage } from './pages/LoginPage';
import { ProjectsPage, ProjectEditorPage } from './pages/ProjectsPage';
import * as fs from 'fs';
import * as path from 'path';

const encodeRouteSegment = (value: string): string =>
  encodeURIComponent(value).replace(/[!'()*]/g, char =>
    `%${char.charCodeAt(0).toString(16).toUpperCase()}`
  );

test.describe('Project management', () => {

  test('admin sees New Project and Import actions', async ({ adminContext }) => {
    const page = await adminContext.newPage();
    const projectsPage = new ProjectsPage(page);

    await projectsPage.goto();
    await projectsPage.expectCreateActionsVisible();

    await page.close();
  });

  test('project user sees New Project and Import actions via createProjects permission', async ({ projectContext }) => {
    const page = await projectContext.newPage();
    const projectsPage = new ProjectsPage(page);

    await projectsPage.goto();
    await projectsPage.expectCreateActionsVisible();

    await page.close();
  });

  test('restricted project user without createProjects permission hides New Project and Import actions', async ({ browser, request }) => {
    const username = `e2e-project-restricted-${Date.now()}`;
    const password = 'RestrictedProject123!';
    await createUser(request, username, 'Restricted Project User', password, 'ProjectUserRole');

    const ctx = await browser.newContext();
    const page = await ctx.newPage();
    const loginPage = new LoginPage(page);
    const projectsPage = new ProjectsPage(page);

    await loginPage.goto();
    await loginPage.login(username, password);
    await loginPage.expectRedirectedToDashboard();

    await projectsPage.goto();
    await projectsPage.expectCreateActionsHidden();

    await ctx.close();
  });

  test('empty project list shows empty-state message', async ({ adminContext }) => {
    const page = await adminContext.newPage();
    const projectsPage = new ProjectsPage(page);

    await page.route('**/api/projects', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([]),
      });
    });

    await projectsPage.goto();
    await projectsPage.expectNoProjectsMessage();

    await page.close();
  });

  test('import project success shows banner and refreshed list entry', async ({ adminContext }) => {
    const importedProjectName = `e2e-imported-${Date.now()}`;
    const importFixture = path.resolve(process.cwd(), 'e2e', 'fixtures', 'import-project.xml');
    let imported = false;

    const page = await adminContext.newPage();
    const projectsPage = new ProjectsPage(page);

    await page.route('**/api/projects', async route => {
      const projects = imported
        ? [{
            id: 999,
            version: 1,
            name: importedProjectName,
            organizationName: 'E2E Test Org',
            status: 'New',
            createdBy: 'System Administrator [admin]',
            stakeholderCount: 0,
            goalCount: 0,
            storyCount: 0,
            useCaseCount: 0,
          }]
        : [];
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(projects),
      });
    });

    await page.route('**/api/commands/ImportProject', async route => {
      imported = true;
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          entityType: 'ImportProject',
          entity: {
            id: 999,
            version: 1,
            name: importedProjectName,
          },
        }),
      });
    });

    await projectsPage.goto();
    await projectsPage.importProjectFromFile(importFixture);
    await projectsPage.expectImportSuccess();
    await projectsPage.expectProjectInTable(importedProjectName);

    await page.close();
  });

  test('import project failure shows error banner', async ({ adminContext }) => {
    const importFixture = path.resolve(process.cwd(), 'e2e', 'fixtures', 'import-project.xml');

    const page = await adminContext.newPage();
    const projectsPage = new ProjectsPage(page);

    await page.route('**/api/projects', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([]),
      });
    });

    await page.route('**/api/commands/ImportProject', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: false,
          entityType: 'ImportProject',
          error: 'Import failed.',
          entity: null,
          violations: null,
        }),
      });
    });

    await projectsPage.goto();
    await projectsPage.importProjectFromFile(importFixture);
    await projectsPage.expectImportError('Import failed.');

    await page.close();
  });

  test('export project XML and re-import round-trips goals, actors, and stories', async ({ adminContext, request }, testInfo) => {
    test.setTimeout(60_000);

    const nonce = Date.now();
    const sourceName = `e2e-roundtrip-source-${nonce}`;
    const goalName = `Roundtrip Goal ${nonce}`;
    const actorName = `Roundtrip Actor ${nonce}`;
    const storyName = `Roundtrip Story ${nonce}`;

    await createProject(request, sourceName, 'XML round-trip source project');
    await createGoal(request, sourceName, goalName, 'goal text for round-trip');
    await createActor(request, sourceName, actorName, 'actor text for round-trip');
    await createStory(request, sourceName, storyName, 'Success', 'story text', actorName);

    // ----- Export via the API (deterministic byte capture) -----
    //
    // We deliberately do NOT drive the export through the UI Export button
    // here. The button's wiring (click → authenticated GET to the export
    // endpoint) is verified by a separate smoke test below; it relies on
    // HttpClient + a hidden <a download> bound to a blob: URL, which Playwright
    // can capture as a Download event but cannot reliably read bytes from
    // (download.saveAs() intermittently writes 0-byte files for blob: URL
    // sources because Chromium routes blob downloads through internal memory
    // rather than the network layer Playwright's CDP capture is wired into).
    //
    // For the round-trip data-integrity assertion we need deterministic bytes,
    // so we fetch the XML directly via the same authenticated GET that the
    // Angular HttpClient call hits. This still exercises the full
    // ProjectQueryController.exportProject endpoint and the streaming export
    // command — only the browser-side "save bytes to disk" step is replaced
    // with an API-side fetch.
    const xml = await exportProjectXml(request, sourceName);
    expect(xml, 'exported XML wraps a <project> element').toMatch(/<project[\s>]/);
    expect(xml, 'exported XML carries the source project goal').toContain(goalName);
    expect(xml, 'exported XML carries the source project actor').toContain(actorName);
    expect(xml, 'exported XML carries the source project story').toContain(storyName);

    // Persist the bytes in the test artifact directory so the import file
    // input has a real file to consume. Using testInfo.outputPath() keeps it
    // co-located with traces/screenshots.
    const exportPath = testInfo.outputPath('roundtrip-export.xml');
    fs.writeFileSync(exportPath, xml, 'utf-8');

    // ----- Re-import via the project list (UI) -----
    const page = await adminContext.newPage();
    const projectsPage = new ProjectsPage(page);

    await projectsPage.goto();
    const importResponsePromise = page.waitForResponse(response =>
      response.url().includes('/api/commands/ImportProject') &&
      response.request().method() === 'POST'
    );
    await projectsPage.importProjectFromFile(exportPath);
    const importResponse = await importResponsePromise;
    expect(importResponse.ok(), 'import command returns HTTP success').toBeTruthy();
    const importResult = await importResponse.json() as { entity?: { name?: string } };
    const importedName = importResult.entity?.name;
    expect(importedName, 'import command returns the imported project name').toBeTruthy();
    await projectsPage.expectImportSuccess();

    await projectsPage.expectProjectInTable(importedName!);

    // ----- Verify the imported entities show up in their respective lists -----
    const importedProjectPathName = encodeRouteSegment(importedName!);

    await page.goto(`/projects/${importedProjectPathName}/goals`);
    await expect(
      page.locator('p-table tbody tr', { hasText: goalName }).first(),
      'imported goal visible in goal list'
    ).toBeVisible();

    await page.goto(`/projects/${importedProjectPathName}/actors`);
    await expect(
      page.locator('p-table tbody tr', { hasText: actorName }).first(),
      'imported actor visible in actor list'
    ).toBeVisible();

    await page.goto(`/projects/${importedProjectPathName}/stories`);
    await expect(
      page.locator('p-table tbody tr', { hasText: storyName }).first(),
      'imported story visible in story list'
    ).toBeVisible();

    await page.close();
  });

  test('clicking Export button sends an authenticated GET to the project export endpoint', async ({ adminContext, request }) => {
    // UI smoke test for the Layer 1 production bug fix — onExport() must call
    // the export endpoint via Angular's HttpClient so the AuthInterceptor
    // attaches the JWT bearer. The previous implementation used
    // window.open(url, '_blank') which issued an unauthenticated navigation
    // and silently 401'd. We assert the click triggers exactly one request to
    // the export URL, that it carries an Authorization: Bearer header, and
    // that the server responds 200 OK. We do NOT assert on captured download
    // bytes here — see the round-trip test for why that path is unreliable.
    const nonce = Date.now();
    const sourceName = `e2e-export-click-${nonce}`;
    await createProject(request, sourceName, 'export-click smoke test');

    const page = await adminContext.newPage();
    const projectsPage = new ProjectsPage(page);
    const editorPage = new ProjectEditorPage(page);

    await projectsPage.goto();
    await projectsPage.openEditor(sourceName);
    await editorPage.waitForLoad(sourceName);

    const exportUrlRegex = new RegExp(
      `/api/projects/${encodeURIComponent(sourceName)}/export(?:\\?|$)`
    );
    const responsePromise = page.waitForResponse(r => exportUrlRegex.test(r.url()));
    await page.getByTestId('project-export').click();
    const response = await responsePromise;

    expect(response.status(), 'export endpoint returns 200 OK to the click').toBe(200);
    expect(
      response.request().headers().authorization,
      'click sends Authorization: Bearer header (proves AuthInterceptor ran)'
    ).toMatch(/^Bearer /);

    await page.close();
  });

  test('import change with no selected file is a no-op and does not call import', async ({ adminContext }) => {
    const page = await adminContext.newPage();
    const projectsPage = new ProjectsPage(page);
    let importCalled = false;

    await page.route('**/api/projects', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([]),
      });
    });

    await page.route('**/api/commands/ImportProject', async route => {
      importCalled = true;
      await route.fulfill({
        status: 500,
        contentType: 'application/json',
        body: JSON.stringify({ error: 'unexpected import call' }),
      });
    });

    await projectsPage.goto();
    await projectsPage.clearImportSelection();
    await expect.poll(() => importCalled).toBe(false);
    // Picking no file is a silent no-op: no import call, no error/warning banner.
    await expect(page.getByTestId('project-list-error')).toHaveCount(0);

    await page.close();
  });

  test('create new project → appears in project list', async ({ adminContext }) => {
    const projectName = `e2e-create-${Date.now()}`;
    const page = await adminContext.newPage();
    const projectsPage = new ProjectsPage(page);
    const editorPage = new ProjectEditorPage(page);

    await projectsPage.goto();
    await projectsPage.clickNewProject();

    await editorPage.fillName(projectName);
    await editorPage.fillDescription('Created by E2E test');
    await editorPage.save();

    // After save on a new project, navigates to /projects/<name>
    await page.waitForURL(`**/projects/${encodeURIComponent(projectName)}`);

    await projectsPage.goto();
    await projectsPage.expectProjectInTable(projectName);

    await page.close();
  });

  test('edit project name → new name shown in list', async ({ adminContext, request }) => {
    const originalName = `e2e-edit-orig-${Date.now()}`;
    const newName = `e2e-edit-new-${Date.now()}`;
    await createProject(request, originalName, 'Edit test project');

    const page = await adminContext.newPage();
    const projectsPage = new ProjectsPage(page);
    const editorPage = new ProjectEditorPage(page);

    await projectsPage.goto();
    await projectsPage.openEditor(originalName);
    await editorPage.waitForLoad(originalName);

    await editorPage.fillName(newName);
    await editorPage.save();

    await page.waitForURL(`**/projects/${encodeURIComponent(newName)}/edit`);

    await projectsPage.goto();
    await projectsPage.expectProjectInTable(newName);
    await projectsPage.expectProjectNotInTable(originalName);

    await page.close();
  });

  test('project save validation failure shows violation messages', async ({ adminContext, request }) => {
    const projectName = `e2e-save-violations-${Date.now()}`;
    await createProject(request, projectName, 'Save validation failure test');

    const page = await adminContext.newPage();
    const projectsPage = new ProjectsPage(page);
    const editorPage = new ProjectEditorPage(page);

    await page.route('**/api/commands/EditProject', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: false,
          entityType: 'EditProject',
          entity: null,
          error: null,
          violations: [
            { message: 'Project name is already used' },
            { message: 'Organization is required' },
          ],
        }),
      });
    });

    await projectsPage.goto();
    await projectsPage.openEditor(projectName);
    await editorPage.waitForLoad(projectName);

    await editorPage.fillDescription('trigger validation failure');
    await editorPage.save();

    await editorPage.expectError('Project name is already used; Organization is required');
    await expect(page).toHaveURL(new RegExp(`/projects/${encodeURIComponent(projectName)}/edit$`));

    await page.close();
  });

  test('project save generic failure shows error banner', async ({ adminContext, request }) => {
    const projectName = `e2e-save-error-${Date.now()}`;
    await createProject(request, projectName, 'Save generic failure test');

    const page = await adminContext.newPage();
    const projectsPage = new ProjectsPage(page);
    const editorPage = new ProjectEditorPage(page);

    await page.route('**/api/commands/EditProject', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: false,
          entityType: 'EditProject',
          entity: null,
          error: 'Server rejected the project update.',
          violations: null,
        }),
      });
    });

    await projectsPage.goto();
    await projectsPage.openEditor(projectName);
    await editorPage.waitForLoad(projectName);

    await editorPage.fillDescription('trigger generic failure');
    await editorPage.save();

    await editorPage.expectError('Server rejected the project update.');
    await expect(page).toHaveURL(new RegExp(`/projects/${encodeURIComponent(projectName)}/edit$`));

    await page.close();
  });

  test('project save network failure shows fallback error banner', async ({ adminContext, request }) => {
    const projectName = `e2e-save-network-${Date.now()}`;
    await createProject(request, projectName, 'Save network failure test');

    const page = await adminContext.newPage();
    const projectsPage = new ProjectsPage(page);
    const editorPage = new ProjectEditorPage(page);

    await page.route('**/api/commands/EditProject', async route => {
      await route.abort('failed');
    });

    await projectsPage.goto();
    await projectsPage.openEditor(projectName);
    await editorPage.waitForLoad(projectName);

    await editorPage.fillDescription('trigger network failure');
    await editorPage.save();

    await editorPage.expectAnyError();
    await expect(page).toHaveURL(new RegExp(`/projects/${encodeURIComponent(projectName)}/edit$`));

    await page.close();
  });

  test('cancel on project editor navigates back to list without saving', async ({ adminContext, request }) => {
    const projectName = `e2e-cancel-${Date.now()}`;
    await createProject(request, projectName, 'Cancel test');

    const page = await adminContext.newPage();
    const projectsPage = new ProjectsPage(page);
    const editorPage = new ProjectEditorPage(page);

    await projectsPage.goto();
    await projectsPage.openEditor(projectName);
    await editorPage.waitForLoad(projectName);

    await editorPage.fillName('should-not-be-saved');

    // Form is now dirty — Cancel triggers the dirty-check confirm() dialog.
    // Accept it ("OK" / leave page) so navigation to /projects proceeds.
    page.once('dialog', dialog => dialog.accept());
    await editorPage.cancel();

    await page.waitForURL('**/projects');
    await projectsPage.expectProjectInTable(projectName);
    await projectsPage.expectProjectNotInTable('should-not-be-saved');

    await page.close();
  });

  test('dirty guard: navigate away with unsaved changes shows confirm dialog; cancel stays on page', async ({ adminContext, request }) => {
    // ProjectEditorComponent implements hasUnsavedChanges() via projectForm.dirty
    const projectName = `e2e-dirty-${Date.now()}`;
    await createProject(request, projectName, 'Dirty guard test');

    const page = await adminContext.newPage();
    const projectsPage = new ProjectsPage(page);
    const editorPage = new ProjectEditorPage(page);

    await projectsPage.goto();
    await projectsPage.openEditor(projectName);
    await editorPage.waitForLoad(projectName);

    // Mark the form dirty
    await editorPage.fillName(`${projectName}-modified`);

    // Set up dialog handler BEFORE clicking (dialog fires synchronously on click)
    page.once('dialog', dialog => dialog.dismiss()); // dismiss = cancel = stay on page

    await page.getByRole('button', { name: 'Cancel' }).click();

    // Should still be on the project editor URL
    await expect(page).toHaveURL(new RegExp(`/projects/${encodeURIComponent(projectName)}/edit$`));

    await page.close();
  });

  test('dirty project switch via sidebar accepts Save & Switch and loads the target project', async ({ adminContext, request }) => {
    const originalName = `e2e-switch-orig-${Date.now()}`;
    const renamedName = `${originalName}-renamed`;
    const targetName = `e2e-switch-target-${Date.now()}`;
    await createProject(request, originalName, 'Switch source project');
    await createProject(request, targetName, 'Switch target project');

    const page = await adminContext.newPage();
    const projectsPage = new ProjectsPage(page);
    const editorPage = new ProjectEditorPage(page);

    await projectsPage.goto();
    await projectsPage.openEditor(originalName);
    await editorPage.waitForLoad(originalName);

    await editorPage.fillName(renamedName);

    const sidebarTree = page.getByTestId('sidebar-tree');
    await sidebarTree.getByText(targetName, { exact: true }).click();

    await expect(page.getByRole('button', { name: 'Save & Switch' })).toBeVisible();
    await page.getByRole('button', { name: 'Save & Switch' }).click();

    // The sidebar navigates to the target's workspace overview (#154), not its editor.
    await expect(page).toHaveURL(new RegExp(`/projects/${encodeURIComponent(targetName)}$`));
    await expect(page.getByTestId('project-workspace')).toContainText(targetName);

    await projectsPage.goto();
    await projectsPage.expectProjectInTable(renamedName);
    await projectsPage.expectProjectInTable(targetName);

    await page.close();
  });

});
