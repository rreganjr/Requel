import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter, Router, ActivatedRoute, convertToParamMap } from '@angular/router';
import { BehaviorSubject } from 'rxjs';
import { ReportListComponent } from './report-list';
import { ReportService } from '../../core/report.service';
import { PermissionService } from '../../core/permission.service';

const MOCK_REPORTS = [
  { id: 40, version: 0, name: 'Requirements Spec', text: null, createdBy: 'alice' },
  { id: 41, version: 0, name: 'Actor Catalog', text: null, createdBy: 'bob' }
];

const flush = () => new Promise(r => setTimeout(r, 0));

describe('ReportListComponent', () => {
  let paramMap$: BehaviorSubject<ReturnType<typeof convertToParamMap>>;
  let reportServiceMock: { listReports: ReturnType<typeof vi.fn> };
  let permissionServiceMock: { loadForProject: ReturnType<typeof vi.fn>; canEdit: ReturnType<typeof vi.fn> };
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  let fixture: any;
  let comp: ReportListComponent;
  let router: Router;

  beforeEach(() => {
    paramMap$ = new BehaviorSubject(convertToParamMap({ name: 'proj1' }));

    reportServiceMock = { listReports: vi.fn().mockResolvedValue(MOCK_REPORTS) };
    permissionServiceMock = {
      loadForProject: vi.fn().mockResolvedValue(undefined),
      canEdit: vi.fn().mockReturnValue(true)
    };

    TestBed.configureTestingModule({
      imports: [ReportListComponent],
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { paramMap: paramMap$.asObservable() } },
        { provide: ReportService, useValue: reportServiceMock },
        { provide: PermissionService, useValue: permissionServiceMock }
      ]
    });
    fixture = TestBed.createComponent(ReportListComponent);
    comp = fixture.componentInstance;
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
  });

  it('reports() populated from reportService.listReports on init', async () => {
    fixture.detectChanges();
    await flush();
    expect(reportServiceMock.listReports).toHaveBeenCalledWith('proj1');
    expect(comp.reports().length).toBe(2);
    expect(comp.reports()[0].name).toBe('Requirements Spec');
    expect(comp.loading()).toBe(false);
  });

  it('canEdit() reflects permissionService.canEdit("ReportGenerator")', async () => {
    fixture.detectChanges();
    await flush();
    expect(permissionServiceMock.canEdit).toHaveBeenCalledWith('ReportGenerator');
    expect(comp.canEdit()).toBe(true);
  });

  it('onEdit navigates to report editor', async () => {
    fixture.detectChanges();
    await flush();
    comp.onEdit(MOCK_REPORTS[0]);
    expect(router.navigate).toHaveBeenCalledWith(['/projects', 'proj1', 'reports', 40]);
  });

  it('onNew navigates to new report', async () => {
    fixture.detectChanges();
    await flush();
    comp.onNew();
    expect(router.navigate).toHaveBeenCalledWith(['/projects', 'proj1', 'reports', 'new']);
  });

  it('errorMessage set when listReports throws', async () => {
    reportServiceMock.listReports.mockRejectedValue(new Error('Network error'));
    fixture.detectChanges();
    await flush();
    expect(comp.errorMessage()).toBe('Failed to load documents.');
    expect(comp.loading()).toBe(false);
  });

  it('name is a real link and the row is intentionally not click-navigable (issue #129)', async () => {
    fixture.detectChanges();
    await flush();
    fixture.detectChanges();
    const el: HTMLElement = fixture.nativeElement;

    // AC #2 - the document name cell is a real link to the report editor route
    const link = el.querySelector('tbody tr.dt-row a.dt-link') as HTMLAnchorElement;
    expect(link).not.toBeNull();
    expect(link.textContent?.trim()).toBe('Requirements Spec');
    expect(link.getAttribute('href')).toBe('/projects/proj1/reports/40');

    // AC #3 - this page opts out of whole-row click ([rowClickable]="false");
    // the name link and the row Edit/Run actions are the only nav paths.
    const row = el.querySelector('tbody tr.dt-row') as HTMLElement;
    expect(row.classList.contains('dt-row--clickable')).toBe(false);
    row.dispatchEvent(new MouseEvent('click', { bubbles: true }));
    expect(router.navigate).not.toHaveBeenCalled();
  });
});
