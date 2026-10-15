import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter, Router, ActivatedRoute, convertToParamMap } from '@angular/router';
import { BehaviorSubject } from 'rxjs';
import { TermListComponent } from './term-list';
import { TermService } from '../../core/term.service';
import { PermissionService } from '../../core/permission.service';

const MOCK_TERMS = [
  { id: 20, version: 0, name: 'Actor', text: 'A role played by a user.', createdBy: 'alice',
    canonicalTermId: null, canonicalTermName: null, alternateTerms: null, referers: null },
  { id: 21, version: 0, name: 'Scenario', text: 'A sequence of steps.', createdBy: 'bob',
    canonicalTermId: null, canonicalTermName: null, alternateTerms: null, referers: null }
];

const flush = () => new Promise(r => setTimeout(r, 0));

describe('TermListComponent', () => {
  let paramMap$: BehaviorSubject<ReturnType<typeof convertToParamMap>>;
  let termServiceMock: { listTerms: ReturnType<typeof vi.fn> };
  let permissionServiceMock: { loadForProject: ReturnType<typeof vi.fn>; canEdit: ReturnType<typeof vi.fn> };
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  let fixture: any;
  let comp: TermListComponent;
  let router: Router;

  beforeEach(() => {
    paramMap$ = new BehaviorSubject(convertToParamMap({ name: 'proj1' }));

    termServiceMock = { listTerms: vi.fn().mockResolvedValue(MOCK_TERMS) };
    permissionServiceMock = {
      loadForProject: vi.fn().mockResolvedValue(undefined),
      canEdit: vi.fn().mockReturnValue(true)
    };

    TestBed.configureTestingModule({
      imports: [TermListComponent],
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { paramMap: paramMap$.asObservable() } },
        { provide: TermService, useValue: termServiceMock },
        { provide: PermissionService, useValue: permissionServiceMock }
      ]
    });
    fixture = TestBed.createComponent(TermListComponent);
    comp = fixture.componentInstance;
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
  });

  it('terms() populated from termService.listTerms on init', async () => {
    fixture.detectChanges();
    await flush();
    expect(termServiceMock.listTerms).toHaveBeenCalledWith('proj1');
    expect(comp.terms().length).toBe(2);
    expect(comp.terms()[0].name).toBe('Actor');
    expect(comp.loading()).toBe(false);
  });

  it('canEdit() reflects permissionService.canEdit("GlossaryTerm")', async () => {
    fixture.detectChanges();
    await flush();
    expect(permissionServiceMock.canEdit).toHaveBeenCalledWith('GlossaryTerm');
    expect(comp.canEdit()).toBe(true);
  });

  it('onRowSelect navigates to term editor', async () => {
    fixture.detectChanges();
    await flush();
    comp.onRowSelect({ data: MOCK_TERMS[0] });
    expect(router.navigate).toHaveBeenCalledWith(['/projects', 'proj1', 'terms', 20]);
  });

  it('onNewTerm navigates to new term', async () => {
    fixture.detectChanges();
    await flush();
    comp.onNewTerm();
    expect(router.navigate).toHaveBeenCalledWith(['/projects', 'proj1', 'terms', 'new']);
  });

  it('errorMessage set when listTerms throws', async () => {
    termServiceMock.listTerms.mockRejectedValue(new Error('Network error'));
    fixture.detectChanges();
    await flush();
    expect(comp.errorMessage()).toBe('Failed to load glossary terms.');
    expect(comp.loading()).toBe(false);
  });

  it('renders the name as a real link and still navigates on row click (issue #129)', async () => {
    fixture.detectChanges();
    await flush();
    fixture.detectChanges();
    const el: HTMLElement = fixture.nativeElement;

    // AC #2 - the term name cell is a real link to the term editor route
    const link = el.querySelector('tbody tr.dt-row a.dt-link') as HTMLAnchorElement;
    expect(link).not.toBeNull();
    expect(link.textContent?.trim()).toBe('Actor');
    expect(link.getAttribute('href')).toBe('/projects/proj1/terms/20');

    // AC #3 - whole-row click still navigates to the same target (kept as a convenience)
    const row = el.querySelector('tbody tr.dt-row') as HTMLElement;
    row.dispatchEvent(new MouseEvent('click', { bubbles: true }));
    expect(router.navigate).toHaveBeenCalledWith(['/projects', 'proj1', 'terms', 20]);
  });
});
