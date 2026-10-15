import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter, ActivatedRoute, convertToParamMap, Router } from '@angular/router';
import { BehaviorSubject, EMPTY } from 'rxjs';
import { MessageService } from 'primeng/api';
import { TermEditorComponent } from './term-editor';
import { TermService } from '../../core/term.service';
import { PermissionService } from '../../core/permission.service';
import { EventStreamService } from '../../core/event-stream.service';
import { expectNoAxeViolations } from '../../shared/testing/a11y';

const MOCK_TERM = {
  id: 2, version: 0, name: 'Goal', text: 'A desired outcome.',
  canonicalTermId: null, alternateTerms: [], referers: []
};

const flush = () => new Promise(r => setTimeout(r, 0));

/**
 * One exclusion, for a defect the page under test does not own:
 *
 * - `p-confirmdialog`: PrimeNG marks its host `role="alertdialog"` even when nothing is
 *   showing, so axe reports an unnamed dialog on any page that mounts one. Belongs with
 *   #139 — see the note on `expectNoAxeViolations`.
 *
 * The `app-annotations-section` heading-order violation these specs first surfaced is
 * fixed at source instead: its `<h3>Annotations</h3>` is now an
 * `<h2 class="rq-section-title">`, matching the section headings #158 standardised.
 */
const EXCLUDE = ['p-confirmdialog'];

describe('TermEditorComponent accessibility (issue #132)', () => {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  let fixture: any;
  let comp: TermEditorComponent;

  async function render(termId = 'new'): Promise<HTMLElement> {
    const paramMap$ = new BehaviorSubject(convertToParamMap({ name: 'proj1', termId }));
    TestBed.configureTestingModule({
      imports: [TermEditorComponent],
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { paramMap: paramMap$.asObservable() } },
        {
          provide: TermService,
          useValue: {
            listTerms: vi.fn().mockResolvedValue([{ id: 1, name: 'Actor', text: '' }]),
            getTerm: vi.fn().mockResolvedValue(MOCK_TERM),
            saveTerm: vi.fn().mockResolvedValue({ success: true, entity: MOCK_TERM }),
            deleteTerm: vi.fn().mockResolvedValue({ success: true }),
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
        {
          provide: EventStreamService,
          useValue: {
            events$: EMPTY,
            addSubscription: vi.fn().mockResolvedValue(undefined),
            removeSubscription: vi.fn().mockResolvedValue(undefined),
          },
        },
        { provide: MessageService, useValue: { add: vi.fn() } },
      ],
    });
    fixture = TestBed.createComponent(TermEditorComponent);
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
    await expectNoAxeViolations(await render('2'), EXCLUDE);
  });

  it('has no axe-core violations with the name field in its error state', async () => {
    const el = await render('new');
    await comp.onSave();
    fixture.detectChanges();

    expect(el.querySelector('[data-testid="field-error"]')).not.toBeNull();
    await expectNoAxeViolations(el, EXCLUDE);
  });

  it('has no axe-core violations with a server error showing on a field', async () => {
    const el = await render('2');
    comp.form.controls.name.setErrors({ server: 'A term with that name already exists.' });
    comp.form.controls.name.markAsTouched();
    fixture.detectChanges();

    await expectNoAxeViolations(el, EXCLUDE);
  });

  it('associates each error with its control', async () => {
    const el = await render('new');
    await comp.onSave();
    fixture.detectChanges();

    const name = el.querySelector<HTMLInputElement>('#name')!;
    expect(name.getAttribute('aria-invalid')).toBe('true');
    expect(name.getAttribute('aria-required')).toBe('true');

    const describedBy = name.getAttribute('aria-describedby');
    expect(el.querySelector(`#${describedBy}`)?.textContent?.trim()).toBe(
      'This field is required.'
    );
  });

  it('gives the p-select a real label rather than pointing at the host element', async () => {
    const el = await render('2');
    const label = el.querySelector<HTMLLabelElement>('label[for="canonical-term-input"]');

    expect(label?.textContent?.trim()).toContain('Canonical Term');
    // The old markup put id="canonical" on the p-select host, which is not focusable and
    // gets no accessible name from a <label for>. app-field targets the inner control.
    expect(el.querySelector('#canonical-term-input')?.tagName.toLowerCase()).not.toBe('p-select');
  });
});
