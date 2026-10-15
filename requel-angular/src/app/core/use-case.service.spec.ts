import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { UseCaseService } from './use-case.service';

describe('UseCaseService', () => {
  let service: UseCaseService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(UseCaseService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('listUseCases() sends GET to the project use-cases endpoint', async () => {
    const promise = service.listUseCases('My Project');
    const req = httpMock.expectOne('/api/projects/My%20Project/use-cases');
    expect(req.request.method).toBe('GET');
    req.flush([{ id: 1, name: 'Login', text: null, primaryActorName: 'Administrator' }]);
    const result = await promise;
    expect(result[0].name).toBe('Login');
  });

  it('getUseCase() sends GET to the individual use-case endpoint', async () => {
    const promise = service.getUseCase('My Project', 3);
    const req = httpMock.expectOne('/api/projects/My%20Project/use-cases/3');
    req.flush({ id: 3, name: 'Login', text: 'Allow a user to log in.', primaryActorName: null });
    const result = await promise;
    expect(result.id).toBe(3);
  });
});
