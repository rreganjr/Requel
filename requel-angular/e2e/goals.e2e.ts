import { test, expect } from './fixtures/auth';
import { createProject, deleteProject, createGoal, deleteGoal, GoalFixture } from './fixtures/api-helper';
import { GoalListPage, GoalEditorPage } from './pages/GoalEditorPage';

// All goal tests share one project to avoid repeated project creation overhead
const PROJECT_NAME = `e2e-goals-${Date.now()}`;
let goalToCleanup: GoalFixture | null = null;

test.beforeAll(async ({ request }) => {
  await createProject(request, PROJECT_NAME, 'Goals E2E test project');
});

test.afterAll(async ({ request }) => {
  await deleteProject(request, PROJECT_NAME);
});

test.afterEach(async ({ request }) => {
  if (goalToCleanup) {
    try {
      await deleteGoal(request, goalToCleanup);
    } catch {
      // may already be deleted by the test
    }
    goalToCleanup = null;
  }
});

test.describe('Goal management', () => {

  test('create goal → appears in goal list', async ({ adminContext, request }) => {
    const goalName = `e2e-goal-create-${Date.now()}`;
    const page = await adminContext.newPage();
    const listPage = new GoalListPage(page);
    const editorPage = new GoalEditorPage(page);

    await listPage.goto(PROJECT_NAME);
    await listPage.clickNewGoal();

    await editorPage.fillName(goalName);
    await editorPage.fillDescription('Goal created by E2E test');
    await editorPage.save();

    // After save the URL changes to /goals/<id>
    await page.waitForURL(/\/goals\/\d+/);

    // Track id from URL for cleanup
    const url = page.url();
    const idMatch = url.match(/\/goals\/(\d+)/);
    if (idMatch) {
      goalToCleanup = { id: parseInt(idMatch[1], 10), version: 0, name: goalName, projectName: PROJECT_NAME };
    }

    await listPage.goto(PROJECT_NAME);
    await listPage.expectGoalInTable(goalName);

    await page.close();
  });

  test('rename goal → new name persists after save and page reload', async ({ adminContext, request }) => {
    const originalName = `e2e-goal-rename-orig-${Date.now()}`;
    const newName = `e2e-goal-rename-new-${Date.now()}`;
    const goal = await createGoal(request, PROJECT_NAME, originalName, 'Rename test goal');
    goalToCleanup = { ...goal, name: newName }; // track new name for cleanup

    const page = await adminContext.newPage();
    const listPage = new GoalListPage(page);
    const editorPage = new GoalEditorPage(page);

    await listPage.goto(PROJECT_NAME);
    await listPage.clickGoal(originalName);

    await editorPage.fillName(newName);
    await editorPage.save();

    // Reload and confirm name persisted
    await page.reload();
    await page.waitForLoadState('domcontentloaded');
    await editorPage.expectNameValue(newName);

    await page.close();
  });

  test('delete goal → removed from list', async ({ adminContext, request }) => {
    const goalName = `e2e-goal-delete-${Date.now()}`;
    await createGoal(request, PROJECT_NAME, goalName, 'Delete test goal');
    goalToCleanup = null; // test deletes it

    const page = await adminContext.newPage();
    const listPage = new GoalListPage(page);
    const editorPage = new GoalEditorPage(page);

    await listPage.goto(PROJECT_NAME);
    await listPage.clickGoal(goalName);

    await editorPage.delete();

    // Should navigate back to goal list
    await page.waitForURL(/\/goals$/);
    await listPage.expectGoalNotInTable(goalName);

    await page.close();
  });

  test('back button navigates to goal list', async ({ adminContext, request }) => {
    const goalName = `e2e-goal-back-${Date.now()}`;
    const goal = await createGoal(request, PROJECT_NAME, goalName, 'Back button test');
    goalToCleanup = goal;

    const page = await adminContext.newPage();
    const listPage = new GoalListPage(page);
    const editorPage = new GoalEditorPage(page);

    await listPage.goto(PROJECT_NAME);
    await listPage.clickGoal(goalName);
    await editorPage.navigateBack(PROJECT_NAME);

    await expect(page).toHaveURL(new RegExp(`/projects/${encodeURIComponent(PROJECT_NAME)}/goals$`));

    await page.close();
  });

});
