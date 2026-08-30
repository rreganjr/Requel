import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { BreadcrumbComponent } from './breadcrumb';

@Component({ selector: 'app-dummy', standalone: true, template: '' })
class DummyComponent {}

// Mirrors the shell shape from app.routes.ts: a '' parent with the flat project
// route table as children (each projects/:name/* is its own leaf, per #142).
const ROUTES = [
  {
    path: '', children: [
      { path: '', title: 'Dashboard', component: DummyComponent },
      { path: 'projects', title: 'Projects', component: DummyComponent },
      { path: 'projects/:name', title: 'Project', component: DummyComponent },
      { path: 'projects/:name/goals', title: 'Goals', component: DummyComponent },
      { path: 'projects/:name/goals/:goalId', title: 'Goal', component: DummyComponent },
      { path: 'settings', title: 'Settings', component: DummyComponent },
    ],
  },
];

describe('BreadcrumbComponent', () => {
  let router: Router;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  let fixture: any;
  let comp: BreadcrumbComponent;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      imports: [BreadcrumbComponent],
      providers: [provideRouter(ROUTES)],
    });
    router = TestBed.inject(Router);
    fixture = TestBed.createComponent(BreadcrumbComponent);
    comp = fixture.componentInstance;
    await router.navigateByUrl('/');
    fixture.detectChanges();
  });

  async function go(url: string): Promise<void> {
    await router.navigateByUrl(url);
    fixture.detectChanges();
  }

  const labels = () => comp.crumbs().map(c => c.label);

  it('renders nothing at the shell root', () => {
    expect(comp.crumbs()).toEqual([]);
    expect(fixture.nativeElement.querySelector('[data-testid="breadcrumb"]')).toBeNull();
  });

  it('renders a single current crumb for the projects list', async () => {
    await go('/projects');
    expect(labels()).toEqual(['Projects']);
    expect(comp.crumbs()[0].current).toBe(true);
  });

  it('builds project -> section trail from a flat route table', async () => {
    await go('/projects/Acme/goals');
    expect(labels()).toEqual(['Projects', 'Acme', 'Goals']);
    expect(comp.crumbs().map(c => c.url)).toEqual([
      '/projects', '/projects/Acme', '/projects/Acme/goals',
    ]);
    expect(comp.crumbs().at(-1)!.current).toBe(true);
    expect(comp.crumbs().slice(0, -1).every(c => !c.current)).toBe(true);
  });

  it('shows the project name (a named :param), never a bare id', async () => {
    await go('/projects/Acme/goals/12');
    // The goal editor leaf shows its TYPE ("Goal"), not the numeric id -
    // resolving the entity name is the #128 PR3 resolver step.
    expect(labels()).toEqual(['Projects', 'Acme', 'Goals', 'Goal']);
    expect(comp.crumbs().at(-1)!.label).not.toBe('12');
  });

  it('decodes an encoded project name segment', async () => {
    await go('/projects/My%20Proj/goals');
    expect(labels()).toEqual(['Projects', 'My Proj', 'Goals']);
  });

  it('links a project name with spaces and parens single-encoded (no double-encoding)', async () => {
    // Regression for #220 / B1: the breadcrumb used to join already-encoded URL
    // segments into a string and bind it as routerLink, so the router encoded it a
    // second time (%20 -> %2520) and the workspace failed to load "Imported Project (10)".
    await go('/projects/My%20Proj%20%2810%29/goals');
    expect(labels()).toEqual(['Projects', 'My Proj (10)', 'Goals']);

    // routerLink is a command array of DECODED segments, so the router encodes once.
    expect(comp.crumbs()[1].commands).toEqual(['/', 'projects', 'My Proj (10)']);

    // The rendered href is single-encoded: it carries no %25 (the double-encode
    // signature), and decoding it once returns the real path.
    const links = fixture.nativeElement.querySelectorAll('a.breadcrumb-link');
    const projectHref = links[1].getAttribute('href') as string;
    expect(projectHref).not.toContain('%25');
    expect(decodeURIComponent(projectHref)).toBe('/projects/My Proj (10)');
  });

  it('falls back to a single crumb for a shallow route', async () => {
    await go('/settings');
    expect(labels()).toEqual(['Settings']);
    expect(comp.crumbs()[0].current).toBe(true);
  });

  it('exposes keyboard-navigable, labelled markup', async () => {
    await go('/projects/Acme/goals/12');
    const el: HTMLElement = fixture.nativeElement;
    const nav = el.querySelector('nav[aria-label="Breadcrumb"]');
    expect(nav).not.toBeNull();
    // Non-leaf crumbs are links; the leaf is a non-link current marker.
    const links = el.querySelectorAll('a.breadcrumb-link');
    expect(links.length).toBe(3);
    links.forEach(a => expect(a.getAttribute('href')).toBeTruthy());
    const current = el.querySelector('[aria-current="page"]');
    expect(current!.textContent).toContain('Goal');
    expect(current!.tagName).toBe('SPAN');
  });
});


describe('BreadcrumbComponent — resolved entity name (#154)', () => {
  const RESOLVED_ROUTES = [
    {
      path: '', children: [
        { path: 'projects', title: 'Projects', component: DummyComponent },
        { path: 'projects/:name', title: 'Project', component: DummyComponent },
        { path: 'projects/:name/goals', title: 'Goals', component: DummyComponent },
        {
          path: 'projects/:name/goals/:goalId', title: 'Goal', component: DummyComponent,
          resolve: { entityName: () => 'Login flow' },
        },
      ],
    },
  ];

  let router: Router;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  let fixture: any;
  let comp: BreadcrumbComponent;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      imports: [BreadcrumbComponent],
      providers: [provideRouter(RESOLVED_ROUTES)],
    });
    router = TestBed.inject(Router);
    fixture = TestBed.createComponent(BreadcrumbComponent);
    comp = fixture.componentInstance;
    await router.navigateByUrl('/');
    fixture.detectChanges();
  });

  it('upgrades the editor leaf from its type label to the resolved entity name', async () => {
    await router.navigateByUrl('/projects/Acme/goals/12');
    fixture.detectChanges();
    const labels = comp.crumbs().map(c => c.label);
    expect(labels).toEqual(['Projects', 'Acme', 'Goals', 'Login flow']);
    expect(comp.crumbs().at(-1)!.current).toBe(true);
  });

  it('leaves the section crumb (no resolver) as its static label', async () => {
    await router.navigateByUrl('/projects/Acme/goals');
    fixture.detectChanges();
    expect(comp.crumbs().map(c => c.label)).toEqual(['Projects', 'Acme', 'Goals']);
  });
});
