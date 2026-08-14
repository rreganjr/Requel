import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter, Router, ActivatedRoute, convertToParamMap } from '@angular/router';
import { BehaviorSubject } from 'rxjs';
import { MessageService } from 'primeng/api';
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

  function fill(name: string, text = ''): void {
    comp.form.setValue({ name, text });
    comp.form.markAsDirty();
  }

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
    expect(comp.form.controls.name.value).toBe('Requirements Doc');
  });

  it('onSave calls reportService.saveReport with name and text', async () => {
    fixture.detectChanges();
    await flush();
    fill('My Template', '<xsl:stylesheet/>');
    await comp.onSave();
    expect(reportServiceMock.saveReport).toHaveBeenCalledWith(
      'proj1', null, 'My Template', '<xsl:stylesheet/>'
    );
  });

  it('onRun calls reportService.downloadReport', async () => {
    paramMap$.next(convertToParamMap({ name: 'proj1', reportId: '7' }));
    fixture.detectChanges();
    await flush();
    await comp.onRun();
    expect(reportServiceMock.downloadReport).toHaveBeenCalledWith('proj1', 7, 'Requirements Doc');
    expect(comp.running()).toBe(false);
  });

  // #185: the edit route renders before the detail GET resolves, so the reset had a window to
  // overwrite whatever the user typed into it - and clearing `dirty` left Save disabled with
  // nothing on screen explaining why.
  it('does not clobber a value typed while the initial load is still in flight', async () => {
    let resolveGet: (report: unknown) => void = () => {};
    reportServiceMock.getReport.mockImplementation(
      () => new Promise(resolve => { resolveGet = resolve; })
    );

    paramMap$.next(convertToParamMap({ name: 'proj1', reportId: '7' }));
    fixture.detectChanges();
    await flush();

    // The user is faster than the network.
    comp.form.controls.name.setValue('Typed while loading');
    comp.form.controls.name.markAsDirty();

    resolveGet(MOCK_REPORT);
    await flush();

    expect(comp.form.controls.name.value).toBe('Typed while loading');
    expect(comp.form.dirty).toBe(true);
    // Server state still landed, so the editor knows which document it is holding.
    expect(comp.report()?.id).toBe(7);
    expect(comp.reportId()).toBe(7);
  });

  // Guards the ordering inside onSave: the post-save refetch has to run against a form already
  // marked pristine, or the #185 guard makes loadReport skip its own result. term-editor had a
  // test for this; report-editor did not.
  it('marks the form pristine after a successful save of an existing document', async () => {
    paramMap$.next(convertToParamMap({ name: 'proj1', reportId: '7' }));
    fixture.detectChanges();
    await flush();
    comp.form.controls.text.setValue('<xsl:edited/>');
    comp.form.controls.text.markAsDirty();

    await comp.onSave();
    await flush();

    expect(comp.form.pristine).toBe(true);
  });

  // #185. The gate is the structural half of the fix: with the form absent until the detail GET
  // resolves, there is no input for a user - or a fast e2e test - to type into before the load
  // lands. The dirty-guard in loadReport() is then belt-and-braces for the SSE / post-save paths.
  describe('render gate (#185, finishing #131)', () => {
    function el(): HTMLElement {
      return fixture.nativeElement as HTMLElement;
    }

    it('shows the skeleton and no form until the detail GET resolves', async () => {
      let resolveGet: (report: unknown) => void = () => {};
      reportServiceMock.getReport.mockImplementation(
        () => new Promise(resolve => { resolveGet = resolve; })
      );

      paramMap$.next(convertToParamMap({ name: 'proj1', reportId: '7' }));
      fixture.detectChanges();
      await flush();
      fixture.detectChanges();

      expect(el().querySelector('[data-testid="report-editor-loading"]')).not.toBeNull();
      // The point of the gate: nothing to type into yet.
      expect(el().querySelector('input#name')).toBeNull();

      resolveGet(MOCK_REPORT);
      await flush();
      fixture.detectChanges();

      expect(el().querySelector('[data-testid="report-editor-loading"]')).toBeNull();
      expect(el().querySelector('input#name')).not.toBeNull();
    });

    // The create route never loads, so the gate has to be resolved synchronously in ngOnInit -
    // otherwise a new document sits behind the skeleton forever.
    it('renders the create form immediately, with no skeleton', async () => {
      fixture.detectChanges();
      await flush();
      fixture.detectChanges();

      expect(comp.loading()).toBe(false);
      expect(el().querySelector('[data-testid="report-editor-loading"]')).toBeNull();
      expect(el().querySelector('input#name')).not.toBeNull();
    });

    it('shows a retryable error state when the load fails, and recovers on retry', async () => {
      reportServiceMock.getReport.mockRejectedValueOnce(new Error('boom'));

      paramMap$.next(convertToParamMap({ name: 'proj1', reportId: '7' }));
      fixture.detectChanges();
      await flush();
      fixture.detectChanges();

      expect(el().querySelector('[data-testid="report-editor-load-error"]')).not.toBeNull();
      expect(el().querySelector('input#name')).toBeNull();

      reportServiceMock.getReport.mockResolvedValue(MOCK_REPORT);
      comp.retryLoad();
      await flush();
      fixture.detectChanges();

      expect(el().querySelector('[data-testid="report-editor-load-error"]')).toBeNull();
      expect(comp.form.controls.name.value).toBe('Requirements Doc');
    });

    // The post-save refetch passes skeleton=false: blanking the form the user just saved would be
    // worse than a stale moment, and a failure there belongs inline rather than in place of it.
    it('does not blank the form for the post-save refetch', async () => {
      paramMap$.next(convertToParamMap({ name: 'proj1', reportId: '7' }));
      fixture.detectChanges();
      await flush();

      let resolveGet: (report: unknown) => void = () => {};
      reportServiceMock.getReport.mockImplementation(
        () => new Promise(resolve => { resolveGet = resolve; })
      );
      fill('Edited', '<xsl:edited/>');
      const saving = comp.onSave();
      await flush();
      fixture.detectChanges();

      expect(comp.loading()).toBe(false);
      expect(el().querySelector('input#name')).not.toBeNull();

      resolveGet(MOCK_REPORT);
      await saving;
    });
  });

  describe('reactive form (issue #132)', () => {
    it('loads the document into the form and leaves it pristine', async () => {
      paramMap$.next(convertToParamMap({ name: 'proj1', reportId: '7' }));
      fixture.detectChanges();
      await flush();

      expect(comp.form.getRawValue()).toEqual({
        name: 'Requirements Doc',
        text: '<xsl:stylesheet>...</xsl:stylesheet>',
      });
      expect(comp.form.pristine).toBe(true);
    });

    it('does not save an empty name, and reports it inline rather than page-level', async () => {
      fixture.detectChanges();
      await flush();
      fill('');

      await comp.onSave();

      expect(reportServiceMock.saveReport).not.toHaveBeenCalled();
      expect(comp.errorMessage()).toBeNull();
      expect(comp.form.controls.name.hasError('required')).toBe(true);
      expect(comp.submitted()).toBe(true);
    });

    it('sends null rather than an empty string for an empty template', async () => {
      fixture.detectChanges();
      await flush();
      fill('Doc', '');

      await comp.onSave();

      expect(reportServiceMock.saveReport).toHaveBeenCalledWith('proj1', null, 'Doc', null);
    });

    it('hasUnsavedChanges() derives from form.dirty', async () => {
      fixture.detectChanges();
      await flush();
      expect(comp.hasUnsavedChanges()).toBe(false);

      comp.form.controls.text.setValue('changed');
      comp.form.controls.text.markAsDirty();
      expect(comp.hasUnsavedChanges()).toBe(true);
    });

    it('disables Save while pristine', async () => {
      paramMap$.next(convertToParamMap({ name: 'proj1', reportId: '7' }));
      fixture.detectChanges();
      await flush();
      fixture.detectChanges();

      const button = (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>(
        '[data-testid="report-save"] button'
      );
      expect(button?.disabled).toBe(true);
    });

    it('keeps the #name and #text ids the e2e page objects locate', async () => {
      fixture.detectChanges();
      await flush();
      fixture.detectChanges();
      const el = fixture.nativeElement as HTMLElement;

      expect(el.querySelector('input#name')).not.toBeNull();
      expect(el.querySelector('textarea#text')).not.toBeNull();
    });
  });

  describe('XSLT upload (issue #132)', () => {
    /** Minimal FileReader stand-in — jsdom's needs a real Blob and fires async. */
    function stubFileReader(contents: string): void {
      class StubReader {
        result: string | null = null;
        onload: (() => void) | null = null;
        readAsText(): void {
          this.result = contents;
          this.onload?.();
        }
      }
      vi.stubGlobal('FileReader', StubReader);
    }

    afterEach(() => {
      vi.unstubAllGlobals();
    });

    it('puts the uploaded text in the form and marks it dirty so Save enables', async () => {
      fixture.detectChanges();
      await flush();
      stubFileReader('<xsl:stylesheet>uploaded</xsl:stylesheet>');

      comp.onFileUpload(new File([''], 'my-template.xsl'));

      expect(comp.form.controls.text.value).toBe('<xsl:stylesheet>uploaded</xsl:stylesheet>');
      expect(comp.form.dirty).toBe(true);
    });

    it('derives the name from the filename only when the name is empty', async () => {
      fixture.detectChanges();
      await flush();
      stubFileReader('<xsl/>');

      comp.onFileUpload(new File([''], 'my-template.xsl'));
      expect(comp.form.controls.name.value).toBe('my-template');
    });

    it('leaves an existing name alone', async () => {
      fixture.detectChanges();
      await flush();
      fill('Chosen name');
      stubFileReader('<xsl/>');

      comp.onFileUpload(new File([''], 'my-template.xsl'));
      expect(comp.form.controls.name.value).toBe('Chosen name');
    });
  });

  describe('command error handling (issue #132)', () => {
    it('puts a field violation on its control instead of the page message', async () => {
      reportServiceMock.saveReport.mockResolvedValue({
        success: false,
        violations: [{ field: 'name', message: 'A document with that name already exists.' }],
        error: 'Validation failed',
      });
      fixture.detectChanges();
      await flush();
      fill('Duplicate');

      await comp.onSave();

      expect(comp.form.controls.name.errors).toEqual({
        server: 'A document with that name already exists.',
      });
      expect(comp.errorMessage()).toBeNull();
    });

    it('puts a text violation on the template control', async () => {
      reportServiceMock.saveReport.mockResolvedValue({
        success: false,
        violations: [{ field: 'text', message: 'Not well-formed XSLT.' }],
        error: 'Validation failed',
      });
      fixture.detectChanges();
      await flush();
      fill('Doc', '<not-xslt>');

      await comp.onSave();

      expect(comp.form.controls.text.errors).toEqual({ server: 'Not well-formed XSLT.' });
    });

    it('shows a command-level failure page-level', async () => {
      reportServiceMock.saveReport.mockResolvedValue({
        success: false,
        violations: [{ field: null, message: 'Validation failed.' }],
        error: 'Validation failed',
      });
      fixture.detectChanges();
      await flush();
      fill('Doc');

      await comp.onSave();

      expect(comp.errorMessage()).toBe('Validation failed.');
    });
  });
});
