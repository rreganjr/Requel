import { test, expect } from './fixtures/auth';
import { createProject, deleteProject, createReport, deleteReport, ReportFixture } from './fixtures/api-helper';
import { ReportListPage, ReportEditorPage } from './pages/ReportEditorPage';
import { reloadAndWaitForGet } from './helpers/navigation';

const PROJECT_NAME = `e2e-reports-${Date.now()}`;
let reportToCleanup: ReportFixture | null = null;

// Minimal valid XSLT 1.0 — produces an HTML page from any XML input.
const MINIMAL_XSLT = [
  '<?xml version="1.0" encoding="UTF-8"?>',
  '<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">',
  '  <xsl:template match="/">',
  '    <html><body><p>E2E Test Report</p></body></html>',
  '  </xsl:template>',
  '</xsl:stylesheet>',
].join('\n');

test.beforeAll(async ({ request }) => {
  await createProject(request, PROJECT_NAME, 'Reports E2E test project');
});

test.afterAll(async ({ request }) => {
  await deleteProject(request, PROJECT_NAME);
});

test.afterEach(async ({ request }) => {
  if (reportToCleanup) {
    try { await deleteReport(request, reportToCleanup); } catch { /* may already be deleted by test */ }
    reportToCleanup = null;
  }
});

test.describe('Report generator management', () => {

  test('report list hides New Document when edit permission is absent', async ({ adminContext }) => {
    const page = await adminContext.newPage();
    const listPage = new ReportListPage(page);

    await page.route(`**/api/projects/${encodeURIComponent(PROJECT_NAME)}/my-permissions`, async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          isStakeholder: true,
          canCreateProjects: false,
          permissions: {},
        }),
      });
    });

    await listPage.goto(PROJECT_NAME);
    await listPage.expectNewButtonHidden();

    await page.close();
  });

  test('empty report list shows empty-state message', async ({ adminContext }) => {
    const page = await adminContext.newPage();
    const listPage = new ReportListPage(page);

    await page.route(`**/api/projects/${encodeURIComponent(PROJECT_NAME)}/reports`, async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([]),
      });
    });

    await listPage.goto(PROJECT_NAME);
    await listPage.expectEmptyState();

    await page.close();
  });

  test('report list load failure shows error banner', async ({ adminContext }) => {
    const page = await adminContext.newPage();
    const listPage = new ReportListPage(page);

    await page.route(`**/api/projects/${encodeURIComponent(PROJECT_NAME)}/reports`, async route => {
      await route.fulfill({
        status: 500,
        contentType: 'application/json',
        body: JSON.stringify({
          error: 'INTERNAL_ERROR',
          message: 'boom',
        }),
      });
    });

    await listPage.goto(PROJECT_NAME);
    await listPage.expectError('Failed to load documents.');

    await page.close();
  });

  test('run report from list failure shows error banner', async ({ adminContext, request }) => {
    const reportName = `e2e-report-run-list-${Date.now()}`;
    const report = await createReport(request, PROJECT_NAME, reportName, MINIMAL_XSLT);
    reportToCleanup = report;

    const page = await adminContext.newPage();
    const listPage = new ReportListPage(page);

    await page.route(`**/api/projects/${encodeURIComponent(PROJECT_NAME)}/reports/${report.id}/run`, async route => {
      await route.fulfill({
        status: 500,
        contentType: 'text/plain',
        body: 'boom',
      });
    });

    await listPage.goto(PROJECT_NAME);
    await listPage.runFromList(reportName);
    await listPage.expectError(`Failed to generate report "${reportName}".`);

    await page.close();
  });

  test('create report → appears in report list', async ({ adminContext }) => {
    const reportName = `e2e-report-create-${Date.now()}`;
    const page = await adminContext.newPage();
    const listPage = new ReportListPage(page);
    const editorPage = new ReportEditorPage(page);

    await listPage.goto(PROJECT_NAME);
    await listPage.clickNewReport();

    await editorPage.fillName(reportName);
    await editorPage.fillText(MINIMAL_XSLT);
    // saveNew() uses waitUntil:'commit' — report-editor uses replaceUrl:true on create
    await editorPage.saveNew();

    const url = page.url();
    const idMatch = url.match(/\/reports\/(\d+)/);
    if (idMatch) {
      reportToCleanup = { id: parseInt(idMatch[1], 10), version: 0, name: reportName, projectName: PROJECT_NAME };
    }

    await listPage.goto(PROJECT_NAME);
    await listPage.expectReportInTable(reportName);

    await page.close();
  });

  test('edit report name → persists after reload', async ({ adminContext, request }) => {
    const reportName = `e2e-report-edit-${Date.now()}`;
    const newName = `e2e-report-edited-${Date.now()}`;
    const report = await createReport(request, PROJECT_NAME, reportName, MINIMAL_XSLT);
    reportToCleanup = report; // id is stable across rename; cleanup uses id not name

    const page = await adminContext.newPage();
    const listPage = new ReportListPage(page);
    const editorPage = new ReportEditorPage(page);

    await listPage.goto(PROJECT_NAME);
    await listPage.clickEdit(reportName);

    await editorPage.fillName(newName);
    await editorPage.save();

    await reloadAndWaitForGet(page, r => /\/reports\/\d+$/.test(r.url()));
    await editorPage.expectNameValue(newName);

    await page.close();
  });

  test('run report → report generated', async ({ adminContext, request }) => {
    const reportName = `e2e-report-run-${Date.now()}`;
    const report = await createReport(request, PROJECT_NAME, reportName, MINIMAL_XSLT);
    reportToCleanup = report;

    const page = await adminContext.newPage();
    const listPage = new ReportListPage(page);
    const editorPage = new ReportEditorPage(page);

    await listPage.goto(PROJECT_NAME);
    await listPage.clickEdit(reportName);

    // run() waits for the GET …/reports/{id}/run response via waitForResponse.
    // downloadReport() uses native fetch (includes Bearer token), so Playwright
    // captures it at the browser network level like any other HTTP call.
    await editorPage.run();

    // No error message should be visible after a successful run
    await expect(page.locator('app-report-editor p-message')).not.toBeVisible();

    await page.close();
  });

  test('delete report → removed from list', async ({ adminContext, request }) => {
    const reportName = `e2e-report-delete-${Date.now()}`;
    await createReport(request, PROJECT_NAME, reportName);
    reportToCleanup = null; // test deletes it

    const page = await adminContext.newPage();
    const listPage = new ReportListPage(page);
    const editorPage = new ReportEditorPage(page);

    await listPage.goto(PROJECT_NAME);
    await listPage.clickEdit(reportName);

    await editorPage.delete();

    await listPage.expectReportNotInTable(reportName);

    await page.close();
  });

});
