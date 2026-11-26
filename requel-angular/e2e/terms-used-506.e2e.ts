import { test, expect } from './fixtures/auth';
import { createProject, deleteProject, createGoal, createTerm } from './fixtures/api-helper';

// #506: a goal editor lists the glossary terms its text uses, live as it is typed, with each
// term's definition on focus; a chip opens its term.

const PROJECT_NAME = `e2e-terms-used-${Date.now()}`;
const enc = encodeURIComponent(PROJECT_NAME);

test.beforeAll(async ({ request }) => {
  await createProject(request, PROJECT_NAME, 'Terms used #506 e2e project');
});

test.afterAll(async ({ request }) => {
  await deleteProject(request, PROJECT_NAME);
});

test.describe('Terms used (#506)', () => {

  test('chips follow the typed text, show the definition, and open the term', async ({ adminContext, request }) => {
    const host = await createTerm(request, PROJECT_NAME, 'Host', 'The person running the meeting.');
    await createTerm(request, PROJECT_NAME, 'video room', 'Where a meeting happens.');
    await createTerm(request, PROJECT_NAME, 'room', 'Any space.');
    const goal = await createGoal(request, PROJECT_NAME, 'End meetings cleanly', 'Nothing defined here.');

    const page = await adminContext.newPage();
    await page.goto(`/projects/${enc}/goals/${goal.id}`);
    const text = page.getByTestId('goal-text');
    await expect(text).toHaveValue('Nothing defined here.');
    await expect(page.getByTestId('terms-used')).toHaveCount(0);

    await text.fill('The Host ends the video room');
    const chips = page.getByTestId('term-chip');
    await expect(chips).toHaveText(['Host', 'video room']);

    await chips.first().focus();
    await expect(page.locator('.p-tooltip')).toContainText('The person running the meeting.');

    // The edit is unsaved, so leaving asks first (dirtyCheckGuard's confirm); accept to open the term.
    page.once('dialog', dialog => void dialog.accept());
    await chips.first().click();
    await page.waitForURL(new RegExp(`/projects/${enc}/terms/${host.id}$`));

    await page.close();
  });
});
