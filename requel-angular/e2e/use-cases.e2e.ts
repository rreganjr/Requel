import { test, expect } from './fixtures/auth';
import {
  createProject, deleteProject,
  createActor, deleteActor, getActorVersion, ActorFixture,
  createGoal, deleteGoal, GoalFixture,
  createStory, deleteStory, getStoryVersion, StoryFixture,
  createScenario, deleteScenario, getScenarioVersion, ScenarioFixture,
  createUseCase, deleteUseCase, getUseCaseVersion, UseCaseFixture,
  addActorToUseCase, addGoalToUseCase, addStoryToUseCase,
  addScenarioToUseCase, setPrimaryScenarioOnUseCase,
} from './fixtures/api-helper';
import { UseCaseListPage, UseCaseEditorPage } from './pages/UseCaseEditorPage';
import { reloadAndWaitForGet } from './helpers/navigation';

const PROJECT_NAME = `e2e-use-cases-${Date.now()}`;
// Shared actor for all tests — use cases require a primary actor (primary_actor_id NOT NULL)
const ACTOR_NAME = 'E2E Actor';
let ucToCleanup: UseCaseFixture | null = null;
let actorToCleanup: ActorFixture | null = null;
let goalToCleanup: GoalFixture | null = null;
let storyToCleanup: StoryFixture | null = null;
let scenarioToCleanup: ScenarioFixture | null = null;

test.beforeAll(async ({ request }) => {
  await createProject(request, PROJECT_NAME, 'Use Cases E2E test project');
  await createActor(request, PROJECT_NAME, ACTOR_NAME);
});

test.afterAll(async ({ request }) => {
  await deleteProject(request, PROJECT_NAME);
});

test.afterEach(async ({ request }) => {
  if (ucToCleanup) {
    try {
      const version = await getUseCaseVersion(request, ucToCleanup);
      await deleteUseCase(request, { ...ucToCleanup, version });
    } catch {
      // may already be deleted by the test
    }
    ucToCleanup = null;
  }
  if (actorToCleanup) {
    try {
      const version = await getActorVersion(request, actorToCleanup);
      await deleteActor(request, { ...actorToCleanup, version });
    } catch { /* ignore */ }
    actorToCleanup = null;
  }
  if (goalToCleanup) {
    try { await deleteGoal(request, goalToCleanup); } catch { /* ignore */ }
    goalToCleanup = null;
  }
  if (storyToCleanup) {
    try {
      const version = await getStoryVersion(request, storyToCleanup);
      await deleteStory(request, { ...storyToCleanup, version });
    } catch { /* ignore */ }
    storyToCleanup = null;
  }
  if (scenarioToCleanup) {
    try {
      const version = await getScenarioVersion(request, scenarioToCleanup);
      await deleteScenario(request, { ...scenarioToCleanup, version });
    } catch { /* ignore */ }
    scenarioToCleanup = null;
  }
});

test.describe('Use Case management', () => {

  test('create use case → appears in use case list', async ({ adminContext }) => {
    const ucName = `e2e-uc-create-${Date.now()}`;
    const page = await adminContext.newPage();
    const listPage = new UseCaseListPage(page);
    const editorPage = new UseCaseEditorPage(page);

    await listPage.goto(PROJECT_NAME);
    await listPage.clickNewUseCase();

    await editorPage.fillName(ucName);
    // Primary actor is required; select the shared actor created in beforeAll
    await editorPage.selectPrimaryActor(ACTOR_NAME);
    await editorPage.fillDescription('Use case created by E2E test');
    await editorPage.save();

    await page.waitForURL(/\/use-cases\/\d+/);

    const url = page.url();
    const idMatch = url.match(/\/use-cases\/(\d+)/);
    if (idMatch) {
      ucToCleanup = { id: parseInt(idMatch[1], 10), version: 0, name: ucName, projectName: PROJECT_NAME };
    }

    await listPage.goto(PROJECT_NAME);
    await listPage.expectUseCaseInTable(ucName);

    await page.close();
  });

  test('rename use case → name persists after save and reload', async ({ adminContext, request }) => {
    const originalName = `e2e-uc-rename-${Date.now()}`;
    const newName = `${originalName}-renamed`;
    const uc = await createUseCase(request, PROJECT_NAME, originalName);
    ucToCleanup = { ...uc, name: newName };

    const page = await adminContext.newPage();
    const listPage = new UseCaseListPage(page);
    const editorPage = new UseCaseEditorPage(page);

    await listPage.goto(PROJECT_NAME);
    await listPage.clickUseCase(originalName);

    await editorPage.fillName(newName);
    await editorPage.save();

    await reloadAndWaitForGet(page, r => /\/use-cases\/\d+$/.test(r.url()));
    await editorPage.expectNameValue(newName);

    await page.close();
  });

  test('delete use case → removed from list', async ({ adminContext, request }) => {
    const ucName = `e2e-uc-delete-${Date.now()}`;
    await createUseCase(request, PROJECT_NAME, ucName);
    ucToCleanup = null;

    const page = await adminContext.newPage();
    const listPage = new UseCaseListPage(page);
    const editorPage = new UseCaseEditorPage(page);

    await listPage.goto(PROJECT_NAME);
    await listPage.clickUseCase(ucName);

    await editorPage.delete();

    await page.waitForURL(/\/use-cases$/);
    await listPage.goto(PROJECT_NAME);
    await listPage.expectUseCaseNotInTable(ucName);

    await page.close();
  });

  test('copy use case → a second entry appears in use case list', async ({ adminContext, request }) => {
    const copyProjectName = `e2e-use-cases-copy-${Date.now()}`;
    const ucName = `e2e-uc-copy-${Date.now()}`;
    await createProject(request, copyProjectName, 'Use Case copy E2E test project');
    await createActor(request, copyProjectName, ACTOR_NAME);
    const uc = await createUseCase(request, copyProjectName, ucName);

    const page = await adminContext.newPage();
    const listPage = new UseCaseListPage(page);
    const editorPage = new UseCaseEditorPage(page);

    await listPage.goto(copyProjectName);
    await listPage.clickUseCase(ucName);

    try {
      await editorPage.copy();

      // After copy, the router navigates to the NEW use case — wait for a URL that differs from the original
      await page.waitForURL(url => url.href.includes('/use-cases/') && !url.href.endsWith(`/use-cases/${uc.id}`));

      await listPage.goto(copyProjectName);
      await expect.poll(() => listPage.countUseCaseRows(ucName)).toBe(2);
    } finally {
      await page.close();
      await deleteProject(request, copyProjectName);
    }
  });

});

test.describe('Use Case sub-tables', () => {

  test('add goal → appears in goals table', async ({ adminContext, request }) => {
    const ucName = `e2e-uc-goal-add-${Date.now()}`;
    const goalName = `e2e-uc-goal-${Date.now()}`;
    const uc = await createUseCase(request, PROJECT_NAME, ucName);
    const goal = await createGoal(request, PROJECT_NAME, goalName);
    ucToCleanup = uc;
    goalToCleanup = goal;

    const page = await adminContext.newPage();
    const listPage = new UseCaseListPage(page);
    const editorPage = new UseCaseEditorPage(page);

    await listPage.goto(PROJECT_NAME);
    await listPage.clickUseCase(ucName);

    await editorPage.addGoal(goalName);
    await editorPage.expectInTable(goalName);

    await page.close();
  });

  test('remove goal → gone from goals table', async ({ adminContext, request }) => {
    const ucName = `e2e-uc-goal-rm-${Date.now()}`;
    const goalName = `e2e-uc-goalrm-${Date.now()}`;
    const uc = await createUseCase(request, PROJECT_NAME, ucName);
    const goal = await createGoal(request, PROJECT_NAME, goalName);
    ucToCleanup = uc;
    goalToCleanup = goal;
    await addGoalToUseCase(request, PROJECT_NAME, uc.id, goal.id);

    const page = await adminContext.newPage();
    const listPage = new UseCaseListPage(page);
    const editorPage = new UseCaseEditorPage(page);

    await listPage.goto(PROJECT_NAME);
    await listPage.clickUseCase(ucName);

    await editorPage.expectInTable(goalName);
    await editorPage.removeGoal(goalName);
    await editorPage.expectNotInTable(goalName);

    await page.close();
  });

  test('add story → appears in stories table', async ({ adminContext, request }) => {
    const ucName = `e2e-uc-story-add-${Date.now()}`;
    const storyName = `e2e-uc-story-${Date.now()}`;
    const uc = await createUseCase(request, PROJECT_NAME, ucName);
    const story = await createStory(request, PROJECT_NAME, storyName);
    ucToCleanup = uc;
    storyToCleanup = story;

    const page = await adminContext.newPage();
    const listPage = new UseCaseListPage(page);
    const editorPage = new UseCaseEditorPage(page);

    await listPage.goto(PROJECT_NAME);
    await listPage.clickUseCase(ucName);

    await editorPage.addStory(storyName);
    await editorPage.expectInTable(storyName);

    await page.close();
  });

  test('remove story → gone from stories table', async ({ adminContext, request }) => {
    const ucName = `e2e-uc-story-rm-${Date.now()}`;
    const storyName = `e2e-uc-storyrm-${Date.now()}`;
    const uc = await createUseCase(request, PROJECT_NAME, ucName);
    const story = await createStory(request, PROJECT_NAME, storyName);
    ucToCleanup = uc;
    storyToCleanup = story;
    await addStoryToUseCase(request, PROJECT_NAME, uc.id, story.id);

    const page = await adminContext.newPage();
    const listPage = new UseCaseListPage(page);
    const editorPage = new UseCaseEditorPage(page);

    await listPage.goto(PROJECT_NAME);
    await listPage.clickUseCase(ucName);

    await editorPage.expectInTable(storyName);
    await editorPage.removeStory(storyName);
    await editorPage.expectNotInTable(storyName);

    await page.close();
  });

  test('add additional actor → appears in actors table', async ({ adminContext, request }) => {
    const ucName = `e2e-uc-actor-add-${Date.now()}`;
    const actorName = `e2e-uc-actor-${Date.now()}`;
    const uc = await createUseCase(request, PROJECT_NAME, ucName);
    const actor = await createActor(request, PROJECT_NAME, actorName);
    ucToCleanup = uc;
    actorToCleanup = actor;

    const page = await adminContext.newPage();
    const listPage = new UseCaseListPage(page);
    const editorPage = new UseCaseEditorPage(page);

    await listPage.goto(PROJECT_NAME);
    await listPage.clickUseCase(ucName);

    await editorPage.addAdditionalActor(actorName);
    await editorPage.expectInTable(actorName);

    await page.close();
  });

  test('remove additional actor → gone from actors table', async ({ adminContext, request }) => {
    const ucName = `e2e-uc-actor-rm-${Date.now()}`;
    const actorName = `e2e-uc-actorrm-${Date.now()}`;
    const uc = await createUseCase(request, PROJECT_NAME, ucName);
    const actor = await createActor(request, PROJECT_NAME, actorName);
    ucToCleanup = uc;
    actorToCleanup = actor;
    await addActorToUseCase(request, PROJECT_NAME, uc.id, actor.id);

    const page = await adminContext.newPage();
    const listPage = new UseCaseListPage(page);
    const editorPage = new UseCaseEditorPage(page);

    await listPage.goto(PROJECT_NAME);
    await listPage.clickUseCase(ucName);

    await editorPage.expectInTable(actorName);
    await editorPage.removeAdditionalActor(actorName);
    await editorPage.expectNotInTable(actorName);

    await page.close();
  });

  test('add additional scenario → appears in scenarios table', async ({ adminContext, request }) => {
    const ucName = `e2e-uc-scen-add-${Date.now()}`;
    const scenName = `e2e-uc-scen-${Date.now()}`;
    const uc = await createUseCase(request, PROJECT_NAME, ucName);
    // Additional scenarios must not be Primary type — ScenarioType enum: PreCondition, Primary, Optional, Alternative, Exception
    const scen = await createScenario(request, PROJECT_NAME, scenName, 'Alternative');
    ucToCleanup = uc;
    scenarioToCleanup = scen;

    const page = await adminContext.newPage();
    const listPage = new UseCaseListPage(page);
    const editorPage = new UseCaseEditorPage(page);

    await listPage.goto(PROJECT_NAME);
    await listPage.clickUseCase(ucName);

    await editorPage.addAdditionalScenario(scenName);
    await editorPage.expectInTable(scenName);

    await page.close();
  });

  test('remove additional scenario → gone from scenarios table', async ({ adminContext, request }) => {
    const ucName = `e2e-uc-scen-rm-${Date.now()}`;
    const scenName = `e2e-uc-scenrm-${Date.now()}`;
    const uc = await createUseCase(request, PROJECT_NAME, ucName);
    const scen = await createScenario(request, PROJECT_NAME, scenName, 'Alternative');
    ucToCleanup = uc;
    scenarioToCleanup = scen;
    await addScenarioToUseCase(request, PROJECT_NAME, uc.id, scen.id);

    const page = await adminContext.newPage();
    const listPage = new UseCaseListPage(page);
    const editorPage = new UseCaseEditorPage(page);

    await listPage.goto(PROJECT_NAME);
    await listPage.clickUseCase(ucName);

    await editorPage.expectInTable(scenName);
    await editorPage.removeAdditionalScenario(scenName);
    await editorPage.expectNotInTable(scenName);

    await page.close();
  });

});

test.describe('Use Case primary scenario', () => {

  // EditUseCase always auto-creates a Primary scenario on new use case creation,
  // so useCase().scenarioId is always set. We test that:
  // (a) the auto-created scenario name is shown in the primary scenario card, and
  // (b) "Open in Editor" navigates to the scenario editor.

  test('primary scenario shown in card and Open in Editor navigates to scenario editor', async ({ adminContext, request }) => {
    const ucName = `e2e-uc-primscen-${Date.now()}`;
    const uc = await createUseCase(request, PROJECT_NAME, ucName);
    ucToCleanup = uc;

    const page = await adminContext.newPage();
    const listPage = new UseCaseListPage(page);
    const editorPage = new UseCaseEditorPage(page);

    await listPage.goto(PROJECT_NAME);
    await listPage.clickUseCase(ucName);

    // The scenario is auto-created with the same name as the use case
    await editorPage.expectPrimaryScenarioName(ucName);
    await editorPage.openPrimaryScenarioInEditor();

    await expect(page).toHaveURL(/\/scenarios\/\d+/);

    await page.close();
  });

  test('SetPrimaryScenarioOnUseCase → links scenario and shows name in card', async ({ adminContext, request }) => {
    const ucName = `e2e-uc-setprim-${Date.now()}`;
    const scenName = `e2e-uc-setprim-scen-${Date.now()}`;
    const uc = await createUseCase(request, PROJECT_NAME, ucName);
    const scen = await createScenario(request, PROJECT_NAME, scenName, 'Primary');
    ucToCleanup = uc;
    scenarioToCleanup = scen;

    // Link the external scenario as the primary scenario via API
    await setPrimaryScenarioOnUseCase(request, PROJECT_NAME, uc.id, scen.id);

    const page = await adminContext.newPage();
    const listPage = new UseCaseListPage(page);
    const editorPage = new UseCaseEditorPage(page);

    await listPage.goto(PROJECT_NAME);
    await listPage.clickUseCase(ucName);

    // The linked scenario name should appear in the primary scenario card
    await editorPage.expectPrimaryScenarioName(scenName);

    await page.close();
  });

});
