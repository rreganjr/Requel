import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter, Router, ActivatedRoute, convertToParamMap } from '@angular/router';
import { BehaviorSubject } from 'rxjs';
import { ScenarioListComponent } from './scenario-list';
import { ScenarioService } from '../../core/scenario.service';
import { PermissionService } from '../../core/permission.service';

const MOCK_SCENARIOS = [
  { id: 5, version: 0, name: 'Login Flow', text: 'User logs in.', scenarioType: 'Primary',
    createdBy: null, steps: null, referencedBy: null },
  { id: 6, version: 0, name: 'Logout Flow', text: 'User logs out.', scenarioType: 'Primary',
    createdBy: null, steps: null, referencedBy: null }
];

const flush = () => new Promise(r => setTimeout(r, 0));

describe('ScenarioListComponent', () => {
  let paramMap$: BehaviorSubject<ReturnType<typeof convertToParamMap>>;
  let scenarioServiceMock: { listScenarios: ReturnType<typeof vi.fn> };
  let permissionServiceMock: { loadForProject: ReturnType<typeof vi.fn>; canEdit: ReturnType<typeof vi.fn> };
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  let fixture: any;
  let comp: ScenarioListComponent;
  let router: Router;

  beforeEach(() => {
    paramMap$ = new BehaviorSubject(convertToParamMap({ name: 'proj1' }));

    scenarioServiceMock = { listScenarios: vi.fn().mockResolvedValue(MOCK_SCENARIOS) };
    permissionServiceMock = {
      loadForProject: vi.fn().mockResolvedValue(undefined),
      canEdit: vi.fn().mockReturnValue(true)
    };

    TestBed.configureTestingModule({
      imports: [ScenarioListComponent],
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { paramMap: paramMap$.asObservable() } },
        { provide: ScenarioService, useValue: scenarioServiceMock },
        { provide: PermissionService, useValue: permissionServiceMock }
      ]
    });
    fixture = TestBed.createComponent(ScenarioListComponent);
    comp = fixture.componentInstance;
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
  });

  it('scenarios() populated from scenarioService.listScenarios on init', async () => {
    fixture.detectChanges();
    await flush();
    expect(scenarioServiceMock.listScenarios).toHaveBeenCalledWith('proj1');
    expect(comp.scenarios().length).toBe(2);
    expect(comp.scenarios()[0].name).toBe('Login Flow');
    expect(comp.loading()).toBe(false);
  });

  it('canEdit() reflects permissionService.canEdit("Scenario")', async () => {
    fixture.detectChanges();
    await flush();
    expect(permissionServiceMock.canEdit).toHaveBeenCalledWith('Scenario');
    expect(comp.canEdit()).toBe(true);
  });

  it('onSelect navigates to scenario editor', async () => {
    fixture.detectChanges();
    await flush();
    comp.onSelect({ data: MOCK_SCENARIOS[0] });
    expect(router.navigate).toHaveBeenCalledWith(['/projects', 'proj1', 'scenarios', 5]);
  });

  it('onNew navigates to new scenario', async () => {
    fixture.detectChanges();
    await flush();
    comp.onNew();
    expect(router.navigate).toHaveBeenCalledWith(['/projects', 'proj1', 'scenarios', 'new']);
  });

  it('errorMessage set when listScenarios throws', async () => {
    scenarioServiceMock.listScenarios.mockRejectedValue(new Error('Network error'));
    fixture.detectChanges();
    await flush();
    expect(comp.errorMessage()).toBe('Failed to load scenarios.');
    expect(comp.loading()).toBe(false);
  });

  it('renders the name as a real link and still navigates on row click (issue #129)', async () => {
    fixture.detectChanges();
    await flush();
    fixture.detectChanges();
    const el: HTMLElement = fixture.nativeElement;

    // AC #2 - the name cell is a real link to the detail route
    const link = el.querySelector('tbody tr.dt-row a.dt-link') as HTMLAnchorElement;
    expect(link).not.toBeNull();
    expect(link.textContent?.trim()).toBe('Login Flow');
    expect(link.getAttribute('href')).toBe('/projects/proj1/scenarios/5');

    // AC #3 - whole-row click still navigates to the same target (kept as a convenience)
    const row = el.querySelector('tbody tr.dt-row') as HTMLElement;
    row.dispatchEvent(new MouseEvent('click', { bubbles: true }));
    expect(router.navigate).toHaveBeenCalledWith(['/projects', 'proj1', 'scenarios', 5]);
  });
});
