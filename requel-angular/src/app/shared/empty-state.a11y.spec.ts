import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { EmptyStateComponent } from './empty-state';
import { expectNoAxeViolations } from './testing/a11y';

describe('EmptyStateComponent — accessibility', () => {
  function render(inputs: Partial<EmptyStateComponent>) {
    TestBed.configureTestingModule({
      imports: [EmptyStateComponent],
      providers: [provideNoopAnimations()],
    });
    const fixture = TestBed.createComponent(EmptyStateComponent);
    Object.assign(fixture.componentInstance, inputs);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  it('has no axe-core violations without an action', async () => {
    const el = render({ title: 'No goals yet', message: 'Create your first goal.' });
    await expectNoAxeViolations(el);
  });

  it('has no axe-core violations with a permitted action', async () => {
    const el = render({
      title: 'No goals yet',
      message: 'Create your first goal.',
      icon: 'pi-flag',
      actionLabel: 'New Goal',
      showAction: true,
    });
    await expectNoAxeViolations(el);
  });
});
