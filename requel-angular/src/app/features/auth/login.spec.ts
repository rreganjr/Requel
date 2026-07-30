import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { LoginComponent } from './login';
import { AuthService } from '../../core/auth.service';

describe('LoginComponent', () => {
  let authServiceMock: { login: ReturnType<typeof vi.fn> };
  let routerNavigateMock: ReturnType<typeof vi.fn>;
  let comp: LoginComponent;

  beforeEach(() => {
    authServiceMock = { login: vi.fn().mockResolvedValue(undefined) };
    routerNavigateMock = vi.fn().mockResolvedValue(true);

    TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        { provide: AuthService, useValue: authServiceMock },
      ]
    });
    const fixture = TestBed.createComponent(LoginComponent);
    comp = fixture.componentInstance;
  });

  it('renders exactly one <h1> page title and no <h2> (issue #135)', () => {
    const fixture = TestBed.createComponent(LoginComponent);
    fixture.detectChanges();
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelectorAll('h1').length).toBe(1);
    expect(el.querySelectorAll('h2').length).toBe(0);
    expect(el.querySelector('h1')?.textContent?.trim()).toBe('Requel');
  });

  it('onLogin() calls authService.login with the entered credentials', async () => {
    comp.username.set('admin');
    comp.password.set('secret');
    await comp.onLogin();
    expect(authServiceMock.login).toHaveBeenCalledWith({ username: 'admin', password: 'secret' });
  });

  it('clears errorMessage and sets loading=false after successful login', async () => {
    comp.username.set('admin');
    comp.password.set('secret');
    await comp.onLogin();
    expect(comp.errorMessage()).toBeNull();
    expect(comp.loading()).toBe(false);
  });

  it('sets errorMessage when authService.login throws', async () => {
    authServiceMock.login.mockRejectedValue(new Error('Invalid credentials'));
    comp.username.set('admin');
    comp.password.set('wrong');
    await comp.onLogin();
    expect(comp.errorMessage()).toBe('Invalid credentials');
    expect(comp.loading()).toBe(false);
  });
});
