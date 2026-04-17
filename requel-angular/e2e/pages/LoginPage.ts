import { Page, expect } from '@playwright/test';

export class LoginPage {
  constructor(private page: Page) {}

  async goto(): Promise<void> {
    await this.page.goto('/login');
  }

  async login(username: string, password: string): Promise<void> {
    await this.page.locator('#username').fill(username);
    // p-password renders an inner <input> inside the component
    await this.page.locator('p-password input').fill(password);
    await this.page.getByRole('button', { name: 'Login' }).click();
  }

  async expectError(message: string): Promise<void> {
    await expect(this.page.locator('p-message')).toContainText(message);
  }

  async expectRedirectedToDashboard(): Promise<void> {
    // Login navigates to '/' which renders the DashboardComponent
    await expect(this.page.getByRole('heading', { name: /Welcome/ })).toBeVisible();
  }
}
