import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { TagService } from './tag.service';

describe('TagService', () => {
  let service: TagService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(TagService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('getTagsForProject() sends GET /api/tags with projectName param', async () => {
    const promise = service.getTagsForProject('My Project');
    const req = httpMock.expectOne(r =>
      r.url === '/api/tags' && r.params.get('projectName') === 'My Project');
    expect(req.request.method).toBe('GET');
    req.flush([]);
    expect(await promise).toEqual([]);
  });

  it('getTagsForProject() omits projectName when not provided', async () => {
    const promise = service.getTagsForProject();
    const req = httpMock.expectOne(r => r.url === '/api/tags' && !r.params.has('projectName'));
    req.flush([]);
    await promise;
  });

  it('getTagsOnEntity() sends GET /api/tags/on-entity with entity params', async () => {
    const promise = service.getTagsOnEntity('Goal', 42);
    const req = httpMock.expectOne(r =>
      r.url === '/api/tags/on-entity' &&
      r.params.get('entityType') === 'Goal' &&
      r.params.get('entityId') === '42');
    expect(req.request.method).toBe('GET');
    req.flush([]);
    await promise;
  });

  it('getCategories() sends GET /api/tags/categories', async () => {
    const promise = service.getCategories('My Project');
    const req = httpMock.expectOne(r =>
      r.url === '/api/tags/categories' && r.params.get('projectName') === 'My Project');
    req.flush(['type']);
    expect(await promise).toEqual(['type']);
  });

  it('getEntitiesWithTag() sends GET /api/tags/{id}/entities', async () => {
    const promise = service.getEntitiesWithTag(7);
    const req = httpMock.expectOne('/api/tags/7/entities');
    expect(req.request.method).toBe('GET');
    req.flush([{ entityType: 'Goal', entityId: 1 }]);
    expect((await promise).length).toBe(1);
  });

  it('editTag() dispatches EditTag command with normalized payload', async () => {
    const promise = service.editTag('My Project', 'type', 'business-rule');
    const req = httpMock.expectOne('/api/commands/EditTag');
    expect(req.request.body).toMatchObject({
      projectName: 'My Project', category: 'type', value: 'business-rule', tagId: null
    });
    req.flush({ success: true, entityType: 'EditTag', entity: { id: 5 }, error: null, violations: null });
    expect((await promise).success).toBe(true);
  });

  it('assignTag() dispatches AssignTag command', async () => {
    const promise = service.assignTag(5, 'Goal', 1);
    const req = httpMock.expectOne('/api/commands/AssignTag');
    expect(req.request.body).toMatchObject({ tagId: 5, entityType: 'Goal', entityId: 1 });
    req.flush({ success: true, entityType: 'AssignTag', entity: null, error: null, violations: null });
    await promise;
  });

  it('unassignTag() dispatches UnassignTag command', async () => {
    const promise = service.unassignTag(5, 'Goal', 1);
    const req = httpMock.expectOne('/api/commands/UnassignTag');
    expect(req.request.body).toMatchObject({ tagId: 5, entityType: 'Goal', entityId: 1 });
    req.flush({ success: true, entityType: 'UnassignTag', entity: null, error: null, violations: null });
    await promise;
  });

  it('deleteTag() dispatches DeleteTag command', async () => {
    const promise = service.deleteTag(5);
    const req = httpMock.expectOne('/api/commands/DeleteTag');
    expect(req.request.body).toMatchObject({ tagId: 5 });
    req.flush({ success: true, entityType: 'DeleteTag', entity: null, error: null, violations: null });
    await promise;
  });
});
