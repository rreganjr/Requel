import { Page, expect } from '@playwright/test';

export class UseCaseListPage {
  constructor(private page: Page) {}

  async goto(projectName: string): Promise<void> {
    await Promise.all([
      this.page.waitForResponse(r => r.url().includes('/use-cases') && r.status() === 200),
      this.page.goto(`/projects/${encodeURIComponent(projectName)}/use-cases`),
    ]);
  }

  async clickNewUseCase(): Promise<void> {
    await this.page.locator('app-list-page').getByRole('button', { name: 'New Use Case' }).click();
    await this.page.waitForURL('**/use-cases/new');
  }

  async clickUseCase(name: string): Promise<void> {
    await this.page.locator('p-table td', { hasText: name }).first().click();
    await this.page.waitForURL(/\/use-cases\/\d+/);
    await expect(this.page.locator('#name')).not.toHaveValue('');
  }

  async expectUseCaseInTable(name: string): Promise<void> {
    await expect(this.page.locator('p-table td', { hasText: name })).toBeVisible();
  }

  async expectUseCaseNotInTable(name: string): Promise<void> {
    await expect(this.page.locator('p-table td', { hasText: name })).not.toBeVisible();
  }
}

export class UseCaseEditorPage {
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

  async selectPrimaryActor(actorName: string): Promise<void> {
    await this.page.locator('#primaryActor').click();
    await this.page.getByRole('option', { name: actorName }).click();
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
    await this.page.waitForURL(`**/projects/${encodeURIComponent(projectName)}/use-cases`);
  }

  async expectNameValue(name: string): Promise<void> {
    await expect(this.page.locator('#name')).toHaveValue(name);
  }
}
