import { test, expect } from './fixtures/auth';
import {
  createProject,
  deleteProject,
  createGoal,
  deleteGoal,
  createActor,
  deleteActor,
  deleteStory,
  GoalFixture,
  ActorFixture,
  StoryFixture,
} from './fixtures/api-helper';
import { GoalListPage, GoalEditorPage } from './pages/GoalEditorPage';
import { StoryListPage, StoryEditorPage } from './pages/StoryEditorPage';
import { gotoAndWaitForGet } from './helpers/navigation';

/**
 * End-to-end coverage for the entity-create wizard (issue #158).
 *
 * The unit suite covers `app-form-wizard` and `app-field` in isolation; this file is
 * about the two migrated flows against a live backend — in particular the things only
 * a real server can prove:
 *
 *  - Tags / Relations / Goals / Actors are reachable *during* create, which before
 *    #158 required saving a half-configured entity first.
 *  - Optional steps can be skipped and the entity is still complete and valid.
 *  - The optimistic-lock contract: stepping back to Details and committing a second
 *    time must send the refreshed version, not the one captured at create. Re-sending
 *    the create-time version is a guaranteed HTTP 409 from
 *    `EntityLockException.staleEntity`, so this is the regression test the whole
 *    version contract exists for.
 */

const PROJECT_NAME = `e2e-wizard-${Date.now()}`;

let relationTargetGoal: GoalFixture | null = null;
let actorFixture: ActorFixture | null = null;
let goalToCleanup: GoalFixture | null = null;
let storyToCleanup: StoryFixture | null = null;

test.beforeAll(async ({ request }) => {
  await createProject(request, PROJECT_NAME, 'Form wizard E2E test project');
  relationTargetGoal = await createGoal(
    request,
    PROJECT_NAME,
    `e2e-wizard-target-${Date.now()}`,
    'Relation target'
  );
  actorFixture = await createActor(request, PROJECT_NAME, `e2e-wizard-actor-${Date.now()}`);
});

test.afterAll(async ({ request }) => {
  if (relationTargetGoal) {
    try {
      await deleteGoal(request, relationTargetGoal);
    } catch {
      // project delete below will take it
    }
  }
  if (actorFixture) {
    try {
      await deleteActor(request, actorFixture);
    } catch {
      // project delete below will take it
    }
  }
  await deleteProject(request, PROJECT_NAME);
});

test.afterEach(async ({ request }) => {
  if (goalToCleanup) {
    try {
      await deleteGoal(request, goalToCleanup);
    } catch {
      // may already be deleted by the test
    }
    goalToCleanup = null;
  }
  if (storyToCleanup) {
    try {
      await deleteStory(request, storyToCleanup);
    } catch {
      // may already be deleted by the test
    }
    storyToCleanup = null;
  }
});

/** Pull the created entity's id out of the URL after the wizard finishes. */
function idFromUrl(url: string, segment: string): number {
  const match = url.match(new RegExp(`/${segment}/(\\d+)`));
  if (!match) {
    throw new Error(`No ${segment} id in URL: ${url}`);
  }
  return parseInt(match[1], 10);
}

test.describe('Goal create wizard', () => {
  test('walks all three steps, adding a tag-free relation before the first manual save', async ({
    adminContext,
  }) => {
    const goalName = `e2e-wizard-goal-${Date.now()}`;
    const page = await adminContext.newPage();
    const listPage = new GoalListPage(page);
    const editor = new GoalEditorPage(page);
    const wizard = editor.wizard;

    await listPage.goto(PROJECT_NAME);
    await listPage.clickNewGoal();

    // Step 1 — Details. The later steps are not reachable yet.
    await wizard.expectActiveStep('details');
    await wizard.expectStepLocked('tags');
    await wizard.expectStepLocked('relations');

    // Required-field gating: Continue stays disabled until Name has a value.
    await wizard.expectContinueDisabled();
    await editor.fillName(goalName);
    await editor.fillDescription('Created through the wizard');
    await editor.expectNameAccessiblyLabelled();
    await wizard.expectContinueEnabled();

    await editor.commitDetails();

    // Step 2 — Tags, now reachable because the goal exists. This is the behaviour the
    // old `@if (!isNew())` gate made impossible.
    await wizard.expectActiveStep('tags');
    await wizard.expectStepComplete('details');
    await wizard.expectStepUnlocked('relations');

    // Skip Tags entirely — an optional step must not block progress.
    await wizard.skipToStep('relations');

    // Step 3 — Relations, also reachable pre-first-manual-save.
    await editor.addRelation(relationTargetGoal!.name);
    await editor.expectRelationInTable(relationTargetGoal!.name);

    await wizard.expectContinueLabel('Done');
    await wizard.finish(/\/goals\/\d+/);

    goalToCleanup = {
      id: idFromUrl(page.url(), 'goals'),
      version: 0,
      name: goalName,
      projectName: PROJECT_NAME,
    };

    // The finished goal matches what the old single-form create produced, plus the
    // relation that used to require a second visit.
    await editor.expectNameValue(goalName);
    await editor.expectDescriptionValue('Created through the wizard');
    await editor.expectRelationInTable(relationTargetGoal!.name);

    await listPage.goto(PROJECT_NAME);
    await listPage.expectGoalInTable(goalName);

    await page.close();
  });

  test('stepping back to Details and committing again does not 409', async ({ adminContext }) => {
    // The regression the version contract exists for: the create-time version is spent
    // once the goal is saved, so a second commit must send the refreshed one.
    const goalName = `e2e-wizard-goal-revisit-${Date.now()}`;
    const revisedName = `${goalName}-revised`;
    const page = await adminContext.newPage();
    const listPage = new GoalListPage(page);
    const editor = new GoalEditorPage(page);
    const wizard = editor.wizard;

    await listPage.goto(PROJECT_NAME);
    await listPage.clickNewGoal();

    await editor.fillName(goalName);
    await editor.commitDetails();
    await wizard.expectActiveStep('tags');

    // Walk forward, then come back to fix the name.
    await wizard.skipToStep('relations');
    await wizard.gotoStep('details');

    await editor.fillName(revisedName);

    // Capture the raw response so a 409 fails loudly rather than as a stuck wizard.
    const [response] = await Promise.all([
      page.waitForResponse(r => r.url().includes('/api/commands/EditGoal')),
      wizard.continueButton().click(),
    ]);
    expect(
      response.status(),
      'second EditGoal should not be an optimistic-lock conflict'
    ).not.toBe(409);
    expect(response.ok()).toBe(true);

    // No stale-version alert, and the wizard advanced.
    await expect(wizard.error()).toHaveCount(0);
    await wizard.expectActiveStep('tags');

    await wizard.skipToStep('relations');
    await wizard.finish(/\/goals\/\d+/);

    goalToCleanup = {
      id: idFromUrl(page.url(), 'goals'),
      version: 0,
      name: revisedName,
      projectName: PROJECT_NAME,
    };

    await editor.expectNameValue(revisedName);

    await page.close();
  });

  test('cancel on an optional step leaves the already-created goal intact', async ({
    adminContext,
  }) => {
    // Documented consequence of commit-on-step-1: abandoning the wizard after Details
    // leaves a saved, valid, named goal rather than nothing.
    const goalName = `e2e-wizard-goal-abandon-${Date.now()}`;
    const page = await adminContext.newPage();
    const listPage = new GoalListPage(page);
    const editor = new GoalEditorPage(page);

    await listPage.goto(PROJECT_NAME);
    await listPage.clickNewGoal();

    await editor.fillName(goalName);
    await editor.commitDetails();
    await editor.wizard.expectActiveStep('tags');

    await editor.wizard.cancel(`**/projects/${encodeURIComponent(PROJECT_NAME)}/goals`);

    await listPage.expectGoalInTable(goalName);
    await listPage.clickGoal(goalName);
    goalToCleanup = {
      id: idFromUrl(page.url(), 'goals'),
      version: 0,
      name: goalName,
      projectName: PROJECT_NAME,
    };
    await editor.expectNameValue(goalName);

    await page.close();
  });

  test('the step nav is keyboard operable', async ({ adminContext }) => {
    const page = await adminContext.newPage();
    const listPage = new GoalListPage(page);
    const editor = new GoalEditorPage(page);
    const wizard = editor.wizard;

    await listPage.goto(PROJECT_NAME);
    await listPage.clickNewGoal();

    await wizard.focusStep('details');
    await expect(wizard.stepButton('details')).toBeFocused();

    // Arrow keys move focus only — the active step must not change.
    await wizard.pressInNav('ArrowDown');
    await expect(wizard.stepButton('tags')).toBeFocused();
    await wizard.expectActiveStep('details');

    await wizard.pressInNav('End');
    await expect(wizard.stepButton('relations')).toBeFocused();

    await wizard.pressInNav('Home');
    await expect(wizard.stepButton('details')).toBeFocused();
    await wizard.expectActiveStep('details');

    await page.close();
  });
});

test.describe('Story create wizard', () => {
  test('walks all three steps, attaching a goal and skipping the actors step', async ({
    adminContext,
  }) => {
    const storyName = `e2e-wizard-story-${Date.now()}`;
    const page = await adminContext.newPage();
    const listPage = new StoryListPage(page);
    const editor = new StoryEditorPage(page);
    const wizard = editor.wizard;

    await listPage.goto(PROJECT_NAME);
    await listPage.clickNewStory();

    await wizard.expectActiveStep('details');
    await wizard.expectStepLocked('goals');
    await wizard.expectStepLocked('actors');

    await wizard.expectContinueDisabled();

    // All four controls live on step 1, including the two p-selects — the reason Story
    // is a pilot at all. Primary Actor stays here because it is a scalar property of
    // the story, not an association.
    await editor.fillName(storyName);
    await editor.fillText('Created through the wizard');
    await editor.selectStoryType('Exception');
    await editor.selectPrimaryActor(actorFixture!.name);
    await editor.expectSelectsAccessiblyLabelled();

    await wizard.expectContinueEnabled();
    await editor.commitDetails();

    // Step 2 — Goals, reachable during create.
    await wizard.expectActiveStep('goals');
    await wizard.expectStepComplete('details');
    await editor.addGoal(relationTargetGoal!.name);
    await editor.expectGoalInTable(relationTargetGoal!.name);

    // Step 3 — skip Additional Actors entirely.
    await wizard.skipToStep('actors');
    await wizard.expectContinueLabel('Done');
    await wizard.finish(/\/stories\/\d+/);

    storyToCleanup = {
      id: idFromUrl(page.url(), 'stories'),
      version: 0,
      name: storyName,
      projectName: PROJECT_NAME,
    };

    // Everything set through the wizard survived, including the two select values.
    await editor.expectNameValue(storyName);
    await editor.expectTextValue('Created through the wizard');
    await editor.expectStoryTypeValue('Exception');
    await editor.expectPrimaryActorValue(actorFixture!.name);
    await editor.expectGoalInTable(relationTargetGoal!.name);
    await editor.expectAdditionalActorNotInTable(actorFixture!.name);

    await listPage.goto(PROJECT_NAME);
    await listPage.expectStoryInTable(storyName);

    await page.close();
  });

  test('stepping back to Details and committing again does not 409', async ({ adminContext }) => {
    const storyName = `e2e-wizard-story-revisit-${Date.now()}`;
    const revisedName = `${storyName}-revised`;
    const page = await adminContext.newPage();
    const listPage = new StoryListPage(page);
    const editor = new StoryEditorPage(page);
    const wizard = editor.wizard;

    await listPage.goto(PROJECT_NAME);
    await listPage.clickNewStory();

    await editor.fillName(storyName);
    await editor.commitDetails();
    await wizard.expectActiveStep('goals');

    await wizard.skipToStep('actors');
    await wizard.gotoStep('details');
    await editor.fillName(revisedName);

    const [response] = await Promise.all([
      page.waitForResponse(r => r.url().includes('/api/commands/EditStory')),
      wizard.continueButton().click(),
    ]);
    expect(
      response.status(),
      'second EditStory should not be an optimistic-lock conflict'
    ).not.toBe(409);
    expect(response.ok()).toBe(true);

    await expect(wizard.error()).toHaveCount(0);
    await wizard.expectActiveStep('goals');

    await wizard.skipToStep('actors');
    await wizard.finish(/\/stories\/\d+/);

    storyToCleanup = {
      id: idFromUrl(page.url(), 'stories'),
      version: 0,
      name: revisedName,
      projectName: PROJECT_NAME,
    };

    await editor.expectNameValue(revisedName);

    await page.close();
  });
});

test.describe('Edit forms after the migration', () => {
  test('goal edit renders app-field rows and gates Save on a real change', async ({
    adminContext,
    request,
  }) => {
    const goal = await createGoal(
      request,
      PROJECT_NAME,
      `e2e-wizard-edit-goal-${Date.now()}`,
      'Edit me'
    );
    goalToCleanup = goal;

    const page = await adminContext.newPage();
    const editor = new GoalEditorPage(page);
    // Wait for the goal GET, not just document load. Everything asserted below the wizard
    // check is also true of an empty form, so a bare page.goto() lets this test type into the
    // input before the fetch lands and then have the load reset it out from under us - Save
    // stays disabled and the failure reads as a Save-gating bug rather than a race.
    await gotoAndWaitForGet(
      page,
      `/projects/${encodeURIComponent(PROJECT_NAME)}/goals/${goal.id}`,
      r => /\/goals\/\d+$/.test(r.url())
    );
    await editor.expectNameValue(goal.name);

    // No wizard chrome on the edit route.
    await expect(editor.wizard.root()).toHaveCount(0);
    await expect(page.locator('app-field')).toHaveCount(2);

    // Save is disabled until something actually changes, and again after saving.
    await expect(editor.saveButton()).toBeDisabled();
    await editor.fillName(`${goal.name}-renamed`);
    await expect(editor.saveButton()).toBeEnabled();

    await editor.save();
    await expect(editor.saveButton()).toBeDisabled();
    goalToCleanup = { ...goal, name: `${goal.name}-renamed` };

    // Clearing a required field surfaces the shared form-errors message and blocks Save.
    await editor.fillName('');
    await editor.nameBlur();
    await expect(editor.fieldError()).toContainText('A goal needs a name.');
    await expect(editor.saveButton()).toBeDisabled();

    await page.close();
  });

  test('story edit renders all four rows with labelled selects', async ({
    adminContext,
    request,
  }) => {
    const goal = await createGoal(
      request,
      PROJECT_NAME,
      `e2e-wizard-edit-story-goal-${Date.now()}`,
      'x'
    );
    goalToCleanup = goal;

    const page = await adminContext.newPage();
    const listPage = new StoryListPage(page);
    const editor = new StoryEditorPage(page);

    // Create through the wizard's fast path, then reopen on the edit route.
    const storyName = `e2e-wizard-edit-story-${Date.now()}`;
    await listPage.goto(PROJECT_NAME);
    await listPage.clickNewStory();
    await editor.fillName(storyName);
    await editor.save();
    storyToCleanup = {
      id: idFromUrl(page.url(), 'stories'),
      version: 0,
      name: storyName,
      projectName: PROJECT_NAME,
    };

    await expect(editor.wizard.root()).toHaveCount(0);
    await expect(page.locator('app-field')).toHaveCount(4);
    await editor.expectSelectsAccessiblyLabelled();

    await expect(editor.saveButton()).toBeDisabled();
    await editor.fillText('Now with text');
    await expect(editor.saveButton()).toBeEnabled();
    await editor.save();
    await editor.expectTextValue('Now with text');

    await page.close();
  });
});
