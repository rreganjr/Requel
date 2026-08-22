/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2026 Ron Regan Jr. All Rights Reserved.
 *
 * Requel is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Requel is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Requel. If not, see <http://www.gnu.org/licenses/>.
 *
 */
import { Component, OnInit, signal } from '@angular/core';
import { PageHeaderComponent } from '../../shared/page-header';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { InputNumberModule } from 'primeng/inputnumber';
import { SelectModule } from 'primeng/select';
import { MessageModule } from 'primeng/message';
import { SubmitErrorComponent } from '../../shared/app-submit-error';
import { PreferencesService } from '../../core/preferences.service';
import { AuthService } from '../../core/auth.service';
import { UserPreferencesDto, STALENESS_OPTIONS } from '../../models/preferences';
import { ApiTokensComponent } from './api-tokens';
import { AppFieldComponent, AppFieldControlDirective } from '../../shared/app-field';
import { integer } from '../../shared/form-errors';

/** The sidebar project limit the reset button restores, and the widget's own bounds. */
const PROJECT_LIMIT_DEFAULT = 10;
const PROJECT_LIMIT_MIN = 1;
const PROJECT_LIMIT_MAX = 100;
const STALENESS_DEFAULT = 'THREE_MONTHS';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [
    PageHeaderComponent,
    ReactiveFormsModule,
    ButtonModule,
    InputNumberModule,
    SelectModule,
    MessageModule,
    SubmitErrorComponent,
    ApiTokensComponent,
    AppFieldComponent,
    AppFieldControlDirective,
  ],
  template: `
    <div class="settings" data-testid="settings-page">
      <div class="page-header">
        <app-page-header title="Settings" />
      </div>

      <div role="status" aria-live="polite">
        @if (successMessage()) {
          <p-message severity="success" [text]="successMessage()" />
        }
      </div>
      <app-submit-error [message]="errorMessage()" testid="settings-error" />

      <form class="settings-form" [formGroup]="form" (ngSubmit)="onSave()">
        <app-field
          label="Sidebar Project Limit"
          helper="Maximum number of projects shown in the sidebar."
          controlId="settings-project-limit-input"
          [control]="form.controls.sidebarProjectLimit"
          [submitted]="submitted()"
        >
          <p-inputNumber
            appFieldControl
            inputId="settings-project-limit-input"
            data-testid="settings-project-limit"
            formControlName="sidebarProjectLimit"
            [min]="projectLimitMin"
            [max]="projectLimitMax"
            [showButtons]="true"
          />
        </app-field>

        <app-field
          label="Project Staleness Threshold"
          helper="Hide projects with no activity older than this threshold."
          controlId="settings-staleness-input"
          [control]="form.controls.sidebarProjectStaleness"
          [submitted]="submitted()"
          [divider]="false"
        >
          <p-select
            appFieldControl
            inputId="settings-staleness-input"
            data-testid="settings-staleness"
            formControlName="sidebarProjectStaleness"
            [options]="stalenessOptions"
            optionLabel="label"
            optionValue="value"
            placeholder="Select staleness threshold"
          />
        </app-field>

        <div class="form-actions">
          <p-button
            type="submit"
            label="Save"
            icon="pi pi-check"
            data-testid="settings-save"
            [loading]="saving()"
            [disabled]="form.invalid || form.pristine || saving()"
          />
          <p-button
            type="button"
            label="Reset to Defaults"
            icon="pi pi-refresh"
            severity="secondary"
            data-testid="settings-reset"
            [outlined]="true"
            (onClick)="onReset()"
            [loading]="saving()"
          />
        </div>
      </form>

      @if (canManageTokens()) {
        <app-api-tokens />
      }
    </div>
  `,
  styles: [
    `
      .settings {
        max-width: 600px;
      }
      .page-header {
        margin-bottom: var(--rq-space-6);
      }
      /* app-field owns each row's internal layout and the hairline between rows, so
         the form only needs to space the action bar off the last row. */
      .settings-form {
        display: block;
      }
      .form-actions {
        margin-top: var(--rq-space-4);
        display: flex;
        gap: var(--rq-space-2);
      }
      /* Projected through app-field, so still this component's nodes to size. */
      p-inputnumber,
      p-select {
        width: 100%;
      }
    `,
  ],
})
export class SettingsComponent implements OnInit {
  readonly stalenessOptions = STALENESS_OPTIONS;
  readonly projectLimitMin = PROJECT_LIMIT_MIN;
  readonly projectLimitMax = PROJECT_LIMIT_MAX;

  readonly saving = signal(false);
  readonly submitted = signal(false);
  readonly successMessage = signal('');
  readonly errorMessage = signal('');

  /**
   * `min`/`max` mirror the `p-inputNumber` bounds so the form cannot hold a value the
   * widget would not produce, and `integer()` covers the typed case — PrimeNG's
   * decimal mode will accept a fractional entry that the preference has no meaning
   * for. These are UI bounds, not backend constraints: preferences are not validated
   * server-side, so they never waited on #171 the way the artifact name limit did.
   */
  readonly form = new FormGroup({
    sidebarProjectLimit: new FormControl<number | null>(PROJECT_LIMIT_DEFAULT, {
      validators: [
        Validators.required,
        Validators.min(PROJECT_LIMIT_MIN),
        Validators.max(PROJECT_LIMIT_MAX),
        integer(),
      ],
    }),
    sidebarProjectStaleness: new FormControl(STALENESS_DEFAULT, {
      validators: Validators.required,
      nonNullable: true,
    }),
  });

  constructor(
    private preferencesService: PreferencesService,
    private authService: AuthService
  ) {}

  /**
   * The PAT section is gated on the per-user manageApiTokens permission (#85), not the role.
   * An admin who is granted the permission (via their ProjectUserRole) qualifies too.
   */
  canManageTokens(): boolean {
    return this.authService.user()?.permissions?.includes('manageApiTokens') ?? false;
  }

  async ngOnInit(): Promise<void> {
    try {
      const prefs = await this.preferencesService.load();
      // patchValue notifies each control's value accessor directly, so p-select and
      // p-inputNumber pick the loaded values up without the ChangeDetectorRef.
      // detectChanges() the ngModel version needed — plain class properties gave
      // zone.js no reason to run a cycle before PrimeNG read them.
      this.form.patchValue(prefs);
      this.form.markAsPristine();
    } catch {
      this.errorMessage.set('Failed to load preferences.');
    }
  }

  /**
   * Restores the defaults and saves immediately, as before. Marked dirty explicitly
   * because `setValue` does not do it, and the save path treats a pristine form as
   * nothing to do.
   */
  async onReset(): Promise<void> {
    this.form.setValue({
      sidebarProjectLimit: PROJECT_LIMIT_DEFAULT,
      sidebarProjectStaleness: STALENESS_DEFAULT,
    });
    this.form.markAsDirty();
    await this.onSave();
  }

  async onSave(): Promise<void> {
    this.submitted.set(true);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.saving.set(true);
    this.successMessage.set('');
    this.errorMessage.set('');
    try {
      const value = this.form.getRawValue();
      const prefs: UserPreferencesDto = {
        // Non-null by validation: `required` blocks the invalid path above.
        sidebarProjectLimit: value.sidebarProjectLimit!,
        sidebarProjectStaleness: value.sidebarProjectStaleness,
      };
      const updated = await this.preferencesService.save(prefs);
      this.form.patchValue(updated);
      this.form.markAsPristine();
      this.submitted.set(false);
      this.successMessage.set('Preferences saved.');
    } catch {
      this.errorMessage.set('Failed to save preferences.');
    } finally {
      this.saving.set(false);
    }
  }
}
