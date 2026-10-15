import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { ScenarioService } from './scenario.service';

describe('ScenarioService', () => {
  let service: ScenarioService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(ScenarioService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('listScenarios() sends GET to the project scenarios endpoint', async () => {
    const promise = service.listScenarios('My Project');
    const req = httpMock.expectOne('/api/projects/My%20Project/scenarios');
    expect(req.request.method).toBe('GET');
    req.flush([{ id: 1, name: 'Successful login', text: null, type: 'Primary', steps: [] }]);
    const result = await promise;
    expect(result[0].name).toBe('Successful login');
  });

  it('getScenario() sends GET to the individual scenario endpoint', async () => {
    const promise = service.getScenario('My Project', 9);
    const req = httpMock.expectOne('/api/projects/My%20Project/scenarios/9');
    req.flush({ id: 9, name: 'Failed login', text: null, type: 'Alternative', steps: [] });
    const result = await promise;
    expect(result.id).toBe(9);
  });
});
