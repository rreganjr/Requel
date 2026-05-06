import { test, expect } from './fixtures/auth';
import { createUser, setUserName } from './fixtures/api-helper';
import { UserListPage, UserEditorPage } from './pages/UserEditorPage';
import { LoginPage } from './pages/LoginPage';
import { reloadAndWaitForGet } from './helpers/navigation';

// The pre-seeded project user used for the rename test; name is restored in afterEach via API
const PROJECT_USERNAME = 'project';
const PROJECT_ORIGINAL_NAME = 'Builtin Project User';

// Set to true when the rename test changes the project user's name; cleared in afterEach
let projectNameWasChanged = false;

// Users accumulate (no DeleteUser command); use e2e-user-* + timestamp prefix
test.describe('Admin user management', () => {

  test.afterEach(async ({ request }) => {
    if (projectNameWasChanged) {
      try {
        await setUserName(request, PROJECT_USERNAME, PROJECT_ORIGINAL_NAME);
      } catch {
        // best-effort restore
      }
      projectNameWasChanged = false;
    }
  });

  test('admin navigates to /users → user list visible with admin in table', async ({ adminContext }) => {
    const page = await adminContext.newPage();
    const listPage = new UserListPage(page);

    await listPage.goto();
    await listPage.expectUserInTable('admin');

    await page.close();
  });

  test('admin creates new user → appears in user list', async ({ adminContext }) => {
    const username = `e2e-user-${Date.now()}-${Math.random().toString(36).slice(2, 12)}`;
    const page = await adminContext.newPage();
    const listPage = new UserListPage(page);
    const editorPage = new UserEditorPage(page);

    await listPage.goto();
    await listPage.clickNewUser();

    // Wait for loadData() to finish — roles are fetched async; ngModel bindings aren't
    // active until the component is fully initialized
    await expect(page.getByTestId('user-roles-section')).toBeVisible();

    await editorPage.fillUsername(username);
    await editorPage.fillName('E2E Test User');
    await editorPage.fillEmail(`${username}@example.com`);
    await editorPage.fillOrganization('E2E Test Org');
    await editorPage.fillPassword('e2ePass123!');
    // /Project/ matches the displayName for ProjectUserRole
    await editorPage.selectRole(/Project/);
    await editorPage.save();

    // After a successful save the component navigates to /users/:username
    await page.waitForURL(`**/users/${username}`, { timeout: 10000 });
    await listPage.goto();
    await listPage.expectUserInTable(username);

    await page.close();
  });

  test('newly created user can log in', async ({ browser, request }) => {
    const username = `e2e-user-login-${Date.now()}`;
    const password = 'e2eLoginTest123!';
    await createUser(request, username, 'Login Test User', password);

    const ctx = await browser.newContext();
    const page = await ctx.newPage();
    const loginPage = new LoginPage(page);

    await loginPage.goto();
    await loginPage.login(username, password);
    await loginPage.expectRedirectedToDashboard();

    await ctx.close();
  });

  test('newly created project user cannot see admin controls', async ({ browser, request }) => {
    const username = `e2e-user-noadmin-${Date.now()}`;
    const password = 'e2eNoAdmin123!';
    await createUser(request, username, 'No Admin User', password, 'ProjectUserRole');

    const ctx = await browser.newContext();
    const page = await ctx.newPage();
    const loginPage = new LoginPage(page);

    await loginPage.goto();
    await loginPage.login(username, password);
    await loginPage.expectRedirectedToDashboard();

    // Admin-only navigation link must not be visible to a project-only user
    await expect(page.getByRole('link', { name: 'List users' })).not.toBeVisible();

    await ctx.close();
  });

  test('admin changes user password → user can log in with new password', async ({ adminContext, browser, request }) => {
    const username = `e2e-pwd-admin-${Date.now()}`;
    const initialPassword = 'InitialAdminPass123!';
    const newPassword = 'NewAdminPass456!';
    await createUser(request, username, 'Admin Password Test', initialPassword);

    const page = await adminContext.newPage();
    const listPage = new UserListPage(page);
    const editorPage = new UserEditorPage(page);

    await listPage.goto();
    await listPage.clickUser(username);
    await editorPage.fillPassword(newPassword);
    await editorPage.save();
    await page.close();

    // Verify: fresh unauthenticated context, log in with the new password
    const ctx = await browser.newContext();
    const loginPage = new LoginPage(await ctx.newPage());
    await loginPage.goto();
    await loginPage.login(username, newPassword);
    await loginPage.expectRedirectedToDashboard();
    await ctx.close();
  });

  test('admin edits user name → persists after reload', async ({ adminContext }) => {
    // Use the pre-seeded "project" user — avoids the 409 that the createUser API helper
    // was hitting (likely from phoneNumber/organizationName constraint differences)
    const newName = `Project User Renamed ${Date.now()}`;
    const page = await adminContext.newPage();
    const listPage = new UserListPage(page);
    const editorPage = new UserEditorPage(page);

    await listPage.goto();
    await listPage.clickUser(PROJECT_USERNAME);

    await editorPage.fillName(newName);
    await editorPage.save();

    // Signal to afterEach that we need to restore the original name
    projectNameWasChanged = true;

    // Filter to the user-specific GET — /api/users/roles and /api/users/organizations
    // also match '/api/users/' and fire before the user GET, causing the old pattern
    // to resolve Promise.all before the form is populated.
    // waitUntil:'domcontentloaded' returns before Angular bootstraps so the listener
    // is guaranteed to be active before Angular fires its GET /api/users/:username.
    await reloadAndWaitForGet(page, r => r.url().includes(`/api/users/${PROJECT_USERNAME}`));
    await editorPage.expectNameValue(newName);

    await page.close();
  });

});
