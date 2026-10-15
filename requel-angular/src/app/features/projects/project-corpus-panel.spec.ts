import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { ProjectCorpusPanelComponent } from './project-corpus-panel';
import { CorpusService } from '../../core/corpus.service';
import { GoalService } from '../../core/goal.service';
import { UseCaseService } from '../../core/use-case.service';
import { CorpusRunDto } from '../../models/corpus';

function run(over: Partial<CorpusRunDto> = {}): CorpusRunDto {
  return {
    runId: 'r1', status: 'SUCCEEDED', createdAt: '2026-10-03T00:00:00Z', completedAt: null,
    errorKind: null, errorSummary: null, summary: '12 entities; 1 possible conflicts, 1 possible overlaps',
    findings: [
      { findingType: 'POSSIBLE_CONFLICT', kind: 'ISSUE', severity: 'LOW', text: 'x', state: 'ACTIVE', annotationId: 7 },
      { findingType: 'POSSIBLE_CONFLICT', kind: 'ISSUE', severity: 'LOW', text: 'x', state: 'ACTIVE', annotationId: 7 },
      { findingType: 'POSSIBLE_OVERLAP', kind: 'ISSUE', severity: 'LOW', text: 'y', state: 'ACTIVE', annotationId: 8 },
      { findingType: 'POSSIBLE_OVERLAP', kind: 'ISSUE', severity: 'LOW', text: 'y', state: 'ACTIVE', annotationId: 8 },
    ],
    ...over,
  };
}

describe('ProjectCorpusPanelComponent (#266)', () => {
  let latest: ReturnType<typeof vi.fn>;
  let request: ReturnType<typeof vi.fn>;
  const flush = () => new Promise(r => setTimeout(r, 0));

  async function render(canAnalyze: boolean) {
    TestBed.configureTestingModule({
      imports: [ProjectCorpusPanelComponent],
      providers: [
        provideNoopAnimations(),
        { provide: CorpusService, useValue: { latest, request } },
        { provide: GoalService, useValue: { listGoals: vi.fn().mockResolvedValue([{ id: 3, name: 'Two-week loans' }]) } },
        { provide: UseCaseService, useValue: { listUseCases: vi.fn().mockResolvedValue([{ id: 9, name: 'Renew a loan' }]) } },
      ],
    });
    const fixture = TestBed.createComponent(ProjectCorpusPanelComponent);
    fixture.componentRef.setInput('projectName', 'Acme');
    fixture.componentRef.setInput('projectId', 1);
    fixture.componentRef.setInput('canAnalyze', canAnalyze);
    fixture.componentRef.setInput('pollMs', 0);
    fixture.detectChanges();
    await flush();
    await flush();
    fixture.detectChanges();
    return fixture;
  }

  beforeEach(() => {
    latest = vi.fn().mockResolvedValue(null);
    request = vi.fn().mockResolvedValue(undefined);
  });

  it('offers the whole project, each goal and each use case as a set', async () => {
    const fixture = await render(true);
    expect(fixture.componentInstance.options().map(o => o.label))
      .toEqual(['Whole project', 'Goal: Two-week loans', 'Use case: Renew a loan']);
    expect(latest).toHaveBeenCalledWith(1, 'PROJECT', null, 'CANDIDATES');
  });

  it('shows the last run with one count per relationship, not per entity', async () => {
    latest = vi.fn().mockResolvedValue(run());
    const fixture = await render(true);
    const text = fixture.nativeElement.querySelector('[data-testid="corpus-latest"]').textContent;
    expect(text).toContain('finished');
    expect(text).toContain('1 possible conflicts');
    expect(text).toContain('(2 open)');
  });

  it('runs Find overlaps on the chosen set and reports when the new run finishes', async () => {
    // load, select and the pre-run read all see the old run; the poll sees the new one
    latest = vi.fn()
      .mockResolvedValueOnce(run({ runId: 'old' }))
      .mockResolvedValueOnce(run({ runId: 'old' }))
      .mockResolvedValueOnce(run({ runId: 'old' }))
      .mockResolvedValue(run({ runId: 'new' }));
    const fixture = await render(true);
    const panel = fixture.componentInstance;
    await panel.select(panel.options()[1]);
    await panel.run('CANDIDATES');
    expect(request).toHaveBeenCalledWith(1, 'GOAL', 3, 'CANDIDATES');
    for (let i = 0; i < 5; i++) await flush();
    fixture.detectChanges();
    expect(panel.busy()).toBe(false);
    expect(fixture.nativeElement.querySelector('[data-testid="corpus-status"]').textContent)
      .toContain('Finished');
  });

  it('has no run button without Annotation[Edit]', async () => {
    const fixture = await render(false);
    expect(fixture.nativeElement.querySelector('[data-testid="corpus-find-overlaps"]')).toBeNull();
  });

  it('shows the server error when the run cannot start', async () => {
    request = vi.fn().mockRejectedValue({ error: { message: 'You do not have access to this project.' } });
    const fixture = await render(true);
    await fixture.componentInstance.run('CANDIDATES');
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[data-testid="corpus-error"]').textContent)
      .toContain('You do not have access');
  });

  it('runs the AI analysis as its own mode', async () => {
    const fixture = await render(true);
    await fixture.componentInstance.run('ANALYSIS');
    expect(request).toHaveBeenCalledWith(1, 'PROJECT', null, 'ANALYSIS');
    expect(latest).toHaveBeenLastCalledWith(1, 'PROJECT', null, 'ANALYSIS');
    expect(fixture.nativeElement.querySelector('[data-testid="corpus-analyse"]')).not.toBeNull();
  });
});
