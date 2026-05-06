import { test, expect } from './fixtures/auth';
import { LoginPage } from './pages/LoginPage';

// Auth tests that require a fresh (unauthenticated) browser context use Playwright's
// built-in `page` fixture rather than the pre-authenticated admin/project contexts.

test.describe('Authentication', () => {

  test('valid credentials redirect to projects page', async ({ browser }) => {
    const ctx = await browser.newContext();
    const page = await ctx.newPage();
    const loginPage = new LoginPage(page);

    await loginPage.goto();
    await loginPage.login('admin', 'admin');
    await loginPage.expectRedirectedToDashboard();

    await ctx.close();
  });

  test('bad credentials show error, no navigation', async ({ browser }) => {
    const ctx = await browser.newContext();
    const page = await ctx.newPage();
    const loginPage = new LoginPage(page);

    await loginPage.goto();
    await loginPage.login('admin', 'wrongpassword');
    await loginPage.expectError('Login failed');
    await expect(page).toHaveURL(/\/login/);

    await ctx.close();
  });

  test('accessing /projects while logged out redirects to /login', async ({ browser }) => {
    const ctx = await browser.newContext(); // no storageState → no token
    const page = await ctx.newPage();

    await page.goto('/projects');
    await page.waitForURL(/\/login/);
    await expect(page).toHaveURL(/\/login/);

    await ctx.close();
  });

  test('admin can see admin section in sidebar', async ({ adminContext }) => {
    const page = await adminContext.newPage();
    await page.goto('/projects');
    await page.waitForLoadState('domcontentloaded');

    await expect(page.getByRole('link', { name: 'List users' })).toBeVisible();
    await page.close();
  });

  test('project user cannot see admin section in sidebar', async ({ projectContext }) => {
    const page = await projectContext.newPage();
    await page.goto('/projects');
    await page.waitForLoadState('domcontentloaded');

    await expect(page.getByRole('link', { name: 'List users' })).not.toBeVisible();
    await page.close();
  });

  // Simulates a mid-session JWT expiry: the client still holds a token in localStorage
  // (so the auth guard lets navigation through), but the server has invalidated it and
  // returns 401. The auth interceptor should detect the 401, call AuthService.logout(),
  // and the resulting /login navigation should be observable in the URL.
  //
  // Covers both the "JWT expiry → subsequent API call redirects to login" row in the
  // Authentication section of RELEASE_20_TEST_PLAN.md and the equivalent
  // "Token expires mid-session" row in the Forbidden-state UX section — they describe
  // the same scenario.
  test('expired JWT on API call triggers interceptor logout and redirects to /login', async ({ adminContext }) => {
    const page = await adminContext.newPage();

    // First load /projects normally so we know the test exercises a mid-session
    // expiry rather than a fresh-load failure.
    await page.goto('/projects');
    await page.waitForLoadState('domcontentloaded');
    await expect(page).toHaveURL(/\/projects$/);

    // Now intercept any subsequent API call (other than /auth/login, which the
    // interceptor explicitly excludes from logout) and return 401 to simulate the
    // server having invalidated the token.
    await page.route('**/api/**', async route => {
      const url = route.request().url();
      if (url.includes('/api/auth/login')) {
        await route.fallback();
        return;
      }
      await route.fulfill({
        status: 401,
        contentType: 'application/json',
        body: JSON.stringify({ error: 'Token expired' }),
      });
    });

    // Trigger an API call by reloading the project list. The new GET /api/projects
    // hits the mock, returns 401, the interceptor calls logout(), and the user is
    // redirected to /login.
    await page.reload();

    await page.waitForURL(/\/login/, { timeout: 10000 });
    await expect(page).toHaveURL(/\/login/);

    // The interceptor's logout() also clears the stored token from localStorage.
    const storedToken = await page.evaluate(() => localStorage.getItem('requel_token'));
    expect(storedToken).toBeNull();

    await page.close();
  });

});
