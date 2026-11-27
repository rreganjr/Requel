import { test, expect } from './fixtures/auth';
import { createProject, deleteProject, createGoal } from './fixtures/api-helper';

// #497: the overview's Traceability button opens the page; a goal nothing realizes is marked in
// the matrix and listed under its gap, and the gap's link opens the goal.

const PROJECT_NAME = `e2e-trace-${Date.now()}`;
const enc = encodeURIComponent(PROJECT_NAME);

test.beforeAll(async ({ request }) => {
  await createProject(request, PROJECT_NAME, 'Traceability #497 e2e project');
});

test.afterAll(async ({ request }) => {
  await deleteProject(request, PROJECT_NAME);
});

test.describe('Traceability (#497)', () => {

  test('overview button, unrealized goal and gap link', async ({ adminContext, request }) => {
    const goal = await createGoal(request, PROJECT_NAME, `e2e-lonely-goal-${Date.now()}`, 'Nothing realizes this.');

    const page = await adminContext.newPage();
    await page.goto(`/projects/${enc}`);
    await page.getByTestId('workspace-traceability').click();
    await page.waitForURL(new RegExp(`/projects/${enc}/traceability$`));

    const row = page.getByTestId('trace-goal-row').filter({ hasText: goal.name });
    await expect(row).toHaveCount(1);
    await expect(row.getByTestId('trace-not-realized')).toBeVisible();

    const gap = page.getByTestId('trace-gap-GOAL_NOT_REALIZED');
    await expect(gap).toContainText(goal.name);
    await gap.getByRole('link', { name: goal.name }).click();
    await page.waitForURL(new RegExp(`/projects/${enc}/goals/${goal.id}$`));

    await page.close();
  });
});
