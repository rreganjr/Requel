import { test, expect } from './fixtures/auth';

/**
 * Access-control tests.
 *
 * Unauthenticated tests create a fresh browser context with no storage state
 * (no JWT in localStorage) and verify the authGuard redirects to /login.
 *
 * The /users route has no admin-role guard — any authenticated user can reach
 * the user list. The component itself has no admin check, so a project user
 * sees the full table rather than a redirect or error.
 */
test.describe('Access control', () => {

  test('unauthenticated access to /projects redirects to /login', async ({ browser }) => {
    const ctx = await browser.newContext(); // no storage state = unauthenticated
    const page = await ctx.newPage();

    await page.goto('/projects');
    await expect(page).toHaveURL(/\/login/);

    await ctx.close();
  });

  test('unauthenticated access to /account redirects to /login', async ({ browser }) => {
    const ctx = await browser.newContext();
    const page = await ctx.newPage();

    await page.goto('/account');
    await expect(page).toHaveURL(/\/login/);

    await ctx.close();
  });

  test('project user can access /users — no admin route guard', async ({ projectContext }) => {
    const page = await projectContext.newPage();

    await page.goto('/users');
    await page.waitForLoadState('domcontentloaded');

    // The user list component renders and loads data — no redirect
    await expect(page).not.toHaveURL(/\/login/);
    await expect(page.locator('p-table')).toBeVisible();

    await page.close();
  });

});
