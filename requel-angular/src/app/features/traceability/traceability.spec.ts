import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { TraceabilityComponent } from './traceability';
import { TraceabilityService } from '../../core/traceability.service';
import { TraceabilityDto } from '../../models/traceability';
import { expectNoAxeViolations } from '../../shared/testing/a11y';

const TRACE: TraceabilityDto = {
  goals: [
    { id: 1, name: 'Fast checkout', realized: true,
      stakeholders: [{ entityType: 'Stakeholder', id: 20, name: 'Owner' }],
      actors: [{ entityType: 'Actor', id: 5, name: 'Clerk' }],
      stories: [{ entityType: 'Story', id: 6, name: 'Clerk scans' }],
      useCases: [{ entityType: 'UseCase', id: 7, name: 'Check out' }] },
    { id: 2, name: 'Fewer refunds', realized: false,
      stakeholders: [{ entityType: 'Stakeholder', id: 21, name: 'Sponsor' }], actors: [], stories: [], useCases: [] },
  ],
  gaps: [
    { kind: 'GOAL_NOT_REALIZED', entity: { entityType: 'Goal', id: 2, name: 'Fewer refunds' } },
    { kind: 'ACTOR_NOT_USED', entity: { entityType: 'Actor', id: 8, name: 'Auditor' } },
    { kind: 'SCENARIO_NOT_IN_USE_CASE', entity: { entityType: 'Scenario', id: 9, name: 'Stray' } },
    { kind: 'STAKEHOLDER_WITHOUT_GOAL', entity: { entityType: 'Stakeholder', id: 22, name: 'Quiet' } },
  ],
};

const flush = () => new Promise(r => setTimeout(r, 0));

describe('TraceabilityComponent (#497)', () => {
  let getTraceability: ReturnType<typeof vi.fn>;

  async function open() {
    const fixture = TestBed.createComponent(TraceabilityComponent);
    fixture.detectChanges();
    await flush();
    fixture.detectChanges();
    return fixture;
  }

  const el = (fixture: { nativeElement: HTMLElement }) => fixture.nativeElement as HTMLElement;
  const all = (fixture: { nativeElement: HTMLElement }, testid: string) =>
    Array.from(el(fixture).querySelectorAll(`[data-testid="${testid}"]`)) as HTMLElement[];

  beforeEach(() => {
    getTraceability = vi.fn().mockResolvedValue(TRACE);
    TestBed.configureTestingModule({
      imports: [TraceabilityComponent],
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ name: 'proj1' }) } } },
        { provide: TraceabilityService, useValue: { getTraceability } },
      ],
    });
  });

  it('shows a row per goal with each referencing entity linked in its column', async () => {
    const fixture = await open();
    expect(getTraceability).toHaveBeenCalledWith('proj1');
    const rows = all(fixture, 'trace-goal-row');
    expect(rows).toHaveLength(2);
    const cells = Array.from(rows[0].querySelectorAll('td:not(.dt-actions-col)')).map(td => td.textContent!.replace(/\s+/g, ' ').trim());
    expect(cells).toEqual(['Fast checkout', 'Owner', 'Clerk', 'Clerk scans', 'Check out']);
    const links = Array.from(rows[0].querySelectorAll('a')).map(a => a.getAttribute('href'));
    expect(links).toEqual(['/projects/proj1/goals/1', '/projects/proj1/stakeholders/20',
      '/projects/proj1/actors/5', '/projects/proj1/stories/6', '/projects/proj1/use-cases/7']);
  });

  it('marks a goal nothing realizes, and only that one', async () => {
    const fixture = await open();
    const rows = all(fixture, 'trace-goal-row');
    expect(rows[0].querySelector('[data-testid="trace-not-realized"]')).toBeNull();
    expect(rows[1].querySelector('[data-testid="trace-not-realized"]')!.textContent).toContain('Not realized');
  });

  it('lists the gaps grouped by kind, in kind order, each linked', async () => {
    const fixture = await open();
    const groups = Array.from(el(fixture).querySelectorAll('[data-testid^="trace-gap-"]'))
      .map(g => g.getAttribute('data-testid'))
      .filter(id => id !== 'trace-gap-link');
    expect(groups).toEqual(['trace-gap-GOAL_NOT_REALIZED', 'trace-gap-ACTOR_NOT_USED',
      'trace-gap-SCENARIO_NOT_IN_USE_CASE', 'trace-gap-STAKEHOLDER_WITHOUT_GOAL']);
    const actorGap = all(fixture, 'trace-gap-ACTOR_NOT_USED')[0];
    expect(actorGap.textContent).toContain('Actors nothing uses');
    expect(actorGap.querySelector('a')!.getAttribute('href')).toBe('/projects/proj1/actors/8');
    expect(el(fixture).textContent).toContain('Gaps 4');
  });

  it('says when there are no gaps, and when there are no goals', async () => {
    getTraceability.mockResolvedValue({ goals: [], gaps: [] });
    const fixture = await open();
    expect(all(fixture, 'trace-no-gaps')).toHaveLength(1);
    expect(all(fixture, 'trace-no-goals')).toHaveLength(1);
    expect(all(fixture, 'trace-goal-row')).toHaveLength(0);
  });

  it('shows an error and loads again on retry', async () => {
    getTraceability.mockRejectedValueOnce(new Error('boom'));
    const fixture = await open();
    expect(all(fixture, 'traceability-error')[0].textContent).toContain('Failed to load traceability.');
    await fixture.componentInstance.load();
    fixture.detectChanges();
    expect(all(fixture, 'trace-goal-row')).toHaveLength(2);
  });

  it('has no axe violations', async () => {
    const fixture = await open();
    await expectNoAxeViolations(el(fixture));
  });
});
