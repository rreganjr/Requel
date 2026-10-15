import { test, expect } from './fixtures/auth';

/**
 * Access-control tests.
 *
 * Unauthenticated tests create a fresh browser context with no storage state
 * (no JWT in localStorage) and verify the authGuard redirects to /login.
 *
 * Admin-route tests use a project user (no SystemAdminUserRole) and verify
 * the adminGuard bounces them off `/users` and `/users/:username` to the
 * dashboard `/`. Non-admins remain authenticated, so the redirect target
 * is intentionally `/`, not `/login`.
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

  test('project user navigating to /users is bounced to dashboard by adminGuard', async ({ projectContext, baseURL }) => {
    const page = await projectContext.newPage();

    await page.goto('/users');
    await page.waitForLoadState('domcontentloaded');

    // adminGuard returns a UrlTree(['/']) for non-admins, so the router
    // navigates to the dashboard. The user-list <p-table> must NOT render.
    await expect(page).not.toHaveURL(/\/users/);
    await expect(page).not.toHaveURL(/\/login/);
    expect(new URL(page.url()).pathname).toBe('/');

    await page.close();
  });

  test('project user navigating to /users/:username is bounced to dashboard by adminGuard', async ({ projectContext }) => {
    const page = await projectContext.newPage();

    // The username does not need to exist — the guard fires before the
    // component loads, so we never hit the user-editor at all.
    await page.goto('/users/admin');
    await page.waitForLoadState('domcontentloaded');

    await expect(page).not.toHaveURL(/\/users/);
    await expect(page).not.toHaveURL(/\/login/);
    expect(new URL(page.url()).pathname).toBe('/');

    await page.close();
  });

});
