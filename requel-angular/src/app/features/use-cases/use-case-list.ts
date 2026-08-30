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
import { Component, OnInit, TemplateRef, ViewChild, signal, ChangeDetectionStrategy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { SubmitErrorComponent } from '../../shared/app-submit-error';
import { UseCaseDto } from '../../models/use-case';
import { UseCaseService } from '../../core/use-case.service';
import { PermissionService } from '../../core/permission.service';
import { ListPageComponent } from '../../shared/list-page';
import { AppDataTableComponent, DataTableColumn, RowAction } from '../../shared/app-data-table';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-use-case-list',
  standalone: true,
  imports: [ListPageComponent, AppDataTableComponent, ButtonModule, SubmitErrorComponent],
  template: `
    <app-list-page title="Use Cases" [fill]="true" [showSearch]="false">
      <app-submit-error [message]="errorMessage()" testid="use-case-list-error" />

      <app-data-table scrollHeight="flex" [value]="useCases()" [columns]="columns" [loading]="loading()"
                      [rowActions]="rowActions" searchPlaceholder="Search use cases..."
                      [globalFilterFields]="['name', 'primaryActorName', 'createdBy']"
                      testid="use-case-list" (rowClick)="onSelect({ data: $event })"
                      emptyTitle="No use cases yet"
                      emptyMessage="Capture the goals actors accomplish with this system."
                      emptyIcon="pi-directions" emptyActionLabel="New Use Case"
                      [showEmptyAction]="canEdit()" (emptyAction)="onCreate()">
        <div toolbarActions>
          @if (canEdit()) {
            <p-button label="New Use Case" icon="pi pi-plus" (onClick)="onCreate()" />
          }
        </div>
      </app-data-table>
    </app-list-page>

    <ng-template #primaryActorCell let-uc>{{ uc.primaryActorName ?? '—' }}</ng-template>
  `,
  styles: [`
    /* Fill mode (#221): claim main-content's height so the data-table body
       scrolls between a pinned header and the paginator. */
    :host { display: flex; flex-direction: column; flex: 1; min-height: 0; }
  `]
})
export class UseCaseListComponent implements OnInit {
  useCases = signal<UseCaseDto[]>([]);
  loading = signal(true);
  errorMessage = signal<string | null>(null);
  canEdit = signal(false);

  @ViewChild('primaryActorCell', { static: true }) primaryActorCell!: TemplateRef<{ $implicit: UseCaseDto }>;
  columns: DataTableColumn<UseCaseDto>[] = [];
  rowActions: RowAction<UseCaseDto>[] = [
    { label: 'Open', icon: 'pi pi-eye', command: uc => this.onSelect({ data: uc }) }
  ];

  private projectName = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private useCaseService: UseCaseService,
    private permissionService: PermissionService
  ) {}

  async ngOnInit(): Promise<void> {
    this.columns = [
      { field: 'name', header: 'Name', sortable: true, link: uc => ['/projects', this.projectName, 'use-cases', uc.id] },
      { field: 'primaryActorName', header: 'Primary Actor', cellTemplate: this.primaryActorCell },
      { field: 'createdBy', header: 'Created By', sortable: true }
    ];
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
