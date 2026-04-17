import { Page, expect } from '@playwright/test';

export class ActorListPage {
  constructor(private page: Page) {}

  async goto(projectName: string): Promise<void> {
    await Promise.all([
      this.page.waitForResponse(r => r.url().includes('/actors') && r.status() === 200),
      this.page.goto(`/projects/${encodeURIComponent(projectName)}/actors`),
    ]);
  }

  async searchFor(name: string): Promise<void> {
    const searchInput = this.page.getByPlaceholder('Search actors...');
    await searchInput.clear();
    await searchInput.fill(name);
  }

  async clickNewActor(): Promise<void> {
    await this.page.locator('app-list-page').getByRole('button', { name: 'New Actor' }).click();
    await this.page.waitForURL('**/actors/new');
  }

  async clickActor(name: string): Promise<void> {
    await this.searchFor(name);
    await this.page.locator('p-table td', { hasText: name }).first().click();
    await this.page.waitForURL(/\/actors\/\d+/);
    await expect(this.page.locator('#name')).not.toHaveValue('');
  }

  async expectActorInTable(name: string): Promise<void> {
    await this.searchFor(name);
    await expect(this.page.locator('p-table td', { hasText: name })).toBeVisible();
  }

  async expectActorNotInTable(name: string): Promise<void> {
    await this.searchFor(name);
    await expect(this.page.locator('p-table td', { hasText: name })).not.toBeVisible();
  }

  async countActorRows(nameSubstring: string): Promise<number> {
    await this.searchFor(nameSubstring);
    return this.page.locator('p-table td', { hasText: nameSubstring }).count();
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
    const createBtn = this.page.getByRole('button', { name: 'Create' });
    const saveBtn = this.page.getByRole('button', { name: 'Save' });
    const btn = (await createBtn.count()) > 0 ? createBtn : saveBtn;
    await btn.click();
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
    await this.page.waitForURL(`**/projects/${encodeURIComponent(projectName)}/actors`);
  }

  async expectNameValue(name: string): Promise<void> {
    await expect(this.page.locator('#name')).toHaveValue(name);
  }

  async currentUrl(): Promise<string> {
    return this.page.url();
  }
}
