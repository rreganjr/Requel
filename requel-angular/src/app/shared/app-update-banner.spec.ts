import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { UpdateBannerComponent } from './app-update-banner';

function setup(over: Partial<UpdateBannerComponent> = {}) {
  TestBed.configureTestingModule({
    imports: [UpdateBannerComponent],
    providers: [provideNoopAnimations()]
  });
  const fixture = TestBed.createComponent(UpdateBannerComponent);
  Object.assign(fixture.componentInstance, { message: 'This goal was changed elsewhere.', testid: 'goal-update', ...over });
  fixture.detectChanges();
  return { fixture, comp: fixture.componentInstance, el: fixture.nativeElement as HTMLElement };
}

describe('UpdateBannerComponent (issue #140)', () => {
  it('renders the message inside a role="status" region', () => {
    const { el } = setup();
    const banner = el.querySelector('[data-testid="goal-update"]');
    expect(banner?.getAttribute('role')).toBe('status');
    expect(banner?.textContent).toContain('This goal was changed elsewhere.');
  });

  it('emits (reload) when the Reload button is clicked', () => {
    const { el, comp } = setup();
    let reloaded = 0;
    comp.reload.subscribe(() => reloaded++);
    (el.querySelector('[data-testid="goal-update-reload"] button') as HTMLButtonElement).click();
    expect(reloaded).toBe(1);
  });

  it('emits (dismiss) with an accessible close control', () => {
    const { el, comp } = setup();
    let dismissed = 0;
    comp.dismiss.subscribe(() => dismissed++);
    const close = el.querySelector('[data-testid="goal-update-dismiss"] button') as HTMLButtonElement;
    expect(close.getAttribute('aria-label')).toBe('Dismiss');
    close.click();
    expect(dismissed).toBe(1);
  });
});
