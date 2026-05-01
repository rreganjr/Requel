import { test, expect } from './fixtures/auth';
import {
  createProject,
  deleteProject,
  createActor,
  deleteActor,
  getActorVersion,
  createStory,
  deleteStory,
  getStoryVersion,
  addActorToStory,
  createGoal,
  deleteGoal,
  StoryFixture,
  ActorFixture,
  GoalFixture,
} from './fixtures/api-helper';
import { StoryListPage, StoryEditorPage } from './pages/StoryEditorPage';
import { GoalEditorPage } from './pages/GoalEditorPage';
import { ActorEditorPage } from './pages/ActorEditorPage';
import { reloadAndWaitForGet } from './helpers/navigation';

const PROJECT_NAME = `e2e-stories-${Date.now()}`;
const ACTOR_NAME = 'E2E Story Actor';
let storiesToCleanup: StoryFixture[] = [];
let actorsToCleanup: ActorFixture[] = [];
let goalsToCleanup: GoalFixture[] = [];

function queueStoryCleanup(story: StoryFixture): void {
  storiesToCleanup.push(story);
}

function queueActorCleanup(actor: ActorFixture): void {
  actorsToCleanup.push(actor);
}

function queueGoalCleanup(goal: GoalFixture): void {
  goalsToCleanup.push(goal);
}

function currentStoryIdFromUrl(url: string): number {
  const idMatch = url.match(/\/stories\/(\d+)/);
  if (!idMatch) {
    throw new Error(`Could not parse story id from URL: ${url}`);
  }
  return parseInt(idMatch[1], 10);
}

test.beforeAll(async ({ request }) => {
  await createProject(request, PROJECT_NAME, 'Stories E2E test project');
  await createActor(request, PROJECT_NAME, ACTOR_NAME);
});

test.afterAll(async ({ request }) => {
  await deleteProject(request, PROJECT_NAME);
});

test.afterEach(async ({ request }) => {
  for (const story of storiesToCleanup) {
    try {
      const version = await getStoryVersion(request, story);
      await deleteStory(request, { ...story, version });
    } catch {
      // may already be deleted by the test
    }
  }
  storiesToCleanup = [];

  for (const actor of actorsToCleanup) {
    try {
      const version = await getActorVersion(request, actor);
      await deleteActor(request, { ...actor, version });
    } catch {
      // may already be deleted by the test
    }
  }
  actorsToCleanup = [];

  for (const goal of goalsToCleanup) {
    try {
      await deleteGoal(request, goal);
    } catch {
      // may already be deleted by the test
    }
  }
  goalsToCleanup = [];
});

test.describe('Story management', () => {

  test('create story → appears in story list', async ({ adminContext }) => {
    const storyName = `e2e-story-create-${Date.now()}`;
    const page = await adminContext.newPage();
    const listPage = new StoryListPage(page);
    const editorPage = new StoryEditorPage(page);

    await listPage.goto(PROJECT_NAME);
    await listPage.clickNewStory();

    await editorPage.fillName(storyName);
    await editorPage.fillText('Story created by E2E test');
    await editorPage.save();

    await page.waitForURL(/\/stories\/\d+/);
    queueStoryCleanup({ id: currentStoryIdFromUrl(page.url()), version: 0, name: storyName, projectName: PROJECT_NAME });

    await listPage.goto(PROJECT_NAME);
    await listPage.expectStoryInTable(storyName);

    await page.close();
  });

  test('change story type → persists after save and reload', async ({ adminContext, request }) => {
    const storyName = `e2e-story-type-${Date.now()}`;
    const story = await createStory(request, PROJECT_NAME, storyName, 'Success');
    queueStoryCleanup(story);

    const page = await adminContext.newPage();
    const listPage = new StoryListPage(page);
    const editorPage = new StoryEditorPage(page);

    await listPage.goto(PROJECT_NAME);
    await listPage.clickStory(storyName);

    await editorPage.selectStoryType('Exception');
    await editorPage.save();

    await reloadAndWaitForGet(page, r => /\/stories\/\d+$/.test(r.url()));
    await editorPage.expectStoryTypeValue('Exception');

    await page.close();
  });

  test('delete story → removed from list', async ({ adminContext, request }) => {
    const storyName = `e2e-story-delete-${Date.now()}`;
    const story = await createStory(request, PROJECT_NAME, storyName);

    const page = await adminContext.newPage();
    const listPage = new StoryListPage(page);
    const editorPage = new StoryEditorPage(page);

    await listPage.goto(PROJECT_NAME);
    await listPage.clickStory(storyName);

    await editorPage.delete();
    storiesToCleanup = storiesToCleanup.filter(item => item.id !== story.id);

    await page.waitForURL(/\/stories$/);
    await listPage.expectStoryNotInTable(storyName);

    await page.close();
  });

  test('set primary actor → actor name persists after save and reload', async ({ adminContext, request }) => {
    const storyName = `e2e-story-actor-set-${Date.now()}`;
    const story = await createStory(request, PROJECT_NAME, storyName);
    queueStoryCleanup(story);

    const page = await adminContext.newPage();
    const listPage = new StoryListPage(page);
    const editorPage = new StoryEditorPage(page);

    await listPage.goto(PROJECT_NAME);
    await listPage.clickStory(storyName);

    await editorPage.selectPrimaryActor(ACTOR_NAME);
    await editorPage.save();

    await reloadAndWaitForGet(page, r => /\/stories\/\d+$/.test(r.url()));

    await editorPage.expectPrimaryActorValue(ACTOR_NAME);

    await page.close();
  });

  test('clear primary actor → placeholder shown after save and reload', async ({ adminContext, request }) => {
    const storyName = `e2e-story-actor-clear-${Date.now()}`;
    const story = await createStory(request, PROJECT_NAME, storyName, 'Success', '', ACTOR_NAME);
    queueStoryCleanup(story);

    const page = await adminContext.newPage();
    const listPage = new StoryListPage(page);
    const editorPage = new StoryEditorPage(page);

    await listPage.goto(PROJECT_NAME);
    await listPage.clickStory(storyName);

    // Verify actor is set before clearing
    await editorPage.expectPrimaryActorValue(ACTOR_NAME);

    await editorPage.clearPrimaryActor();
    await editorPage.save();

    await reloadAndWaitForGet(page, r => /\/stories\/\d+$/.test(r.url()));

    await editorPage.expectNoPrimaryActor();

    await page.close();
  });

  test('rename story → name persists after save and reload', async ({ adminContext, request }) => {
    const originalName = `e2e-story-rename-${Date.now()}`;
    const newName = `${originalName}-renamed`;
    const story = await createStory(request, PROJECT_NAME, originalName);
    queueStoryCleanup({ ...story, name: newName });

    const page = await adminContext.newPage();
    const listPage = new StoryListPage(page);
    const editorPage = new StoryEditorPage(page);

    await listPage.goto(PROJECT_NAME);
    await listPage.clickStory(originalName);

    await editorPage.fillName(newName);
    await editorPage.save();

    await reloadAndWaitForGet(page, r => /\/stories\/\d+$/.test(r.url()));
    await editorPage.expectNameValue(newName);

    await page.close();
  });

  test('copy story → navigates to copied story and shows two matching rows in the list', async ({ adminContext, request }) => {
    const storyName = `e2e-story-copy-${Date.now()}`;
    const story = await createStory(request, PROJECT_NAME, storyName);
    queueStoryCleanup(story);

    const page = await adminContext.newPage();
    const listPage = new StoryListPage(page);
    const editorPage = new StoryEditorPage(page);

    await listPage.goto(PROJECT_NAME);
    await listPage.clickStory(storyName);

    const originalId = currentStoryIdFromUrl(page.url());
    const copiedId = await editorPage.copy();
    await page.waitForURL(`**/projects/${encodeURIComponent(PROJECT_NAME)}/stories/${copiedId}`);
    expect(copiedId).not.toBe(originalId);
    queueStoryCleanup({ id: copiedId, version: 0, name: storyName, projectName: PROJECT_NAME });

    await listPage.goto(PROJECT_NAME);
    expect(await listPage.countStoryRows(storyName)).toBeGreaterThanOrEqual(2);

    await page.close();
  });

  test('back button → returns to story list', async ({ adminContext, request }) => {
    const storyName = `e2e-story-back-${Date.now()}`;
    const story = await createStory(request, PROJECT_NAME, storyName);
    queueStoryCleanup(story);

    const page = await adminContext.newPage();
    const listPage = new StoryListPage(page);
    const editorPage = new StoryEditorPage(page);

    await listPage.goto(PROJECT_NAME);
    await listPage.clickStory(storyName);
    await editorPage.navigateBack(PROJECT_NAME);
    await listPage.expectStoryInTable(storyName);

    await page.close();
  });

});

test.describe('Story goals', () => {

  test('add goal → appears in table and persists after reload', async ({ adminContext, request }) => {
    const storyName = `e2e-story-addgoal-${Date.now()}`;
    const goalName = `e2e-story-goal-${Date.now()}`;
    const story = await createStory(request, PROJECT_NAME, storyName);
    const goal = await createGoal(request, PROJECT_NAME, goalName);
    queueStoryCleanup(story);
    queueGoalCleanup(goal);

    const page = await adminContext.newPage();
    const listPage = new StoryListPage(page);
    const editorPage = new StoryEditorPage(page);

    await listPage.goto(PROJECT_NAME);
    await listPage.clickStory(storyName);

    await editorPage.addGoal(goalName);
    await editorPage.expectGoalInTable(goalName);

    await reloadAndWaitForGet(page, r => /\/stories\/\d+$/.test(r.url()));
    await editorPage.expectGoalInTable(goalName);

    await page.close();
  });

  test('remove goal → gone after reload', async ({ adminContext, request }) => {
    const storyName = `e2e-story-rmgoal-${Date.now()}`;
    const goalName = `e2e-story-rmgoal-goal-${Date.now()}`;
    const story = await createStory(request, PROJECT_NAME, storyName);
    const goal = await createGoal(request, PROJECT_NAME, goalName);
    queueStoryCleanup(story);
    queueGoalCleanup(goal);

    const page = await adminContext.newPage();
    const listPage = new StoryListPage(page);
    const editorPage = new StoryEditorPage(page);

    await listPage.goto(PROJECT_NAME);
    await listPage.clickStory(storyName);

    await editorPage.addGoal(goalName);
    await editorPage.expectGoalInTable(goalName);
    await reloadAndWaitForGet(page, r => /\/stories\/\d+$/.test(r.url()));

    await editorPage.removeGoal(goalName);
    await editorPage.expectGoalNotInTable(goalName);
    await reloadAndWaitForGet(page, r => /\/stories\/\d+$/.test(r.url()));
    await editorPage.expectGoalNotInTable(goalName);

    await page.close();
  });

  test('click goal link → navigates to goal editor', async ({ adminContext, request }) => {
    const storyName = `e2e-story-goallink-${Date.now()}`;
    const goalName = `e2e-story-goallink-goal-${Date.now()}`;
    const story = await createStory(request, PROJECT_NAME, storyName);
    const goal = await createGoal(request, PROJECT_NAME, goalName);
    queueStoryCleanup(story);
    queueGoalCleanup(goal);

    const page = await adminContext.newPage();
    const listPage = new StoryListPage(page);
    const storyEditorPage = new StoryEditorPage(page);
    const goalEditorPage = new GoalEditorPage(page);

    await listPage.goto(PROJECT_NAME);
    await listPage.clickStory(storyName);

    await storyEditorPage.addGoal(goalName);
    await storyEditorPage.expectGoalInTable(goalName);
    await storyEditorPage.clickGoal(goalName);

    await page.waitForURL(/\/goals\/\d+$/);
    await goalEditorPage.expectNameValue(goalName);

    await page.close();
  });
});

test.describe('Story additional actors', () => {

  test('add additional actor → appears in table and persists after reload', async ({ adminContext, request }) => {
    const storyName = `e2e-story-addactor-${Date.now()}`;
    const actorName = `e2e-story-actor-${Date.now()}`;
    const story = await createStory(request, PROJECT_NAME, storyName);
    const actor = await createActor(request, PROJECT_NAME, actorName);
    queueStoryCleanup(story);
    queueActorCleanup(actor);

    const page = await adminContext.newPage();
    const listPage = new StoryListPage(page);
    const editorPage = new StoryEditorPage(page);

    await listPage.goto(PROJECT_NAME);
    await listPage.clickStory(storyName);

    await editorPage.addAdditionalActor(actorName);
    await editorPage.expectAdditionalActorInTable(actorName);

    // Reload to confirm the actor persisted
    await reloadAndWaitForGet(page, r => /\/stories\/\d+$/.test(r.url()));
    await editorPage.expectAdditionalActorInTable(actorName);

    await page.close();
  });

  test('remove additional actor → gone after removal', async ({ adminContext, request }) => {
    const storyName = `e2e-story-rmactor-${Date.now()}`;
    const actorName = `e2e-story-rmactor-actor-${Date.now()}`;
    const story = await createStory(request, PROJECT_NAME, storyName);
    const actor = await createActor(request, PROJECT_NAME, actorName);
    queueStoryCleanup(story);
    queueActorCleanup(actor);
    await addActorToStory(request, PROJECT_NAME, story.id, actor.id);

    const page = await adminContext.newPage();
    const listPage = new StoryListPage(page);
    const editorPage = new StoryEditorPage(page);

    await listPage.goto(PROJECT_NAME);
    await listPage.clickStory(storyName);

    await editorPage.expectAdditionalActorInTable(actorName);
    await editorPage.removeAdditionalActor(actorName);
    await editorPage.expectAdditionalActorNotInTable(actorName);

    await page.close();
  });

  test('click additional actor link → navigates to actor editor', async ({ adminContext, request }) => {
    const storyName = `e2e-story-actorlink-${Date.now()}`;
    const actorName = `e2e-story-actorlink-actor-${Date.now()}`;
    const story = await createStory(request, PROJECT_NAME, storyName);
    const actor = await createActor(request, PROJECT_NAME, actorName);
    queueStoryCleanup(story);
    queueActorCleanup(actor);

    const page = await adminContext.newPage();
    const listPage = new StoryListPage(page);
    const storyEditorPage = new StoryEditorPage(page);
    const actorEditorPage = new ActorEditorPage(page);

    await listPage.goto(PROJECT_NAME);
    await listPage.clickStory(storyName);

    await storyEditorPage.addAdditionalActor(actorName);
    await storyEditorPage.expectAdditionalActorInTable(actorName);
    await storyEditorPage.clickAdditionalActor(actorName);

    await page.waitForURL(/\/actors\/\d+$/);
    await actorEditorPage.expectNameValue(actorName);

    await page.close();
  });

});
