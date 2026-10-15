import { Page, expect } from '@playwright/test';
import { BaseListPage } from './BaseListPage';

export class ReportListPage extends BaseListPage {
  constructor(page: Page) {
    super(page);
  }

  async goto(projectName: string): Promise<void> {
    await this.gotoList(`/projects/${encodeURIComponent(projectName)}/reports`, '/reports');
  }

  async searchFor(name: string): Promise<void> {
    await this.searchWithPlaceholder('Search documents...', name);
  }

  async clickNewReport(): Promise<void> {
    await this.page.getByTestId('report-list-new').click();
    await this.page.waitForURL('**/reports/new');
  }

  async clickEdit(name: string): Promise<void> {
    await this.searchFor(name);
    const row = this.page.getByTestId('report-list-row').filter({ hasText: name }).first();
    await row.getByTestId('report-list-edit').click();
    await this.page.waitForURL(/\/reports\/\d+/);
    await expect(this.page.locator('#name')).not.toHaveValue('');
  }

  async runFromList(name: string): Promise<void> {
    await this.searchFor(name);
    const row = this.page.getByTestId('report-list-row').filter({ hasText: name }).first();
    await row.getByTestId('report-list-run').click();
  }

  async expectReportInTable(name: string): Promise<void> {
    await this.searchFor(name);
    await this.expectTableRowVisible(name);
  }

  async expectReportNotInTable(name: string): Promise<void> {
    await this.searchFor(name);
    await this.expectTableRowNotVisible(name);
  }

  async expectNewButtonVisible(): Promise<void> {
    await expect(this.page.getByTestId('report-list-new')).toBeVisible();
  }

  async expectNewButtonHidden(): Promise<void> {
    await expect(this.page.getByTestId('report-list-new')).toHaveCount(0);
  }

  async expectError(message: string): Promise<void> {
    await expect(this.page.getByTestId('report-list-error')).toContainText(message);
  }

  async expectEmptyState(): Promise<void> {
    await expect(this.page.getByTestId('report-list-empty')).toContainText('No documents yet');
  }
}

export class ReportEditorPage {
  constructor(private page: Page) {}

  async fillName(name: string): Promise<void> {
    const input = this.page.locator('#name');
    await input.clear();
    await input.fill(name);
  }

  async fillText(text: string): Promise<void> {
    const ta = this.page.locator('#text');
    await ta.clear();
    await ta.fill(text);
  }

  // report-editor uses replaceUrl:true on create (same as term-editor) — no load event
  // fires after history.replaceState(), so waitUntil:'commit' is required.
  async saveNew(): Promise<void> {
    await this.page.getByTestId('report-save').click();
    await this.page.waitForURL(/\/reports\/\d+/, { waitUntil: 'commit', timeout: 10000 });
  }

  async save(): Promise<void> {
    await Promise.all([
      this.page.waitForResponse(r => r.url().includes('/api/commands/EditReportGenerator')),
      this.page.getByTestId('report-save').click(),
    ]);
  }

  async run(): Promise<void> {
    await Promise.all([
      this.page.waitForResponse(r => r.url().includes('/reports/') && r.url().includes('/run')),
      this.page.getByTestId('report-run').click(),
    ]);
  }

  async delete(): Promise<void> {
    await this.page.getByTestId('report-delete').click();
    await this.page.getByRole('button', { name: 'Yes' }).click();
    await this.page.waitForURL(/\/reports$/);
  }

  async navigateBack(projectName: string): Promise<void> {
    await this.page.getByTestId('report-back').click();
    await this.page.waitForURL(`**/projects/${encodeURIComponent(projectName)}/reports`);
  }

  async expectNameValue(name: string): Promise<void> {
    await expect(this.page.locator('#name')).toHaveValue(name);
  }
}
