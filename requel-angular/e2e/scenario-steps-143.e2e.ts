import { test, expect } from './fixtures/auth';
import { Page } from '@playwright/test';
import { createProject, deleteProject, createScenario, deleteScenario, getScenarioVersion, ScenarioFixture } from './fixtures/api-helper';
import { ScenarioListPage, ScenarioEditorPage } from './pages/ScenarioEditorPage';
import { reloadAndWaitForGet } from './helpers/navigation';

// Coverage for #143 (scenario step-state → FormArray) that unit tests only approximate:
// real CDK drag-reorder, and the native-confirm unsaved-changes guard (dirtyCheckGuard).
// These must stay green across the FormArray refactor — behavior is unchanged.

const PROJECT_NAME = `e2e-scenario-143-${Date.now()}`;
let scenarioToCleanup: ScenarioFixture | null = null;

test.beforeAll(async ({ request }) => {
  await createProject(request, PROJECT_NAME, 'Scenario #143 e2e project');
});

test.afterAll(async ({ request }) => {
  await deleteProject(request, PROJECT_NAME);
});

test.afterEach(async ({ request }) => {
  if (scenarioToCleanup) {
    try {
      const version = await getScenarioVersion(request, scenarioToCleanup);
      await deleteScenario(request, { ...scenarioToCleanup, version });
    } catch { /* may already be deleted */ }
    scenarioToCleanup = null;
  }
});

/**
 * Drag the step at `fromIndex` onto the row at `toIndex` via the drag handle.
 * CDK needs a small threshold move to start the drag and several intermediate
 * moves to track it — a single mouse.move will not register a reorder.
 * (Promote to ScenarioEditorPage if a second spec needs it.)
 */
async function reorderStep(page: Page, fromIndex: number, toIndex: number): Promise<void> {
  const rows = page.getByTestId('scenario-step-row');
  const handle = rows.nth(fromIndex).getByTestId('scenario-step-drag-handle');
  const hb = await handle.boundingBox();
  const tb = await rows.nth(toIndex).boundingBox();
  if (!hb || !tb) throw new Error('drag handle / target row not visible');
  await page.mouse.move(hb.x + hb.width / 2, hb.y + hb.height / 2);
  await page.mouse.down();
  await page.mouse.move(hb.x + hb.width / 2, hb.y + hb.height / 2 + 8); // pass drag threshold
  const destY = toIndex > fromIndex ? tb.y + tb.height - 4 : tb.y + 4;
  await page.mouse.move(tb.x + tb.width / 2, destY, { steps: 12 });
  await page.mouse.move(tb.x + tb.width / 2, destY); // settle over target
  await page.mouse.up();
}

test.describe('Scenario steps — reorder & unsaved guard (#143)', () => {

  test('reorder steps via drag → order persists after save and reload', async ({ adminContext, request }) => {
    const name = `e2e-scenario-reorder-${Date.now()}`;
    const scenario = await createScenario(request, PROJECT_NAME, name);
    scenarioToCleanup = { ...scenario, name };

    const page = await adminContext.newPage();
    const listPage = new ScenarioListPage(page);
    const editor = new ScenarioEditorPage(page);

    await listPage.goto(PROJECT_NAME);
    await listPage.clickScenario(name);

    await editor.addStep();
    await editor.fillStepName(0, 'Step A');
    await editor.addStep();
    await editor.fillStepName(1, 'Step B');
    await editor.save();

    await reorderStep(page, 1, 0); // drag Step B above Step A
    await expect(page.getByTestId('scenario-step-row').nth(0).getByTestId('scenario-step-name'))
      .toHaveValue('Step B');
    await editor.save();

    await reloadAndWaitForGet(page, r => /\/scenarios\/\d+$/.test(r.url()));
    await editor.expectStepName(0, 'Step B');
    await editor.expectStepName(1, 'Step A');

    await page.close();
  });

  test('unsaved inline step edit → Back prompts; cancelling keeps the edit', async ({ adminContext, request }) => {
    const name = `e2e-scenario-guard-${Date.now()}`;
    const scenario = await createScenario(request, PROJECT_NAME, name);
    scenarioToCleanup = { ...scenario, name };

    const page = await adminContext.newPage();
    const listPage = new ScenarioListPage(page);
    const editor = new ScenarioEditorPage(page);

    await listPage.goto(PROJECT_NAME);
    await listPage.clickScenario(name);

    await editor.addStep();
    await editor.fillStepName(0, 'Original');
    await editor.save();

    // Make an unsaved change (fillStepName tabs out → (blur) → stepsSaveNeeded = true).
    await editor.fillStepName(0, 'Edited but not saved');

    // dirtyCheckGuard uses a native confirm(); cancelling it must keep us on the editor.
    let dialogMessage = '';
    page.once('dialog', async d => { dialogMessage = d.message(); await d.dismiss(); });
    await page.getByTestId('scenario-back').click();
    await page.waitForTimeout(250);

    expect(dialogMessage).toContain('unsaved changes');
    await expect(page).toHaveURL(/\/scenarios\/\d+/);
    await editor.expectStepName(0, 'Edited but not saved');

    await page.close();
  });
});
