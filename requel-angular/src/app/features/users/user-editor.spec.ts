import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter, Router, ActivatedRoute, convertToParamMap } from '@angular/router';
import { BehaviorSubject } from 'rxjs';
import { UserEditorComponent } from './user-editor';
import { UserService } from '../../core/user.service';
import { CommandService } from '../../core/command.service';
import { ARTIFACT_NAME_MAX_LENGTH } from '../../shared/validation-limits';

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

  /** A complete, valid create-mode form. */
  function fillNewUser(overrides: Record<string, unknown> = {}): void {
    comp.form.patchValue({
      username: 'newuser',
      name: 'New User',
      emailAddress: 'new@example.com',
      password: 'hunter2',
      repassword: 'hunter2',
      userRoleNames: ['ProjectUserRole'],
      ...overrides,
    });
    comp.form.markAsDirty();
  }

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
    expect(comp.form.getRawValue().username).toBe('bob');
    expect(comp.form.controls.name.value).toBe('Bob Smith');
    expect(comp.form.controls.userRoleNames.value).toContain('ProjectUserRole');
  });

  it('onSave calls commandService.execute("EditUser") with roles and permissions', async () => {
    fixture.detectChanges();
    await flush();
    fillNewUser();
    comp.permissionsControl('ProjectUserRole').setValue(['editGoals']);

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
    fillNewUser();
    await comp.onSave();
    expect(comp.successMessage()).toBe('User saved successfully.');
    expect(comp.saving()).toBe(false);
  });

  it('onSave navigates to /users/:username for new user after success', async () => {
    fixture.detectChanges();
    await flush();
    fillNewUser({ username: 'brandnewuser' });
    await comp.onSave();
    expect(router.navigate).toHaveBeenCalledWith(['/users', 'brandnewuser']);
  });

  describe('reactive form (issue #132)', () => {
    it('starts invalid on create, so Save is disabled', async () => {
      fixture.detectChanges();
      await flush();
      expect(comp.form.invalid).toBe(true);
      expect(comp.form.pristine).toBe(true);
    });

    it('is valid once every required field is filled', async () => {
      fixture.detectChanges();
      await flush();
      fillNewUser();
      expect(comp.form.valid).toBe(true);
    });

    it.each(['username', 'name'] as const)('requires %s', async field => {
      fixture.detectChanges();
      await flush();
      fillNewUser({ [field]: '' });
      expect(comp.form.controls[field].hasError('required')).toBe(true);
    });

    it('rejects a malformed email', async () => {
      fixture.detectChanges();
      await flush();
      fillNewUser({ emailAddress: 'nope' });
      expect(comp.form.controls.emailAddress.hasError('email')).toBe(true);
    });

    it('requires at least one role', async () => {
      fixture.detectChanges();
      await flush();
      fillNewUser({ userRoleNames: [] });
      expect(comp.form.controls.userRoleNames.hasError('atLeastOne')).toBe(true);
      expect(comp.form.invalid).toBe(true);
    });

    it('loads an existing user pristine, so Save starts disabled', async () => {
      paramMap$.next(convertToParamMap({ username: 'bob' }));
      fixture.detectChanges();
      await flush();
      expect(comp.form.pristine).toBe(true);
      expect(comp.hasUnsavedChanges()).toBe(false);
    });

    it('hasUnsavedChanges() derives from form.dirty', async () => {
      paramMap$.next(convertToParamMap({ username: 'bob' }));
      fixture.detectChanges();
      await flush();

      comp.form.controls.phoneNumber.setValue('555-1111');
      comp.form.controls.phoneNumber.markAsDirty();
      expect(comp.hasUnsavedChanges()).toBe(true);
    });

    it('does not save an invalid form', async () => {
      fixture.detectChanges();
      await flush();
      fillNewUser({ name: '' });
      await comp.onSave();
      expect(commandServiceMock.execute).not.toHaveBeenCalled();
      expect(comp.submitted()).toBe(true);
    });

    it('disables username on edit but still sends it', async () => {
      paramMap$.next(convertToParamMap({ username: 'bob' }));
      fixture.detectChanges();
      await flush();
      expect(comp.form.controls.username.disabled).toBe(true);

      comp.form.controls.name.setValue('Bob Renamed');
      comp.form.markAsDirty();
      await comp.onSave();

      expect(commandServiceMock.execute).toHaveBeenCalledWith(
        'EditUser',
        expect.objectContaining({ username: 'bob' })
      );
    });

    it('leaves username enabled on create', async () => {
      fixture.detectChanges();
      await flush();
      expect(comp.form.controls.username.enabled).toBe(true);
    });

    it('marks pristine after a successful save', async () => {
      paramMap$.next(convertToParamMap({ username: 'bob' }));
      fixture.detectChanges();
      await flush();
      comp.form.controls.name.setValue('Renamed');
      comp.form.markAsDirty();

      await comp.onSave();

      expect(comp.form.pristine).toBe(true);
    });
  });

  describe('password rules (issue #132)', () => {
    it('requires a password when creating', async () => {
      fixture.detectChanges();
      await flush();
      fillNewUser({ password: '', repassword: '' });
      expect(comp.form.controls.password.hasError('required')).toBe(true);
    });

    /**
     * Editing must not force a password change — blank means "keep the current one",
     * matching edit-account and the existing "only include password if set" payload.
     */
    it('does not require a password when editing', async () => {
      paramMap$.next(convertToParamMap({ username: 'bob' }));
      fixture.detectChanges();
      await flush();
      expect(comp.form.controls.password.hasError('required')).toBe(false);
      expect(comp.form.valid).toBe(true);
    });

    it('omits the password from the payload when blank on edit', async () => {
      paramMap$.next(convertToParamMap({ username: 'bob' }));
      fixture.detectChanges();
      await flush();
      comp.form.controls.name.setValue('Renamed');
      comp.form.markAsDirty();

      await comp.onSave();

      const input = commandServiceMock.execute.mock.calls[0][1] as Record<string, unknown>;
      expect('password' in input).toBe(false);
    });

    it('reports a mismatch on the confirm row', async () => {
      fixture.detectChanges();
      await flush();
      fillNewUser({ repassword: 'different' });
      expect(comp.form.controls.repassword.hasError('passwordMismatch')).toBe(true);
      expect(comp.form.invalid).toBe(true);
    });

    it('rejects a password over the server maximum', async () => {
      fixture.detectChanges();
      await flush();
      const pw = 'x'.repeat(129);
      fillNewUser({ password: pw, repassword: pw });
      expect(comp.form.controls.password.hasError('maxlength')).toBe(true);
    });
  });

  describe('roles and permissions (issue #132)', () => {
    it('creates a permission control per role once roles load', async () => {
      fixture.detectChanges();
      await flush();
      for (const role of MOCK_ROLES) {
        expect(comp.permissionsControl(role.roleName)).toBeDefined();
      }
    });

    it('seeds per-role permissions from the loaded user', async () => {
      paramMap$.next(convertToParamMap({ username: 'bob' }));
      fixture.detectChanges();
      await flush();
      expect(comp.permissionsControl('ProjectUserRole').value).toEqual(['editGoals']);
    });

    /**
     * The permission controls live INSIDE the form for exactly this reason: with the
     * checkboxes outside it, a permission-only change would not mark the form dirty and
     * Save would stay disabled with unsaved work on screen.
     */
    it('marks the form dirty when only a permission changes', async () => {
      paramMap$.next(convertToParamMap({ username: 'bob' }));
      fixture.detectChanges();
      await flush();
      expect(comp.form.dirty).toBe(false);

      const permissions = comp.permissionsControl('ProjectUserRole');
      permissions.setValue(['editGoals', 'deleteGoals']);
      permissions.markAsDirty();

      expect(comp.form.dirty).toBe(true);
      expect(comp.hasUnsavedChanges()).toBe(true);
    });

    it('isRoleSelected() reads the userRoleNames control', async () => {
      fixture.detectChanges();
      await flush();
      comp.form.controls.userRoleNames.setValue(['SystemAdminUserRole']);
      expect(comp.isRoleSelected('SystemAdminUserRole')).toBe(true);
      expect(comp.isRoleSelected('ProjectUserRole')).toBe(false);
    });

    it('sends permissions only for the selected roles', async () => {
      fixture.detectChanges();
      await flush();
      fillNewUser();
      comp.permissionsControl('ProjectUserRole').setValue(['editGoals']);
      // Left over from a role the user ticked and then unticked.
      comp.permissionsControl('SystemAdminUserRole').setValue(['somethingStale']);

      await comp.onSave();

      const input = commandServiceMock.execute.mock.calls[0][1] as Record<string, unknown>;
      expect(input['userRolePermissionNames']).toEqual({ ProjectUserRole: ['editGoals'] });
    });

    it('shows the roles error only after a submit attempt', async () => {
      fixture.detectChanges();
      await flush();
      expect(comp.showRolesError()).toBe(false);

      fillNewUser({ userRoleNames: [] });
      await comp.onSave();

      expect(comp.showRolesError()).toBe(true);
      fixture.detectChanges();
      const el = fixture.nativeElement as HTMLElement;
      const error = el.querySelector('[data-testid="user-roles-error"]');
      expect(error).not.toBeNull();
      // Wording comes from the shared map, not from the component.
      expect(error?.textContent?.trim()).toBe('Select at least one.');
    });

    it('keeps a mapped server violation on the roles control across a render', async () => {
      fixture.detectChanges();
      await flush();
      fillNewUser();
      commandServiceMock.execute.mockResolvedValue({
        success: false,
        violations: [{ field: 'userRoleNames', message: 'Role assignment was rejected.' }],
        error: 'Validation failed',
      });

      await comp.onSave();
      // The roles checkboxes bind via [formControl], so a render revalidates the control.
      // A setErrors-written server error was nulled here; a validator-backed one survives.
      fixture.detectChanges();

      expect(comp.form.controls.userRoleNames.errors).toEqual({
        server: 'Role assignment was rejected.',
      });
      expect(
        (fixture.nativeElement as HTMLElement)
          .querySelector('[data-testid="user-roles-error"]')?.textContent?.trim()
      ).toBe('Role assignment was rejected.');
    });

    it('drops that server violation once the roles selection changes', async () => {
      fixture.detectChanges();
      await flush();
      fillNewUser();
      commandServiceMock.execute.mockResolvedValue({
        success: false,
        violations: [{ field: 'userRoleNames', message: 'Role assignment was rejected.' }],
        error: 'Validation failed',
      });
      await comp.onSave();

      comp.form.controls.userRoleNames.setValue(['SystemAdminUserRole']);

      expect(comp.form.controls.userRoleNames.errors).toBeNull();
    });

    it('keeps the roles-section test hooks', async () => {
      fixture.detectChanges();
      await flush();
      fixture.detectChanges();
      const el = fixture.nativeElement as HTMLElement;

      expect(el.querySelector('[data-testid="user-roles-section"]')).not.toBeNull();
      expect(el.querySelectorAll('[data-testid="user-role-group"]').length).toBe(2);
      expect(el.querySelectorAll('[data-testid="user-role-label"]').length).toBe(2);
    });
  });

  describe('two-column layout (issue #172)', () => {
    it('lays the identity fields out in an app-field-group', async () => {
      fixture.detectChanges();
      await flush();
      fixture.detectChanges();
      const el = fixture.nativeElement as HTMLElement;

      const group = el.querySelector<HTMLElement>('.app-field-group');
      expect(group).not.toBeNull();
      expect(group?.style.getPropertyValue('--rq-field-group-columns')).toBe('2');
      expect(el.querySelectorAll('.app-field-group > app-field').length).toBe(7);
    });

    /** Seven rows over two columns: the final row holds one cell and draws no divider. */
    it('suppresses the divider on the partial final row', async () => {
      fixture.detectChanges();
      await flush();
      fixture.detectChanges();
      const rows = Array.from(
        (fixture.nativeElement as HTMLElement).querySelectorAll('.app-field-group > app-field')
      );

      const lastRow = rows.map(r => r.classList.contains('rq-field-cell-last-row'));
      expect(lastRow).toEqual([false, false, false, false, false, false, true]);
    });
  });

  describe('command error handling (issue #132)', () => {
    it('puts a field violation on its control instead of the page message', async () => {
      commandServiceMock.execute.mockResolvedValue({
        success: false,
        violations: [{ field: 'username', message: 'That username is taken.' }],
        error: 'Validation failed',
      });
      fixture.detectChanges();
      await flush();
      fillNewUser();

      await comp.onSave();

      expect(comp.form.controls.username.errors).toEqual({ server: 'That username is taken.' });
      expect(comp.errorMessage()).toBeNull();
    });

    it('routes a userRoleNames violation onto the userRoleNames control (#176)', async () => {
      commandServiceMock.execute.mockResolvedValue({
        success: false,
        violations: [{ field: 'userRoleNames', message: 'At least one role is required.' }],
        error: 'Validation failed',
      });
      fixture.detectChanges();
      await flush();
      fillNewUser();

      await comp.onSave();

      expect(comp.form.controls.userRoleNames.errors).toEqual({
        server: 'At least one role is required.',
      });
    });

    it('shows an unmappable violation page-level rather than dropping it', async () => {
      commandServiceMock.execute.mockResolvedValue({
        success: false,
        violations: [{ field: 'mystery', message: 'Unexpected.' }],
        error: 'Validation failed',
      });
      fixture.detectChanges();
      await flush();
      fillNewUser();

      await comp.onSave();

      expect(comp.errorMessage()).toBe('Unexpected.');
    });

    it('lets a second save through after a server error', async () => {
      commandServiceMock.execute.mockResolvedValue({
        success: false,
        violations: [{ field: 'username', message: 'Taken.' }],
        error: 'Validation failed',
      });
      fixture.detectChanges();
      await flush();
      fillNewUser();
      await comp.onSave();
      expect(comp.form.controls.username.errors?.['server']).toBe('Taken.');

      commandServiceMock.execute.mockResolvedValue({ success: true });
      await comp.onSave();

      expect(comp.form.controls.username.errors).toBeNull();
      expect(commandServiceMock.execute).toHaveBeenCalledTimes(2);
    });

    it('still surfaces a load failure through the retryable error state', async () => {
      userServiceMock.listRoles.mockRejectedValue(new Error('Network down'));
      fixture.detectChanges();
      await flush();

      expect(comp.loadError()).toBe('Network down');
      fixture.detectChanges();
      expect(
        (fixture.nativeElement as HTMLElement).querySelector('[data-testid="user-editor-load-error"]')
      ).not.toBeNull();
    });
  });

  // #171: users.name and users.username share the same varchar(255) column, and UserImpl now
  // carries @Size on both. Mirrored client-side so neither can be typed past what will save.
  describe('name and username max length (#171)', () => {
    it('bounds both identity fields at the shared limit', () => {
      fixture.detectChanges();

      for (const field of ['name', 'username'] as const) {
        const control = comp.form.controls[field];

        control.setValue('a'.repeat(ARTIFACT_NAME_MAX_LENGTH));
        expect(control.hasError('maxlength')).toBe(false);

        control.setValue('a'.repeat(ARTIFACT_NAME_MAX_LENGTH + 1));
        expect(control.hasError('maxlength')).toBe(true);
      }
    });

    it('exposes the limit for the maxlength attribute', () => {
      // Bound as [attr.maxlength], not [maxlength] -- the latter would register a second
      // MaxLengthValidator on top of the one in the form definition.
      expect(comp.nameMaxLength).toBe(ARTIFACT_NAME_MAX_LENGTH);
    });
  });
});
