import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter, Router, ActivatedRoute, convertToParamMap } from '@angular/router';
import { BehaviorSubject } from 'rxjs';
import { ConfirmationService, MessageService } from 'primeng/api';
import { ReportEditorComponent } from './report-editor';
import { ReportService } from '../../core/report.service';
import { PermissionService } from '../../core/permission.service';

const MOCK_REPORT = {
  id: 7, version: 0, name: 'Requirements Doc', text: '<xsl:stylesheet>...</xsl:stylesheet>'
};

const flush = () => new Promise(r => setTimeout(r, 0));

describe('ReportEditorComponent', () => {
  let paramMap$: BehaviorSubject<ReturnType<typeof convertToParamMap>>;
  let reportServiceMock: {
    getReport: ReturnType<typeof vi.fn>;
    saveReport: ReturnType<typeof vi.fn>;
    deleteReport: ReturnType<typeof vi.fn>;
    downloadReport: ReturnType<typeof vi.fn>;
  };
  let permissionServiceMock: { loadForProject: ReturnType<typeof vi.fn>; canEdit: ReturnType<typeof vi.fn>; canDelete: ReturnType<typeof vi.fn> };
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  let fixture: any;
  let comp: ReportEditorComponent;
  let router: Router;

  beforeEach(() => {
    paramMap$ = new BehaviorSubject(convertToParamMap({ name: 'proj1', reportId: 'new' }));

    reportServiceMock = {
      getReport: vi.fn().mockResolvedValue(MOCK_REPORT),
      saveReport: vi.fn().mockResolvedValue({ success: true, entity: MOCK_REPORT }),
      deleteReport: vi.fn().mockResolvedValue({ success: true }),
      downloadReport: vi.fn().mockResolvedValue(undefined)
    };
    permissionServiceMock = {
      loadForProject: vi.fn().mockResolvedValue(undefined),
      canEdit: vi.fn().mockReturnValue(true),
      canDelete: vi.fn().mockReturnValue(true)
    };

    TestBed.configureTestingModule({
      imports: [ReportEditorComponent],
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { paramMap: paramMap$.asObservable() } },
        { provide: ReportService, useValue: reportServiceMock },
        { provide: PermissionService, useValue: permissionServiceMock },
        { provide: MessageService, useValue: { add: vi.fn() } }
      ]
    });
    fixture = TestBed.createComponent(ReportEditorComponent);
    comp = fixture.componentInstance;
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
  });

  it('isNew() is true when reportId param is "new"', async () => {
    fixture.detectChanges();
    await flush();
    expect(comp.isNew()).toBe(true);
    expect(comp.reportId()).toBeNull();
  });

  it('loadReport populates reportName() and reportId()', async () => {
    paramMap$.next(convertToParamMap({ name: 'proj1', reportId: '7' }));
    fixture.detectChanges();
    await flush();
    expect(reportServiceMock.getReport).toHaveBeenCalledWith('proj1', 7);
    expect(comp.reportName()).toBe('Requirements Doc');
    expect(comp.reportId()).toBe(7);
    expect(comp.name).toBe('Requirements Doc');
  });

  it('onSave calls reportService.saveReport with name and text', async () => {
    fixture.detectChanges();
    await flush();
    comp.name = 'My Template';
    comp.text = '<xsl:stylesheet/>';
    await comp.onSave();
    expect(reportServiceMock.saveReport).toHaveBeenCalledWith(
      'proj1', null, 'My Template', '<xsl:stylesheet/>'
    );
  });

  it('onSave sets errorMessage when name is empty', async () => {
    fixture.detectChanges();
    await flush();
    comp.name = '';
    await comp.onSave();
    expect(comp.errorMessage()).toBe('Document name is required.');
    expect(reportServiceMock.saveReport).not.toHaveBeenCalled();
  });

  it('onRun calls reportService.downloadReport', async () => {
    paramMap$.next(convertToParamMap({ name: 'proj1', reportId: '7' }));
    fixture.detectChanges();
    await flush();
    await comp.onRun();
    expect(reportServiceMock.downloadReport).toHaveBeenCalledWith('proj1', 7, 'Requirements Doc');
    expect(comp.running()).toBe(false);
  });
});
