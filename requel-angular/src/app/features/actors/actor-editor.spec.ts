import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter, Router, ActivatedRoute, convertToParamMap } from '@angular/router';
import { BehaviorSubject, Subject } from 'rxjs';
import { ConfirmationService, MessageService } from 'primeng/api';
import { ActorEditorComponent } from './actor-editor';
import { ActorService } from '../../core/actor.service';
import { CommandService } from '../../core/command.service';
import { ProjectService } from '../../core/project.service';
import { PermissionService } from '../../core/permission.service';
import { EventStreamService } from '../../core/event-stream.service';

const MOCK_ACTOR = {
  id: 5, version: 0, name: 'Customer', text: 'End user of the system.',
  goals: [{ id: 1, name: 'Purchase item', entityType: 'Goal' }],
  referencedByUseCases: [{ id: 30, name: 'Place order', entityType: 'UseCase' }],
  referencedByStories: [{ id: 40, name: 'Order arrives', entityType: 'Story' }]
};

const flush = () => new Promise(r => setTimeout(r, 0));

describe('ActorEditorComponent', () => {
  let paramMap$: BehaviorSubject<ReturnType<typeof convertToParamMap>>;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  let events$: Subject<any>;
  let actorServiceMock: { getActor: ReturnType<typeof vi.fn> };
  let commandServiceMock: { execute: ReturnType<typeof vi.fn> };
  let projectServiceMock: { notifyTreeChanged: ReturnType<typeof vi.fn> };
  let messageServiceMock: { add: ReturnType<typeof vi.fn> };
  let permissionServiceMock: { loadForProject: ReturnType<typeof vi.fn>; canEdit: ReturnType<typeof vi.fn>; canDelete: ReturnType<typeof vi.fn> };
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  let eventStreamServiceMock: { events$: Subject<any>; addSubscription: ReturnType<typeof vi.fn>; removeSubscription: ReturnType<typeof vi.fn> };
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  let fixture: any;
  let comp: ActorEditorComponent;
  let router: Router;

  beforeEach(() => {
    paramMap$ = new BehaviorSubject(convertToParamMap({ name: 'proj1', actorId: 'new' }));
    events$ = new Subject();

    actorServiceMock = { getActor: vi.fn().mockResolvedValue(MOCK_ACTOR) };
    commandServiceMock = {
      execute: vi.fn().mockResolvedValue({ success: true, entity: MOCK_ACTOR })
    };
    projectServiceMock = { notifyTreeChanged: vi.fn() };
    messageServiceMock = { add: vi.fn() };
    permissionServiceMock = {
      loadForProject: vi.fn().mockResolvedValue(undefined),
      canEdit: vi.fn().mockReturnValue(true),
      canDelete: vi.fn().mockReturnValue(true)
    };
    eventStreamServiceMock = {
      events$,
      addSubscription: vi.fn().mockResolvedValue(undefined),
      removeSubscription: vi.fn().mockResolvedValue(undefined)
    };

    TestBed.configureTestingModule({
      imports: [ActorEditorComponent],
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { paramMap: paramMap$.asObservable() } },
        { provide: ActorService, useValue: actorServiceMock },
        { provide: CommandService, useValue: commandServiceMock },
        { provide: ProjectService, useValue: projectServiceMock },
        { provide: PermissionService, useValue: permissionServiceMock },
        { provide: EventStreamService, useValue: eventStreamServiceMock },
        { provide: MessageService, useValue: messageServiceMock }
      ]
    });
    fixture = TestBed.createComponent(ActorEditorComponent);
    comp = fixture.componentInstance;
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
  });

  it('isNew() is true when actorId param is "new"', async () => {
    fixture.detectChanges();
    await flush();
    expect(comp.isNew()).toBe(true);
  });

  it('loads actor: actorName(), actor(), and goals() populated', async () => {
    paramMap$.next(convertToParamMap({ name: 'proj1', actorId: '5' }));
    fixture.detectChanges();
    await flush();
    expect(actorServiceMock.getActor).toHaveBeenCalledWith('proj1', 5);
    expect(comp.actorName()).toBe('Customer');
    expect(comp.actor()?.id).toBe(5);
    expect(comp.goals().length).toBe(1);
    expect(comp.goals()[0].name).toBe('Purchase item');
  });

  it('trackChanges() sets hasChanges() when name differs from original', async () => {
    paramMap$.next(convertToParamMap({ name: 'proj1', actorId: '5' }));
    fixture.detectChanges();
    await flush();
    expect(comp.hasChanges()).toBe(false);
    comp.name = 'Modified Actor';
    comp.trackChanges();
    expect(comp.hasChanges()).toBe(true);
  });

  it('onSave calls commandService.execute("EditActor") with actor fields', async () => {
    fixture.detectChanges();
    await flush();
    comp.name = 'New Actor';
    comp.text = 'Description';
    await comp.onSave();
    expect(commandServiceMock.execute).toHaveBeenCalledWith('EditActor', expect.objectContaining({
      projectName: 'proj1',
      name: 'New Actor',
      description: 'Description'
    }));
  });

  it('onDelete triggers confirm then calls execute("DeleteActor")', async () => {
    paramMap$.next(convertToParamMap({ name: 'proj1', actorId: '5' }));
    fixture.detectChanges();
    await flush();

    const cs = fixture.debugElement.injector.get(ConfirmationService);
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    vi.spyOn(cs, 'confirm').mockImplementation((conf: any) => conf.accept?.());

    comp.onDelete();
    await flush();

    expect(commandServiceMock.execute).toHaveBeenCalledWith('DeleteActor', expect.objectContaining({
      projectName: 'proj1',
      actorId: 5
    }));
    expect(router.navigate).toHaveBeenCalledWith(['/projects', 'proj1', 'actors']);
  });

  it('loads existing actor: referencedByUseCases() and referencedByStories() populated', async () => {
    paramMap$.next(convertToParamMap({ name: 'proj1', actorId: '5' }));
    fixture.detectChanges();
    await flush();
    expect(comp.referencedByUseCases().length).toBe(1);
    expect(comp.referencedByUseCases()[0].name).toBe('Place order');
    expect(comp.referencedByStories().length).toBe(1);
    expect(comp.referencedByStories()[0].name).toBe('Order arrives');
  });

  it('onSave (existing actor) updates originalName/version and shows success message', async () => {
    paramMap$.next(convertToParamMap({ name: 'proj1', actorId: '5' }));
    fixture.detectChanges();
    await flush();

    commandServiceMock.execute.mockResolvedValue({
      success: true,
      entity: { ...MOCK_ACTOR, name: 'Customer Renamed', version: 1 }
    });
    comp.name = 'Customer Renamed';
    comp.text = 'Updated description';
    await comp.onSave();

    expect(commandServiceMock.execute).toHaveBeenCalledWith('EditActor', expect.objectContaining({
      actorId: 5,
      name: 'Customer Renamed',
      description: 'Updated description'
    }));
    expect(comp.actorName()).toBe('Customer Renamed');
    expect(comp.version).toBe(1);
    expect(comp.hasChanges()).toBe(false);
    expect(messageServiceMock.add).toHaveBeenCalledWith(expect.objectContaining({
      severity: 'success', summary: 'Saved'
    }));
    // The router should NOT navigate when saving an existing actor.
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('onSave sets errorMessage when command returns failure', async () => {
    fixture.detectChanges();
    await flush();
    commandServiceMock.execute.mockResolvedValue({ success: false, error: 'Name conflict' });
    comp.name = 'Duplicate';
    await comp.onSave();
    expect(comp.errorMessage()).toBe('Name conflict');
  });

  it('onSave catch sets generic error when command throws', async () => {
    fixture.detectChanges();
    await flush();
    commandServiceMock.execute.mockRejectedValue(new Error('network down'));
    comp.name = 'Anything';
    await comp.onSave();
    expect(comp.errorMessage()).toBe('Save failed.');
  });

  it('onCopy triggers confirm then calls execute("CopyActor") and navigates to copy', async () => {
    paramMap$.next(convertToParamMap({ name: 'proj1', actorId: '5' }));
    fixture.detectChanges();
    await flush();

    commandServiceMock.execute.mockResolvedValue({
      success: true, entity: { ...MOCK_ACTOR, id: 99, name: 'Copy of Customer' }
    });
    const cs = fixture.debugElement.injector.get(ConfirmationService);
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    vi.spyOn(cs, 'confirm').mockImplementation((conf: any) => conf.accept?.());

    comp.onCopy();
    await flush();

    expect(commandServiceMock.execute).toHaveBeenCalledWith('CopyActor', expect.objectContaining({
      projectName: 'proj1', actorId: 5
    }));
    expect(projectServiceMock.notifyTreeChanged).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/projects', 'proj1', 'actors', 99]);
  });

  it('onCopy sets errorMessage when copy fails', async () => {
    paramMap$.next(convertToParamMap({ name: 'proj1', actorId: '5' }));
    fixture.detectChanges();
    await flush();
    commandServiceMock.execute.mockResolvedValue({ success: false, error: 'Cannot copy' });
    const cs = fixture.debugElement.injector.get(ConfirmationService);
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    vi.spyOn(cs, 'confirm').mockImplementation((conf: any) => conf.accept?.());
    comp.onCopy();
    await flush();
    expect(comp.errorMessage()).toBe('Cannot copy');
  });

  it('onDelete sets errorMessage when delete fails', async () => {
    paramMap$.next(convertToParamMap({ name: 'proj1', actorId: '5' }));
    fixture.detectChanges();
    await flush();
    commandServiceMock.execute.mockResolvedValue({ success: false, error: 'In use' });
    const cs = fixture.debugElement.injector.get(ConfirmationService);
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    vi.spyOn(cs, 'confirm').mockImplementation((conf: any) => conf.accept?.());
    comp.onDelete();
    await flush();
    expect(comp.errorMessage()).toBe('In use');
  });

  it('onGoalSelected calls AddGoalToGoalContainer and appends to goals()', async () => {
    paramMap$.next(convertToParamMap({ name: 'proj1', actorId: '5' }));
    fixture.detectChanges();
    await flush();
    commandServiceMock.execute.mockResolvedValue({ success: true });
    await comp.onGoalSelected({ id: 7, name: 'Avoid late fees', entityType: 'Goal' });
    expect(commandServiceMock.execute).toHaveBeenCalledWith('AddGoalToGoalContainer', expect.objectContaining({
      projectName: 'proj1',
      goalContainerId: 5,
      goalId: 7,
      containerType: 'Actor'
    }));
    expect(comp.goals().some(g => g.id === 7)).toBe(true);
    expect(messageServiceMock.add).toHaveBeenCalledWith(expect.objectContaining({
      severity: 'success', summary: 'Goal added'
    }));
  });

  it('onGoalSelected sets errorMessage when add fails', async () => {
    paramMap$.next(convertToParamMap({ name: 'proj1', actorId: '5' }));
    fixture.detectChanges();
    await flush();
    commandServiceMock.execute.mockResolvedValue({ success: false, error: 'Already linked' });
    await comp.onGoalSelected({ id: 7, name: 'Avoid late fees', entityType: 'Goal' });
    expect(comp.errorMessage()).toBe('Already linked');
  });

  it('onRemoveGoal calls RemoveGoalFromGoalContainer and removes from goals()', async () => {
    paramMap$.next(convertToParamMap({ name: 'proj1', actorId: '5' }));
    fixture.detectChanges();
    await flush();
    commandServiceMock.execute.mockResolvedValue({ success: true });
    await comp.onRemoveGoal({ id: 1, name: 'Purchase item', entityType: 'Goal' });
    expect(commandServiceMock.execute).toHaveBeenCalledWith('RemoveGoalFromGoalContainer', expect.objectContaining({
      goalId: 1,
      containerType: 'Actor'
    }));
    expect(comp.goals().some(g => g.id === 1)).toBe(false);
    expect(messageServiceMock.add).toHaveBeenCalledWith(expect.objectContaining({
      severity: 'info', summary: 'Goal removed'
    }));
  });

  it('onRemoveGoal sets errorMessage when remove fails', async () => {
    paramMap$.next(convertToParamMap({ name: 'proj1', actorId: '5' }));
    fixture.detectChanges();
    await flush();
    commandServiceMock.execute.mockResolvedValue({ success: false, error: 'Not allowed' });
    await comp.onRemoveGoal({ id: 1, name: 'Purchase item', entityType: 'Goal' });
    expect(comp.errorMessage()).toBe('Not allowed');
  });

  it('onGoalClick navigates to the goal editor', () => {
    comp.projectName = 'proj1';
    comp.onGoalClick(42);
    expect(router.navigate).toHaveBeenCalledWith(['/projects', 'proj1', 'goals', 42]);
  });

  it('navigate(type, id) navigates to that entity editor', () => {
    comp.projectName = 'proj1';
    comp.navigate('use-cases', 30);
    expect(router.navigate).toHaveBeenCalledWith(['/projects', 'proj1', 'use-cases', 30]);
    comp.navigate('stories', 40);
    expect(router.navigate).toHaveBeenCalledWith(['/projects', 'proj1', 'stories', 40]);
  });

  it('onBack navigates back to actor list', () => {
    comp.projectName = 'proj1';
    comp.onBack();
    expect(router.navigate).toHaveBeenCalledWith(['/projects', 'proj1', 'actors']);
  });

  it('hasUnsavedChanges() returns hasChanges() value', () => {
    expect(comp.hasUnsavedChanges()).toBe(false);
    comp.hasChanges.set(true);
    expect(comp.hasUnsavedChanges()).toBe(true);
  });

  it('loadActor sets errorMessage when getActor throws', async () => {
    actorServiceMock.getActor.mockRejectedValue(new Error('not found'));
    paramMap$.next(convertToParamMap({ name: 'proj1', actorId: '999' }));
    fixture.detectChanges();
    await flush();
    expect(comp.errorMessage()).toBe('Failed to load actor.');
  });

  it('SSE event for matching actor triggers loadActor reload', async () => {
    paramMap$.next(convertToParamMap({ name: 'proj1', actorId: '5' }));
    fixture.detectChanges();
    await flush();
    expect(eventStreamServiceMock.addSubscription).toHaveBeenCalledWith('Actor', 5);
    expect(actorServiceMock.getActor).toHaveBeenCalledTimes(1);

    actorServiceMock.getActor.mockResolvedValue({
      ...MOCK_ACTOR, name: 'Customer Updated By SSE'
    });
    events$.next({ targetType: 'Actor', targetId: 5 });
    await flush();

    expect(actorServiceMock.getActor).toHaveBeenCalledTimes(2);
    expect(comp.actorName()).toBe('Customer Updated By SSE');
  });

  it('SSE event preserves unsaved edits (hasChanges() short-circuits reload)', async () => {
    paramMap$.next(convertToParamMap({ name: 'proj1', actorId: '5' }));
    fixture.detectChanges();
    await flush();

    comp.name = 'Editing in progress';
    comp.trackChanges();
    expect(comp.hasChanges()).toBe(true);

    events$.next({ targetType: 'Actor', targetId: 5 });
    await flush();

    // The reload was attempted (getActor called) but the result was discarded —
    // so the component's name field still holds the user's unsaved edit.
    expect(comp.name).toBe('Editing in progress');
    expect(comp.actorName()).toBe('Customer'); // unchanged from initial load
  });

  it('SSE event for unrelated entity does NOT trigger reload', async () => {
    paramMap$.next(convertToParamMap({ name: 'proj1', actorId: '5' }));
    fixture.detectChanges();
    await flush();
    actorServiceMock.getActor.mockClear();

    events$.next({ targetType: 'Goal', targetId: 5 });
    events$.next({ targetType: 'Actor', targetId: 99 });
    await flush();

    expect(actorServiceMock.getActor).not.toHaveBeenCalled();
  });

  it('ngOnDestroy unsubscribes and removes the SSE subscription', async () => {
    paramMap$.next(convertToParamMap({ name: 'proj1', actorId: '5' }));
    fixture.detectChanges();
    await flush();
    fixture.destroy();
    expect(eventStreamServiceMock.removeSubscription).toHaveBeenCalledWith('Actor', 5);
  });
});
