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
