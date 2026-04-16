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

  it('username() is computed from authService.user().username', () => {
    expect(comp.username()).toBe('admin');
  });

  it('ngOnInit populates fields from authService.user()', async () => {
    fixture.detectChanges();
    await fixture.whenStable();
    expect(comp.name()).toBe('Admin User');
    expect(comp.emailAddress()).toBe('admin@example.com');
    expect(comp.phoneNumber()).toBe('555-1234');
    expect(comp.organizationName()).toBe('Acme');
  });

  it('ngOnInit calls userService.listOrganizations', async () => {
    fixture.detectChanges();
    await fixture.whenStable();
    expect(userServiceMock.listOrganizations).toHaveBeenCalled();
    expect(comp.orgOptions().length).toBe(2);
  });

  it('onSave calls commandService.execute("EditUser") with username and fields', async () => {
    comp.name.set('New Name');
    comp.emailAddress.set('new@example.com');
    comp.phoneNumber.set('');
    comp.organizationName.set('Beta');
    await comp.onSave();
    expect(commandServiceMock.execute).toHaveBeenCalledWith('EditUser', expect.objectContaining({
      username: 'admin',
      name: 'New Name',
      emailAddress: 'new@example.com'
    }));
  });

  it('onSave sets successMessage on success', async () => {
    comp.name.set('Updated');
    await comp.onSave();
    expect(comp.successMessage()).toBe('Account updated.');
    expect(comp.saving()).toBe(false);
  });

  it('onSave sets errorMessage when command fails', async () => {
    commandServiceMock.execute.mockResolvedValue({ success: false, error: 'Permission denied' });
    await comp.onSave();
    expect(comp.errorMessage()).toBe('Permission denied');
    expect(comp.saving()).toBe(false);
  });
});
