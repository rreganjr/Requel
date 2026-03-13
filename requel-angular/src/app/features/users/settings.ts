import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { InputNumberModule } from 'primeng/inputnumber';
import { SelectModule } from 'primeng/select';
import { MessageModule } from 'primeng/message';
import { PreferencesService } from '../../core/preferences.service';
import { UserPreferencesDto, STALENESS_OPTIONS } from '../../models/preferences';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [FormsModule, ButtonModule, InputNumberModule, SelectModule, MessageModule],
  template: `
    <div class="settings">
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
          <p-inputNumber id="projectLimit" [(ngModel)]="sidebarProjectLimit"
                         [min]="1" [max]="100" [showButtons]="true" />
          <small>Maximum number of projects shown in the sidebar.</small>
        </div>

        <div class="field">
          <label for="staleness">Project Staleness Threshold</label>
          <p-select id="staleness" [(ngModel)]="sidebarProjectStaleness"
                    [options]="stalenessOptions" optionLabel="label" optionValue="value"
                    placeholder="Select staleness threshold" />
          <small>Hide projects with no activity older than this threshold.</small>
        </div>

        <div class="form-actions">
          <p-button label="Save" icon="pi pi-check" (onClick)="onSave()" [loading]="saving()" />
        </div>
      </div>
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

  constructor(private preferencesService: PreferencesService) {}

  async ngOnInit(): Promise<void> {
    try {
      const prefs = await this.preferencesService.load();
      this.sidebarProjectLimit = prefs.sidebarProjectLimit;
      this.sidebarProjectStaleness = prefs.sidebarProjectStaleness;
    } catch {
      this.errorMessage.set('Failed to load preferences.');
    }
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
