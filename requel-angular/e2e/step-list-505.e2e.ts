import { test, expect } from './fixtures/auth';
import {
  createProject, deleteProject, createScenario, setScenarioSteps, useExistingSteps, addIssue,
} from './fixtures/api-helper';

// #505: the overview's Steps card opens the steps list; "Has open issues" and "Shared" filter it;
// a row opens the step page, whose Back returns to the filtered list.

const PROJECT_NAME = `e2e-step-list-${Date.now()}`;
const enc = encodeURIComponent(PROJECT_NAME);

test.beforeAll(async ({ request }) => {
  await createProject(request, PROJECT_NAME, 'Steps list #505 e2e project');
});

test.afterAll(async ({ request }) => {
  await deleteProject(request, PROJECT_NAME);
});

test.describe('Steps list (#505)', () => {

  test('card, filters, row navigation and Back', async ({ adminContext, request }) => {
    const ts = Date.now();
    const first = await createScenario(request, PROJECT_NAME, `e2e-take-order-${ts}`);
    const [sharedId, flaggedId] = await setScenarioSteps(request, first,
      [`Clerk enters the order ${ts}`, `Clerk prints a receipt ${ts}`]);
    const second = await createScenario(request, PROJECT_NAME, `e2e-change-order-${ts}`);
    await useExistingSteps(request, second, [sharedId]);
    await addIssue(request, PROJECT_NAME, 'Step', flaggedId, `Which printer ${ts}?`, false, 'HIGH');

    const page = await adminContext.newPage();
    await page.goto(`/projects/${enc}`);
    const card = page.getByTestId('count-steps');
    await expect(card).toContainText('2');
    await card.click();
    await page.waitForURL(new RegExp(`/projects/${enc}/steps$`));

    const rows = page.getByTestId('step-row');
    await expect(rows).toHaveCount(2);

    await page.getByText('Has open issues', { exact: true }).click();
    await expect(rows).toHaveCount(1);
    await expect(rows.first()).toContainText(`Clerk prints a receipt ${ts}`);
    await expect(rows.first().getByTestId('step-severity')).toContainText('High');
    await page.waitForURL(/issues=1/);

    await page.getByText('Has open issues', { exact: true }).click();
    await page.getByText('Shared', { exact: true }).click();
    await expect(rows).toHaveCount(1);
    await expect(rows.first()).toContainText(`Clerk enters the order ${ts}`);

    await rows.first().click();
    await page.waitForURL(new RegExp(`/projects/${enc}/steps/${sharedId}$`));
    await expect(page.getByTestId('step-used-in-row')).toHaveCount(2);

    await page.getByTestId('step-back').click();
    await page.waitForURL(new RegExp(`/projects/${enc}/steps\\?shared=1$`));
    await expect(rows).toHaveCount(1);

    await page.close();
  });
});
