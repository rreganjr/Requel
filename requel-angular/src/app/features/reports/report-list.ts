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
import { Component, OnInit, signal, ChangeDetectionStrategy, inject, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { SubmitErrorComponent } from '../../shared/app-submit-error';
import { ReportGeneratorDto } from '../../models/report';
import { ReportService } from '../../core/report.service';
import { PermissionService } from '../../core/permission.service';
import { ListPageComponent } from '../../shared/list-page';
import { AppDataTableComponent, DataTableColumn } from '../../shared/app-data-table';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-report-list',
  standalone: true,
  imports: [ListPageComponent, AppDataTableComponent, ButtonModule, SubmitErrorComponent],
  template: `
    <app-list-page title="Documents" [showSearch]="false">
      <app-submit-error [message]="errorMessage()" testid="report-list-error" />

      <app-data-table [value]="reports()" [columns]="columns" [loading]="loading()"
                      [rowClickable]="false" rowTestid="report-list-row"
                      searchPlaceholder="Search documents..."
                      [globalFilterFields]="['name', 'createdBy']" testid="report-list"
                      emptyTitle="No documents yet"
                      emptyMessage="Create a document generator to produce reports from this project."
                      emptyIcon="pi-file" emptyActionLabel="New Document"
                      [showEmptyAction]="canEdit()" (emptyAction)="onNew()">
        <div toolbarActions>
          @if (canEdit()) {
            <p-button label="New Document" icon="pi pi-plus" data-testid="report-list-new" (onClick)="onNew()" />
          }
        </div>
        <ng-template #rowActions let-r>
          <div class="action-cell">
            <p-button label="Edit" icon="pi pi-pencil" size="small" [text]="true"
                      data-testid="report-list-edit" (onClick)="onEdit(r)" />
            <p-button label="Run" icon="pi pi-play" size="small" [text]="true"
                      data-testid="report-list-run" severity="success"
                      (onClick)="onRun(r)" [loading]="runningId() === r.id" />
          </div>
        </ng-template>
      </app-data-table>
    </app-list-page>
  `,
  styles: [`
    .action-cell { display: flex; gap: 0.25rem; justify-content: flex-end; }
  `]
})
export class ReportListComponent implements OnInit {
  reports = signal<ReportGeneratorDto[]>([]);
  loading = signal(true);
  errorMessage = signal<string | null>(null);
  runningId = signal<number | null>(null);
  canEdit = signal(false);

  columns: DataTableColumn<ReportGeneratorDto>[] = [
    { field: 'name', header: 'Name', sortable: true, link: r => ['/projects', this.projectName, 'reports', r.id] },
    { field: 'createdBy', header: 'Created By', sortable: true }
  ];

  private projectName = '';
  private readonly destroyRef = inject(DestroyRef);

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private reportService: ReportService,
    private permissionService: PermissionService
  ) {}

  ngOnInit(): void {
    this.route.paramMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(async params => {
      const name = params.get('name') ?? '';
      if (name !== this.projectName) {
        this.projectName = name;
        await this.permissionService.loadForProject(name);
        this.canEdit.set(this.permissionService.canEdit('ReportGenerator'));
        this.loadReports();
      }
    });
  }

  async loadReports(): Promise<void> {
    this.loading.set(true);
    try {
      this.reports.set(await this.reportService.listReports(this.projectName));
    } catch {
      this.errorMessage.set('Failed to load documents.');
    } finally {
      this.loading.set(false);
    }
  }

  onEdit(r: ReportGeneratorDto): void {
    this.router.navigate(['/projects', this.projectName, 'reports', r.id]);
  }

  async onRun(r: ReportGeneratorDto): Promise<void> {
    this.runningId.set(r.id);
    try {
      await this.reportService.downloadReport(this.projectName, r.id, r.name);
    } catch {
      this.errorMessage.set(`Failed to generate report "${r.name}".`);
    } finally {
      this.runningId.set(null);
    }
  }

  onNew(): void {
    this.router.navigate(['/projects', this.projectName, 'reports', 'new']);
  }
}
