import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { EmptyStateComponent } from './empty-state';

describe('EmptyStateComponent (issue #131)', () => {
  function render(inputs: Partial<EmptyStateComponent> = {}) {
    TestBed.configureTestingModule({
      imports: [EmptyStateComponent],
      providers: [provideNoopAnimations()],
    });
    const fixture = TestBed.createComponent(EmptyStateComponent);
    Object.assign(fixture.componentInstance, inputs);
    fixture.detectChanges();
    return fixture;
  }

  it('renders the title and guidance message', () => {
    const el: HTMLElement = render({ title: 'No goals yet', message: 'Create your first goal.' }).nativeElement;
    expect(el.querySelector('.empty-state__title')?.textContent?.trim()).toBe('No goals yet');
    expect(el.querySelector('.empty-state__message')?.textContent?.trim()).toBe('Create your first goal.');
  });

  it('does not render a heading element (keeps page heading order intact)', () => {
    const el: HTMLElement = render({ title: 'No goals yet' }).nativeElement;
    expect(el.querySelectorAll('h1,h2,h3,h4,h5,h6').length).toBe(0);
  });

  it('hides the action when showAction is false even if a label is set', () => {
    const el: HTMLElement = render({ title: 'Empty', actionLabel: 'New Goal', showAction: false }).nativeElement;
    expect(el.querySelector('[data-testid="empty-state-action"]')).toBeNull();
  });

  it('shows the action when permitted and emits (action) on click', () => {
    const fixture = render({ title: 'Empty', actionLabel: 'New Goal', showAction: true });
    const spy = vi.fn();
    fixture.componentInstance.action.subscribe(spy);
    const btn = fixture.nativeElement.querySelector('[data-testid="empty-state-action"] button') as HTMLButtonElement;
    expect(btn).not.toBeNull();
    btn.click();
    expect(spy).toHaveBeenCalledOnce();
  });
});
