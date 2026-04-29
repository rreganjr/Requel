import { test, expect } from './fixtures/auth';
import { createProject, createGoal, deleteGoal, GoalFixture } from './fixtures/api-helper';
import { GoalListPage, GoalEditorPage } from './pages/GoalEditorPage';

// Projects accumulate (no DeleteProject command); use a unique name per run
const PROJECT_NAME = `e2e-dirty-guard-${Date.now()}`;
let goalToCleanup: GoalFixture | null = null;

test.beforeAll(async ({ request }) => {
  await createProject(request, PROJECT_NAME, 'Dirty guard test project');
});

test.afterEach(async ({ request }) => {
  if (goalToCleanup) {
    try { await deleteGoal(request, goalToCleanup); } catch { /* ignore */ }
    goalToCleanup = null;
  }
});

test.describe('Dirty guard', () => {

  test('unsaved changes → cancel dialog → stays on editor', async ({ adminContext, request }) => {
    const goalName = `e2e-dirty-cancel-${Date.now()}`;
    const goal = await createGoal(request, PROJECT_NAME, goalName);
    goalToCleanup = goal;

    const page = await adminContext.newPage();
    const listPage = new GoalListPage(page);
    const editorPage = new GoalEditorPage(page);

    await listPage.goto(PROJECT_NAME);
    await listPage.clickGoal(goalName);

    // Dirty the form without saving
    await editorPage.fillName('Unsaved Name Change');

    // Register dismiss handler BEFORE the click that triggers confirm()
    page.once('dialog', dialog => dialog.dismiss());
    await page.getByRole('button', { name: 'Back' }).click();

    // Guard returned false — navigation cancelled; still on goal editor
    await expect(page).toHaveURL(/\/goals\/\d+$/);

    await page.close();
  });

  test('unsaved changes → confirm dialog → navigates away', async ({ adminContext, request }) => {
    const goalName = `e2e-dirty-confirm-${Date.now()}`;
    const goal = await createGoal(request, PROJECT_NAME, goalName);
    goalToCleanup = goal;

    const page = await adminContext.newPage();
    const listPage = new GoalListPage(page);
    const editorPage = new GoalEditorPage(page);

    await listPage.goto(PROJECT_NAME);
    await listPage.clickGoal(goalName);

    // Dirty the form without saving
    await editorPage.fillName('Unsaved Name Change');

    // Register accept handler BEFORE the click that triggers confirm()
    page.once('dialog', dialog => dialog.accept());
    await page.getByRole('button', { name: 'Back' }).click();

    // Guard returned true — navigation allowed; back on goal list
    await page.waitForURL(`**/projects/${encodeURIComponent(PROJECT_NAME)}/goals`);

    await page.close();
  });

});
