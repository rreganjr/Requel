import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { signal } from '@angular/core';
import { EditAccountComponent } from './edit-account';
import { AuthService } from '../../core/auth.service';
import { CommandService } from '../../core/command.service';
import { UserService } from '../../core/user.service';
import { expectNoAxeViolations } from '../../shared/testing/a11y';

const MOCK_USER = {
  id: 1, version: 0, username: 'admin', name: 'Admin User',
  emailAddress: 'admin@example.com', phoneNumber: '555-1234',
  organizationName: 'Acme', roles: ['SystemAdminUserRole'], permissions: [],
  permissionsByRole: null
};

describe('EditAccountComponent accessibility (issue #132)', () => {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  let fixture: any;
  let comp: EditAccountComponent;

  async function render(): Promise<HTMLElement> {
    TestBed.configureTestingModule({
      imports: [EditAccountComponent],
      providers: [
        provideNoopAnimations(),
        { provide: AuthService, useValue: { user: signal(MOCK_USER) } },
        { provide: CommandService, useValue: { execute: vi.fn().mockResolvedValue({ success: true }) } },
        {
          provide: UserService,
          useValue: { listOrganizations: vi.fn().mockResolvedValue([{ id: 1, name: 'Acme' }]) },
        },
      ],
    });
    fixture = TestBed.createComponent(EditAccountComponent);
    comp = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  it('has no axe-core violations at rest', async () => {
    await expectNoAxeViolations(await render());
  });

  it('has no axe-core violations with fields in their error state', async () => {
    const el = await render();
    comp.form.patchValue({ name: '', emailAddress: 'nope', password: 'a', repassword: 'b' });
    comp.form.markAsDirty();
    await comp.onSave();
    fixture.detectChanges();

    expect(el.querySelectorAll('[data-testid="field-error"]').length).toBeGreaterThan(0);
    await expectNoAxeViolations(el);
  });

  it('has no axe-core violations with a page-level save failure showing', async () => {
    const el = await render();
    comp.errorMessage.set('Save failed.');
    fixture.detectChanges();
    await expectNoAxeViolations(el);
  });

  /**
   * The disabled username row still needs a label — a disabled control is exempt from
   * some axe rules, so this asserts the association directly rather than relying on the
   * axe pass alone.
   */
  it('labels the disabled username row', async () => {
    const el = await render();
    const input = el.querySelector<HTMLInputElement>('#username')!;

    expect(input.disabled).toBe(true);
    expect(el.querySelector('label[for="username"]')?.textContent?.trim()).toContain('Username');
  });

  /**
   * The password row's "leave blank to keep current" hint used to be part of the label
   * text. It is helper text now, linked by aria-describedby, so the accessible NAME stays
   * short while the explanation is still announced.
   */
  it('links the password helper text without putting it in the label', async () => {
    const el = await render();
    const password = el.querySelector<HTMLInputElement>('#password')!;
    const label = el.querySelector('label[for="password"]')!;

    expect(label.textContent?.trim()).toBe('New Password');
    const describedBy = password.getAttribute('aria-describedby');
    expect(el.querySelector(`#${describedBy}`)?.textContent?.trim()).toBe(
      'Leave blank to keep your current password.'
    );
  });

  it('puts the mismatch message under the confirm row, where the fix is', async () => {
    const el = await render();
    comp.form.patchValue({ password: 'hunter2', repassword: 'hunter3' });
    comp.form.markAllAsTouched();
    fixture.detectChanges();

    const repassword = el.querySelector<HTMLInputElement>('#repassword')!;
    expect(repassword.getAttribute('aria-invalid')).toBe('true');

    const describedBy = repassword.getAttribute('aria-describedby');
    expect(el.querySelector(`#${describedBy}`)?.textContent?.trim()).toBe(
      'Passwords do not match.'
    );
    // And not on the password row, which the user does not need to change.
    expect(el.querySelector<HTMLInputElement>('#password')!.getAttribute('aria-invalid')).toBe(
      'false'
    );
  });
});
