import { Page, expect } from '@playwright/test';

export class StoryListPage {
  constructor(private page: Page) {}

  async goto(projectName: string): Promise<void> {
    await Promise.all([
      this.page.waitForResponse(r => r.url().includes('/stories') && r.status() === 200),
      this.page.goto(`/projects/${encodeURIComponent(projectName)}/stories`),
    ]);
  }

  async searchFor(name: string): Promise<void> {
    const searchInput = this.page.getByPlaceholder('Search stories...');
    await searchInput.clear();
    await searchInput.fill(name);
  }

  async clickNewStory(): Promise<void> {
    await this.page.locator('app-list-page').getByRole('button', { name: 'New Story' }).click();
    await this.page.waitForURL('**/stories/new');
  }

  async clickStory(name: string): Promise<void> {
    await this.searchFor(name);
    await this.page.locator('p-table td', { hasText: name }).first().click();
    await this.page.waitForURL(/\/stories\/\d+/);
    await expect(this.page.locator('#name')).not.toHaveValue('');
  }

  async expectStoryInTable(name: string): Promise<void> {
    await this.searchFor(name);
    await expect(this.page.locator('p-table td', { hasText: name })).toBeVisible();
  }

  async expectStoryNotInTable(name: string): Promise<void> {
    await this.searchFor(name);
    await expect(this.page.locator('p-table td', { hasText: name })).not.toBeVisible();
  }
}

export class StoryEditorPage {
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

  /**
   * Select a story type via PrimeNG p-select.
   * Clicks the dropdown trigger, then selects the matching option.
   */
  async selectStoryType(type: 'Success' | 'Exception'): Promise<void> {
    await this.page.locator('#type').click();
    await this.page.getByRole('option', { name: type }).click();
  }

  /**
   * Select a primary actor by name, or "(none)" to clear.
   */
  async selectPrimaryActor(actorName: string): Promise<void> {
    await this.page.locator('#primaryActor').click();
    await this.page.getByRole('option', { name: actorName }).click();
  }

  async save(): Promise<void> {
    const [response] = await Promise.all([
      this.page.waitForResponse(r => r.url().includes('/api/commands/EditStory')),
      this.page.getByRole('button', { name: 'Save' }).click(),
    ]);
    if (!response.ok()) {
      throw new Error(`EditStory command failed: ${response.status()} ${await response.text()}`);
    }
  }

  async clearPrimaryActor(): Promise<void> {
    // PrimeNG 21 Select renders the clear icon as an svg with data-pc-section="clearicon"
    // (not a button with aria-label="clear") when [showClear]="true" and a value is selected.
    await this.page.locator('#primaryActor').locator('[data-pc-section="clearicon"]').click();
  }

  async expectPrimaryActorValue(actorName: string): Promise<void> {
    await expect(this.page.locator('#primaryActor')).toContainText(actorName);
  }

  async expectNoPrimaryActor(): Promise<void> {
    // p-select shows the placeholder when no value is selected
    await expect(this.page.locator('#primaryActor')).toContainText('Select primary actor');
  }

  async delete(): Promise<void> {
    await this.page.getByRole('button', { name: 'Delete' }).click();
    const [response] = await Promise.all([
      this.page.waitForResponse(r => r.url().includes('/api/commands/DeleteStory') && r.status() === 200),
      this.page.getByRole('button', { name: 'Yes' }).click(),
    ]);
    if (!response.ok()) {
      throw new Error(`DeleteStory command failed: ${response.status()} ${await response.text()}`);
    }
  }

  async navigateBack(projectName: string): Promise<void> {
    await this.page.getByRole('button', { name: 'Back' }).click();
    await this.page.waitForURL(`**/projects/${encodeURIComponent(projectName)}/stories`);
  }

  async expectNameValue(name: string): Promise<void> {
    await expect(this.page.locator('#name')).toHaveValue(name);
  }

  async expectStoryTypeValue(type: string): Promise<void> {
    // p-select renders the selected value in a span inside the component
    await expect(this.page.locator('#type')).toContainText(type);
  }
}
