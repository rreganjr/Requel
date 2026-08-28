import { Component, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter, Router } from '@angular/router';
import { LayoutComponent } from './layout';
import { AuthService } from '../../core/auth.service';
import { EventStreamService } from '../../core/event-stream.service';
import { SidebarNavComponent } from '../../shared/sidebar-nav';

// Lightweight stand-in for the sidebar so the layout can render without the
// sidebar's data services / SSE subscriptions.
@Component({ selector: 'app-sidebar-nav', standalone: true, template: '' })
class SidebarNavStubComponent {}

@Component({ selector: 'app-route-stub', standalone: true, template: '' })
class RouteStubComponent {}

describe('LayoutComponent accessibility (issue #135)', () => {
  function createFixture() {
    TestBed.configureTestingModule({
      imports: [LayoutComponent],
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        { provide: AuthService, useValue: { user: signal(null), logout: vi.fn() } },
        { provide: EventStreamService, useValue: { connect: vi.fn(), isConnected: () => false } }
      ]
    });
    TestBed.overrideComponent(LayoutComponent, {
      remove: { imports: [SidebarNavComponent] },
      add: { imports: [SidebarNavStubComponent] }
    });
    const fixture = TestBed.createComponent(LayoutComponent);
    fixture.detectChanges();
    return fixture;
  }

  it('renders a skip link as the first focusable element, targeting #main-content', () => {
    const el: HTMLElement = createFixture().nativeElement;

    const skip = el.querySelector<HTMLAnchorElement>('a.skip-link');
    expect(skip).toBeTruthy();
    expect(skip!.getAttribute('href')).toBe('#main-content');

    const focusable = el.querySelectorAll<HTMLElement>(
      'a[href], button, input, select, textarea, [tabindex]:not([tabindex="-1"])'
    );
    expect(focusable.length).toBeGreaterThan(0);
    expect(focusable[0]).toBe(skip);
  });

  it('exposes <main> as a focus target with id="main-content" and tabindex="-1"', () => {
    const el: HTMLElement = createFixture().nativeElement;

    const main = el.querySelector<HTMLElement>('main#main-content');
    expect(main).toBeTruthy();
    expect(main!.getAttribute('tabindex')).toBe('-1');
  });
});


// ----- #154 top bar + sidebar collapse -------------------------------------
const SIDEBAR_COLLAPSED_KEY = 'requel_sidebar_collapsed';

describe('LayoutComponent top bar (issue #154)', () => {
  function createFixture() {
    TestBed.configureTestingModule({
      imports: [LayoutComponent],
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        { provide: AuthService, useValue: { user: signal(null), logout: vi.fn() } },
        { provide: EventStreamService, useValue: { connect: vi.fn(), isConnected: () => false } }
      ]
    });
    TestBed.overrideComponent(LayoutComponent, {
      remove: { imports: [SidebarNavComponent] },
      add: { imports: [SidebarNavStubComponent] }
    });
    const fixture = TestBed.createComponent(LayoutComponent);
    fixture.detectChanges();
    return fixture;
  }

  beforeEach(() => localStorage.removeItem(SIDEBAR_COLLAPSED_KEY));
  afterEach(() => localStorage.removeItem(SIDEBAR_COLLAPSED_KEY));

  it('renders the top-bar regions: sidebar toggle, disabled search, account menu', () => {
    const el: HTMLElement = createFixture().nativeElement;
    expect(el.querySelector('[data-testid="sidebar-toggle"]')).toBeTruthy();
    const search = el.querySelector<HTMLButtonElement>('[data-testid="header-search"]');
    expect(search).toBeTruthy();
    expect(search!.disabled).toBe(true);
    expect(el.querySelector('.account-trigger')).toBeTruthy();
    // Breadcrumb region is reserved (filled dynamically in the #128 step).
    expect(el.querySelector('[data-testid="breadcrumb-region"]')).toBeTruthy();
  });

  it('hides the back button at the shell root', () => {
    const el: HTMLElement = createFixture().nativeElement;
    // provideRouter([]) leaves the url at '/', so showBack() is false.
    expect(el.querySelector('[data-testid="back-button"]')).toBeNull();
  });

  it('collapses the sidebar and persists the choice when the toggle is clicked', () => {
    const fixture = createFixture();
    const el: HTMLElement = fixture.nativeElement;
    const aside = el.querySelector<HTMLElement>('#app-sidebar')!;
    const toggle = el.querySelector<HTMLButtonElement>('[data-testid="sidebar-toggle"]')!;

    expect(aside.hidden).toBe(false);
    expect(toggle.getAttribute('aria-expanded')).toBe('true');

    toggle.click();
    fixture.detectChanges();

    expect(aside.hidden).toBe(true);
    expect(toggle.getAttribute('aria-expanded')).toBe('false');
    expect(localStorage.getItem(SIDEBAR_COLLAPSED_KEY)).toBe('true');
  });

  it('seeds the collapsed state from localStorage', () => {
    localStorage.setItem(SIDEBAR_COLLAPSED_KEY, 'true');
    const el: HTMLElement = createFixture().nativeElement;
    const aside = el.querySelector<HTMLElement>('#app-sidebar')!;
    expect(aside.hidden).toBe(true);
    expect(el.querySelector('[data-testid="sidebar-toggle"]')!.getAttribute('aria-expanded'))
      .toBe('false');
  });

  it('exposes the sidebar as a landmark region the toggle controls', () => {
    const el: HTMLElement = createFixture().nativeElement;
    const toggle = el.querySelector('[data-testid="sidebar-toggle"]')!;
    expect(toggle.getAttribute('aria-controls')).toBe('app-sidebar');
    expect(el.querySelector('#app-sidebar')).toBeTruthy();
  });

  // Regression guard (dirty-guard.e2e.ts): every artifact editor has its own
  // <p-button label="Back">, and the e2e drives it with
  // getByRole('button', { name: 'Back' }) - a case-insensitive SUBSTRING match.
  // If the top-bar back button's accessible name contains "back" it becomes a
  // second match and the click hits a strict-mode violation. So the global
  // button must be named distinctly (browser-history back, not "back to list").
  it('names the top-bar back button without the substring "back"', async () => {
    TestBed.configureTestingModule({
      imports: [LayoutComponent],
      providers: [
        provideNoopAnimations(),
        provideRouter([{ path: 'somewhere', component: RouteStubComponent }]),
        { provide: AuthService, useValue: { user: signal(null), logout: vi.fn() } },
        { provide: EventStreamService, useValue: { connect: vi.fn(), isConnected: () => false } }
      ]
    });
    TestBed.overrideComponent(LayoutComponent, {
      remove: { imports: [SidebarNavComponent] },
      add: { imports: [SidebarNavStubComponent] }
    });
    const router = TestBed.inject(Router);
    const fixture = TestBed.createComponent(LayoutComponent);
    await router.navigateByUrl('/somewhere');   // off the shell root → showBack() is true
    fixture.detectChanges();

    const back = fixture.nativeElement.querySelector('[data-testid="back-button"]');
    expect(back, 'back button should render off the shell root').toBeTruthy();
    const name = (back.getAttribute('aria-label') ?? '').toLowerCase();
    expect(name.length).toBeGreaterThan(0);
    expect(name).not.toContain('back');
  });
});
