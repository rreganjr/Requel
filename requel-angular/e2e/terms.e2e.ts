import { test, expect } from './fixtures/auth';
import {
  createProject, deleteProject, createTerm, deleteTerm, getTermDetail, TermFixture
} from './fixtures/api-helper';
import { TermListPage, TermEditorPage } from './pages/TermEditorPage';
import { reloadAndWaitForGet, gotoAndWaitForGet } from './helpers/navigation';

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

  test('save with empty name shows validation error and does not save', async ({ adminContext }) => {
    // Since #132 this asserts the *user-visible* guard rather than onSave()'s early
    // return, because that return is no longer reachable from the UI: Save is disabled
    // while the form is invalid, and Enter does not submit either — implicit submission
    // goes through the form's default button, which is that same disabled Save. The
    // early return survives as defence in depth and is covered by term-editor.spec.ts.
    //
    // What the user sees instead: a `required` validator rendering INLINE under the Term
    // row via app-field, with wording from the shared map in form-errors.ts ('This field
    // is required.', not the old bespoke 'Term name is required.'). The page-level
    // <p-message data-testid="term-error"> is now reserved for failures with no field to
    // attach to. No fixture is needed because no term is created.
    const page = await adminContext.newPage();
    const listPage = new TermListPage(page);
    const editorPage = new TermEditorPage(page);

    await listPage.goto(PROJECT_NAME);
    await listPage.clickNewTerm();

    // Definition only — leave the name blank.
    await editorPage.fillText('A term with no name.');

    // Touching the empty Term field is what surfaces its error — app-field deliberately
    // does not shout at a create form nobody has filled in yet.
    const nameInput = page.locator('#name');
    await nameInput.click();
    await nameInput.blur();

    await editorPage.expectFieldError('This field is required.');
    await expect(page.getByTestId('term-save').locator('button')).toBeDisabled();

    // Confirm we stayed on /terms/new (no URL change) — proves the save was blocked
    // before the API was invoked.
    expect(page.url()).toMatch(/\/terms\/new$/);

    await page.close();
  });

  test('alternate terms: term with canonical pointer appears in canonical term editor and link navigates', async ({ adminContext, request }) => {
    // Coverage targets in term-editor.ts:
    //   - the @if (!isNew() && term()?.alternateTerms?.length) section render
    //   - the (click)="navigateToTerm(a.id)" handler on alternate rows
    //   - navigateToTerm() itself
    // Setup: term A (canonical), term B with canonicalTermId=A. Open A, expect B
    // to appear under "Alternate Terms", click it, assert /terms/<bId>.
    const canonicalName = `e2e-term-canonical-${Date.now()}`;
    const alternateName = `e2e-term-alternate-${Date.now()}`;
    const canonical = await createTerm(request, PROJECT_NAME, canonicalName, 'The canonical form.');
    const alternate = await createTerm(request, PROJECT_NAME, alternateName, 'Alias of the canonical.', canonical.id);

    // Cleanup: alternate first (it points at canonical), then canonical.
    // termToCleanup only holds one — we delete the alternate inline and let the
    // afterEach handle canonical cleanup.
    termToCleanup = canonical;

    // Pre-flight: confirm at the API layer that the bidirectional relationship
    // is actually populated. If this fails, the bug is in command wiring or the
    // detail-DTO mapper, not in the UI/page object — and the assertion will tell
    // us exactly what was returned. The previous run failed at the UI assertion
    // with no insight into whether the data was even present.
    //
    // Note on the canonicalTermId check: GlossaryTermDto has
    // @JsonInclude(NON_NULL), so a null canonicalTermId is OMITTED from the
    // JSON rather than serialized as `null`. We use toBeFalsy() to accept
    // both wire shapes (`undefined` from the omission, `null` from a hand-
    // built DTO) — what we care about is that A is not pointing at anything,
    // not the exact wire representation of "no pointer".
    const canonicalDetail = await getTermDetail(request, PROJECT_NAME, canonical.id);
    expect(canonicalDetail.canonicalTermId, 'canonical itself must have no canonical pointer').toBeFalsy();
    expect(
      (canonicalDetail.alternateTerms ?? []).map(a => a.name),
      'GET /api/projects/.../terms/<canonicalId> must report the alternate before we open the editor'
    ).toContain(alternateName);

    const page = await adminContext.newPage();
    const editorPage = new TermEditorPage(page);

    // Navigate directly to A's editor by id rather than going through the term
    // list. The list page's PrimeNG global filter searches across multiple
    // columns (`name`, `text`, `canonicalTermName`, `createdBy`) — so when the
    // search text is the canonical's name, BOTH rows pass the filter:
    //   - A's row (matches via `name`)
    //   - B's row (matches via `canonicalTermName`, since B points at A)
    // `tableRowsWithText(...).first()` then picks whichever sorts first, and
    // alphabetically "alternate" < "canonical" — so the list-click path
    // accidentally opened B's editor and the alternate-terms section never
    // rendered (B has no alternates of its own). The sister test below
    // ("set canonical term via UI selector …") already uses direct
    // navigation for the same reason.
    // gotoAndWaitForGet, not goto: page.goto() resolves on document load, well before the term
    // detail fetch returns, and #185 now gates the form on that fetch. Asserting straight after
    // a bare goto races the skeleton.
    await gotoAndWaitForGet(
      page,
      `/projects/${encodeURIComponent(PROJECT_NAME)}/terms/${canonical.id}`,
      response => response.url().includes(`/terms/${canonical.id}`)
    );

    await editorPage.expectAlternateTermInTable(alternateName);
    await editorPage.clickAlternateTerm(alternateName);

    // Now on the alternate's editor — the URL has switched to /terms/<alternate.id>.
    await expect(page).toHaveURL(new RegExp(`/terms/${alternate.id}$`));
    await editorPage.expectNameValue(alternateName);

    // Manual cleanup of the alternate (it would block deleting canonical otherwise
    // because of the FK pointer the alternate holds back to canonical).
    try { await deleteTerm(request, alternate); } catch { /* may already be cleaned */ }

    await page.close();
  });

  test('set canonical term via UI selector → save and reload preserves the link', async ({ adminContext, request }) => {
    // Coverage targets:
    //   - the canonical p-select in the form-grid (id="canonical", testid term-canonical-select)
    //   - the canonicalTermId !== originalCanonicalTermId branch of isDirty()
    //   - onSave's "edit existing term" path that sends canonicalTermId
    // Setup: two terms A and B, both freestanding. Open B, pick A as canonical, save,
    // reload, assert via API that the relation persisted (UI doesn't render the
    // canonical name back to the form, but the underlying GlossaryTermDto does).
    const aName = `e2e-term-set-canonical-a-${Date.now()}`;
    const bName = `e2e-term-set-canonical-b-${Date.now()}`;
    const a = await createTerm(request, PROJECT_NAME, aName, 'Term A');
    const b = await createTerm(request, PROJECT_NAME, bName, 'Term B');
    termToCleanup = a; // b will need explicit cleanup below

    const page = await adminContext.newPage();
    const listPage = new TermListPage(page);
    const editorPage = new TermEditorPage(page);

    await listPage.goto(PROJECT_NAME);
    await listPage.clickTerm(bName);

    await editorPage.selectCanonicalTerm(aName);
    await editorPage.save();

    await reloadAndWaitForGet(page, r => /\/terms\/\d+$/.test(r.url()));

    // Confirm the relation persisted at the API layer first — gives a clear
    // diagnostic if the UI assertion later fails (so we know whether the bug
    // is backend wiring or UI rendering).
    const aDetail = await getTermDetail(request, PROJECT_NAME, a.id);
    expect(
      (aDetail.alternateTerms ?? []).map(t => t.name),
      'after saving B with canonical=A, GET /terms/<aId> must report B as an alternate'
    ).toContain(bName);

    // Assert the relation server-side via the alternate-terms section on A's editor —
    // this is the canonical observable proof that B.canonicalTermId === A.id.
    await gotoAndWaitForGet(
      page,
      `/projects/${encodeURIComponent(PROJECT_NAME)}/terms/${a.id}`,
      response => response.url().includes(`/terms/${a.id}`)
    );
    await editorPage.expectAlternateTermInTable(bName);

    // Clean up B explicitly — without clearing its canonical pointer first the
    // delete would leave a dangling FK, but DeleteGlossaryTerm handles that on
    // the backend.
    try { await deleteTerm(request, b); } catch { /* fall through to afterEach */ }

    await page.close();
  });

  test('back button on term editor returns to term list', async ({ adminContext, request }) => {
    // Covers onBack() — a tiny method but currently zero E2E coverage. The Back
    // button is at the page-header level and has data-testid="term-back" (used by
    // TermEditorPage.navigateBack()).
    const termName = `e2e-term-back-${Date.now()}`;
    const term = await createTerm(request, PROJECT_NAME, termName, 'Term used for back-button test');
    termToCleanup = term;

    const page = await adminContext.newPage();
    const listPage = new TermListPage(page);
    const editorPage = new TermEditorPage(page);

    await listPage.goto(PROJECT_NAME);
    await listPage.clickTerm(termName);

    await editorPage.navigateBack(PROJECT_NAME);
    await listPage.expectTermInTable(termName);

    await page.close();
  });

});
