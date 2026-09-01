import { Page, expect } from '@playwright/test';
import { BaseListPage } from './BaseListPage';
import { completeCreateWizard } from './wizard';

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
    // Create is a wizard since #173; edit still has a Save button.
    if (await completeCreateWizard(this.page, /\/api\/commands\/EditActor/)) {
      return;
    }
    // Edit mode shows "Save"; a pre-#173 build shows "Create" on the new-actor form. Keeping
    // the fallback means this page object still works against an older build instead of
    // failing on a missing testid, which is otherwise a confusing way to discover you are
    // running the wrong jar.
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

  // #24 replaced the two read-only referenced-by tables (actor-refby-usecase / -story) with one
  // combined, editable "Referenced By" section, so both rows now share actor-referrer-row /
  // actor-referrer-link and are matched by the (unique, timestamped) entity name.
  async clickReferencedByUseCase(name: string): Promise<void> {
    await this.clickReferrer(name);
  }

  async clickReferencedByStory(name: string): Promise<void> {
    await this.clickReferrer(name);
  }

  private async clickReferrer(name: string): Promise<void> {
    await this.page.getByTestId('actor-referrer-row')
      .filter({ hasText: name })
      .getByTestId('actor-referrer-link')
      .first()
      .click();
  }

  async expectReferencedByUseCase(name: string): Promise<void> {
    await this.expectReferrer(name);
  }

  async expectReferencedByStory(name: string): Promise<void> {
    await this.expectReferrer(name);
  }

  private async expectReferrer(name: string): Promise<void> {
    await expect(
      this.page.getByTestId('actor-referrer-row').filter({ hasText: name }).first()
    ).toBeVisible();
  }
}
