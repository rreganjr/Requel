import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { SubmitErrorComponent } from './app-submit-error';

describe('SubmitErrorComponent (issue #133)', () => {
  function render(inputs: Partial<SubmitErrorComponent> = {}) {
    TestBed.configureTestingModule({
      imports: [SubmitErrorComponent],
      providers: [provideNoopAnimations()],
    });
    const fixture = TestBed.createComponent(SubmitErrorComponent);
    Object.assign(fixture.componentInstance, inputs);
    fixture.detectChanges();
    return fixture;
  }

  it('renders nothing when message is null', () => {
    const el: HTMLElement = render({ message: null }).nativeElement;
    expect(el.querySelector('.submit-error')).toBeNull();
  });

  it('renders nothing when message is empty', () => {
    const el: HTMLElement = render({ message: '' }).nativeElement;
    expect(el.querySelector('.submit-error')).toBeNull();
  });

  it('shows the message as an assertive alert, no Retry by default', () => {
    const el: HTMLElement = render({ message: 'Save failed.' }).nativeElement;
    expect(el.querySelector('.submit-error__message')?.textContent?.trim()).toBe('Save failed.');
    expect(el.querySelector('[role="alert"]')).not.toBeNull();
    expect(el.querySelector('[data-testid="submit-error-retry"]')).toBeNull();
  });

  it('shows a Retry button only when retryable and emits (retry) on click', () => {
    const fixture = render({ message: 'Network error', retryable: true });
    const spy = vi.fn();
    fixture.componentInstance.retry.subscribe(spy);
    const btn = fixture.nativeElement
      .querySelector('[data-testid="submit-error-retry"] button') as HTMLButtonElement;
    expect(btn).not.toBeNull();
    btn.click();
    expect(spy).toHaveBeenCalledOnce();
  });

  it('honours a custom testid and retryLabel', () => {
    const el: HTMLElement = render({
      message: 'Boom', retryable: true, testid: 'project-editor-error', retryLabel: 'Try again',
    }).nativeElement;
    expect(el.querySelector('[data-testid="project-editor-error"]')).not.toBeNull();
    expect(el.querySelector('[data-testid="project-editor-error-retry"]')?.textContent).toContain('Try again');
  });
});
