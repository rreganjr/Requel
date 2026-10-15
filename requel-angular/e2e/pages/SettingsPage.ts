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

  /**
   * Save is `[disabled]="form.invalid || form.pristine || saving()"` and `data-testid` sits on
   * the `p-button` host, not the inner control -- so clicking the host while the inner button
   * is disabled silently no-ops and `waitForResponse` then burns the entire test timeout with
   * a useless "target closed" trace. Assert enabled first so an unchanged form says so.
   */
  private async clickAndAwaitPut(testId: string): Promise<void> {
    const button = this.page.getByTestId(testId).locator('button');
    await expect(button, `${testId} is disabled -- the form is pristine or invalid`)
      .toBeEnabled({ timeout: 10_000 });
    const [response] = await Promise.all([
      this.page.waitForResponse(r =>
        r.url().includes('/user-preferences') && r.request().method() === 'PUT'
      ),
      button.click({ timeout: 10_000 }),
    ]);
    if (!response.ok()) {
      throw new Error(
        `PUT /user-preferences (${testId}) failed: ${response.status()} ${await response.text()}`
      );
    }
  }

  async save(): Promise<void> {
    await this.clickAndAwaitPut('settings-save');
  }

  async resetToDefaults(): Promise<void> {
    await this.clickAndAwaitPut('settings-reset');
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
