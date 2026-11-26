import { test, expect } from './fixtures/auth';
import { createProject, deleteProject, createGoal, createStory, addIssue } from './fixtures/api-helper';

// #504: search a project from the workspace header, filter by type, and open a result.
// The matches are created through the API under a word unique to the run.

const PROJECT_NAME = `e2e-search-${Date.now()}`;
const enc = encodeURIComponent(PROJECT_NAME);

test.beforeAll(async ({ request }) => {
  await createProject(request, PROJECT_NAME, 'Project search #504 e2e project');
});

test.afterAll(async ({ request }) => {
  await deleteProject(request, PROJECT_NAME);
});

test.describe('Project search (#504)', () => {

  test('search from the header, filter to a type, and open a result', async ({ adminContext, request }) => {
    const word = `zq${Date.now()}`;
    const goal = await createGoal(request, PROJECT_NAME, `Speed up ${word}`, 'Nothing to see here');
    const story = await createStory(request, PROJECT_NAME, `Clerk files a form ${word.slice(2)}`, 'Success',
      `Then the ${word} is saved.`);
    await addIssue(request, PROJECT_NAME, 'Goal', goal.id, `Is the ${word} measured?`);

    const page = await adminContext.newPage();
    await page.goto(`/projects/${enc}`);
    const box = page.locator('.ws-header').getByTestId('project-search-input');
    await expect(box).toBeVisible();

    // Under 2 characters: the button is off and the hint says why.
    await box.fill('z');
    await expect(page.getByTestId('project-search-submit').locator('button')).toBeDisabled();
    await expect(page.getByTestId('project-search-hint')).toBeVisible();

    await box.fill(word);
    await Promise.all([
      page.waitForResponse(r => r.url().includes(`/api/projects/${enc}/search`) && r.status() === 200),
      box.press('Enter'),
    ]);
    await page.waitForURL(new RegExp(`/projects/${enc}/search\\?q=${word}$`));

    await expect(page.getByTestId('search-summary')).toContainText('Showing 3 of 3 matches');
    const results = page.getByTestId('search-result');
    await expect(results).toHaveCount(3);
    await expect(results.first().getByTestId('search-result-link')).toHaveText(`Speed up ${word}`);
    await expect(results.nth(1).locator('mark')).toHaveText(word);
    await expect(page.getByTestId('search-group-Issue').getByTestId('search-issue-state')).toHaveText(/Open/);

    await Promise.all([
      page.waitForResponse(r => r.url().includes('type=Story') && r.status() === 200),
      page.getByTestId('search-filter-Story').click(),
    ]);
    await expect(results).toHaveCount(1);
    await expect(page.getByTestId('search-filter-Goal')).toContainText('1');

    await results.first().getByTestId('search-result-link').click();
    await page.waitForURL(new RegExp(`/projects/${enc}/stories/${story.id}$`));

    await page.close();
  });
});
