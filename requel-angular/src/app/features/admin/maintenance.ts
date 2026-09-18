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
import { Component, ChangeDetectionStrategy, signal, inject } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { InputText } from 'primeng/inputtext';
import { ConfirmationService, MessageService } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { PageHeaderComponent } from '../../shared/page-header';
import { SubmitErrorComponent } from '../../shared/app-submit-error';
import { MaintenanceService } from '../../core/maintenance.service';
import { RepairProjectStakeholdersResult } from '../../models/maintenance';

/**
 * Admin surface for estate-wide maintenance tasks. Reached at /maintenance behind the
 * adminGuard; every command it dispatches is additionally gated server-side by
 * SystemAdminUserRole, which is the real enforcement - the guard only keeps the page out
 * of the nav.
 *
 * One task today (issue #256). It lives on its own page rather than bolted onto Users or
 * Global Tags because it is an action with no entity to browse, and #302's follow-up
 * repairs will want the same home.
 */
@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-maintenance',
  standalone: true,
  imports: [PageHeaderComponent, ReactiveFormsModule, ButtonModule, InputText,
            SubmitErrorComponent, ConfirmDialogModule],
  // MessageService is inherited from the auth layout; ConfirmationService is provided
  // per-component throughout this codebase (see term-editor et al.), so provide it here.
  providers: [ConfirmationService],
  template: `
    <div class="maintenance" data-testid="maintenance">
      <div class="page-header"><app-page-header title="Maintenance" /></div>

      <app-submit-error [message]="errorMessage()" testid="maintenance-error" />

      <fieldset class="rq-fieldset" data-testid="repair-stakeholders-task" [formGroup]="repairForm">
        <legend>Repair project stakeholders</legend>
        <p class="task-description">
          Restores the creator's stakeholder row and full permission set on projects that
          lost them, so the owner can manage a project they created. Safe to re-run - it
          only adds what is missing.
        </p>
        <div class="task-row">
          <input pInputText formControlName="projectName" placeholder="project name (optional)"
                 aria-label="Project name to repair, or blank for every project"
                 data-testid="repair-project-name" class="name-input" />
          <p-button label="Run" icon="pi pi-wrench" data-testid="repair-run"
                    [disabled]="running()" [loading]="running()" (onClick)="runRepair()" />
        </div>

        @if (result(); as r) {
          <dl class="result" data-testid="repair-result">
            <div><dt>Projects scanned</dt><dd data-testid="repair-scanned">{{ r.projectsScanned }}</dd></div>
            <div><dt>Stakeholders created</dt><dd data-testid="repair-created">{{ r.stakeholdersCreated }}</dd></div>
            <div><dt>Permissions granted</dt><dd data-testid="repair-granted">{{ r.permissionsGranted }}</dd></div>
            <div><dt>Projects skipped</dt><dd data-testid="repair-skipped">{{ r.projectsSkipped }}</dd></div>
          </dl>
        }
      </fieldset>

      <p-confirmDialog />
    </div>
  `,
  styles: [`
    .maintenance { max-width: 800px; }
    .page-header { margin-bottom: 1rem; }
    .task-description { margin: 0 0 0.75rem; max-width: 60ch; }
    .task-row { display: flex; align-items: center; gap: 0.5rem; flex-wrap: wrap; }
    .name-input { max-width: 280px; }
    .result { display: flex; flex-wrap: wrap; gap: 1.5rem; margin: 1rem 0 0; }
    .result div { display: flex; flex-direction: column; }
    .result dt { font-size: 0.85rem; opacity: 0.75; }
    .result dd { margin: 0; font-size: 1.25rem; font-variant-numeric: tabular-nums; }
  `]
})
export class MaintenanceComponent {
  private readonly maintenanceService = inject(MaintenanceService);
  private readonly messageService = inject(MessageService);
  private readonly confirmationService = inject(ConfirmationService);

  readonly running = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly result = signal<RepairProjectStakeholdersResult | null>(null);

  readonly repairForm = new FormGroup({
    projectName: new FormControl('', { nonNullable: true }),
  });

  /**
   * Blank project name means every project, so confirm that case and only that case:
   * naming one project is a targeted fix, running across the estate is a bulk write.
   */
  runRepair(): void {
    const projectName = this.repairForm.controls.projectName.value.trim();
    if (projectName) {
      void this.repair(projectName);
      return;
    }
    this.confirmationService.confirm({
      message: 'Repair stakeholders on every project? This adds missing creator '
        + 'stakeholder rows and permissions across the whole estate.',
      header: 'Confirm Repair',
      icon: 'pi pi-exclamation-triangle',
      accept: () => void this.repair(null),
    });
  }

  private async repair(projectName: string | null): Promise<void> {
    this.running.set(true);
    this.errorMessage.set(null);
    try {
      const outcome = await this.maintenanceService.repairProjectStakeholders(projectName);
      if (outcome.success && outcome.entity) {
        this.result.set(outcome.entity);
        this.messageService.add({ severity: 'success', summary: 'Repair complete', life: 3000 });
      } else {
        this.result.set(null);
        this.errorMessage.set(outcome.error ?? 'Repair failed.');
      }
    } catch {
      // CommandService maps HTTP failures into a failed CommandResult rather than throwing,
      // so this is the belt-and-braces path. Without it a throw leaves the button un-spinning
      // with nothing on screen to say why, and an unhandled rejection behind it.
      this.result.set(null);
      this.errorMessage.set('Repair failed.');
    } finally {
      this.running.set(false);
    }
  }
}
