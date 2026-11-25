import { test, expect } from './fixtures/auth';
import {
  createProject, deleteProject, createActor, createUseCase, createScenario, setScenarioSteps,
  setPrimaryScenarioOnUseCase,
} from './fixtures/api-helper';

// #468: a step's actor as a role. One step, "ends the room" with the primary-actor role, reads
// with each use case's own primary actor and stays one step.

const PROJECT_NAME = `e2e-step-role-${Date.now()}`;
const enc = encodeURIComponent(PROJECT_NAME);

test.beforeAll(async ({ request }) => {
  await createProject(request, PROJECT_NAME, 'Step actor role #468 e2e project');
});

test.afterAll(async ({ request }) => {
  await deleteProject(request, PROJECT_NAME);
});

test.describe('A step\'s actor as a role (#468)', () => {

  test('a primary-actor step reads with each use case\'s primary actor', async ({ adminContext, request }) => {
    const ts = Date.now();
    await createActor(request, PROJECT_NAME, `Operator ${ts}`);
    await createActor(request, PROJECT_NAME, `Engineer ${ts}`);
    const scenario = await createScenario(request, PROJECT_NAME, `e2e-role-scenario-${ts}`);
    const [stepId] = await setScenarioSteps(request, scenario, [`ends the room ${ts}`]);
    const run = await createUseCase(request, PROJECT_NAME, `Run a session ${ts}`, '', `Operator ${ts}`);
    const respond = await createUseCase(request, PROJECT_NAME, `Respond to an alarm ${ts}`, '', `Engineer ${ts}`);
    await setPrimaryScenarioOnUseCase(request, PROJECT_NAME, run.id, scenario.id);
    await setPrimaryScenarioOnUseCase(request, PROJECT_NAME, respond.id, scenario.id);

    const page = await adminContext.newPage();
    await Promise.all([
      page.waitForResponse(r => /\/scenarios\/\d+$/.test(r.url()) && r.status() === 200),
      page.goto(`/projects/${enc}/scenarios/${scenario.id}`),
    ]);
    await page.getByTestId('scenario-step-row').nth(0).getByTestId('scenario-step-who').click();
    await page.getByRole('option', { name: 'Primary actor', exact: true }).click();
    await Promise.all([
      page.waitForResponse(r => r.url().includes('/api/commands/EditScenario') && r.status() === 200),
      page.getByTestId('scenario-save').click(),
    ]);

    await page.goto(`/projects/${enc}/steps/${stepId}`);
    await expect(page.getByTestId('step-reads-as-row')).toHaveText([
      `Engineer ${ts} ends the room ${ts} in Respond to an alarm ${ts}`,
      `Operator ${ts} ends the room ${ts} in Run a session ${ts}`,
    ]);
    await expect(page.getByTestId('step-used-in-row')).toHaveCount(1);

    await page.goto(`/projects/${enc}/use-cases/${run.id}`);
    await expect(page.getByTestId('use-case-step-name')).toHaveText([`Operator ${ts} ends the room ${ts}`]);
    await page.goto(`/projects/${enc}/use-cases/${respond.id}`);
    await expect(page.getByTestId('use-case-step-name')).toHaveText([`Engineer ${ts} ends the room ${ts}`]);
  });
});
