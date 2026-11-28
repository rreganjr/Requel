import { test, expect } from './fixtures/auth';
import { createProject, deleteProject, createGoal, deleteGoal } from './fixtures/api-helper';

// #525: an editor opened on an id that doesn't exist (deleted, or a stale link) shows a
// not-found page with a way back to the list, not a load error with a Retry that can't help,
// and offers nothing to act on: no Copy, Delete, Annotations or Sources.

const PROJECT_NAME = `e2e-not-found-${Date.now()}`;
const enc = encodeURIComponent(PROJECT_NAME);

test.beforeAll(async ({ request }) => {
  await createProject(request, PROJECT_NAME, 'Editor not-found #525 e2e project');
});

test.afterAll(async ({ request }) => {
  await deleteProject(request, PROJECT_NAME);
});

test.describe('Editor not-found state (#525)', () => {

  test('a deleted goal shows not-found, and its link goes back to the goals list', async ({ adminContext, request }) => {
    const goal = await createGoal(request, PROJECT_NAME, `e2e-gone-${Date.now()}`, 'About to be deleted.');
    await deleteGoal(request, goal);

    const page = await adminContext.newPage();
    await page.goto(`/projects/${enc}/goals/${goal.id}`);

    await expect(page.getByTestId('goal-editor-not-found')).toBeVisible();
    await expect(page.getByRole('heading', { level: 1 })).toHaveText('Goal not found');
    await expect(page.getByTestId('goal-editor-load-error')).toHaveCount(0);
    await expect(page.getByTestId('goal-back')).toHaveCount(0);
    await expect(page.getByTestId('goal-copy')).toHaveCount(0);
    await expect(page.getByTestId('goal-delete')).toHaveCount(0);
    await expect(page.locator('app-annotations-section')).toHaveCount(0);
    await expect(page.locator('app-sources-section')).toHaveCount(0);

    await page.getByTestId('goal-editor-not-found-list').click();
    await expect(page).toHaveURL(new RegExp(`/projects/${enc}/goals$`));

    await page.close();
  });

  test('a term id that never existed shows not-found too', async ({ adminContext }) => {
    const page = await adminContext.newPage();
    await page.goto(`/projects/${enc}/terms/999999999`);

    await expect(page.getByTestId('term-editor-not-found')).toBeVisible();
    await expect(page.getByRole('heading', { level: 1 })).toHaveText('Glossary term not found');
    await expect(page.getByTestId('term-delete')).toHaveCount(0);

    await page.close();
  });
});
