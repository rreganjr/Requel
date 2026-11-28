import { test, expect } from './fixtures/auth';
import { createProject, deleteProject, createGoal, addIssue } from './fixtures/api-helper';

// #507: tick two must-resolve issues on Open Issues, Mark advisory, confirm; the summary says
// both were done and the list shows them as no longer required.

const PROJECT_NAME = `e2e-bulk-${Date.now()}`;
const enc = encodeURIComponent(PROJECT_NAME);

test.beforeAll(async ({ request }) => {
  await createProject(request, PROJECT_NAME, 'Bulk issue actions #507 e2e project');
});

test.afterAll(async ({ request }) => {
  await deleteProject(request, PROJECT_NAME);
});

test.describe('Bulk actions on Open Issues (#507)', () => {

  test('select two issues, mark them advisory, see the summary', async ({ adminContext, request }) => {
    const ts = Date.now();
    const goal = await createGoal(request, PROJECT_NAME, `e2e-bulk-goal-${ts}`, 'A goal with questions.');
    await addIssue(request, PROJECT_NAME, 'Goal', goal.id, `Measured how ${ts}?`, true, 'MEDIUM');
    await addIssue(request, PROJECT_NAME, 'Goal', goal.id, `Over what period ${ts}?`, true, 'MEDIUM');

    const page = await adminContext.newPage();
    await page.goto(`/projects/${enc}/open-issues`);
    await expect(page.getByTestId('open-issue-required')).toHaveCount(2);
    await expect(page.getByTestId('bulk-toolbar')).toHaveCount(0);

    await page.getByRole('checkbox', { name: `Select issue: Measured how ${ts}?` }).click();
    await page.getByRole('checkbox', { name: `Select issue: Over what period ${ts}?` }).click();
    await expect(page.getByTestId('bulk-count')).toHaveText('2 selected');

    await page.getByTestId('bulk-mark-advisory').click();
    const dialog = page.getByRole('alertdialog');
    await expect(dialog).toContainText('Mark advisory: 2 issues?');
    await Promise.all([
      page.waitForResponse(r => r.url().endsWith('/commands/BulkResolveIssues') && r.status() === 200),
      dialog.getByRole('button', { name: 'Mark advisory' }).click(),
    ]);

    await expect(page.getByTestId('bulk-summary-counts')).toContainText('2 done, 0 skipped, 0 refused, 0 failed');
    await expect(page.getByTestId('bulk-toolbar')).toHaveCount(0);
    await expect(page.getByTestId('open-issue-required')).toHaveCount(0);
    await expect(page.getByTestId('open-issue-optional')).toHaveCount(2);

    await page.close();
  });
});
