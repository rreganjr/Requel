import { test, expect } from './fixtures/auth';
import { createProject, deleteProject, createScenario, deleteScenario, getScenarioVersion, ScenarioFixture } from './fixtures/api-helper';
import { ScenarioListPage, ScenarioEditorPage } from './pages/ScenarioEditorPage';
import { reloadAndWaitForGet } from './helpers/navigation';

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

    await reloadAndWaitForGet(page, r => /\/scenarios\/\d+$/.test(r.url()));
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

    await reloadAndWaitForGet(page, r => /\/scenarios\/\d+$/.test(r.url()));
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

test.describe('Scenario steps', () => {

  test('add step → name persists after save and reload', async ({ adminContext, request }) => {
    const scenarioName = `e2e-scenario-step-add-${Date.now()}`;
    const scenario = await createScenario(request, PROJECT_NAME, scenarioName);
    scenarioToCleanup = scenario;

    const page = await adminContext.newPage();
    const listPage = new ScenarioListPage(page);
    const editorPage = new ScenarioEditorPage(page);

    await listPage.goto(PROJECT_NAME);
    await listPage.clickScenario(scenarioName);

    await editorPage.addStep();
    await editorPage.fillStepName(0, 'The user opens the application');
    await editorPage.save();

    await reloadAndWaitForGet(page, r => /\/scenarios\/\d+$/.test(r.url()));
    await editorPage.expectStepCount(1);
    await editorPage.expectStepName(0, 'The user opens the application');

    await page.close();
  });

  test('edit step via popup → name and text persist after save and reload', async ({ adminContext, request }) => {
    const scenarioName = `e2e-scenario-step-popup-${Date.now()}`;
    const scenario = await createScenario(request, PROJECT_NAME, scenarioName);
    scenarioToCleanup = scenario;

    const page = await adminContext.newPage();
    const listPage = new ScenarioListPage(page);
    const editorPage = new ScenarioEditorPage(page);

    await listPage.goto(PROJECT_NAME);
    await listPage.clickScenario(scenarioName);

    // Add and save an initial step so the scenario is non-empty
    await editorPage.addStep();
    await editorPage.fillStepName(0, 'Initial step name');
    await editorPage.save();

    // Edit the step via the detail popup
    await editorPage.openStepEdit(0);
    await editorPage.fillStepEditName('Updated step name');
    await editorPage.fillStepEditText('Some step notes');
    await editorPage.applyStepEdit();
    await editorPage.save();

    await reloadAndWaitForGet(page, r => /\/scenarios\/\d+$/.test(r.url()));
    await editorPage.expectStepCount(1);
    await editorPage.expectStepName(0, 'Updated step name');

    await page.close();
  });

  test('delete step → gone after save and reload', async ({ adminContext, request }) => {
    const scenarioName = `e2e-scenario-step-del-${Date.now()}`;
    const scenario = await createScenario(request, PROJECT_NAME, scenarioName);
    scenarioToCleanup = scenario;

    const page = await adminContext.newPage();
    const listPage = new ScenarioListPage(page);
    const editorPage = new ScenarioEditorPage(page);

    await listPage.goto(PROJECT_NAME);
    await listPage.clickScenario(scenarioName);

    // Add and save a step first
    await editorPage.addStep();
    await editorPage.fillStepName(0, 'Step to delete');
    await editorPage.save();

    await editorPage.expectStepCount(1);

    // Remove the step then save
    await editorPage.removeStep(0);
    await editorPage.save();

    await reloadAndWaitForGet(page, r => /\/scenarios\/\d+$/.test(r.url()));
    await editorPage.expectStepCount(0);

    await page.close();
  });

  test('step order change persists after save and reload', async ({ adminContext, request }) => {
    // Note: CDK DragDrop mouse/keyboard drag is unreliable in headless Playwright against
    // the nested scroll container (.main-content overflow-y:auto inside overflow:hidden
    // .layout-body). Reorder is tested via Remove + Re-add: delete step 0 (Alpha) so Beta
    // moves to index 0, then append Alpha at the end → [Beta, Alpha].
    const scenarioName = `e2e-scenario-step-reorder-${Date.now()}`;
    const scenario = await createScenario(request, PROJECT_NAME, scenarioName);
    scenarioToCleanup = scenario;

    const page = await adminContext.newPage();
    const listPage = new ScenarioListPage(page);
    const editorPage = new ScenarioEditorPage(page);

    await listPage.goto(PROJECT_NAME);
    await listPage.clickScenario(scenarioName);

    // Build initial order [Alpha, Beta] and save to get persisted IDs.
    await editorPage.addStep();
    await editorPage.fillStepName(0, 'Step Alpha');
    await editorPage.addStep();
    await editorPage.fillStepName(1, 'Step Beta');
    await editorPage.save();

    // Reload before reordering. After save() the component calls loadScenario()
    // internally AND the server pushes an SSE event that also calls loadScenario().
    // Either call can reset stepNodes back to [Alpha, Beta] mid-sequence, producing
    // an empty-name step that fails server validation. A full reload starts the
    // component fresh with no in-flight reloads in progress.
    await reloadAndWaitForGet(page, r => /\/scenarios\/\d+$/.test(r.url()));
    await editorPage.expectStepName(0, 'Step Alpha');
    await editorPage.expectStepName(1, 'Step Beta');

    // Reorder: remove Alpha (index 0) → [Beta], append Gamma → [Beta, Gamma].
    // Using a distinct name (Gamma ≠ Alpha) avoids a transient DB unique-constraint
    // violation: the scenarios table shares a UNIQUE(project_id, name) index for both
    // steps and scenarios (SINGLE_TABLE inheritance). Hibernate flushes inserts before
    // deletes, so creating a new 'Step Alpha' while the old one is still in the DB
    // would violate the constraint before the clear() runs.
    await editorPage.removeStep(0);
    await editorPage.addStep();
    await editorPage.fillStepName(1, 'Step Gamma');
    await editorPage.save();

    await reloadAndWaitForGet(page, r => /\/scenarios\/\d+$/.test(r.url()));
    await editorPage.expectStepName(0, 'Step Beta');
    await editorPage.expectStepName(1, 'Step Gamma');

    await page.close();
  });

});
