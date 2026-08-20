import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter, Router, ActivatedRoute, convertToParamMap } from '@angular/router';
import { BehaviorSubject, EMPTY, Subject } from 'rxjs';
import { ConfirmationService, MessageService } from 'primeng/api';
import { StakeholderEditorComponent } from './stakeholder-editor';
import { StakeholderService } from '../../core/stakeholder.service';
import { CommandService } from '../../core/command.service';
import { ProjectService } from '../../core/project.service';
import { UserService } from '../../core/user.service';
import { PermissionService } from '../../core/permission.service';
import { EventStreamService } from '../../core/event-stream.service';

const MOCK_USERS = [
  { id: 1, version: 0, username: 'alice', name: 'Alice', emailAddress: null,
    phoneNumber: null, organizationName: null, roles: [], permissions: [], permissionsByRole: null }
];

const MOCK_AVAILABLE_PERMISSIONS = [
  { entityType: 'Goal', permissionKey: 'edit_goal', permissionType: 'Edit' },
  { entityType: 'Goal', permissionKey: 'delete_goal', permissionType: 'Delete' }
];

const MOCK_STAKEHOLDER_USER = {
  id: 50, version: 0, name: 'Alice', type: 'user',
  goals: [],
  userDetails: { username: 'alice', teamName: 'Dev', permissionKeys: ['edit_goal'], emailAddress: null, phoneNumber: null },
  nonUserDetails: null
};

const MOCK_STAKEHOLDER_NONUSER = {
  id: 51, version: 0, name: 'FASB', type: 'non-user',
  goals: [],
  userDetails: null,
  nonUserDetails: { text: 'Financial authority' }
};

const flush = () => new Promise(r => setTimeout(r, 0));

describe('StakeholderEditorComponent', () => {
  let paramMap$: BehaviorSubject<ReturnType<typeof convertToParamMap>>;
  let stakeholderServiceMock: {
    getStakeholder: ReturnType<typeof vi.fn>;
    getAvailablePermissions: ReturnType<typeof vi.fn>;
  };
  let userServiceMock: { listUsers: ReturnType<typeof vi.fn> };
  let commandServiceMock: { execute: ReturnType<typeof vi.fn> };
  let permissionServiceMock: { loadForProject: ReturnType<typeof vi.fn>; canDelete: ReturnType<typeof vi.fn>; canEdit: ReturnType<typeof vi.fn> };
  let eventStreamServiceMock: { events$: typeof EMPTY; addSubscription: ReturnType<typeof vi.fn>; removeSubscription: ReturnType<typeof vi.fn> };
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  let fixture: any;
  let comp: StakeholderEditorComponent;
  let router: Router;

  beforeEach(() => {
    paramMap$ = new BehaviorSubject(convertToParamMap({ name: 'proj1', stakeholderId: 'new-user' }));

    stakeholderServiceMock = {
      getStakeholder: vi.fn().mockResolvedValue(MOCK_STAKEHOLDER_USER),
      getAvailablePermissions: vi.fn().mockResolvedValue(MOCK_AVAILABLE_PERMISSIONS)
    };
    userServiceMock = { listUsers: vi.fn().mockResolvedValue(MOCK_USERS) };
    commandServiceMock = {
      execute: vi.fn().mockResolvedValue({ success: true, entity: MOCK_STAKEHOLDER_USER })
    };
    permissionServiceMock = {
      loadForProject: vi.fn().mockResolvedValue(undefined),
      canDelete: vi.fn().mockReturnValue(true),
      canEdit: vi.fn().mockReturnValue(true)
    };
    eventStreamServiceMock = {
      events$: EMPTY,
      addSubscription: vi.fn().mockResolvedValue(undefined),
      removeSubscription: vi.fn().mockResolvedValue(undefined)
    };

    TestBed.configureTestingModule({
      imports: [StakeholderEditorComponent],
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { paramMap: paramMap$.asObservable() } },
        { provide: StakeholderService, useValue: stakeholderServiceMock },
        { provide: UserService, useValue: userServiceMock },
        { provide: CommandService, useValue: commandServiceMock },
        { provide: ProjectService, useValue: { notifyTreeChanged: vi.fn() } },
        { provide: PermissionService, useValue: permissionServiceMock },
        { provide: EventStreamService, useValue: eventStreamServiceMock },
        { provide: MessageService, useValue: { add: vi.fn() } }
      ]
    });
    fixture = TestBed.createComponent(StakeholderEditorComponent);
    comp = fixture.componentInstance;
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
  });

  it('isNew() and isUserType() are true for "new-user" param', async () => {
    fixture.detectChanges();
    await flush();
    expect(comp.isNew()).toBe(true);
    expect(comp.isUserType()).toBe(true);
  });

  it('isNew() true and isUserType() false for "new-nonuser" param', async () => {
    paramMap$.next(convertToParamMap({ name: 'proj1', stakeholderId: 'new-nonuser' }));
    fixture.detectChanges();
    await flush();
    expect(comp.isNew()).toBe(true);
    expect(comp.isUserType()).toBe(false);
  });

  it('loadUsers() called and userOptions populated for "new-user"', async () => {
    fixture.detectChanges();
    await flush();
    expect(userServiceMock.listUsers).toHaveBeenCalled();
    expect(comp.userOptions().length).toBe(1);
    expect(comp.userOptions()[0].value).toBe('alice');
  });

  it('onSave calls execute("EditUserStakeholder") for user-type stakeholder', async () => {
    fixture.detectChanges();
    await flush();
    comp.detailsForm.patchValue({ username: 'alice', teamName: 'Engineering' });
    comp.detailsForm.markAsDirty();
    await comp.onSave();
    expect(commandServiceMock.execute).toHaveBeenCalledWith('EditUserStakeholder', expect.objectContaining({
      projectName: 'proj1',
      username: 'alice',
      teamName: 'Engineering'
    }));
  });

  it('onSave calls execute("EditNonUserStakeholder") for non-user-type', async () => {
    paramMap$.next(convertToParamMap({ name: 'proj1', stakeholderId: 'new-nonuser' }));
    fixture.detectChanges();
    await flush();
    comp.detailsForm.patchValue({ name: 'FASB', text: 'Financial authority' });
    comp.detailsForm.markAsDirty();
    await comp.onSave();
    expect(commandServiceMock.execute).toHaveBeenCalledWith('EditNonUserStakeholder', expect.objectContaining({
      projectName: 'proj1',
      name: 'FASB',
      text: 'Financial authority'
    }));
  });

  // #173: permissions moved from mutating perm.checked into their own FormRecord, so dirtiness
  // is form state rather than a string-join comparison against originalPermissionKeys.
  describe('permissions form (#173)', () => {
    it('builds one control per permission key, pristine after load', async () => {
      paramMap$.next(convertToParamMap({ name: 'proj1', stakeholderId: '50' }));
      fixture.detectChanges();
      await flush();

      expect(Object.keys(comp.permissionsForm.controls).sort()).toEqual(['delete_goal', 'edit_goal']);
      expect(comp.permissionControl('edit_goal').value).toBe(true);
      expect(comp.permissionControl('delete_goal').value).toBe(false);
      // Loading is not an edit - otherwise the unsaved-changes guard arms on open.
      expect(comp.permissionsForm.dirty).toBe(false);
      expect(comp.hasUnsavedChanges()).toBe(false);
    });

    it('ticking a permission alone marks the editor dirty', async () => {
      paramMap$.next(convertToParamMap({ name: 'proj1', stakeholderId: '50' }));
      fixture.detectChanges();
      await flush();

      comp.permissionControl('delete_goal').setValue(true);
      comp.permissionControl('delete_goal').markAsDirty();

      expect(comp.detailsForm.dirty).toBe(false);
      expect(comp.hasUnsavedChanges()).toBe(true);
      expect(comp.getSelectedPermissionKeys().sort()).toEqual(['delete_goal', 'edit_goal']);
    });
  });

  // #185. Two forms to protect here rather than one, and an unusually wide window: loadUsers()
  // and loadPermissions() are awaited inside loadStakeholder(), so on the user path the form is
  // typeable across three round trips.
  describe('unsaved edits survive a load (#185)', () => {
    it('does not clobber a value typed while the initial load is still in flight', async () => {
      let resolveGet: (stakeholder: unknown) => void = () => {};
      stakeholderServiceMock.getStakeholder.mockImplementation(
        () => new Promise(resolve => { resolveGet = resolve; })
      );

      paramMap$.next(convertToParamMap({ name: 'proj1', stakeholderId: '51' }));
      fixture.detectChanges();
      await flush();

      // The user is faster than the network.
      comp.detailsForm.controls.name.setValue('Typed while loading');
      comp.detailsForm.controls.name.markAsDirty();

      resolveGet({ ...MOCK_STAKEHOLDER_NONUSER, version: 4 });
      await flush();

      expect(comp.detailsForm.controls.name.value).toBe('Typed while loading');
      expect(comp.detailsForm.dirty).toBe(true);
      // Server state still landed, so Save has a usable version to send.
      expect(comp.isUserType()).toBe(false);
    });

    // The distinguishing case for this editor: loadPermissions() rebuilds the checkbox controls
    // from the server's key set, so running it over a half-ticked matrix silently reverts the
    // ticks. detailsForm is pristine throughout - only permissionsForm is dirty.
    it('does not revert permission ticks when an SSE refresh lands', async () => {
      const events$ = new Subject<{ targetType: string; targetId: number }>();
      eventStreamServiceMock.events$ = events$.asObservable() as typeof EMPTY;

      paramMap$.next(convertToParamMap({ name: 'proj1', stakeholderId: '50' }));
      fixture.detectChanges();
      await flush();

      comp.permissionControl('delete_goal').setValue(true);
      comp.permissionControl('delete_goal').markAsDirty();
      expect(comp.detailsForm.dirty).toBe(false);

      // A refresh whose server state still has only edit_goal selected.
      stakeholderServiceMock.getStakeholder.mockResolvedValue({
        ...MOCK_STAKEHOLDER_USER,
        version: 7,
        goals: [{ id: 1, name: 'Added elsewhere', entityType: 'Goal' }]
      });

      events$.next({ targetType: 'Stakeholder', targetId: 50 });
      await flush();

      // The tick survives...
      expect(comp.permissionControl('delete_goal').value).toBe(true);
      expect(comp.permissionsForm.dirty).toBe(true);
      // ...while the goals table still refreshed, rather than sitting stale behind the edit.
      expect(comp.goals().length).toBe(1);
    });
  });

  // The mode's unused half is disabled, so its required validator cannot block Continue for a
  // mode that never shows the field.
  describe('mode switching (#173)', () => {
    it('user mode enables username/team and disables name/text', async () => {
      fixture.detectChanges();
      await flush();
      expect(comp.detailsForm.controls.username.enabled).toBe(true);
      expect(comp.detailsForm.controls.name.disabled).toBe(true);
    });

    it('non-user mode enables name/text and disables username/team', async () => {
      paramMap$.next(convertToParamMap({ name: 'proj1', stakeholderId: 'new-nonuser' }));
      fixture.detectChanges();
      await flush();
      expect(comp.detailsForm.controls.name.enabled).toBe(true);
      expect(comp.detailsForm.controls.username.disabled).toBe(true);
      // A blank username must not make the non-user form invalid.
      comp.detailsForm.controls.name.setValue('FASB');
      expect(comp.detailsForm.valid).toBe(true);
    });
  });

  // #173 required test (§10.3), updated for #180: goal associations bump the stakeholder's @Version
  // and now return the merged stakeholder, so the wizard reads the new version from the response
  // instead of refetching. Without that, returning to Details would 409.
  describe('wizard version contract (#173)', () => {
    it('survives create -> add goal -> back to Details -> edit -> Continue', async () => {
      paramMap$.next(convertToParamMap({ name: 'proj1', stakeholderId: 'new-nonuser' }));
      fixture.detectChanges();
      await flush();

      let version = 0;
      commandServiceMock.execute.mockImplementation(async () => {
        // Every command here merges the stakeholder and returns it with a bumped @Version (#180).
        version += 1;
        return { success: true, entity: { ...MOCK_STAKEHOLDER_NONUSER, id: 51, version } };
      });
      // saveDetails() still refetches once right after CREATE (loadStakeholder(false)) - that is a
      // separate load from the association refetch #180 removes, so this mock must stay or the
      // reload falls back to the default (a user stakeholder) and flips the editor's mode.
      stakeholderServiceMock.getStakeholder.mockImplementation(async () => ({
        ...MOCK_STAKEHOLDER_NONUSER, id: 51, version
      }));

      comp.detailsForm.controls.name.setValue('FASB');

      const step1 = { step: { key: 'details' }, complete: vi.fn(), fail: vi.fn() };
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      await comp.onStepCommit(step1 as any);
      expect(step1.complete).toHaveBeenCalled();

      await comp.onGoalSelected({ id: 7, name: 'Avoid late fees', entityType: 'Goal' });

      comp.detailsForm.controls.name.setValue('FASB Renamed');
      const step3 = { step: { key: 'details' }, complete: vi.fn(), fail: vi.fn() };
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      await comp.onStepCommit(step3 as any);

      expect(step3.complete).toHaveBeenCalled();
      expect(step3.fail).not.toHaveBeenCalled();

      const edits = commandServiceMock.execute.mock.calls.filter(c => c[0] === 'EditNonUserStakeholder');
      const last = edits[edits.length - 1][1];
      expect(last.name).toBe('FASB Renamed');
      // Not the version captured at step 1 - that is the 409 this guards.
      expect(last.version).not.toBe(1);
    });

    it('takes version from the association response', async () => {
      paramMap$.next(convertToParamMap({ name: 'proj1', stakeholderId: '50' }));
      fixture.detectChanges();
      await flush();
      stakeholderServiceMock.getStakeholder.mockClear();

      commandServiceMock.execute.mockResolvedValue({ success: true, entity: { ...MOCK_STAKEHOLDER_USER, version: 6 } });

      await comp.onGoalSelected({ id: 7, name: 'Avoid late fees', entityType: 'Goal' });
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      expect((comp as any).version).toBe(6);
      expect(stakeholderServiceMock.getStakeholder).not.toHaveBeenCalled();
    });
  });

  it('canDelete() set from permissionService on init', async () => {
    fixture.detectChanges();
    await flush();
    expect(permissionServiceMock.canDelete).toHaveBeenCalledWith('Stakeholder');
    expect(comp.canDelete()).toBe(true);
  });

  it('canEditGoals() delegates to permissionService.canEdit("Goal")', async () => {
    fixture.detectChanges();
    await flush();
    permissionServiceMock.canEdit.mockClear();
    const result = comp.canEditGoals();
    expect(permissionServiceMock.canEdit).toHaveBeenCalledWith('Goal');
    expect(result).toBe(true);
  });

  it('loads existing user stakeholder: goals() and loadedUserDetails() populated', async () => {
    const stakeholderWithGoals = {
      ...MOCK_STAKEHOLDER_USER,
      goals: [{ id: 10, name: 'Buy product', entityType: 'Goal' }]
    };
    stakeholderServiceMock.getStakeholder.mockResolvedValue(stakeholderWithGoals);
    paramMap$.next(convertToParamMap({ name: 'proj1', stakeholderId: '50' }));
    fixture.detectChanges();
    await flush();
    expect(stakeholderServiceMock.getStakeholder).toHaveBeenCalledWith('proj1', 50);
    expect(comp.goals().length).toBe(1);
    expect(comp.loadedUserDetails()).not.toBeNull();
    expect(comp.loadedUserDetails()?.username).toBe('alice');
  });

  it('loads existing non-user stakeholder: isUserType() false and stakeholderName() set', async () => {
    stakeholderServiceMock.getStakeholder.mockResolvedValue(MOCK_STAKEHOLDER_NONUSER);
    paramMap$.next(convertToParamMap({ name: 'proj1', stakeholderId: '51' }));
    fixture.detectChanges();
    await flush();
    expect(comp.isUserType()).toBe(false);
    expect(comp.stakeholderName()).toBe('FASB');
  });

  it('onGoalSelected calls AddGoalToGoalContainer and applies the returned stakeholder to goals()', async () => {
    paramMap$.next(convertToParamMap({ name: 'proj1', stakeholderId: '50' }));
    fixture.detectChanges();
    await flush();
    stakeholderServiceMock.getStakeholder.mockClear();
    commandServiceMock.execute.mockResolvedValue({
      success: true, entity: { ...MOCK_STAKEHOLDER_USER, version: 1, goals: [{ id: 10, name: 'Buy product', entityType: 'Goal' }] }
    });
    await comp.onGoalSelected({ id: 10, name: 'Buy product', entityType: 'Goal' });
    expect(commandServiceMock.execute).toHaveBeenCalledWith('AddGoalToGoalContainer', expect.objectContaining({
      projectName: 'proj1',
      goalContainerId: 50,
      goalId: 10,
      containerType: 'Stakeholder'
    }));
    expect(comp.goals().some(g => g.id === 10)).toBe(true);
    expect(stakeholderServiceMock.getStakeholder).not.toHaveBeenCalled();
  });

  it('onRemoveGoal calls RemoveGoalFromGoalContainer and removes from goals()', async () => {
    const stakeholderWithGoals = {
      ...MOCK_STAKEHOLDER_USER,
      goals: [{ id: 10, name: 'Buy product', entityType: 'Goal' }]
    };
    stakeholderServiceMock.getStakeholder.mockResolvedValue(stakeholderWithGoals);
    paramMap$.next(convertToParamMap({ name: 'proj1', stakeholderId: '50' }));
    fixture.detectChanges();
    await flush();
    commandServiceMock.execute.mockResolvedValue({ success: true, entity: { ...MOCK_STAKEHOLDER_USER, version: 1, goals: [] } });
    await comp.onRemoveGoal({ id: 10, name: 'Buy product', entityType: 'Goal' });
    expect(commandServiceMock.execute).toHaveBeenCalledWith('RemoveGoalFromGoalContainer', expect.objectContaining({
      goalId: 10,
      containerType: 'Stakeholder'
    }));
    expect(comp.goals().some(g => g.id === 10)).toBe(false);
  });

  it('onGoalClick navigates to goal editor', () => {
    comp.projectName = 'proj1';
    comp.onGoalClick({ id: 10, name: 'Buy product', entityType: 'Goal' });
    expect(router.navigate).toHaveBeenCalledWith(['/projects', 'proj1', 'goals', 10]);
  });

  it('onDelete confirms and calls DeleteStakeholder then navigates', async () => {
    paramMap$.next(convertToParamMap({ name: 'proj1', stakeholderId: '50' }));
    fixture.detectChanges();
    await flush();
    commandServiceMock.execute.mockResolvedValue({ success: true });
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const cs = fixture.debugElement.injector.get(ConfirmationService);
    vi.spyOn(cs, 'confirm').mockImplementation((conf: any) => conf.accept?.());
    comp.onDelete();
    await flush();
    expect(commandServiceMock.execute).toHaveBeenCalledWith('DeleteStakeholder',
      expect.objectContaining({ stakeholderId: 50 }));
    expect(router.navigate).toHaveBeenCalled();
  });
  // #185. The gate is the structural half of the fix: with the form absent until the detail GET
  // resolves, there is no input for a user - or a fast e2e test - to type into before the load
  // lands. The dirty-guard in loadStakeholder() is then belt-and-braces for the background callers.
  // Finishes the #131 / #168 migration for this editor.
  describe('render gate (#185, finishing #131)', () => {
    function el(): HTMLElement {
      return fixture.nativeElement as HTMLElement;
    }

    it('shows the skeleton and no form until the detail GET resolves', async () => {
      let resolveGet: (entity: unknown) => void = () => {};
      stakeholderServiceMock.getStakeholder.mockImplementation(
        () => new Promise(resolve => { resolveGet = resolve; })
      );

      paramMap$.next(convertToParamMap({ name: 'proj1', stakeholderId: '51' }));
      fixture.detectChanges();
      await flush();
      fixture.detectChanges();

      expect(el().querySelector('[data-testid="stakeholder-editor-loading"]')).not.toBeNull();
      // The point of the gate: nothing to type into yet.
      expect(el().querySelector('[data-testid="stakeholder-name"]')).toBeNull();

      resolveGet(MOCK_STAKEHOLDER_NONUSER);
      await flush();
      fixture.detectChanges();

      expect(el().querySelector('[data-testid="stakeholder-editor-loading"]')).toBeNull();
      expect(el().querySelector('[data-testid="stakeholder-name"]')).not.toBeNull();
    });

    // The create route never loads, so the gate has to be resolved there explicitly - otherwise
    // the wizard sits behind the skeleton forever and create becomes unreachable.
    it('renders the create wizard, with no skeleton', async () => {
      paramMap$.next(convertToParamMap({ name: 'proj1', stakeholderId: 'new-nonuser' }));
      fixture.detectChanges();
      await flush();
      fixture.detectChanges();

      expect(comp.loading()).toBe(false);
      expect(el().querySelector('[data-testid="stakeholder-editor-loading"]')).toBeNull();
      expect(el().querySelector('[data-testid="stakeholder-wizard"]')).not.toBeNull();
    });

    it('shows a retryable error state when the load fails, and recovers on retry', async () => {
      stakeholderServiceMock.getStakeholder.mockRejectedValueOnce(new Error('boom'));

      paramMap$.next(convertToParamMap({ name: 'proj1', stakeholderId: '51' }));
      fixture.detectChanges();
      await flush();
      fixture.detectChanges();

      expect(el().querySelector('[data-testid="stakeholder-editor-load-error"]')).not.toBeNull();
      expect(el().querySelector('[data-testid="stakeholder-name"]')).toBeNull();

      stakeholderServiceMock.getStakeholder.mockResolvedValue(MOCK_STAKEHOLDER_NONUSER);
      comp.retryLoad();
      await flush();
      fixture.detectChanges();

      expect(el().querySelector('[data-testid="stakeholder-editor-load-error"]')).toBeNull();
      expect(el().querySelector('[data-testid="stakeholder-name"]')).not.toBeNull();
    });

    // Background callers pass skeleton=false. Blanking the form under a user who is reading it
    // because someone else touched the entity would be its own bug.
    it('does not blank the form for a background reload', async () => {
      // Explicitly a non-user stakeholder: the default mock is the user type, whose Details step
      // renders username/team instead of the name field this test asserts on.
      stakeholderServiceMock.getStakeholder.mockResolvedValue(MOCK_STAKEHOLDER_NONUSER);
      paramMap$.next(convertToParamMap({ name: 'proj1', stakeholderId: '51' }));
      fixture.detectChanges();
      await flush();

      let resolveGet: (entity: unknown) => void = () => {};
      stakeholderServiceMock.getStakeholder.mockImplementation(
        () => new Promise(resolve => { resolveGet = resolve; })
      );
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      const reload = (comp as any).loadStakeholder(false);
      await flush();
      fixture.detectChanges();

      expect(comp.loading()).toBe(false);
      expect(el().querySelector('[data-testid="stakeholder-name"]')).not.toBeNull();

      resolveGet(MOCK_STAKEHOLDER_NONUSER);
      await reload;
    });
  });

});
