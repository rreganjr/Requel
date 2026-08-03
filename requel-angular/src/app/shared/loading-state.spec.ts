import { TestBed } from '@angular/core/testing';
import { LoadingStateComponent } from './loading-state';

describe('LoadingStateComponent (issue #131)', () => {
  function render(inputs: Partial<LoadingStateComponent> = {}): HTMLElement {
    TestBed.configureTestingModule({ imports: [LoadingStateComponent] });
    const fixture = TestBed.createComponent(LoadingStateComponent);
    Object.assign(fixture.componentInstance, inputs);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  it('renders the default number of skeleton bars', () => {
    const el = render();
    expect(el.querySelectorAll('.skeleton-bar').length).toBe(3);
  });

  it('renders the requested number of skeleton bars', () => {
    const el = render({ lines: 5 });
    expect(el.querySelectorAll('.skeleton-bar').length).toBe(5);
  });

  it('exposes a readable status label to assistive tech and hides the bars', () => {
    const el = render({ label: 'Loading project…' });
    const status = el.querySelector('[role="status"]');
    expect(status?.textContent?.trim()).toBe('Loading project…');
    expect(status?.getAttribute('aria-live')).toBe('polite');
    // Decorative bars must be hidden from the accessibility tree.
    expect(el.querySelector('.skeleton')?.getAttribute('aria-hidden')).toBe('true');
  });

  it('uses the provided testid', () => {
    const el = render({ testid: 'project-loading' });
    expect(el.querySelector('[data-testid="project-loading"]')).not.toBeNull();
  });
});
