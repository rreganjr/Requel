import { test, expect } from './fixtures/auth';
import {
  createProject, deleteProject,
  createActor,
  createUseCase, deleteUseCase, getUseCaseVersion, UseCaseFixture
} from './fixtures/api-helper';
import { UseCaseListPage, UseCaseEditorPage } from './pages/UseCaseEditorPage';

const PROJECT_NAME = `e2e-use-cases-${Date.now()}`;
// Shared actor for all tests — use cases require a primary actor (primary_actor_id NOT NULL)
const ACTOR_NAME = 'E2E Actor';
let ucToCleanup: UseCaseFixture | null = null;

test.beforeAll(async ({ request }) => {
  await createProject(request, PROJECT_NAME, 'Use Cases E2E test project');
  await createActor(request, PROJECT_NAME, ACTOR_NAME);
});

test.afterAll(async ({ request }) => {
  await deleteProject(request, PROJECT_NAME);
});

test.afterEach(async ({ request }) => {
  if (ucToCleanup) {
    try {
      const version = await getUseCaseVersion(request, ucToCleanup);
      await deleteUseCase(request, { ...ucToCleanup, version });
    } catch {
      // may already be deleted by the test
    }
    ucToCleanup = null;
  }
});

test.describe('Use Case management', () => {

  test('create use case → appears in use case list', async ({ adminContext }) => {
    const ucName = `e2e-uc-create-${Date.now()}`;
    const page = await adminContext.newPage();
    const listPage = new UseCaseListPage(page);
    const editorPage = new UseCaseEditorPage(page);

    await listPage.goto(PROJECT_NAME);
    await listPage.clickNewUseCase();

    await editorPage.fillName(ucName);
    // Primary actor is required; select the shared actor created in beforeAll
    await editorPage.selectPrimaryActor(ACTOR_NAME);
    await editorPage.fillDescription('Use case created by E2E test');
    await editorPage.save();

    await page.waitForURL(/\/use-cases\/\d+/);

    const url = page.url();
    const idMatch = url.match(/\/use-cases\/(\d+)/);
    if (idMatch) {
      ucToCleanup = { id: parseInt(idMatch[1], 10), version: 0, name: ucName, projectName: PROJECT_NAME };
    }

    await listPage.goto(PROJECT_NAME);
    await listPage.expectUseCaseInTable(ucName);

    await page.close();
  });

  test('rename use case → name persists after save and reload', async ({ adminContext, request }) => {
    const originalName = `e2e-uc-rename-${Date.now()}`;
    const newName = `${originalName}-renamed`;
    const uc = await createUseCase(request, PROJECT_NAME, originalName);
    ucToCleanup = { ...uc, name: newName };

    const page = await adminContext.newPage();
    const listPage = new UseCaseListPage(page);
    const editorPage = new UseCaseEditorPage(page);

    await listPage.goto(PROJECT_NAME);
    await listPage.clickUseCase(originalName);

    await editorPage.fillName(newName);
    await editorPage.save();

    await page.reload();
    await page.waitForLoadState('domcontentloaded');
    await editorPage.expectNameValue(newName);

    await page.close();
  });

  test('delete use case → removed from list', async ({ adminContext, request }) => {
    const ucName = `e2e-uc-delete-${Date.now()}`;
    await createUseCase(request, PROJECT_NAME, ucName);
    ucToCleanup = null;

    const page = await adminContext.newPage();
    const listPage = new UseCaseListPage(page);
    const editorPage = new UseCaseEditorPage(page);

    await listPage.goto(PROJECT_NAME);
    await listPage.clickUseCase(ucName);

    await editorPage.delete();

    await page.waitForURL(/\/use-cases$/);
    await listPage.goto(PROJECT_NAME);
    await listPage.expectUseCaseNotInTable(ucName);

    await page.close();
  });

  test('copy use case → a second entry appears in use case list', async ({ adminContext, request }) => {
    const ucName = `e2e-uc-copy-${Date.now()}`;
    const uc = await createUseCase(request, PROJECT_NAME, ucName);
    ucToCleanup = uc;

    const page = await adminContext.newPage();
    const listPage = new UseCaseListPage(page);
    const editorPage = new UseCaseEditorPage(page);

    await listPage.goto(PROJECT_NAME);
    await listPage.clickUseCase(ucName);

    await editorPage.copy();

    await page.waitForURL(/\/use-cases\/\d+/);

    // Both original and "Copy of ..." contain ucName — 2 rows visible
    await listPage.goto(PROJECT_NAME);
    await expect(page.locator('p-table td', { hasText: ucName })).toHaveCount(2, { timeout: 10000 });

    // Cleanup the copy
    const copyUrl = page.url();
    const copyIdMatch = copyUrl.match(/\/use-cases\/(\d+)/);
    if (copyIdMatch && parseInt(copyIdMatch[1], 10) !== uc.id) {
      await deleteUseCase(request, {
        id: parseInt(copyIdMatch[1], 10),
        version: 0,
        name: `Copy of ${ucName}`,
        projectName: PROJECT_NAME,
      });
    }

    await page.close();
  });

});
