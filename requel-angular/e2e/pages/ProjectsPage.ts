import { Page, expect } from '@playwright/test';
import { BaseListPage } from './BaseListPage';

export class ProjectsPage extends BaseListPage {
  constructor(page: Page) {
    super(page);
  }

  async goto(): Promise<void> {
    await this.gotoList('/projects', '/api/projects');
  }

  /** Filter the table so only rows matching name are visible — handles pagination. */
  async searchFor(name: string): Promise<void> {
    await this.searchWithPlaceholder('Search projects...', name);
  }

  async expectProjectInTable(name: string): Promise<void> {
    await this.searchFor(name);
    await this.expectTableRowVisible(name);
  }

  async expectProjectNotInTable(name: string): Promise<void> {
    await this.searchFor(name);
    await this.expectTableRowNotVisible(name);
  }

  async clickNewProject(): Promise<void> {
    await this.clickNewButton('New Project', '**/projects/new');
  }

  async clickProject(name: string): Promise<void> {
    await this.searchFor(name);
    await this.clickTableRow(name, `**/projects/${encodeURIComponent(name)}`);
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
