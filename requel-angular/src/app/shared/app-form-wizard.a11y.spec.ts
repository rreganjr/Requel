import { Component, viewChild } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { AppFieldComponent, AppFieldControlDirective } from './app-field';
import {
  AppFormWizardComponent,
  AppWizardStepComponent,
  WizardCommitRequest,
} from './app-form-wizard';
import { expectNoAxeViolations } from './testing/a11y';

@Component({
  standalone: true,
  imports: [
    AppFormWizardComponent,
    AppWizardStepComponent,
    AppFieldComponent,
    AppFieldControlDirective,
    ReactiveFormsModule,
  ],
  template: `
    <app-form-wizard [(activeKey)]="activeKey" (stepCommit)="onCommit($event)">
      <app-wizard-step key="details" label="Details" [form]="detailsForm">
        <ng-template>
          <!--
            [formControl], not formControlName: the step body is projected into the
            wizard, and formControlName resolves its parent formGroup from the
            injector at the INSERTION point, where there is none.
          -->
          <app-field label="Name" helper="Short, outcome-focused."
                     [control]="detailsForm.controls.name" [submitted]="submitted">
            <input appFieldControl [formControl]="detailsForm.controls.name" />
          </app-field>
          <app-field label="Description" [control]="detailsForm.controls.text" [divider]="false">
            <textarea appFieldControl [formControl]="detailsForm.controls.text" rows="4"></textarea>
          </app-field>
        </ng-template>
      </app-wizard-step>

      <app-wizard-step key="tags" label="Tags" helper="Optional." [optional]="true">
        <ng-template>
          <p>Tag selector goes here.</p>
        </ng-template>
      </app-wizard-step>
    </app-form-wizard>`,
})
class WizardA11yHostComponent {
  readonly wizard = viewChild.required(AppFormWizardComponent);

  activeKey = 'details';
  submitted = false;
  failWith: string | null = null;

  detailsForm = new FormGroup({
    name: new FormControl('', { validators: Validators.required, nonNullable: true }),
    text: new FormControl('', { nonNullable: true }),
  });

  onCommit(request: WizardCommitRequest): void {
    if (this.failWith) {
      request.fail(this.failWith);
      return;
    }
    request.complete();
  }
}

describe('AppFormWizardComponent accessibility (issue #158)', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [WizardA11yHostComponent] });
  });

  function render(configure?: (host: WizardA11yHostComponent) => void): HTMLElement {
    const fixture = TestBed.createComponent(WizardA11yHostComponent);
    configure?.(fixture.componentInstance);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  /** `fixture.nativeElement` is `any`, so narrow it before a generic querySelector. */
  function clickContinue(el: HTMLElement): void {
    el.querySelector<HTMLButtonElement>('[data-testid="wizard-continue"] button')?.click();
  }

  it('has no axe-core violations on the first step', async () => {
    await expectNoAxeViolations(render());
  });

  it('has no axe-core violations while a field shows a validation error', async () => {
    const el = render(host => {
      host.submitted = true;
      host.detailsForm.controls.name.markAsTouched();
    });
    await expectNoAxeViolations(el);
  });

  it('has no axe-core violations while the commit-failure alert is showing', async () => {
    const fixture = TestBed.createComponent(WizardA11yHostComponent);
    fixture.componentInstance.detailsForm.setValue({ name: 'Reduce setup time', text: '' });
    fixture.componentInstance.failWith = 'This goal was changed elsewhere.';
    fixture.detectChanges();

    const el = fixture.nativeElement as HTMLElement;
    clickContinue(el);
    fixture.detectChanges();

    expect(el.querySelector('[data-testid="wizard-error"]')).not.toBeNull();
    await expectNoAxeViolations(el);
  });

  it('has no axe-core violations on an optional step', async () => {
    const fixture = TestBed.createComponent(WizardA11yHostComponent);
    fixture.componentInstance.detailsForm.setValue({ name: 'Reduce setup time', text: '' });
    fixture.detectChanges();

    const el = fixture.nativeElement as HTMLElement;
    clickContinue(el);
    fixture.detectChanges();

    expect(fixture.componentInstance.wizard().activeKey).toBe('tags');
    await expectNoAxeViolations(el);
  });
});
