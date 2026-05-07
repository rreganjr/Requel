import { Page, expect } from '@playwright/test';
import { BaseListPage } from './BaseListPage';

export class ActorListPage extends BaseListPage {
  constructor(page: Page) {
    super(page);
  }

  async goto(projectName: string): Promise<void> {
    await this.gotoList(`/projects/${encodeURIComponent(projectName)}/actors`, '/actors');
  }

  async searchFor(name: string): Promise<void> {
    await this.searchWithPlaceholder('Search actors...', name);
  }

  async clickNewActor(): Promise<void> {
    await this.clickNewButton('New Actor', '**/actors/new');
  }

  async clickActor(name: string): Promise<void> {
    await this.searchFor(name);
    await this.clickTableRow(name, /\/actors\/\d+/);
  }

  async expectActorInTable(name: string): Promise<void> {
    await this.searchFor(name);
    await this.expectTableRowVisible(name);
  }

  async expectActorNotInTable(name: string): Promise<void> {
    await this.searchFor(name);
    await this.expectTableRowNotVisible(name);
  }

  async countActorRows(nameSubstring: string): Promise<number> {
    await this.searchFor(nameSubstring);
    return this.countTableRows(nameSubstring);
  }
}

export class ActorEditorPage {
  constructor(private page: Page) {}

  async fillName(name: string): Promise<void> {
    const input = this.page.locator('#name');
    await input.clear();
    await input.fill(name);
  }

  async fillDescription(text: string): Promise<void> {
    const ta = this.page.locator('#text');
    await ta.clear();
    await ta.fill(text);
  }

  /** New actors show "Create"; existing actors show "Save" */
  async save(): Promise<void> {
    // Try "Create" first (new actor), fall back to "Save" (existing actor)
    const createBtn = this.page.getByTestId('actor-create');
    const saveBtn = this.page.getByTestId('actor-save');
    const btn = (await createBtn.count()) > 0 ? createBtn : saveBtn;
    await btn.click();
    await this.page.waitForLoadState('domcontentloaded');
  }

  async copy(): Promise<void> {
    await this.page.getByTestId('actor-copy').click();
    // Register the response waiter before clicking Yes — the API call fires immediately on confirm.
    const [response] = await Promise.all([
      this.page.waitForResponse(r => r.url().includes('/api/commands/CopyActor')),
      this.page.getByRole('button', { name: 'Yes' }).click(),
    ]);
    if (!response.ok()) {
      throw new Error(`CopyActor failed: ${response.status()} ${await response.text()}`);
    }
  }

  async delete(): Promise<void> {
    await this.page.getByTestId('actor-delete').click();
    await this.page.getByRole('button', { name: 'Yes' }).click();
    await this.page.waitForLoadState('domcontentloaded');
  }

  async navigateBack(projectName: string): Promise<void> {
    await this.page.getByTestId('actor-back').click();
    await this.page.waitForURL(`**/projects/${encodeURIComponent(projectName)}/actors`);
  }

  async expectNameValue(name: string): Promise<void> {
    await expect(this.page.locator('#name')).toHaveValue(name);
  }

  async currentUrl(): Promise<string> {
    return this.page.url();
  }

  // ── Goals sub-table helpers ──

  private goalRow(goalName: string) {
    return this.page.getByTestId('actor-goal-row').filter({ hasText: goalName });
  }

  async addGoal(goalName: string): Promise<void> {
    await this.page.getByTestId('actor-add-goal').click();
    const dialog = this.page.getByRole('dialog', { name: 'Select Goal' });
    await dialog.waitFor({ state: 'visible' });
    await dialog.getByTestId('entity-selector-search').fill(goalName);
    const [response] = await Promise.all([
      this.page.waitForResponse(r => r.url().includes('/api/commands/AddGoalToGoalContainer')),
      dialog.getByTestId('entity-selector-row').filter({ hasText: goalName }).first().click(),
    ]);
    if (!response.ok()) {
      throw new Error(`AddGoalToGoalContainer failed: ${response.status()} ${await response.text()}`);
    }
  }

  async removeGoal(goalName: string): Promise<void> {
    const [response] = await Promise.all([
      this.page.waitForResponse(r => r.url().includes('/api/commands/RemoveGoalFromGoalContainer')),
      this.goalRow(goalName).getByTestId('actor-remove-goal').click(),
    ]);
    if (!response.ok()) {
      throw new Error(`RemoveGoalFromGoalContainer failed: ${response.status()} ${await response.text()}`);
    }
  }

  async clickGoal(goalName: string): Promise<void> {
    await this.goalRow(goalName).getByTestId('actor-goal-link').first().click();
  }

  async expectGoalInTable(goalName: string): Promise<void> {
    await expect(this.goalRow(goalName).first()).toBeVisible();
  }

  async expectGoalNotInTable(goalName: string): Promise<void> {
    await expect(this.goalRow(goalName)).toHaveCount(0);
  }

  // ── Referenced By helpers ──

  async clickReferencedByUseCase(name: string): Promise<void> {
    await this.page.getByTestId('actor-refby-usecase-row')
      .filter({ hasText: name })
      .getByTestId('actor-refby-usecase-link')
      .first()
      .click();
  }

  async clickReferencedByStory(name: string): Promise<void> {
    await this.page.getByTestId('actor-refby-story-row')
      .filter({ hasText: name })
      .getByTestId('actor-refby-story-link')
      .first()
      .click();
  }

  async expectReferencedByUseCase(name: string): Promise<void> {
    await expect(
      this.page.getByTestId('actor-refby-usecase-row').filter({ hasText: name }).first()
    ).toBeVisible();
  }

  async expectReferencedByStory(name: string): Promise<void> {
    await expect(
      this.page.getByTestId('actor-refby-story-row').filter({ hasText: name }).first()
    ).toBeVisible();
  }
}
