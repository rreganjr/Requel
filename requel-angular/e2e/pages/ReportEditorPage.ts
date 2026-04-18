import { Page, expect } from '@playwright/test';

export class ReportListPage {
  constructor(private page: Page) {}

  async goto(projectName: string): Promise<void> {
    await Promise.all([
      this.page.waitForResponse(r => r.url().includes('/reports') && r.status() === 200),
      this.page.goto(`/projects/${encodeURIComponent(projectName)}/reports`),
    ]);
  }

  async searchFor(name: string): Promise<void> {
    const input = this.page.getByPlaceholder('Search documents...');
    await input.clear();
    await input.fill(name);
  }

  async clickNewReport(): Promise<void> {
    await this.page.locator('app-list-page').getByRole('button', { name: 'New Document' }).click();
    await this.page.waitForURL('**/reports/new');
  }

  async clickEdit(name: string): Promise<void> {
    await this.searchFor(name);
    const row = this.page.locator('p-table tr', { hasText: name });
    await row.getByRole('button', { name: 'Edit' }).click();
    await this.page.waitForURL(/\/reports\/\d+/);
    await expect(this.page.locator('#name')).not.toHaveValue('');
  }

  async expectReportInTable(name: string): Promise<void> {
    await this.searchFor(name);
    await expect(this.page.locator('p-table td', { hasText: name })).toBeVisible();
  }

  async expectReportNotInTable(name: string): Promise<void> {
    await this.searchFor(name);
    await expect(this.page.locator('p-table td', { hasText: name })).not.toBeVisible();
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
    await this.page.getByRole('button', { name: 'Save' }).click();
    await this.page.waitForURL(/\/reports\/\d+/, { waitUntil: 'commit', timeout: 10000 });
  }

  async save(): Promise<void> {
    await Promise.all([
      this.page.waitForResponse(r => r.url().includes('/api/commands/EditReportGenerator')),
      this.page.getByRole('button', { name: 'Save' }).click(),
    ]);
  }

  async run(): Promise<void> {
    await Promise.all([
      this.page.waitForResponse(r => r.url().includes('/reports/') && r.url().includes('/run')),
      this.page.getByRole('button', { name: 'Run' }).click(),
    ]);
  }

  async delete(): Promise<void> {
    await this.page.getByRole('button', { name: 'Delete' }).click();
    await this.page.getByRole('button', { name: 'Yes' }).click();
    await this.page.waitForURL(/\/reports$/);
  }

  async navigateBack(projectName: string): Promise<void> {
    await this.page.getByRole('button', { name: 'Back' }).click();
    await this.page.waitForURL(`**/projects/${encodeURIComponent(projectName)}/reports`);
  }

  async expectNameValue(name: string): Promise<void> {
    await expect(this.page.locator('#name')).toHaveValue(name);
  }
}
