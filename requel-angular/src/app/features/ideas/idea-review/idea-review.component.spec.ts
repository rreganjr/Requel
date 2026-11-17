import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { IdeaView } from '../../../models/idea-view';
import { IdeaService } from '../idea.service';
import { IdeaReviewComponent } from './idea-review.component';

function idea(overrides: Partial<IdeaView> = {}): IdeaView {
  return {
    id: 'i1', projectId: 1, ideaText: 'text', status: 'DRAFTED', errorSummary: null,
    summary: 'A gym app', attempts: 1, retryable: false, messages: ['What if a class is full?'],
    problems: [],
    drafts: [
      { kind: 'ACTOR', name: 'Gym member', text: 'A member.', note: null, assumption: false,
        evidence: [], evidenceUnverified: false, confidence: 0.9, references: [] },
      { kind: 'GOAL', name: 'Book remotely', text: 'Book without calling.', note: null,
        assumption: true, evidence: [], evidenceUnverified: false, confidence: null, references: [] },
    ],
    ...overrides,
  };
}

describe('IdeaReviewComponent', () => {
  let fixture: ComponentFixture<IdeaReviewComponent>;
  let component: IdeaReviewComponent;
  const getIdea = vi.fn();
  const accept = vi.fn();
  const retry = vi.fn();

  async function create(): Promise<void> {
    await TestBed.configureTestingModule({
      imports: [IdeaReviewComponent],
      providers: [
        provideRouter([]),
        { provide: IdeaService, useValue: { getIdea, accept, retry } },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ project: 'Gym', id: 'i1' }) } } },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(IdeaReviewComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  beforeEach(() => {
    getIdea.mockReset();
    accept.mockReset();
    retry.mockReset();
  });

  afterEach(() => vi.useRealTimers());

  it('lists the drafts by kind, all chosen to start with', async () => {
    getIdea.mockResolvedValue(idea());
    await create();
    expect(component.groups().map(g => g.heading)).toEqual(['Actors', 'Goals']);
    expect(component.selected().size).toBe(2);
    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Gym member');
    expect(text).toContain('What if a class is full?');
    expect(text).toContain('assumption');
  });

  it('accepts only the chosen positions', async () => {
    getIdea.mockResolvedValue(idea());
    accept.mockResolvedValue({ projectName: 'Gym', created: { goal: 1 }, notes: [] });
    await create();
    component.toggle(0);
    await component.accept();
    expect(accept).toHaveBeenCalledWith('Gym', 'i1', [1]);
    expect(component.accepted()?.created).toEqual({ goal: 1 });
  });

  it('will not accept with nothing chosen', async () => {
    getIdea.mockResolvedValue(idea());
    await create();
    component.selectAll(false);
    await component.accept();
    expect(accept).not.toHaveBeenCalled();
  });

  it('keeps polling while generation is pending, then shows the drafts', async () => {
    vi.useFakeTimers();
    getIdea.mockResolvedValueOnce(idea({ status: 'PENDING', drafts: [] })).mockResolvedValue(idea());
    await create();
    expect(component.working()).toBe(true);
    await vi.advanceTimersByTimeAsync(2100);
    expect(component.working()).toBe(false);
    expect(component.idea()?.drafts.length).toBe(2);
    expect(getIdea).toHaveBeenCalledTimes(2);
  });

  it('shows why a failed idea failed and retries it', async () => {
    getIdea.mockResolvedValue(idea({ status: 'FAILED', errorSummary: 'provider down', retryable: true, drafts: [] }));
    retry.mockResolvedValue({});
    await create();
    expect(fixture.nativeElement.textContent).toContain('provider down');
    await component.retry();
    expect(retry).toHaveBeenCalledWith('Gym', 'i1');
  });

  it('opens the project', async () => {
    getIdea.mockResolvedValue(idea());
    await create();
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    component.openProject();
    expect(navigate).toHaveBeenCalledWith(['/projects', 'Gym']);
  });
});
