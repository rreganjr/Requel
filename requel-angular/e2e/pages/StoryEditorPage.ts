import { Page, expect } from '@playwright/test';
import { BaseListPage } from './BaseListPage';

export class StoryListPage extends BaseListPage {
  constructor(page: Page) {
    super(page);
  }

  async goto(projectName: string): Promise<void> {
    await this.gotoList(`/projects/${encodeURIComponent(projectName)}/stories`, '/stories');
  }

  async searchFor(name: string): Promise<void> {
    await this.searchWithPlaceholder('Search stories...', name);
  }

  async clickNewStory(): Promise<void> {
    await this.clickNewButton('New Story', '**/stories/new');
  }

  async clickStory(name: string): Promise<void> {
    await this.searchFor(name);
    await this.clickTableRow(name, /\/stories\/\d+/);
  }

  async expectStoryInTable(name: string): Promise<void> {
    await this.searchFor(name);
    await this.expectTableRowVisible(name);
  }

  async expectStoryNotInTable(name: string): Promise<void> {
    await this.searchFor(name);
    await this.expectTableRowNotVisible(name);
  }
}

export class StoryEditorPage {
  constructor(private page: Page) {}

  private storyTypeSelect() {
    return this.page.getByTestId('story-type');
  }

  private primaryActorSelect() {
    return this.page.getByTestId('story-primary-actor');
  }

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
    await this.storyTypeSelect().click();
    await this.page.getByRole('option', { name: type }).click();
  }

  /**
   * Select a primary actor by name, or "(none)" to clear.
   */
  async selectPrimaryActor(actorName: string): Promise<void> {
    await this.primaryActorSelect().click();
    await this.page.getByRole('option', { name: actorName }).click();
  }

  async save(): Promise<void> {
    const [response] = await Promise.all([
      this.page.waitForResponse(r => r.url().includes('/api/commands/EditStory')),
      this.page.getByTestId('story-save').click(),
    ]);
    if (!response.ok()) {
      throw new Error(`EditStory command failed: ${response.status()} ${await response.text()}`);
    }
  }

  async clearPrimaryActor(): Promise<void> {
    await this.page.getByTestId('story-primary-actor-clear').click();
  }

  async expectPrimaryActorValue(actorName: string): Promise<void> {
    await expect(this.primaryActorSelect()).toContainText(actorName);
  }

  async expectNoPrimaryActor(): Promise<void> {
    // p-select shows the placeholder when no value is selected
    await expect(this.primaryActorSelect()).toContainText('Select primary actor');
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
    await expect(this.storyTypeSelect()).toContainText(type);
  }

  async addAdditionalActor(actorName: string): Promise<void> {
    await this.page.getByTestId('story-add-actor').click();
    const dialog = this.page.getByRole('dialog', { name: 'Select Actor' });
    await dialog.waitFor({ state: 'visible' });
    await dialog.getByTestId('entity-selector-search').fill(actorName);
    const [response] = await Promise.all([
      this.page.waitForResponse(r => r.url().includes('/api/commands/AddActorToActorContainer')),
      dialog.getByTestId('entity-selector-row').filter({ hasText: actorName }).first().click(),
    ]);
    if (!response.ok()) {
      throw new Error(`AddActorToActorContainer failed: ${response.status()} ${await response.text()}`);
    }
  }

  async removeAdditionalActor(actorName: string): Promise<void> {
    const [response] = await Promise.all([
      this.page.waitForResponse(r => r.url().includes('/api/commands/RemoveActorFromActorContainer')),
      this.page.getByTestId('story-additional-actor-row').filter({ hasText: actorName })
               .getByTestId('story-remove-additional-actor')
               .first()
               .click(),
    ]);
    if (!response.ok()) {
      throw new Error(`RemoveActorFromActorContainer failed: ${response.status()} ${await response.text()}`);
    }
  }

  async expectAdditionalActorInTable(actorName: string): Promise<void> {
    await expect(
      this.page.getByTestId('story-additional-actor-row').filter({ hasText: actorName }).first()
    ).toBeVisible();
  }

  async expectAdditionalActorNotInTable(actorName: string): Promise<void> {
    await expect(
      this.page.getByTestId('story-additional-actor-row').filter({ hasText: actorName })
    ).toHaveCount(0);
  }
}
