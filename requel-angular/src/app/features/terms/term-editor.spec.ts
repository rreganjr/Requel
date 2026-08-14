import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter, Router, ActivatedRoute, convertToParamMap } from '@angular/router';
import { BehaviorSubject, EMPTY, Subject } from 'rxjs';
import { MessageService } from 'primeng/api';
import { TermEditorComponent } from './term-editor';
import { TermService } from '../../core/term.service';
import { PermissionService } from '../../core/permission.service';
import { EventStreamService } from '../../core/event-stream.service';

const MOCK_TERMS = [
  { id: 1, name: 'Actor', text: 'A participant.' },
  { id: 2, name: 'Goal', text: 'An objective.' },
  { id: 3, name: 'Story', text: 'A narrative.' }
];

const MOCK_TERM = {
  id: 2, version: 0, name: 'Goal', text: 'A desired outcome.',
  canonicalTermId: null, alternateTerms: [], referers: []
};

const flush = () => new Promise(r => setTimeout(r, 0));

describe('TermEditorComponent', () => {
  let paramMap$: BehaviorSubject<ReturnType<typeof convertToParamMap>>;
  let events$: Subject<{ targetType: string; targetId: number }>;
  let termServiceMock: {
    listTerms: ReturnType<typeof vi.fn>;
    getTerm: ReturnType<typeof vi.fn>;
    saveTerm: ReturnType<typeof vi.fn>;
    deleteTerm: ReturnType<typeof vi.fn>;
  };
  let permissionServiceMock: { loadForProject: ReturnType<typeof vi.fn>; canEdit: ReturnType<typeof vi.fn>; canDelete: ReturnType<typeof vi.fn> };
  let eventStreamServiceMock: { events$: unknown; addSubscription: ReturnType<typeof vi.fn>; removeSubscription: ReturnType<typeof vi.fn> };
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  let fixture: any;
  let comp: TermEditorComponent;
  let router: Router;

  beforeEach(() => {
    paramMap$ = new BehaviorSubject(convertToParamMap({ name: 'proj1', termId: 'new' }));
    events$ = new Subject();

    termServiceMock = {
      listTerms: vi.fn().mockResolvedValue(MOCK_TERMS),
      getTerm: vi.fn().mockResolvedValue(MOCK_TERM),
      saveTerm: vi.fn().mockResolvedValue({ success: true, entity: MOCK_TERM }),
      deleteTerm: vi.fn().mockResolvedValue({ success: true })
    };
    permissionServiceMock = {
      loadForProject: vi.fn().mockResolvedValue(undefined),
      canEdit: vi.fn().mockReturnValue(true),
      canDelete: vi.fn().mockReturnValue(true)
    };
    eventStreamServiceMock = {
      events$: EMPTY,
      addSubscription: vi.fn().mockResolvedValue(undefined),
      removeSubscription: vi.fn().mockResolvedValue(undefined)
    };

    TestBed.configureTestingModule({
      imports: [TermEditorComponent],
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { paramMap: paramMap$.asObservable() } },
        { provide: TermService, useValue: termServiceMock },
        { provide: PermissionService, useValue: permissionServiceMock },
        { provide: EventStreamService, useValue: eventStreamServiceMock },
        { provide: MessageService, useValue: { add: vi.fn() } }
      ]
    });
    fixture = TestBed.createComponent(TermEditorComponent);
    comp = fixture.componentInstance;
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
  });

  /** Fills the form the way a user would, leaving it dirty. */
  function fill(name: string, text = '', canonicalTermId: number | null = null): void {
    comp.form.setValue({ name, text, canonicalTermId });
    comp.form.markAsDirty();
  }

  it('isNew() is true when termId param is "new"', async () => {
    fixture.detectChanges();
    await flush();
    expect(comp.isNew()).toBe(true);
    expect(comp.termId()).toBeNull();
  });

  it('canonicalOptions loaded from listTerms excluding current term', async () => {
    paramMap$.next(convertToParamMap({ name: 'proj1', termId: '2' }));
    fixture.detectChanges();
    await flush();
    const options = comp.canonicalOptions();
    expect(options.some(o => o.value === 2)).toBe(false);
    expect(options.length).toBe(MOCK_TERMS.length - 1);
  });

  it('loadTerm populates termName() and termId()', async () => {
    paramMap$.next(convertToParamMap({ name: 'proj1', termId: '2' }));
    fixture.detectChanges();
    await flush();
    expect(termServiceMock.getTerm).toHaveBeenCalledWith('proj1', 2);
    expect(comp.termName()).toBe('Goal');
    expect(comp.termId()).toBe(2);
  });

  it('onSave calls termService.saveTerm with name and text', async () => {
    fixture.detectChanges();
    await flush();
    fill('New Term', 'A definition');
    await comp.onSave();
    expect(termServiceMock.saveTerm).toHaveBeenCalledWith(
      'proj1', null, 'New Term', 'A definition', null
    );
  });

  // #185. The gate is the structural half of the fix: with the form absent until the detail GET
  // resolves, there is no input for a user - or a fast e2e test - to type into before the load
  // lands. The dirty-guard in loadTerm() is then belt-and-braces for the SSE / post-save paths.
  describe('render gate (#185, finishing #131)', () => {
    function el(): HTMLElement {
      return fixture.nativeElement as HTMLElement;
    }

    it('shows the skeleton and no form until the detail GET resolves', async () => {
      let resolveGet: (term: unknown) => void = () => {};
      termServiceMock.getTerm.mockImplementation(
        () => new Promise(resolve => { resolveGet = resolve; })
      );

      paramMap$.next(convertToParamMap({ name: 'proj1', termId: '2' }));
      fixture.detectChanges();
      await flush();
      fixture.detectChanges();

      expect(el().querySelector('[data-testid="term-editor-loading"]')).not.toBeNull();
      // The point of the gate: nothing to type into yet.
      expect(el().querySelector('input#name')).toBeNull();

      resolveGet(MOCK_TERM);
      await flush();
      fixture.detectChanges();

      expect(el().querySelector('[data-testid="term-editor-loading"]')).toBeNull();
      expect(el().querySelector('input#name')).not.toBeNull();
    });

    // The create route never loads, so the gate has to be resolved synchronously in ngOnInit -
    // otherwise a new term sits behind the skeleton forever.
    it('renders the create form immediately, with no skeleton', async () => {
      fixture.detectChanges();
      await flush();
      fixture.detectChanges();

      expect(comp.loading()).toBe(false);
      expect(el().querySelector('[data-testid="term-editor-loading"]')).toBeNull();
      expect(el().querySelector('input#name')).not.toBeNull();
    });

    it('shows a retryable error state when the load fails, and recovers on retry', async () => {
      termServiceMock.getTerm.mockRejectedValueOnce(new Error('boom'));

      paramMap$.next(convertToParamMap({ name: 'proj1', termId: '2' }));
      fixture.detectChanges();
      await flush();
      fixture.detectChanges();

      expect(el().querySelector('[data-testid="term-editor-load-error"]')).not.toBeNull();
      expect(el().querySelector('input#name')).toBeNull();

      termServiceMock.getTerm.mockResolvedValue(MOCK_TERM);
      comp.retryLoad();
      await flush();
      fixture.detectChanges();

      expect(el().querySelector('[data-testid="term-editor-load-error"]')).toBeNull();
      expect(comp.form.controls.name.value).toBe('Goal');
    });

    // An SSE refresh passes skeleton=false. Blanking the form under a user who is reading it
    // because someone else touched the term would be its own bug.
    it('does not blank the form for an SSE refresh', async () => {
      eventStreamServiceMock.events$ = events$.asObservable();

      paramMap$.next(convertToParamMap({ name: 'proj1', termId: '2' }));
      fixture.detectChanges();
      await flush();

      let resolveGet: (term: unknown) => void = () => {};
      termServiceMock.getTerm.mockImplementation(
        () => new Promise(resolve => { resolveGet = resolve; })
      );

      events$.next({ targetType: 'GlossaryTerm', targetId: 2 });
      await flush();
      fixture.detectChanges();

      expect(comp.loading()).toBe(false);
      expect(el().querySelector('input#name')).not.toBeNull();

      resolveGet(MOCK_TERM);
      await flush();
    });
  });

  describe('reactive form (issue #132)', () => {
    it('loads the term into the form and leaves it pristine', async () => {
      paramMap$.next(convertToParamMap({ name: 'proj1', termId: '2' }));
      fixture.detectChanges();
      await flush();

      expect(comp.form.getRawValue()).toEqual({
        name: 'Goal',
        text: 'A desired outcome.',
        canonicalTermId: null,
      });
      expect(comp.form.pristine).toBe(true);
    });

    it('resets the form for the new-term path', async () => {
      paramMap$.next(convertToParamMap({ name: 'proj1', termId: '2' }));
      fixture.detectChanges();
      await flush();

      paramMap$.next(convertToParamMap({ name: 'proj1', termId: 'new' }));
      await flush();

      expect(comp.form.getRawValue()).toEqual({ name: '', text: '', canonicalTermId: null });
      expect(comp.form.pristine).toBe(true);
    });

    /**
     * Replaces the imperative `if (!this.name.trim())` guard and its page-level "Term
     * name is required." message. The complaint now renders under the field, so the
     * page-level slot is left for things that have no field.
     */
    it('does not save an empty name, and reports it inline rather than page-level', async () => {
      fixture.detectChanges();
      await flush();
      fill('');

      await comp.onSave();

      expect(termServiceMock.saveTerm).not.toHaveBeenCalled();
      expect(comp.errorMessage()).toBeNull();
      expect(comp.form.controls.name.hasError('required')).toBe(true);
      expect(comp.form.controls.name.touched).toBe(true);
      expect(comp.submitted()).toBe(true);
    });

    it('treats a whitespace-only name as present but trims it before sending', async () => {
      fixture.detectChanges();
      await flush();
      fill('  Spaced  ', 'text');

      await comp.onSave();

      expect(termServiceMock.saveTerm).toHaveBeenCalledWith('proj1', null, 'Spaced', 'text', null);
    });

    it('sends null rather than an empty string for a blank definition', async () => {
      fixture.detectChanges();
      await flush();
      fill('Term', '');

      await comp.onSave();

      expect(termServiceMock.saveTerm).toHaveBeenCalledWith('proj1', null, 'Term', null, null);
    });

    it('hasUnsavedChanges() derives from form.dirty', async () => {
      fixture.detectChanges();
      await flush();
      expect(comp.hasUnsavedChanges()).toBe(false);

      comp.form.controls.name.setValue('Changed');
      comp.form.controls.name.markAsDirty();
      expect(comp.hasUnsavedChanges()).toBe(true);
    });

    it('marks the form pristine after a successful save of an existing term', async () => {
      paramMap$.next(convertToParamMap({ name: 'proj1', termId: '2' }));
      fixture.detectChanges();
      await flush();
      comp.form.controls.text.setValue('Edited');
      comp.form.controls.text.markAsDirty();

      await comp.onSave();
      await flush();

      expect(comp.form.pristine).toBe(true);
    });

    it('disables Save while pristine and enables it once dirty and valid', async () => {
      paramMap$.next(convertToParamMap({ name: 'proj1', termId: '2' }));
      fixture.detectChanges();
      await flush();
      fixture.detectChanges();

      const save = () =>
        (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>(
          '[data-testid="term-save"] button'
        );
      expect(save()?.disabled).toBe(true);

      comp.form.controls.name.setValue('Renamed');
      comp.form.controls.name.markAsDirty();
      fixture.detectChanges();
      expect(save()?.disabled).toBe(false);
    });

    it('keeps the #name and #text ids the e2e page objects locate', async () => {
      fixture.detectChanges();
      await flush();
      fixture.detectChanges();
      const el = fixture.nativeElement as HTMLElement;

      expect(el.querySelector('input#name')).not.toBeNull();
      expect(el.querySelector('textarea#text')).not.toBeNull();
      expect(el.querySelector('[data-testid="term-canonical-select"]')).not.toBeNull();
    });
  });

  describe('command error handling (issue #132)', () => {
    it('puts a field violation on its control instead of the page message', async () => {
      termServiceMock.saveTerm.mockResolvedValue({
        success: false,
        violations: [{ field: 'name', message: 'A term with that name already exists.' }],
        error: 'Validation failed',
      });
      fixture.detectChanges();
      await flush();
      fill('Duplicate');

      await comp.onSave();

      expect(comp.form.controls.name.errors).toEqual({
        server: 'A term with that name already exists.',
      });
      expect(comp.errorMessage()).toBeNull();
    });

    it('maps the canonicalTerm entity property onto the canonicalTermId control', async () => {
      termServiceMock.saveTerm.mockResolvedValue({
        success: false,
        violations: [{ field: 'canonicalTerm', message: 'Cannot be its own canonical term.' }],
        error: 'Validation failed',
      });
      fixture.detectChanges();
      await flush();
      fill('Term', '', 2);

      await comp.onSave();

      expect(comp.form.controls.canonicalTermId.errors).toEqual({
        server: 'Cannot be its own canonical term.',
      });
    });

    it('shows an unmappable violation page-level rather than dropping it', async () => {
      termServiceMock.saveTerm.mockResolvedValue({
        success: false,
        violations: [{ field: 'somethingElse', message: 'Deeply unexpected.' }],
        error: 'Validation failed',
      });
      fixture.detectChanges();
      await flush();
      fill('Term');

      await comp.onSave();

      expect(comp.errorMessage()).toBe('Deeply unexpected.');
    });

    it('falls back to the page-level error when there are no violations at all', async () => {
      termServiceMock.saveTerm.mockResolvedValue({
        success: false,
        violations: null,
        error: 'Save failed.',
      });
      fixture.detectChanges();
      await flush();
      fill('Term');

      await comp.onSave();

      expect(comp.errorMessage()).toBe('Save failed.');
    });

    /**
     * A server error makes its control invalid, which is what disables Save until the
     * user changes something. The trap: if onSave cleared those errors *after* its
     * validity guard, the guard would see the stale error, bail, and the form would be
     * stuck at that value forever with no retry and no feedback.
     */
    it('clears a previous attempt\'s server error before re-submitting', async () => {
      termServiceMock.saveTerm.mockResolvedValue({
        success: false,
        violations: [{ field: 'name', message: 'Taken.' }],
        error: 'Validation failed',
      });
      fixture.detectChanges();
      await flush();
      fill('Duplicate');
      await comp.onSave();
      expect(comp.form.controls.name.errors?.['server']).toBe('Taken.');

      // Same value, so no valueChanges to clear it implicitly — onSave must do it.
      termServiceMock.saveTerm.mockResolvedValue({ success: true, entity: MOCK_TERM });
      await comp.onSave();

      expect(comp.form.controls.name.errors).toBeNull();
      expect(termServiceMock.saveTerm).toHaveBeenCalledTimes(2);
    });

    it('a standing server error disables Save until something changes', async () => {
      termServiceMock.saveTerm.mockResolvedValue({
        success: false,
        violations: [{ field: 'name', message: 'Taken.' }],
        error: 'Validation failed',
      });
      fixture.detectChanges();
      await flush();
      fill('Duplicate');
      await comp.onSave();
      fixture.detectChanges();

      const save = () =>
        (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>(
          '[data-testid="term-save"] button'
        );
      expect(save()?.disabled).toBe(true);

      comp.form.controls.name.setValue('Something else');
      fixture.detectChanges();
      expect(save()?.disabled).toBe(false);
    });
  });

  describe('SSE reload (issue #132)', () => {
    beforeEach(() => {
      eventStreamServiceMock.events$ = events$.asObservable();
    });

    // #185 changed the shape of this guarantee. The guard used to be an early `return` on this
    // subscription, so a remote change while dirty issued no fetch at all and `term` / `termId`
    // went stale behind the edit - the same trap #184 found in `actor-editor`. The reload now
    // always runs and always adopts server state; only the form is protected.
    it('refetches on a remote change but does not clobber in-progress edits', async () => {
      paramMap$.next(convertToParamMap({ name: 'proj1', termId: '2' }));
      fixture.detectChanges();
      await flush();

      comp.form.controls.text.setValue('My unsaved edit');
      comp.form.controls.text.markAsDirty();
      const callsBefore = termServiceMock.getTerm.mock.calls.length;
      termServiceMock.getTerm.mockResolvedValue({
        ...MOCK_TERM, name: 'Renamed remotely', text: 'Remote definition.'
      });

      events$.next({ targetType: 'GlossaryTerm', targetId: 2 });
      await flush();

      // Server state landed...
      expect(termServiceMock.getTerm.mock.calls.length).toBeGreaterThan(callsBefore);
      expect(comp.term()?.name).toBe('Renamed remotely');
      // ...but the form and its dirty flag are untouched.
      expect(comp.form.controls.text.value).toBe('My unsaved edit');
      expect(comp.form.dirty).toBe(true);
    });

    it('does not clobber a value typed while the initial load is still in flight', async () => {
      let resolveGet: (term: unknown) => void = () => {};
      termServiceMock.getTerm.mockImplementation(
        () => new Promise(resolve => { resolveGet = resolve; })
      );

      paramMap$.next(convertToParamMap({ name: 'proj1', termId: '2' }));
      fixture.detectChanges();
      await flush();

      // The user is faster than the network.
      comp.form.controls.name.setValue('Typed while loading');
      comp.form.controls.name.markAsDirty();

      resolveGet({ ...MOCK_TERM, name: 'Goal' });
      await flush();

      expect(comp.form.controls.name.value).toBe('Typed while loading');
      expect(comp.form.dirty).toBe(true);
      // Server state still landed, so the editor knows which term it is holding.
      expect(comp.term()?.id).toBe(2);
      expect(comp.termId()).toBe(2);
    });

    it('reloads when the form is clean', async () => {
      paramMap$.next(convertToParamMap({ name: 'proj1', termId: '2' }));
      fixture.detectChanges();
      await flush();
      const callsBefore = termServiceMock.getTerm.mock.calls.length;

      events$.next({ targetType: 'GlossaryTerm', targetId: 2 });
      await flush();

      expect(termServiceMock.getTerm.mock.calls.length).toBeGreaterThan(callsBefore);
    });
  });
});
