import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { Component } from '@angular/core';

@Component({ template: '' })
class StubLoginComponent {}
import { AuthService } from './auth.service';
import { LoginResponse, UserDto } from '../models/user';

const MOCK_USER: UserDto = {
  id: 1, username: 'admin', name: 'Admin User',
  emailAddress: null, phoneNumber: null, organizationName: null,
  roles: ['SystemAdminUserRole'], permissions: [], permissionsByRole: null, version: 0
};

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([{ path: 'login', component: StubLoginComponent }])
      ]
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('isAuthenticated is false when no token in storage', () => {
    expect(service.isAuthenticated()).toBe(false);
    expect(service.token()).toBeNull();
  });

  it('login() posts credentials, stores token, and updates signals', async () => {
    const response: LoginResponse = { token: 'test-jwt', user: MOCK_USER };

    const loginPromise = service.login({ username: 'admin', password: 'admin' });
    const req = httpMock.expectOne('/api/auth/login');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ username: 'admin', password: 'admin' });
    req.flush(response);
    await loginPromise;

    expect(service.token()).toBe('test-jwt');
    expect(service.isAuthenticated()).toBe(true);
    expect(service.user()?.username).toBe('admin');
    expect(localStorage.getItem('requel_token')).toBe('test-jwt');
  });

  it('logout() clears token, signals, storage, and navigates to /login', async () => {
    // Seed a session
    localStorage.setItem('requel_token', 'old-token');
    localStorage.setItem('requel_user', JSON.stringify(MOCK_USER));

    // Re-create service so it picks up the stored token
    TestBed.resetTestingModule();
    localStorage.setItem('requel_token', 'old-token');
    localStorage.setItem('requel_user', JSON.stringify(MOCK_USER));
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([{ path: 'login', component: StubLoginComponent }])
      ]
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);

    expect(service.isAuthenticated()).toBe(true);

    service.logout();

    expect(service.token()).toBeNull();
    expect(service.isAuthenticated()).toBe(false);
    expect(service.user()).toBeNull();
    expect(localStorage.getItem('requel_token')).toBeNull();
  });

  it('loads stored user from localStorage on construction', () => {
    localStorage.setItem('requel_user', JSON.stringify(MOCK_USER));
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([{ path: 'login', component: StubLoginComponent }])
      ]
    });
    const fresh = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
    expect(fresh.user()?.username).toBe('admin');
  });

  describe('changePassword (#381)', () => {
    const request = { currentPassword: 'old', newPassword: 'new', confirmPassword: 'new' };

    it('posts the change and replaces the session with the returned token and user', async () => {
      localStorage.setItem('requel_token', 'flagged-jwt');
      const promise = service.changePassword(request);
      const req = httpMock.expectOne('/api/auth/password');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(request);
      req.flush({ token: 'fresh-jwt', user: { ...MOCK_USER, mustChangePassword: false } });

      expect(await promise).toBeNull();
      expect(service.token()).toBe('fresh-jwt');
      expect(localStorage.getItem('requel_token')).toBe('fresh-jwt');
      expect(service.mustChangePassword()).toBe(false);
    });

    it('returns the refusal so the form can show its violations', async () => {
      const promise = service.changePassword(request);
      const refusal = {
        success: false, entity: null, entityType: null, error: 'Password not changed.',
        violations: [{ field: 'currentPassword', message: 'Current password is incorrect.' }],
      };
      httpMock.expectOne('/api/auth/password')
        .flush(refusal, { status: 422, statusText: 'Unprocessable Entity' });

      expect(await promise).toEqual(refusal);
      expect(service.token()).toBeNull();
    });

    it('throws on any other failure', async () => {
      const promise = service.changePassword(request);
      httpMock.expectOne('/api/auth/password').flush('boom', { status: 500, statusText: 'Server Error' });
      await expect(promise).rejects.toBeTruthy();
    });
  });

  it('mustChangePassword follows the stored user, and markPasswordChangeRequired sets it', () => {
    expect(service.mustChangePassword()).toBe(false);
    service.user.set(MOCK_USER);
    expect(service.mustChangePassword()).toBe(false);
    service.markPasswordChangeRequired();
    expect(service.mustChangePassword()).toBe(true);
    expect(JSON.parse(localStorage.getItem('requel_user')!).mustChangePassword).toBe(true);
  });
});
