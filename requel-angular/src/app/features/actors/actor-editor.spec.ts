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

  // #173: trackChanges()/hasChanges() are gone; the form owns dirtiness now.
  it('hasUnsavedChanges() follows form.dirty', async () => {
    paramMap$.next(convertToParamMap({ name: 'proj1', actorId: '5' }));
    fixture.detectChanges();
    await flush();
    expect(comp.hasUnsavedChanges()).toBe(false);
    comp.detailsForm.controls.name.setValue('Modified Actor');
    comp.detailsForm.controls.name.markAsDirty();
    expect(comp.hasUnsavedChanges()).toBe(true);
  });

  it('onSave calls commandService.execute("EditActor") with actor fields', async () => {
    fixture.detectChanges();
    await flush();
    comp.detailsForm.setValue({ name: 'New Actor', text: 'Description' });
    comp.detailsForm.markAsDirty();
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
    comp.detailsForm.setValue({ name: 'Customer Renamed', text: 'Updated description' });
    comp.detailsForm.markAsDirty();
    await comp.onSave();

    expect(commandServiceMock.execute).toHaveBeenCalledWith('EditActor', expect.objectContaining({
      actorId: 5,
      name: 'Customer Renamed',
      description: 'Updated description'
    }));
    expect(comp.actorName()).toBe('Customer Renamed');
    expect(comp.version).toBe(1);
    expect(comp.hasUnsavedChanges()).toBe(false);
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
    comp.detailsForm.controls.name.setValue('Duplicate');
    await comp.onSave();
    expect(comp.errorMessage()).toBe('Name conflict');
  });

  it('onSave catch sets generic error when command throws', async () => {
    fixture.detectChanges();
    await flush();
    commandServiceMock.execute.mockRejectedValue(new Error('network down'));
    comp.detailsForm.controls.name.setValue('Anything');
    await comp.onSave();
    expect(comp.errorMessage()).toBe('Save failed.');
  });

  // #173 required test (§10.3). The Goals step's associations bump the actor's @Version but
  // return no entity, so the wizard has to refetch. If it did not, coming back to Details and
  // pressing Continue would send the version captured at step 1 and 409.
  describe('wizard version contract (#173)', () => {
    it('survives create -> add goal -> back to Details -> rename -> Continue', async () => {
      fixture.detectChanges();
      await flush();

      let version = 0;
      commandServiceMock.execute.mockImplementation(async (type: string) => {
        if (type === 'EditActor') {
          version += 1;
          return { success: true, entity: { ...MOCK_ACTOR, id: 5, version } };
        }
        // Association commands return no entity - that is the whole point.
        version += 1;
        return { success: true, entity: null };
      });
      actorServiceMock.getActor.mockImplementation(async () => ({ ...MOCK_ACTOR, id: 5, version }));

      comp.detailsForm.controls.name.setValue('Customer');

      const step1 = { step: { key: 'details' }, complete: vi.fn(), fail: vi.fn() };
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      await comp.onStepCommit(step1 as any);
      expect(step1.complete).toHaveBeenCalled();

      // Goals step: associate, which bumps the actor server-side.
      await comp.onGoalSelected({ id: 7, name: 'Avoid late fees', entityType: 'Goal' });

      // Back to Details, rename, Continue again.
      comp.detailsForm.controls.name.setValue('Customer Renamed');
      const step3 = { step: { key: 'details' }, complete: vi.fn(), fail: vi.fn() };
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      await comp.onStepCommit(step3 as any);

      expect(step3.complete).toHaveBeenCalled();
      expect(step3.fail).not.toHaveBeenCalled();

      // The second EditActor must not carry the version from the first.
      const edits = commandServiceMock.execute.mock.calls.filter(c => c[0] === 'EditActor');
      expect(edits).toHaveLength(2);
      expect(edits[1][1].version).toBe(2);
      expect(edits[1][1].name).toBe('Customer Renamed');
    });

    it('the Goals step just advances - it issues no command of its own', async () => {
      fixture.detectChanges();
      await flush();
      commandServiceMock.execute.mockClear();

      const request = { step: { key: 'goals' }, complete: vi.fn(), fail: vi.fn() };
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      await comp.onStepCommit(request as any);

      expect(request.complete).toHaveBeenCalled();
      expect(commandServiceMock.execute).not.toHaveBeenCalled();
    });
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

  // Regression: AddGoal/RemoveGoal merge the container (this actor), bumping its @Version,
  // but register no result extractor - so result.entity is null and the new version can only
  // be had by refetching. Before this, adding a goal and then saving the name gave a 409.
  describe('stale version after a goal association', () => {
    it('re-reads version after adding a goal', async () => {
      paramMap$.next(convertToParamMap({ name: 'proj1', actorId: '5' }));
      fixture.detectChanges();
      await flush();
      expect(comp.version).toBe(0);

      commandServiceMock.execute.mockResolvedValue({ success: true, entity: null });
      actorServiceMock.getActor.mockResolvedValue({ ...MOCK_ACTOR, version: 3 });

      await comp.onGoalSelected({ id: 7, name: 'Avoid late fees', entityType: 'Goal' });
      expect(comp.version).toBe(3);
    });

    it('re-reads version after removing a goal', async () => {
      paramMap$.next(convertToParamMap({ name: 'proj1', actorId: '5' }));
      fixture.detectChanges();
      await flush();

      commandServiceMock.execute.mockResolvedValue({ success: true, entity: null });
      actorServiceMock.getActor.mockResolvedValue({ ...MOCK_ACTOR, version: 4 });

      await comp.onRemoveGoal({ id: 1, name: 'Purchase item', entityType: 'Goal' });
      expect(comp.version).toBe(4);
    });

    it('does not discard unsaved detail edits while refreshing the version', async () => {
      paramMap$.next(convertToParamMap({ name: 'proj1', actorId: '5' }));
      fixture.detectChanges();
      await flush();

      // User types a new name, then adds a goal without saving first.
      comp.detailsForm.controls.name.setValue('Renamed but unsaved');
      comp.detailsForm.controls.name.markAsDirty();

      commandServiceMock.execute.mockResolvedValue({ success: true, entity: null });
      actorServiceMock.getActor.mockResolvedValue({ ...MOCK_ACTOR, version: 9 });
      await comp.onGoalSelected({ id: 7, name: 'Avoid late fees', entityType: 'Goal' });

      // The version advanced, but the in-progress edit survived - which is why the refresh
      // is narrow instead of a full loadActor().
      expect(comp.version).toBe(9);
      expect(comp.detailsForm.controls.name.value).toBe('Renamed but unsaved');
      expect(comp.hasUnsavedChanges()).toBe(true);
    });

    it('leaves the held version alone when the refresh fetch fails', async () => {
      paramMap$.next(convertToParamMap({ name: 'proj1', actorId: '5' }));
      fixture.detectChanges();
      await flush();

      commandServiceMock.execute.mockResolvedValue({ success: true, entity: null });
      actorServiceMock.getActor.mockRejectedValue(new Error('network'));

      await comp.onGoalSelected({ id: 7, name: 'Avoid late fees', entityType: 'Goal' });
      // Still 0, and no error surfaced: the association itself succeeded.
      expect(comp.version).toBe(0);
      expect(comp.goals().some(g => g.id === 7)).toBe(true);
    });
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

  it('renders the goal link as a routerLink anchor', async () => {
    paramMap$.next(convertToParamMap({ name: 'proj1', actorId: '5' }));
    fixture.detectChanges();
    await flush();
    fixture.detectChanges();
    const a = fixture.nativeElement.querySelector('[data-testid="actor-goal-link"]');
    expect(a.tagName).toBe('A');
    expect(a.getAttribute('href')).toBe('/projects/proj1/goals/1');
  });

  it('renders referenced-by links as routerLink anchors', async () => {
    paramMap$.next(convertToParamMap({ name: 'proj1', actorId: '5' }));
    fixture.detectChanges();
    await flush();
    fixture.detectChanges();
    const uc = fixture.nativeElement.querySelector('[data-testid="actor-refby-usecase-link"]');
    const st = fixture.nativeElement.querySelector('[data-testid="actor-refby-story-link"]');
    expect(uc.getAttribute('href')).toBe('/projects/proj1/use-cases/30');
    expect(st.getAttribute('href')).toBe('/projects/proj1/stories/40');
  });

  it('onBack navigates back to actor list', () => {
    comp.projectName = 'proj1';
    comp.onBack();
    expect(router.navigate).toHaveBeenCalledWith(['/projects', 'proj1', 'actors']);
  });

  it('hasUnsavedChanges() reflects the form dirty state', () => {
    expect(comp.hasUnsavedChanges()).toBe(false);
    comp.detailsForm.markAsDirty();
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

    comp.detailsForm.controls.name.setValue('Editing in progress');
    comp.detailsForm.controls.name.markAsDirty();
    expect(comp.hasUnsavedChanges()).toBe(true);

    events$.next({ targetType: 'Actor', targetId: 5 });
    await flush();

    // The reload was attempted (getActor called) but the result was discarded —
    // so the component's name field still holds the user's unsaved edit.
    expect(comp.detailsForm.controls.name.value).toBe('Editing in progress');
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
