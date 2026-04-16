import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter, Router, ActivatedRoute, convertToParamMap } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { BehaviorSubject } from 'rxjs';
import { OpenIssuesComponent } from './open-issues';

const MOCK_ISSUES = [
  { issueId: 1, issueText: 'Missing details', mustBeResolved: true,
    entityType: 'Goal', entityId: 10, entityName: 'Goal A' },
  { issueId: 2, issueText: 'Ambiguous text', mustBeResolved: false,
    entityType: 'Story', entityId: 20, entityName: 'Story B' }
];

const flush = () => new Promise(r => setTimeout(r, 0));

describe('OpenIssuesComponent', () => {
  let paramMap$: BehaviorSubject<ReturnType<typeof convertToParamMap>>;
  let httpTesting: HttpTestingController;
  let router: Router;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  let fixture: any;
  let comp: OpenIssuesComponent;

  beforeEach(() => {
    paramMap$ = new BehaviorSubject(convertToParamMap({ name: 'proj1' }));

    TestBed.configureTestingModule({
      imports: [OpenIssuesComponent],
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: ActivatedRoute, useValue: { paramMap: paramMap$.asObservable() } }
      ]
    });

    httpTesting = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
    fixture = TestBed.createComponent(OpenIssuesComponent);
    comp = fixture.componentInstance;
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('loadIssues makes GET request to project open-issues URL', async () => {
    fixture.detectChanges();
    const req = httpTesting.expectOne(r => r.url.includes('open-issues'));
    expect(req.request.method).toBe('GET');
    req.flush(MOCK_ISSUES);
    await flush();
  });

  it('issues() populated from HTTP response', async () => {
    fixture.detectChanges();
    const req = httpTesting.expectOne(r => r.url.includes('open-issues'));
    req.flush(MOCK_ISSUES);
    await flush();
    expect(comp.issues().length).toBe(2);
    expect(comp.issues()[0].entityName).toBe('Goal A');
  });

  it('mustResolveCount() counts mustBeResolved issues', async () => {
    fixture.detectChanges();
    const req = httpTesting.expectOne(r => r.url.includes('open-issues'));
    req.flush(MOCK_ISSUES);
    await flush();
    expect(comp.mustResolveCount()).toBe(1);
  });

  it('navigateTo routes to correct entity path for known type', async () => {
    fixture.detectChanges();
    httpTesting.expectOne(r => r.url.includes('open-issues')).flush([]);
    await flush();
    comp.navigateTo(MOCK_ISSUES[0]);
    expect(router.navigate).toHaveBeenCalledWith(['/projects', 'proj1', 'goals', 10]);
  });

  it('errorMessage set when HTTP request fails', async () => {
    fixture.detectChanges();
    const req = httpTesting.expectOne(r => r.url.includes('open-issues'));
    req.flush('Server Error', { status: 500, statusText: 'Internal Server Error' });
    await flush();
    expect(comp.errorMessage()).toBe('Failed to load open issues.');
    expect(comp.loading()).toBe(false);
  });
});
