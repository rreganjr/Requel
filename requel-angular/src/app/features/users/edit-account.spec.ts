import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { signal } from '@angular/core';
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
    organizationName: string; password: string; repassword: string;
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

    it('accepts an empty email', () => {
      patch({ emailAddress: '' });
      expect(comp.form.controls.emailAddress.valid).toBe(true);
    });

    it('hasUnsavedChanges() derives from form.dirty', () => {
      patch({ phoneNumber: '555-9999' });
      expect(comp.hasUnsavedChanges()).toBe(true);
    });

    it('marks pristine and clears the password rows after a successful save', async () => {
      patch({ name: 'Updated', password: 'hunter2', repassword: 'hunter2' });
      await comp.onSave();

      expect(comp.form.controls.password.value).toBe('');
      expect(comp.form.controls.repassword.value).toBe('');
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

    it('keeps the ids the e2e page objects locate, with #password on the real input', () => {
      fixture.detectChanges();
      const el = fixture.nativeElement as HTMLElement;

      expect(el.querySelector('input#username')).not.toBeNull();
      expect(el.querySelector('input#name')).not.toBeNull();
      expect(el.querySelector('input#email')).not.toBeNull();
      // The id lands on the inner input, not the p-password host — the e2e locators
      // changed from .locator('#password').locator('input') to .locator('#password').
      expect(el.querySelector('#password')?.tagName.toLowerCase()).toBe('input');
      expect(el.querySelector('#repassword')?.tagName.toLowerCase()).toBe('input');
    });
  });

  describe('optional password (issue #132)', () => {
    beforeEach(async () => {
      fixture.detectChanges();
      await fixture.whenStable();
    });

    /**
     * "Leave blank to keep current" needs no conditional branch: Angular's minLength
     * returns null for an empty value, and passwordsMatch is satisfied when both rows
     * are empty. This asserts that reading of the semantics, since it is the reason
     * there is no isNew-style flag here.
     */
    it('is valid with both password rows blank', () => {
      patch({ name: 'Updated' });
      expect(comp.form.valid).toBe(true);
    });

    it('omits password from the payload when blank', async () => {
      patch({ name: 'Updated' });
      await comp.onSave();

      const input = commandServiceMock.execute.mock.calls[0][1] as Record<string, unknown>;
      expect('password' in input).toBe(false);
      expect('repassword' in input).toBe(false);
    });

    it('includes password and repassword when set', async () => {
      patch({ name: 'Updated', password: 'hunter2', repassword: 'hunter2' });
      await comp.onSave();

      expect(commandServiceMock.execute).toHaveBeenCalledWith(
        'EditUser',
        expect.objectContaining({ password: 'hunter2', repassword: 'hunter2' })
      );
    });

    it('reports a mismatch on the confirm row and blocks the save', async () => {
      patch({ name: 'Updated', password: 'hunter2', repassword: 'hunter3' });

      expect(comp.form.controls.repassword.hasError('passwordMismatch')).toBe(true);
      expect(comp.form.invalid).toBe(true);

      await comp.onSave();
      expect(commandServiceMock.execute).not.toHaveBeenCalled();
    });

    it('clears the mismatch when the confirm row catches up', () => {
      patch({ password: 'hunter2', repassword: 'hunter3' });
      comp.form.controls.repassword.setValue('hunter2');
      expect(comp.form.controls.repassword.errors).toBeNull();
    });

    it('re-flags a mismatch when the password changes after confirming', () => {
      patch({ name: 'Updated', password: 'hunter2', repassword: 'hunter2' });
      expect(comp.form.valid).toBe(true);

      comp.form.controls.password.setValue('hunter3');
      expect(comp.form.controls.repassword.hasError('passwordMismatch')).toBe(true);
    });

    it('rejects a password over the server maximum', () => {
      patch({ password: 'x'.repeat(129), repassword: 'x'.repeat(129) });
      expect(comp.form.controls.password.hasError('maxlength')).toBe(true);
    });

    it('accepts a password at exactly the server maximum', () => {
      const pw = 'x'.repeat(128);
      patch({ name: 'Updated', password: pw, repassword: pw });
      expect(comp.form.controls.password.valid).toBe(true);
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

    it('routes a password violation onto the password control (#176)', async () => {
      commandServiceMock.execute.mockResolvedValue({
        success: false,
        violations: [{ field: 'password', message: 'Password is not acceptable.' }],
        error: 'Validation failed',
      });
      patch({ name: 'Updated', password: 'hunter2', repassword: 'hunter2' });

      await comp.onSave();

      expect(comp.form.controls.password.errors).toEqual({
        server: 'Password is not acceptable.',
      });
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
