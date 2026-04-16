import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter, Router, ActivatedRoute, convertToParamMap } from '@angular/router';
import { BehaviorSubject, EMPTY } from 'rxjs';
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
    comp.username = 'alice';
    comp.teamName = 'Engineering';
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
    comp.stakeholderName.set('FASB');
    comp.text = 'Financial authority';
    await comp.onSave();
    expect(commandServiceMock.execute).toHaveBeenCalledWith('EditNonUserStakeholder', expect.objectContaining({
      projectName: 'proj1',
      name: 'FASB',
      text: 'Financial authority'
    }));
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

  it('onGoalSelected calls AddGoalToGoalContainer and updates goals()', async () => {
    paramMap$.next(convertToParamMap({ name: 'proj1', stakeholderId: '50' }));
    fixture.detectChanges();
    await flush();
    commandServiceMock.execute.mockResolvedValue({ success: true });
    await comp.onGoalSelected({ id: 10, name: 'Buy product', entityType: 'Goal' });
    expect(commandServiceMock.execute).toHaveBeenCalledWith('AddGoalToGoalContainer', expect.objectContaining({
      projectName: 'proj1',
      goalContainerId: 50,
      goalId: 10,
      containerType: 'Stakeholder'
    }));
    expect(comp.goals().some(g => g.id === 10)).toBe(true);
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
    commandServiceMock.execute.mockResolvedValue({ success: true });
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
});
