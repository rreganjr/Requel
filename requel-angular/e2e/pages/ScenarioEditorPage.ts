import { Page, expect } from '@playwright/test';

export class ScenarioListPage {
  constructor(private page: Page) {}

  async goto(projectName: string): Promise<void> {
    await Promise.all([
      this.page.waitForResponse(r => r.url().includes('/scenarios') && r.status() === 200),
      this.page.goto(`/projects/${encodeURIComponent(projectName)}/scenarios`),
    ]);
  }

  async clickNewScenario(): Promise<void> {
    await this.page.locator('app-list-page').getByRole('button', { name: 'New Scenario' }).click();
    await this.page.waitForURL('**/scenarios/new');
  }

  async clickScenario(name: string): Promise<void> {
    await this.page.locator('p-table td', { hasText: name }).first().click();
    await this.page.waitForURL(/\/scenarios\/\d+/);
    await expect(this.page.locator('#name')).not.toHaveValue('');
  }

  async expectScenarioInTable(name: string): Promise<void> {
    await expect(this.page.locator('p-table td', { hasText: name })).toBeVisible();
  }

  async expectScenarioNotInTable(name: string): Promise<void> {
    await expect(this.page.locator('p-table td', { hasText: name })).not.toBeVisible();
  }
}

export class ScenarioEditorPage {
  constructor(private page: Page) {}

  async fillName(name: string): Promise<void> {
    const input = this.page.locator('#name');
    await input.clear();
    await input.fill(name);
  }

  /**
   * Select a scenario type via PrimeNG p-select.
   */
  async selectType(type: 'Primary' | 'PreCondition' | 'Optional' | 'Alternative' | 'Exception'): Promise<void> {
    await this.page.locator('#type').click();
    await this.page.getByRole('option', { name: type }).click();
  }

  async save(): Promise<void> {
    await this.page.getByRole('button', { name: 'Save' }).click();
    await this.page.waitForLoadState('domcontentloaded');
  }

  async copy(): Promise<void> {
    await this.page.getByRole('button', { name: 'Copy' }).click();
    // PrimeNG confirmation dialog — click Yes to confirm
    await this.page.getByRole('button', { name: 'Yes' }).click();
    await this.page.waitForLoadState('domcontentloaded');
  }

  async delete(): Promise<void> {
    await this.page.getByRole('button', { name: 'Delete' }).click();
    await this.page.getByRole('button', { name: 'Yes' }).click();
    await this.page.waitForLoadState('domcontentloaded');
  }

  async navigateBack(projectName: string): Promise<void> {
    await this.page.getByRole('button', { name: 'Back' }).click();
    await this.page.waitForURL(`**/projects/${encodeURIComponent(projectName)}/scenarios`);
  }

  async expectNameValue(name: string): Promise<void> {
    await expect(this.page.locator('#name')).toHaveValue(name);
  }

  async expectTypeValue(type: string): Promise<void> {
    await expect(this.page.locator('#type')).toContainText(type);
  }
}
