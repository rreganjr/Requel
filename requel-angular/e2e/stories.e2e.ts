import { test, expect } from './fixtures/auth';
import { createProject, deleteProject, createActor, createStory, deleteStory, getStoryVersion, StoryFixture } from './fixtures/api-helper';
import { StoryListPage, StoryEditorPage } from './pages/StoryEditorPage';

const PROJECT_NAME = `e2e-stories-${Date.now()}`;
const ACTOR_NAME = 'E2E Story Actor';
let storyToCleanup: StoryFixture | null = null;

test.beforeAll(async ({ request }) => {
  await createProject(request, PROJECT_NAME, 'Stories E2E test project');
  await createActor(request, PROJECT_NAME, ACTOR_NAME);
});

test.afterAll(async ({ request }) => {
  await deleteProject(request, PROJECT_NAME);
});

test.afterEach(async ({ request }) => {
  if (storyToCleanup) {
    try {
      const version = await getStoryVersion(request, storyToCleanup);
      await deleteStory(request, { ...storyToCleanup, version });
    } catch {
      // may already be deleted by the test
    }
    storyToCleanup = null;
  }
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

    const url = page.url();
    const idMatch = url.match(/\/stories\/(\d+)/);
    if (idMatch) {
      storyToCleanup = { id: parseInt(idMatch[1], 10), version: 0, name: storyName, projectName: PROJECT_NAME };
    }

    await listPage.goto(PROJECT_NAME);
    await listPage.expectStoryInTable(storyName);

    await page.close();
  });

  test('change story type → persists after save and reload', async ({ adminContext, request }) => {
    const storyName = `e2e-story-type-${Date.now()}`;
    const story = await createStory(request, PROJECT_NAME, storyName, 'Success');
    storyToCleanup = story;

    const page = await adminContext.newPage();
    const listPage = new StoryListPage(page);
    const editorPage = new StoryEditorPage(page);

    await listPage.goto(PROJECT_NAME);
    await listPage.clickStory(storyName);

    await editorPage.selectStoryType('Exception');
    await editorPage.save();

    await page.reload();
    await page.waitForLoadState('domcontentloaded');
    await editorPage.expectStoryTypeValue('Exception');

    await page.close();
  });

  test('delete story → removed from list', async ({ adminContext, request }) => {
    const storyName = `e2e-story-delete-${Date.now()}`;
    await createStory(request, PROJECT_NAME, storyName);
    storyToCleanup = null;

    const page = await adminContext.newPage();
    const listPage = new StoryListPage(page);
    const editorPage = new StoryEditorPage(page);

    await listPage.goto(PROJECT_NAME);
    await listPage.clickStory(storyName);

    await editorPage.delete();

    await page.waitForURL(/\/stories$/);
    await listPage.expectStoryNotInTable(storyName);

    await page.close();
  });

  test('set primary actor → actor name persists after save and reload', async ({ adminContext, request }) => {
    const storyName = `e2e-story-actor-set-${Date.now()}`;
    const story = await createStory(request, PROJECT_NAME, storyName);
    storyToCleanup = story;

    const page = await adminContext.newPage();
    const listPage = new StoryListPage(page);
    const editorPage = new StoryEditorPage(page);

    await listPage.goto(PROJECT_NAME);
    await listPage.clickStory(storyName);

    await editorPage.selectPrimaryActor(ACTOR_NAME);
    await editorPage.save();

    const storyLoaded = page.waitForResponse(
      r => r.url().includes('/stories/') && r.status() === 200 && r.request().method() === 'GET'
    );
    await page.reload({ waitUntil: 'domcontentloaded' });
    await storyLoaded;

    await editorPage.expectPrimaryActorValue(ACTOR_NAME);

    await page.close();
  });

  test('clear primary actor → placeholder shown after save and reload', async ({ adminContext, request }) => {
    const storyName = `e2e-story-actor-clear-${Date.now()}`;
    const story = await createStory(request, PROJECT_NAME, storyName, 'Success', '', ACTOR_NAME);
    storyToCleanup = story;

    const page = await adminContext.newPage();
    const listPage = new StoryListPage(page);
    const editorPage = new StoryEditorPage(page);

    await listPage.goto(PROJECT_NAME);
    await listPage.clickStory(storyName);

    // Verify actor is set before clearing
    await editorPage.expectPrimaryActorValue(ACTOR_NAME);

    await editorPage.clearPrimaryActor();
    await editorPage.save();

    const storyLoaded = page.waitForResponse(
      r => r.url().includes('/stories/') && r.status() === 200 && r.request().method() === 'GET'
    );
    await page.reload({ waitUntil: 'domcontentloaded' });
    await storyLoaded;

    await editorPage.expectNoPrimaryActor();

    await page.close();
  });

  test('rename story → name persists after save and reload', async ({ adminContext, request }) => {
    const originalName = `e2e-story-rename-${Date.now()}`;
    const newName = `${originalName}-renamed`;
    const story = await createStory(request, PROJECT_NAME, originalName);
    storyToCleanup = { ...story, name: newName };

    const page = await adminContext.newPage();
    const listPage = new StoryListPage(page);
    const editorPage = new StoryEditorPage(page);

    await listPage.goto(PROJECT_NAME);
    await listPage.clickStory(originalName);

    await editorPage.fillName(newName);
    await editorPage.save();

    await page.reload();
    await page.waitForLoadState('domcontentloaded');
    await editorPage.expectNameValue(newName);

    await page.close();
  });

});
