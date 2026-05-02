import { test, expect } from './fixtures/auth';
import { createProject, deleteProject } from './fixtures/api-helper';
import { ProjectsPage, ProjectEditorPage } from './pages/ProjectsPage';
import * as path from 'path';

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
    await projectsPage.clickProject(originalName);
    await editorPage.waitForLoad(originalName);

    await editorPage.fillName(newName);
    await editorPage.save();

    await page.waitForURL(`**/projects/${encodeURIComponent(newName)}`);

    await projectsPage.goto();
    await projectsPage.expectProjectInTable(newName);
    await projectsPage.expectProjectNotInTable(originalName);

    await page.close();
  });

  test('cancel on project editor navigates back to list without saving', async ({ adminContext, request }) => {
    const projectName = `e2e-cancel-${Date.now()}`;
    await createProject(request, projectName, 'Cancel test');

    const page = await adminContext.newPage();
    const projectsPage = new ProjectsPage(page);
    const editorPage = new ProjectEditorPage(page);

    await projectsPage.goto();
    await projectsPage.clickProject(projectName);
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
    await projectsPage.clickProject(projectName);
    await editorPage.waitForLoad(projectName);

    // Mark the form dirty
    await editorPage.fillName(`${projectName}-modified`);

    // Set up dialog handler BEFORE clicking (dialog fires synchronously on click)
    page.once('dialog', dialog => dialog.dismiss()); // dismiss = cancel = stay on page

    await page.getByRole('button', { name: 'Cancel' }).click();

    // Should still be on the project editor URL
    await expect(page).toHaveURL(new RegExp(`/projects/${encodeURIComponent(projectName)}$`));

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
    await projectsPage.clickProject(originalName);
    await editorPage.waitForLoad(originalName);

    await editorPage.fillName(renamedName);

    const sidebarTree = page.locator('.sidebar-tree');
    await sidebarTree.getByText(targetName, { exact: true }).click();

    await expect(page.getByRole('button', { name: 'Save & Switch' })).toBeVisible();
    await page.getByRole('button', { name: 'Save & Switch' }).click();

    await expect(page).toHaveURL(new RegExp(`/projects/${encodeURIComponent(targetName)}$`));
    await expect(page.locator('#name')).toHaveValue(targetName);

    await projectsPage.goto();
    await projectsPage.expectProjectInTable(renamedName);
    await projectsPage.expectProjectInTable(targetName);

    await page.close();
  });

});
