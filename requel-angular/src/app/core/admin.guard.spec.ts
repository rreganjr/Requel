import { TestBed } from '@angular/core/testing';
import { provideRouter, UrlTree } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { adminGuard } from './admin.guard';
import { AuthService } from './auth.service';
import { UserDto } from '../models/user';

function makeUser(roles: string[]): UserDto {
  return {
    id: 1,
    username: 'tester',
    name: 'Test User',
    emailAddress: null,
    phoneNumber: null,
    organizationName: null,
    roles,
    permissions: [],
    permissionsByRole: null,
    version: 0,
  };
}

describe('adminGuard', () => {
  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
      ],
    });
  });

  afterEach(() => localStorage.clear());

  it('returns true when the authenticated user has the SystemAdminUserRole', () => {
    const auth = TestBed.inject(AuthService);
    auth.user.set(makeUser(['SystemAdminUserRole']));

    const result = TestBed.runInInjectionContext(() => adminGuard({} as any, {} as any));
    expect(result).toBe(true);
  });

  it('returns a UrlTree to / when the authenticated user is missing the admin role', () => {
    const auth = TestBed.inject(AuthService);
    auth.user.set(makeUser(['ProjectUserRole']));

    const result = TestBed.runInInjectionContext(() => adminGuard({} as any, {} as any));
    expect(result).toBeInstanceOf(UrlTree);
    expect((result as UrlTree).toString()).toBe('/');
  });

  it('returns a UrlTree to / when there is no authenticated user', () => {
    const result = TestBed.runInInjectionContext(() => adminGuard({} as any, {} as any));
    expect(result).toBeInstanceOf(UrlTree);
    expect((result as UrlTree).toString()).toBe('/');
  });

  it('returns a UrlTree to / when the user has roles but no admin role', () => {
    const auth = TestBed.inject(AuthService);
    auth.user.set(makeUser([]));

    const result = TestBed.runInInjectionContext(() => adminGuard({} as any, {} as any));
    expect(result).toBeInstanceOf(UrlTree);
    expect((result as UrlTree).toString()).toBe('/');
  });
});
