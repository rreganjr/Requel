import { Page, expect } from '@playwright/test';
import { BaseListPage } from './BaseListPage';

export class GoalListPage extends BaseListPage {
  constructor(page: Page) {
    super(page);
  }

  async goto(projectName: string): Promise<void> {
    await this.gotoList(`/projects/${encodeURIComponent(projectName)}/goals`, '/goals');
  }

  async searchFor(name: string): Promise<void> {
    await this.searchWithPlaceholder('Search goals...', name);
  }

  async clickNewGoal(): Promise<void> {
    await this.clickNewButton('New Goal', '**/goals/new');
  }

  async clickGoal(name: string): Promise<void> {
    await this.searchFor(name);
    await this.clickTableRow(name, /\/goals\/\d+/);
  }

  async expectGoalInTable(name: string): Promise<void> {
    await this.searchFor(name);
    await this.expectTableRowVisible(name);
  }

  async expectGoalNotInTable(name: string): Promise<void> {
    await this.searchFor(name);
    await this.expectTableRowNotVisible(name);
  }
}

export class GoalEditorPage {
  constructor(private page: Page) {}

  private relationRows(name: string) {
    return this.page.getByTestId('goal-relation-row').filter({ hasText: name });
  }

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

  async save(): Promise<void> {
    await this.page.getByTestId('goal-save').click();
    await this.page.waitForLoadState('domcontentloaded');
  }

  async delete(): Promise<void> {
    await this.page.getByTestId('goal-delete').click();
    // PrimeNG p-confirmDialog accept button
    await this.page.getByRole('button', { name: 'Yes' }).click();
    await this.page.waitForLoadState('domcontentloaded');
  }

  async copy(): Promise<void> {
    await this.page.getByTestId('goal-copy').click();
    // Register the response waiter before clicking Yes — the API call fires immediately on confirm.
    const [response] = await Promise.all([
      this.page.waitForResponse(r => r.url().includes('/api/commands/CopyGoal')),
      this.page.getByRole('button', { name: 'Yes' }).click(),
    ]);
    if (!response.ok()) {
      throw new Error(`CopyGoal failed: ${response.status()} ${await response.text()}`);
    }
  }

  async navigateBack(projectName: string): Promise<void> {
    await this.page.getByTestId('goal-back').click();
    await this.page.waitForURL(`**/projects/${encodeURIComponent(projectName)}/goals`);
  }

  async expectNameValue(name: string): Promise<void> {
    await expect(this.page.locator('#name')).toHaveValue(name);
  }

  async expectDescriptionValue(text: string): Promise<void> {
    await expect(this.page.locator('#text')).toHaveValue(text);
  }

  /**
   * Click "Add Relation", pick the target goal in the entity-selector dialog,
   * optionally change the relation type, then confirm.
   */
  async addRelation(toGoalName: string, relationType: 'Supports' | 'Conflicts' = 'Supports'): Promise<void> {
    await this.page.getByTestId('goal-add-relation').click();
    // p-dialog with appendTo="body" renders at document root; use role+name to distinguish
    // from p-confirmDialog (which renders as alertdialog).
    const dialog = this.page.getByRole('dialog', { name: 'Select Goal' });
    await dialog.waitFor({ state: 'visible' });
    await dialog.getByTestId('entity-selector-search').fill(toGoalName);
    await dialog.getByTestId('entity-selector-row').filter({ hasText: toGoalName }).first().click();
    // Relation-type dialog appears after the goal is selected. Like the selector above,
    // it is now a p-dialog with appendTo="body", so the dialog DOM is teleported to the
    // document root and the goal-relation-type-dialog testid sits on an empty, never-visible
    // host element. Match the real dialog by role+name (header is `Relation to "<goal>"`).
    const relationDialog = this.page.getByRole('dialog', { name: `Relation to "${toGoalName}"` });
    await relationDialog.waitFor({ state: 'visible' });
    if (relationType !== 'Supports') {
      await relationDialog.getByTestId('goal-relation-type-select').click();
      await this.page.getByRole('option', { name: relationType }).click();
    }
    const [response] = await Promise.all([
      this.page.waitForResponse(r => r.url().includes('/api/commands/EditGoalRelation')),
      relationDialog.getByTestId('goal-relation-add').click(),
    ]);
    if (!response.ok()) {
      throw new Error(`EditGoalRelation failed: ${response.status()} ${await response.text()}`);
    }
  }

  async removeRelation(toGoalName: string): Promise<void> {
    const [response] = await Promise.all([
      this.page.waitForResponse(r => r.url().includes('/api/commands/DeleteGoalRelation')),
      this.relationRows(toGoalName).getByTestId('goal-remove-relation').first().click(),
    ]);
    if (!response.ok()) {
      throw new Error(`DeleteGoalRelation failed: ${response.status()} ${await response.text()}`);
    }
  }

  async expectRelationInTable(toGoalName: string): Promise<void> {
    await expect(this.relationRows(toGoalName).first()).toBeVisible();
  }

  async expectRelationNotInTable(toGoalName: string): Promise<void> {
    await expect(this.relationRows(toGoalName)).toHaveCount(0);
  }
}
