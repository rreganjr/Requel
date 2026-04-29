import { test, expect } from './fixtures/auth';
import {
  createProject, deleteProject,
  createGoal, deleteGoal,
  addIssue, addPosition,
  GoalFixture,
} from './fixtures/api-helper';
import { GoalListPage } from './pages/GoalEditorPage';

// All annotation tests share one project; each test gets its own goal for isolation.
// Deleting the goal cascades to its annotations, so no per-annotation cleanup is needed.
// NOTE: the NLP assistant auto-creates lexical issues on every new goal, so locators must
// be scoped by the specific text the test creates — never rely on being the only annotation.
const PROJECT_NAME = `e2e-annotations-${Date.now()}`;
let goalFixture: GoalFixture | null = null;

test.beforeAll(async ({ request }) => {
  await createProject(request, PROJECT_NAME, 'Annotations E2E project');
});

test.afterAll(async ({ request }) => {
  await deleteProject(request, PROJECT_NAME);
});

test.afterEach(async ({ request }) => {
  if (goalFixture) {
    try { await deleteGoal(request, goalFixture); } catch { /* best-effort */ }
    goalFixture = null;
  }
});

test.describe('Annotations (IBIS)', () => {

  test('add issue to goal → appears in annotations section', async ({ adminContext, request }) => {
    const issueText = 'This is a test issue';
    goalFixture = await createGoal(request, PROJECT_NAME, `e2e-ann-issue-${Date.now()}`);

    const page = await adminContext.newPage();
    const listPage = new GoalListPage(page);
    await listPage.goto(PROJECT_NAME);
    await listPage.clickGoal(goalFixture.name);

    const annotations = page.getByTestId('annotations-section');
    await expect(annotations).toBeVisible();
    await page.getByTestId('annotation-add-issue').click();
    await page.getByTestId('annotation-issue-text').fill(issueText);

    await Promise.all([
      page.waitForResponse(r => r.url().includes('/api/commands/EditIssue')),
      page.getByTestId('annotation-save-issue').click(),
    ]);

    // Scope to the specific issue — the NLP assistant adds its own issues automatically
    const issueItem = page.getByTestId('annotation-issue').filter({ hasText: issueText });
    await expect(issueItem).toContainText(issueText);
    await expect(issueItem.getByTestId('annotation-issue-badge')).toContainText('Issue');

    await page.close();
  });

  test('add position to issue → nested under issue', async ({ adminContext, request }) => {
    const issueText = 'Issue needing a position';
    const positionText = 'This is a test position';
    goalFixture = await createGoal(request, PROJECT_NAME, `e2e-ann-pos-${Date.now()}`);
    await addIssue(request, PROJECT_NAME, 'Goal', goalFixture.id, issueText);

    const page = await adminContext.newPage();
    const listPage = new GoalListPage(page);
    await listPage.goto(PROJECT_NAME);
    await listPage.clickGoal(goalFixture.name);

    // Scope to our issue — NLP issues are also present
    const issueItem = page.getByTestId('annotation-issue').filter({ hasText: issueText });
    await expect(issueItem).toBeVisible({ timeout: 5000 });

    await issueItem.getByRole('button', { name: 'Add Position' }).click();
    await issueItem.getByTestId('annotation-position-text').fill(positionText);

    await Promise.all([
      page.waitForResponse(r => r.url().includes('/api/commands/EditPosition')),
      issueItem.getByTestId('annotation-save-position').click(),
    ]);

    const positionItem = issueItem.getByTestId('annotation-position').filter({ hasText: positionText });
    await expect(positionItem).toContainText(positionText);
    await expect(positionItem.getByTestId('annotation-position-badge')).toContainText('Position');

    await page.close();
  });

  test('add argument to position → nested under position', async ({ adminContext, request }) => {
    const issueText = 'Issue with position';
    const positionText = 'Position needing an argument';
    const argText = 'This supports the position';
    goalFixture = await createGoal(request, PROJECT_NAME, `e2e-ann-arg-${Date.now()}`);
    const issue = await addIssue(request, PROJECT_NAME, 'Goal', goalFixture.id, issueText);
    await addPosition(request, PROJECT_NAME, issue.id, positionText);

    const page = await adminContext.newPage();
    const listPage = new GoalListPage(page);
    await listPage.goto(PROJECT_NAME);
    await listPage.clickGoal(goalFixture.name);

    // Scope to our issue, then our position within it
    const issueItem = page.getByTestId('annotation-issue').filter({ hasText: issueText });
    const positionItem = issueItem.getByTestId('annotation-position').filter({ hasText: positionText });
    await expect(positionItem).toBeVisible({ timeout: 5000 });

    await positionItem.getByRole('button', { name: 'Add Argument' }).click();
    await positionItem.getByTestId('annotation-argument-text').fill(argText);
    // support level defaults to 'For' — leave as-is

    await Promise.all([
      page.waitForResponse(r => r.url().includes('/api/commands/EditArgument')),
      positionItem.getByTestId('annotation-save-argument').click(),
    ]);

    await expect(positionItem.getByTestId('annotation-argument').filter({ hasText: argText })).toBeVisible();

    await page.close();
  });

  test('resolve issue → status shows Resolved', async ({ adminContext, request }) => {
    const issueText = 'Issue to resolve';
    const positionText = 'Proposed resolution';
    goalFixture = await createGoal(request, PROJECT_NAME, `e2e-ann-resolve-${Date.now()}`);
    const issue = await addIssue(request, PROJECT_NAME, 'Goal', goalFixture.id, issueText);
    await addPosition(request, PROJECT_NAME, issue.id, positionText);

    const page = await adminContext.newPage();
    const listPage = new GoalListPage(page);
    await listPage.goto(PROJECT_NAME);
    await listPage.clickGoal(goalFixture.name);

    const issueItem = page.getByTestId('annotation-issue').filter({ hasText: issueText });
    const positionItem = issueItem.getByTestId('annotation-position').filter({ hasText: positionText });
    await expect(positionItem).toBeVisible({ timeout: 5000 });

    // Generic positions (PositionImpl.getSimpleName()) fall through to default 'Ignore' label
    await Promise.all([
      page.waitForResponse(r => r.url().includes('/api/commands/ResolveIssue')),
      positionItem.getByTestId('annotation-resolve-issue').click(),
    ]);

    // After resolution the issue item gains the .resolved class
    const resolvedIssue = page.locator('[data-testid="annotation-issue"][data-resolved="true"]', { hasText: issueText });
    await expect(resolvedIssue).toBeVisible();
    await expect(
      resolvedIssue.getByTestId('annotation-issue-badge')
    ).toContainText('Resolved');

    await page.close();
  });

  test('open-issues page shows unresolved issue; click navigates to goal', async ({ adminContext, request }) => {
    const issueText = 'An unresolved open issue';
    goalFixture = await createGoal(request, PROJECT_NAME, `e2e-ann-open-${Date.now()}`);
    await addIssue(request, PROJECT_NAME, 'Goal', goalFixture.id, issueText);

    const page = await adminContext.newPage();
    await Promise.all([
      page.waitForResponse(r => r.url().includes('/open-issues') && r.status() === 200),
      page.goto(`/projects/${encodeURIComponent(PROJECT_NAME)}/open-issues`),
    ]);

    // Get the table row that contains our specific issue text, then click its entity link
    const issueRow = page.getByRole('row', { name: new RegExp(issueText) });
    await expect(issueRow).toBeVisible();
    await issueRow.getByTestId('open-issue-entity-link').click();

    await page.waitForURL(new RegExp(`/goals/${goalFixture.id}$`));
    await expect(page.locator('#name')).not.toHaveValue('');

    await page.close();
  });

});
