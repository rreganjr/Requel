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
