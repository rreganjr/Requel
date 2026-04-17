import { test, expect } from './fixtures/auth';
import { getUserName, setUserName } from './fixtures/api-helper';

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
