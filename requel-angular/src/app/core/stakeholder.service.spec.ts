import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { StakeholderService } from './stakeholder.service';

describe('StakeholderService', () => {
  let service: StakeholderService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(StakeholderService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('listStakeholders() sends GET to the project stakeholders endpoint', async () => {
    const promise = service.listStakeholders('My Project');
    const req = httpMock.expectOne('/api/projects/My%20Project/stakeholders');
    expect(req.request.method).toBe('GET');
    req.flush([{ id: 1, name: 'admin', userStakeholder: true, permissions: [] }]);
    const result = await promise;
    expect(result[0].name).toBe('admin');
  });

  it('getStakeholder() sends GET to the individual stakeholder endpoint', async () => {
    const promise = service.getStakeholder('My Project', 11);
    const req = httpMock.expectOne('/api/projects/My%20Project/stakeholders/11');
    req.flush({ id: 11, name: 'admin', userStakeholder: true, permissions: [] });
    const result = await promise;
    expect(result.id).toBe(11);
  });

  it('getAvailablePermissions() sends GET to the stakeholder-permissions endpoint', async () => {
    const promise = service.getAvailablePermissions();
    const req = httpMock.expectOne('/api/projects/stakeholder-permissions');
    expect(req.request.method).toBe('GET');
    req.flush([{ id: 1, entityType: 'Goal', permissionType: 'Edit' }]);
    const result = await promise;
    expect(result[0].entityType).toBe('Goal');
  });
});
