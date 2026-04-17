import { Page, expect } from '@playwright/test';

export class ProjectsPage {
  constructor(private page: Page) {}

  async goto(): Promise<void> {
    await Promise.all([
      this.page.waitForResponse(r => r.url().includes('/api/projects') && r.status() === 200),
      this.page.goto('/projects'),
    ]);
  }

  /** Filter the table so only rows matching name are visible — handles pagination. */
  async searchFor(name: string): Promise<void> {
    const searchInput = this.page.getByPlaceholder('Search projects...');
    await searchInput.clear();
    await searchInput.fill(name);
  }

  async expectProjectInTable(name: string): Promise<void> {
    await this.searchFor(name);
    await expect(this.page.locator('p-table td', { hasText: name })).toBeVisible();
  }

  async expectProjectNotInTable(name: string): Promise<void> {
    await this.searchFor(name);
    await expect(this.page.locator('p-table td', { hasText: name })).not.toBeVisible();
  }

  async clickNewProject(): Promise<void> {
    // Scope to app-list-page to avoid matching the sidebar's "New project" button
    await this.page.locator('app-list-page').getByRole('button', { name: 'New Project' }).click();
    await this.page.waitForURL('**/projects/new');
  }

  async clickProject(name: string): Promise<void> {
    await this.searchFor(name);
    await this.page.locator('p-table td', { hasText: name }).first().click();
    await this.page.waitForURL(`**/projects/${encodeURIComponent(name)}`);
    // Wait for form data to load before callers interact with the form
    await expect(this.page.locator('#name')).not.toHaveValue('');
  }
}

export class ProjectEditorPage {
  constructor(private page: Page) {}

  async fillName(name: string): Promise<void> {
    const input = this.page.locator('#name');
    await input.clear();
    await input.fill(name);
  }

  async fillDescription(text: string): Promise<void> {
    const ta = this.page.locator('#description');
    await ta.clear();
    await ta.fill(text);
  }

  async save(): Promise<void> {
    await this.page.getByRole('button', { name: 'Save' }).click();
    // wait for navigation away from /new or for save to complete
    await this.page.waitForLoadState('domcontentloaded');
  }

  async cancel(): Promise<void> {
    await this.page.getByRole('button', { name: 'Cancel' }).click();
  }

  async expectHeaderContains(text: string): Promise<void> {
    await expect(this.page.locator('h2, .page-title, .editor-title')).toContainText(text);
  }

  async delete(): Promise<void> {
    await this.page.getByRole('button', { name: 'Delete' }).click();
    // PrimeNG confirmation dialog — click Accept
    await this.page.getByRole('button', { name: 'Yes' }).click();
    await this.page.waitForURL('**/projects');
  }
}
