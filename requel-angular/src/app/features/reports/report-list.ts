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
import { ReportGeneratorDto } from '../../models/report';
import { ReportService } from '../../core/report.service';
import { PermissionService } from '../../core/permission.service';
import { ListPageComponent } from '../../shared/list-page';

@Component({
  selector: 'app-report-list',
  standalone: true,
  imports: [ListPageComponent, TableModule, ButtonModule, MessageModule],
  template: `
    <app-list-page title="Documents" searchPlaceholder="Search documents..."
                   (search)="dt.filterGlobal($event, 'contains')">
      <ng-container actions>
        @if (canEdit()) {
          <p-button label="New Document" icon="pi pi-plus" (onClick)="onNew()" />
        }
      </ng-container>

      @if (errorMessage()) {
        <p-message severity="error" [text]="errorMessage()!" />
      }

      <p-table #dt [value]="reports()" [loading]="loading()" [paginator]="true" [rows]="20"
               [rowHover]="true" [globalFilterFields]="['name', 'createdBy']">
        <ng-template #header>
          <tr>
            <th pSortableColumn="name">Name <p-sortIcon field="name" /></th>
            <th pSortableColumn="createdBy">Created By <p-sortIcon field="createdBy" /></th>
            <th>Actions</th>
          </tr>
        </ng-template>
        <ng-template #body let-r>
          <tr>
            <td>{{ r.name }}</td>
            <td>{{ r.createdBy }}</td>
            <td class="action-cell">
              <p-button label="Edit" icon="pi pi-pencil" size="small" [text]="true"
                        (onClick)="onEdit(r)" />
              <p-button label="Run" icon="pi pi-play" size="small" [text]="true"
                        severity="success" (onClick)="onRun(r)" [loading]="runningId() === r.id" />
            </td>
          </tr>
        </ng-template>
        <ng-template #emptymessage>
          <tr><td colspan="3" class="text-center">No documents found.</td></tr>
        </ng-template>
      </p-table>
    </app-list-page>
  `,
  styles: [`
    .text-center { text-align: center; }
    .action-cell { display: flex; gap: 0.25rem; }
  `]
})
export class ReportListComponent implements OnInit, OnDestroy {
  reports = signal<ReportGeneratorDto[]>([]);
  loading = signal(true);
  errorMessage = signal<string | null>(null);
  runningId = signal<number | null>(null);
  canEdit = signal(false);

  private projectName = '';
  private paramSub?: Subscription;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private reportService: ReportService,
    private permissionService: PermissionService
  ) {}

  ngOnInit(): void {
    this.paramSub = this.route.paramMap.subscribe(async params => {
      const name = params.get('name') ?? '';
      if (name !== this.projectName) {
        this.projectName = name;
        await this.permissionService.loadForProject(name);
        this.canEdit.set(this.permissionService.canEdit('ReportGenerator'));
        this.loadReports();
      }
    });
  }

  ngOnDestroy(): void {
    this.paramSub?.unsubscribe();
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
