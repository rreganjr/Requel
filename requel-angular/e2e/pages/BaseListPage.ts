import { Locator, Page, expect } from '@playwright/test';

export abstract class BaseListPage {
  protected constructor(protected readonly page: Page) {}

  protected async gotoList(url: string, responseUrlFragment: string): Promise<void> {
    await Promise.all([
      this.page.waitForResponse(r => r.url().includes(responseUrlFragment) && r.status() === 200),
      this.page.goto(url),
    ]);
  }

  protected async searchWithPlaceholder(placeholder: string, text: string): Promise<void> {
    const input = this.page.getByPlaceholder(placeholder);
    await input.clear();
    await input.fill(text);
  }

  protected async clickNewButton(buttonName: string, targetUrl: string): Promise<void> {
    await this.page.locator('app-list-page').getByRole('button', { name: buttonName }).click();
    await this.page.waitForURL(targetUrl);
  }

  protected tableRowsWithText(text: string): Locator {
    return this.page.locator('p-table tbody tr', { hasText: text });
  }

  protected async clickTableRow(text: string, targetUrl: string | RegExp, readySelector = '#name'): Promise<void> {
    const row = this.tableRowsWithText(text).first();
    await expect(row).toBeVisible();
    await row.click();
    await this.page.waitForURL(targetUrl);
    await expect(this.page.locator(readySelector)).not.toHaveValue('');
  }

  protected async expectTableRowVisible(text: string): Promise<void> {
    await expect(this.tableRowsWithText(text).first()).toBeVisible();
  }

  protected async expectTableRowNotVisible(text: string): Promise<void> {
    await expect(this.tableRowsWithText(text)).toHaveCount(0);
  }

  protected async countTableRows(text: string): Promise<number> {
    return this.tableRowsWithText(text).count();
  }
}
