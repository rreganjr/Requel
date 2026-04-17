import { test, expect } from './fixtures/auth';
import { createProject, deleteProject, createActor, deleteActor, getActorVersion, ActorFixture } from './fixtures/api-helper';
import { ActorListPage, ActorEditorPage } from './pages/ActorEditorPage';

const PROJECT_NAME = `e2e-actors-${Date.now()}`;
let actorToCleanup: ActorFixture | null = null;

test.beforeAll(async ({ request }) => {
  await createProject(request, PROJECT_NAME, 'Actors E2E test project');
});

test.afterAll(async ({ request }) => {
  await deleteProject(request, PROJECT_NAME);
});

test.afterEach(async ({ request }) => {
  if (actorToCleanup) {
    try {
      // Re-fetch current version — saves increment it, making stored version stale.
      const version = await getActorVersion(request, actorToCleanup);
      await deleteActor(request, { ...actorToCleanup, version });
    } catch {
      // may already be deleted by the test
    }
    actorToCleanup = null;
  }
});

test.describe('Actor management', () => {

  test('create actor → appears in actor list', async ({ adminContext }) => {
    const actorName = `e2e-actor-create-${Date.now()}`;
    const page = await adminContext.newPage();
    const listPage = new ActorListPage(page);
    const editorPage = new ActorEditorPage(page);

    await listPage.goto(PROJECT_NAME);
    await listPage.clickNewActor();

    await editorPage.fillName(actorName);
    await editorPage.fillDescription('Actor created by E2E test');
    await editorPage.save(); // "Create" button for new actor

    await page.waitForURL(/\/actors\/\d+/);

    const url = page.url();
    const idMatch = url.match(/\/actors\/(\d+)/);
    if (idMatch) {
      actorToCleanup = { id: parseInt(idMatch[1], 10), version: 0, name: actorName, projectName: PROJECT_NAME };
    }

    await listPage.goto(PROJECT_NAME);
    await listPage.expectActorInTable(actorName);

    await page.close();
  });

  test('rename actor → name persists after save and reload', async ({ adminContext, request }) => {
    const originalName = `e2e-actor-rename-${Date.now()}`;
    const newName = `${originalName}-renamed`;
    const actor = await createActor(request, PROJECT_NAME, originalName);
    actorToCleanup = { ...actor, name: newName };

    const page = await adminContext.newPage();
    const listPage = new ActorListPage(page);
    const editorPage = new ActorEditorPage(page);

    await listPage.goto(PROJECT_NAME);
    await listPage.clickActor(originalName);

    await editorPage.fillName(newName);
    await editorPage.save();

    await page.reload();
    await page.waitForLoadState('domcontentloaded');
    await editorPage.expectNameValue(newName);

    await page.close();
  });

  test('copy actor → a second entry appears in actor list', async ({ adminContext, request }) => {
    const actorName = `e2e-actor-copy-${Date.now()}`;
    const actor = await createActor(request, PROJECT_NAME, actorName);
    actorToCleanup = actor;

    const page = await adminContext.newPage();
    const listPage = new ActorListPage(page);
    const editorPage = new ActorEditorPage(page);

    await listPage.goto(PROJECT_NAME);
    await listPage.clickActor(actorName);

    await editorPage.copy();

    // Copy navigates to the copied actor's editor (any actor URL)
    await page.waitForURL(/\/actors\/\d+/);

    // Navigate to list and wait for 2 data rows — toHaveCount auto-waits for Angular
    // This project is fresh (beforeAll creates it empty) so only this test's actors exist.
    // Empty-state message is a <tr> with colspan; data rows each have <td> without colspan.
    await listPage.goto(PROJECT_NAME);
    // Search by actorName so both "e2e-actor-copy-..." and "Copy of e2e-actor-copy-..." appear,
    // filtering out any leftover actors from other tests in this project.
    await listPage.searchFor(actorName);
    await expect(page.locator('p-table td:not([colspan])')).toHaveCount(6, { timeout: 10000 });
    // 2 actors × 3 columns (name, description, createdBy) = 6 non-colspan tds

    // Cleanup the copy by deleting it via API (id is in current URL)
    const copyUrl = page.url();
    const copyIdMatch = copyUrl.match(/\/actors\/(\d+)/);
    if (copyIdMatch && parseInt(copyIdMatch[1], 10) !== actor.id) {
      await deleteActor(request, {
        id: parseInt(copyIdMatch[1], 10),
        version: 0,
        name: `Copy of ${actorName}`,
        projectName: PROJECT_NAME,
      });
    }

    await page.close();
  });

  test('delete actor → removed from list', async ({ adminContext, request }) => {
    const actorName = `e2e-actor-delete-${Date.now()}`;
    await createActor(request, PROJECT_NAME, actorName);
    actorToCleanup = null;

    const page = await adminContext.newPage();
    const listPage = new ActorListPage(page);
    const editorPage = new ActorEditorPage(page);

    await listPage.goto(PROJECT_NAME);
    await listPage.clickActor(actorName);

    await editorPage.delete();

    await page.waitForURL(/\/actors$/);
    await listPage.expectActorNotInTable(actorName);

    await page.close();
  });

});
