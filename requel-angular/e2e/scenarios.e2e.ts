import { test, expect } from './fixtures/auth';
import { createProject, deleteProject, createScenario, deleteScenario, getScenarioVersion, ScenarioFixture } from './fixtures/api-helper';
import { ScenarioListPage, ScenarioEditorPage } from './pages/ScenarioEditorPage';

const PROJECT_NAME = `e2e-scenarios-${Date.now()}`;
let scenarioToCleanup: ScenarioFixture | null = null;

test.beforeAll(async ({ request }) => {
  await createProject(request, PROJECT_NAME, 'Scenarios E2E test project');
});

test.afterAll(async ({ request }) => {
  await deleteProject(request, PROJECT_NAME);
});

test.afterEach(async ({ request }) => {
  if (scenarioToCleanup) {
    try {
      const version = await getScenarioVersion(request, scenarioToCleanup);
      await deleteScenario(request, { ...scenarioToCleanup, version });
    } catch {
      // may already be deleted by the test
    }
    scenarioToCleanup = null;
  }
});

test.describe('Scenario management', () => {

  test('create scenario → appears in scenario list', async ({ adminContext }) => {
    const scenarioName = `e2e-scenario-create-${Date.now()}`;
    const page = await adminContext.newPage();
    const listPage = new ScenarioListPage(page);
    const editorPage = new ScenarioEditorPage(page);

    await listPage.goto(PROJECT_NAME);
    await listPage.clickNewScenario();

    await editorPage.fillName(scenarioName);
    await editorPage.selectType('Primary');
    await editorPage.save();

    await page.waitForURL(/\/scenarios\/\d+/);

    const url = page.url();
    const idMatch = url.match(/\/scenarios\/(\d+)/);
    if (idMatch) {
      scenarioToCleanup = { id: parseInt(idMatch[1], 10), version: 0, name: scenarioName, projectName: PROJECT_NAME };
    }

    await listPage.goto(PROJECT_NAME);
    await listPage.expectScenarioInTable(scenarioName);

    await page.close();
  });

  test('rename scenario → name persists after save and reload', async ({ adminContext, request }) => {
    const originalName = `e2e-scenario-rename-${Date.now()}`;
    const newName = `${originalName}-renamed`;
    const scenario = await createScenario(request, PROJECT_NAME, originalName);
    scenarioToCleanup = { ...scenario, name: newName };

    const page = await adminContext.newPage();
    const listPage = new ScenarioListPage(page);
    const editorPage = new ScenarioEditorPage(page);

    await listPage.goto(PROJECT_NAME);
    await listPage.clickScenario(originalName);

    await editorPage.fillName(newName);
    await editorPage.save();

    await page.reload();
    await page.waitForLoadState('domcontentloaded');
    await editorPage.expectNameValue(newName);

    await page.close();
  });

  test('change scenario type → persists after save and reload', async ({ adminContext, request }) => {
    const scenarioName = `e2e-scenario-type-${Date.now()}`;
    const scenario = await createScenario(request, PROJECT_NAME, scenarioName, 'Primary');
    scenarioToCleanup = scenario;

    const page = await adminContext.newPage();
    const listPage = new ScenarioListPage(page);
    const editorPage = new ScenarioEditorPage(page);

    await listPage.goto(PROJECT_NAME);
    await listPage.clickScenario(scenarioName);

    await editorPage.selectType('Alternative');
    await editorPage.save();

    await page.reload();
    await page.waitForLoadState('domcontentloaded');
    await editorPage.expectTypeValue('Alternative');

    await page.close();
  });

  test('delete scenario → removed from list', async ({ adminContext, request }) => {
    const scenarioName = `e2e-scenario-delete-${Date.now()}`;
    await createScenario(request, PROJECT_NAME, scenarioName);
    scenarioToCleanup = null;

    const page = await adminContext.newPage();
    const listPage = new ScenarioListPage(page);
    const editorPage = new ScenarioEditorPage(page);

    await listPage.goto(PROJECT_NAME);
    await listPage.clickScenario(scenarioName);

    await editorPage.delete();

    await page.waitForURL(/\/scenarios$/);
    await listPage.goto(PROJECT_NAME);
    await listPage.expectScenarioNotInTable(scenarioName);

    await page.close();
  });

});
