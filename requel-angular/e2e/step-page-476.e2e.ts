import { test, expect } from './fixtures/auth';
import {
  createProject, deleteProject, createScenario, setScenarioSteps, addIssue, addPosition,
} from './fixtures/api-helper';

// #476 (rc1): a saved step's pencil opens the step's own page, where its issues can be seen and
// resolved; a row whose step has open issues shows a warning icon (#478: in the pencil's place);
// Open Issues links step issues.
// The issues are added through the API so the test doesn't depend on analysis timing.

const PROJECT_NAME = `e2e-step-page-${Date.now()}`;
const enc = encodeURIComponent(PROJECT_NAME);

test.beforeAll(async ({ request }) => {
  await createProject(request, PROJECT_NAME, 'Step page #476 e2e project');
});

test.afterAll(async ({ request }) => {
  await deleteProject(request, PROJECT_NAME);
});

test.describe('Step page (#476)', () => {

  test('the triangle on the row opens the step page; resolve and rename there', async ({ adminContext, request }) => {
    const ts = Date.now();
    const scenario = await createScenario(request, PROJECT_NAME, `e2e-step-page-scenario-${ts}`);
    const [stepId] = await setScenarioSteps(request, scenario,
      [`Member fills in the form ${ts}`, `The system records the request ${ts}`]);
    const issueText = `Step issue ${ts}`;
    const issue = await addIssue(request, PROJECT_NAME, 'Step', stepId, issueText, false, 'HIGH');
    await addPosition(request, PROJECT_NAME, issue.id, 'Leave it as it is');

    const page = await adminContext.newPage();
    await Promise.all([
      page.waitForResponse(r => /\/scenarios\/\d+$/.test(r.url()) && r.status() === 200),
      page.goto(`/projects/${enc}/scenarios/${scenario.id}`),
    ]);

    const rows = page.getByTestId('scenario-step-row');
    // #478: the triangle takes the pencil's place on a row with open issues.
    const icon = rows.nth(0).getByTestId('scenario-step-issues');
    await expect(icon).toBeVisible();
    await expect(icon).toHaveAttribute('data-severity', 'HIGH');
    await expect(icon.locator('button')).toHaveAttribute('aria-label', /open issue/);
    await expect(rows.nth(0).getByTestId('scenario-step-edit')).toHaveCount(0);

    await icon.locator('button').click();
    await page.waitForURL(new RegExp(`/projects/${enc}/steps/${stepId}$`));
    await expect(page.getByTestId('step-name')).toHaveValue(`Member fills in the form ${ts}`);
    await expect(page.getByTestId('step-used-in-row')).toHaveCount(1);

    const issueItem = page.getByTestId('annotation-issue').filter({ hasText: issueText });
    const positionItem = issueItem.getByTestId('annotation-position').filter({ hasText: 'Leave it as it is' });
    await expect(positionItem).toBeVisible();
    await Promise.all([
      page.waitForResponse(r => r.url().includes('/api/commands/ResolveIssue')),
      positionItem.getByTestId('annotation-resolve-issue').click(),
    ]);
    await expect(page.locator('[data-testid="annotation-issue"][data-resolved="true"]', { hasText: issueText }))
      .toBeVisible();

    await page.getByTestId('step-name').fill(`Member completes the form ${ts}`);
    await Promise.all([
      page.waitForResponse(r => r.url().includes('/api/commands/EditScenarioStep') && r.status() === 200),
      page.getByTestId('step-save').click(),
    ]);

    await Promise.all([
      page.waitForResponse(r => /\/scenarios\/\d+$/.test(r.url()) && r.status() === 200),
      page.getByTestId('step-used-in-link').first().click(),
    ]);
    await expect(rows.nth(0).getByTestId('scenario-step-name')).toHaveValue(`Member completes the form ${ts}`);
    // The HIGH issue is resolved; anything left on the step comes from analysis, never HIGH.
    await expect(rows.nth(0).locator('[data-testid="scenario-step-issues"][data-severity="HIGH"]'))
      .toHaveCount(0);

    await page.close();
  });

  test('the step button is disabled while the scenario has unsaved changes', async ({ adminContext, request }) => {
    const ts = Date.now();
    const scenario = await createScenario(request, PROJECT_NAME, `e2e-step-page-dirty-${ts}`);
    await setScenarioSteps(request, scenario, [`Member signs in ${ts}`]);

    const page = await adminContext.newPage();
    await Promise.all([
      page.waitForResponse(r => /\/scenarios\/\d+$/.test(r.url()) && r.status() === 200),
      page.goto(`/projects/${enc}/scenarios/${scenario.id}`),
    ]);
    const row = page.getByTestId('scenario-step-row').nth(0);
    // #478: analysis may have put an issue on the step, so the row shows the pencil or the
    // triangle; both follow the same rule.
    const pencil = row.locator('[data-testid="scenario-step-edit"], [data-testid="scenario-step-issues"]')
      .locator('button');
    await expect(pencil).toBeEnabled();

    await page.getByTestId('scenario-name').fill(`e2e-step-page-dirty-renamed-${ts}`);
    await expect(pencil).toBeDisabled();

    await page.close();
  });

  test('Open Issues links a step issue to the step page', async ({ adminContext, request }) => {
    const ts = Date.now();
    const scenario = await createScenario(request, PROJECT_NAME, `e2e-step-page-open-${ts}`);
    const [stepId] = await setScenarioSteps(request, scenario, [`Member pays the fine ${ts}`]);
    const issueText = `Open step issue ${ts}`;
    await addIssue(request, PROJECT_NAME, 'Step', stepId, issueText, true);

    const page = await adminContext.newPage();
    await Promise.all([
      page.waitForResponse(r => r.url().includes(`/api/projects/${enc}/open-issues`) && r.status() === 200),
      page.goto(`/projects/${enc}/open-issues`),
    ]);
    const row = page.getByRole('row', { name: new RegExp(issueText) });
    await row.getByTestId('open-issue-entity-link').click();
    await page.waitForURL(new RegExp(`/projects/${enc}/steps/${stepId}$`));
    await expect(page.getByTestId('step-name')).toHaveValue(`Member pays the fine ${ts}`);

    await page.close();
  });
});
