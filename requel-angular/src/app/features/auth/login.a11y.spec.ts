import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { LoginComponent } from './login';
import { AuthService } from '../../core/auth.service';
import { expectNoAxeViolations } from '../../shared/testing/a11y';

describe('LoginComponent accessibility (issue #132)', () => {
  function render(): { el: HTMLElement; comp: LoginComponent; detect: () => void } {
    TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        { provide: AuthService, useValue: { login: vi.fn().mockResolvedValue(undefined) } },
      ],
    });
    const fixture = TestBed.createComponent(LoginComponent);
    fixture.detectChanges();
    return {
      el: fixture.nativeElement as HTMLElement,
      comp: fixture.componentInstance,
      detect: () => fixture.detectChanges(),
    };
  }

  it('has no axe-core violations at rest', async () => {
    const { el } = render();
    await expectNoAxeViolations(el);
  });

  it('has no axe-core violations with both fields in their error state', async () => {
    const { el, comp, detect } = render();
    await comp.onLogin();
    detect();

    expect(el.querySelectorAll('[data-testid="field-error"]')).toHaveLength(2);
    await expectNoAxeViolations(el);
  });

  it('has no axe-core violations with a page-level login failure showing', async () => {
    const { el, comp, detect } = render();
    comp.errorMessage.set('Invalid credentials');
    detect();
    await expectNoAxeViolations(el);
  });

  it('associates each error with its control via aria-describedby / aria-invalid', async () => {
    const { el, comp, detect } = render();
    await comp.onLogin();
    detect();

    const username = el.querySelector<HTMLInputElement>('#username')!;
    expect(username.getAttribute('aria-invalid')).toBe('true');

    const describedBy = username.getAttribute('aria-describedby');
    expect(describedBy).toBeTruthy();
    expect(el.querySelector(`#${describedBy}`)?.textContent?.trim()).toBe(
      'This field is required.'
    );
  });
});
