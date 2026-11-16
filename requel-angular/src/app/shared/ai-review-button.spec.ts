import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { AiReviewButtonComponent } from './ai-review-button';
import { AiReviewService } from '../core/ai-review.service';
import { AiReviewRun } from '../models/ai-review';
import { expectNoAxeViolations } from './testing/a11y';

// Issue #387: "Review with AI" beside an editor's Save.

const settle = async (rounds = 6) => {
  for (let i = 0; i < rounds; i++) await new Promise(r => setTimeout(r, 0));
};

function run(status: string, runId = 'run-1', findings: AiReviewRun['findings'] = []): AiReviewRun {
  return { runId, status, createdAt: '2026-10-30T12:00:00Z', completedAt: null, errorKind: null,
    errorSummary: status === 'FAILED' ? 'provider timed out' : null, summary: null, findings };
}

const finding = (kind: string) => ({ findingType: 'AMBIGUOUS', kind, severity: 'HIGH', text: 'x',
  state: 'ACTIVE', annotationId: 1 });

describe('AiReviewButtonComponent', () => {
  let reviews: {
    availability: ReturnType<typeof vi.fn>;
    request: ReturnType<typeof vi.fn>;
    latest: ReturnType<typeof vi.fn>;
    notifyCompleted: ReturnType<typeof vi.fn>;
  };
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  let fixture: any;

  beforeEach(() => {
    reviews = {
      availability: vi.fn().mockResolvedValue({ available: true, reason: null }),
      request: vi.fn().mockResolvedValue({ runId: 'run-1', policyRunId: null }),
      latest: vi.fn().mockResolvedValue(null),
      notifyCompleted: vi.fn(),
    };
    TestBed.configureTestingModule({
      imports: [AiReviewButtonComponent],
      providers: [provideNoopAnimations(), { provide: AiReviewService, useValue: reviews }],
    });
  });

  afterEach(() => fixture?.destroy());

  async function create(inputs: Record<string, unknown> = {}) {
    fixture = TestBed.createComponent(AiReviewButtonComponent);
    const all = { entityType: 'Goal', entityId: 3, pollMs: 0, ...inputs };
    for (const [k, v] of Object.entries(all)) fixture.componentRef.setInput(k, v);
    fixture.detectChanges();
    await settle();
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  const button = (el: HTMLElement) =>
    el.querySelector('[data-testid="ai-review-button"] button') as HTMLButtonElement | null;
  const status = (el: HTMLElement) =>
    el.querySelector('[data-testid="ai-review-status"]')!.textContent!.trim();

  it('shows the button when a review would run', async () => {
    const el = await create();
    expect(reviews.availability).toHaveBeenCalledWith({ entityType: 'Goal', entityId: 3 });
    expect(button(el)).not.toBeNull();
    expect(button(el)!.disabled).toBe(false);
    expect(button(el)!.textContent).toContain('Review with AI');
  });

  it('shows nothing when a review would not run', async () => {
    reviews.availability.mockResolvedValue({ available: false, reason: 'AI review is off on this server.' });
    const el = await create();
    expect(button(el)).toBeNull();
    expect(status(el)).toBe('');
  });

  it('shows nothing, and asks nothing, for a step, a report or an unsaved entity', async () => {
    for (const inputs of [{ entityType: 'Step' }, { entityType: 'ReportGenerator' }, { entityId: null }]) {
      const el = await create(inputs);
      expect(button(el)).toBeNull();
      fixture.destroy();
    }
    expect(reviews.availability).not.toHaveBeenCalled();
  });

  it('shows nothing when availability cannot be read', async () => {
    reviews.availability.mockRejectedValue(new HttpErrorResponse({ status: 403 }));
    const el = await create();
    expect(button(el)).toBeNull();
  });

  it('is disabled with the save-first hint while the form has unsaved changes', async () => {
    const el = await create({ dirty: true });
    expect(button(el)!.disabled).toBe(true);
    expect(button(el)!.getAttribute('aria-label')).toContain('Save first');
    await fixture.componentInstance.requestReview();
    expect(reviews.request).not.toHaveBeenCalled();

    fixture.componentRef.setInput('dirty', false);
    fixture.detectChanges();
    expect(button(el)!.disabled).toBe(false);
  });

  it('requests a review, follows it to done and reports it for the annotations to reload', async () => {
    reviews.latest
      .mockResolvedValueOnce(null)                    // on load: never reviewed
      .mockResolvedValueOnce(run('QUEUED'))
      .mockResolvedValueOnce(run('RUNNING'))
      .mockResolvedValue(run('SUCCEEDED', 'run-1', [finding('ISSUE'), finding('ISSUE'), finding('NOTE')]));
    const el = await create();

    button(el)!.click();
    await settle(2);
    fixture.detectChanges();
    expect(reviews.request).toHaveBeenCalledWith({ entityType: 'Goal', entityId: 3 });
    expect(button(el)!.disabled).toBe(true);

    await settle(12);
    fixture.detectChanges();
    expect(status(el)).toBe('AI review done: 2 issues and 1 note, in Annotations below.');
    expect(reviews.notifyCompleted).toHaveBeenCalledWith({ entityType: 'Goal', entityId: 3 });
    expect(button(el)!.disabled).toBe(false);
  });

  it('waits for its own run, not an older finished one', async () => {
    reviews.latest
      .mockResolvedValueOnce(run('SUCCEEDED', 'old'))  // on load
      .mockResolvedValueOnce(run('SUCCEEDED', 'old'))  // first poll: not ours yet
      .mockResolvedValue(run('SUCCEEDED', 'run-1'));
    const el = await create();
    expect(status(el)).toBe('');

    await fixture.componentInstance.requestReview();
    await settle(12);
    fixture.detectChanges();
    expect(status(el)).toBe('AI review done: no findings.');
    expect(reviews.notifyCompleted).toHaveBeenCalledTimes(1);
  });

  it('says why a failed review failed, without reloading the annotations', async () => {
    reviews.latest.mockResolvedValueOnce(null).mockResolvedValue(run('FAILED'));
    const el = await create();
    await fixture.componentInstance.requestReview();
    await settle(8);
    fixture.detectChanges();
    expect(status(el)).toBe('AI review failed: provider timed out.');
    expect(reviews.notifyCompleted).not.toHaveBeenCalled();
  });

  it('drops the button and says why when the request is refused as unavailable', async () => {
    reviews.request.mockRejectedValue(new HttpErrorResponse({ status: 409,
      error: { error: 'AI_REVIEW_UNAVAILABLE', message: 'AI review of goals is switched off for this project.' } }));
    const el = await create();
    await fixture.componentInstance.requestReview();
    fixture.detectChanges();
    expect(button(el)).toBeNull();
    expect(status(el)).toBe('AI review of goals is switched off for this project.');
  });

  it('follows a review already in flight when the editor opens', async () => {
    reviews.latest.mockResolvedValueOnce(run('RUNNING', 'earlier'))
      .mockResolvedValue(run('SUCCEEDED', 'earlier'));
    const el = await create();
    await settle(8);
    fixture.detectChanges();
    expect(reviews.request).not.toHaveBeenCalled();
    expect(status(el)).toBe('AI review done: no findings.');
    expect(reviews.notifyCompleted).toHaveBeenCalled();
  });

  it('gives up after the cap with a check-back message', async () => {
    reviews.latest.mockResolvedValueOnce(null).mockResolvedValue(run('RUNNING'));
    const el = await create({ maxPolls: 3 });
    await fixture.componentInstance.requestReview();
    await settle(12);
    fixture.detectChanges();
    expect(status(el)).toBe('The AI review is still running; check back later.');
    expect(reviews.latest).toHaveBeenCalledTimes(4);
    expect(button(el)!.disabled).toBe(false);
  });

  it('stops polling when destroyed', async () => {
    reviews.latest.mockResolvedValueOnce(null).mockResolvedValue(run('RUNNING'));
    await create({ pollMs: 5 });
    await fixture.componentInstance.requestReview();
    fixture.destroy();
    const calls = reviews.latest.mock.calls.length;
    await new Promise(r => setTimeout(r, 30));
    expect(reviews.latest.mock.calls.length).toBe(calls);
    fixture = null;
  });

  it('has no axe-core violations', async () => {
    const el = await create();
    await expectNoAxeViolations(el);
  });
});
