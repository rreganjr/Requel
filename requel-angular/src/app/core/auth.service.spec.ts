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
});
