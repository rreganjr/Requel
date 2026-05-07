import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter, Router, ActivatedRoute, convertToParamMap } from '@angular/router';
import { BehaviorSubject } from 'rxjs';
import { UserEditorComponent } from './user-editor';
import { UserService } from '../../core/user.service';
import { CommandService } from '../../core/command.service';

const MOCK_ROLES = [
  { roleName: 'ProjectUserRole', displayName: 'Project User',
    availablePermissions: [{ name: 'editGoals' }, { name: 'deleteGoals' }] },
  { roleName: 'SystemAdminUserRole', displayName: 'System Admin', availablePermissions: [] }
];

const MOCK_ORGS = [{ id: 1, name: 'Acme' }];

const MOCK_USER = {
  id: 1, version: 0, username: 'bob', name: 'Bob Smith',
  emailAddress: 'bob@example.com', phoneNumber: null, organizationName: 'Acme',
  roles: ['ProjectUserRole'], permissions: ['editGoals'],
  permissionsByRole: { ProjectUserRole: ['editGoals'] }
};

const flush = () => new Promise(r => setTimeout(r, 0));

describe('UserEditorComponent', () => {
  let paramMap$: BehaviorSubject<ReturnType<typeof convertToParamMap>>;
  let userServiceMock: {
    listRoles: ReturnType<typeof vi.fn>;
    listOrganizations: ReturnType<typeof vi.fn>;
    getUser: ReturnType<typeof vi.fn>;
  };
  let commandServiceMock: { execute: ReturnType<typeof vi.fn> };
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  let fixture: any;
  let comp: UserEditorComponent;
  let router: Router;

  beforeEach(() => {
    paramMap$ = new BehaviorSubject(convertToParamMap({ username: 'new' }));

    userServiceMock = {
      listRoles: vi.fn().mockResolvedValue(MOCK_ROLES),
      listOrganizations: vi.fn().mockResolvedValue(MOCK_ORGS),
      getUser: vi.fn().mockResolvedValue(MOCK_USER)
    };
    commandServiceMock = {
      execute: vi.fn().mockResolvedValue({ success: true })
    };

    TestBed.configureTestingModule({
      imports: [UserEditorComponent],
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { paramMap: paramMap$.asObservable() } },
        { provide: UserService, useValue: userServiceMock },
        { provide: CommandService, useValue: commandServiceMock }
      ]
    });
    fixture = TestBed.createComponent(UserEditorComponent);
    comp = fixture.componentInstance;
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
  });

  it('isNew() is true when username param is "new"', async () => {
    fixture.detectChanges();
    await flush();
    expect(comp.isNew()).toBe(true);
  });

  it('loads roles and organizations on init', async () => {
    fixture.detectChanges();
    await flush();
    expect(userServiceMock.listRoles).toHaveBeenCalled();
    expect(userServiceMock.listOrganizations).toHaveBeenCalled();
    expect(comp.availableRoles().length).toBe(2);
    expect(comp.orgOptions().length).toBe(1);
  });

  it('loads existing user data when username is not "new"', async () => {
    paramMap$.next(convertToParamMap({ username: 'bob' }));
    fixture.detectChanges();
    await flush();
    expect(userServiceMock.getUser).toHaveBeenCalledWith('bob');
    expect(comp.username).toBe('bob');
    expect(comp.name).toBe('Bob Smith');
    expect(comp.selectedRoleNames).toContain('ProjectUserRole');
  });

  it('onSave calls commandService.execute("EditUser") with roles and permissions', async () => {
    fixture.detectChanges();
    await flush();
    comp.username = 'newuser';
    comp.name = 'New User';
    comp.selectedRoleNames = ['ProjectUserRole'];
    comp.selectedPermissions = { ProjectUserRole: ['editGoals'] };
    await comp.onSave();
    expect(commandServiceMock.execute).toHaveBeenCalledWith('EditUser', expect.objectContaining({
      username: 'newuser',
      name: 'New User',
      userRoleNames: ['ProjectUserRole'],
      userRolePermissionNames: { ProjectUserRole: ['editGoals'] }
    }));
  });

  it('onSave sets successMessage when save succeeds', async () => {
    fixture.detectChanges();
    await flush();
    comp.username = 'testuser';
    await comp.onSave();
    expect(comp.successMessage()).toBe('User saved successfully.');
    expect(comp.saving()).toBe(false);
  });

  it('onSave navigates to /users/:username for new user after success', async () => {
    fixture.detectChanges();
    await flush();
    comp.username = 'brandnewuser';
    await comp.onSave();
    expect(router.navigate).toHaveBeenCalledWith(['/users', 'brandnewuser']);
  });
});
