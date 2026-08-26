import { Route } from '@angular/router';
import { routes } from './app.routes';
import { authGuard } from './core/auth.guard';
import { adminGuard } from './core/admin.guard';
import { dirtyCheckGuard } from './core/dirty-check.guard';

const layout = routes.find(r => r.path === '' && r.children) as Route;
const children = layout?.children ?? [];
const byPath = (p: string) => children.find(c => c.path === p);

const EXPECTED = [
  '', 'account', 'settings',
  'users', 'users/:username', 'global-tags', 'tag-categories',
  'projects', 'projects/new',
  'projects/:name/stakeholders', 'projects/:name/stakeholders/:stakeholderId',
  'projects/:name/goals', 'projects/:name/goals/:goalId',
  'projects/:name/stories', 'projects/:name/stories/:storyId',
  'projects/:name/actors', 'projects/:name/actors/:actorId',
  'projects/:name/scenarios', 'projects/:name/scenarios/:scenarioId',
  'projects/:name/use-cases', 'projects/:name/use-cases/:useCaseId',
  'projects/:name/terms', 'projects/:name/terms/:termId',
  'projects/:name/reports', 'projects/:name/reports/:reportId',
  'projects/:name/open-issues',
  'projects/:name/edit',
  'projects/:name',
];

const ADMIN = ['users', 'users/:username', 'global-tags', 'tag-categories'];
const EDITORS = [
  'account', 'users/:username', 'projects/new', 'projects/:name/edit',
  'projects/:name/stakeholders/:stakeholderId', 'projects/:name/goals/:goalId',
  'projects/:name/stories/:storyId', 'projects/:name/actors/:actorId',
  'projects/:name/scenarios/:scenarioId', 'projects/:name/use-cases/:useCaseId',
  'projects/:name/terms/:termId', 'projects/:name/reports/:reportId',
];

describe('app routes (#142)', () => {
  it('keeps login top-level, the auth-guarded shell, and the ** fallback', () => {
    expect(routes.some(r => r.path === 'login')).toBe(true);
    expect(layout).toBeDefined();
    expect(layout.canActivate).toContain(authGuard);
    expect(routes[routes.length - 1]).toMatchObject({ path: '**', redirectTo: '' });
  });

  it('covers every expected child path exactly once', () => {
    expect([...children.map(c => c.path)].sort()).toEqual([...EXPECTED].sort());
  });

  it('gates admin routes with adminGuard', () => {
    for (const p of ADMIN) expect(byPath(p)?.canActivate, p).toContain(adminGuard);
  });

  it('keeps the dirty-check canDeactivate guard on every editor', () => {
    for (const p of EDITORS) expect(byPath(p)?.canDeactivate, p).toContain(dirtyCheckGuard);
  });

  it('carries a title and data.section on every child, and stays lazy (except the eager dashboard)', () => {
    for (const c of children) {
      expect(typeof c.title, `title ${c.path}`).toBe('string');
      expect((c.data as { section?: string })?.section, `section ${c.path}`).toBeTruthy();
      if (c.path !== '') {
        expect(typeof c.loadComponent, `lazy ${c.path}`).toBe('function');
      }
    }
  });

  // #154 (moved from #128): artifact editor routes resolve the entity's name for
  // the breadcrumb leaf + document title. The project editor is excluded - its name
  // is already the :name URL segment.
  it('attaches artifactNameResolver (resolve.entityName) to every artifact editor, not the project editor', () => {
    const ARTIFACT_EDITORS = [
      'projects/:name/stakeholders/:stakeholderId', 'projects/:name/goals/:goalId',
      'projects/:name/stories/:storyId', 'projects/:name/actors/:actorId',
      'projects/:name/scenarios/:scenarioId', 'projects/:name/use-cases/:useCaseId',
      'projects/:name/terms/:termId', 'projects/:name/reports/:reportId',
    ];
    for (const p of ARTIFACT_EDITORS) {
      const r = byPath(p) as Route & { resolve?: Record<string, unknown> };
      expect(typeof r?.resolve?.['entityName'], `resolver on ${p}`).toBe('function');
    }
    const projectEditor = byPath('projects/:name') as Route & { resolve?: Record<string, unknown> };
    expect(projectEditor?.resolve?.['entityName'], 'project editor needs no resolver').toBeUndefined();
  });

  it('matches projects/:name (project editor) AFTER its /:name/* children', () => {
    const editorIdx = children.findIndex(c => c.path === 'projects/:name');
    expect(editorIdx).toBeGreaterThan(-1);
    children.forEach((c, i) => {
      if (c.path?.startsWith('projects/:name/')) {
        expect(i, `${c.path} must precede the project editor`).toBeLessThan(editorIdx);
      }
    });
  });
});
