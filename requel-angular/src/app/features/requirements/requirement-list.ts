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
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Requel. If not, see <http://www.gnu.org/licenses/>.
 *
 */
import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { ButtonModule } from 'primeng/button';
import { SelectModule } from 'primeng/select';
import { FormsModule } from '@angular/forms';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import { AppDataTableComponent, DataTableColumn, RowAction } from '../../shared/app-data-table';
import { AppCardComponent } from '../../shared/app-card';
import { PageHeaderComponent } from '../../shared/page-header';
import { LoadingStateComponent } from '../../shared/loading-state';
import { ErrorStateComponent } from '../../shared/error-state';
import { RequirementService } from '../../core/requirement.service';
import { RequirementDto, RequirementStatus, getStatusLabel, getStatusSeverity, REVIEW_DEPARTMENTS, getDepartmentLabel } from '../../models/requirement';
import { ProjectService } from '../../core/project.service';
import { PermissionService } from '../../core/permission.service';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, ParamMap } from '@angular/router';
import { DestroyRef } from '@angular/core';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-requirement-list',
  standalone: true,
  imports: [DatePipe, PageHeaderComponent, AppCardComponent, RouterLink, ButtonModule, SelectModule, FormsModule,
            TagModule, TooltipModule, AppDataTableComponent, LoadingStateComponent, ErrorStateComponent],
  template: `
    <div class="requirement-list" data-testid="requirement-list">
      <div class="page-header">
        <app-page-header title="Requirements" subtitle="Review workflow" />
        <div class="page-actions">
          <p-button label="New Requirement" icon="pi pi-plus" data-testid="req-new"
                    [routerLink]="['/projects', projectName, 'requirements', 'new']" />
        </div>
      </div>

      <div class="toolbar">
        <p-select [(ngModel)]="statusFilter" [options]="statusOptions" placeholder="All Statuses"
                  [showClear]="true" styleClass="w-auto" data-testid="req-status-filter"
                  (onChange)="loadRequirements()" />
      </div>

      @if (loading()) {
        <app-card>
          <app-loading-state label="Loading requirements…" [lines]="3" testid="req-list-loading" />
        </app-card>
      } @else if (loadError()) {
        <app-error-state [message]="loadError()!" testid="req-list-load-error" (retry)="loadRequirements()" />
      } @else {
        <app-data-table
          [value]="requirements()"
          [columns]="columns"
          [loading]="false"
          [paginator]="true"
          [rows]="20"
          [showToolbar]="false"
          [rowClickable]="true"
          [defaultActions]="false"
          [rowActions]="rowActions"
          [dataKey]="'id'"
          [testid]="'requirement-table'"
          (rowClick)="onRowClick($event)"
        >
          <ng-template #row let-req>
            <td>
              <span class="req-id">{{ req.requirementId }}</span>
            </td>
            <td class="req-text-cell">
              <div class="req-shall-text">{{ req.shallText }}</div>
            </td>
            <td>
              <p-tag [value]="getStatusLabel(req.status)" [severity]="getStatusSeverity(req.status)" />
            </td>
            <td>
              <div class="review-badges">
                @for (dept of reviewDepartments; track dept.value) {
                  <span class="review-badge"
                        [class.passed]="getReviewResult(req, dept.value) === 'PASSED'"
                        [class.failed]="getReviewResult(req, dept.value) === 'FAILED'"
                        [class.pending]="getReviewResult(req, dept.value) === 'NOT_REVIEWED'"
                        pTooltip="{{ getDepartmentLabel(dept.value) }}: {{ getReviewResultLabel(getReviewResult(req, dept.value)) }}"
                        tooltipPosition="top">
                    <i [class]="dept.icon"></i>
                  </span>
                }
              </div>
            </td>
            <td>{{ req.createdBy ?? '—' }}</td>
            <td>{{ req.updatedAt | date:'short' }}</td>
          </ng-template>
        </app-data-table>
      }
    </div>
  `,
  styles: [`
    .page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem; }
    .page-actions { display: flex; gap: 0.5rem; }
    .toolbar { margin-bottom: 1rem; }
    .req-id { font-family: monospace; font-weight: 600; color: var(--p-primary-color); }
    .req-text-cell { max-width: 40rem; }
    .req-shall-text { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; font-size: 0.875rem; }
    .review-badges { display: flex; gap: 0.25rem; flex-wrap: wrap; }
    .review-badge {
      display: inline-flex; align-items: center; justify-content: center;
      width: 1.5rem; height: 1.5rem; border-radius: 50%;
      background: var(--p-content-border-color); color: var(--p-text-muted-color);
      font-size: 0.7rem;
    }
    .review-badge.passed { background: var(--p-green-100); color: var(--p-green-600); }
    .review-badge.failed { background: var(--p-red-100); color: var(--p-red-600); }
    .review-badge.pending { background: var(--p-gray-100); color: var(--p-gray-500); }
    .review-badge i { font-size: 0.75rem; }
  `]
})
export class RequirementListComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly requirementService = inject(RequirementService);
  private readonly projectService = inject(ProjectService);
  private readonly permissionService = inject(PermissionService);
  private readonly destroyRef = inject(DestroyRef);

  projectName = '';
  loading = signal(true);
  loadError = signal<string | null>(null);
  requirements = signal<RequirementDto[]>([]);
  statusFilter = signal<RequirementStatus | null>(null);

  readonly statusOptions = [
    { label: 'All Statuses', value: null },
    ...[
      { label: 'Draft', value: 'DRAFT' as RequirementStatus },
      { label: 'In Review', value: 'IN_REVIEW' as RequirementStatus },
      { label: 'Approved', value: 'APPROVED' as RequirementStatus },
      { label: 'Published', value: 'PUBLISHED' as RequirementStatus },
      { label: 'Rejected', value: 'REJECTED' as RequirementStatus },
    ]
  ];

  readonly reviewDepartments = REVIEW_DEPARTMENTS;

  readonly columns: DataTableColumn<RequirementDto>[] = [
    { field: 'requirementId', header: 'ID', sortable: true, class: 'req-id-col' },
    { field: 'shallText', header: 'Shall Statement', sortable: false },
    { field: 'status', header: 'Status', sortable: true, class: 'status-col' },
    { field: 'reviews', header: 'Reviews', sortable: false, class: 'reviews-col' },
    { field: 'createdBy', header: 'Author', sortable: true },
    { field: 'updatedAt', header: 'Updated', sortable: true },
  ];

  readonly rowActions: RowAction<RequirementDto>[] = [
    {
      label: 'Open',
      icon: 'pi pi-eye',
      command: (req) => this.router.navigate(['/projects', this.projectName, 'requirements', req.id])
    },
    {
      label: 'Edit',
      icon: 'pi pi-pencil',
      visible: (req) => this.canEdit() && req.status === 'DRAFT',
      command: (req) => this.router.navigate(['/projects', this.projectName, 'requirements', req.id, 'edit'])
    },
    {
      label: 'Delete',
      icon: 'pi pi-trash',
      visible: (req) => this.canDelete() && req.status === 'DRAFT',
      command: (req) => this.onDelete(req)
    }
  ];

  canEdit = computed(() => this.permissionService.canEdit('Requirement'));
  canDelete = computed(() => this.permissionService.canDelete('Requirement'));

  ngOnInit(): void {
    this.route.paramMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(async (params: ParamMap) => {
      this.projectName = params.get('name') ?? '';
      await this.permissionService.loadForProject(this.projectName);
      this.loadRequirements();
    });
  }

  async loadRequirements(): Promise<void> {
    this.loading.set(true);
    this.loadError.set(null);
    try {
      const reqs = await this.requirementService.listRequirements(this.projectName, this.statusFilter() ?? undefined);
      this.requirements.set(reqs);
    } catch (e: any) {
      this.loadError.set('Failed to load requirements.');
    } finally {
      this.loading.set(false);
    }
  }

  onRowClick(req: RequirementDto): void {
    this.router.navigate(['/projects', this.projectName, 'requirements', req.id]);
  }

  async onDelete(req: RequirementDto): Promise<void> {
    if (!confirm(`Delete requirement ${req.requirementId}?`)) return;
    try {
      await this.requirementService.deleteRequirement(this.projectName, req.id, req.version);
      this.loadRequirements();
    } catch (e: any) {
      alert('Delete failed: ' + (e.error?.message ?? e.message));
    }
  }

  getReviewResult(req: RequirementDto, department: 'QUALITY' | 'FEASIBILITY' | 'TESTABILITY'): 'PASSED' | 'FAILED' | 'NOT_REVIEWED' {
    const review = req.reviews?.find(r => r.department === department);
    return review?.result ?? 'NOT_REVIEWED';
  }

  getReviewResultLabel(result: 'PASSED' | 'FAILED' | 'NOT_REVIEWED'): string {
    switch (result) {
      case 'PASSED': return 'Passed';
      case 'FAILED': return 'Failed';
      default: return 'Not Reviewed';
    }
  }

  getStatusLabel = getStatusLabel;
  getStatusSeverity = getStatusSeverity;
  getDepartmentLabel = getDepartmentLabel;
}
