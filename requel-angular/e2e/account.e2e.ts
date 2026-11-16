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

  test('change password from /account → older sign-ins end, the new password works', async ({ browser, request }) => {
    const username = `e2e-pwd-${Date.now()}`;
    const initialPassword = 'InitialPass123!';
    const newPassword = 'NewPass456!';
    await createUser(request, username, 'Password Test User', initialPassword);

    // Log in as the new user
    const ctx1 = await browser.newContext();
    const pg1 = await ctx1.newPage();
    await pg1.goto('/login');
    await pg1.locator('#username').fill(username);
    await pg1.locator('p-password input').fill(initialPassword);
    await pg1.getByRole('button', { name: 'Login' }).click();
    await pg1.waitForURL('**/');
    const oldToken = await pg1.evaluate(() => localStorage.getItem('requel_token'));
    expect(oldToken).toBeTruthy();

    // #446: /account has no password rows, only a link to the Change Password page.
    await pg1.goto('/account');
    await pg1.waitForLoadState('domcontentloaded');
    await expect(pg1.locator('#password')).toHaveCount(0);
    await pg1.getByTestId('account-change-password').click();
    await pg1.waitForURL('**/change-password');

    // Tokens carry their issue time to the second; make sure the change is in a later one.
    await pg1.waitForTimeout(1100);

    // p-password needs pressSequentially to fire per-char input events for Angular's CVA
    for (const [id, value] of [['currentPassword', initialPassword], ['newPassword', newPassword],
                               ['confirmPassword', newPassword]] as const) {
      const input = pg1.locator(`#${id}`);
      await input.focus();
      await input.pressSequentially(value);
    }
    const [res] = await Promise.all([
      pg1.waitForResponse(r => r.url().includes('/api/auth/password')),
      pg1.getByTestId('change-password-submit').click(),
    ]);
    if (!res.ok()) throw new Error(`Password change failed: ${res.status()} ${await res.text()}`);
    await pg1.waitForURL(url => !url.pathname.endsWith('/change-password'));
    await ctx1.close();

    // The token from before the change no longer works.
    const me = await request.get('/api/auth/me', { headers: { Authorization: `Bearer ${oldToken}` } });
    expect(me.status()).toBe(401);

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

    await page.getByTestId('account-save').click();
    await page.waitForLoadState('domcontentloaded');

    // Save marks form pristine and shows success message; the field retains the new value
    // Note: reloading shows the old name because edit-account doesn't update authService.user()
    await expect(page.locator('p-message').first()).toBeVisible({ timeout: 5000 });
    await expect(nameInput).toHaveValue(newName);

    await page.close();
  });

  test('save validation failure shows error banner with violation messages', async ({ adminContext }) => {
    const page = await adminContext.newPage();

    await page.route('**/api/commands/EditUser', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: false,
          entityType: 'EditUser',
          entity: null,
          error: null,
          violations: [
            { message: 'Email is invalid' },
            { message: 'Phone is required' },
          ],
        }),
      });
    });

    await page.goto('/account');
    await page.waitForLoadState('domcontentloaded');

    const nameInput = page.locator('#name');
    await nameInput.clear();
    await nameInput.fill('triggers-validation-failure');

    await page.getByTestId('account-save').click();

    await expect(page.getByTestId('account-editor-error'))
      .toContainText('Email is invalid; Phone is required');

    await page.close();
  });

  test('save with generic error response shows error banner', async ({ adminContext }) => {
    const page = await adminContext.newPage();

    await page.route('**/api/commands/EditUser', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: false,
          entityType: 'EditUser',
          entity: null,
          error: 'Server rejected the change.',
          violations: null,
        }),
      });
    });

    await page.goto('/account');
    await page.waitForLoadState('domcontentloaded');

    const nameInput = page.locator('#name');
    await nameInput.clear();
    await nameInput.fill('triggers-generic-error');

    await page.getByTestId('account-save').click();

    await expect(page.getByTestId('account-editor-error'))
      .toContainText('Server rejected the change.');

    await page.close();
  });

  test('save network failure shows fallback error message', async ({ adminContext }) => {
    const page = await adminContext.newPage();

    await page.route('**/api/commands/EditUser', async route => {
      await route.abort('failed');
    });

    await page.goto('/account');
    await page.waitForLoadState('domcontentloaded');

    const nameInput = page.locator('#name');
    await nameInput.clear();
    await nameInput.fill('triggers-network-failure');

    await page.getByTestId('account-save').click();

    // catch(err) sets either err.message or 'Save failed.' — both are acceptable
    await expect(page.getByTestId('account-editor-error')).toBeVisible({ timeout: 5000 });

    await page.close();
  });

  test('dirty account form prompts confirm dialog on navigate away; dismiss stays on /account', async ({ adminContext }) => {
    const page = await adminContext.newPage();

    await page.goto('/account');
    await page.waitForLoadState('domcontentloaded');

    // Mark the form dirty
    const nameInput = page.locator('#name');
    await nameInput.clear();
    await nameInput.fill('unsaved-account-change');

    // Dismiss the confirm() dialog so navigation is cancelled
    page.once('dialog', dialog => dialog.dismiss());
    await page.getByTestId('header-brand').click();

    // Guard returned false — still on /account
    await expect(page).toHaveURL(/\/account$/);

    await page.close();
  });

});
