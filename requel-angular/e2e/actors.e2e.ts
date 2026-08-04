import { test, expect } from './fixtures/auth';
import {
  createProject, deleteProject,
  createActor, deleteActor, getActorVersion, ActorFixture,
  createGoal, deleteGoal, GoalFixture,
  createStory, deleteStory, getStoryVersion, StoryFixture,
  createUseCase, deleteUseCase, getUseCaseVersion, UseCaseFixture,
  addGoalToActor,
} from './fixtures/api-helper';
import { ActorListPage, ActorEditorPage } from './pages/ActorEditorPage';
import { StoryEditorPage } from './pages/StoryEditorPage';
import { UseCaseEditorPage } from './pages/UseCaseEditorPage';
import { GoalEditorPage } from './pages/GoalEditorPage';
import { reloadAndWaitForGet } from './helpers/navigation';

const PROJECT_NAME = `e2e-actors-${Date.now()}`;
let actorToCleanup: ActorFixture | null = null;
let goalToCleanup: GoalFixture | null = null;
let storyToCleanup: StoryFixture | null = null;
let ucToCleanup: UseCaseFixture | null = null;

test.beforeAll(async ({ request }) => {
  await createProject(request, PROJECT_NAME, 'Actors E2E test project');
});

test.afterAll(async ({ request }) => {
  await deleteProject(request, PROJECT_NAME);
});

test.afterEach(async ({ request }) => {
  // Clean up referencing entities first (use cases / stories / goals) so the
  // actor isn't left referenced when we try to delete it.
  if (ucToCleanup) {
    try {
      const version = await getUseCaseVersion(request, ucToCleanup);
      await deleteUseCase(request, { ...ucToCleanup, version });
    } catch { /* ignore */ }
    ucToCleanup = null;
  }
  if (storyToCleanup) {
    try {
      const version = await getStoryVersion(request, storyToCleanup);
      await deleteStory(request, { ...storyToCleanup, version });
    } catch { /* ignore */ }
    storyToCleanup = null;
  }
  if (goalToCleanup) {
    try { await deleteGoal(request, goalToCleanup); } catch { /* ignore */ }
    goalToCleanup = null;
  }
  if (actorToCleanup) {
    try {
      // Re-fetch current version — saves increment it, making stored version stale.
      const version = await getActorVersion(request, actorToCleanup);
      await deleteActor(request, { ...actorToCleanup, version });
    } catch {
      // may already be deleted by the test
    }
    actorToCleanup = null;
  }
});

test.describe('Actor management', () => {

  test('create actor → appears in actor list', async ({ adminContext }) => {
    const actorName = `e2e-actor-create-${Date.now()}`;
    const page = await adminContext.newPage();
    const listPage = new ActorListPage(page);
    const editorPage = new ActorEditorPage(page);

    await listPage.goto(PROJECT_NAME);
    await listPage.clickNewActor();

    await editorPage.fillName(actorName);
    await editorPage.fillDescription('Actor created by E2E test');
    await editorPage.save(); // "Create" button for new actor

    await page.waitForURL(/\/actors\/\d+/);

    const url = page.url();
    const idMatch = url.match(/\/actors\/(\d+)/);
    if (idMatch) {
      actorToCleanup = { id: parseInt(idMatch[1], 10), version: 0, name: actorName, projectName: PROJECT_NAME };
    }

    await listPage.goto(PROJECT_NAME);
    await listPage.expectActorInTable(actorName);

    await page.close();
  });

  test('rename actor → name persists after save and reload', async ({ adminContext, request }) => {
    const originalName = `e2e-actor-rename-${Date.now()}`;
    const newName = `${originalName}-renamed`;
    const actor = await createActor(request, PROJECT_NAME, originalName);
    actorToCleanup = { ...actor, name: newName };

    const page = await adminContext.newPage();
    const listPage = new ActorListPage(page);
    const editorPage = new ActorEditorPage(page);

    await listPage.goto(PROJECT_NAME);
    await listPage.clickActor(originalName);

    await editorPage.fillName(newName);
    await editorPage.save();

    await reloadAndWaitForGet(page, r => /\/actors\/\d+$/.test(r.url()));
    await editorPage.expectNameValue(newName);

    await page.close();
  });

  test('copy actor → a second entry appears in actor list', async ({ adminContext, request }) => {
    const actorName = `e2e-actor-copy-${Date.now()}`;
    const actor = await createActor(request, PROJECT_NAME, actorName);
    actorToCleanup = actor;

    const page = await adminContext.newPage();
    const listPage = new ActorListPage(page);
    const editorPage = new ActorEditorPage(page);

    await listPage.goto(PROJECT_NAME);
    await listPage.clickActor(actorName);

    await editorPage.copy();

    // copy() waits for the CopyActor API response; Angular navigates to the copy's URL.
    // Wait for navigation to a different actor ID than the original.
    await page.waitForURL(url => !url.toString().includes(`/actors/${actor.id}`));

    // Navigate to list and wait for 2 data rows — toHaveCount auto-waits for Angular
    // This project is fresh (beforeAll creates it empty) so only this test's actors exist.
    await listPage.goto(PROJECT_NAME);
    // Search by actorName so both "e2e-actor-copy-..." and "Copy of e2e-actor-copy-..." appear,
    // filtering out any leftover actors from other tests in this project.
    await listPage.searchFor(actorName);
    await expect.poll(() => listPage.countActorRows(actorName)).toBe(2);

    // Cleanup the copy by deleting it via API (id is in current URL)
    const copyUrl = page.url();
    const copyIdMatch = copyUrl.match(/\/actors\/(\d+)/);
    if (copyIdMatch && parseInt(copyIdMatch[1], 10) !== actor.id) {
      await deleteActor(request, {
        id: parseInt(copyIdMatch[1], 10),
        version: 0,
        name: `Copy of ${actorName}`,
        projectName: PROJECT_NAME,
      });
    }

    await page.close();
  });

  test('delete actor → removed from list', async ({ adminContext, request }) => {
    const actorName = `e2e-actor-delete-${Date.now()}`;
    await createActor(request, PROJECT_NAME, actorName);
    actorToCleanup = null;

    const page = await adminContext.newPage();
    const listPage = new ActorListPage(page);
    const editorPage = new ActorEditorPage(page);

    await listPage.goto(PROJECT_NAME);
    await listPage.clickActor(actorName);

    await editorPage.delete();

    await page.waitForURL(/\/actors$/);
    await listPage.expectActorNotInTable(actorName);

    await page.close();
  });

  test('newly-created actor appears in the primary-actor dropdown for both story and use-case editors', async ({ adminContext, request }) => {
    const actorName = `e2e-actor-dropdown-${Date.now()}`;
    const actor = await createActor(request, PROJECT_NAME, actorName);
    actorToCleanup = actor;

    const page = await adminContext.newPage();
    const storyEditorPage = new StoryEditorPage(page);
    const ucEditorPage = new UseCaseEditorPage(page);

    // Story editor — open new-story form and verify the actor is offered as a primary-actor option.
    // Since #158 /stories/new is the wizard; Primary Actor lives on its first (Details) step,
    // so the dropdown is reachable without advancing, and ids are generated — locate by testid.
    await page.goto(`/projects/${encodeURIComponent(PROJECT_NAME)}/stories/new`);
    await expect(page.getByTestId('story-name')).toBeVisible();
    await storyEditorPage.expectActorInPrimaryActorDropdown(actorName);

    // Use-case editor — same verification on the use-case form.
    await page.goto(`/projects/${encodeURIComponent(PROJECT_NAME)}/use-cases/new`);
    await expect(page.locator('#name')).toBeVisible();
    await ucEditorPage.expectActorInPrimaryActorDropdown(actorName);

    await page.close();
  });

  test('back button on actor editor → returns to actor list', async ({ adminContext, request }) => {
    const actorName = `e2e-actor-back-${Date.now()}`;
    const actor = await createActor(request, PROJECT_NAME, actorName);
    actorToCleanup = actor;

    const page = await adminContext.newPage();
    const listPage = new ActorListPage(page);
    const editorPage = new ActorEditorPage(page);

    await listPage.goto(PROJECT_NAME);
    await listPage.clickActor(actorName);

    await editorPage.navigateBack(PROJECT_NAME);
    // navigateBack already waits for the URL — re-asserting keeps the test self-documenting.
    expect(page.url()).toMatch(/\/actors$/);

    await page.close();
  });

});

test.describe('Actor sub-tables', () => {

  test('add goal to actor → appears in goals table', async ({ adminContext, request }) => {
    const actorName = `e2e-actor-goal-add-${Date.now()}`;
    const goalName = `e2e-actor-goal-${Date.now()}`;
    const actor = await createActor(request, PROJECT_NAME, actorName);
    const goal = await createGoal(request, PROJECT_NAME, goalName);
    actorToCleanup = actor;
    goalToCleanup = goal;

    const page = await adminContext.newPage();
    const listPage = new ActorListPage(page);
    const editorPage = new ActorEditorPage(page);

    await listPage.goto(PROJECT_NAME);
    await listPage.clickActor(actorName);

    await editorPage.addGoal(goalName);
    await editorPage.expectGoalInTable(goalName);

    await page.close();
  });

  test('remove goal from actor → gone from goals table', async ({ adminContext, request }) => {
    const actorName = `e2e-actor-goal-rm-${Date.now()}`;
    const goalName = `e2e-actor-goalrm-${Date.now()}`;
    const actor = await createActor(request, PROJECT_NAME, actorName);
    const goal = await createGoal(request, PROJECT_NAME, goalName);
    actorToCleanup = actor;
    goalToCleanup = goal;
    await addGoalToActor(request, PROJECT_NAME, actor.id, goal.id);

    const page = await adminContext.newPage();
    const listPage = new ActorListPage(page);
    const editorPage = new ActorEditorPage(page);

    await listPage.goto(PROJECT_NAME);
    await listPage.clickActor(actorName);

    await editorPage.expectGoalInTable(goalName);
    await editorPage.removeGoal(goalName);
    await editorPage.expectGoalNotInTable(goalName);

    await page.close();
  });

  test('click goal link in actor goals table → navigates to goal editor', async ({ adminContext, request }) => {
    const actorName = `e2e-actor-goal-nav-${Date.now()}`;
    const goalName = `e2e-actor-goalnav-${Date.now()}`;
    const actor = await createActor(request, PROJECT_NAME, actorName);
    const goal = await createGoal(request, PROJECT_NAME, goalName);
    actorToCleanup = actor;
    goalToCleanup = goal;
    await addGoalToActor(request, PROJECT_NAME, actor.id, goal.id);

    const page = await adminContext.newPage();
    const listPage = new ActorListPage(page);
    const editorPage = new ActorEditorPage(page);
    const goalEditorPage = new GoalEditorPage(page);

    await listPage.goto(PROJECT_NAME);
    await listPage.clickActor(actorName);

    await editorPage.expectGoalInTable(goalName);
    await editorPage.clickGoal(goalName);

    await page.waitForURL(/\/goals\/\d+$/);
    await goalEditorPage.expectNameValue(goalName);

    await page.close();
  });

  test('click referenced-by use case link → navigates to use case editor', async ({ adminContext, request }) => {
    const actorName = `e2e-actor-refuc-${Date.now()}`;
    const ucName = `e2e-actor-refuc-uc-${Date.now()}`;
    const actor = await createActor(request, PROJECT_NAME, actorName);
    // The use case must reference this actor as primary or additional — the
    // simplest path is creating the UC with this actor as primary; the backend
    // wires up the referenced-by link automatically.
    const uc = await createUseCase(request, PROJECT_NAME, ucName, '', actorName);
    actorToCleanup = actor;
    ucToCleanup = uc;

    const page = await adminContext.newPage();
    const listPage = new ActorListPage(page);
    const editorPage = new ActorEditorPage(page);
    const ucEditorPage = new UseCaseEditorPage(page);

    await listPage.goto(PROJECT_NAME);
    await listPage.clickActor(actorName);

    await editorPage.expectReferencedByUseCase(ucName);
    await editorPage.clickReferencedByUseCase(ucName);

    await page.waitForURL(/\/use-cases\/\d+$/);
    await ucEditorPage.expectNameValue(ucName);

    await page.close();
  });

  test('click referenced-by story link → navigates to story editor', async ({ adminContext, request }) => {
    const actorName = `e2e-actor-refstory-${Date.now()}`;
    const storyName = `e2e-actor-refstory-name-${Date.now()}`;
    const actor = await createActor(request, PROJECT_NAME, actorName);
    const story = await createStory(request, PROJECT_NAME, storyName, 'Success', '', actorName);
    actorToCleanup = actor;
    storyToCleanup = story;

    const page = await adminContext.newPage();
    const listPage = new ActorListPage(page);
    const editorPage = new ActorEditorPage(page);
    const storyEditorPage = new StoryEditorPage(page);

    await listPage.goto(PROJECT_NAME);
    await listPage.clickActor(actorName);

    await editorPage.expectReferencedByStory(storyName);
    await editorPage.clickReferencedByStory(storyName);

    await page.waitForURL(/\/stories\/\d+$/);
    await storyEditorPage.expectNameValue(storyName);

    await page.close();
  });

});
