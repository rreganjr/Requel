import { test, expect } from './fixtures/auth';
import { createProject, deleteProject, createScenario, setScenarioSteps } from './fixtures/api-helper';

// #483 (rc4): a new step's name suggests the project's existing steps, and picking one links it
// instead of adding a new step. A new step named like an existing one, not picked, is refused with
// the editor's own message rather than the API's hint.

const PROJECT_NAME = `e2e-step-reuse-${Date.now()}`;
const enc = encodeURIComponent(PROJECT_NAME);

test.beforeAll(async ({ request }) => {
  await createProject(request, PROJECT_NAME, 'Step reuse #483 e2e project');
});

test.afterAll(async ({ request }) => {
  await deleteProject(request, PROJECT_NAME);
});

test.describe('Reusing a step (#483)', () => {

  test('picking a suggested step links it, and its page lists both scenarios', async ({ adminContext, request }) => {
    const ts = Date.now();
    const stepName = `Member downloads the receipt ${ts}`;
    const first = await createScenario(request, PROJECT_NAME, `e2e-reuse-first-${ts}`);
    const [stepId] = await setScenarioSteps(request, first, [stepName]);
    const second = await createScenario(request, PROJECT_NAME, `e2e-reuse-second-${ts}`);

    const page = await adminContext.newPage();
    await Promise.all([
      page.waitForResponse(r => /\/scenarios\/\d+$/.test(r.url()) && r.status() === 200),
      page.goto(`/projects/${enc}/scenarios/${second.id}`),
    ]);

    await page.getByTestId('scenario-add-step-bottom').click();
    const name = page.getByTestId('scenario-step-row').last().getByTestId('scenario-step-name');
    await name.click();
    await name.pressSequentially('downloads the rec', { delay: 30 });
    const suggestion = page.getByTestId('scenario-step-suggestion').filter({ hasText: stepName });
    await expect(suggestion).toBeVisible();
    await expect(suggestion).toContainText('used in 1 scenario');
    await suggestion.click();
    await expect(page.getByTestId('scenario-step-row').last().getByTestId('scenario-step-name'))
      .toHaveValue(stepName);

    await Promise.all([
      page.waitForResponse(r => r.url().includes('/api/commands/EditScenario') && r.status() === 200),
      page.getByTestId('scenario-save').click(),
    ]);

    await page.goto(`/projects/${enc}/steps/${stepId}`);
    await expect(page.getByTestId('step-used-in-row')).toHaveCount(2);
  });

  test('a new step named like an existing one, not picked, gets the editor\'s message', async ({ adminContext, request }) => {
    const ts = Date.now();
    const stepName = `Member signs the slip ${ts}`;
    const first = await createScenario(request, PROJECT_NAME, `e2e-taken-first-${ts}`);
    await setScenarioSteps(request, first, [stepName]);
    const second = await createScenario(request, PROJECT_NAME, `e2e-taken-second-${ts}`);

    const page = await adminContext.newPage();
    await Promise.all([
      page.waitForResponse(r => /\/scenarios\/\d+$/.test(r.url()) && r.status() === 200),
      page.goto(`/projects/${enc}/scenarios/${second.id}`),
    ]);

    await page.getByTestId('scenario-add-step-bottom').click();
    const name = page.getByTestId('scenario-step-row').last().getByTestId('scenario-step-name');
    await name.fill(stepName);
    await name.press('Escape');
    await name.press('Tab');

    await Promise.all([
      page.waitForResponse(r => r.url().includes('/api/commands/EditScenario') && r.status() === 409),
      page.getByTestId('scenario-save').click(),
    ]);
    await expect(page.getByTestId('scenario-error'))
      .toContainText(`Another step in this project is already named '${stepName}'`);
    await expect(page.getByTestId('scenario-error')).not.toContainText('stepId');
  });
});
