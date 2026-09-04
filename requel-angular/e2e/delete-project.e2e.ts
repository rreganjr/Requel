import { test, expect } from './fixtures/auth';
import { createProject, deleteProject, listProjectNames } from './fixtures/api-helper';
import { ProjectsPage } from './pages/ProjectsPage';

/**
 * #241 — Delete Project UI with export-first backup.
 *
 * Drives the guarded delete flow end to end from both placements (workspace
 * header and list row): take the backup export, confirm, and verify the project
 * is gone. The admin persona is a full stakeholder on the projects it creates
 * (creator holds Project[Delete]), so the action is visible; the hidden-when-
 * unauthorized gate is covered by the component unit tests, since the e2e
 * fixtures cannot yet seed a stakeholder without Delete.
 */
test.describe('Delete project', () => {

  test('deletes a project from the workspace header, backup first', async ({ adminContext, request }) => {
    const name = `e2e-delete-ws-${Date.now()}`;
    await createProject(request, name, 'workspace delete target');

    const page = await adminContext.newPage();
    const projectsPage = new ProjectsPage(page);

    await projectsPage.goto();
    await projectsPage.openWorkspaceDeleteDialog(name);
    await projectsPage.confirmDelete();

    // Workspace routes back to the list once the project is gone.
    await page.waitForURL('**/projects');
    await projectsPage.expectProjectNotInTable(name);

    expect(await listProjectNames(request)).not.toContain(name);
    await page.close();
  });

  test('deletes a project from the list row, backup first', async ({ adminContext, request }) => {
    const name = `e2e-delete-row-${Date.now()}`;
    await createProject(request, name, 'list-row delete target');

    const page = await adminContext.newPage();
    const projectsPage = new ProjectsPage(page);

    await projectsPage.goto();
    await projectsPage.openRowDeleteDialog(name);
    await projectsPage.confirmDelete();

    // The list refreshes in place and shows a success message.
    await expect(page.getByTestId('project-list-success')).toContainText('Project deleted.');
    await projectsPage.expectProjectNotInTable(name);

    expect(await listProjectNames(request)).not.toContain(name);
    await page.close();
  });

  test('cancel aborts the delete and leaves the project intact', async ({ adminContext, request }) => {
    const name = `e2e-delete-cancel-${Date.now()}`;
    await createProject(request, name, 'cancel target');

    const page = await adminContext.newPage();
    const projectsPage = new ProjectsPage(page);

    await projectsPage.goto();
    await projectsPage.openRowDeleteDialog(name);
    await projectsPage.cancelDeleteDialog();

    await projectsPage.expectProjectInTable(name);
    expect(await listProjectNames(request)).toContain(name);

    // Clean up the fixture project.
    await deleteProject(request, name);
    await page.close();
  });
});
