import { test, expect, Page } from '@playwright/test';
import { createUser } from './fixtures/api-helper';

/**
 * #381: a user who must change their password (here, one an admin created with the flag set, so
 * the shared admin account is never touched) lands on the change-password page at sign-in, can go
 * nowhere else, and gets in normally once it is changed.
 */

/** p-password needs per-character input events for Angular's value accessor (see account.e2e). */
async function type(page: Page, id: string, value: string): Promise<void> {
  const input = page.locator(`#${id}`);
  await input.fill('');
  await input.focus();
  await input.pressSequentially(value);
}

async function signIn(page: Page, username: string, password: string): Promise<void> {
  await page.goto('/login');
  await page.locator('#username').fill(username);
  await page.locator('p-password input').fill(password);
  await page.getByRole('button', { name: 'Login' }).click();
}

test.describe('Forced password change (#381)', () => {
  test('a flagged user must change their password before anything else', async ({ browser, request }) => {
    const username = `e2e-pwc-${Date.now()}`;
    const first = 'FirstPass123!';
    const second = 'SecondPass456!';
    await createUser(request, username, 'Must Change User', first, 'ProjectUserRole', true);

    const ctx = await browser.newContext();
    const page = await ctx.newPage();

    await signIn(page, username, first);
    await page.waitForURL('**/change-password');
    await expect(page.getByRole('heading', { level: 1, name: 'Change your password' })).toBeVisible();
    await expect(page.getByTestId('change-password-required')).toBeVisible();

    // Anywhere else sends the user back.
    await page.goto('/projects');
    await page.waitForURL('**/change-password');

    // The same password is refused on the new-password field.
    await type(page, 'currentPassword', first);
    await type(page, 'newPassword', first);
    await type(page, 'confirmPassword', first);
    await page.getByTestId('change-password-submit').click();
    await expect(page.getByText('New password must be different from the current password.')).toBeVisible();
    await expect(page).toHaveURL(/\/change-password$/);

    // A wrong current password is refused on its field.
    await type(page, 'currentPassword', 'not-my-password');
    await type(page, 'newPassword', second);
    await type(page, 'confirmPassword', second);
    await page.getByTestId('change-password-submit').click();
    await expect(page.getByText('Current password is incorrect.')).toBeVisible();

    // The right one goes through to the dashboard.
    await type(page, 'currentPassword', first);
    await page.getByTestId('change-password-submit').click();
    await page.waitForURL(url => !url.pathname.endsWith('/change-password'));
    await page.goto('/projects');
    await expect(page).toHaveURL(/\/projects$/);
    await ctx.close();

    // The new password signs in cleanly, straight past the change page.
    const ctx2 = await browser.newContext();
    const page2 = await ctx2.newPage();
    await signIn(page2, username, second);
    await page2.waitForURL(url => !url.pathname.endsWith('/login'));
    await expect(page2).not.toHaveURL(/\/change-password$/);
    await ctx2.close();
  });

  test('sign out from the change-password page returns to login', async ({ browser, request }) => {
    const username = `e2e-pwc-out-${Date.now()}`;
    await createUser(request, username, 'Must Change User', 'FirstPass123!', 'ProjectUserRole', true);

    const ctx = await browser.newContext();
    const page = await ctx.newPage();
    await signIn(page, username, 'FirstPass123!');
    await page.waitForURL('**/change-password');
    await page.getByTestId('change-password-sign-out').click();
    await page.waitForURL('**/login');
    await ctx.close();
  });
});
