import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { ProjectService } from './project.service';
import { ProjectDto } from '../models/project';

const MOCK_PROJECT: ProjectDto = {
  id: 1, version: 0, name: 'My Project', description: null, organizationName: 'Acme',
  createdBy: 'admin', status: null,
  stakeholderCount: 0, goalCount: 2, storyCount: 0, actorCount: 0,
  useCaseCount: 0, scenarioCount: 0, glossaryTermCount: 0, reportGeneratorCount: 0
};

describe('ProjectService', () => {
  let service: ProjectService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(ProjectService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('listProjects() sends GET /api/projects and returns the array', async () => {
    const promise = service.listProjects();
    const req = httpMock.expectOne('/api/projects');
    expect(req.request.method).toBe('GET');
    req.flush([MOCK_PROJECT]);
    const result = await promise;
    expect(result.length).toBe(1);
    expect(result[0].name).toBe('My Project');
  });

  it('getProject() sends GET /api/projects/:name with encoding', async () => {
    const promise = service.getProject('My Project');
    const req = httpMock.expectOne('/api/projects/My%20Project');
    req.flush(MOCK_PROJECT);
    const result = await promise;
    expect(result.name).toBe('My Project');
  });

  it('notifyTreeChanged() emits on the onTreeChanged observable', async () => {
    let emitted = false;
    service.onTreeChanged.subscribe(() => { emitted = true; });
    service.notifyTreeChanged();
    expect(emitted).toBe(true);
  });

  it('getExportUrl() returns a correctly encoded URL', () => {
    const url = service.getExportUrl('My Project');
    expect(url).toBe('/api/projects/My%20Project/export');
  });

  it('downloadProjectXml() GETs the export URL as a blob (auth interceptor adds bearer)', async () => {
    const promise = service.downloadProjectXml('My Project');
    const req = httpMock.expectOne('/api/projects/My%20Project/export');
    expect(req.request.method).toBe('GET');
    expect(req.request.responseType).toBe('blob');
    const xmlBlob = new Blob(['<project name="My Project"/>'], { type: 'application/xml' });
    req.flush(xmlBlob);
    const result = await promise;
    expect(result).toBeInstanceOf(Blob);
    expect(await result.text()).toBe('<project name="My Project"/>');
  });
});
