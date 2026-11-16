import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { ChangePasswordComponent } from './change-password';
import { AuthService } from '../../core/auth.service';
import { expectNoAxeViolations } from '../../shared/testing/a11y';

describe('ChangePasswordComponent accessibility (#381)', () => {
  function render() {
    TestBed.configureTestingModule({
      imports: [ChangePasswordComponent],
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        {
          provide: AuthService,
          useValue: { changePassword: vi.fn(), mustChangePassword: vi.fn().mockReturnValue(true), logout: vi.fn() },
        },
      ],
    });
    const fixture = TestBed.createComponent(ChangePasswordComponent);
    fixture.detectChanges();
    return { el: fixture.nativeElement as HTMLElement, comp: fixture.componentInstance, detect: () => fixture.detectChanges() };
  }

  it('has no axe-core violations at rest', async () => {
    const { el } = render();
    await expectNoAxeViolations(el);
  });

  it('has no axe-core violations with every field in its error state', async () => {
    const { el, comp, detect } = render();
    await comp.onSubmit();
    detect();
    expect(el.querySelectorAll('[data-testid="field-error"]').length).toBe(3);
    await expectNoAxeViolations(el);
  });

  it('has no axe-core violations with a page-level error showing', async () => {
    const { el, comp, detect } = render();
    comp.errorMessage.set('Password not changed.');
    detect();
    await expectNoAxeViolations(el);
  });
});
