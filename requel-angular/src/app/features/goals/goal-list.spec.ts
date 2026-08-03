import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter, Router, ActivatedRoute, convertToParamMap } from '@angular/router';
import { BehaviorSubject } from 'rxjs';
import { GoalListComponent } from './goal-list';
import { GoalService } from '../../core/goal.service';
import { TagService } from '../../core/tag.service';
import { PermissionService } from '../../core/permission.service';

const MOCK_GOALS = [
  { id: 1, version: 0, name: 'Improve login', text: 'Make login faster.', createdBy: 'alice',
    relationsFromThisGoal: null, relationsToThisGoal: null, referencedBy: null },
  { id: 2, version: 0, name: 'Add reporting', text: 'Support PDF reports.', createdBy: 'bob',
    relationsFromThisGoal: null, relationsToThisGoal: null, referencedBy: null }
];

const flush = () => new Promise(r => setTimeout(r, 0));

describe('GoalListComponent', () => {
  let paramMap$: BehaviorSubject<ReturnType<typeof convertToParamMap>>;
  let goalServiceMock: { listGoals: ReturnType<typeof vi.fn> };
  let tagServiceMock: { getTagsForProject: ReturnType<typeof vi.fn>; getEntitiesWithTag: ReturnType<typeof vi.fn> };
  let permissionServiceMock: { loadForProject: ReturnType<typeof vi.fn>; canEdit: ReturnType<typeof vi.fn> };
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  let fixture: any;
  let comp: GoalListComponent;
  let router: Router;

  beforeEach(() => {
    paramMap$ = new BehaviorSubject(convertToParamMap({ name: 'proj1' }));

    goalServiceMock = { listGoals: vi.fn().mockResolvedValue(MOCK_GOALS) };
    tagServiceMock = {
      getTagsForProject: vi.fn().mockResolvedValue([]),
      getEntitiesWithTag: vi.fn().mockResolvedValue([])
    };
    permissionServiceMock = {
      loadForProject: vi.fn().mockResolvedValue(undefined),
      canEdit: vi.fn().mockReturnValue(true)
    };

    TestBed.configureTestingModule({
      imports: [GoalListComponent],
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { paramMap: paramMap$.asObservable() } },
        { provide: GoalService, useValue: goalServiceMock },
        { provide: TagService, useValue: tagServiceMock },
        { provide: PermissionService, useValue: permissionServiceMock }
      ]
    });
    fixture = TestBed.createComponent(GoalListComponent);
    comp = fixture.componentInstance;
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
  });

  it('goals() populated from goalService.listGoals on init', async () => {
    fixture.detectChanges();
    await flush();
    expect(goalServiceMock.listGoals).toHaveBeenCalledWith('proj1');
    expect(comp.goals().length).toBe(2);
    expect(comp.goals()[0].name).toBe('Improve login');
    expect(comp.loading()).toBe(false);
  });

  it('canEdit() reflects permissionService.canEdit("Goal")', async () => {
    fixture.detectChanges();
    await flush();
    expect(permissionServiceMock.canEdit).toHaveBeenCalledWith('Goal');
    expect(comp.canEdit()).toBe(true);
  });

  it('openGoal navigates to goal editor', async () => {
    fixture.detectChanges();
    await flush();
    comp.openGoal(MOCK_GOALS[0]);
    expect(router.navigate).toHaveBeenCalledWith(['/projects', 'proj1', 'goals', 1]);
  });

  it('onNewGoal navigates to new goal', async () => {
    fixture.detectChanges();
    await flush();
    comp.onNewGoal();
    expect(router.navigate).toHaveBeenCalledWith(['/projects', 'proj1', 'goals', 'new']);
  });

  it('errorMessage set when listGoals throws', async () => {
    goalServiceMock.listGoals.mockRejectedValue(new Error('Network error'));
    fixture.detectChanges();
    await flush();
    expect(comp.errorMessage()).toBe('Failed to load goals.');
    expect(comp.loading()).toBe(false);
  });
});
