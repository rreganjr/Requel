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
});
