import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { UserService } from './user.service';

describe('UserService', () => {
  let service: UserService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(UserService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('listUsers() sends GET /api/users', async () => {
    const promise = service.listUsers();
    const req = httpMock.expectOne('/api/users');
    expect(req.request.method).toBe('GET');
    req.flush([{ id: 1, username: 'admin' }]);
    const result = await promise;
    expect(result[0].username).toBe('admin');
  });

  it('getUser() sends GET /api/users/:username', async () => {
    const promise = service.getUser('admin');
    const req = httpMock.expectOne('/api/users/admin');
    req.flush({ id: 1, username: 'admin', name: 'Admin' });
    const result = await promise;
    expect(result.username).toBe('admin');
  });

  it('listOrganizations() sends GET /api/users/organizations', async () => {
    const promise = service.listOrganizations();
    const req = httpMock.expectOne('/api/users/organizations');
    req.flush([{ id: 1, name: 'Acme Corp' }]);
    const result = await promise;
    expect(result[0].name).toBe('Acme Corp');
  });

  it('listRoles() sends GET /api/users/roles', async () => {
    const promise = service.listRoles();
    const req = httpMock.expectOne('/api/users/roles');
    req.flush([{ roleName: 'SystemAdminUserRole', displayName: 'System Admin', availablePermissions: [] }]);
    const result = await promise;
    expect(result[0].roleName).toBe('SystemAdminUserRole');
  });
});
