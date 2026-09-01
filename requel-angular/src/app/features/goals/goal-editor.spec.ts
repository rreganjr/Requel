import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter, Router, ActivatedRoute, convertToParamMap } from '@angular/router';
import { BehaviorSubject, Subject } from 'rxjs';
import { ConfirmationService, MessageService } from 'primeng/api';
import { GoalEditorComponent } from './goal-editor';
import { GoalService } from '../../core/goal.service';
import { TagService } from '../../core/tag.service';
import { CommandService } from '../../core/command.service';
import { ProjectService } from '../../core/project.service';
import { PermissionService } from '../../core/permission.service';
import { EventStreamService } from '../../core/event-stream.service';
import { AnnouncerService } from '../../core/announcer.service';
import { StreamEventEnvelope } from '../../models/stream';
import { AppWizardStepComponent, WizardCommitRequest } from '../../shared/app-form-wizard';
import { ARTIFACT_NAME_MAX_LENGTH } from '../../shared/validation-limits';

const MOCK_GOAL = {
  id: 10, version: 5, name: 'Improve UX', text: 'Make it great.',
  relationsFromThisGoal: [], relationsToThisGoal: [], referencedBy: []
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

describe('GoalEditorComponent', () => {
  let paramMap$: BehaviorSubject<ReturnType<typeof convertToParamMap>>;
  let goalServiceMock: { getGoal: ReturnType<typeof vi.fn> };
  let commandServiceMock: { execute: ReturnType<typeof vi.fn> };
  let projectServiceMock: { notifyTreeChanged: ReturnType<typeof vi.fn> };
  let permissionServiceMock: { loadForProject: ReturnType<typeof vi.fn>; canEdit: ReturnType<typeof vi.fn>; canDelete: ReturnType<typeof vi.fn> };
  let events$: Subject<StreamEventEnvelope>;
  let eventStreamServiceMock: { events$: Subject<StreamEventEnvelope>; addSubscription: ReturnType<typeof vi.fn>; removeSubscription: ReturnType<typeof vi.fn> };
  let announcerMock: { announce: ReturnType<typeof vi.fn>; announceThrottled: ReturnType<typeof vi.fn> };
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  let fixture: any;
  let comp: GoalEditorComponent;
  let router: Router;

  /** Payload of the nth (0-based) EditGoal call. */
  const editGoalCall = (n: number) =>
    commandServiceMock.execute.mock.calls.filter(c => c[0] === 'EditGoal')[n]?.[1];

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
    events$ = new Subject<StreamEventEnvelope>();
    eventStreamServiceMock = {
      events$,
      addSubscription: vi.fn().mockResolvedValue(undefined),
      removeSubscription: vi.fn().mockResolvedValue(undefined)
    };
    announcerMock = { announce: vi.fn(), announceThrottled: vi.fn() };

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
            getCategories: vi.fn().mockResolvedValue([]),
            getTypedCategories: vi.fn().mockResolvedValue([])
          } },
        { provide: CommandService, useValue: commandServiceMock },
        { provide: ProjectService, useValue: projectServiceMock },
        { provide: PermissionService, useValue: permissionServiceMock },
        { provide: EventStreamService, useValue: eventStreamServiceMock },
        { provide: AnnouncerService, useValue: announcerMock },
        { provide: MessageService, useValue: { add: vi.fn() } }
      ]
    });
    fixture = TestBed.createComponent(GoalEditorComponent);
    comp = fixture.componentInstance;
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
  });

  /** Render the create route. */
  async function renderNew(): Promise<void> {
    fixture.detectChanges();
    await flush();
    fixture.detectChanges();
  }

  /** Render the edit route for goal 10. */
  async function renderExisting(): Promise<void> {
    paramMap$.next(convertToParamMap({ name: 'proj1', goalId: '10' }));
    fixture.detectChanges();
    await flush();
    fixture.detectChanges();
  }

  it('isNew() is true when goalId param is "new"', async () => {
    await renderNew();
    expect(comp.isNew()).toBe(true);
  });

  it('isNew() is false and goal() loaded when goalId is numeric', async () => {
    await renderExisting();
    expect(comp.isNew()).toBe(false);
    expect(goalServiceMock.getGoal).toHaveBeenCalledWith('proj1', 10);
    expect(comp.goalName()).toBe('Improve UX');
    expect(comp.goal()?.id).toBe(10);
  });

  it('loads the existing goal into the reactive form', async () => {
    await renderExisting();
    expect(comp.detailsForm.getRawValue()).toEqual({ name: 'Improve UX', text: 'Make it great.' });
    expect(comp.detailsForm.pristine).toBe(true);
  });

  it('onSave calls commandService.execute("EditGoal") with the form values', async () => {
    await renderExisting();
    comp.detailsForm.setValue({ name: 'New Goal', text: 'Details' });
    await comp.onSave();

    expect(commandServiceMock.execute).toHaveBeenCalledWith('EditGoal', expect.objectContaining({
      projectName: 'proj1',
      name: 'New Goal',
      text: 'Details'
    }));
  });

  it('onSave sets errorMessage when command returns error', async () => {
    commandServiceMock.execute.mockResolvedValue({ success: false, error: 'Name conflict' });
    await renderExisting();
    comp.detailsForm.setValue({ name: 'Duplicate', text: '' });
    await comp.onSave();

    expect(comp.errorMessage()).toBe('Name conflict');
  });

  it('onSave refuses an invalid form without calling the command', async () => {
    await renderExisting();
    commandServiceMock.execute.mockClear();
    comp.detailsForm.setValue({ name: '', text: 'no name' });
    await comp.onSave();

    expect(commandServiceMock.execute).not.toHaveBeenCalled();
    expect(comp.detailsForm.controls.name.touched).toBe(true);
  });

  describe('the edit-mode Save policy', () => {
    it('is disabled while the form is pristine', async () => {
      await renderExisting();
      expect(comp.canSave()).toBe(false);
    });

    it('is enabled once a valid change is made', async () => {
      await renderExisting();
      comp.detailsForm.controls.name.setValue('Renamed');
      comp.detailsForm.controls.name.markAsDirty();
      expect(comp.canSave()).toBe(true);
    });

    it('is disabled again after a successful save', async () => {
      await renderExisting();
      comp.detailsForm.setValue({ name: 'Renamed', text: 'x' });
      await comp.onSave();
      expect(comp.canSave()).toBe(false);
      expect(comp.hasUnsavedChanges()).toBe(false);
    });

    it('is disabled while the name is blank', async () => {
      await renderExisting();
      comp.detailsForm.setValue({ name: '', text: 'x' });
      expect(comp.canSave()).toBe(false);
    });
  });

  describe('the create wizard', () => {
    it('renders the wizard on the create route and not on the edit route', async () => {
      await renderNew();
      expect(fixture.nativeElement.querySelector('app-form-wizard')).not.toBeNull();

      await renderExisting();
      expect(fixture.nativeElement.querySelector('app-form-wizard')).toBeNull();
      expect(fixture.nativeElement.querySelector('[data-testid="goal-save"]')).not.toBeNull();
    });

    it('starts on the details step with an empty form', async () => {
      await renderNew();
      expect(comp.wizardStep).toBe('details');
      expect(comp.detailsForm.getRawValue()).toEqual({ name: '', text: '' });
      expect(comp.goalId).toBeNull();
    });

    it('creates the goal on the details commit, without a goalId or version', async () => {
      await renderNew();
      comp.detailsForm.setValue({ name: 'Reduce setup time', text: 'Cut it to a day.' });

      const request = commitRequest('details');
      await comp.onStepCommit(request);

      expect(editGoalCall(0)).toEqual({
        projectName: 'proj1',
        name: 'Reduce setup time',
        text: 'Cut it to a day.'
      });
      expect(request.completed()).toBe(true);
      expect(comp.goalId).toBe(10);
      expect(projectServiceMock.notifyTreeChanged).toHaveBeenCalled();
    });

    it('hydrates relations after create so the later steps have data', async () => {
      await renderNew();
      comp.detailsForm.setValue({ name: 'Reduce setup time', text: '' });
      await comp.onStepCommit(commitRequest('details'));

      expect(goalServiceMock.getGoal).toHaveBeenCalledWith('proj1', 10);
      expect(comp.goal()?.id).toBe(10);
    });

    it('advances the optional steps without calling the API', async () => {
      await renderNew();
      commandServiceMock.execute.mockClear();

      const tags = commitRequest('tags');
      await comp.onStepCommit(tags);
      const relations = commitRequest('relations');
      await comp.onStepCommit(relations);

      expect(tags.completed()).toBe(true);
      expect(relations.completed()).toBe(true);
      expect(commandServiceMock.execute).not.toHaveBeenCalled();
    });

    it('reports a failed create on the step instead of advancing', async () => {
      commandServiceMock.execute.mockResolvedValue({ success: false, error: 'Name conflict' });
      await renderNew();
      comp.detailsForm.setValue({ name: 'Duplicate', text: '' });

      const request = commitRequest('details');
      await comp.onStepCommit(request);

      expect(request.completed()).toBe(false);
      expect(request.failure()).toBe('Name conflict');
      expect(comp.goalId).toBeNull();
    });

    it('navigates to the saved goal when the wizard finishes', async () => {
      await renderNew();
      comp.detailsForm.setValue({ name: 'Reduce setup time', text: '' });
      await comp.onStepCommit(commitRequest('details'));

      comp.onWizardFinished();
      expect(router.navigate).toHaveBeenCalledWith(['/projects', 'proj1', 'goals', 10]);
    });
  });

  describe('the version contract', () => {
    it('adopts the version from the response and sends it on the next save', async () => {
      await renderExisting();
      // The server bumps the version on each accepted edit; the editor must re-read it
      // rather than keep sending the one it loaded with.
      commandServiceMock.execute.mockResolvedValue({
        success: true,
        entity: { ...MOCK_GOAL, version: 9 }
      });

      comp.detailsForm.setValue({ name: 'First rename', text: '' });
      await comp.onSave();
      comp.detailsForm.setValue({ name: 'Second rename', text: '' });
      await comp.onSave();

      expect(editGoalCall(0)).toEqual(expect.objectContaining({ goalId: 10, version: 5 }));
      expect(editGoalCall(1)).toEqual(expect.objectContaining({ goalId: 10, version: 9 }));
    });

    it('sends the refreshed version when the user steps back to Details and re-commits', async () => {
      // The regression this whole contract exists for: create on step 1, walk forward,
      // come back to fix a typo, Continue again. Re-sending the create-time version
      // would be a guaranteed 409.
      await renderNew();
      comp.detailsForm.setValue({ name: 'Reduce setup time', text: '' });
      await comp.onStepCommit(commitRequest('details'));

      comp.detailsForm.setValue({ name: 'Reduce setup time drastically', text: '' });
      const second = commitRequest('details');
      await comp.onStepCommit(second);

      expect(editGoalCall(0)['version']).toBeUndefined();
      expect(editGoalCall(1)).toEqual(expect.objectContaining({ goalId: 10, version: 5 }));
      expect(second.completed()).toBe(true);
    });

    it('recovers from a stale-version 409 by refetching and keeping the step', async () => {
      await renderExisting();
      goalServiceMock.getGoal.mockClear();
      commandServiceMock.execute.mockResolvedValue({
        success: false,
        status: 409,
        error: 'Goal has been changed by another user.'
      });

      comp.detailsForm.setValue({ name: 'Renamed', text: '' });
      const request = commitRequest('details');
      await comp.onStepCommit(request);

      expect(goalServiceMock.getGoal).toHaveBeenCalledWith('proj1', 10);
      expect(request.completed()).toBe(false);
      expect(request.failure()).toContain('changed elsewhere');
    });

    it('surfaces the stale-version message on an edit-mode save', async () => {
      await renderExisting();
      commandServiceMock.execute.mockResolvedValue({
        success: false,
        status: 409,
        error: 'Goal has been changed by another user.'
      });

      comp.detailsForm.setValue({ name: 'Renamed', text: '' });
      await comp.onSave();

      expect(comp.errorMessage()).toContain('changed elsewhere');
    });

    it('treats a non-409 failure as an ordinary error, with no refetch', async () => {
      await renderExisting();
      goalServiceMock.getGoal.mockClear();
      commandServiceMock.execute.mockResolvedValue({
        success: false,
        status: 400,
        error: 'Name is required.'
      });

      comp.detailsForm.setValue({ name: 'Renamed', text: '' });
      await comp.onSave();

      expect(goalServiceMock.getGoal).not.toHaveBeenCalled();
      expect(comp.errorMessage()).toBe('Name is required.');
    });
  });

  it('onDelete triggers confirm then calls execute("DeleteGoal")', async () => {
    await renderExisting();

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
    await renderExisting();

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

  it('sends the persisted name as the relation source, not an unsaved rename', async () => {
    await renderExisting();
    comp.detailsForm.controls.name.setValue('Unsaved rename');
    comp.onRelationGoalSelected({ entityType: 'Goal', id: 2, name: 'Reduce churn' });
    await comp.onConfirmRelation();

    expect(commandServiceMock.execute).toHaveBeenCalledWith('EditGoalRelation', expect.objectContaining({
      fromGoalName: 'Improve UX',
      toGoalName: 'Reduce churn'
    }));
  });

  it('reverse-add: adds this goal into the picked container, then reloads to refresh referencedBy', async () => {
    await renderExisting();
    goalServiceMock.getGoal.mockClear();
    // The reverse command merges the CONTAINER, not the goal, so the handler must reload the goal.
    commandServiceMock.execute.mockResolvedValue({ success: true, entity: { id: 999 } });
    await comp.onReferrerSelected({ entityType: 'Actor', id: 7, name: 'Shopper' });
    expect(commandServiceMock.execute).toHaveBeenCalledWith('AddGoalToGoalContainer', expect.objectContaining({
      projectName: 'proj1', goalContainerId: 7, goalId: 10, containerType: 'Actor'
    }));
    expect(goalServiceMock.getGoal).toHaveBeenCalledWith('proj1', 10);
  });

  it('reverse-remove: removes the reference, then reloads to refresh referencedBy', async () => {
    await renderExisting();
    goalServiceMock.getGoal.mockClear();
    commandServiceMock.execute.mockResolvedValue({ success: true, entity: { id: 999 } });
    await comp.onRemoveReferrer({ entityType: 'UseCase', id: 8, name: 'Checkout' });
    expect(commandServiceMock.execute).toHaveBeenCalledWith('RemoveGoalFromGoalContainer', expect.objectContaining({
      projectName: 'proj1', goalContainerId: 8, goalId: 10, containerType: 'UseCase'
    }));
    expect(goalServiceMock.getGoal).toHaveBeenCalledWith('proj1', 10);
  });

  it('links a user-stakeholder referrer to the stakeholders route', async () => {
    await renderExisting();
    // The container type comes through as the concrete interface name (UserStakeholder /
    // NonUserStakeholder), both of which resolve to the shared stakeholders editor route.
    expect(comp.referrerLink({ entityType: 'UserStakeholder', id: 3, name: 'Dr. Smith' }))
      .toEqual(['/projects', 'proj1', 'stakeholders', 3]);
    expect(comp.referrerLink({ entityType: 'NonUserStakeholder', id: 4, name: 'Payer' }))
      .toEqual(['/projects', 'proj1', 'stakeholders', 4]);
  });

  it('keeps the SSE guard from clobbering unsaved edits but still takes the new version', async () => {
    await renderExisting();
    comp.detailsForm.controls.name.setValue('Local edit');
    comp.detailsForm.controls.name.markAsDirty();

    goalServiceMock.getGoal.mockResolvedValue({ ...MOCK_GOAL, name: 'Remote edit', version: 12 });
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    await (comp as any).loadGoal(true);

    expect(comp.detailsForm.controls.name.value).toBe('Local edit');

    commandServiceMock.execute.mockResolvedValue({ success: true, entity: MOCK_GOAL });
    await comp.onSave();
    expect(editGoalCall(0)).toEqual(expect.objectContaining({ version: 12 }));
  });

  // #171: the server caps an artifact name at 255 (ValidationLimits.ARTIFACT_NAME_MAX, applied as
  // @Size on every artifact entity and its Edit*Input). The client mirrors it so an over-long name
  // is reported under the field instead of coming back as a 422 after a round trip.
  describe('name max length (#171)', () => {
    it('accepts a name at the limit and rejects one over it', async () => {
      await renderNew();

      comp.detailsForm.controls.name.setValue('a'.repeat(ARTIFACT_NAME_MAX_LENGTH));
      expect(comp.detailsForm.controls.name.hasError('maxlength')).toBe(false);

      comp.detailsForm.controls.name.setValue('a'.repeat(ARTIFACT_NAME_MAX_LENGTH + 1));
      expect(comp.detailsForm.controls.name.hasError('maxlength')).toBe(true);
    });

    it('exposes the limit for the maxlength attribute', async () => {
      await renderNew();
      // Bound as [attr.maxlength], not [maxlength] -- the latter would register a second
      // MaxLengthValidator on top of the one in the form definition.
      expect(comp.nameMaxLength).toBe(ARTIFACT_NAME_MAX_LENGTH);
    });
  });
  // #185. The gate is the structural half of the fix: with the form absent until the detail GET
  // resolves, there is no input for a user - or a fast e2e test - to type into before the load
  // lands. The dirty-guard in loadGoal() is then belt-and-braces for the background callers.
  // Finishes the #131 / #168 migration for this editor.
  describe('render gate (#185, finishing #131)', () => {
    function el(): HTMLElement {
      return fixture.nativeElement as HTMLElement;
    }

    it('shows the skeleton and no form until the detail GET resolves', async () => {
      let resolveGet: (goal: unknown) => void = () => {};
      goalServiceMock.getGoal.mockImplementation(
        () => new Promise(resolve => { resolveGet = resolve; })
      );

      paramMap$.next(convertToParamMap({ name: 'proj1', goalId: '7' }));
      fixture.detectChanges();
      await flush();
      fixture.detectChanges();

      expect(el().querySelector('[data-testid="goal-editor-loading"]')).not.toBeNull();
      // The point of the gate: nothing to type into yet.
      expect(el().querySelector('[data-testid="goal-name"]')).toBeNull();

      resolveGet(MOCK_GOAL);
      await flush();
      fixture.detectChanges();

      expect(el().querySelector('[data-testid="goal-editor-loading"]')).toBeNull();
      expect(el().querySelector('[data-testid="goal-name"]')).not.toBeNull();
    });

    // The create route never loads, so the gate has to be resolved synchronously in ngOnInit -
    // otherwise the wizard sits behind the skeleton forever and create is unreachable.
    it('renders the create wizard immediately, with no skeleton', async () => {
      fixture.detectChanges();
      await flush();
      fixture.detectChanges();

      expect(comp.loading()).toBe(false);
      expect(el().querySelector('[data-testid="goal-editor-loading"]')).toBeNull();
      expect(el().querySelector('[data-testid="goal-wizard"]')).not.toBeNull();
    });

    it('shows a retryable error state when the load fails, and recovers on retry', async () => {
      goalServiceMock.getGoal.mockRejectedValueOnce(new Error('boom'));

      paramMap$.next(convertToParamMap({ name: 'proj1', goalId: '7' }));
      fixture.detectChanges();
      await flush();
      fixture.detectChanges();

      expect(el().querySelector('[data-testid="goal-editor-load-error"]')).not.toBeNull();
      expect(el().querySelector('[data-testid="goal-name"]')).toBeNull();

      goalServiceMock.getGoal.mockResolvedValue(MOCK_GOAL);
      comp.retryLoad();
      await flush();
      fixture.detectChanges();

      expect(el().querySelector('[data-testid="goal-editor-load-error"]')).toBeNull();
      expect(el().querySelector('[data-testid="goal-name"]')).not.toBeNull();
    });

    // Background callers pass skeleton=false. Blanking the form under a user who is reading it
    // because someone else touched the entity would be its own bug.
    it('does not blank the form for a background reload', async () => {
      paramMap$.next(convertToParamMap({ name: 'proj1', goalId: '7' }));
      fixture.detectChanges();
      await flush();

      let resolveGet: (goal: unknown) => void = () => {};
      goalServiceMock.getGoal.mockImplementation(
        () => new Promise(resolve => { resolveGet = resolve; })
      );
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      const reload = (comp as any).loadGoal(false);
      await flush();
      fixture.detectChanges();

      expect(comp.loading()).toBe(false);
      expect(el().querySelector('[data-testid="goal-name"]')).not.toBeNull();

      resolveGet(MOCK_GOAL);
      await reload;
    });
  });

  // #185 acceptance criterion: one of these per editor with a detail form, including the three
  // #184 fixed. goal-editor's existing guard coverage was the SSE path only
  // ("keeps the SSE guard from clobbering unsaved edits"), which is a different caller - this
  // pins the guard on the *initial* load, where the bug was originally reported.
  //
  // Since the render gate landed, a user cannot actually type during this window - the form is
  // not on screen. The test still earns its place: the guard is the only defence on the SSE,
  // 409 and post-save paths, and holding the GET open is the cheapest way to pin it. It asserts
  // the component contract, not the rendered one.
  it('does not clobber a value typed while the initial load is still in flight', async () => {
    let resolveGet: (entity: unknown) => void = () => {};
    goalServiceMock.getGoal.mockImplementation(
      () => new Promise(resolve => { resolveGet = resolve; })
    );

    paramMap$.next(convertToParamMap({ name: 'proj1', goalId: '10' }));
    fixture.detectChanges();
    await flush();

    comp.detailsForm.controls.name.setValue('Typed while loading');
    comp.detailsForm.controls.name.markAsDirty();

    resolveGet({ ...MOCK_GOAL, version: 9 });
    await flush();

    expect(comp.detailsForm.controls.name.value).toBe('Typed while loading');
    expect(comp.detailsForm.dirty).toBe(true);
    // Server state still landed, so the next save carries a version that will not 409.
      expect(comp.goal()?.id).toBe(10);
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      expect((comp as any).version).toBe(9);
  });

  describe('SSE announcements (#140)', () => {
    it('a cross-session update while the form is dirty shows the banner and announces it', async () => {
      await renderExisting();
      comp.detailsForm.setValue({ name: 'my local edit', text: 'x' });
      comp.detailsForm.markAsDirty();

      events$.next({ eventType: 'Data', targetType: 'Goal', targetId: 10, payload: null });
      await flush();

      expect(comp.updateAvailable()).toBe(true);
      expect(announcerMock.announce).toHaveBeenCalledWith(
        'This goal was changed elsewhere. Your unsaved changes are preserved.');
      fixture.detectChanges();
      expect(fixture.nativeElement.querySelector('[data-testid="goal-update-banner"]')).not.toBeNull();
    });

    it('a cross-session update while the form is clean announces (throttled) and shows no banner', async () => {
      await renderExisting();
      expect(comp.hasUnsavedChanges()).toBe(false);

      events$.next({ eventType: 'Data', targetType: 'Goal', targetId: 10, payload: null });
      await flush();

      expect(comp.updateAvailable()).toBe(false);
      expect(announcerMock.announceThrottled).toHaveBeenCalledWith('Goal:10', 'This goal was updated.');
    });

    it('a TargetDeleted event announces deletion', async () => {
      await renderExisting();
      events$.next({ eventType: 'TargetDeleted', targetType: 'Goal', targetId: 10, payload: null });
      await flush();
      expect(announcerMock.announce).toHaveBeenCalledWith('This goal was deleted in another session.');
    });

    it('reloadFromExternalChange clears the banner and re-applies server state', async () => {
      await renderExisting();
      comp.updateAvailable.set(true);
      comp.detailsForm.setValue({ name: 'my local edit', text: 'x' });
      comp.detailsForm.markAsDirty();
      goalServiceMock.getGoal.mockResolvedValue({ ...MOCK_GOAL, name: 'Server Name', version: 3 });

      await comp.reloadFromExternalChange();
      await flush();

      expect(comp.updateAvailable()).toBe(false);
      expect(comp.detailsForm.getRawValue().name).toBe('Server Name');
    });
  });
});
