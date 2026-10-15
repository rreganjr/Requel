import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { TermService } from './term.service';

describe('TermService', () => {
  let service: TermService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(TermService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('listTerms() sends GET to the project terms endpoint', async () => {
    const promise = service.listTerms('My Project');
    const req = httpMock.expectOne('/api/projects/My%20Project/terms');
    expect(req.request.method).toBe('GET');
    req.flush([{ id: 1, name: 'stakeholder', text: 'A person with interest in the system.', canonicalTermId: null }]);
    const result = await promise;
    expect(result[0].name).toBe('stakeholder');
  });

  it('getTerm() sends GET to the individual term endpoint', async () => {
    const promise = service.getTerm('My Project', 4);
    const req = httpMock.expectOne('/api/projects/My%20Project/terms/4');
    req.flush({ id: 4, name: 'req', text: 'Short for requirement.', canonicalTermId: 1 });
    const result = await promise;
    expect(result.id).toBe(4);
  });
});
