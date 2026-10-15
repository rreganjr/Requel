import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { LoginComponent } from './login';
import { AuthService } from '../../core/auth.service';

describe('LoginComponent', () => {
  let authServiceMock: { login: ReturnType<typeof vi.fn> };
  let comp: LoginComponent;

  beforeEach(() => {
    authServiceMock = { login: vi.fn().mockResolvedValue(undefined) };

    TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        { provide: AuthService, useValue: authServiceMock },
      ],
    });
    const fixture = TestBed.createComponent(LoginComponent);
    comp = fixture.componentInstance;
  });

  /** Fills the reactive form the way a user would leave it. */
  function fill(username: string, password: string): void {
    comp.form.setValue({ username, password });
  }

  it('renders exactly one <h1> page title and no <h2> (issue #135)', () => {
    const fixture = TestBed.createComponent(LoginComponent);
    fixture.detectChanges();
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelectorAll('h1').length).toBe(1);
    expect(el.querySelectorAll('h2').length).toBe(0);
    expect(el.querySelector('h1')?.textContent?.trim()).toBe('Requel');
  });

  it('onLogin() calls authService.login with the entered credentials', async () => {
    fill('admin', 'secret');
    await comp.onLogin();
    expect(authServiceMock.login).toHaveBeenCalledWith({ username: 'admin', password: 'secret' });
  });

  it('clears errorMessage and sets loading=false after successful login', async () => {
    fill('admin', 'secret');
    await comp.onLogin();
    expect(comp.errorMessage()).toBeNull();
    expect(comp.loading()).toBe(false);
  });

  it('sets errorMessage when authService.login throws', async () => {
    authServiceMock.login.mockRejectedValue(new Error('Invalid credentials'));
    fill('admin', 'wrong');
    await comp.onLogin();
    expect(comp.errorMessage()).toBe('Invalid credentials');
    expect(comp.loading()).toBe(false);
  });

  describe('reactive form (issue #132)', () => {
    it('starts invalid with both fields required', () => {
      expect(comp.form.invalid).toBe(true);
      expect(comp.form.controls.username.hasError('required')).toBe(true);
      expect(comp.form.controls.password.hasError('required')).toBe(true);
    });

    it('becomes valid once both fields have values', () => {
      fill('admin', 'secret');
      expect(comp.form.valid).toBe(true);
    });

    it.each([
      ['', 'secret'],
      ['admin', ''],
      ['', ''],
    ])('stays invalid for username=%p password=%p', (username, password) => {
      fill(username, password);
      expect(comp.form.invalid).toBe(true);
    });

    it('renders the submit button disabled while the form is invalid', () => {
      const fixture = TestBed.createComponent(LoginComponent);
      fixture.detectChanges();
      const button = (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>(
        '[data-testid="login-submit"] button'
      );
      expect(button?.disabled).toBe(true);
    });

    /**
     * A disabled button does not stop Enter from submitting the form, so the guard has
     * to live in onLogin too — otherwise a blank submit would reach the auth endpoint.
     */
    it('does not call the auth service when submitted while invalid', async () => {
      await comp.onLogin();
      expect(authServiceMock.login).not.toHaveBeenCalled();
    });

    it('marks fields touched and submitted on an invalid submit, so errors show', async () => {
      await comp.onLogin();
      expect(comp.submitted()).toBe(true);
      expect(comp.form.controls.username.touched).toBe(true);
      expect(comp.form.controls.password.touched).toBe(true);
    });

    it('shows the inline required error after an invalid submit', () => {
      const fixture = TestBed.createComponent(LoginComponent);
      fixture.detectChanges();
      void fixture.componentInstance.onLogin();
      fixture.detectChanges();

      const errors = (fixture.nativeElement as HTMLElement).querySelectorAll(
        '[data-testid="field-error"]'
      );
      expect(errors).toHaveLength(2);
      expect(errors[0].textContent?.trim()).toBe('This field is required.');
    });

    it('shows no inline errors before any interaction', () => {
      const fixture = TestBed.createComponent(LoginComponent);
      fixture.detectChanges();
      expect(
        (fixture.nativeElement as HTMLElement).querySelectorAll('[data-testid="field-error"]')
      ).toHaveLength(0);
    });

    it('re-enables the form after a failed login so the user can retry', async () => {
      authServiceMock.login.mockRejectedValue(new Error('Invalid credentials'));
      fill('admin', 'wrong');
      await comp.onLogin();
      expect(comp.form.enabled).toBe(true);
    });

    it('binds each label to its control (issue #138)', () => {
      const fixture = TestBed.createComponent(LoginComponent);
      fixture.detectChanges();
      const el = fixture.nativeElement as HTMLElement;

      const labels = Array.from(el.querySelectorAll('label'));
      expect(labels.map(l => l.getAttribute('for'))).toEqual(['username', 'password']);
      expect(el.querySelector('#username')).not.toBeNull();
    });

    it('keeps the autocomplete hints password managers rely on', () => {
      const fixture = TestBed.createComponent(LoginComponent);
      fixture.detectChanges();
      const el = fixture.nativeElement as HTMLElement;

      expect(el.querySelector('#username')?.getAttribute('autocomplete')).toBe('username');
      expect(
        el.querySelector('p-password input')?.getAttribute('autocomplete')
      ).toBe('current-password');
    });
  });
});
