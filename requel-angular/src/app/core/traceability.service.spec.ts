import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { TraceabilityService } from './traceability.service';

describe('TraceabilityService (#497)', () => {
  it('reads the project traceability', async () => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    const http = TestBed.inject(HttpTestingController);
    const answer = TestBed.inject(TraceabilityService).getTraceability('My Project');
    http.expectOne('/api/projects/My%20Project/traceability').flush({ goals: [], gaps: [] });
    expect(await answer).toEqual({ goals: [], gaps: [] });
    http.verify();
  });
});
