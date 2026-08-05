import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter, ActivatedRoute, convertToParamMap, Router } from '@angular/router';
import { BehaviorSubject } from 'rxjs';
import { MessageService } from 'primeng/api';
import { ReportEditorComponent } from './report-editor';
import { ReportService } from '../../core/report.service';
import { PermissionService } from '../../core/permission.service';
import { expectNoAxeViolations } from '../../shared/testing/a11y';

const MOCK_REPORT = {
  id: 7, version: 0, name: 'Requirements Doc', text: '<xsl:stylesheet>...</xsl:stylesheet>'
};

const flush = () => new Promise(r => setTimeout(r, 0));

/** Same exclusion as term-editor.a11y.spec.ts, for the same reason. */
const EXCLUDE = ['p-confirmdialog'];

describe('ReportEditorComponent accessibility (issue #132)', () => {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  let fixture: any;
  let comp: ReportEditorComponent;

  async function render(reportId = 'new'): Promise<HTMLElement> {
    const paramMap$ = new BehaviorSubject(convertToParamMap({ name: 'proj1', reportId }));
    TestBed.configureTestingModule({
      imports: [ReportEditorComponent],
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { paramMap: paramMap$.asObservable() } },
        {
          provide: ReportService,
          useValue: {
            getReport: vi.fn().mockResolvedValue(MOCK_REPORT),
            saveReport: vi.fn().mockResolvedValue({ success: true, entity: MOCK_REPORT }),
            deleteReport: vi.fn().mockResolvedValue({ success: true }),
            downloadReport: vi.fn().mockResolvedValue(undefined),
          },
        },
        {
          provide: PermissionService,
          useValue: {
            loadForProject: vi.fn().mockResolvedValue(undefined),
            canEdit: vi.fn().mockReturnValue(true),
            canDelete: vi.fn().mockReturnValue(true),
          },
        },
        { provide: MessageService, useValue: { add: vi.fn() } },
      ],
    });
    fixture = TestBed.createComponent(ReportEditorComponent);
    comp = fixture.componentInstance;
    vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    fixture.detectChanges();
    await flush();
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  it('has no axe-core violations on the create form', async () => {
    await expectNoAxeViolations(await render('new'), EXCLUDE);
  });

  it('has no axe-core violations on the edit form', async () => {
    await expectNoAxeViolations(await render('7'), EXCLUDE);
  });

  it('has no axe-core violations with the name field in its error state', async () => {
    const el = await render('new');
    await comp.onSave();
    fixture.detectChanges();

    expect(el.querySelector('[data-testid="field-error"]')).not.toBeNull();
    await expectNoAxeViolations(el, EXCLUDE);
  });

  /**
   * The XSLT row projects a textarea *and* the upload button into one control column.
   * Worth an axe pass of its own: the label must still resolve to the textarea rather
   * than being ambiguous between the two controls in the cell.
   */
  it('labels the textarea, not the upload button beside it', async () => {
    const el = await render('7');
    const label = el.querySelector<HTMLLabelElement>('label[for="text"]');

    expect(label?.textContent?.trim()).toContain('XSLT Template');
    expect(el.querySelector('#text')?.tagName.toLowerCase()).toBe('textarea');
    await expectNoAxeViolations(el, EXCLUDE);
  });
});
