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
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { MessageModule } from 'primeng/message';
import { StakeholderDto } from '../../models/stakeholder';
import { StakeholderService } from '../../core/stakeholder.service';
import { PermissionService } from '../../core/permission.service';
import { ListPageComponent } from '../../shared/list-page';

@Component({
  selector: 'app-stakeholder-list',
  standalone: true,
  imports: [ListPageComponent, TableModule, ButtonModule, MessageModule],
  template: `
    <app-list-page title="Stakeholders" searchPlaceholder="Search stakeholders..."
                   (search)="dt.filterGlobal($event, 'contains')">
      <ng-container actions>
        @if (canEdit()) {
          <p-button label="Add User" icon="pi pi-user-plus"
                    (onClick)="onNewUserStakeholder()" />
          <p-button label="Add Non-User" icon="pi pi-building" severity="secondary"
                    [outlined]="true" (onClick)="onNewNonUserStakeholder()" />
        }
      </ng-container>

      @if (errorMessage()) {
        <p-message severity="error" [text]="errorMessage()!" />
      }

      <p-table #dt [value]="stakeholders()" [loading]="loading()" [paginator]="true" [rows]="20"
               [rowHover]="true" selectionMode="single" (onRowSelect)="onRowSelect($event)"
               [globalFilterFields]="['name', 'type', 'userDetails.emailAddress', 'userDetails.teamName']">
        <ng-template #header>
          <tr>
            <th pSortableColumn="name">Name <p-sortIcon field="name" /></th>
            <th pSortableColumn="type">Type <p-sortIcon field="type" /></th>
            <th>Team</th>
            <th>Email</th>
            <th>Phone</th>
            <th>Created By</th>
          </tr>
        </ng-template>
        <ng-template #body let-s>
          <tr [pSelectableRow]="s">
            <td>{{ s.name }}</td>
            <td>{{ s.type === 'user' ? 'User' : 'Non-User' }}</td>
            <td>{{ s.userDetails?.teamName }}</td>
            <td>{{ s.userDetails?.emailAddress }}</td>
            <td>{{ s.userDetails?.phoneNumber }}</td>
            <td>{{ s.createdBy }}</td>
          </tr>
        </ng-template>
        <ng-template #emptymessage>
          <tr><td colspan="6" class="text-center">No stakeholders found.</td></tr>
        </ng-template>
      </p-table>
    </app-list-page>
  `
})
export class StakeholderListComponent implements OnInit, OnDestroy {
  stakeholders = signal<StakeholderDto[]>([]);
  loading = signal(true);
  errorMessage = signal<string | null>(null);
  canEdit = signal(false);

  private projectName = '';
  private paramSub?: Subscription;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private stakeholderService: StakeholderService,
    private permissionService: PermissionService
  ) {}

  ngOnInit(): void {
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
