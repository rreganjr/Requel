import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { GoalService } from './goal.service';

describe('GoalService', () => {
  let service: GoalService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(GoalService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('listGoals() sends GET to the project goals endpoint', async () => {
    const promise = service.listGoals('My Project');
    const req = httpMock.expectOne('/api/projects/My%20Project/goals');
    expect(req.request.method).toBe('GET');
    req.flush([{ id: 1, name: 'Increase revenue', text: null }]);
    const result = await promise;
    expect(result[0].name).toBe('Increase revenue');
  });

  it('getGoal() sends GET to the individual goal endpoint', async () => {
    const promise = service.getGoal('My Project', 42);
    const req = httpMock.expectOne('/api/projects/My%20Project/goals/42');
    expect(req.request.method).toBe('GET');
    req.flush({ id: 42, name: 'Reduce costs', text: 'Cut expenses.' });
    const result = await promise;
    expect(result.id).toBe(42);
  });
});
