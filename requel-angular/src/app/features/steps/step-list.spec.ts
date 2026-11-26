import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter, Router, ActivatedRoute, convertToParamMap, Params } from '@angular/router';
import { StepListComponent } from './step-list';
import { ScenarioService } from '../../core/scenario.service';
import { StepSummaryDto } from '../../models/scenario';
import { expectNoAxeViolations } from '../../shared/testing/a11y';

// #505: a shared step, a single-use step, and a step with a HIGH open issue and a named actor.
const STEPS: StepSummaryDto[] = [
  { id: 1, name: 'enters the order', scenarioType: 'Primary', usedIn: 2, openIssues: 0,
    actorRole: 'PRIMARY_ACTOR', displayName: 'Primary actor enters the order' },
  { id: 2, name: 'Clerk counts the shelf', scenarioType: 'Optional', usedIn: 1, openIssues: 0 },
  { id: 3, name: 'prints a receipt', scenarioType: 'Primary', usedIn: 1, openIssues: 2,
    highestOpenSeverity: 'HIGH', actorRole: 'NAMED', actorName: 'Printer', displayName: 'Printer prints a receipt' },
];

const flush = () => new Promise(r => setTimeout(r, 0));

describe('StepListComponent (#505)', () => {
  let listSteps: ReturnType<typeof vi.fn>;
  let router: Router;

  async function open(queryParams: Params = {}) {
    TestBed.overrideProvider(ActivatedRoute, { useValue: { snapshot: {
      paramMap: convertToParamMap({ name: 'proj1' }),
      queryParamMap: convertToParamMap(queryParams),
    } } });
    const fixture = TestBed.createComponent(StepListComponent);
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
    fixture.detectChanges();
    await flush();
    fixture.detectChanges();
    return fixture;
  }

  const rows = (fixture: { nativeElement: HTMLElement }) =>
    Array.from(fixture.nativeElement.querySelectorAll('[data-testid="step-row"]')) as HTMLElement[];
  const names = (fixture: { componentInstance: StepListComponent }) =>
    fixture.componentInstance.visible().map(s => s.name);

  beforeEach(() => {
    listSteps = vi.fn().mockResolvedValue(STEPS);
    TestBed.configureTestingModule({
      imports: [StepListComponent],
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        { provide: ActivatedRoute, useValue: {} },
        { provide: ScenarioService, useValue: { listSteps } },
      ],
    });
  });

  it('lists every step with who does it, its usage and its open issues', async () => {
    const fixture = await open();
    expect(listSteps).toHaveBeenCalledWith('proj1');
    const r = rows(fixture);
    expect(r).toHaveLength(3);
    const text = r.map(row => row.textContent!.replace(/\s+/g, ' '));
    expect(text[0]).toContain('enters the order');
    expect(text[0]).toContain('Primary actor');
    expect(text[0]).toContain('2');
    expect(text[2]).toContain('Printer');
    const severity = r[2].querySelector('[data-testid="step-severity"]')!;
    expect(severity.textContent).toContain('High');
    expect(r[2].querySelector('[data-testid="step-open-issues"]')!.textContent).toContain('2');
    expect(r[1].querySelector('[data-testid="step-severity"]')).toBeNull();
  });

  it('"Has open issues" shows only the step with issues, and goes in the URL', async () => {
    const fixture = await open();
    fixture.componentInstance.setWithIssues(true);
    fixture.detectChanges();
    expect(names(fixture)).toEqual(['prints a receipt']);
    expect(router.navigate).toHaveBeenLastCalledWith([], expect.objectContaining({
      replaceUrl: true, queryParams: { q: null, type: null, issues: '1', shared: null } }));
  });

  it('"Shared" shows only the step used by more than one scenario', async () => {
    const fixture = await open();
    fixture.componentInstance.setShared(true);
    expect(names(fixture)).toEqual(['enters the order']);
  });

  it('filters by type and by name, including the name as it reads', async () => {
    const fixture = await open();
    expect(fixture.componentInstance.typeOptions()).toEqual(['Optional', 'Primary']);
    fixture.componentInstance.setType('Optional');
    expect(names(fixture)).toEqual(['Clerk counts the shelf']);
    fixture.componentInstance.setType(null);
    fixture.componentInstance.setQuery('printer');
    expect(names(fixture)).toEqual(['prints a receipt']);
  });

  it('restores its filters from the URL', async () => {
    const fixture = await open({ q: 'order', type: 'Primary', shared: '1' });
    expect(names(fixture)).toEqual(['enters the order']);
    const search = fixture.nativeElement.querySelector('[data-testid="data-table-search"]') as HTMLInputElement;
    expect(search.value).toBe('order');
    expect(fixture.componentInstance.shared()).toBe(true);
  });

  it('opens a step page telling it Back comes here', async () => {
    const fixture = await open();
    rows(fixture)[0].click();
    expect(router.navigate).toHaveBeenCalledWith(['/projects', 'proj1', 'steps', 1],
      { state: { fromStepList: true } });
    const link = rows(fixture)[0].querySelector('a.dt-link') as HTMLAnchorElement;
    expect(link.getAttribute('href')).toBe('/projects/proj1/steps/1');
  });

  it('says when there are no steps, and when none match', async () => {
    listSteps.mockResolvedValue([]);
    const empty = await open();
    expect(empty.nativeElement.textContent).toContain('No steps yet');

    listSteps.mockResolvedValue(STEPS);
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      imports: [StepListComponent],
      providers: [provideNoopAnimations(), provideRouter([]), { provide: ActivatedRoute, useValue: {} },
        { provide: ScenarioService, useValue: { listSteps } }],
    });
    const none = await open({ q: 'zzz' });
    expect(none.nativeElement.textContent).toContain('No steps match');
  });

  it('shows an error when the steps cannot be loaded', async () => {
    listSteps.mockRejectedValue(new Error('boom'));
    const fixture = await open();
    expect(fixture.nativeElement.querySelector('[data-testid="step-list-error"]')!.textContent)
      .toContain('Failed to load steps.');
  });

  it('has no axe violations', async () => {
    const fixture = await open();
    await expectNoAxeViolations(fixture.nativeElement);
  });
});
