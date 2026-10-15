import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { AppFieldComponent, AppFieldControlDirective } from './app-field';
import { expectNoAxeViolations } from './testing/a11y';

@Component({
  standalone: true,
  imports: [AppFieldComponent, AppFieldControlDirective, ReactiveFormsModule],
  template: `
    <form [formGroup]="form">
      <app-field
        label="Name"
        helper="Short, outcome-focused."
        [control]="form.controls.name"
        [submitted]="submitted"
      >
        <input appFieldControl formControlName="name" />
      </app-field>

      <app-field
        label="Description"
        [control]="form.controls.text"
        [divider]="false"
        [submitted]="submitted"
      >
        <textarea appFieldControl formControlName="text" rows="4"></textarea>
      </app-field>
    </form>`,
})
class FormHostComponent {
  submitted = false;
  form = new FormGroup({
    name: new FormControl('', { validators: Validators.required, nonNullable: true }),
    text: new FormControl('', { nonNullable: true }),
  });
}

describe('AppFieldComponent accessibility (issue #158)', () => {
  function render(configure?: (host: FormHostComponent) => void): HTMLElement {
    TestBed.configureTestingModule({ imports: [FormHostComponent] });
    const fixture = TestBed.createComponent(FormHostComponent);
    configure?.(fixture.componentInstance);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  it('has no axe-core violations in the pristine state', async () => {
    await expectNoAxeViolations(render());
  });

  it('has no axe-core violations while showing a validation error', async () => {
    const el = render(host => {
      host.submitted = true;
      host.form.controls.name.markAsTouched();
    });
    await expectNoAxeViolations(el);
  });

  it('has no axe-core violations once the form is valid', async () => {
    const el = render(host => {
      host.form.setValue({ name: 'Reduce onboarding time', text: 'Cut setup to one day.' });
      host.form.markAllAsTouched();
    });
    await expectNoAxeViolations(el);
  });
});
