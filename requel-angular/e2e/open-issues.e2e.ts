import { test, expect } from './fixtures/auth';
import {
  createProject, deleteProject,
  createGoal, deleteGoal, GoalFixture,
  createStory, deleteStory, getStoryVersion, StoryFixture,
  createActor, deleteActor, getActorVersion, ActorFixture,
  addIssue,
} from './fixtures/api-helper';

const PROJECT_NAME = `e2e-open-issues-${Date.now()}`;

let goalToCleanup: GoalFixture | null = null;
let storyToCleanup: StoryFixture | null = null;
let actorToCleanup: ActorFixture | null = null;

test.beforeAll(async ({ request }) => {
  await createProject(request, PROJECT_NAME, 'Open issues E2E test project');
});

test.afterAll(async ({ request }) => {
  await deleteProject(request, PROJECT_NAME);
});

test.afterEach(async ({ request }) => {
  if (goalToCleanup) {
    try {
      await deleteGoal(request, goalToCleanup);
    } catch {
      // ignore
    }
    goalToCleanup = null;
  }

  if (storyToCleanup) {
    try {
      const version = await getStoryVersion(request, storyToCleanup);
      await deleteStory(request, { ...storyToCleanup, version });
    } catch {
      // ignore
    }
    storyToCleanup = null;
  }

  if (actorToCleanup) {
    try {
      const version = await getActorVersion(request, actorToCleanup);
      await deleteActor(request, { ...actorToCleanup, version });
    } catch {
      // ignore
    }
    actorToCleanup = null;
  }
});

test.describe('Open Issues', () => {

  test('no open issues shows empty-state message and no badge', async ({ adminContext }) => {
    const page = await adminContext.newPage();

    await Promise.all([
      page.waitForResponse(r => r.url().includes(`/api/projects/${encodeURIComponent(PROJECT_NAME)}/open-issues`) && r.status() === 200),
      page.goto(`/projects/${encodeURIComponent(PROJECT_NAME)}/open-issues`),
    ]);

    await expect(page.getByTestId('open-issues-empty')).toContainText('No open issues');
    await expect(page.getByTestId('open-issues-badge')).toHaveCount(0);

    await page.close();
  });

  test('mixed required and optional issues show badge count and row markers', async ({ adminContext, request }) => {
    const requiredIssueText = `Required issue ${Date.now()}`;
    const optionalIssueText = `Optional issue ${Date.now()}`;

    goalToCleanup = await createGoal(request, PROJECT_NAME, `e2e-open-goal-${Date.now()}`);
    storyToCleanup = await createStory(request, PROJECT_NAME, `e2e-open-story-${Date.now()}`);

    await addIssue(request, PROJECT_NAME, 'Goal', goalToCleanup.id, requiredIssueText, true);
    await addIssue(request, PROJECT_NAME, 'Story', storyToCleanup.id, optionalIssueText, false);

    const page = await adminContext.newPage();

    await Promise.all([
      page.waitForResponse(r => r.url().includes(`/api/projects/${encodeURIComponent(PROJECT_NAME)}/open-issues`) && r.status() === 200),
      page.goto(`/projects/${encodeURIComponent(PROJECT_NAME)}/open-issues`),
    ]);

    await expect(page.getByTestId('open-issues-badge')).toContainText('1');

    const requiredRow = page.getByRole('row', { name: new RegExp(requiredIssueText) });
    await expect(requiredRow.getByTestId('open-issue-required')).toContainText('Yes');

    const optionalRow = page.getByRole('row', { name: new RegExp(optionalIssueText) });
    await expect(optionalRow.getByTestId('open-issue-optional')).toBeVisible();

    await page.close();
  });

  test('clicking issue entity link navigates to actor and story editors', async ({ adminContext, request }) => {
    const actorIssueText = `Actor issue ${Date.now()}`;
    const storyIssueText = `Story issue ${Date.now()}`;

    actorToCleanup = await createActor(request, PROJECT_NAME, `e2e-open-actor-${Date.now()}`);
    storyToCleanup = await createStory(request, PROJECT_NAME, `e2e-open-story-nav-${Date.now()}`);

    await addIssue(request, PROJECT_NAME, 'Actor', actorToCleanup.id, actorIssueText, true);
    await addIssue(request, PROJECT_NAME, 'Story', storyToCleanup.id, storyIssueText, false);

    const page = await adminContext.newPage();

    await Promise.all([
      page.waitForResponse(r => r.url().includes(`/api/projects/${encodeURIComponent(PROJECT_NAME)}/open-issues`) && r.status() === 200),
      page.goto(`/projects/${encodeURIComponent(PROJECT_NAME)}/open-issues`),
    ]);

    const actorRow = page.getByRole('row', { name: new RegExp(actorIssueText) });
    await actorRow.getByTestId('open-issue-entity-link').click();
    await page.waitForURL(new RegExp(`/projects/${encodeURIComponent(PROJECT_NAME)}/actors/${actorToCleanup.id}$`));
    await expect(page.locator('#name')).not.toHaveValue('');

    await Promise.all([
      page.goBack(),
      page.waitForResponse(r => r.url().includes(`/api/projects/${encodeURIComponent(PROJECT_NAME)}/open-issues`) && r.status() === 200),
      page.waitForURL(new RegExp(`/projects/${encodeURIComponent(PROJECT_NAME)}/open-issues$`)),
    ]);

    const storyRow = page.getByRole('row', { name: new RegExp(storyIssueText) });
    await storyRow.getByTestId('open-issue-entity-link').click();
    await page.waitForURL(new RegExp(`/projects/${encodeURIComponent(PROJECT_NAME)}/stories/${storyToCleanup.id}$`));
    // Since #158 the story form is app-field rows with generated ids — locate by testid.
    await expect(page.getByTestId('story-name')).not.toHaveValue('');

    await page.close();
  });

  test('load failure shows error banner', async ({ adminContext }) => {
    const page = await adminContext.newPage();

    await page.route(`**/api/projects/${encodeURIComponent(PROJECT_NAME)}/open-issues`, async route => {
      await route.fulfill({
        status: 500,
        contentType: 'application/json',
        body: JSON.stringify({
          error: 'INTERNAL_ERROR',
          message: 'boom',
        }),
      });
    });

    await page.goto(`/projects/${encodeURIComponent(PROJECT_NAME)}/open-issues`);
    await expect(page.getByTestId('open-issues-error')).toContainText('Failed to load open issues.');

    await page.close();
  });

});
