import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter, ActivatedRoute, convertToParamMap, Router } from '@angular/router';
import { BehaviorSubject } from 'rxjs';
import { UserEditorComponent } from './user-editor';
import { UserService } from '../../core/user.service';
import { CommandService } from '../../core/command.service';
import { expectNoAxeViolations } from '../../shared/testing/a11y';

const MOCK_ROLES = [
  { roleName: 'ProjectUserRole', displayName: 'Project User',
    availablePermissions: [{ name: 'editGoals' }, { name: 'deleteGoals' }] },
  { roleName: 'SystemAdminUserRole', displayName: 'System Admin', availablePermissions: [] }
];

const MOCK_USER = {
  id: 1, version: 0, username: 'bob', name: 'Bob Smith',
  emailAddress: 'bob@example.com', phoneNumber: null, organizationName: 'Acme',
  roles: ['ProjectUserRole'], permissions: ['editGoals'],
  permissionsByRole: { ProjectUserRole: ['editGoals'] }
};

const flush = () => new Promise(r => setTimeout(r, 0));

describe('UserEditorComponent accessibility (issue #132)', () => {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  let fixture: any;
  let comp: UserEditorComponent;

  async function render(username = 'new'): Promise<HTMLElement> {
    const paramMap$ = new BehaviorSubject(convertToParamMap({ username }));
    TestBed.configureTestingModule({
      imports: [UserEditorComponent],
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { paramMap: paramMap$.asObservable() } },
        {
          provide: UserService,
          useValue: {
            listRoles: vi.fn().mockResolvedValue(MOCK_ROLES),
            listOrganizations: vi.fn().mockResolvedValue([{ id: 1, name: 'Acme' }]),
            getUser: vi.fn().mockResolvedValue(MOCK_USER),
          },
        },
        { provide: CommandService, useValue: { execute: vi.fn().mockResolvedValue({ success: true }) } },
      ],
    });
    fixture = TestBed.createComponent(UserEditorComponent);
    comp = fixture.componentInstance;
    vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    fixture.detectChanges();
    await flush();
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  it('has no axe-core violations on the create form', async () => {
    await expectNoAxeViolations(await render('new'));
  });

  it('has no axe-core violations on the edit form, with a role expanded', async () => {
    const el = await render('bob');
    // A selected role reveals its permission checkboxes, which is the denser state.
    expect(el.querySelectorAll('.permissions p-checkbox').length).toBeGreaterThan(0);
    await expectNoAxeViolations(el);
  });

  it('has no axe-core violations with every field in its error state', async () => {
    const el = await render('new');
    comp.form.patchValue({
      username: '',
      name: '',
      emailAddress: 'nope',
      password: 'a',
      repassword: 'b',
      roleNames: [],
    });
    comp.form.markAsDirty();
    await comp.onSave();
    fixture.detectChanges();

    expect(el.querySelectorAll('[data-testid="field-error"]').length).toBeGreaterThan(0);
    expect(el.querySelector('[data-testid="user-roles-error"]')).not.toBeNull();
    await expectNoAxeViolations(el);
  });

  it('has no axe-core violations in the loading state', async () => {
    const el = await render('bob');
    comp.loading.set(true);
    fixture.detectChanges();
    await expectNoAxeViolations(el);
  });

  /**
   * The two-column group must not reorder anything: DOM order is what both the screen
   * reader and sequential focus follow, so it has to match the visual grid order.
   */
  it('keeps reading order matching the visual two-column order', async () => {
    const el = await render('new');
    const labels = Array.from(el.querySelectorAll('.app-field-group app-field label')).map(l =>
      l.textContent?.trim().replace(/\*$/, '').trim()
    );

    expect(labels).toEqual([
      'Username',
      'Name',
      'Email',
      'Phone',
      'Organization',
      'Password',
      'Confirm Password',
    ]);
  });

  it('gives every identity control exactly one label', async () => {
    const el = await render('new');
    const controls = Array.from(
      el.querySelectorAll<HTMLElement>('.app-field-group app-field input')
    ).filter(c => c.id);

    expect(controls.length).toBeGreaterThanOrEqual(7);
    for (const control of controls) {
      expect(el.querySelectorAll(`label[for="${control.id}"]`).length, control.id).toBe(1);
    }
  });

  /**
   * The roles error is group-level — there is no single control for it to sit under — so
   * it renders next to the checkboxes with role="alert" rather than being swallowed into
   * the page-level message where it would lose its association with the checkbox list.
   */
  it('announces the roles error as an alert beside the checkboxes', async () => {
    const el = await render('new');
    comp.submitted.set(true);
    comp.form.controls.roleNames.setValue([]);
    fixture.detectChanges();

    const error = el.querySelector('[data-testid="user-roles-error"]')!;
    expect(error.getAttribute('role')).toBe('alert');
    expect(el.querySelector('[data-testid="user-roles-section"]')!.contains(error)).toBe(true);
  });

  it('renders the roles heading as an h2 so the page heading order is unbroken', async () => {
    const el = await render('bob');
    const headings = Array.from(el.querySelectorAll('h1,h2,h3,h4,h5,h6')).map(h => h.tagName);

    expect(headings[0]).toBe('H1');
    expect(headings).toContain('H2');
    expect(headings).not.toContain('H3');
  });
});
