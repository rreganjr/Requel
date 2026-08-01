import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter, Router, ActivatedRoute, convertToParamMap } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { BehaviorSubject } from 'rxjs';
import { OpenIssuesComponent } from './open-issues';
import { expectNoAxeViolations } from '../../shared/testing/a11y';

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

  it('routeFor returns the entity route for a known type', async () => {
    fixture.detectChanges();
    httpTesting.expectOne(r => r.url.includes('open-issues')).flush([]);
    await flush();
    expect(comp.routeFor(MOCK_ISSUES[0])).toEqual(['/projects', 'proj1', 'goals', 10]);
  });

  it('routeFor returns null for an unmapped entity type', async () => {
    fixture.detectChanges();
    httpTesting.expectOne(r => r.url.includes('open-issues')).flush([]);
    await flush();
    expect(comp.routeFor({ ...MOCK_ISSUES[0], entityType: 'Unknown' })).toBeNull();
  });

  it('renders a routerLink anchor for mapped issues', async () => {
    fixture.detectChanges();
    httpTesting.expectOne(r => r.url.includes('open-issues')).flush(MOCK_ISSUES);
    await flush();
    fixture.detectChanges();
    const a = fixture.nativeElement.querySelector('[data-testid="open-issue-entity-link"]');
    expect(a.tagName).toBe('A');
    expect(a.getAttribute('href')).toBe('/projects/proj1/goals/10');
  });

  // Issue #141 (WCAG 1.4.1 Use of Color): the "Required" column must convey
  // required vs optional with text, not color alone. Required renders "Yes",
  // optional renders "No" (previously a bare em-dash, which is not a label).
  it('renders text labels (not color alone) for the Required column', async () => {
    fixture.detectChanges();
    httpTesting.expectOne(r => r.url.includes('open-issues')).flush(MOCK_ISSUES);
    await flush();
    fixture.detectChanges();
    const required = fixture.nativeElement.querySelector('[data-testid="open-issue-required"]');
    const optional = fixture.nativeElement.querySelector('[data-testid="open-issue-optional"]');
    expect(required.textContent.trim()).toBe('Yes');
    expect(optional.textContent.trim()).toBe('No');
  });

  it('has no axe-core violations for the issues table', async () => {
    fixture.detectChanges();
    httpTesting.expectOne(r => r.url.includes('open-issues')).flush(MOCK_ISSUES);
    await flush();
    fixture.detectChanges();
    await expectNoAxeViolations(fixture.nativeElement);
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
