import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { SubmitErrorComponent } from './app-submit-error';
import { expectNoAxeViolations } from './testing/a11y';

describe('SubmitErrorComponent — accessibility', () => {
  function render(inputs: Partial<SubmitErrorComponent>) {
    TestBed.configureTestingModule({
      imports: [SubmitErrorComponent],
      providers: [provideNoopAnimations()],
    });
    const fixture = TestBed.createComponent(SubmitErrorComponent);
    Object.assign(fixture.componentInstance, inputs);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  it('has no axe-core violations as a plain blocking error', async () => {
    await expectNoAxeViolations(render({ message: 'Save failed.' }));
  });

  it('has no axe-core violations as a retryable network error', async () => {
    await expectNoAxeViolations(render({ message: 'Network error', retryable: true }));
  });
});
