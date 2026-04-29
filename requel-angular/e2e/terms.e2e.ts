import { test, expect } from './fixtures/auth';
import { createProject, deleteProject, createTerm, deleteTerm, TermFixture } from './fixtures/api-helper';
import { TermListPage, TermEditorPage } from './pages/TermEditorPage';
import { reloadAndWaitForGet } from './helpers/navigation';

const PROJECT_NAME = `e2e-terms-${Date.now()}`;
let termToCleanup: TermFixture | null = null;

test.beforeAll(async ({ request }) => {
  await createProject(request, PROJECT_NAME, 'Terms E2E test project');
});

test.afterAll(async ({ request }) => {
  await deleteProject(request, PROJECT_NAME);
});

test.afterEach(async ({ request }) => {
  if (termToCleanup) {
    try { await deleteTerm(request, termToCleanup); } catch { /* may already be deleted by test */ }
    termToCleanup = null;
  }
});

test.describe('Glossary term management', () => {

  test('create term → appears in term list', async ({ adminContext }) => {
    const termName = `e2e-term-create-${Date.now()}`;
    const page = await adminContext.newPage();
    const listPage = new TermListPage(page);
    const editorPage = new TermEditorPage(page);

    await listPage.goto(PROJECT_NAME);
    await listPage.clickNewTerm();

    await editorPage.fillName(termName);
    await editorPage.fillText('A term created by the E2E test suite');
    // saveNew() waits for the URL to change — Angular uses replaceUrl:true which can
    // race with waitForResponse (waitForResponse is reserved for edit-in-place saves)
    await editorPage.saveNew();

    const url = page.url();
    const idMatch = url.match(/\/terms\/(\d+)/);
    if (idMatch) {
      termToCleanup = { id: parseInt(idMatch[1], 10), version: 0, name: termName, projectName: PROJECT_NAME };
    }

    await listPage.goto(PROJECT_NAME);
    await listPage.expectTermInTable(termName);

    await page.close();
  });

  test('edit term text → persists after save and page reload', async ({ adminContext, request }) => {
    const termName = `e2e-term-edit-${Date.now()}`;
    const newText = 'Updated definition from E2E test';
    const term = await createTerm(request, PROJECT_NAME, termName, 'Original definition');
    termToCleanup = term;

    const page = await adminContext.newPage();
    const listPage = new TermListPage(page);
    const editorPage = new TermEditorPage(page);

    await listPage.goto(PROJECT_NAME);
    await listPage.clickTerm(termName);

    await editorPage.fillText(newText);
    await editorPage.save();

    await reloadAndWaitForGet(page, r => /\/terms\/\d+$/.test(r.url()));
    await editorPage.expectTextValue(newText);

    await page.close();
  });

  test('delete term → removed from list', async ({ adminContext, request }) => {
    const termName = `e2e-term-delete-${Date.now()}`;
    await createTerm(request, PROJECT_NAME, termName, 'Term to be deleted');
    termToCleanup = null; // test deletes it

    const page = await adminContext.newPage();
    const listPage = new TermListPage(page);
    const editorPage = new TermEditorPage(page);

    await listPage.goto(PROJECT_NAME);
    await listPage.clickTerm(termName);

    await editorPage.delete();

    await listPage.expectTermNotInTable(termName);

    await page.close();
  });

});
