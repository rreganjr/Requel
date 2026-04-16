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
    comp.newNoteText = 'My note';
    await comp.saveNote();
    expect(annotationServiceMock.addNote).toHaveBeenCalledWith('proj1', 'Goal', 42, 'My note');
    expect(annotationServiceMock.getAnnotations).toHaveBeenCalledTimes(2); // initial load + reload
    expect(comp.showNoteForm()).toBe(false);
  });

  it('saveNote() does nothing when text is blank', async () => {
    comp.newNoteText = '   ';
    await comp.saveNote();
    expect(annotationServiceMock.addNote).not.toHaveBeenCalled();
  });

  it('cancelNote() clears showNoteForm and resets text', () => {
    comp.showNoteForm.set(true);
    comp.newNoteText = 'draft';
    comp.cancelNote();
    expect(comp.showNoteForm()).toBe(false);
    expect(comp.newNoteText).toBe('');
  });

  it('saveIssue() calls addIssue with mustResolve flag and reloads', async () => {
    comp.newIssueText = 'My issue';
    comp.newIssueMustResolve = true;
    await comp.saveIssue();
    expect(annotationServiceMock.addIssue).toHaveBeenCalledWith('proj1', 'Goal', 42, 'My issue', true);
    expect(comp.showIssueForm()).toBe(false);
    expect(comp.newIssueMustResolve).toBe(false);
  });

  it('cancelIssue() clears showIssueForm and resets state', () => {
    comp.showIssueForm.set(true);
    comp.newIssueText = 'draft';
    comp.newIssueMustResolve = true;
    comp.cancelIssue();
    expect(comp.showIssueForm()).toBe(false);
    expect(comp.newIssueText).toBe('');
    expect(comp.newIssueMustResolve).toBe(false);
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

  it('startAddPosition() sets addPosIssueId and clears newPosText', () => {
    comp.newPosText = 'stale text';
    comp.startAddPosition(MOCK_ISSUE);
    expect(comp.addPosIssueId()).toBe(10);
    expect(comp.newPosText).toBe('');
  });

  it('savePosition() calls addPosition and clears state', async () => {
    comp.newPosText = 'My position';
    await comp.savePosition(MOCK_ISSUE);
    expect(annotationServiceMock.addPosition).toHaveBeenCalledWith('proj1', 10, 'My position');
    expect(comp.addPosIssueId()).toBeNull();
    expect(comp.newPosText).toBe('');
  });

  it('deletePosition() calls annotationService.deletePosition', async () => {
    await comp.deletePosition(MOCK_POSITION);
    expect(annotationServiceMock.deletePosition).toHaveBeenCalledWith('proj1', 20);
  });

  it('startAddArgument() sets addArgPositionId and resets fields', () => {
    comp.newArgText = 'stale';
    comp.newArgSupportLevel = 'Against';
    comp.startAddArgument(MOCK_POSITION);
    expect(comp.addArgPositionId()).toBe(20);
    expect(comp.newArgText).toBe('');
    expect(comp.newArgSupportLevel).toBe('For');
  });

  it('saveArgument() calls addArgument and clears state', async () => {
    comp.newArgText = 'My argument';
    comp.newArgSupportLevel = 'Against';
    await comp.saveArgument(MOCK_POSITION);
    expect(annotationServiceMock.addArgument).toHaveBeenCalledWith('proj1', 20, 'My argument', 'Against');
    expect(comp.addArgPositionId()).toBeNull();
    expect(comp.newArgText).toBe('');
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

  it('getSupportClass() returns correct CSS class for support levels', () => {
    expect(comp.getSupportClass('For')).toBe('arg-for');
    expect(comp.getSupportClass('StronglyFor')).toBe('arg-for');
    expect(comp.getSupportClass('Against')).toBe('arg-against');
    expect(comp.getSupportClass('StronglyAgainst')).toBe('arg-against');
    expect(comp.getSupportClass('Neutral')).toBe('arg-neutral');
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

  it('savePosition() does nothing when newPosText is blank', async () => {
    comp.newPosText = '   ';
    await comp.savePosition(MOCK_ISSUE);
    expect(annotationServiceMock.addPosition).not.toHaveBeenCalled();
  });

  it('saveArgument() does nothing when newArgText is blank', async () => {
    comp.newArgText = '   ';
    await comp.saveArgument(MOCK_POSITION);
    expect(annotationServiceMock.addArgument).not.toHaveBeenCalled();
  });

  it('resolveIssue() does nothing when entityId is null', async () => {
    fixture.componentRef.setInput('entityId', null);
    fixture.detectChanges();
    await flush();
    await comp.resolveIssue(MOCK_ISSUE, MOCK_POSITION);
    expect(annotationServiceMock.resolveIssue).not.toHaveBeenCalled();
  });
});
