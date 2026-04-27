import { test, expect } from './fixtures/auth';
import { getUserName, setUserName, createUser } from './fixtures/api-helper';
import { LoginPage } from './pages/LoginPage';

// The admin user's name before the test — restored in afterEach via API
const ADMIN_USERNAME = process.env['E2E_ADMIN_USERNAME'] ?? 'admin';
let originalName = '';

test.beforeEach(async ({ request }) => {
  originalName = await getUserName(request, ADMIN_USERNAME);
});

test.afterEach(async ({ request }) => {
  if (originalName) {
    try {
      await setUserName(request, ADMIN_USERNAME, originalName);
    } catch {
      // best-effort restore
    }
  }
});

test.describe('Edit account', () => {

  test('username is pre-filled and disabled on /account', async ({ adminContext }) => {
    const page = await adminContext.newPage();

    await page.goto('/account');
    await page.waitForLoadState('domcontentloaded');

    await expect(page.locator('#username')).toBeDisabled();
    await expect(page.locator('#username')).toHaveValue(ADMIN_USERNAME);

    await page.close();
  });

  test('change password → can log back in with new password', async ({ browser, request }) => {
    const username = `e2e-pwd-${Date.now()}`;
    const initialPassword = 'InitialPass123!';
    const newPassword = 'NewPass456!';
    await createUser(request, username, 'Password Test User', initialPassword);

    // Log in as the new user and change the password via /account
    const ctx1 = await browser.newContext();
    const pg1 = await ctx1.newPage();
    await pg1.goto('/login');
    await pg1.locator('#username').fill(username);
    await pg1.locator('p-password input').fill(initialPassword);
    await pg1.getByRole('button', { name: 'Login' }).click();
    await pg1.waitForURL('**/');

    await pg1.goto('/account');
    await pg1.waitForLoadState('domcontentloaded');

    // p-password needs pressSequentially to fire per-char input events for Angular's CVA
    const pwInput = pg1.locator('#password').locator('input');
    await pwInput.focus();
    await pwInput.pressSequentially(newPassword);
    const rpwInput = pg1.locator('#repassword').locator('input');
    await rpwInput.focus();
    await rpwInput.pressSequentially(newPassword);

    const [res] = await Promise.all([
      pg1.waitForResponse(r => r.url().includes('/api/commands/EditUser')),
      pg1.getByRole('button', { name: 'Save' }).click(),
    ]);
    if (!res.ok()) throw new Error(`EditUser failed: ${res.status()} ${await res.text()}`);
    await ctx1.close();

    // Verify: fresh context, log in with the new password
    const ctx2 = await browser.newContext();
    const loginPage = new LoginPage(await ctx2.newPage());
    await loginPage.goto();
    await loginPage.login(username, newPassword);
    await loginPage.expectRedirectedToDashboard();
    await ctx2.close();
  });

  test('change name → save succeeds and field reflects new value', async ({ adminContext }) => {
    const newName = `Admin E2E ${Date.now()}`;
    const page = await adminContext.newPage();

    await page.goto('/account');
    await page.waitForLoadState('domcontentloaded');

    const nameInput = page.locator('#name');
    await nameInput.clear();
    await nameInput.fill(newName);

    await page.getByRole('button', { name: 'Save' }).click();
    await page.waitForLoadState('domcontentloaded');

    // Save marks form pristine and shows success message; the field retains the new value
    // Note: reloading shows the old name because edit-account doesn't update authService.user()
    await expect(page.locator('p-message').first()).toBeVisible({ timeout: 5000 });
    await expect(nameInput).toHaveValue(newName);

    await page.close();
  });

});
