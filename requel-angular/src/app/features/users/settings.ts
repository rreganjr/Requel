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
import { ChangeDetectorRef, Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { InputNumberModule } from 'primeng/inputnumber';
import { SelectModule } from 'primeng/select';
import { MessageModule } from 'primeng/message';
import { PreferencesService } from '../../core/preferences.service';
import { AuthService } from '../../core/auth.service';
import { UserPreferencesDto, STALENESS_OPTIONS } from '../../models/preferences';
import { ApiTokensComponent } from './api-tokens';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [FormsModule, ButtonModule, InputNumberModule, SelectModule, MessageModule,
    ApiTokensComponent],
  template: `
    <div class="settings" data-testid="settings-page">
      <div class="page-header">
        <h2>Settings</h2>
      </div>

      @if (successMessage()) {
        <p-message severity="success" [text]="successMessage()" />
      }
      @if (errorMessage()) {
        <p-message severity="error" [text]="errorMessage()" />
      }

      <div class="settings-form">
        <div class="field">
          <label for="projectLimit">Sidebar Project Limit</label>
          <p-inputNumber id="projectLimit" inputId="projectLimitInput"
                         data-testid="settings-project-limit" [(ngModel)]="sidebarProjectLimit"
                         [min]="1" [max]="100" [showButtons]="true" />
          <small>Maximum number of projects shown in the sidebar.</small>
        </div>

        <div class="field">
          <label for="staleness">Project Staleness Threshold</label>
          <p-select id="staleness" inputId="stalenessInput"
                    data-testid="settings-staleness" [(ngModel)]="sidebarProjectStaleness"
                    [options]="stalenessOptions" optionLabel="label" optionValue="value"
                    placeholder="Select staleness threshold" />
          <small>Hide projects with no activity older than this threshold.</small>
        </div>

        <div class="form-actions">
          <p-button label="Save" icon="pi pi-check" data-testid="settings-save"
                    (onClick)="onSave()" [loading]="saving()" />
          <p-button label="Reset to Defaults" icon="pi pi-refresh" severity="secondary"
                    data-testid="settings-reset"
                    [outlined]="true" (onClick)="onReset()" [loading]="saving()" />
        </div>
      </div>

      @if (canManageTokens()) {
        <app-api-tokens />
      }
    </div>
  `,
  styles: [`
    .settings { max-width: 600px; }
    .page-header { margin-bottom: 1.5rem; }
    .page-header h2 { margin: 0; }
    .settings-form { display: flex; flex-direction: column; gap: 1.5rem; }
    .field { display: flex; flex-direction: column; gap: 0.25rem; }
    .field label { font-weight: 600; }
    .field small { color: var(--p-text-muted-color); }
    .form-actions { margin-top: 0.5rem; }
  `]
})
export class SettingsComponent implements OnInit {

  readonly stalenessOptions = STALENESS_OPTIONS;
  readonly saving = signal(false);
  readonly successMessage = signal('');
  readonly errorMessage = signal('');

  sidebarProjectLimit = 10;
  sidebarProjectStaleness = 'THREE_MONTHS';

  constructor(
    private preferencesService: PreferencesService,
    private authService: AuthService,
    private cdr: ChangeDetectorRef
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
      this.sidebarProjectLimit = prefs.sidebarProjectLimit;
      this.sidebarProjectStaleness = prefs.sidebarProjectStaleness;
      // Plain class properties don't guarantee that zone.js schedules a change detection
      // cycle before PrimeNG's p-select reads its ngModel value. detectChanges() forces
      // the component tree to update synchronously so the select displays the loaded value.
      this.cdr.detectChanges();
    } catch {
      this.errorMessage.set('Failed to load preferences.');
    }
  }

  async onReset(): Promise<void> {
    this.sidebarProjectLimit = 10;
    this.sidebarProjectStaleness = 'THREE_MONTHS';
    await this.onSave();
  }

  async onSave(): Promise<void> {
    this.saving.set(true);
    this.successMessage.set('');
    this.errorMessage.set('');
    try {
      const prefs: UserPreferencesDto = {
        sidebarProjectLimit: this.sidebarProjectLimit,
        sidebarProjectStaleness: this.sidebarProjectStaleness
      };
      const updated = await this.preferencesService.save(prefs);
      this.sidebarProjectLimit = updated.sidebarProjectLimit;
      this.sidebarProjectStaleness = updated.sidebarProjectStaleness;
      this.successMessage.set('Preferences saved.');
    } catch {
      this.errorMessage.set('Failed to save preferences.');
    } finally {
      this.saving.set(false);
    }
  }
}
