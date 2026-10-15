import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { AnnotationService } from './annotation.service';

describe('AnnotationService', () => {
  let service: AnnotationService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(AnnotationService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('getAnnotations() sends GET /api/annotations with correct query params', async () => {
    const promise = service.getAnnotations('My Project', 'Goal', 1);
    const req = httpMock.expectOne(r =>
      r.url === '/api/annotations' &&
      r.params.get('projectName') === 'My Project' &&
      r.params.get('entityType') === 'Goal' &&
      r.params.get('entityId') === '1'
    );
    expect(req.request.method).toBe('GET');
    req.flush({ issues: [], notes: [] });
    const result = await promise;
    expect(result.issues).toEqual([]);
  });

  it('addIssue() dispatches EditIssue command', async () => {
    const promise = service.addIssue('My Project', 'Goal', 1, 'Ambiguous goal', true);
    const req = httpMock.expectOne('/api/commands/EditIssue');
    expect(req.request.body).toMatchObject({ entityType: 'Goal', entityId: 1, mustBeResolved: true });
    req.flush({ success: true, entityType: 'EditIssue', entity: null, error: null, violations: null });
    const result = await promise;
    expect(result.success).toBe(true);
  });

  it('addIssue() sends MEDIUM by default and the given severity otherwise (#271)', async () => {
    const byDefault = service.addIssue('My Project', 'Goal', 1, 'Default severity', false);
    const first = httpMock.expectOne('/api/commands/EditIssue');
    expect(first.request.body).toMatchObject({ severity: 'MEDIUM' });
    first.flush({ success: true, entityType: 'EditIssue', entity: null, error: null, violations: null });
    await byDefault;

    const high = service.addIssue('My Project', 'Goal', 1, 'High severity', false, 'HIGH');
    const second = httpMock.expectOne('/api/commands/EditIssue');
    expect(second.request.body).toMatchObject({ severity: 'HIGH' });
    second.flush({ success: true, entityType: 'EditIssue', entity: null, error: null, violations: null });
    await high;
  });

  it('resolveIssue() dispatches ResolveIssue command', async () => {
    const promise = service.resolveIssue('My Project', 10, 5);
    const req = httpMock.expectOne('/api/commands/ResolveIssue');
    expect(req.request.body).toMatchObject({ issueId: 10, positionId: 5 });
    req.flush({ success: true, entityType: 'ResolveIssue', entity: null, error: null, violations: null });
    const result = await promise;
    expect(result.success).toBe(true);
  });
});
