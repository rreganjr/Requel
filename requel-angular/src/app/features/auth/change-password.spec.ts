import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter, Router } from '@angular/router';
import { ChangePasswordComponent } from './change-password';
import { AuthService } from '../../core/auth.service';

describe('ChangePasswordComponent (#381)', () => {
  let auth: {
    changePassword: ReturnType<typeof vi.fn>;
    mustChangePassword: ReturnType<typeof vi.fn>;
    logout: ReturnType<typeof vi.fn>;
  };

  function create(required = true) {
    auth = {
      changePassword: vi.fn().mockResolvedValue(null),
      mustChangePassword: vi.fn().mockReturnValue(required),
      logout: vi.fn(),
    };
    TestBed.configureTestingModule({
      imports: [ChangePasswordComponent],
      providers: [provideNoopAnimations(), provideRouter([]), { provide: AuthService, useValue: auth }],
    });
    const fixture = TestBed.createComponent(ChangePasswordComponent);
    fixture.detectChanges();
    return { fixture, comp: fixture.componentInstance, el: fixture.nativeElement as HTMLElement };
  }

  function fill(comp: ChangePasswordComponent, current: string, next: string, confirm: string) {
    comp.form.setValue({ currentPassword: current, newPassword: next, confirmPassword: confirm });
  }

  it('says the change is required when the user was sent here', () => {
    const { el } = create(true);
    expect(el.querySelector('h1')?.textContent?.trim()).toBe('Change your password');
    expect(el.querySelector('[data-testid="change-password-required"]')).not.toBeNull();
  });

  it('does not say so otherwise', () => {
    const { el } = create(false);
    expect(el.querySelector('[data-testid="change-password-required"]')).toBeNull();
  });

  it('does not submit an incomplete or mismatched form', async () => {
    const { comp } = create();
    await comp.onSubmit();
    fill(comp, 'old', 'new-one', 'new-two');
    await comp.onSubmit();
    expect(auth.changePassword).not.toHaveBeenCalled();
    expect(comp.form.controls.confirmPassword.hasError('passwordMismatch')).toBe(true);
  });

  it('changes the password and goes to the dashboard', async () => {
    const { comp } = create();
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    fill(comp, 'admin', 'better-one', 'better-one');
    await comp.onSubmit();
    expect(auth.changePassword).toHaveBeenCalledWith({
      currentPassword: 'admin', newPassword: 'better-one', confirmPassword: 'better-one',
    });
    expect(navigate).toHaveBeenCalledWith(['/']);
    expect(comp.saving()).toBe(false);
  });

  it('shows the server refusal on the field it names', async () => {
    const { comp } = create();
    auth.changePassword.mockResolvedValue({
      success: false, entity: null, entityType: null, error: 'Password not changed.',
      violations: [{ field: 'newPassword', message: 'New password must be different from the current password.' }],
    });
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate');
    fill(comp, 'admin', 'admin', 'admin');
    await comp.onSubmit();
    expect(navigate).not.toHaveBeenCalled();
    expect(comp.form.controls.newPassword.invalid).toBe(true);
    expect(comp.errorMessage()).toBeNull();
  });

  it('shows a page-level error when the request fails', async () => {
    const { comp } = create();
    auth.changePassword.mockRejectedValue(new Error('Network down'));
    fill(comp, 'admin', 'better-one', 'better-one');
    await comp.onSubmit();
    expect(comp.errorMessage()).toBe('Network down');
  });

  it('signs out', () => {
    const { comp } = create();
    comp.onSignOut();
    expect(auth.logout).toHaveBeenCalled();
  });
});
