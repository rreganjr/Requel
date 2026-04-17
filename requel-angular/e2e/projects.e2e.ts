import { test, expect } from './fixtures/auth';
import { createProject, deleteProject } from './fixtures/api-helper';
import { ProjectsPage, ProjectEditorPage } from './pages/ProjectsPage';

test.describe('Project management', () => {

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
    await projectsPage.clickProject(originalName); // waits for form data to load

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
    await projectsPage.clickProject(projectName); // waits for form data

    // Mark the form dirty
    await editorPage.fillName(`${projectName}-modified`);

    // Set up dialog handler BEFORE clicking (dialog fires synchronously on click)
    page.once('dialog', dialog => dialog.dismiss()); // dismiss = cancel = stay on page

    await page.getByRole('button', { name: 'Cancel' }).click();

    // Should still be on the project editor URL
    await expect(page).toHaveURL(new RegExp(`/projects/${encodeURIComponent(projectName)}$`));

    await page.close();
  });

});
