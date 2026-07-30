import { Component, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { LayoutComponent } from './layout';
import { AuthService } from '../../core/auth.service';
import { EventStreamService } from '../../core/event-stream.service';
import { SidebarNavComponent } from '../../shared/sidebar-nav';

// Lightweight stand-in for the sidebar so the layout can render without the
// sidebar's data services / SSE subscriptions.
@Component({ selector: 'app-sidebar-nav', standalone: true, template: '' })
class SidebarNavStubComponent {}

describe('LayoutComponent accessibility (issue #135)', () => {
  function createFixture() {
    TestBed.configureTestingModule({
      imports: [LayoutComponent],
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        { provide: AuthService, useValue: { user: signal(null), logout: vi.fn() } },
        { provide: EventStreamService, useValue: { connect: vi.fn() } }
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
