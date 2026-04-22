import { Page, expect } from '@playwright/test';

export class SettingsPage {
  constructor(private page: Page) {}

  async goto(): Promise<void> {
    await Promise.all([
      this.page.waitForResponse(r => r.url().includes('/user-preferences') && r.status() === 200),
      this.page.goto('/settings'),
    ]);
  }

  /**
   * p-inputNumber puts id="projectLimit" on the host element, not the inner <input>.
   * Target the <input> directly. Triple-click selects the current value; pressSequentially()
   * fires real keydown/keypress/keyup events that PrimeNG's InputNumber CVA processes
   * (unlike fill() which only fires an input event and PrimeNG ignores for value updates).
   */
  async fillProjectLimit(limit: number): Promise<void> {
    const input = this.page.locator('p-inputnumber input');
    await input.click({ clickCount: 3 }); // select all current text
    await input.pressSequentially(String(limit)); // PrimeNG responds to real key events
  }

  /**
   * p-select (non-editable) — click the host to open the overlay panel,
   * wait for the option list to be visible, then click by PrimeNG CSS class.
   * Using li.p-select-option avoids depending on role="option" ARIA rendering.
   */
  async selectStaleness(label: string): Promise<void> {
    await this.page.locator('p-select').click();
    const option = this.page.locator('li.p-select-option', { hasText: label });
    await option.waitFor({ state: 'visible' });
    await option.click();
  }

  async save(): Promise<void> {
    const [response] = await Promise.all([
      this.page.waitForResponse(r =>
        r.url().includes('/user-preferences') && r.request().method() === 'PUT'
      ),
      this.page.getByRole('button', { name: 'Save' }).click(),
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
      this.page.getByRole('button', { name: 'Reset to Defaults' }).click(),
    ]);
    if (!response.ok()) {
      throw new Error(`PUT /user-preferences (reset) failed: ${response.status()} ${await response.text()}`);
    }
  }

  async expectProjectLimit(limit: number): Promise<void> {
    await expect(this.page.locator('p-inputnumber input')).toHaveValue(String(limit));
  }

  async expectStaleness(label: string): Promise<void> {
    await expect(this.page.locator('p-select')).toContainText(label);
  }

  async expectSuccessMessage(): Promise<void> {
    await expect(this.page.locator('app-settings p-message')).toBeVisible();
  }
}
