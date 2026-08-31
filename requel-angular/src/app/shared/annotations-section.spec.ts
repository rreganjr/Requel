import { TestBed } from '@angular/core/testing';
import { render, screen } from '@testing-library/angular';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { MessageService } from 'primeng/api';
import { AnnotationsSectionComponent } from './annotations-section';
import { AnnotationService } from '../core/annotation.service';

const MOCK_ANNOTATIONS = {
  notes: [
    { id: 1, version: 0, text: 'A note about this goal', createdBy: 'admin' }
  ],
  issues: [
    {
      id: 10, version: 0, text: 'An open issue', mustBeResolved: false,
      resolved: false, createdBy: 'admin', resolvedBy: null, resolvedByPosition: null, positions: []
    }
  ]
};

describe('AnnotationsSectionComponent', () => {
  let annotationServiceMock: {
    getAnnotations: ReturnType<typeof vi.fn>;
    addNote: ReturnType<typeof vi.fn>;
    addIssue: ReturnType<typeof vi.fn>;
    resolveIssue: ReturnType<typeof vi.fn>;
  };
  let messageServiceMock: { add: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    annotationServiceMock = {
      getAnnotations: vi.fn().mockResolvedValue({ notes: [], issues: [] }),
      addNote: vi.fn().mockResolvedValue({ success: true }),
      addIssue: vi.fn().mockResolvedValue({ success: true }),
      resolveIssue: vi.fn().mockResolvedValue({ success: true }),
    };
    messageServiceMock = { add: vi.fn() };
  });

  function providers() {
    return [
      provideNoopAnimations(),
      { provide: AnnotationService, useValue: annotationServiceMock },
      { provide: MessageService, useValue: messageServiceMock },
    ];
  }

  it('shows "No annotations." when service returns empty data', async () => {
    const { fixture } = await render(AnnotationsSectionComponent, {
      providers: providers(),
      inputs: { projectName: 'proj1', entityType: 'Goal', entityId: 1, canEdit: false }
    });
    await fixture.whenStable();
    fixture.detectChanges();
    expect(screen.getByText('No annotations.')).toBeInTheDocument();
  });

  it('calls getAnnotations when entityId is provided', async () => {
    const { fixture } = await render(AnnotationsSectionComponent, {
      providers: providers(),
      inputs: { projectName: 'proj1', entityType: 'Goal', entityId: 42, canEdit: false }
    });
    await fixture.whenStable();
    expect(annotationServiceMock.getAnnotations).toHaveBeenCalledWith('proj1', 'Goal', 42);
  });

  it('does not call getAnnotations when entityId is null', async () => {
    const { fixture } = await render(AnnotationsSectionComponent, {
      providers: providers(),
      inputs: { projectName: 'proj1', entityType: 'Goal', entityId: null, canEdit: false }
    });
    await fixture.whenStable();
    expect(annotationServiceMock.getAnnotations).not.toHaveBeenCalled();
  });

  it('hides Add Note and Add Issue buttons when canEdit is false', async () => {
    const { fixture } = await render(AnnotationsSectionComponent, {
      providers: providers(),
      inputs: { projectName: 'proj1', entityType: 'Goal', entityId: 1, canEdit: false }
    });
    await fixture.whenStable();
    fixture.detectChanges();
    expect(screen.queryByText('Add Note')).not.toBeInTheDocument();
    expect(screen.queryByText('Add Issue')).not.toBeInTheDocument();
  });

  it('shows Add Note and Add Issue buttons when canEdit is true', async () => {
    const { fixture } = await render(AnnotationsSectionComponent, {
      providers: providers(),
      inputs: { projectName: 'proj1', entityType: 'Goal', entityId: 1, canEdit: true }
    });
    await fixture.whenStable();
    fixture.detectChanges();
    expect(screen.getByText('Add Note')).toBeInTheDocument();
    expect(screen.getByText('Add Issue')).toBeInTheDocument();
  });

  it('renders note and issue text from loaded annotations', async () => {
    annotationServiceMock.getAnnotations.mockResolvedValue(MOCK_ANNOTATIONS);
    const { fixture } = await render(AnnotationsSectionComponent, {
      providers: providers(),
      inputs: { projectName: 'proj1', entityType: 'Goal', entityId: 1, canEdit: false }
    });
    await fixture.whenStable();
    fixture.detectChanges();
    expect(screen.getByText('A note about this goal')).toBeInTheDocument();
    expect(screen.getByText('An open issue')).toBeInTheDocument();
  });
});

describe('AnnotationsSectionComponent (method coverage)', () => {
  const flush = () => new Promise(r => setTimeout(r, 0));

  const MOCK_NOTE = { id: 1, version: 0, text: 'A note', createdBy: 'admin' };
  const MOCK_ISSUE = {
    id: 10, version: 0, text: 'An issue', mustBeResolved: false,
    resolved: false, createdBy: 'admin', resolvedBy: null, resolvedByPosition: null, positions: []
  };
  const MOCK_POSITION = {
    id: 20, version: 0, text: 'A position', positionType: 'AddActorPosition',
    createdBy: 'admin', arguments: []
  };

  let annotationServiceMock: {
    getAnnotations: ReturnType<typeof vi.fn>;
    addNote: ReturnType<typeof vi.fn>;
    addIssue: ReturnType<typeof vi.fn>;
    resolveIssue: ReturnType<typeof vi.fn>;
    deleteNote: ReturnType<typeof vi.fn>;
    deleteIssue: ReturnType<typeof vi.fn>;
    addPosition: ReturnType<typeof vi.fn>;
    deletePosition: ReturnType<typeof vi.fn>;
    addArgument: ReturnType<typeof vi.fn>;
    deleteArgument: ReturnType<typeof vi.fn>;
  };
  let messageServiceMock: { add: ReturnType<typeof vi.fn> };
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  let fixture: any;
  let comp: AnnotationsSectionComponent;

  beforeEach(() => {
    annotationServiceMock = {
      getAnnotations: vi.fn().mockResolvedValue({ notes: [], issues: [] }),
      addNote: vi.fn().mockResolvedValue({ success: true }),
      addIssue: vi.fn().mockResolvedValue({ success: true }),
      resolveIssue: vi.fn().mockResolvedValue({ success: true }),
      deleteNote: vi.fn().mockResolvedValue({ success: true }),
      deleteIssue: vi.fn().mockResolvedValue({ success: true }),
      addPosition: vi.fn().mockResolvedValue({ success: true }),
      deletePosition: vi.fn().mockResolvedValue({ success: true }),
      addArgument: vi.fn().mockResolvedValue({ success: true }),
      deleteArgument: vi.fn().mockResolvedValue({ success: true }),
    };
    messageServiceMock = { add: vi.fn() };

    TestBed.configureTestingModule({
      imports: [AnnotationsSectionComponent],
      providers: [
        provideNoopAnimations(),
        { provide: AnnotationService, useValue: annotationServiceMock },
        { provide: MessageService, useValue: messageServiceMock }
      ]
    });
    fixture = TestBed.createComponent(AnnotationsSectionComponent);
    comp = fixture.componentInstance;
    fixture.componentRef.setInput('projectName', 'proj1');
    fixture.componentRef.setInput('entityType', 'Goal');
    fixture.componentRef.setInput('entityId', 42);
    fixture.componentRef.setInput('canEdit', true);
    fixture.detectChanges();
  });

  it('saveNote() calls addNote and reloads', async () => {
    comp.noteForm.controls.text.setValue('My note');
    await comp.saveNote();
    expect(annotationServiceMock.addNote).toHaveBeenCalledWith('proj1', 'Goal', 42, 'My note');
    expect(annotationServiceMock.getAnnotations).toHaveBeenCalledTimes(2); // initial load + reload
    expect(comp.showNoteForm()).toBe(false);
  });

  it('saveNote() shows the required message and calls nothing when text is blank', async () => {
    comp.showNoteForm.set(true);
    fixture.detectChanges();
    comp.noteForm.controls.text.setValue('   ');
    await comp.saveNote();
    fixture.detectChanges();
    expect(annotationServiceMock.addNote).not.toHaveBeenCalled();
    const el = fixture.nativeElement as HTMLElement;
    const input = el.querySelector('[data-testid="annotation-note-text"]') as HTMLElement;
    expect(input.getAttribute('aria-invalid')).toBe('true');
    expect(input.getAttribute('aria-describedby')).toBe('annotation-note-error');
    const err = el.querySelector('[data-testid="annotation-note-error"]') as HTMLElement;
    expect(err.textContent).toContain('Note text is required.');
    expect(err.getAttribute('role')).toBe('alert');
  });

  it('cancelNote() clears showNoteForm and resets text', () => {
    comp.showNoteForm.set(true);
    comp.noteForm.controls.text.setValue('draft');
    comp.cancelNote();
    expect(comp.showNoteForm()).toBe(false);
    expect(comp.noteForm.controls.text.value).toBe('');
  });

  it('saveIssue() calls addIssue with mustResolve flag and reloads', async () => {
    comp.issueForm.controls.text.setValue('My issue');
    comp.issueForm.controls.mustResolve.setValue(true);
    await comp.saveIssue();
    expect(annotationServiceMock.addIssue).toHaveBeenCalledWith('proj1', 'Goal', 42, 'My issue', true);
    expect(comp.showIssueForm()).toBe(false);
    expect(comp.issueForm.controls.mustResolve.value).toBe(false);
  });

  it('cancelIssue() clears showIssueForm and resets state', () => {
    comp.showIssueForm.set(true);
    comp.issueForm.controls.text.setValue('draft');
    comp.issueForm.controls.mustResolve.setValue(true);
    comp.cancelIssue();
    expect(comp.showIssueForm()).toBe(false);
    expect(comp.issueForm.controls.text.value).toBe('');
    expect(comp.issueForm.controls.mustResolve.value).toBe(false);
  });

  it('deleteNote() calls annotationService.deleteNote and reloads', async () => {
    await comp.deleteNote(MOCK_NOTE);
    expect(annotationServiceMock.deleteNote).toHaveBeenCalledWith('proj1', 1);
    expect(annotationServiceMock.getAnnotations).toHaveBeenCalledTimes(2);
  });

  it('deleteIssue() calls annotationService.deleteIssue and reloads', async () => {
    await comp.deleteIssue(MOCK_ISSUE);
    expect(annotationServiceMock.deleteIssue).toHaveBeenCalledWith('proj1', 10);
    expect(annotationServiceMock.getAnnotations).toHaveBeenCalledTimes(2);
  });

  it('startAddPosition() sets addPosIssueId and clears the position text', () => {
    comp.positionForm.controls.text.setValue('stale text');
    comp.startAddPosition(MOCK_ISSUE);
    expect(comp.addPosIssueId()).toBe(10);
    expect(comp.positionForm.controls.text.value).toBe('');
  });

  it('savePosition() calls addPosition and clears state', async () => {
    comp.positionForm.controls.text.setValue('My position');
    await comp.savePosition(MOCK_ISSUE);
    expect(annotationServiceMock.addPosition).toHaveBeenCalledWith('proj1', 10, 'My position');
    expect(comp.addPosIssueId()).toBeNull();
    expect(comp.positionForm.controls.text.value).toBe('');
  });

  it('deletePosition() calls annotationService.deletePosition', async () => {
    await comp.deletePosition(MOCK_POSITION);
    expect(annotationServiceMock.deletePosition).toHaveBeenCalledWith('proj1', 20);
  });

  it('startAddArgument() sets addArgPositionId and resets fields', () => {
    comp.argumentForm.controls.text.setValue('stale');
    comp.argumentForm.controls.supportLevel.setValue('Against');
    comp.startAddArgument(MOCK_POSITION);
    expect(comp.addArgPositionId()).toBe(20);
    expect(comp.argumentForm.controls.text.value).toBe('');
    expect(comp.argumentForm.controls.supportLevel.value).toBe('For');
  });

  it('saveArgument() calls addArgument and clears state', async () => {
    comp.argumentForm.controls.text.setValue('My argument');
    comp.argumentForm.controls.supportLevel.setValue('Against');
    await comp.saveArgument(MOCK_POSITION);
    expect(annotationServiceMock.addArgument).toHaveBeenCalledWith('proj1', 20, 'My argument', 'Against');
    expect(comp.addArgPositionId()).toBeNull();
    expect(comp.argumentForm.controls.text.value).toBe('');
  });

  it('deleteArgument() calls annotationService.deleteArgument', async () => {
    await comp.deleteArgument(MOCK_POSITION, { id: 30 });
    expect(annotationServiceMock.deleteArgument).toHaveBeenCalledWith('proj1', 30);
  });

  it('resolveIssue() calls annotationService.resolveIssue and reloads', async () => {
    await comp.resolveIssue(MOCK_ISSUE, MOCK_POSITION);
    expect(annotationServiceMock.resolveIssue).toHaveBeenCalledWith('proj1', 10, 20);
    expect(annotationServiceMock.getAnnotations).toHaveBeenCalledTimes(2);
  });

  it('supportTone() maps support levels to app-tag tones', () => {
    expect(comp.supportTone('For')).toBe('success');
    expect(comp.supportTone('StronglyFor')).toBe('success');
    expect(comp.supportTone('Against')).toBe('danger');
    expect(comp.supportTone('StronglyAgainst')).toBe('danger');
    expect(comp.supportTone('Neutral')).toBe('neutral');
  });

  it('supportIcon() maps support levels to leading icons', () => {
    expect(comp.supportIcon('For')).toBe('pi pi-thumbs-up');
    expect(comp.supportIcon('StronglyAgainst')).toBe('pi pi-thumbs-down');
    expect(comp.supportIcon('Neutral')).toBe('pi pi-minus-circle');
  });

  it('formatSupportLevel() returns label from SUPPORT_LEVEL_OPTIONS', () => {
    expect(comp.formatSupportLevel('StronglyFor')).toBe('Strongly For');
    expect(comp.formatSupportLevel('For')).toBe('For');
    expect(comp.formatSupportLevel('UnknownLevel')).toBe('UnknownLevel');
  });

  it('resolveLabel() returns correct label for known position types', () => {
    expect(comp.resolveLabel('AddWordToDictionaryPosition')).toBe('Add to Dictionary');
    expect(comp.resolveLabel('ChangeSpellingPosition')).toBe('Fix Spelling');
    expect(comp.resolveLabel('AddActorPosition')).toBe('Add as Actor');
    expect(comp.resolveLabel('AddGlossaryTermPosition')).toBe('Add to Glossary');
    expect(comp.resolveLabel('UnknownPosition')).toBe('Ignore');
  });

  it('savePosition() shows the required error and calls nothing when text is blank', async () => {
    comp.positionForm.controls.text.setValue('   ');
    await comp.savePosition(MOCK_ISSUE);
    expect(annotationServiceMock.addPosition).not.toHaveBeenCalled();
    expect(comp.positionForm.controls.text.invalid).toBe(true);
    expect(comp.positionForm.controls.text.touched).toBe(true);
  });

  it('saveArgument() shows the required error and calls nothing when text is blank', async () => {
    comp.argumentForm.controls.text.setValue('   ');
    await comp.saveArgument(MOCK_POSITION);
    expect(annotationServiceMock.addArgument).not.toHaveBeenCalled();
    expect(comp.argumentForm.controls.text.invalid).toBe(true);
    expect(comp.argumentForm.controls.text.touched).toBe(true);
  });

  it('resolveIssue() does nothing when entityId is null', async () => {
    fixture.componentRef.setInput('entityId', null);
    fixture.detectChanges();
    await flush();
    await comp.resolveIssue(MOCK_ISSUE, MOCK_POSITION);
    expect(annotationServiceMock.resolveIssue).not.toHaveBeenCalled();
  });

  it('toggleIssue() collapses and expands a single issue body (#226)', async () => {
    annotationServiceMock.getAnnotations.mockResolvedValue({
      notes: [],
      issues: [{ ...MOCK_ISSUE, positions: [MOCK_POSITION] }]
    });
    comp.reload();
    await flush();
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    expect(comp.isCollapsed(10)).toBe(false);
    expect(el.querySelector('[data-testid="annotation-position"]')).not.toBeNull();

    comp.toggleIssue(10);
    fixture.detectChanges();
    expect(comp.isCollapsed(10)).toBe(true);
    // body hidden, but the issue row itself still shows
    expect(el.querySelector('[data-testid="annotation-position"]')).toBeNull();
    expect(el.textContent).toContain('An issue');

    comp.toggleIssue(10);
    fixture.detectChanges();
    expect(comp.isCollapsed(10)).toBe(false);
    expect(el.querySelector('[data-testid="annotation-position"]')).not.toBeNull();
  });

  it('toggleAll() collapses every issue then expands them (#226)', async () => {
    annotationServiceMock.getAnnotations.mockResolvedValue({
      notes: [],
      issues: [
        { ...MOCK_ISSUE, id: 10, positions: [MOCK_POSITION] },
        { ...MOCK_ISSUE, id: 11, text: 'Second issue', positions: [{ ...MOCK_POSITION, id: 21 }] }
      ]
    });
    comp.reload();
    await flush();
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    expect(comp.allIssuesCollapsed()).toBe(false);
    expect(el.querySelectorAll('[data-testid="annotation-position"]').length).toBe(2);

    comp.toggleAll();
    fixture.detectChanges();
    expect(comp.allIssuesCollapsed()).toBe(true);
    expect(comp.isCollapsed(10)).toBe(true);
    expect(comp.isCollapsed(11)).toBe(true);
    expect(el.querySelectorAll('[data-testid="annotation-position"]').length).toBe(0);

    comp.toggleAll();
    fixture.detectChanges();
    expect(comp.allIssuesCollapsed()).toBe(false);
    expect(el.querySelectorAll('[data-testid="annotation-position"]').length).toBe(2);
  });

  it('collapse-all button is absent and allIssuesCollapsed() false with no issues (#226)', () => {
    const el = fixture.nativeElement as HTMLElement;
    expect(comp.allIssuesCollapsed()).toBe(false);
    expect(el.querySelector('[data-testid="annotation-collapse-all"]')).toBeNull();
  });
});
