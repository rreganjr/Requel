import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { ActorService } from './actor.service';

describe('ActorService', () => {
  let service: ActorService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(ActorService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('listActors() sends GET to the project actors endpoint', async () => {
    const promise = service.listActors('My Project');
    const req = httpMock.expectOne('/api/projects/My%20Project/actors');
    expect(req.request.method).toBe('GET');
    req.flush([{ id: 1, name: 'Administrator', text: null }]);
    const result = await promise;
    expect(result[0].name).toBe('Administrator');
  });

  it('getActor() sends GET to the individual actor endpoint', async () => {
    const promise = service.getActor('My Project', 5);
    const req = httpMock.expectOne('/api/projects/My%20Project/actors/5');
    req.flush({ id: 5, name: 'End User', text: 'A person who uses the system.' });
    const result = await promise;
    expect(result.id).toBe(5);
  });
});
