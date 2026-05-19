import { chromium, request } from '@playwright/test';
import * as fs from 'fs';
import * as path from 'path';

const BASE_URL = 'http://localhost:8080';
const AUTH_DIR = path.join(__dirname, '.auth');

/**
 * Calls POST /api/dev/reset-admin to ensure admin's password is reset to the
 * default before the test suite runs. This endpoint is only registered when
 * the server is started with --requel.dev.reset-admin.enabled=true; if the
 * endpoint is absent (404) the call is silently skipped.
 */
async function resetAdminPassword(): Promise<void> {
  const ctx = await request.newContext({ baseURL: BASE_URL });
  try {
    const res = await ctx.post('/api/dev/reset-admin');
    if (res.status() === 404) {
      console.warn('[global-setup] /api/dev/reset-admin not available — start the server with --spring.profiles.active=dev (or --requel.dev.reset-admin.enabled=true) to auto-reset admin credentials before each run');
    }
  } finally {
    await ctx.dispose();
  }
}

/**
 * Calls POST /api/dev/reset-project to ensure the project user is in its
 * canonical state before the test suite runs (name, password, and crucially
 * roles = [ProjectUserRole] only). Guards against state drift that breaks
 * adminGuard tests when the project user has inadvertently been granted
 * SystemAdminUserRole on a developer's local database. Only registered when
 * the server is started with --requel.dev.reset-project.enabled=true; if the
 * endpoint is absent (404) the call is silently skipped.
 */
async function resetProjectUser(): Promise<void> {
  const ctx = await request.newContext({ baseURL: BASE_URL });
  try {
    const res = await ctx.post('/api/dev/reset-project');
    if (res.status() === 404) {
      console.warn('[global-setup] /api/dev/reset-project not available — start the server with --spring.profiles.active=dev (or --requel.dev.reset-project.enabled=true) to auto-reset the project user to canonical state before each run');
    }
  } finally {
    await ctx.dispose();
  }
}

async function saveAuthState(username: string, password: string): Promise<void> {
  // Use the API to obtain a JWT — no UI login needed
  const ctx = await request.newContext({ baseURL: BASE_URL });
  const res = await ctx.post('/api/auth/login', {
    data: { username, password },
  });
  if (!res.ok()) {
    throw new Error(`Login failed for ${username}: ${res.status()} ${await res.text()}`);
  }
  const { token, user } = await res.json();
  await ctx.dispose();

  // Persist token + user in localStorage via a headless browser page
  const browser = await chromium.launch();
  const page = await browser.newPage();
  await page.goto(BASE_URL);
  await page.evaluate(
    ({ token, user }: { token: string; user: unknown }) => {
      localStorage.setItem('requel_token', token);
      localStorage.setItem('requel_user', JSON.stringify(user));
    },
    { token, user }
  );
  const statePath = path.join(AUTH_DIR, `${username}.json`);
  await page.context().storageState({ path: statePath });
  await browser.close();
}

export default async function globalSetup(): Promise<void> {
  fs.mkdirSync(AUTH_DIR, { recursive: true });

  // Reset built-in users to canonical state before capturing storage state.
  // Order matters: resetAdminPassword first (so admin can authenticate the
  // subsequent project reset, since resetProjectUser uses admin as editedBy
  // server-side), then resetProjectUser.
  await resetAdminPassword();
  await resetProjectUser();

  const adminUser = process.env['E2E_ADMIN_USERNAME'] ?? 'admin';
  const adminPass = process.env['E2E_ADMIN_PASSWORD'] ?? 'admin';
  const projectUser = process.env['E2E_PROJECT_USERNAME'] ?? 'project';
  const projectPass = process.env['E2E_PROJECT_PASSWORD'] ?? 'project';

  await saveAuthState(adminUser, adminPass);
  await saveAuthState(projectUser, projectPass);
}
