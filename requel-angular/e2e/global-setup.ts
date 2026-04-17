import { chromium, request } from '@playwright/test';
import * as fs from 'fs';
import * as path from 'path';

const BASE_URL = 'http://localhost:8080';
const AUTH_DIR = path.join(__dirname, '.auth');

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

  const adminUser = process.env['E2E_ADMIN_USERNAME'] ?? 'admin';
  const adminPass = process.env['E2E_ADMIN_PASSWORD'] ?? 'admin';
  const projectUser = process.env['E2E_PROJECT_USERNAME'] ?? 'project';
  const projectPass = process.env['E2E_PROJECT_PASSWORD'] ?? 'project';

  await saveAuthState(adminUser, adminPass);
  await saveAuthState(projectUser, projectPass);
}
