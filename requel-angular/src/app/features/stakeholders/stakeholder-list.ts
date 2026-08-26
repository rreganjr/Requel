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
import { Component, OnDestroy, OnInit, TemplateRef, ViewChild, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { ButtonModule } from 'primeng/button';
import { SubmitErrorComponent } from '../../shared/app-submit-error';
import { StakeholderDto } from '../../models/stakeholder';
import { StakeholderService } from '../../core/stakeholder.service';
import { PermissionService } from '../../core/permission.service';
import { ListPageComponent } from '../../shared/list-page';
import { AppDataTableComponent, DataTableColumn, RowAction } from '../../shared/app-data-table';

@Component({
  selector: 'app-stakeholder-list',
  standalone: true,
  imports: [ListPageComponent, AppDataTableComponent, ButtonModule, SubmitErrorComponent],
  template: `
    <app-list-page title="Stakeholders" [showSearch]="false">
      <app-submit-error [message]="errorMessage()" testid="stakeholder-list-error" [retryable]="true" (retry)="loadStakeholders()" />

      <app-data-table [value]="stakeholders()" [columns]="columns" [loading]="loading()"
                      [rowActions]="rowActions" searchPlaceholder="Search stakeholders..."
                      [globalFilterFields]="['name', 'type', 'userDetails.emailAddress', 'userDetails.teamName']"
                      testid="stakeholder-list" (rowClick)="onRowSelect({ data: $event })"
                      emptyTitle="No stakeholders yet"
                      emptyMessage="Add the people and groups with a stake in this project to capture their perspectives."
                      emptyIcon="pi-users" emptyActionLabel="Add User"
                      [showEmptyAction]="canEdit()" (emptyAction)="onNewUserStakeholder()">
        <div toolbarActions class="stakeholder-toolbar-actions">
          @if (canEdit()) {
            <p-button label="Add User" icon="pi pi-user-plus" (onClick)="onNewUserStakeholder()" />
            <p-button label="Add Non-User" icon="pi pi-building" severity="secondary"
                      [outlined]="true" (onClick)="onNewNonUserStakeholder()" />
          }
        </div>
      </app-data-table>
    </app-list-page>

    <ng-template #typeCell let-s>{{ s.type === 'user' ? 'User' : 'Non-User' }}</ng-template>
  `,
  styles: [`
    .stakeholder-toolbar-actions { display: flex; align-items: center; gap: var(--rq-space-2); }
  `]
})
export class StakeholderListComponent implements OnInit, OnDestroy {
  stakeholders = signal<StakeholderDto[]>([]);
  loading = signal(true);
  errorMessage = signal<string | null>(null);
  canEdit = signal(false);

  @ViewChild('typeCell', { static: true }) typeCell!: TemplateRef<{ $implicit: StakeholderDto }>;
  columns: DataTableColumn<StakeholderDto>[] = [];
  rowActions: RowAction<StakeholderDto>[] = [
    { label: 'Open', icon: 'pi pi-eye', command: s => this.onRowSelect({ data: s }) }
  ];

  private projectName = '';
  private paramSub?: Subscription;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private stakeholderService: StakeholderService,
    private permissionService: PermissionService
  ) {}

  ngOnInit(): void {
    this.columns = [
      { field: 'name', header: 'Name', sortable: true, link: s => ['/projects', this.projectName, 'stakeholders', s.id] },
      { field: 'type', header: 'Type', sortable: true, cellTemplate: this.typeCell },
      { field: 'userDetails.teamName', header: 'Team' },
      { field: 'userDetails.emailAddress', header: 'Email' },
      { field: 'userDetails.phoneNumber', header: 'Phone' },
      { field: 'createdBy', header: 'Created By' }
    ];
    this.paramSub = this.route.paramMap.subscribe(async params => {
      const name = params.get('name') ?? '';
      if (name !== this.projectName) {
        this.projectName = name;
        await this.permissionService.loadForProject(name);
        this.canEdit.set(this.permissionService.canEdit('Stakeholder'));
        this.loadStakeholders();
      }
    });
  }

  ngOnDestroy(): void {
    this.paramSub?.unsubscribe();
  }

  async loadStakeholders(): Promise<void> {
    this.loading.set(true);
    try {
      const list = await this.stakeholderService.listStakeholders(this.projectName);
      this.stakeholders.set(list);
    } catch {
      this.errorMessage.set('Failed to load stakeholders.');
    } finally {
      this.loading.set(false);
    }
  }

  onRowSelect(event: { data?: StakeholderDto | StakeholderDto[] }): void {
    const s = Array.isArray(event.data) ? event.data[0] : event.data;
    if (!s) return;
    this.router.navigate(['/projects', this.projectName, 'stakeholders', s.id]);
  }

  onNewUserStakeholder(): void {
    this.router.navigate(['new-user'], { relativeTo: this.route });
  }

  onNewNonUserStakeholder(): void {
    this.router.navigate(['new-nonuser'], { relativeTo: this.route });
  }
}
