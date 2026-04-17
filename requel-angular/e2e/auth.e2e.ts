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

});
