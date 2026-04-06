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
import { ActivatedRoute, Router } from '@angular/router';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { MessageModule } from 'primeng/message';
import { UseCaseDto } from '../../models/use-case';
import { UseCaseService } from '../../core/use-case.service';
import { PermissionService } from '../../core/permission.service';
import { ListPageComponent } from '../../shared/list-page';

@Component({
  selector: 'app-use-case-list',
  standalone: true,
  imports: [ListPageComponent, TableModule, ButtonModule, MessageModule],
  template: `
    <app-list-page title="Use Cases" [showSearch]="false">
      <ng-container actions>
        @if (canEdit()) {
          <p-button label="New Use Case" icon="pi pi-plus"
                    (onClick)="onCreate()" />
        }
      </ng-container>
      @if (errorMessage()) {
        <p-message severity="error" [text]="errorMessage()!" />
      }
      <p-table [value]="useCases()" [loading]="loading()"
               selectionMode="single" (onRowSelect)="onSelect($event)"
               [rowHover]="true" styleClass="p-datatable-sm">
        <ng-template pTemplate="header">
          <tr>
            <th>Name</th>
            <th>Primary Actor</th>
            <th>Created By</th>
          </tr>
        </ng-template>
        <ng-template pTemplate="body" let-uc>
          <tr [pSelectableRow]="uc">
            <td>{{ uc.name }}</td>
            <td>{{ uc.primaryActorName ?? '—' }}</td>
            <td>{{ uc.createdBy }}</td>
          </tr>
        </ng-template>
        <ng-template pTemplate="emptymessage">
          <tr><td colspan="3" style="text-align:center">No use cases yet.</td></tr>
        </ng-template>
      </p-table>
    </app-list-page>
  `,
  styles: []
})
export class UseCaseListComponent implements OnInit {
  useCases = signal<UseCaseDto[]>([]);
  loading = signal(true);
  errorMessage = signal<string | null>(null);
  canEdit = signal(false);

  private projectName = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private useCaseService: UseCaseService,
    private permissionService: PermissionService
  ) {}

  async ngOnInit(): Promise<void> {
    this.projectName = this.route.snapshot.paramMap.get('name') ?? '';
    await this.permissionService.loadForProject(this.projectName);
    this.canEdit.set(this.permissionService.canEdit('UseCase'));
    try {
      this.useCases.set(await this.useCaseService.listUseCases(this.projectName));
    } catch {
      this.errorMessage.set('Failed to load use cases.');
    } finally {
      this.loading.set(false);
    }
  }

  onSelect(event: { data?: UseCaseDto | UseCaseDto[] }): void {
    const uc = Array.isArray(event.data) ? event.data[0] : event.data;
    if (uc) this.router.navigate(['/projects', this.projectName, 'use-cases', uc.id]);
  }

  onCreate(): void {
    this.router.navigate(['/projects', this.projectName, 'use-cases', 'new']);
  }
}
