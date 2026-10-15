import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter, Router, ActivatedRoute, convertToParamMap } from '@angular/router';
import { UseCaseListComponent } from './use-case-list';
import { UseCaseService } from '../../core/use-case.service';
import { PermissionService } from '../../core/permission.service';

const MOCK_USE_CASES = [
  { id: 30, version: 0, name: 'Place Order', text: 'User places an order.', primaryActorName: 'Customer',
    createdBy: null, scenarioId: null, scenarioName: null, scenarioStepCount: null,
    goals: null, actors: null, stories: null, additionalScenarios: null },
  { id: 31, version: 0, name: 'Cancel Order', text: 'User cancels an order.', primaryActorName: 'Customer',
    createdBy: null, scenarioId: null, scenarioName: null, scenarioStepCount: null,
    goals: null, actors: null, stories: null, additionalScenarios: null }
];

const flush = () => new Promise(r => setTimeout(r, 0));

describe('UseCaseListComponent', () => {
  let useCaseServiceMock: { listUseCases: ReturnType<typeof vi.fn> };
  let permissionServiceMock: { loadForProject: ReturnType<typeof vi.fn>; canEdit: ReturnType<typeof vi.fn> };
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  let fixture: any;
  let comp: UseCaseListComponent;
  let router: Router;

  beforeEach(() => {
    useCaseServiceMock = { listUseCases: vi.fn().mockResolvedValue(MOCK_USE_CASES) };
    permissionServiceMock = {
      loadForProject: vi.fn().mockResolvedValue(undefined),
      canEdit: vi.fn().mockReturnValue(true)
    };

    TestBed.configureTestingModule({
      imports: [UseCaseListComponent],
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ name: 'proj1' }) } } },
        { provide: UseCaseService, useValue: useCaseServiceMock },
        { provide: PermissionService, useValue: permissionServiceMock }
      ]
    });
    fixture = TestBed.createComponent(UseCaseListComponent);
    comp = fixture.componentInstance;
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
  });

  it('useCases() populated from useCaseService.listUseCases on init', async () => {
    fixture.detectChanges();
    await flush();
    expect(useCaseServiceMock.listUseCases).toHaveBeenCalledWith('proj1');
    expect(comp.useCases().length).toBe(2);
    expect(comp.useCases()[0].name).toBe('Place Order');
    expect(comp.loading()).toBe(false);
  });

  it('canEdit() reflects permissionService.canEdit("UseCase")', async () => {
    fixture.detectChanges();
    await flush();
    expect(permissionServiceMock.canEdit).toHaveBeenCalledWith('UseCase');
    expect(comp.canEdit()).toBe(true);
  });

  it('onSelect navigates to use case editor', async () => {
    fixture.detectChanges();
    await flush();
    comp.onSelect({ data: MOCK_USE_CASES[0] });
    expect(router.navigate).toHaveBeenCalledWith(['/projects', 'proj1', 'use-cases', 30]);
  });

  it('onCreate navigates to new use case', async () => {
    fixture.detectChanges();
    await flush();
    comp.onCreate();
    expect(router.navigate).toHaveBeenCalledWith(['/projects', 'proj1', 'use-cases', 'new']);
  });

  it('errorMessage set when listUseCases throws', async () => {
    useCaseServiceMock.listUseCases.mockRejectedValue(new Error('Network error'));
    fixture.detectChanges();
    await flush();
    expect(comp.errorMessage()).toBe('Failed to load use cases.');
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
    expect(link.textContent?.trim()).toBe('Place Order');
    expect(link.getAttribute('href')).toBe('/projects/proj1/use-cases/30');

    // AC #3 - whole-row click still navigates to the same target (kept as a convenience)
    const row = el.querySelector('tbody tr.dt-row') as HTMLElement;
    row.dispatchEvent(new MouseEvent('click', { bubbles: true }));
    expect(router.navigate).toHaveBeenCalledWith(['/projects', 'proj1', 'use-cases', 30]);
  });
});
