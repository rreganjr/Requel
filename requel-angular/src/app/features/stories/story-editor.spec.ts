import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter, Router, ActivatedRoute, convertToParamMap } from '@angular/router';
import { BehaviorSubject, EMPTY } from 'rxjs';
import { ConfirmationService, MessageService } from 'primeng/api';
import { StoryEditorComponent } from './story-editor';
import { StoryService } from '../../core/story.service';
import { ActorService } from '../../core/actor.service';
import { CommandService } from '../../core/command.service';
import { ProjectService } from '../../core/project.service';
import { PermissionService } from '../../core/permission.service';
import { EventStreamService } from '../../core/event-stream.service';
import { AppWizardStepComponent, WizardCommitRequest } from '../../shared/app-form-wizard';

const MOCK_ACTORS = [
  { id: 1, version: 0, name: 'Customer', text: null, goals: null, referencedByUseCases: null, referencedByStories: null },
  { id: 2, version: 0, name: 'Admin', text: null, goals: null, referencedByUseCases: null, referencedByStories: null }
];

const MOCK_STORY = {
  id: 20, version: 4, name: 'User logs in', text: 'A user logs in with credentials.',
  storyType: 'Success', primaryActorName: 'Customer', goals: [], actors: []
};

const flush = () => new Promise(r => setTimeout(r, 0));

/** A stand-in for the wizard's commit handshake, so the host can be driven directly. */
function commitRequest(key: string): WizardCommitRequest & {
  completed: () => boolean;
  failure: () => string | null;
} {
  let completed = false;
  let failure: string | null = null;
  return {
    step: { key } as AppWizardStepComponent,
    complete: () => {
      completed = true;
    },
    fail: (message: string) => {
      failure = message;
    },
    completed: () => completed,
    failure: () => failure,
  };
}

describe('StoryEditorComponent', () => {
  let paramMap$: BehaviorSubject<ReturnType<typeof convertToParamMap>>;
  let storyServiceMock: { getStory: ReturnType<typeof vi.fn> };
  let actorServiceMock: { listActors: ReturnType<typeof vi.fn> };
  let commandServiceMock: { execute: ReturnType<typeof vi.fn> };
  let permissionServiceMock: { loadForProject: ReturnType<typeof vi.fn>; canEdit: ReturnType<typeof vi.fn>; canDelete: ReturnType<typeof vi.fn> };
  let eventStreamServiceMock: { events$: typeof EMPTY; addSubscription: ReturnType<typeof vi.fn>; removeSubscription: ReturnType<typeof vi.fn> };
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  let fixture: any;
  let comp: StoryEditorComponent;
  let router: Router;

  /** Payload of the nth (0-based) EditStory call. */
  const editStoryCall = (n: number) =>
    commandServiceMock.execute.mock.calls.filter(c => c[0] === 'EditStory')[n]?.[1];

  beforeEach(() => {
    paramMap$ = new BehaviorSubject(convertToParamMap({ name: 'proj1', storyId: 'new' }));

    storyServiceMock = { getStory: vi.fn().mockResolvedValue(MOCK_STORY) };
    actorServiceMock = { listActors: vi.fn().mockResolvedValue(MOCK_ACTORS) };
    commandServiceMock = {
      execute: vi.fn().mockResolvedValue({ success: true, entity: MOCK_STORY })
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
      imports: [StoryEditorComponent],
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { paramMap: paramMap$.asObservable() } },
        { provide: StoryService, useValue: storyServiceMock },
        { provide: ActorService, useValue: actorServiceMock },
        { provide: CommandService, useValue: commandServiceMock },
        { provide: ProjectService, useValue: { notifyTreeChanged: vi.fn() } },
        { provide: PermissionService, useValue: permissionServiceMock },
        { provide: EventStreamService, useValue: eventStreamServiceMock },
        { provide: MessageService, useValue: { add: vi.fn() } }
      ]
    });
    fixture = TestBed.createComponent(StoryEditorComponent);
    comp = fixture.componentInstance;
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
  });

  async function renderNew(): Promise<void> {
    fixture.detectChanges();
    await flush();
    fixture.detectChanges();
  }

  async function renderExisting(): Promise<void> {
    paramMap$.next(convertToParamMap({ name: 'proj1', storyId: '20' }));
    fixture.detectChanges();
    await flush();
    fixture.detectChanges();
  }

  it('isNew() is true when storyId param is "new"', async () => {
    await renderNew();
    expect(comp.isNew()).toBe(true);
  });

  it('loads story when storyId param is numeric', async () => {
    await renderExisting();
    expect(storyServiceMock.getStory).toHaveBeenCalledWith('proj1', 20);
    expect(comp.storyName()).toBe('User logs in');
    expect(comp.story()?.id).toBe(20);
  });

  it('actorOptions populated from actorService.listActors', async () => {
    await renderNew();
    expect(actorServiceMock.listActors).toHaveBeenCalledWith('proj1');
    expect(comp.actorOptions().length).toBe(2);
    expect(comp.actorOptions()[0].label).toBe('Customer');
  });

  it('loads all four fields into the reactive form', async () => {
    await renderExisting();
    expect(comp.detailsForm.getRawValue()).toEqual({
      name: 'User logs in',
      storyType: 'Success',
      primaryActorName: 'Customer',
      text: 'A user logs in with credentials.',
    });
    expect(comp.detailsForm.pristine).toBe(true);
  });

  it('treats a missing primaryActorName as empty rather than null', async () => {
    storyServiceMock.getStory.mockResolvedValue({ ...MOCK_STORY, primaryActorName: null });
    await renderExisting();
    expect(comp.detailsForm.controls.primaryActorName.value).toBe('');
  });

  it('onSave calls commandService.execute("EditStory") with story fields', async () => {
    await renderExisting();
    comp.detailsForm.setValue({
      name: 'My Story', storyType: 'Success', primaryActorName: 'Admin', text: 'Story content'
    });
    await comp.onSave();

    expect(commandServiceMock.execute).toHaveBeenCalledWith('EditStory', expect.objectContaining({
      projectName: 'proj1',
      name: 'My Story',
      text: 'Story content',
      storyTypeName: 'Success',
      primaryActorName: 'Admin'
    }));
  });

  it('sends a cleared primary actor as null', async () => {
    await renderExisting();
    comp.detailsForm.patchValue({ primaryActorName: '' });
    comp.detailsForm.markAsDirty();
    await comp.onSave();

    expect(editStoryCall(0)).toEqual(expect.objectContaining({ primaryActorName: null }));
  });

  describe('the edit-mode Save policy', () => {
    it('replaces the old trackChanges()/hasChanges() pair with the form dirty state', async () => {
      await renderExisting();
      expect(comp.hasUnsavedChanges()).toBe(false);
      expect(comp.canSave()).toBe(false);

      comp.detailsForm.controls.name.setValue('Modified Name');
      comp.detailsForm.controls.name.markAsDirty();

      expect(comp.hasUnsavedChanges()).toBe(true);
      expect(comp.canSave()).toBe(true);
    });

    it('is disabled again after a successful save', async () => {
      await renderExisting();
      comp.detailsForm.patchValue({ name: 'Renamed' });
      comp.detailsForm.markAsDirty();
      await comp.onSave();

      expect(comp.canSave()).toBe(false);
      expect(comp.hasUnsavedChanges()).toBe(false);
    });

    it('is disabled while the name is blank', async () => {
      await renderExisting();
      comp.detailsForm.patchValue({ name: '' });
      comp.detailsForm.markAsDirty();
      expect(comp.canSave()).toBe(false);
    });

    it('refuses an invalid form without calling the command', async () => {
      await renderExisting();
      commandServiceMock.execute.mockClear();
      comp.detailsForm.patchValue({ name: '' });
      await comp.onSave();

      expect(commandServiceMock.execute).not.toHaveBeenCalled();
      expect(comp.detailsForm.controls.name.touched).toBe(true);
    });
  });

  describe('the create wizard', () => {
    it('renders the wizard on the create route and not on the edit route', async () => {
      await renderNew();
      expect(fixture.nativeElement.querySelector('app-form-wizard')).not.toBeNull();

      await renderExisting();
      expect(fixture.nativeElement.querySelector('app-form-wizard')).toBeNull();
      expect(fixture.nativeElement.querySelector('[data-testid="story-save"]')).not.toBeNull();
    });

    it('offers Details, Goals and Additional Actors as the three steps', async () => {
      await renderNew();
      const keys = Array.from(
        (fixture.nativeElement as HTMLElement).querySelectorAll('[data-testid^="wizard-step-"]')
      ).map(n => n.getAttribute('data-testid'));

      expect(keys).toEqual(['wizard-step-details', 'wizard-step-goals', 'wizard-step-actors']);
    });

    it('starts on details with the default story type and no id', async () => {
      await renderNew();
      expect(comp.wizardStep).toBe('details');
      expect(comp.detailsForm.controls.storyType.value).toBe('Success');
      expect(comp.storyId).toBeNull();
    });

    it('creates the story on the details commit, without a storyId or version', async () => {
      await renderNew();
      comp.detailsForm.setValue({
        name: 'User logs out', storyType: 'Success', primaryActorName: 'Customer', text: 'Bye.'
      });

      const request = commitRequest('details');
      await comp.onStepCommit(request);

      expect(editStoryCall(0)).toEqual({
        projectName: 'proj1',
        name: 'User logs out',
        text: 'Bye.',
        storyTypeName: 'Success',
        primaryActorName: 'Customer'
      });
      expect(request.completed()).toBe(true);
      expect(comp.storyId).toBe(20);
    });

    it('hydrates goals and actors after create so the later steps have data', async () => {
      await renderNew();
      comp.detailsForm.patchValue({ name: 'User logs out' });
      await comp.onStepCommit(commitRequest('details'));

      expect(storyServiceMock.getStory).toHaveBeenCalledWith('proj1', 20);
      expect(comp.story()?.id).toBe(20);
    });

    it('advances the optional steps without calling the API', async () => {
      await renderNew();
      commandServiceMock.execute.mockClear();

      const goals = commitRequest('goals');
      await comp.onStepCommit(goals);
      const actors = commitRequest('actors');
      await comp.onStepCommit(actors);

      expect(goals.completed()).toBe(true);
      expect(actors.completed()).toBe(true);
      expect(commandServiceMock.execute).not.toHaveBeenCalled();
    });

    it('reports a failed create on the step instead of advancing', async () => {
      commandServiceMock.execute.mockResolvedValue({ success: false, error: 'Name conflict' });
      await renderNew();
      comp.detailsForm.patchValue({ name: 'Duplicate' });

      const request = commitRequest('details');
      await comp.onStepCommit(request);

      expect(request.completed()).toBe(false);
      expect(request.failure()).toBe('Name conflict');
      expect(comp.storyId).toBeNull();
    });

    it('navigates to the saved story when the wizard finishes', async () => {
      await renderNew();
      comp.detailsForm.patchValue({ name: 'User logs out' });
      await comp.onStepCommit(commitRequest('details'));

      comp.onWizardFinished();
      expect(router.navigate).toHaveBeenCalledWith(['/projects', 'proj1', 'stories', 20]);
    });
  });

  describe('the version contract', () => {
    it('adopts the version from the response and sends it on the next save', async () => {
      await renderExisting();
      commandServiceMock.execute.mockResolvedValue({
        success: true,
        entity: { ...MOCK_STORY, version: 11 }
      });

      comp.detailsForm.patchValue({ name: 'First rename' });
      comp.detailsForm.markAsDirty();
      await comp.onSave();
      comp.detailsForm.patchValue({ name: 'Second rename' });
      comp.detailsForm.markAsDirty();
      await comp.onSave();

      expect(editStoryCall(0)).toEqual(expect.objectContaining({ storyId: 20, version: 4 }));
      expect(editStoryCall(1)).toEqual(expect.objectContaining({ storyId: 20, version: 11 }));
    });

    it('sends the refreshed version when the user steps back to Details and re-commits', async () => {
      await renderNew();
      comp.detailsForm.patchValue({ name: 'User logs out' });
      await comp.onStepCommit(commitRequest('details'));

      comp.detailsForm.patchValue({ name: 'User signs out' });
      const second = commitRequest('details');
      await comp.onStepCommit(second);

      expect(editStoryCall(0)['version']).toBeUndefined();
      expect(editStoryCall(1)).toEqual(expect.objectContaining({ storyId: 20, version: 4 }));
      expect(second.completed()).toBe(true);
    });

    it('recovers from a stale-version 409 by refetching and keeping the step', async () => {
      await renderExisting();
      storyServiceMock.getStory.mockClear();
      commandServiceMock.execute.mockResolvedValue({
        success: false,
        status: 409,
        error: 'Story has been changed by another user.'
      });

      comp.detailsForm.patchValue({ name: 'Renamed' });
      const request = commitRequest('details');
      await comp.onStepCommit(request);

      expect(storyServiceMock.getStory).toHaveBeenCalledWith('proj1', 20);
      expect(request.completed()).toBe(false);
      expect(request.failure()).toContain('changed elsewhere');
    });

    it('treats a non-409 failure as an ordinary error, with no refetch', async () => {
      await renderExisting();
      storyServiceMock.getStory.mockClear();
      commandServiceMock.execute.mockResolvedValue({
        success: false,
        status: 400,
        error: 'Name is required.'
      });

      comp.detailsForm.patchValue({ name: 'Renamed' });
      comp.detailsForm.markAsDirty();
      await comp.onSave();

      expect(storyServiceMock.getStory).not.toHaveBeenCalled();
      expect(comp.errorMessage()).toBe('Name is required.');
    });

    it('keeps unsaved edits on a reload but still takes the new version', async () => {
      // The first implementation had no guard here at all and overwrote whatever the user was
      // typing. The second guarded only SSE-driven reloads, so the initial load still did.
      await renderExisting();
      comp.detailsForm.controls.name.setValue('Local edit');
      comp.detailsForm.controls.name.markAsDirty();

      storyServiceMock.getStory.mockResolvedValue({ ...MOCK_STORY, name: 'Remote edit', version: 15 });
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      await (comp as any).loadStory();

      expect(comp.detailsForm.controls.name.value).toBe('Local edit');

      commandServiceMock.execute.mockResolvedValue({ success: true, entity: MOCK_STORY });
      await comp.onSave();
      expect(editStoryCall(0)).toEqual(expect.objectContaining({ version: 15 }));
    });

    // The initial load is the case that actually bit: form-wizard.e2e.ts typed into the edit
    // form before the detail GET resolved, the reset landed on top, and Save stayed disabled
    // with the typed value gone.
    it('does not clobber a value typed while the initial load is still in flight', async () => {
      let resolveGet: (story: unknown) => void = () => {};
      storyServiceMock.getStory.mockImplementation(
        () => new Promise(resolve => { resolveGet = resolve; })
      );

      paramMap$.next(convertToParamMap({ name: 'proj1', storyId: '20' }));
      fixture.detectChanges();
      await flush();

      // The user is faster than the network.
      comp.detailsForm.controls.name.setValue('Typed while loading');
      comp.detailsForm.controls.name.markAsDirty();

      resolveGet({ ...MOCK_STORY, name: 'User logs in', version: 4 });
      await flush();

      expect(comp.detailsForm.controls.name.value).toBe('Typed while loading');
      expect(comp.detailsForm.dirty).toBe(true);
      // Server state still landed, so Save has a usable version to send.
      expect(comp.story()?.id).toBe(20);

      commandServiceMock.execute.mockResolvedValue({ success: true, entity: MOCK_STORY });
      await comp.onSave();
      expect(editStoryCall(0)).toEqual(expect.objectContaining({
        name: 'Typed while loading', version: 4
      }));
    });
  });

  // Regression #179, updated for #180. AddGoal/RemoveGoal/AddActor/RemoveActor all merge the
  // container — here the story — bumping its @Version. Since #178 they return the merged story as
  // result.entity, so the component reads the new version and lists off the response instead of
  // refetching. Before this, adding a goal and then saving the name gave a 409 the user could not
  // avoid.
  describe('association result (#179/#180)', () => {
    const GOAL_A = { id: 7, name: 'Avoid late fees', entityType: 'Goal' };
    const GOAL_B = { id: 8, name: 'Buy quickly', entityType: 'Goal' };
    const ACTOR_A = { id: 1, name: 'Customer', entityType: 'Actor' };

    /** The association command returns the merged story as `result.entity` (#180). */
    function associationSucceeds(entity: unknown = MOCK_STORY): void {
      commandServiceMock.execute.mockResolvedValue({ success: true, entity });
    }

    /** Renames and saves, so the version the component now holds shows up on the wire. */
    async function saveAndReadVersion(): Promise<number | undefined> {
      commandServiceMock.execute.mockResolvedValue({ success: true, entity: MOCK_STORY });
      comp.detailsForm.patchValue({ name: 'Renamed after association' });
      comp.detailsForm.markAsDirty();
      await comp.onSave();
      return editStoryCall(0)?.['version'] as number | undefined;
    }

    it.each([
      ['onGoalSelected', GOAL_A, 5],
      ['onRemoveGoal', GOAL_A, 6],
      ['onActorSelected', ACTOR_A, 7],
      ['onRemoveActor', ACTOR_A, 8],
    ] as const)('%s takes the bumped version from the response and sends it on the next save', async (method, ref, bumped) => {
      await renderExisting();
      storyServiceMock.getStory.mockClear();
      associationSucceeds({ ...MOCK_STORY, version: bumped });

      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      await (comp as any)[method](ref);

      // No follow-up GET: the version came off result.entity.
      expect(storyServiceMock.getStory).not.toHaveBeenCalled();
      // Without it this would still be MOCK_STORY's 4 and the save would 409.
      expect(await saveAndReadVersion()).toBe(bumped);
    });

    it('onActorSelected sends AddActorToActorContainer with containerType Story', async () => {
      await renderExisting();
      associationSucceeds();
      await comp.onActorSelected(ACTOR_A);
      expect(commandServiceMock.execute).toHaveBeenCalledWith('AddActorToActorContainer',
        expect.objectContaining({
          projectName: 'proj1',
          actorContainerId: 20,
          actorId: 1,
          containerType: 'Story'
        }));
    });

    it('onRemoveActor sends RemoveActorFromActorContainer with containerType Story', async () => {
      await renderExisting();
      associationSucceeds();
      await comp.onRemoveActor(ACTOR_A);
      expect(commandServiceMock.execute).toHaveBeenCalledWith('RemoveActorFromActorContainer',
        expect.objectContaining({
          projectName: 'proj1',
          actorContainerId: 20,
          actorId: 1,
          containerType: 'Story'
        }));
    });

    it('takes the goals list from the response instead of patching it locally', async () => {
      await renderExisting();
      storyServiceMock.getStory.mockClear();
      // The server sorted, renamed and assigned — none of which a local patch could know.
      associationSucceeds({ ...MOCK_STORY, version: 5, goals: [GOAL_A, GOAL_B] });

      await comp.onGoalSelected(GOAL_A);

      expect(comp.story()?.goals).toEqual([GOAL_A, GOAL_B]);
      expect(comp.existingGoalIds()).toEqual([7, 8]);
      expect(storyServiceMock.getStory).not.toHaveBeenCalled();
    });

    it('ignores an out-of-order response so the newer one wins', async () => {
      await renderExisting();

      // Two removes in flight at once — two clicks on the goals table. Their command responses are
      // held so they can be resolved in the wrong order on purpose.
      const resolvers: Array<(result: unknown) => void> = [];
      commandServiceMock.execute.mockImplementation(
        () => new Promise(resolve => resolvers.push(resolve))
      );

      const first = comp.onRemoveGoal(GOAL_A);
      await flush();
      const second = comp.onRemoveGoal(GOAL_B);
      await flush();
      expect(resolvers.length).toBe(2);

      // Newer response lands first, then the older one — the case the version guard exists for.
      resolvers[1]({ success: true, entity: { ...MOCK_STORY, version: 9, goals: [] } });
      resolvers[0]({ success: true, entity: { ...MOCK_STORY, version: 7, goals: [GOAL_B] } });
      await Promise.all([first, second]);

      // Unguarded, the stale response would resurrect GOAL_B and roll the version back to 7.
      expect(comp.story()?.goals).toEqual([]);
      expect(comp.story()?.version).toBe(9);
      expect(await saveAndReadVersion()).toBe(9);
    });

    it('leaves the held version and list unchanged when the response carries no entity', async () => {
      await renderExisting();
      associationSucceeds(null);

      await comp.onGoalSelected(GOAL_A);

      // Nothing is surfaced, the held version is still sent, and any resulting 409 is reported
      // through recoverFromStaleVersion() on the next save.
      expect(comp.errorMessage()).toBeNull();
      expect(comp.story()?.goals).toEqual([]);
      expect(await saveAndReadVersion()).toBe(4);
    });

    it('does not refresh when the association command fails', async () => {
      await renderExisting();
      storyServiceMock.getStory.mockClear();
      commandServiceMock.execute.mockResolvedValue({
        success: false, status: 400, error: 'Goal already associated.'
      });

      await comp.onGoalSelected(GOAL_A);

      expect(storyServiceMock.getStory).not.toHaveBeenCalled();
      expect(comp.errorMessage()).toBe('Goal already associated.');
    });
  });

  it('onDelete triggers confirm then calls execute("DeleteStory")', async () => {
    await renderExisting();

    const cs = fixture.debugElement.injector.get(ConfirmationService);
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    vi.spyOn(cs, 'confirm').mockImplementation((conf: any) => conf.accept?.());

    comp.onDelete();
    await flush();

    expect(commandServiceMock.execute).toHaveBeenCalledWith('DeleteStory', expect.objectContaining({
      projectName: 'proj1',
      storyId: 20
    }));
    expect(router.navigate).toHaveBeenCalledWith(['/projects', 'proj1', 'stories']);
  });
  // #185. The gate is the structural half of the fix: with the form absent until the detail GET
  // resolves, there is no input for a user - or a fast e2e test - to type into before the load
  // lands. The dirty-guard in loadStory() is then belt-and-braces for the background callers.
  // Finishes the #131 / #168 migration for this editor.
  describe('render gate (#185, finishing #131)', () => {
    function el(): HTMLElement {
      return fixture.nativeElement as HTMLElement;
    }

    it('shows the skeleton and no form until the detail GET resolves', async () => {
      let resolveGet: (story: unknown) => void = () => {};
      storyServiceMock.getStory.mockImplementation(
        () => new Promise(resolve => { resolveGet = resolve; })
      );

      paramMap$.next(convertToParamMap({ name: 'proj1', storyId: '20' }));
      fixture.detectChanges();
      await flush();
      fixture.detectChanges();

      expect(el().querySelector('[data-testid="story-editor-loading"]')).not.toBeNull();
      // The point of the gate: nothing to type into yet.
      expect(el().querySelector('[data-testid="story-name"]')).toBeNull();

      resolveGet(MOCK_STORY);
      await flush();
      fixture.detectChanges();

      expect(el().querySelector('[data-testid="story-editor-loading"]')).toBeNull();
      expect(el().querySelector('[data-testid="story-name"]')).not.toBeNull();
    });

    // The create route never loads, so the gate has to be resolved synchronously in ngOnInit -
    // otherwise the wizard sits behind the skeleton forever and create is unreachable.
    it('renders the create wizard immediately, with no skeleton', async () => {
      fixture.detectChanges();
      await flush();
      fixture.detectChanges();

      expect(comp.loading()).toBe(false);
      expect(el().querySelector('[data-testid="story-editor-loading"]')).toBeNull();
      expect(el().querySelector('[data-testid="story-wizard"]')).not.toBeNull();
    });

    it('shows a retryable error state when the load fails, and recovers on retry', async () => {
      storyServiceMock.getStory.mockRejectedValueOnce(new Error('boom'));

      paramMap$.next(convertToParamMap({ name: 'proj1', storyId: '20' }));
      fixture.detectChanges();
      await flush();
      fixture.detectChanges();

      expect(el().querySelector('[data-testid="story-editor-load-error"]')).not.toBeNull();
      expect(el().querySelector('[data-testid="story-name"]')).toBeNull();

      storyServiceMock.getStory.mockResolvedValue(MOCK_STORY);
      comp.retryLoad();
      await flush();
      fixture.detectChanges();

      expect(el().querySelector('[data-testid="story-editor-load-error"]')).toBeNull();
      expect(el().querySelector('[data-testid="story-name"]')).not.toBeNull();
    });

    // Background callers pass skeleton=false. Blanking the form under a user who is reading it
    // because someone else touched the entity would be its own bug.
    it('does not blank the form for a background reload', async () => {
      paramMap$.next(convertToParamMap({ name: 'proj1', storyId: '20' }));
      fixture.detectChanges();
      await flush();

      let resolveGet: (story: unknown) => void = () => {};
      storyServiceMock.getStory.mockImplementation(
        () => new Promise(resolve => { resolveGet = resolve; })
      );
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      const reload = (comp as any).loadStory(false);
      await flush();
      fixture.detectChanges();

      expect(comp.loading()).toBe(false);
      expect(el().querySelector('[data-testid="story-name"]')).not.toBeNull();

      resolveGet(MOCK_STORY);
      await reload;
    });
  });

});
