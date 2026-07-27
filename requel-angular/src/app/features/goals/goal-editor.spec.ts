import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter, Router, ActivatedRoute, convertToParamMap } from '@angular/router';
import { BehaviorSubject, EMPTY } from 'rxjs';
import { ConfirmationService, MessageService } from 'primeng/api';
import { GoalEditorComponent } from './goal-editor';
import { GoalService } from '../../core/goal.service';
import { TagService } from '../../core/tag.service';
import { CommandService } from '../../core/command.service';
import { ProjectService } from '../../core/project.service';
import { PermissionService } from '../../core/permission.service';
import { EventStreamService } from '../../core/event-stream.service';

const MOCK_GOAL = {
  id: 10, version: 0, name: 'Improve UX', text: 'Make it great.',
  relationsFromThisGoal: [], relationsToThisGoal: [], referencedBy: []
};

const flush = () => new Promise(r => setTimeout(r, 0));

describe('GoalEditorComponent', () => {
  let paramMap$: BehaviorSubject<ReturnType<typeof convertToParamMap>>;
  let goalServiceMock: { getGoal: ReturnType<typeof vi.fn> };
  let commandServiceMock: { execute: ReturnType<typeof vi.fn> };
  let projectServiceMock: { notifyTreeChanged: ReturnType<typeof vi.fn> };
  let permissionServiceMock: { loadForProject: ReturnType<typeof vi.fn>; canEdit: ReturnType<typeof vi.fn>; canDelete: ReturnType<typeof vi.fn> };
  let eventStreamServiceMock: { events$: typeof EMPTY; addSubscription: ReturnType<typeof vi.fn>; removeSubscription: ReturnType<typeof vi.fn> };
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  let fixture: any;
  let comp: GoalEditorComponent;
  let router: Router;

  beforeEach(() => {
    paramMap$ = new BehaviorSubject(convertToParamMap({ name: 'proj1', goalId: 'new' }));

    goalServiceMock = { getGoal: vi.fn().mockResolvedValue(MOCK_GOAL) };
    commandServiceMock = {
      execute: vi.fn().mockResolvedValue({ success: true, entity: MOCK_GOAL })
    };
    projectServiceMock = { notifyTreeChanged: vi.fn() };
    permissionServiceMock = {
      loadForProject: vi.fn().mockResolvedValue(undefined),
      canEdit: vi.fn().mockReturnValue(true),
      canDelete: vi.fn().mockReturnValue(true)
    };
    eventStreamServiceMock = {
      events$: EMPTY,
      addSubscription: vi.fn().mockResolvedValue(undefined),
      removeSubscription: vi.fn().mockResolvedValue(undefined)
    };

    TestBed.configureTestingModule({
      imports: [GoalEditorComponent],
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { paramMap: paramMap$.asObservable() } },
        { provide: GoalService, useValue: goalServiceMock },
        { provide: TagService, useValue: {
            getTagsOnEntity: vi.fn().mockResolvedValue([]),
            getTagsForProject: vi.fn().mockResolvedValue([]),
            getCategories: vi.fn().mockResolvedValue([])
          } },
        { provide: CommandService, useValue: commandServiceMock },
        { provide: ProjectService, useValue: projectServiceMock },
        { provide: PermissionService, useValue: permissionServiceMock },
        { provide: EventStreamService, useValue: eventStreamServiceMock },
        { provide: MessageService, useValue: { add: vi.fn() } }
      ]
    });
    fixture = TestBed.createComponent(GoalEditorComponent);
    comp = fixture.componentInstance;
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
  });

  it('isNew() is true when goalId param is "new"', async () => {
    fixture.detectChanges();
    await flush();
    expect(comp.isNew()).toBe(true);
  });

  it('isNew() is false and goal() loaded when goalId is numeric', async () => {
    paramMap$.next(convertToParamMap({ name: 'proj1', goalId: '10' }));
    fixture.detectChanges();
    await flush();
    expect(comp.isNew()).toBe(false);
    expect(goalServiceMock.getGoal).toHaveBeenCalledWith('proj1', 10);
    expect(comp.goalName()).toBe('Improve UX');
    expect(comp.goal()?.id).toBe(10);
  });

  it('onSave calls commandService.execute("EditGoal") with projectName and name', async () => {
    fixture.detectChanges();
    await flush();
    comp.name = 'New Goal';
    comp.text = 'Details';
    await comp.onSave();
    expect(commandServiceMock.execute).toHaveBeenCalledWith('EditGoal', expect.objectContaining({
      projectName: 'proj1',
      name: 'New Goal',
      text: 'Details'
    }));
  });

  it('onSave sets errorMessage when command returns error', async () => {
    commandServiceMock.execute.mockResolvedValue({ success: false, error: 'Name conflict' });
    comp.name = 'Duplicate';
    await comp.onSave();
    expect(comp.errorMessage()).toBe('Name conflict');
  });

  it('onDelete triggers confirm then calls execute("DeleteGoal")', async () => {
    paramMap$.next(convertToParamMap({ name: 'proj1', goalId: '10' }));
    fixture.detectChanges();
    await flush();

    const cs = fixture.debugElement.injector.get(ConfirmationService);
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    vi.spyOn(cs, 'confirm').mockImplementation((conf: any) => conf.accept?.());

    comp.onDelete();
    await flush();

    expect(commandServiceMock.execute).toHaveBeenCalledWith('DeleteGoal', expect.objectContaining({
      projectName: 'proj1',
      goalId: 10
    }));
    expect(router.navigate).toHaveBeenCalledWith(['/projects', 'proj1', 'goals']);
  });

  it('onCopy triggers confirm then calls execute("CopyGoal")', async () => {
    paramMap$.next(convertToParamMap({ name: 'proj1', goalId: '10' }));
    fixture.detectChanges();
    await flush();

    commandServiceMock.execute.mockResolvedValue({ success: true, entity: { ...MOCK_GOAL, id: 99 } });
    const cs = fixture.debugElement.injector.get(ConfirmationService);
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    vi.spyOn(cs, 'confirm').mockImplementation((conf: any) => conf.accept?.());

    comp.onCopy();
    await flush();

    expect(commandServiceMock.execute).toHaveBeenCalledWith('CopyGoal', expect.objectContaining({
      projectName: 'proj1',
      goalId: 10
    }));
    expect(router.navigate).toHaveBeenCalledWith(['/projects', 'proj1', 'goals', 99]);
  });
});
