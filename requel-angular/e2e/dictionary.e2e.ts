import { test, expect } from './fixtures/auth';
import {
  createProject, deleteProject, addUserStakeholder, addProjectDictionaryWord,
  removeInstallDictionaryWordByLemma,
} from './fixtures/api-helper';
import { SidebarPage } from './pages/SidebarPage';
import { gotoAndWaitForGet } from './helpers/navigation';

/**
 * Issue #319: a project's Dictionary page (left nav, add, remove, read-only without
 * Project[Edit]) and the admin Installation Dictionary page.
 */
const nonce = Date.now();
const PROJECT_NAME = `e2e-dictionary-${nonce}`;
// Letters only: the words must be single tokens, and unlikely to be in any dictionary.
const suffix = nonce.toString(36).replace(/[0-9]/g, 'q');
const PROJECT_WORD = `requelproj${suffix}`;
const READONLY_WORD = `requelread${suffix}`;
const INSTALL_WORD = `requelwide${suffix}`;

test.beforeAll(async ({ request }) => {
  await createProject(request, PROJECT_NAME, 'Dictionary E2E test project');
  // The seeded "project" user can see the project but not edit it.
  await addUserStakeholder(request, PROJECT_NAME, 'project',
    ['com.rreganjr.requel.project.Goal[Edit]']);
  await addProjectDictionaryWord(request, PROJECT_NAME, READONLY_WORD);
});

test.afterAll(async ({ request }) => {
  await removeInstallDictionaryWordByLemma(request, INSTALL_WORD);
  await deleteProject(request, PROJECT_NAME);
});

test.describe('Project dictionary', () => {

  test('open from the left nav, add a word, remove it; the nav count follows', async ({ adminContext }) => {
    const page = await adminContext.newPage();
    const sidebar = new SidebarPage(page);
    await page.goto('/');
    await sidebar.expectProjectInTree(PROJECT_NAME);
    await sidebar.expandProject(PROJECT_NAME);
    await sidebar.expectEntityGroup(PROJECT_NAME, /^Dictionary \(1\)$/);

    const listLoaded = page.waitForResponse(r => r.request().method() === 'GET'
      && r.url().includes('/dictionary') && r.status() === 200);
    await sidebar.projectNode(PROJECT_NAME).getByRole('treeitem')
      .filter({ hasText: /^Dictionary \(\d+\)$/ }).first().click();
    await page.waitForURL(`**/projects/${encodeURIComponent(PROJECT_NAME)}/dictionary`);
    await listLoaded;

    await page.getByTestId('project-dictionary-word').fill(PROJECT_WORD);
    await page.getByTestId('project-dictionary-add').click();
    await expect(page.getByTestId('project-dictionary-lemma').filter({ hasText: PROJECT_WORD }))
      .toBeVisible();
    await expect(page.getByTestId('project-dictionary-word')).toHaveValue('');
    await sidebar.expectEntityGroup(PROJECT_NAME, /^Dictionary \(2\)$/);

    await page.getByRole('button', { name: `Remove ${PROJECT_WORD}` }).click();
    await page.getByRole('alertdialog').getByRole('button', { name: 'Remove' }).click();
    await expect(page.getByTestId('project-dictionary-lemma').filter({ hasText: PROJECT_WORD }))
      .toHaveCount(0);
    await sidebar.expectEntityGroup(PROJECT_NAME, /^Dictionary \(1\)$/);

    await page.close();
  });

  test('a word with a space is refused on the field', async ({ adminContext }) => {
    const page = await adminContext.newPage();
    await gotoAndWaitForGet(page, `/projects/${encodeURIComponent(PROJECT_NAME)}/dictionary`,
      r => r.url().includes('/dictionary'));
    await page.getByTestId('project-dictionary-word').fill('two words');
    await page.getByTestId('project-dictionary-add').click();
    await expect(page.getByTestId('project-dictionary-word-error'))
      .toHaveText('One word only, with no spaces.');
    await page.close();
  });

  test('a stakeholder without Project[Edit] sees the words but cannot change them', async ({ projectContext }) => {
    const page = await projectContext.newPage();
    await gotoAndWaitForGet(page, `/projects/${encodeURIComponent(PROJECT_NAME)}/dictionary`,
      r => r.url().includes('/dictionary'));
    await expect(page.getByTestId('project-dictionary-lemma').filter({ hasText: READONLY_WORD }))
      .toBeVisible();
    await expect(page.getByTestId('project-dictionary-add-form')).toHaveCount(0);
    await expect(page.getByTestId('project-dictionary-remove')).toHaveCount(0);
    await page.close();
  });
});

test.describe('Installation dictionary', () => {

  test('admin adds and removes an installation word from the admin menu', async ({ adminContext }) => {
    const page = await adminContext.newPage();
    await page.goto('/');
    const listLoaded = page.waitForResponse(r => r.request().method() === 'GET'
      && r.url().includes('/api/admin/dictionary') && r.status() === 200);
    await page.getByRole('link', { name: 'Manage the installation dictionary' }).click();
    await page.waitForURL('**/dictionary');
    await listLoaded;
    await expect(page.getByRole('heading', { name: 'Installation Dictionary' })).toBeVisible();

    await page.getByTestId('install-dictionary-word').fill(INSTALL_WORD);
    await page.getByTestId('install-dictionary-add').click();
    await expect(page.getByTestId('install-dictionary-lemma').filter({ hasText: INSTALL_WORD }))
      .toBeVisible();

    await page.getByRole('button', { name: `Remove ${INSTALL_WORD}` }).click();
    await page.getByRole('alertdialog').getByRole('button', { name: 'Remove' }).click();
    await expect(page.getByTestId('install-dictionary-lemma').filter({ hasText: INSTALL_WORD }))
      .toHaveCount(0);
    await page.close();
  });

  test('a project user has no admin link and is bounced from the page', async ({ projectContext }) => {
    const page = await projectContext.newPage();
    await page.goto('/dictionary');
    await expect(page).not.toHaveURL(/\/dictionary$/);
    await expect(page.getByRole('link', { name: 'Manage the installation dictionary' })).toHaveCount(0);
    await page.close();
  });
});
