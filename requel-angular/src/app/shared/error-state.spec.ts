import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { ErrorStateComponent } from './error-state';

describe('ErrorStateComponent (issue #131)', () => {
  function render(inputs: Partial<ErrorStateComponent> = {}) {
    TestBed.configureTestingModule({
      imports: [ErrorStateComponent],
      providers: [provideNoopAnimations()],
    });
    const fixture = TestBed.createComponent(ErrorStateComponent);
    Object.assign(fixture.componentInstance, inputs);
    fixture.detectChanges();
    return fixture;
  }

  it('shows the message and, by default, an alert role with a Retry button', () => {
    const fixture = render({ message: 'Could not load project.' });
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('.error-state__message')?.textContent?.trim()).toBe('Could not load project.');
    expect(el.querySelector('[role="alert"]')).not.toBeNull();
    expect(el.querySelector('[data-testid="error-state-retry"]')).not.toBeNull();
  });

  it('emits (retry) when the Retry button is clicked', () => {
    const fixture = render({ message: 'Boom' });
    const spy = vi.fn();
    fixture.componentInstance.retry.subscribe(spy);
    const btn = fixture.nativeElement.querySelector('[data-testid="error-state-retry"] button') as HTMLButtonElement;
    btn.click();
    expect(spy).toHaveBeenCalledOnce();
  });

  it('renders warn severity as a polite status without a Retry button', () => {
    const fixture = render({
      message: 'Tags could not be loaded.',
      severity: 'warn',
      retryable: false,
    });
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('[role="status"]')).not.toBeNull();
    expect(el.querySelector('[role="alert"]')).toBeNull();
    expect(el.querySelector('.error-state--warn')).not.toBeNull();
    expect(el.querySelector('[data-testid="error-state-retry"]')).toBeNull();
  });

  it('shows optional support detail when provided', () => {
    const fixture = render({
      message: 'You do not have permission to view this section.',
      detail: 'Contact your project admin for access.',
      retryable: false,
    });
    expect(fixture.nativeElement.querySelector('[data-testid="error-state-detail"]')?.textContent?.trim())
      .toBe('Contact your project admin for access.');
  });
});
