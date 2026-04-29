import { Page, expect } from '@playwright/test';

export class SettingsPage {
  constructor(private page: Page) {}

  private projectLimit() {
    return this.page.getByTestId('settings-project-limit').locator('input');
  }

  private stalenessSelect() {
    return this.page.getByTestId('settings-staleness');
  }

  async goto(): Promise<void> {
    await Promise.all([
      this.page.waitForResponse(r => r.url().includes('/user-preferences') && r.status() === 200),
      this.page.goto('/settings'),
    ]);
  }

  /**
   * Target the app-owned test hook and type into the underlying spinbutton.
   */
  async fillProjectLimit(limit: number): Promise<void> {
    const input = this.projectLimit();
    await input.click({ clickCount: 3 }); // select all current text
    await input.pressSequentially(String(limit)); // PrimeNG responds to real key events
  }

  /**
   * Open the select from the app-owned test hook, then choose the visible option by role.
   */
  async selectStaleness(label: string): Promise<void> {
    await this.stalenessSelect().click();
    const option = this.page.getByRole('option', { name: label, exact: true });
    await option.click();
  }

  async save(): Promise<void> {
    const [response] = await Promise.all([
      this.page.waitForResponse(r =>
        r.url().includes('/user-preferences') && r.request().method() === 'PUT'
      ),
      this.page.getByTestId('settings-save').click(),
    ]);
    if (!response.ok()) {
      throw new Error(`PUT /user-preferences failed: ${response.status()} ${await response.text()}`);
    }
  }

  async resetToDefaults(): Promise<void> {
    const [response] = await Promise.all([
      this.page.waitForResponse(r =>
        r.url().includes('/user-preferences') && r.request().method() === 'PUT'
      ),
      this.page.getByTestId('settings-reset').click(),
    ]);
    if (!response.ok()) {
      throw new Error(`PUT /user-preferences (reset) failed: ${response.status()} ${await response.text()}`);
    }
  }

  async expectProjectLimit(limit: number): Promise<void> {
    await expect(this.projectLimit()).toHaveValue(String(limit));
  }

  async expectStaleness(label: string): Promise<void> {
    await expect(this.stalenessSelect()).toContainText(label);
  }

  async expectSuccessMessage(): Promise<void> {
    await expect(this.page.locator('app-settings p-message')).toBeVisible();
  }
}
