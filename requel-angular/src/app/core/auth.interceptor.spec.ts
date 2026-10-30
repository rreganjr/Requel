import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptors, HttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { Component } from '@angular/core';

@Component({ template: '' })
class StubLoginComponent {}
import { firstValueFrom } from 'rxjs';
import { authInterceptor } from './auth.interceptor';

describe('authInterceptor', () => {
  let httpMock: HttpTestingController;
  let http: HttpClient;

  function setup(token: string | null) {
    localStorage.clear();
    if (token) {
      localStorage.setItem('requel_token', token);
    }
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        provideRouter([
          { path: 'login', component: StubLoginComponent },
          { path: 'change-password', component: StubLoginComponent },
        ])
      ]
    });
    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  }

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('attaches Authorization header for /api/ requests when a token is present', async () => {
    setup('my-jwt');
    const promise = firstValueFrom(http.get('/api/projects'));
    const req = httpMock.expectOne('/api/projects');
    expect(req.request.headers.get('Authorization')).toBe('Bearer my-jwt');
    req.flush([]);
    await promise;
  });

  it('does not attach Authorization header when no token is stored', async () => {
    setup(null);
    const promise = firstValueFrom(http.get('/api/projects'));
    const req = httpMock.expectOne('/api/projects');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush([]);
    await promise;
  });

  it('does not attach Authorization header for non-/api/ requests', async () => {
    setup('my-jwt');
    const promise = firstValueFrom(http.get('/public/health'));
    const req = httpMock.expectOne('/public/health');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({ ok: true });
    await promise;
  });

  it('calls logout() on 401 response for non-login requests', async () => {
    setup('expired-token');
    const promise = firstValueFrom(http.get('/api/projects')).catch(() => null);
    const req = httpMock.expectOne('/api/projects');
    req.flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });
    await promise;
    // After 401 the interceptor calls authService.logout() which clears the token
    expect(localStorage.getItem('requel_token')).toBeNull();
  });

  it('does not call logout() on 401 from the login endpoint itself', async () => {
    setup('stale-token');
    const promise = firstValueFrom(http.post('/api/auth/login', {})).catch(() => null);
    const req = httpMock.expectOne('/api/auth/login');
    req.flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });
    await promise;
    // Token should still be in storage — logout was NOT triggered
    expect(localStorage.getItem('requel_token')).toBe('stale-token');
  });

  it('sends the user to change-password on a 403 PASSWORD_CHANGE_REQUIRED (#381)', async () => {
    setup('flagged-token');
    localStorage.setItem('requel_user', JSON.stringify({ username: 'ron', mustChangePassword: false }));
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    const promise = firstValueFrom(http.get('/api/projects')).catch(() => null);
    const req = httpMock.expectOne('/api/projects');
    req.flush({ error: 'PASSWORD_CHANGE_REQUIRED', message: 'Change it.' },
      { status: 403, statusText: 'Forbidden' });
    await promise;
    expect(navigate).toHaveBeenCalledWith(['/change-password']);
    expect(JSON.parse(localStorage.getItem('requel_user')!).mustChangePassword).toBe(true);
    // Still signed in: the change itself needs the token.
    expect(localStorage.getItem('requel_token')).toBe('flagged-token');
  });

  it('leaves any other 403 to the caller', async () => {
    setup('my-jwt');
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate');
    const promise = firstValueFrom(http.get('/api/projects')).catch(() => null);
    const req = httpMock.expectOne('/api/projects');
    req.flush({ error: 'FORBIDDEN' }, { status: 403, statusText: 'Forbidden' });
    await promise;
    expect(navigate).not.toHaveBeenCalled();
    expect(localStorage.getItem('requel_token')).toBe('my-jwt');
  });
});
