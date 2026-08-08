import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter, Router, ActivatedRoute, convertToParamMap } from '@angular/router';
import { Location } from '@angular/common';
import { BehaviorSubject, EMPTY } from 'rxjs';
import { ConfirmationService, MessageService } from 'primeng/api';
import { UseCaseEditorComponent } from './use-case-editor';
import { UseCaseService } from '../../core/use-case.service';
import { ActorService } from '../../core/actor.service';
import { ScenarioService } from '../../core/scenario.service';
import { CommandService } from '../../core/command.service';
import { ProjectService } from '../../core/project.service';
import { PermissionService } from '../../core/permission.service';
import { EventStreamService } from '../../core/event-stream.service';

const MOCK_ACTORS = [
  { id: 1, version: 0, name: 'Customer', text: null, goals: null, referencedByUseCases: null, referencedByStories: null }
];

const MOCK_USE_CASE = {
  id: 30, version: 0, name: 'Place Order', text: 'User places an order.',
  primaryActorName: 'Customer', scenarioId: null,
  goals: [{ id: 1, name: 'Buy product', entityType: 'Goal' }],
  stories: [{ id: 5, name: 'Happy path', entityType: 'Story' }],
  actors: [],
  additionalScenarios: []
};

const flush = () => new Promise(r => setTimeout(r, 0));

describe('UseCaseEditorComponent', () => {
  let paramMap$: BehaviorSubject<ReturnType<typeof convertToParamMap>>;
  let useCaseServiceMock: { getUseCase: ReturnType<typeof vi.fn> };
  let actorServiceMock: { listActors: ReturnType<typeof vi.fn> };
  let scenarioServiceMock: { getScenario: ReturnType<typeof vi.fn> };
  let commandServiceMock: { execute: ReturnType<typeof vi.fn> };
  let permissionServiceMock: { loadForProject: ReturnType<typeof vi.fn>; canEdit: ReturnType<typeof vi.fn>; canDelete: ReturnType<typeof vi.fn> };
  let eventStreamServiceMock: { events$: typeof EMPTY; addSubscription: ReturnType<typeof vi.fn>; removeSubscription: ReturnType<typeof vi.fn> };
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  let fixture: any;
  let comp: UseCaseEditorComponent;
  let router: Router;

  beforeEach(() => {
    paramMap$ = new BehaviorSubject(convertToParamMap({ name: 'proj1', useCaseId: 'new' }));

    useCaseServiceMock = { getUseCase: vi.fn().mockResolvedValue(MOCK_USE_CASE) };
    actorServiceMock = { listActors: vi.fn().mockResolvedValue(MOCK_ACTORS) };
    scenarioServiceMock = { getScenario: vi.fn().mockResolvedValue(null) };
    commandServiceMock = {
      execute: vi.fn().mockResolvedValue({ success: true, entity: MOCK_USE_CASE })
    };
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
      imports: [UseCaseEditorComponent],
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { paramMap: paramMap$.asObservable() } },
        { provide: Location, useValue: { back: vi.fn() } },
        { provide: UseCaseService, useValue: useCaseServiceMock },
        { provide: ActorService, useValue: actorServiceMock },
        { provide: ScenarioService, useValue: scenarioServiceMock },
        { provide: CommandService, useValue: commandServiceMock },
        { provide: ProjectService, useValue: { notifyTreeChanged: vi.fn() } },
        { provide: PermissionService, useValue: permissionServiceMock },
        { provide: EventStreamService, useValue: eventStreamServiceMock },
        { provide: MessageService, useValue: { add: vi.fn() } }
      ]
    });
    fixture = TestBed.createComponent(UseCaseEditorComponent);
    comp = fixture.componentInstance;
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
  });

  it('isNew() is true when useCaseId param is "new"', async () => {
    fixture.detectChanges();
    await flush();
    expect(comp.isNew()).toBe(true);
  });

  it('loads use case: useCaseName(), goals(), stories() populated', async () => {
    paramMap$.next(convertToParamMap({ name: 'proj1', useCaseId: '30' }));
    fixture.detectChanges();
    await flush();
    expect(useCaseServiceMock.getUseCase).toHaveBeenCalledWith('proj1', 30);
    expect(comp.useCaseName()).toBe('Place Order');
    expect(comp.goals().length).toBe(1);
    expect(comp.stories().length).toBe(1);
  });

  it('actorOptions populated from actorService.listActors', async () => {
    fixture.detectChanges();
    await flush();
    expect(actorServiceMock.listActors).toHaveBeenCalledWith('proj1');
    expect(comp.actorOptions().length).toBe(1);
    expect(comp.actorOptions()[0].label).toBe('Customer');
  });

  it('onSave calls commandService.execute("EditUseCase") with fields', async () => {
    fixture.detectChanges();
    await flush();
    comp.detailsForm.patchValue({ name: 'New Use Case', text: 'Description' });
    comp.detailsForm.markAsDirty();
    await comp.onSave();
    expect(commandServiceMock.execute).toHaveBeenCalledWith('EditUseCase', expect.objectContaining({
      projectName: 'proj1',
      name: 'New Use Case',
      text: 'Description'
    }));
  });

  it('onSave sets errorMessage when command fails', async () => {
    commandServiceMock.execute.mockResolvedValue({ success: false, error: 'Conflict' });
    comp.detailsForm.controls.name.setValue('Test');
    comp.detailsForm.markAsDirty();
    await comp.onSave();
    expect(comp.errorMessage()).toBe('Conflict');
    expect(comp.saving()).toBe(false);
  });

  it('canEdit() and canDelete() set from permissionService on init', async () => {
    fixture.detectChanges();
    await flush();
    expect(permissionServiceMock.canEdit).toHaveBeenCalledWith('UseCase');
    expect(permissionServiceMock.canDelete).toHaveBeenCalledWith('UseCase');
    expect(comp.canEdit()).toBe(true);
    expect(comp.canDelete()).toBe(true);
  });

  it('additionalScenarios() and actors() populated on load', async () => {
    const fullUseCase = {
      ...MOCK_USE_CASE,
      actors: [{ id: 1, version: 0, name: 'Customer', text: null, createdBy: null,
        goals: null, referencedByUseCases: null, referencedByStories: null }],
      additionalScenarios: [{ id: 99, version: 0, name: 'Exception flow', text: null,
        scenarioType: 'Exception', createdBy: null, steps: null }]
    };
    useCaseServiceMock.getUseCase.mockResolvedValue(fullUseCase);
    paramMap$.next(convertToParamMap({ name: 'proj1', useCaseId: '30' }));
    fixture.detectChanges();
    await flush();
    expect(comp.actors().length).toBe(1);
    expect(comp.additionalScenarios().length).toBe(1);
    expect(comp.additionalScenarios()[0].name).toBe('Exception flow');
  });

  // #173: trackChanges()/hasChanges() are gone; the form owns dirtiness.
  it('hasUnsavedChanges() follows form.dirty', async () => {
    paramMap$.next(convertToParamMap({ name: 'proj1', useCaseId: '30' }));
    fixture.detectChanges();
    await flush();
    expect(comp.hasUnsavedChanges()).toBe(false);
    comp.detailsForm.controls.name.setValue('Different Name');
    comp.detailsForm.controls.name.markAsDirty();
    expect(comp.hasUnsavedChanges()).toBe(true);
  });

  // #173 required test (§10.3). refreshCollections() refetched every collection but never the
  // version, so an association left it stale and the next save 409'd. All eight association
  // commands on this editor bump the use case, so this is the editor where it mattered most.
  describe('wizard version contract (#173)', () => {
    it('re-reads version after an association, so a later save does not 409', async () => {
      paramMap$.next(convertToParamMap({ name: 'proj1', useCaseId: '30' }));
      fixture.detectChanges();
      await flush();

      commandServiceMock.execute.mockResolvedValue({ success: true, entity: null });
      useCaseServiceMock.getUseCase.mockResolvedValue({ ...MOCK_USE_CASE, version: 5 });

      await comp.addGoal({ id: 7, name: 'Avoid late fees', entityType: 'Goal' });

      commandServiceMock.execute.mockClear();
      commandServiceMock.execute.mockResolvedValue({
        success: true, entity: { ...MOCK_USE_CASE, version: 6 }
      });
      comp.detailsForm.controls.name.setValue('Renamed');
      comp.detailsForm.markAsDirty();
      await comp.onSave();

      const edit = commandServiceMock.execute.mock.calls.find(c => c[0] === 'EditUseCase');
      expect(edit![1].version).toBe(5);
    });

    it('a non-details step just advances - it issues no command of its own', async () => {
      fixture.detectChanges();
      await flush();
      commandServiceMock.execute.mockClear();

      for (const key of ['scenarios', 'goals-stories', 'actors']) {
        const request = { step: { key }, complete: vi.fn(), fail: vi.fn() };
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        await comp.onStepCommit(request as any);
        expect(request.complete).toHaveBeenCalled();
      }
      expect(commandServiceMock.execute).not.toHaveBeenCalled();
    });

    it('step 1 captures the use case without navigating away', async () => {
      fixture.detectChanges();
      await flush();
      const navigate = vi.spyOn(router, 'navigate').mockResolvedValue(true);
      navigate.mockClear();

      commandServiceMock.execute.mockResolvedValue({
        success: true, entity: { ...MOCK_USE_CASE, id: 30, version: 1 }
      });
      comp.detailsForm.controls.name.setValue('New Use Case');

      const request = { step: { key: 'details' }, complete: vi.fn(), fail: vi.fn() };
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      await comp.onStepCommit(request as any);

      expect(request.complete).toHaveBeenCalled();
      expect(navigate).not.toHaveBeenCalled();
      expect(comp.useCaseId).toBe(30);
    });
  });

  it('addGoal calls AddGoalToGoalContainer and refreshes collections', async () => {
    paramMap$.next(convertToParamMap({ name: 'proj1', useCaseId: '30' }));
    fixture.detectChanges();
    await flush();
    const callsBefore = useCaseServiceMock.getUseCase.mock.calls.length;
    await comp.addGoal({ id: 10, name: 'New Goal', entityType: 'Goal' });
    expect(commandServiceMock.execute).toHaveBeenCalledWith('AddGoalToGoalContainer', expect.objectContaining({
      projectName: 'proj1',
      goalContainerId: 30,
      goalId: 10,
      containerType: 'UseCase'
    }));
    expect(useCaseServiceMock.getUseCase.mock.calls.length).toBeGreaterThan(callsBefore);
  });

  it('removeGoal calls RemoveGoalFromGoalContainer', async () => {
    paramMap$.next(convertToParamMap({ name: 'proj1', useCaseId: '30' }));
    fixture.detectChanges();
    await flush();
    const goal = { id: 1, version: 0, name: 'Buy product', text: 'text', createdBy: null,
      relationsFromThisGoal: null, relationsToThisGoal: null, referencedBy: null };
    await comp.removeGoal(goal);
    expect(commandServiceMock.execute).toHaveBeenCalledWith('RemoveGoalFromGoalContainer', expect.objectContaining({
      goalContainerId: 30,
      goalId: 1,
      containerType: 'UseCase'
    }));
  });

  it('addStory calls AddStoryToStoryContainer', async () => {
    paramMap$.next(convertToParamMap({ name: 'proj1', useCaseId: '30' }));
    fixture.detectChanges();
    await flush();
    await comp.addStory({ id: 5, name: 'Happy path', entityType: 'Story' });
    expect(commandServiceMock.execute).toHaveBeenCalledWith('AddStoryToStoryContainer', expect.objectContaining({
      projectName: 'proj1',
      storyContainerId: 30,
      storyId: 5
    }));
  });

  it('removeStory calls RemoveStoryFromStoryContainer', async () => {
    paramMap$.next(convertToParamMap({ name: 'proj1', useCaseId: '30' }));
    fixture.detectChanges();
    await flush();
    const story = { id: 5, version: 0, name: 'Happy path', text: 'text',
      storyType: 'Success' as const, createdBy: null, primaryActorName: null, goals: null, actors: null };
    await comp.removeStory(story);
    expect(commandServiceMock.execute).toHaveBeenCalledWith('RemoveStoryFromStoryContainer', expect.objectContaining({
      storyContainerId: 30,
      storyId: 5
    }));
  });

  it('onCopy calls CopyUseCase and navigates to copy', async () => {
    paramMap$.next(convertToParamMap({ name: 'proj1', useCaseId: '30' }));
    fixture.detectChanges();
    await flush();
    const copy = { ...MOCK_USE_CASE, id: 88 };
    commandServiceMock.execute.mockResolvedValue({ success: true, entity: copy });
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const cs = fixture.debugElement.injector.get(ConfirmationService);
    vi.spyOn(cs, 'confirm').mockImplementation((conf: any) => conf.accept?.());
    comp.onCopy();
    await flush();
    expect(commandServiceMock.execute).toHaveBeenCalledWith('CopyUseCase',
      expect.objectContaining({ useCaseId: 30 }));
    expect(router.navigate).toHaveBeenCalledWith(['/projects', 'proj1', 'use-cases', 88]);
  });

  it('onDelete calls DeleteUseCase and navigates to list', async () => {
    paramMap$.next(convertToParamMap({ name: 'proj1', useCaseId: '30' }));
    fixture.detectChanges();
    await flush();
    commandServiceMock.execute.mockResolvedValue({ success: true });
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const cs = fixture.debugElement.injector.get(ConfirmationService);
    vi.spyOn(cs, 'confirm').mockImplementation((conf: any) => conf.accept?.());
    comp.onDelete();
    await flush();
    expect(commandServiceMock.execute).toHaveBeenCalledWith('DeleteUseCase',
      expect.objectContaining({ useCaseId: 30 }));
    expect(router.navigate).toHaveBeenCalledWith(['/projects', 'proj1', 'use-cases']);
  });

  it('navigateTo routes to correct entity path', () => {
    comp.projectName = 'proj1';
    comp.navigateTo('goals', 42);
    expect(router.navigate).toHaveBeenCalledWith(['/projects', 'proj1', 'goals', 42]);
  });
});
