import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter, Router, ActivatedRoute, convertToParamMap } from '@angular/router';
import { BehaviorSubject, EMPTY } from 'rxjs';
import { ConfirmationService, MessageService } from 'primeng/api';
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
  let termServiceMock: {
    listTerms: ReturnType<typeof vi.fn>;
    getTerm: ReturnType<typeof vi.fn>;
    saveTerm: ReturnType<typeof vi.fn>;
    deleteTerm: ReturnType<typeof vi.fn>;
  };
  let permissionServiceMock: { loadForProject: ReturnType<typeof vi.fn>; canEdit: ReturnType<typeof vi.fn>; canDelete: ReturnType<typeof vi.fn> };
  let eventStreamServiceMock: { events$: typeof EMPTY; addSubscription: ReturnType<typeof vi.fn>; removeSubscription: ReturnType<typeof vi.fn> };
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  let fixture: any;
  let comp: TermEditorComponent;
  let router: Router;

  beforeEach(() => {
    paramMap$ = new BehaviorSubject(convertToParamMap({ name: 'proj1', termId: 'new' }));

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
    // term id=2 should be excluded from canonical options
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
    comp.name = 'New Term';
    comp.text = 'A definition';
    comp.canonicalTermId = null;
    await comp.onSave();
    expect(termServiceMock.saveTerm).toHaveBeenCalledWith(
      'proj1', null, 'New Term', 'A definition', null
    );
  });

  it('onSave sets errorMessage when name is empty', async () => {
    fixture.detectChanges();
    await flush();
    comp.name = '';
    await comp.onSave();
    expect(comp.errorMessage()).toBe('Term name is required.');
    expect(termServiceMock.saveTerm).not.toHaveBeenCalled();
  });
});
