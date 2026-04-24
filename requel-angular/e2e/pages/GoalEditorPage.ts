import { Page, expect } from '@playwright/test';

export class GoalListPage {
  constructor(private page: Page) {}

  async goto(projectName: string): Promise<void> {
    await Promise.all([
      this.page.waitForResponse(r => r.url().includes('/goals') && r.status() === 200),
      this.page.goto(`/projects/${encodeURIComponent(projectName)}/goals`),
    ]);
  }

  async searchFor(name: string): Promise<void> {
    const searchInput = this.page.getByPlaceholder('Search goals...');
    await searchInput.clear();
    await searchInput.fill(name);
  }

  async clickNewGoal(): Promise<void> {
    await this.page.locator('app-list-page').getByRole('button', { name: 'New Goal' }).click();
    await this.page.waitForURL('**/goals/new');
  }

  async clickGoal(name: string): Promise<void> {
    await this.searchFor(name);
    await this.page.locator('p-table td', { hasText: name }).first().click();
    await this.page.waitForURL(/\/goals\/\d+/);
    await expect(this.page.locator('#name')).not.toHaveValue('');
  }

  async expectGoalInTable(name: string): Promise<void> {
    await this.searchFor(name);
    await expect(this.page.locator('p-table td', { hasText: name })).toBeVisible();
  }

  async expectGoalNotInTable(name: string): Promise<void> {
    await this.searchFor(name);
    await expect(this.page.locator('p-table td', { hasText: name })).not.toBeVisible();
  }
}

export class GoalEditorPage {
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

  async save(): Promise<void> {
    await this.page.getByRole('button', { name: 'Save' }).click();
    await this.page.waitForLoadState('domcontentloaded');
  }

  async delete(): Promise<void> {
    await this.page.getByRole('button', { name: 'Delete' }).click();
    // PrimeNG p-confirmDialog accept button
    await this.page.getByRole('button', { name: 'Yes' }).click();
    await this.page.waitForLoadState('domcontentloaded');
  }

  async copy(): Promise<void> {
    await this.page.getByRole('button', { name: 'Copy' }).click();
    // PrimeNG confirmation dialog — click Yes to confirm
    await this.page.getByRole('button', { name: 'Yes' }).click();
    await this.page.waitForLoadState('domcontentloaded');
  }

  async navigateBack(projectName: string): Promise<void> {
    await this.page.getByRole('button', { name: 'Back' }).click();
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
    await this.page.getByRole('button', { name: 'Add Relation' }).click();
    // p-dialog with appendTo="body" renders at document root; use role+name to distinguish
    // from p-confirmDialog (which renders as alertdialog).
    const dialog = this.page.getByRole('dialog', { name: 'Select Goal' });
    await dialog.waitFor({ state: 'visible' });
    await dialog.locator('[aria-label="Search"]').fill(toGoalName);
    await dialog.locator('p-table tr', { hasText: toGoalName }).first().click();
    // Custom relation-type dialog appears after goal is selected
    await this.page.locator('.relation-type-dialog').waitFor({ state: 'visible' });
    if (relationType !== 'Supports') {
      await this.page.locator('.relation-type-dialog p-select').click();
      await this.page.getByRole('option', { name: relationType }).click();
    }
    const [response] = await Promise.all([
      this.page.waitForResponse(r => r.url().includes('/api/commands/EditGoalRelation')),
      this.page.locator('.dialog-actions').getByRole('button', { name: 'Add' }).click(),
    ]);
    if (!response.ok()) {
      throw new Error(`EditGoalRelation failed: ${response.status()} ${await response.text()}`);
    }
  }

  async removeRelation(toGoalName: string): Promise<void> {
    const [response] = await Promise.all([
      this.page.waitForResponse(r => r.url().includes('/api/commands/DeleteGoalRelation')),
      this.page.locator('p-table tr', { hasText: toGoalName })
               .getByRole('button')
               .first()
               .click(),
    ]);
    if (!response.ok()) {
      throw new Error(`DeleteGoalRelation failed: ${response.status()} ${await response.text()}`);
    }
  }

  async expectRelationInTable(toGoalName: string): Promise<void> {
    await expect(this.page.locator('p-table td', { hasText: toGoalName }).first()).toBeVisible();
  }

  async expectRelationNotInTable(toGoalName: string): Promise<void> {
    await expect(this.page.locator('p-table td', { hasText: toGoalName }).first()).not.toBeVisible();
  }
}
