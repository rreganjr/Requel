#!/usr/bin/env node
//
// guide-screenshots.mjs — retake the user guide's screenshots (#377).
//
// Drives a running Requel 2.0 with Playwright and writes website/images/guide/*.png, so every
// release's guide shows the same data. It changes the data it runs against, so point it at a
// throwaway install, never a real one:
//
//   1. Start Requel on a new, empty database (see doc/guides/INSTALL.md) and wait for the first
//      start to finish.
//   2. From the repo root (requel-angular's node_modules must be installed):
//        node scripts/guide-screenshots.mjs
//
// Before the screenshots it sets the install up through the UI, skipping what is already done:
//   - imports doc/samples/Requel.xml (Projects -> Import);
//   - gives the user the manageApiTokens permission, so Settings shows the token section.
//
// Settings (environment variables):
//   REQUEL_URL       default http://localhost:8080
//   REQUEL_USER      default admin (needs System Admin)
//   REQUEL_PASSWORD  default admin
//   REQUEL_PROJECT   default Requel (the sample's name)
//   GUIDE_OUT        default website/images/guide
//   CHROMIUM_PATH    a Chromium binary, if Playwright's own browsers aren't installed
//
import { createRequire } from 'node:module';
import { mkdirSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const require = createRequire(path.join(root, 'requel-angular', 'package.json'));
const { chromium } = require('@playwright/test');

const BASE = process.env.REQUEL_URL ?? 'http://localhost:8080';
const USER = process.env.REQUEL_USER ?? 'admin';
const PASSWORD = process.env.REQUEL_PASSWORD ?? 'admin';
const PROJECT = process.env.REQUEL_PROJECT ?? 'Requel';
const OUT = path.resolve(root, process.env.GUIDE_OUT ?? 'website/images/guide');
const P = encodeURIComponent(PROJECT);

async function api(token, url) {
  const res = await fetch(`${BASE}/api${url}`, { headers: { Authorization: `Bearer ${token}` } });
  if (!res.ok) throw new Error(`GET ${url}: ${res.status}`);
  return res.json();
}

async function projectExists(token) {
  const res = await fetch(`${BASE}/api/projects/${P}`, { headers: { Authorization: `Bearer ${token}` } });
  return res.ok;
}

async function idOf(token, kind, name) {
  const list = await api(token, `/projects/${P}/${kind}`);
  const items = Array.isArray(list) ? list : (list.content ?? []);
  const hit = items.find(i => (i.name ?? '').startsWith(name));
  if (!hit) throw new Error(`no ${kind} named "${name}" in ${PROJECT}`);
  return hit.id;
}

const login = await fetch(`${BASE}/api/auth/login`, {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ username: USER, password: PASSWORD }),
});
if (!login.ok) throw new Error(`login as ${USER} failed: ${login.status}`);
const { token } = await login.json();

mkdirSync(OUT, { recursive: true });
const browser = await chromium.launch(process.env.CHROMIUM_PATH ? { executablePath: process.env.CHROMIUM_PATH } : {});
const page = await browser.newPage({ viewport: { width: 1440, height: 900 }, deviceScaleFactor: 1 });

// Not 'networkidle': the live-refresh event stream keeps a request open for good.
async function open(route) {
  await page.goto(`${BASE}${route}`, { waitUntil: 'load' });
  await page.locator('main').first().waitFor();
  await page.waitForTimeout(2000);  // let data, lazy panels and skeletons settle
}

// The UI reads the user's permissions at login, so setup logs in again after changing them.
async function logIn({ shoot = false } = {}) {
  await page.goto(`${BASE}/login`);
  await page.evaluate(() => { localStorage.clear(); sessionStorage.clear(); });
  await page.goto(`${BASE}/login`);
  if (shoot) await page.screenshot({ path: path.join(OUT, 'login.png') });
  await page.locator('#username').fill(USER);
  await page.locator('p-password input').fill(PASSWORD);
  await page.getByRole('button', { name: 'Login' }).click();
  await page.waitForURL(u => !u.pathname.endsWith('/login'));
}

await logIn({ shoot: true });

// Setup 1: the sample project, as a user who imports it sees it (its saved analysis, not re-run).
if (!(await projectExists(token))) {
  console.log(`importing doc/samples/Requel.xml as ${PROJECT}`);
  await open('/projects');
  const [chooser] = await Promise.all([
    page.waitForEvent('filechooser'),
    page.locator('main').getByRole('button', { name: 'Import' }).click(),
  ]);
  await chooser.setFiles(path.join(root, 'doc/samples/Requel.xml'));
  await page.getByText('Project imported successfully.').waitFor({ timeout: 120_000 });
  if (!(await projectExists(token))) throw new Error(`the import didn't create ${PROJECT}`);
}

// Setup 2: manageApiTokens, so Settings shows Personal Access Tokens.
await open(`/users/${encodeURIComponent(USER)}`);
const tokens = page.locator('[data-testid="user-role-group"] label.checkbox-label', { hasText: 'manageApiTokens' })
  .locator('input[type="checkbox"]');
if (!(await tokens.isChecked())) {
  console.log(`granting ${USER} manageApiTokens`);
  await tokens.check();
  await page.getByTestId('user-save').click();
  await page.waitForTimeout(2000);
  await logIn();
}

// Each shot: file name, route, and options. full: capture the whole page, not just the first
// screen. element (and hasText): capture only the first element that matches. before: something
// to do on the page first (never saved).
const perm = (type, op) => `stakeholder-perm-com.rreganjr.requel.project.${type}[${op}]`;
const shots = [
  { name: 'projects', route: '/projects' },
  { name: 'project-new', route: '/projects/new' },
  { name: 'project-overview', route: `/projects/${P}`, full: true },
  { name: 'goals', route: `/projects/${P}/goals` },
  { name: 'goal-editor', route: `/projects/${P}/goals/${await idOf(token, 'goals', 'Collaborative Elicitation')}`, full: true },
  { name: 'story-editor', route: `/projects/${P}/stories/${await idOf(token, 'stories', 'Rich creates a new project')}`, full: true },
  {
    // One issue with its positions: what resolving looks like.
    name: 'issue',
    route: `/projects/${P}/goals/${await idOf(token, 'goals', 'Automatted Assistance')}`,
    element: '[data-testid="annotation-issue"]',
    hasText: 'The word "Automatted"',
  },
  { name: 'use-case-editor', route: `/projects/${P}/use-cases/${await idOf(token, 'use-cases', 'A user creates a new project')}`, full: true },
  { name: 'scenario-editor', route: `/projects/${P}/scenarios/${await idOf(token, 'scenarios', 'Create a new project')}`, full: true },
  { name: 'terms', route: `/projects/${P}/terms` },
  { name: 'stakeholders', route: `/projects/${P}/stakeholders` },
  {
    name: 'stakeholder-permissions',
    route: `/projects/${P}/stakeholders/${await idOf(token, 'stakeholders', 'Builtin Project User')}`,
    full: true,
    // Tick Use case Edit and Delete (not saved) to show what comes with them.
    before: async page => {
      await page.getByTestId(perm('UseCase', 'Edit')).click();
      await page.getByTestId(perm('UseCase', 'Delete')).click();
    },
  },
  { name: 'open-issues', route: `/projects/${P}/open-issues` },
  { name: 'dictionary', route: `/projects/${P}/dictionary` },
  { name: 'definitions', route: `/projects/${P}/definitions` },
  { name: 'definition-editor', route: `/projects/${P}/definitions/ai-review-goal`, full: true },
  { name: 'project-settings', route: `/projects/${P}/edit`, full: true },
  { name: 'reports', route: `/projects/${P}/reports` },
  { name: 'user-editor', route: `/users/${encodeURIComponent(USER)}` },
  { name: 'account', route: '/account' },
  { name: 'settings', route: '/settings', full: true },
];


for (const { name, route, full, element, hasText, before } of shots) {
  await open(route);
  if (before) {
    await before(page);
    await page.waitForTimeout(500);
  }
  const file = path.join(OUT, `${name}.png`);
  if (element) {
    await page.locator(element, hasText ? { hasText } : {}).first().screenshot({ path: file });
  } else {
    await page.screenshot({ path: file, fullPage: !!full });
  }
  console.log(`${name}.png  ${route}`);
}

await browser.close();
console.log(`wrote ${shots.length + 1} screenshots to ${path.relative(root, OUT)}`);
