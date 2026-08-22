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
import { Component, OnDestroy, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { ButtonModule } from 'primeng/button';
import { SubmitErrorComponent } from '../../shared/app-submit-error';
import { ScenarioDto } from '../../models/scenario';
import { ScenarioService } from '../../core/scenario.service';
import { PermissionService } from '../../core/permission.service';
import { ListPageComponent } from '../../shared/list-page';
import { AppDataTableComponent, DataTableColumn, RowAction } from '../../shared/app-data-table';

@Component({
  selector: 'app-scenario-list',
  standalone: true,
  imports: [ListPageComponent, AppDataTableComponent, ButtonModule, SubmitErrorComponent],
  template: `
    <app-list-page title="Scenarios" [showSearch]="false">
      <app-submit-error [message]="errorMessage()" testid="scenario-list-error" />

      <app-data-table [value]="scenarios()" [columns]="columns" [loading]="loading()"
                      [rowActions]="rowActions" [rows]="15" searchPlaceholder="Search scenarios..."
                      [globalFilterFields]="['name', 'scenarioType', 'createdBy']"
                      testid="scenario-list" (rowClick)="onSelect({ data: $event })"
                      emptyTitle="No scenarios yet"
                      emptyMessage="Describe the flows this project needs to support."
                      emptyIcon="pi-sitemap" emptyActionLabel="New Scenario"
                      [showEmptyAction]="canEdit()" (emptyAction)="onNew()">
        <div toolbarActions>
          @if (canEdit()) {
            <p-button label="New Scenario" icon="pi pi-plus" (onClick)="onNew()" />
          }
        </div>
      </app-data-table>
    </app-list-page>
  `,
  styles: []
})
export class ScenarioListComponent implements OnInit, OnDestroy {
  scenarios = signal<ScenarioDto[]>([]);
  loading = signal(false);
  errorMessage = signal<string | null>(null);
  canEdit = signal(false);

  columns: DataTableColumn<ScenarioDto>[] = [
    { field: 'name', header: 'Name', sortable: true },
    { field: 'scenarioType', header: 'Type', sortable: true },
    { field: 'createdBy', header: 'Created By', sortable: true }
  ];
  rowActions: RowAction<ScenarioDto>[] = [
    { label: 'Open', icon: 'pi pi-eye', command: s => this.onSelect({ data: s }) }
  ];

  projectName = '';
  private paramSub?: Subscription;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private scenarioService: ScenarioService,
    private permissionService: PermissionService
  ) {}

  ngOnInit(): void {
    this.paramSub = this.route.paramMap.subscribe(async params => {
      this.projectName = params.get('name') ?? '';
      await this.permissionService.loadForProject(this.projectName);
      this.canEdit.set(this.permissionService.canEdit('Scenario'));
      this.loadScenarios();
    });
  }

  ngOnDestroy(): void {
    this.paramSub?.unsubscribe();
  }

  private async loadScenarios(): Promise<void> {
    this.loading.set(true);
    try {
      this.scenarios.set(await this.scenarioService.listScenarios(this.projectName));
    } catch {
      this.errorMessage.set('Failed to load scenarios.');
    } finally {
      this.loading.set(false);
    }
  }

  onNew(): void {
    this.router.navigate(['/projects', this.projectName, 'scenarios', 'new']);
  }

  onSelect(event: { data?: ScenarioDto | ScenarioDto[] }): void {
    const s = Array.isArray(event.data) ? event.data[0] : event.data;
    if (s) this.router.navigate(['/projects', this.projectName, 'scenarios', s.id]);
  }
}
