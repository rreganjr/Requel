import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { AppFieldComponent, AppFieldControlDirective } from './app-field';
import { AppFieldGroupComponent } from './app-field-group';
import { expectNoAxeViolations } from './testing/a11y';

/**
 * Two-column group with an odd row count, so the partial last row is covered too.
 * Mirrors the shape `user-editor` will take: identity fields side by side, then a
 * full-width description row outside the group.
 */
@Component({
  standalone: true,
  imports: [
    AppFieldGroupComponent,
    AppFieldComponent,
    AppFieldControlDirective,
    ReactiveFormsModule,
  ],
  template: `
    <form [formGroup]="form">
      <app-field-group [columns]="2">
        <app-field label="Username" helper="Lowercase, no spaces." [control]="form.controls.username" [submitted]="submitted">
          <input appFieldControl formControlName="username" />
        </app-field>
        <app-field label="Name" [control]="form.controls.name" [submitted]="submitted">
          <input appFieldControl formControlName="name" />
        </app-field>
        <app-field label="Email" [control]="form.controls.email" [submitted]="submitted">
          <input appFieldControl type="email" formControlName="email" />
        </app-field>
      </app-field-group>

      <app-field label="Organization" [control]="form.controls.organization" [divider]="false">
        <input appFieldControl formControlName="organization" />
      </app-field>
    </form>
  `,
})
class GroupHostComponent {
  submitted = false;
  form = new FormGroup({
    username: new FormControl('', { validators: Validators.required, nonNullable: true }),
    name: new FormControl('', { validators: Validators.required, nonNullable: true }),
    email: new FormControl('', { validators: Validators.email, nonNullable: true }),
    organization: new FormControl('', { nonNullable: true }),
  });
}

describe('AppFieldGroupComponent accessibility (issue #172)', () => {
  function render(configure?: (host: GroupHostComponent) => void): HTMLElement {
    TestBed.configureTestingModule({ imports: [GroupHostComponent] });
    const fixture = TestBed.createComponent(GroupHostComponent);
    configure?.(fixture.componentInstance);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  it('has no axe-core violations in the pristine state', async () => {
    await expectNoAxeViolations(render());
  });

  it('has no axe-core violations while rows are showing validation errors', async () => {
    const el = render(host => {
      host.submitted = true;
      host.form.markAllAsTouched();
      host.form.controls.email.setValue('not-an-email');
    });
    await expectNoAxeViolations(el);
  });

  it('has no axe-core violations once the form is valid', async () => {
    const el = render(host => {
      host.form.setValue({
        username: 'rregan',
        name: 'Ron Regan',
        email: 'ron@example.com',
        organization: 'Requel',
      });
      host.form.markAllAsTouched();
    });
    await expectNoAxeViolations(el);
  });

  /**
   * The group is a CSS grid, and `grid-auto-flow: row` lays cells out in source
   * order — so DOM order, which is what assistive tech and sequential focus both
   * follow, matches the visual left-to-right / top-to-bottom reading order. This
   * asserts the invariant that makes that true: nothing reorders the cells.
   */
  it('keeps DOM order (and so reading and tab order) matching visual order', () => {
    const el = render();
    const labels = Array.from(el.querySelectorAll('.app-field-group app-field label')).map(label =>
      label.textContent?.trim().replace(/\*$/, '')
    );

    expect(labels).toEqual(['Username', 'Name', 'Email']);
  });

  it('gives every control in the group exactly one label', () => {
    const el = render();
    const controls = Array.from(
      el.querySelectorAll<HTMLInputElement>('.app-field-group app-field input')
    );

    expect(controls).toHaveLength(3);
    for (const control of controls) {
      const forLabels = el.querySelectorAll(`label[for="${control.id}"]`);
      expect(control.id).toBeTruthy();
      expect(forLabels).toHaveLength(1);
    }
  });

  it('adds no landmark, group or region semantics of its own', () => {
    const el = render();
    const group = el.querySelector('.app-field-group')!;

    expect(group.getAttribute('role')).toBeNull();
    expect(group.getAttribute('aria-labelledby')).toBeNull();
    expect(group.getAttribute('aria-describedby')).toBeNull();
    expect(group.tagName.toLowerCase()).toBe('div');
  });

  it('associates helper text with its own control only', () => {
    const el = render();
    const username = el.querySelector<HTMLInputElement>('input[formcontrolname="username"]')!;
    const name = el.querySelector<HTMLInputElement>('input[formcontrolname="name"]')!;

    expect(username.getAttribute('aria-describedby')).toBeTruthy();
    expect(name.getAttribute('aria-describedby')).toBeNull();
  });
});
