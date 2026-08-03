import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { ErrorStateComponent } from './error-state';
import { expectNoAxeViolations } from './testing/a11y';

describe('ErrorStateComponent — accessibility', () => {
  function render(inputs: Partial<ErrorStateComponent>) {
    TestBed.configureTestingModule({
      imports: [ErrorStateComponent],
      providers: [provideNoopAnimations()],
    });
    const fixture = TestBed.createComponent(ErrorStateComponent);
    Object.assign(fixture.componentInstance, inputs);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  it('has no axe-core violations as a blocking error with retry', async () => {
    const el = render({ message: 'Could not load project.' });
    await expectNoAxeViolations(el);
  });

  it('has no axe-core violations as a non-blocking warning with detail', async () => {
    const el = render({
      message: 'You do not have permission to view this section.',
      detail: 'Contact your project admin for access.',
      severity: 'warn',
      retryable: false,
    });
    await expectNoAxeViolations(el);
  });
});
