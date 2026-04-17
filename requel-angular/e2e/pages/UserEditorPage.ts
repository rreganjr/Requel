import { Page, expect } from '@playwright/test';

export class UserListPage {
  constructor(private page: Page) {}

  async goto(): Promise<void> {
    await Promise.all([
      this.page.waitForResponse(r => r.url().includes('/api/users') && r.status() === 200),
      this.page.goto('/users'),
    ]);
  }

  async searchFor(text: string): Promise<void> {
    const input = this.page.getByPlaceholder('Search users...');
    await input.clear();
    await input.fill(text);
  }

  async clickNewUser(): Promise<void> {
    await this.page.locator('app-list-page').getByRole('button', { name: 'New User' }).click();
    await this.page.waitForURL('**/users/new');
  }

  async clickUser(username: string): Promise<void> {
    await this.searchFor(username);
    // PrimeNG table rows have an accessible name built from all their cell content.
    // The username is always the first column, so /^username/ matches only that row.
    // Waiting for visibility here also ensures the filter has applied before we click.
    const row = this.page.getByRole('row', { name: new RegExp(`^${username}`, 'i') });
    await expect(row).toBeVisible({ timeout: 5000 });
    await row.click();
    await this.page.waitForURL(`**/users/${encodeURIComponent(username)}`);
    await expect(this.page.locator('#name')).toBeVisible();
  }

  async expectUserInTable(username: string): Promise<void> {
    await this.searchFor(username);
    await expect(
      this.page.getByRole('row', { name: new RegExp(`^${username}`, 'i') })
    ).toBeVisible();
  }
}

export class UserEditorPage {
  constructor(private page: Page) {}

  async fillUsername(username: string): Promise<void> {
    await this.page.locator('#username').fill(username);
  }

  async fillName(name: string): Promise<void> {
    const input = this.page.locator('#name');
    await input.clear();
    await input.fill(name);
  }

  async fillEmail(email: string): Promise<void> {
    await this.page.locator('#email').fill(email);
  }

  async fillOrganization(org: string): Promise<void> {
    // p-select with [editable]="true": fill() opens the dropdown. Clicking the matching
    // option selects it and closes the dropdown in one step. Falls back to Tab if no exact
    // match exists (new environment where the org hasn't been created yet).
    await this.page.locator('input[name="org"]').fill(org);
    const option = this.page.getByRole('option', { name: org, exact: true });
    if (await option.count() > 0) {
      await option.click();
    } else {
      await this.page.keyboard.press('Tab');
    }
  }

  async fillPassword(password: string): Promise<void> {
    // p-password wraps the real <input>. fill() sets the value directly but does not
    // fire per-character input events that PrimeNG's onInput/onModelChange requires.
    // pressSequentially types one character at a time, firing keydown+input+keyup per
    // char so the ControlValueAccessor propagates the value to the Angular signal.
    // focus() is used instead of click() so that an open p-select dropdown (from org field)
    // does not intercept the action — focus() calls element.focus() directly, bypassing
    // Playwright's overlay intercept check.
    const pwInput = this.page.locator('#password').locator('input');
    await pwInput.focus();
    await pwInput.pressSequentially(password);
    const rpwInput = this.page.locator('#repassword').locator('input');
    await rpwInput.focus();
    await rpwInput.pressSequentially(password);
  }

  /**
   * Click the role checkbox by its displayName label.
   * Role displayNames: ProjectUserRole → "Project", SystemAdminUserRole → "System Admin"
   * Waits for the roles section to be visible first — roles are loaded async in loadData().
   */
  async selectRole(displayName: string | RegExp): Promise<void> {
    const label = this.page.locator('.checkbox-label', { hasText: displayName });
    await expect(label).toBeVisible({ timeout: 5000 });
    await label.click();
  }

  async save(): Promise<void> {
    // Wait for the EditUser command to complete before returning. Without this,
    // waitForLoadState('domcontentloaded') resolves immediately (no page navigation
    // for existing-user edits), and a subsequent page.reload() races the API call.
    await Promise.all([
      this.page.waitForResponse(r => r.url().includes('/api/commands/EditUser')),
      this.page.getByRole('button', { name: 'Save' }).click(),
    ]);
  }

  async expectNameValue(name: string): Promise<void> {
    await expect(this.page.locator('#name')).toHaveValue(name);
  }

  async expectUsernameValue(username: string): Promise<void> {
    await expect(this.page.locator('#username')).toHaveValue(username);
  }

  async expectUsernameDisabled(): Promise<void> {
    await expect(this.page.locator('#username')).toBeDisabled();
  }
}
