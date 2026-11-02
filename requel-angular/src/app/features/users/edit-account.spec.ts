import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { signal } from '@angular/core';
import { provideRouter } from '@angular/router';
import { EditAccountComponent } from './edit-account';
import { AuthService } from '../../core/auth.service';
import { CommandService } from '../../core/command.service';
import { UserService } from '../../core/user.service';

const MOCK_USER = {
  id: 1, version: 0, username: 'admin', name: 'Admin User',
  emailAddress: 'admin@example.com', phoneNumber: '555-1234',
  organizationName: 'Acme', roles: ['SystemAdminUserRole'], permissions: [],
  permissionsByRole: null
};

const MOCK_ORGS = [{ id: 1, name: 'Acme' }, { id: 2, name: 'Beta Corp' }];

describe('EditAccountComponent', () => {
  let authServiceMock: { user: ReturnType<typeof signal> };
  let commandServiceMock: { execute: ReturnType<typeof vi.fn> };
  let userServiceMock: { listOrganizations: ReturnType<typeof vi.fn> };
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  let fixture: any;
  let comp: EditAccountComponent;

  beforeEach(() => {
    authServiceMock = { user: signal(MOCK_USER) };
    commandServiceMock = { execute: vi.fn().mockResolvedValue({ success: true }) };
    userServiceMock = { listOrganizations: vi.fn().mockResolvedValue(MOCK_ORGS) };

    TestBed.configureTestingModule({
      imports: [EditAccountComponent],
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        { provide: AuthService, useValue: authServiceMock },
        { provide: CommandService, useValue: commandServiceMock },
        { provide: UserService, useValue: userServiceMock }
      ]
    });
    fixture = TestBed.createComponent(EditAccountComponent);
    comp = fixture.componentInstance;
  });

  /** Edit the form the way a user would, leaving it dirty. */
  function patch(values: Partial<{
    name: string; emailAddress: string; phoneNumber: string;
    organizationName: string;
  }>): void {
    comp.form.patchValue(values);
    comp.form.markAsDirty();
  }

  it('username() is computed from authService.user().username', () => {
    expect(comp.username()).toBe('admin');
  });

  it('ngOnInit populates fields from authService.user()', async () => {
    fixture.detectChanges();
    await fixture.whenStable();
    expect(comp.form.getRawValue()).toMatchObject({
      username: 'admin',
      name: 'Admin User',
      emailAddress: 'admin@example.com',
      phoneNumber: '555-1234',
      organizationName: 'Acme',
    });
  });

  it('ngOnInit calls userService.listOrganizations', async () => {
    fixture.detectChanges();
    await fixture.whenStable();
    expect(userServiceMock.listOrganizations).toHaveBeenCalled();
    expect(comp.orgOptions().length).toBe(2);
  });

  it('onSave calls commandService.execute("EditUser") with username and fields', async () => {
    fixture.detectChanges();
    await fixture.whenStable();
    patch({ name: 'New Name', emailAddress: 'new@example.com', phoneNumber: '', organizationName: 'Beta' });
    await comp.onSave();
    expect(commandServiceMock.execute).toHaveBeenCalledWith('EditUser', expect.objectContaining({
      username: 'admin',
      name: 'New Name',
      emailAddress: 'new@example.com'
    }));
  });

  it('onSave sets successMessage on success', async () => {
    fixture.detectChanges();
    await fixture.whenStable();
    patch({ name: 'Updated' });
    await comp.onSave();
    expect(comp.successMessage()).toBe('Account updated.');
    expect(comp.saving()).toBe(false);
  });

  it('onSave sets errorMessage when command fails', async () => {
    commandServiceMock.execute.mockResolvedValue({ success: false, error: 'Permission denied' });
    fixture.detectChanges();
    await fixture.whenStable();
    patch({ name: 'Updated' });
    await comp.onSave();
    expect(comp.errorMessage()).toBe('Permission denied');
    expect(comp.saving()).toBe(false);
  });

  describe('reactive form (issue #132)', () => {
    beforeEach(async () => {
      fixture.detectChanges();
      await fixture.whenStable();
    });

    it('starts valid and pristine, so Save is disabled', () => {
      expect(comp.form.valid).toBe(true);
      expect(comp.form.pristine).toBe(true);
      expect(comp.hasUnsavedChanges()).toBe(false);
    });

    it('keeps username disabled but still sends it', async () => {
      expect(comp.form.controls.username.disabled).toBe(true);
      patch({ name: 'Updated' });
      await comp.onSave();
      expect(commandServiceMock.execute).toHaveBeenCalledWith(
        'EditUser',
        expect.objectContaining({ username: 'admin' })
      );
    });

    it('requires a name', () => {
      patch({ name: '' });
      expect(comp.form.controls.name.hasError('required')).toBe(true);
      expect(comp.form.invalid).toBe(true);
    });

    it('rejects a malformed email', () => {
      patch({ emailAddress: 'not-an-email' });
      expect(comp.form.controls.emailAddress.hasError('email')).toBe(true);
    });

    // #398: the server requires an email address and an organization.
    it.each(['emailAddress', 'organizationName'] as const)('requires %s', field => {
      patch({ [field]: '' });
      expect(comp.form.controls[field].hasError('required')).toBe(true);
    });

    it('hasUnsavedChanges() derives from form.dirty', () => {
      patch({ phoneNumber: '555-9999' });
      expect(comp.hasUnsavedChanges()).toBe(true);
    });

    it('marks pristine after a successful save', async () => {
      patch({ name: 'Updated' });
      await comp.onSave();
      expect(comp.form.pristine).toBe(true);
    });

    it('does not save an invalid form', async () => {
      patch({ name: '' });
      await comp.onSave();
      expect(commandServiceMock.execute).not.toHaveBeenCalled();
      expect(comp.submitted()).toBe(true);
    });

    it('disables Save while pristine and enables it once dirty', () => {
      const save = () =>
        (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>(
          '[data-testid="account-save"] button'
        );
      fixture.detectChanges();
      expect(save()?.disabled).toBe(true);

      patch({ name: 'Changed' });
      fixture.detectChanges();
      expect(save()?.disabled).toBe(false);
    });

    it('keeps the ids the e2e page objects locate', () => {
      fixture.detectChanges();
      const el = fixture.nativeElement as HTMLElement;

      expect(el.querySelector('input#username')).not.toBeNull();
      expect(el.querySelector('input#name')).not.toBeNull();
      expect(el.querySelector('input#email')).not.toBeNull();
    });
  });

  describe('password (issue #446)', () => {
    beforeEach(async () => {
      fixture.detectChanges();
      await fixture.whenStable();
    });

    it('has no password rows; your own password changes on the Change Password page', () => {
      const el = fixture.nativeElement as HTMLElement;
      expect(el.querySelector('#password')).toBeNull();
      expect(el.querySelector('#repassword')).toBeNull();
      expect('password' in comp.form.controls).toBe(false);

      const link = el.querySelector<HTMLAnchorElement>('[data-testid="account-change-password"]');
      expect(link).not.toBeNull();
      expect(link!.getAttribute('href')).toBe('/change-password');
    });

    it('never sends a password', async () => {
      patch({ name: 'Updated' });
      await comp.onSave();

      const input = commandServiceMock.execute.mock.calls[0][1] as Record<string, unknown>;
      expect('password' in input).toBe(false);
      expect('repassword' in input).toBe(false);
    });
  });

  describe('command error handling (issue #132)', () => {
    beforeEach(async () => {
      fixture.detectChanges();
      await fixture.whenStable();
    });

    it('puts a field violation on its control instead of the page message', async () => {
      commandServiceMock.execute.mockResolvedValue({
        success: false,
        violations: [{ field: 'emailAddress', message: 'That address is already in use.' }],
        error: 'Validation failed',
      });
      patch({ name: 'Updated' });

      await comp.onSave();

      expect(comp.form.controls.emailAddress.errors).toEqual({
        server: 'That address is already in use.',
      });
      expect(comp.errorMessage()).toBeNull();
    });

    it('shows a password violation page-level, since there is no password row (#446)', async () => {
      commandServiceMock.execute.mockResolvedValue({
        success: false,
        violations: [{ field: 'password',
          message: 'validation failed: Change your own password on the Change Password page.' }],
        error: 'Validation failed',
      });
      patch({ name: 'Updated' });

      await comp.onSave();

      expect(comp.errorMessage()).toContain('Change Password page');
    });

    /**
     * Several command-level messages share the one banner, so the separator matters.
     * Regression: #132 briefly joined with a space, which turned two fragments into one
     * broken sentence ("Email is invalid Phone is required") — caught by
     * e2e/account.e2e.ts, not by any unit test, hence this one.
     */
    it('joins several command-level violations readably', async () => {
      commandServiceMock.execute.mockResolvedValue({
        success: false,
        violations: [
          { field: null, message: 'Email is invalid' },
          { field: null, message: 'Phone is required' },
        ],
        error: null,
      });
      patch({ name: 'Updated' });

      await comp.onSave();

      expect(comp.errorMessage()).toBe('Email is invalid; Phone is required');
    });

    it('treats a violation with no field at all as command-level', async () => {
      // The server omits `field` entirely rather than sending null — same handling.
      commandServiceMock.execute.mockResolvedValue({
        success: false,
        violations: [{ message: 'Email is invalid' }, { message: 'Phone is required' }],
        error: null,
      });
      patch({ name: 'Updated' });

      await comp.onSave();

      expect(comp.errorMessage()).toBe('Email is invalid; Phone is required');
    });

    it('shows an unmappable violation page-level rather than dropping it', async () => {
      commandServiceMock.execute.mockResolvedValue({
        success: false,
        violations: [{ field: 'somethingElse', message: 'Unexpected.' }],
        error: 'Validation failed',
      });
      patch({ name: 'Updated' });

      await comp.onSave();

      expect(comp.errorMessage()).toBe('Unexpected.');
    });

    it('lets a second save through after a server error, rather than deadlocking', async () => {
      commandServiceMock.execute.mockResolvedValue({
        success: false,
        violations: [{ field: 'emailAddress', message: 'Taken.' }],
        error: 'Validation failed',
      });
      patch({ name: 'Updated' });
      await comp.onSave();
      expect(comp.form.controls.emailAddress.errors?.['server']).toBe('Taken.');

      commandServiceMock.execute.mockResolvedValue({ success: true });
      await comp.onSave();

      expect(comp.form.controls.emailAddress.errors).toBeNull();
      expect(commandServiceMock.execute).toHaveBeenCalledTimes(2);
    });
  });
});
