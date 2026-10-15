import { test, expect } from './fixtures/auth';
import { createProject, deleteProject, addUserStakeholder, createUser } from './fixtures/api-helper';

/**
 * #390: a project member who holds Stakeholder Edit but isn't an admin manages stakeholders.
 *
 * - The editor used the admin-only user list, so they got "Failed to load users." and an empty
 *   user picker; it now uses the project's stakeholder candidates.
 * - After Save, every permission box stayed disabled until a reload (overlapping permission
 *   refreshes); the boxes they may change must stay enabled.
 */
const nonce = Date.now();
const PROJECT_NAME = `e2e-stakeholders-${nonce}`;
const CANDIDATE = `e2e-cand-${nonce}`;
const OTHER = `e2e-other-${nonce}`;
const GOAL_EDIT = 'com.rreganjr.requel.project.Goal[Edit]';
const GOAL_GRANT = 'com.rreganjr.requel.project.Goal[Grant]';
const STORY_EDIT = 'com.rreganjr.requel.project.Story[Edit]';
let otherId: number;

test.beforeAll(async ({ request }) => {
  await createProject(request, PROJECT_NAME, 'Stakeholder editor E2E project');
  await createUser(request, CANDIDATE, `Candidate ${nonce}`, 'E2e-cand-pw-1');
  await createUser(request, OTHER, `Other ${nonce}`, 'E2e-other-pw-1');
  // The seeded "project" user manages stakeholders and may grant Goal permissions only.
  await addUserStakeholder(request, PROJECT_NAME, 'project',
    [GOAL_EDIT, GOAL_GRANT, 'com.rreganjr.requel.project.Stakeholder[Edit]']);
  otherId = await addUserStakeholder(request, PROJECT_NAME, OTHER, [STORY_EDIT]);
});

test.afterAll(async ({ request }) => {
  await deleteProject(request, PROJECT_NAME);
});

test.describe('Stakeholder editor for a non-admin with Stakeholder Edit (#390)', () => {

  test('Add User offers the project\'s candidates, with no load error', async ({ projectContext }) => {
    const page = await projectContext.newPage();
    const candidates = page.waitForResponse(r =>
      r.url().includes('/stakeholder-candidates') && r.request().method() === 'GET');
    await page.goto(`/projects/${encodeURIComponent(PROJECT_NAME)}/stakeholders/new-user`);
    expect((await candidates).status()).toBe(200);
    await expect(page.getByText('Failed to load users.')).toHaveCount(0);

    await page.getByTestId('stakeholder-user').click();
    await expect(page.getByRole('option', { name: `Candidate ${nonce}` })).toBeVisible();
    // Existing stakeholders aren't offered again.
    await expect(page.getByRole('option', { name: `Other ${nonce}` })).toHaveCount(0);
    await page.close();
  });

  test('editing a stakeholder: no load error, and the grantable boxes stay enabled after Save',
    async ({ projectContext }) => {
      const page = await projectContext.newPage();
      await page.goto(`/projects/${encodeURIComponent(PROJECT_NAME)}/stakeholders/${otherId}`);
      const goalEdit = page.locator(`[id="perm-${GOAL_EDIT}"]`);
      const storyEdit = page.locator(`[id="perm-${STORY_EDIT}"]`);
      await expect(goalEdit).toBeEnabled();
      await expect(storyEdit).toBeDisabled();
      await expect(page.getByText('Failed to load users.')).toHaveCount(0);

      await goalEdit.check();
      await page.getByTestId('stakeholder-save').click();
      await expect(page.getByText('Stakeholder saved.')).toBeVisible();

      await expect(goalEdit).toBeEnabled();
      await expect(goalEdit).toBeChecked();
      await expect(storyEdit).toBeChecked();
      await page.close();
    });
});
