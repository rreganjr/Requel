import { test, Browser, BrowserContext, Page, APIRequestContext } from '@playwright/test';
import * as fs from 'fs';
import * as path from 'path';

/**
 * #519: the UI review walk. Not an e2e test: it asserts nothing. It visits every route in four
 * views (light/dark x wide/narrow), runs axe on each, and writes to ui-review/:
 *   <page>--<state>--<view>.png   a full-page screenshot
 *   axe/<page>--<state>--<view>.json   axe's violations
 *   summary.md   every page's violations by impact, and the pages it couldn't reach
 *
 * Data: doc/samples/Requel.xml imported fresh (every entity type, so every detail route has a
 * target), a new empty project for the list pages' empty states, and a detail id that doesn't
 * exist for the error state. Both projects are deleted at the end.
 *
 * Run with the app up on :8080 with the dev profile:  npm run ui-review
 * Narrow the run with UI_REVIEW_ONLY=<substring of a page name>, e.g. UI_REVIEW_ONLY=goal.
 */

const BASE_URL = 'http://localhost:8080';
const OUT = path.resolve(__dirname, '..', '..', 'ui-review');
const AUTH_DIR = path.resolve(__dirname, '..', '.auth');
const SAMPLE = path.resolve(__dirname, '..', '..', '..', 'doc', 'samples', 'Requel.xml');
const AXE = require.resolve('axe-core/axe.min.js');
const ONLY = process.env['UI_REVIEW_ONLY'];

type Theme = 'light' | 'dark';
interface View { name: string; theme: Theme; width: number; height: number }
const VIEWS: View[] = [
  { name: 'light-wide', theme: 'light', width: 1440, height: 900 },
  { name: 'dark-wide', theme: 'dark', width: 1440, height: 900 },
  // Below the 768px point where the sidebar starts collapsed (layout.ts NARROW_QUERY).
  { name: 'light-narrow', theme: 'light', width: 390, height: 844 },
  { name: 'dark-narrow', theme: 'dark', width: 390, height: 844 },
];

/** What the walk resolved before visiting: project names and one id per detail route. */
interface Targets {
  project: string;
  empty: string;
  ids: Record<string, string | undefined>;
}

type State = 'populated' | 'empty' | 'error';
interface Visit {
  page: string;
  state: State;
  /** null when the target data doesn't exist; the page is listed as not reached. */
  url: (t: Targets) => string | null;
  /** Visited signed out (the login page). */
  signedOut?: boolean;
}

const p = (t: Targets, rest = '') => `/projects/${encodeURIComponent(t.project)}${rest}`;
const e = (t: Targets, rest = '') => `/projects/${encodeURIComponent(t.empty)}${rest}`;
const detail = (key: string, segment: string) => (t: Targets) =>
  t.ids[key] ? p(t, `/${segment}/${encodeURIComponent(t.ids[key]!)}`) : null;

/** The lists that have an empty state worth seeing, by route segment. */
const LISTS = ['stakeholders', 'goals', 'stories', 'actors', 'scenarios', 'use-cases', 'terms',
  'reports', 'definitions', 'open-issues', 'dictionary'];

const VISITS: Visit[] = [
  { page: 'login', state: 'populated', url: () => '/login', signedOut: true },
  { page: 'change-password', state: 'populated', url: () => '/change-password' },
  { page: 'dashboard', state: 'populated', url: () => '/' },
  { page: 'account', state: 'populated', url: () => '/account' },
  { page: 'settings', state: 'populated', url: () => '/settings' },
  { page: 'users', state: 'populated', url: () => '/users' },
  { page: 'user-editor', state: 'populated', url: () => '/users/project' },
  { page: 'global-tags', state: 'populated', url: () => '/global-tags' },
  { page: 'tag-categories', state: 'populated', url: () => '/tag-categories' },
  { page: 'install-dictionary', state: 'populated', url: () => '/dictionary' },
  { page: 'maintenance', state: 'populated', url: () => '/maintenance' },
  { page: 'projects', state: 'populated', url: () => '/projects' },
  { page: 'project-new', state: 'populated', url: () => '/projects/new' },
  { page: 'project-workspace', state: 'populated', url: t => p(t) },
  { page: 'project-editor', state: 'populated', url: t => p(t, '/edit') },
  ...LISTS.map(list => ({ page: list, state: 'populated' as State, url: (t: Targets) => p(t, `/${list}`) })),
  { page: 'stakeholder-editor', state: 'populated', url: detail('stakeholder', 'stakeholders') },
  { page: 'goal-editor', state: 'populated', url: detail('goal', 'goals') },
  { page: 'story-editor', state: 'populated', url: detail('story', 'stories') },
  { page: 'actor-editor', state: 'populated', url: detail('actor', 'actors') },
  { page: 'scenario-editor', state: 'populated', url: detail('scenario', 'scenarios') },
  { page: 'step-editor', state: 'populated', url: detail('step', 'steps') },
  { page: 'use-case-editor', state: 'populated', url: detail('useCase', 'use-cases') },
  { page: 'term-editor', state: 'populated', url: detail('term', 'terms') },
  { page: 'report-editor', state: 'populated', url: detail('report', 'reports') },
  { page: 'definition-editor', state: 'populated', url: detail('definition', 'definitions') },
  { page: 'project-workspace', state: 'empty', url: t => e(t) },
  ...LISTS.map(list => ({ page: list, state: 'empty' as State, url: (t: Targets) => e(t, `/${list}`) })),
  { page: 'goal-editor', state: 'error', url: t => p(t, '/goals/999999999') },
];

interface Result { visit: Visit; view: string; file: string; counts: Record<string, number>; rules: string[] }
const results: Result[] = [];
const unreached: string[] = [];
let targets: Targets;
let api: APIRequestContext;
let token: string;

test.describe.configure({ mode: 'serial' });

test.beforeAll(async ({ playwright }) => {
  fs.rmSync(OUT, { recursive: true, force: true });
  fs.mkdirSync(path.join(OUT, 'axe'), { recursive: true });
  api = await playwright.request.newContext({ baseURL: BASE_URL });
  const login = await api.post('/api/auth/login', {
    data: { username: process.env['E2E_ADMIN_USERNAME'] ?? 'admin',
      password: process.env['E2E_ADMIN_PASSWORD'] ?? 'admin' },
  });
  if (!login.ok()) throw new Error(`admin login failed: ${login.status()}`);
  token = (await login.json()).token;

  const stamp = Date.now();
  const imported = await command('ImportProject', { name: `ui-review-${stamp}` },
    fs.readFileSync(SAMPLE));
  const project = (imported['entity'] as { name: string }).name;
  const emptyResult = await command('EditProject', {
    projectName: null, name: `ui-review-empty-${stamp}`, description: 'UI review: no entities',
    organizationName: 'UI Review',
  });
  const empty = (emptyResult['entity'] as { name: string }).name;

  const first = async (segment: string, field = 'id') => {
    const res = await api.get(`/api/projects/${encodeURIComponent(project)}/${segment}`, auth());
    if (!res.ok()) return undefined;
    const list = await res.json() as Record<string, unknown>[];
    return list.length ? String(list[0][field]) : undefined;
  };
  targets = {
    project,
    empty,
    ids: {
      stakeholder: await first('stakeholders'),
      goal: await first('goals'),
      story: await first('stories'),
      actor: await first('actors'),
      scenario: await first('scenarios'),
      step: await first('steps'),
      useCase: await first('use-cases'),
      term: await first('terms'),
      report: await first('reports'),
      definition: await first('definitions', 'key'),
    },
  };
});

test.afterAll(async () => {
  writeSummary();
  if (!targets) return;
  for (const name of [targets.project, targets.empty]) {
    await command('DeleteProject', { projectName: name }).catch(err =>
      console.warn(`[ui-review] couldn't delete ${name}: ${err}`));
  }
  await api.dispose();
});

for (const visit of VISITS) {
  const label = `${visit.page} (${visit.state})`;
  if (ONLY && !visit.page.includes(ONLY)) continue;
  test(label, async ({ browser }) => {
    const url = visit.url(targets);
    if (!url) {
      unreached.push(`${label}: the sample project has no target for it`);
      return;
    }
    for (const view of VIEWS) {
      const context = await newContext(browser, view, visit.signedOut);
      try {
        const page = await context.newPage();
        await page.goto(url);
        await settle(page);
        const file = `${visit.page}--${visit.state}--${view.name}`;
        await growToContent(page, view);
        await page.screenshot({ path: path.join(OUT, `${file}.png`), fullPage: true });
        const violations = await runAxe(page);
        fs.writeFileSync(path.join(OUT, 'axe', `${file}.json`), JSON.stringify(violations, null, 2));
        const counts: Record<string, number> = {};
        for (const v of violations) counts[v.impact ?? 'unknown'] = (counts[v.impact ?? 'unknown'] ?? 0) + v.nodes.length;
        results.push({ visit, view: view.name, file, counts,
          rules: violations.map(v => `${v.impact}: ${v.id} (${v.nodes.length})`) });
      } finally {
        await context.close();
      }
    }
  });
}

// ---------------------------------------------------------------------------

function auth() {
  return { headers: { Authorization: `Bearer ${token}` } };
}

async function command(type: string, input: Record<string, unknown>, file?: Buffer)
    : Promise<Record<string, unknown>> {
  const res = file
    ? await api.post(`/api/commands/${type}`, {
      ...auth(),
      multipart: {
        input: { name: 'input', mimeType: 'application/json', buffer: Buffer.from(JSON.stringify(input)) },
        file: { name: 'Requel.xml', mimeType: 'application/xml', buffer: file },
      },
    })
    : await api.post(`/api/commands/${type}`, { ...auth(), data: input });
  if (!res.ok()) throw new Error(`${type} failed: ${res.status()} ${await res.text()}`);
  return res.json();
}

async function newContext(browser: Browser, view: View, signedOut?: boolean): Promise<BrowserContext> {
  const context = await browser.newContext({
    baseURL: BASE_URL,
    viewport: { width: view.width, height: view.height },
    colorScheme: view.theme,
    storageState: signedOut ? undefined : path.join(AUTH_DIR, 'admin.json'),
  });
  // The theme service reads its mode from localStorage before Angular starts.
  await context.addInitScript(theme => {
    try { localStorage.setItem('requel_theme', theme); } catch { /* storage blocked */ }
  }, view.theme);
  return context;
}

/** Waits for the page's data: no loading indicator left, then a beat for late renders. */
async function settle(page: Page): Promise<void> {
  await page.waitForLoadState('domcontentloaded');
  // The event stream keeps a request open, so 'networkidle' never comes.
  await page.locator('app-loading-state, p-progressspinner, .p-skeleton').first()
    .waitFor({ state: 'detached', timeout: 15_000 }).catch(() => undefined);
  await page.waitForTimeout(500);
}

/**
 * The layout scrolls inside its own content area, so a full-page screenshot stops at the window.
 * Grow the window by whatever the tallest scrolled area hides (capped), then let it re-lay out.
 */
async function growToContent(page: Page, view: View): Promise<void> {
  const hidden = await page.evaluate(() => {
    let most = 0;
    for (const el of Array.from(document.querySelectorAll<HTMLElement>('body *'))) {
      const overflowY = getComputedStyle(el).overflowY;
      if ((overflowY === 'auto' || overflowY === 'scroll') && el.scrollHeight > el.clientHeight) {
        most = Math.max(most, el.scrollHeight - el.clientHeight);
      }
    }
    return most;
  });
  if (hidden > 0) {
    await page.setViewportSize({ width: view.width, height: Math.min(view.height + hidden, 8000) });
    await page.waitForTimeout(300);
  }
}

interface AxeViolation { id: string; impact: string | null; help: string; nodes: { target: string[] }[] }

async function runAxe(page: Page): Promise<AxeViolation[]> {
  await page.addScriptTag({ path: AXE });
  return page.evaluate(async () => {
    const axe = (window as unknown as { axe: { run: (ctx: Document, opts: object) => Promise<{ violations: AxeViolation[] }> } }).axe;
    const result = await axe.run(document, { resultTypes: ['violations'] });
    return result.violations.map(v => ({ id: v.id, impact: v.impact, help: v.help,
      nodes: v.nodes.map(n => ({ target: n.target })) }));
  });
}

function writeSummary(): void {
  const impacts = ['critical', 'serious', 'moderate', 'minor'];
  const lines = [
    '# UI review walk',
    '',
    `Run ${new Date().toISOString()} against ${BASE_URL}. Node counts per impact; rules in the last column.`,
    '',
    `| Page | State | View | ${impacts.join(' | ')} | Rules |`,
    `|---|---|---|${impacts.map(() => '---').join('|')}|---|`,
    ...results.map(r => `| ${r.visit.page} | ${r.visit.state} | ${r.view} | `
      + impacts.map(i => r.counts[i] ?? 0).join(' | ') + ` | ${r.rules.join('; ')} |`),
    '',
    '## Not reached',
    '',
    ...(unreached.length ? unreached.map(u => `- ${u}`) : ['None.']),
    '',
  ];
  fs.writeFileSync(path.join(OUT, 'summary.md'), lines.join('\n'));
}
