import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter, Router } from '@angular/router';
import { UserListComponent } from './user-list';
import { UserService } from '../../core/user.service';

const MOCK_USERS = [
  { id: 1, version: 0, username: 'alice', name: 'Alice', emailAddress: 'alice@x.com',
    phoneNumber: null, organizationName: 'Org A', roles: ['ProjectUserRole'], permissions: [],
    permissionsByRole: null },
  { id: 2, version: 0, username: 'bob', name: 'Bob', emailAddress: null,
    phoneNumber: null, organizationName: null, roles: ['SystemAdminUserRole'], permissions: [],
    permissionsByRole: null }
];

describe('UserListComponent', () => {
  let userServiceMock: { listUsers: ReturnType<typeof vi.fn> };
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  let fixture: any;
  let comp: UserListComponent;
  let router: Router;

  beforeEach(() => {
    userServiceMock = { listUsers: vi.fn().mockResolvedValue(MOCK_USERS) };

    TestBed.configureTestingModule({
      imports: [UserListComponent],
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        { provide: UserService, useValue: userServiceMock }
      ]
    });
    fixture = TestBed.createComponent(UserListComponent);
    comp = fixture.componentInstance;
    router = TestBed.inject(Router);
  });

  it('ngOnInit calls userService.listUsers and populates users()', async () => {
    fixture.detectChanges();
    await fixture.whenStable();
    expect(userServiceMock.listUsers).toHaveBeenCalled();
    expect(comp.users().length).toBe(2);
    expect(comp.users()[0].username).toBe('alice');
  });

  it('loading() is false after init', async () => {
    fixture.detectChanges();
    await fixture.whenStable();
    expect(comp.loading()).toBe(false);
  });

  it('onNewUser navigates to /users/new', () => {
    const spy = vi.spyOn(router, 'navigate').mockResolvedValue(true);
    comp.onNewUser();
    expect(spy).toHaveBeenCalledWith(['/users', 'new']);
  });

  it('onRowSelect navigates to /users/:username', () => {
    const spy = vi.spyOn(router, 'navigate').mockResolvedValue(true);
    comp.onRowSelect({ data: MOCK_USERS[0] });
    expect(spy).toHaveBeenCalledWith(['/users', 'alice']);
  });
});
