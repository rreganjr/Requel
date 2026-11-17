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
import { ChangeDetectionStrategy, Component, computed, DestroyRef, inject, OnDestroy, OnInit, signal, ViewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NgTemplateOutlet } from '@angular/common';
import { DatePipe } from '@angular/common';
import { PageHeaderComponent } from '../../shared/page-header';
import { AppCardComponent } from '../../shared/app-card';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { DirtyCheckable } from '../../core/dirty-check.guard';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { InputText } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import { SubmitErrorComponent } from '../../shared/app-submit-error';
import { UpdateBannerComponent } from '../../shared/app-update-banner';
import { AnnouncerService } from '../../core/announcer.service';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ConfirmationService, MessageService } from 'primeng/api';
import { CommandResult } from '../../models/command';
import { RequirementDto, RequirementReviewDto, RequirementStatus, ReviewDepartment, ReviewResult,
         getStatusLabel, getStatusSeverity, getDepartmentLabel, REVIEW_DEPARTMENTS,
         canReviewDepartment, canApprove, canPublish, getReviewResultLabel, getReviewResultSeverity } from '../../models/requirement';
import { EntityReferenceDto } from '../../models/entity-reference';
import { RequirementService } from '../../core/requirement.service';
import { CommandService, isNetworkError } from '../../core/command.service';
import { ProjectService } from '../../core/project.service';
import { PermissionService } from '../../core/permission.service';
import { EventStreamService } from '../../core/event-stream.service';
import { AnnotationsSectionComponent } from '../../shared/annotations-section';
import { SourcesSectionComponent } from '../../shared/sources-section';
import { AppFieldComponent, AppFieldControlDirective } from '../../shared/app-field';
import { LoadingStateComponent } from '../../shared/loading-state';
import { ErrorStateComponent } from '../../shared/error-state';
import { AppFormWizardComponent, AppWizardStepComponent, WizardCommitRequest } from '../../shared/app-form-wizard';
import { ARTIFACT_NAME_MAX_LENGTH } from '../../shared/validation-limits';
import { SelectModule } from 'primeng/select';
import { DialogModule } from 'primeng/dialog';
import { AppDataTableComponent } from '../../shared/app-data-table';
import { FormsModule } from '@angular/forms';

const STALE_VERSION_MESSAGE =
  'This requirement was changed elsewhere. Your copy has been refreshed — review the values and continue.';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-requirement-editor',
  standalone: true,
  imports: [PageHeaderComponent, AppCardComponent, RouterLink, ReactiveFormsModule, NgTemplateOutlet,
            ButtonModule, InputText, TextareaModule, TagModule, TooltipModule,
            SubmitErrorComponent, UpdateBannerComponent, ConfirmDialogModule,
            AnnotationsSectionComponent, SourcesSectionComponent, AppFieldComponent, AppFieldControlDirective,
            AppFormWizardComponent, AppWizardStepComponent,
            LoadingStateComponent, ErrorStateComponent, SelectModule, DialogModule, AppDataTableComponent, FormsModule],
  providers: [ConfirmationService, DatePipe],
  template: `
    <div class="requirement-editor" data-testid="requirement-editor">
      <div class="page-header">
        <app-page-header [title]="isNew() ? 'New Requirement' : requirementName()" />
        <div class="page-actions">
          <p-button label="Back" icon="pi pi-arrow-left" severity="secondary" data-testid="req-back"
                    [outlined]="true" (onClick)="onBack()" />
          @if (!isNew()) {
            @if (canEdit()) {
              <p-button label="Copy" icon="pi pi-copy" severity="secondary" data-testid="req-copy"
                        [outlined]="true" (onClick)="onCopy()" />
            }
            @if (canDelete() && requirement()?.status === 'DRAFT') {
              <p-button label="Delete" icon="pi pi-trash" severity="danger" data-testid="req-delete"
                        [outlined]="true" (onClick)="onDelete()" />
            }
          }
        </div>
      </div>

      <app-submit-error [message]="errorMessage()" testid="req-editor-error" [retryable]="retryable()" (retry)="onSave()" />
      @if (updateAvailable()) {
        <app-update-banner message="This requirement was changed elsewhere. Your unsaved changes are preserved."
                           testid="req-update-banner"
                           (reload)="reloadFromExternalChange()" (dismiss)="updateAvailable.set(false)" />
      }

      @if (loading()) {
        <app-card>
          <app-loading-state label="Loading requirement…" [lines]="4" testid="req-editor-loading" />
        </app-card>
      } @else if (loadError()) {
        <app-error-state [message]="loadError()!" testid="req-editor-load-error" (retry)="retryLoad()" />
      } @else if (isNew()) {
        <app-form-wizard
          [(activeKey)]="wizardStep"
          navLabel="New requirement steps"
          (stepCommit)="onStepCommit($event)"
          (cancelled)="onBack()"
          (finished)="onWizardFinished()"
          data-testid="req-wizard"
        >
          <app-wizard-step key="details" label="Details" helper="Requirement ID and shall statement"
                           [form]="detailsForm">
            <ng-template>
              <ng-container [ngTemplateOutlet]="detailsFields" />
            </ng-template>
          </app-wizard-step>

          <app-wizard-step key="source" label="Source" helper="Link to originating idea" [optional]="true">
            <ng-template>
              <ng-container [ngTemplateOutlet]="sourceSection" />
            </ng-template>
          </app-wizard-step>
        </app-form-wizard>
      } @else {
        <app-card>
          <ng-container [ngTemplateOutlet]="detailsFields" />

          <div class="form-actions">
            @if (canEdit() && requirement()?.status === 'DRAFT') {
              <p-button label="Save" icon="pi pi-check" data-testid="req-save"
                        [disabled]="!canSave()" [loading]="saving()" (onClick)="onSave()" />
            }
          </div>
        </app-card>

        <!-- Review Status Panel -->
        <app-card class="review-status-card">
          <h2 class="rq-section-title">Review Status</h2>
          <p-tag [value]="getStatusLabel(reqStatus())" [severity]="getStatusSeverity(reqStatus())" styleClass="mb-3" />

          <div class="review-grid">
            @for (dept of reviewDepartments; track dept.value) {
              <div class="review-card" [class.can-review]="canReviewDept(dept.value)">
                <div class="review-header">
                  <i [class]="dept.icon" class="review-icon"></i>
                  <div>
                    <div class="review-title">{{ getDepartmentLabel(dept.value) }}</div>
                    <div class="review-desc">{{ dept.description }}</div>
                  </div>
                </div>
                <div class="review-result">
                  <p-tag [value]="getReviewResultLabel(getReviewResult(dept.value))"
                         [severity]="getReviewResultSeverity(getReviewResult(dept.value))" />
                </div>
                <div class="review-comments" *ngIf="getReviewComments(dept.value)">
                  <strong>Comments:</strong> {{ getReviewComments(dept.value) }}
                </div>
                <div class="review-meta" *ngIf="getReviewer(dept.value)">
                  Reviewed by {{ getReviewer(dept.value) }} ({{ getReviewerRole(dept.value) }})
                  @if (getReviewedAt(dept.value)) { · {{ getReviewedAt(dept.value) | date:'short' }} }
                </div>
                @if (canReviewDept(dept.value) && reqStatus() === 'IN_REVIEW') {
                  <p-button label="Submit Review" icon="pi pi-pencil" size="small"
                            [attr.data-testid]="'req-review-' + dept.value.toLowerCase()"
                            (onClick)="openReviewDialog(dept.value)" />
                }
              </div>
            }
          </div>

          <!-- Approve / Publish Actions -->
          <div class="review-actions" *ngIf="reqStatus() === 'IN_REVIEW' && allReviewsPassed()">
            <hr />
            @if (canApproveUser()) {
              <p-button label="Approve Requirement" icon="pi pi-check-circle" severity="success"
                        data-testid="req-approve" (onClick)="onApprove()" />
            }
          </div>
          <div class="review-actions" *ngIf="reqStatus() === 'APPROVED'">
            <hr />
            @if (canPublishUser()) {
              <p-button label="Publish to Jama" icon="pi pi-upload" severity="info"
                        data-testid="req-publish" (onClick)="onPublish()" />
            }
            @if (canApproveUser()) {
              <p-button label="Reopen for Review" icon="pi pi-undo" severity="secondary" [outlined]="true"
                        data-testid="req-reopen" (onClick)="onReopen()" />
            }
          </div>
        </app-card>

        <!-- Overlap Comparisons -->
        <app-card *ngIf="comparisons().length" class="comparisons-card">
          <h2 class="rq-section-title">Overlap Comparisons</h2>
          <app-data-table
            [value]="comparisons()"
            [columns]="comparisonColumns"
            [paginator]="true"
            [rows]="10"
            [showToolbar]="false"
            [defaultActions]="false"
            [rowActions]="comparisonRowActions"
            [dataKey]="'id'"
            [testid]="'req-comparisons'"
          >
            <ng-template #row let-c>
              <td>{{ c.existingRequirementRef?.requirementId ?? c.existingRequirementRef?.name }}</td>
              <td class="comparison-finding">{{ c.aiFinding }}</td>
              <td>
                <p-tag *ngIf="c.resolution" [value]="c.resolution" [severity]="getResolutionSeverity(c.resolution)" />
                <span *ngIf="!c.resolution" class="text-muted">Pending</span>
              </td>
              <td>{{ c.resolvedBy ?? '—' }}</td>
              <td>{{ c.resolvedAt ? (c.resolvedAt | date:'short') : '—' }}</td>
            </ng-template>
          </app-data-table>
        </app-card>

        <ng-container [ngTemplateOutlet]="sourceSection" />

        <!-- Annotations & Sources -->
        @if (requirementId != null) {
          <app-annotations-section
            [projectName]="projectName"
            entityType="Requirement"
            [entityId]="requirementId"
            />
            <app-sources-section
              [projectName]="projectName"
              entityType="Requirement"
              [entityId]="requirementId"
              />
        }
      }

      <!-- Review Dialog -->
      <p-dialog [visible]="reviewDialogVisible()" (visibleChange)="onReviewDialogVisibleChange($event)"
                [modal]="true" [focusOnShow]="true" [dismissableMask]="true" [style]="{ width: '40rem' }"
                [header]="'Review: ' + (reviewDialogDept() ? getDepartmentLabel(reviewDialogDept()!) : '')"
                appendTo="body" data-testid="req-review-dialog">
        <div class="dialog-body">
          <div class="field">
            <label>Result</label>
            <p-select [(ngModel)]="reviewDialogResult" [options]="reviewResultOptions"
                      placeholder="Select result" ariaLabel="Review result" />
          </div>
          <div class="field">
            <label>Comments</label>
            <textarea pTextarea [(ngModel)]="reviewDialogComments" rows="4" [autoResize]="true"
                      placeholder="Review comments (required for FAILED)" ariaLabel="Review comments"></textarea>
          </div>
          <div class="dialog-actions">
            <p-button label="Submit" icon="pi pi-check" (onClick)="submitReview()" [loading]="submittingReview()" />
            <p-button label="Cancel" severity="secondary" [outlined]="true" (onClick)="reviewDialogVisible.set(false)" />
          </div>
        </div>
      </p-dialog>

      <!-- Compare Dialog -->
      <p-dialog [visible]="compareDialogVisible()" (visibleChange)="onCompareDialogVisibleChange($event)"
                [modal]="true" [focusOnShow]="true" [dismissableMask]="true" [style]="{ width: '50rem' }"
                header="Compare with Existing Requirements" appendTo="body" data-testid="req-compare-dialog">
        <div class="dialog-body">
          <div class="field">
            <label>Search Jama Requirements</label>
            <div class="search-row">
              <input pInputText [(ngModel)]="compareSearchQuery" placeholder="Search query..." />
              <p-button label="Search" icon="pi pi-search" (onClick)="searchJamaRequirements()" [loading]="compareSearching()" />
            </div>
          </div>
          <div class="field" *ngIf="jamaSearchResults().length">
            <label>Select Requirements to Compare</label>
            <div class="checkbox-list">
              @for (r of jamaSearchResults(); track r.id) {
                <div class="checkbox-item">
                  <input type="checkbox" [id]="'jama-' + r.id" [(ngModel)]="selectedJamaIds" [value]="r.id" />
                  <label [for]="'jama-' + r.id">{{ r.name }} ({{ r.id }})</label>
                </div>
              }
            </div>
          </div>
          <div class="dialog-actions">
            <p-button label="Compare" icon="pi pi-sitemap" (onClick)="runComparison()" [disabled]="!selectedJamaIds.length" [loading]="compareRunning()" />
            <p-button label="Cancel" severity="secondary" [outlined]="true" (onClick)="compareDialogVisible.set(false)" />
          </div>
        </div>
      </p-dialog>

      <p-confirmDialog />

      <!-- Shared templates -->
      <ng-template #detailsFields>
        <app-field label="Requirement ID" helper="Auto-generated if left blank."
                   [control]="detailsForm.controls.requirementId"
                   [submitted]="submitted()">
          <input appFieldControl pInputText [formControl]="detailsForm.controls.requirementId"
                 placeholder="e.g., REQ-123" data-testid="req-id" />
        </app-field>

        <app-field label="Shall Statement" [control]="detailsForm.controls.shallText" [divider]="false"
                   [errorMessages]="shallTextErrors" [submitted]="submitted()">
          <textarea appFieldControl pTextarea [formControl]="detailsForm.controls.shallText" rows="6" [autoResize]="true"
                    placeholder="The system shall..." data-testid="req-shall-text"></textarea>
        </app-field>
      </ng-template>

      <ng-template #sourceSection>
        <app-card>
          <h3 class="rq-section-title">Source Idea</h3>
          @if (requirement()?.ideaId) {
            <div class="source-info">
              <strong>{{ requirement()?.ideaName ?? 'Idea #' + requirement()?.ideaId }}</strong>
              <p class="source-text">{{ requirement()?.sourceText }}</p>
              <p-button label="View Idea" icon="pi pi-external-link" size="small" [outlined]="true"
                        (onClick)="navigateToIdea()" />
            </div>
          } @else {
            <p class="text-muted">No source idea linked.</p>
          }
        </app-card>
      </ng-template>
    </div>
  `,
  styles: [`
    .page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem; }
    .page-actions { display: flex; gap: 0.5rem; }
    .form-actions { margin-top: 1rem; }
    .review-status-card { margin-top: 1.5rem; }
    .review-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 1rem; margin-top: 1rem; }
    .review-card {
      border: 1px solid var(--p-content-border-color); border-radius: var(--rq-radius-md);
      padding: 1rem; background: var(--p-surface-card);
      transition: box-shadow 0.2s;
    }
    .review-card.can-review { box-shadow: 0 0 0 2px var(--p-primary-200); }
    .review-header { display: flex; gap: 0.75rem; margin-bottom: 0.75rem; }
    .review-icon { font-size: 1.5rem; color: var(--p-primary-color); }
    .review-title { font-weight: 600; }
    .review-desc { font-size: 0.8rem; color: var(--p-text-muted-color); }
    .review-result { margin-bottom: 0.5rem; }
    .review-comments { font-size: 0.875rem; margin-top: 0.5rem; padding-top: 0.5rem; border-top: 1px solid var(--p-content-border-color); }
    .review-meta { font-size: 0.75rem; color: var(--p-text-muted-color); margin-top: 0.5rem; }
    .review-actions { display: flex; gap: 0.5rem; margin-top: 1rem; flex-wrap: wrap; }
    .comparisons-card { margin-top: 1.5rem; }
    .comparison-finding { max-width: 30rem; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
    .source-info { padding: 1rem; background: var(--p-surface-hover); border-radius: var(--rq-radius-md); }
    .source-text { margin: 0.5rem 0; font-style: italic; color: var(--p-text-secondary-color); }
    .dialog-body { display: flex; flex-direction: column; gap: 1rem; }
    .field { display: flex; flex-direction: column; gap: 0.35rem; }
    .search-row { display: flex; gap: 0.5rem; }
    .search-row input { flex: 1; }
    .checkbox-list { max-height: 20rem; overflow-y: auto; border: 1px solid var(--p-content-border-color); border-radius: var(--rq-radius-sm); padding: 0.5rem; }
    .checkbox-item { display: flex; align-items: center; gap: 0.5rem; padding: 0.25rem 0; }
    .dialog-actions { display: flex; justify-content: flex-end; gap: 0.5rem; margin-top: 0.5rem; }
    .text-muted { color: var(--p-text-muted-color); }
  `]
})
export class RequirementEditorComponent implements OnInit, OnDestroy, DirtyCheckable {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly requirementService = inject(RequirementService);
  private readonly commandService = inject(CommandService);
  private readonly projectService = inject(ProjectService);
  private readonly permissionService = inject(PermissionService);
  private readonly eventStreamService = inject(EventStreamService);
  private readonly announcer = inject(AnnouncerService);
  private readonly messageService = inject(MessageService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly destroyRef = inject(DestroyRef);

  isNew = signal(true);
  requirementName = signal('');
  requirement = signal<RequirementDto | null>(null);
  errorMessage = signal<string | null>(null);
  retryable = signal(false);
  loading = signal(true);
  loadError = signal<string | null>(null);
  saving = signal(false);
  submitted = signal(false);
  updateAvailable = signal(false);
  comparisons = signal<any[]>([]);
  jamaSearchResults = signal<{ id: string; name: string; text: string }[]>([]);
  selectedJamaIds: string[] = [];
  compareSearchQuery = '';
  compareSearching = signal(false);
  compareRunning = signal(false);
  compareDialogVisible = signal(false);
  reviewDialogVisible = signal(false);
  reviewDialogDept = signal<ReviewDepartment | null>(null);
  reviewDialogResult = signal<ReviewResult>('PASSED');
  reviewDialogComments = '';
  submittingReview = signal(false);

  readonly reviewDepartments = REVIEW_DEPARTMENTS;
  readonly reviewResultOptions = [
    { label: 'Passed', value: 'PASSED' as ReviewResult },
    { label: 'Failed', value: 'FAILED' as ReviewResult },
  ];

  projectName = '';
  requirementId: number | null = null;
  private version: number | null = null;
  private sseBound = false;

  readonly detailsForm = new FormGroup({
    requirementId: new FormControl('', { nonNullable: true }),
    shallText: new FormControl('', { validators: [Validators.required], nonNullable: true }),
  });

  readonly shallTextErrors = { required: 'A shall statement is required.' };

  wizardStep = 'details';

  readonly reqStatus = computed(() => this.requirement()?.status ?? 'DRAFT');
  readonly allReviewsPassed = computed(() => this.requirement()?.allReviewsPassed ?? false);
  readonly canEdit = computed(() => this.permissionService.canEdit('Requirement'));
  readonly canDelete = computed(() => this.permissionService.canDelete('Requirement'));

  readonly comparisonColumns: any[] = [
    { field: 'existingRequirementRef.name', header: 'Existing Requirement', sortable: true },
    { field: 'aiFinding', header: 'AI Finding', sortable: false },
    { field: 'resolution', header: 'Resolution', sortable: true },
    { field: 'resolvedBy', header: 'Resolved By', sortable: true },
    { field: 'resolvedAt', header: 'Resolved At', sortable: true },
  ];

  readonly comparisonRowActions: any[] = [
    {
      label: 'Resolve',
      icon: 'pi pi-check',
      visible: (c: any) => !c.resolution,
      command: (c: any) => this.openResolveDialog(c)
    },
    {
      label: 'View',
      icon: 'pi pi-eye',
      command: (c: any) => this.viewComparison(c)
    }
  ];

  ngOnInit(): void {
    this.route.paramMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(async params => {
      this.projectName = params.get('name') ?? '';
      const idParam = params.get('requirementId') ?? '';
      const newIsNew = idParam === 'new';

      if (newIsNew) {
        this.isNew.set(true);
        this.requirement.set(null);
        this.requirementId = null;
        this.version = null;
        this.wizardStep = 'details';
        this.submitted.set(false);
        this.detailsForm.reset({ requirementId: '', shallText: '' });
        this.loading.set(false);
        this.loadError.set(null);
      }

      await this.permissionService.loadForProject(this.projectName);

      if (!newIsNew) {
        this.isNew.set(false);
        this.requirementId = +idParam;
        this.loadRequirement();
      }
    });
  }

  hasUnsavedChanges(): boolean {
    return this.detailsForm.dirty;
  }

  ngOnDestroy(): void {
    if (this.requirementId) {
      void this.eventStreamService.removeSubscription('Requirement', this.requirementId);
    }
  }

  private showError(message: string | null): void {
    this.errorMessage.set(message);
    this.retryable.set(false);
  }

  async reloadFromExternalChange(): Promise<void> {
    this.updateAvailable.set(false);
    this.detailsForm.markAsPristine();
    await this.loadRequirement(false);
    this.announcer.announce('Requirement reloaded.');
  }

  retryLoad(): void {
    void this.loadRequirement();
  }

  private async loadRequirement(skeleton = true): Promise<void> {
    if (skeleton) {
      this.loading.set(true);
      this.loadError.set(null);
    }
    try {
      const req = await this.requirementService.getRequirement(this.projectName, this.requirementId!);
      this.requirement.set(req);
      this.version = req.version;
      // Comparisons not in current model - using empty array
      this.comparisons.set([]);
      if (!this.hasUnsavedChanges()) {
        this.requirementName.set(req.requirementId);
        this.detailsForm.reset({ requirementId: req.requirementId, shallText: req.shallText });
      }
    } catch {
      if (skeleton) this.loadError.set('Failed to load requirement.');
      else this.showError('Failed to load requirement.');
    } finally {
      if (skeleton) this.loading.set(false);
    }
    if (this.requirementId && !this.sseBound) {
      void this.eventStreamService.addSubscription('Requirement', this.requirementId);
      this.sseBound = true;
      this.eventStreamService.events$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(envelope => {
        if (envelope.targetType !== 'Requirement' || envelope.targetId !== this.requirementId) return;
        if (envelope.eventType === 'TargetDeleted') {
          this.announcer.announce('This requirement was deleted in another session.');
          return;
        }
        const dirty = this.hasUnsavedChanges();
        void this.loadRequirement(false);
        if (dirty) {
          this.updateAvailable.set(true);
          this.announcer.announce('This requirement was changed elsewhere. Your unsaved changes are preserved.');
        } else {
          this.announcer.announceThrottled('Requirement:' + this.requirementId, 'This requirement was updated.');
        }
      });
    }
  }

  canSave(): boolean {
    return this.detailsForm.valid && this.detailsForm.dirty && !this.saving();
  }

  async onStepCommit(request: WizardCommitRequest): Promise<void> {
    if (request.step.key !== 'details') {
      request.complete();
      return;
    }
    this.submitted.set(true);
    const result = await this.saveDetails();
    if (result.success) {
      request.complete();
      return;
    }
    if (await this.recoverFromStaleVersion(result)) {
      request.fail(STALE_VERSION_MESSAGE);
      return;
    }
    request.fail(result.error ?? 'Save failed.');
  }

  onWizardFinished(): void {
    if (this.requirementId != null) {
      this.router.navigate(['/projects', this.projectName, 'requirements', this.requirementId]);
    } else {
      this.onBack();
    }
  }

  private async saveDetails(): Promise<CommandResult<unknown>> {
    this.saving.set(true);
    this.showError(null);
    try {
      const { requirementId, shallText } = this.detailsForm.getRawValue();
      const input: Record<string, unknown> = { projectName: this.projectName, shallText };
      if (requirementId) input['requirementId'] = requirementId;
      if (this.requirementId != null) input['requirementId'] = this.requirementId;
      if (this.version != null) input['version'] = this.version;

      const result = await this.commandService.execute('EditRequirement', input);
      if (!result.success) return result;

      const wasCreate = this.requirementId == null;
      if (wasCreate) this.projectService.notifyTreeChanged();

      const saved = result.entity as RequirementDto | null;
      if (saved) {
        this.requirementId = saved.id;
        this.version = saved.version;
        this.requirementName.set(saved.requirementId);
      }
      this.detailsForm.markAsPristine();
      this.messageService.add({ severity: 'success', summary: 'Saved', detail: 'Requirement saved.' });

      if (wasCreate && this.requirementId != null) {
        await this.loadRequirement(false);
      }
      return result;
    } catch {
      return { success: false, entityType: 'Requirement', entity: null, error: 'Unexpected error', violations: null };
    } finally {
      this.saving.set(false);
    }
  }

  private async recoverFromStaleVersion(result: CommandResult<unknown>): Promise<boolean> {
    if (result.status !== 409 || this.requirementId == null) return false;
    await this.loadRequirement(false);
    return true;
  }

  async onSave(): Promise<void> {
    this.submitted.set(true);
    if (this.detailsForm.invalid) { this.detailsForm.markAllAsTouched(); return; }
    const result = await this.saveDetails();
    if (result.success) return;
    if (await this.recoverFromStaleVersion(result)) { this.showError(STALE_VERSION_MESSAGE); return; }
    this.showError(result.error ?? 'Save failed.');
    this.retryable.set(isNetworkError(result));
  }

  onCopy(): void {
    this.confirmationService.confirm({
      message: 'Create a copy of this requirement?',
      accept: async () => {
        const result = await this.commandService.execute('CopyRequirement', { projectName: this.projectName, requirementId: this.requirementId });
        if (result.success && result.entity) {
          this.projectService.notifyTreeChanged();
          const copy = result.entity as RequirementDto;
          this.router.navigate(['/projects', this.projectName, 'requirements', copy.id]);
        } else { this.showError(result.error ?? 'Copy failed.'); }
      }
    });
  }

  onDelete(): void {
    this.confirmationService.confirm({
      message: 'Delete this requirement?',
      accept: async () => {
        const result = await this.commandService.execute('DeleteRequirement', { projectName: this.projectName, requirementId: this.requirementId, version: this.version });
        if (result.success) {
          this.projectService.notifyTreeChanged();
          this.detailsForm.markAsPristine();
          this.router.navigate(['/projects', this.projectName, 'requirements']);
        } else { this.showError(result.error ?? 'Delete failed.'); }
      }
    });
  }

  // --- Review Actions ---

  getReviewResult(dept: ReviewDepartment): ReviewResult {
    const review = this.requirement()?.reviews?.find(r => r.department === dept);
    return review?.result ?? 'NOT_REVIEWED';
  }

  getReviewComments(dept: ReviewDepartment): string | null {
    return this.requirement()?.reviews?.find(r => r.department === dept)?.comments ?? null;
  }

  getReviewer(dept: ReviewDepartment): string | null {
    return this.requirement()?.reviews?.find(r => r.department === dept)?.reviewer ?? null;
  }

  getReviewerRole(dept: ReviewDepartment): string | null {
    return this.requirement()?.reviews?.find(r => r.department === dept)?.reviewerRole ?? null;
  }

  getReviewedAt(dept: ReviewDepartment): string | null {
    return this.requirement()?.reviews?.find(r => r.department === dept)?.reviewedAt ?? null;
  }

  canReviewDept(dept: ReviewDepartment): boolean {
    const userRoles = this.permissionService.getRoles();
    return canReviewDepartment(userRoles, dept);
  }

  canApproveUser(): boolean {
    const userRoles = this.permissionService.getRoles();
    return canApprove(userRoles);
  }

  canPublishUser(): boolean {
    const userRoles = this.permissionService.getRoles();
    return canPublish(userRoles);
  }

  openReviewDialog(dept: ReviewDepartment): void {
    this.reviewDialogDept.set(dept);
    const existing = this.requirement()?.reviews?.find(r => r.department === dept);
    this.reviewDialogResult.set(existing?.result ?? 'PASSED');
    this.reviewDialogComments = existing?.comments ?? '';
    this.reviewDialogVisible.set(true);
  }

  onReviewDialogVisibleChange(visible: boolean): void {
    this.reviewDialogVisible.set(visible);
  }

  onCompareDialogVisibleChange(visible: boolean): void {
    this.compareDialogVisible.set(visible);
  }

  async submitReview(): Promise<void> {
    const dept = this.reviewDialogDept();
    if (!dept || this.requirementId == null) return;
    if (this.reviewDialogResult() === 'FAILED' && !this.reviewDialogComments.trim()) {
      alert('Comments are required for a FAILED review.');
      return;
    }
    // Service only accepts PASSED or FAILED, not NOT_REVIEWED
    const resultValue = this.reviewDialogResult();
    if (resultValue === 'NOT_REVIEWED') {
      alert('Please select Passed or Failed.');
      return;
    }
    this.submittingReview.set(true);
    try {
      const result = await this.requirementService.submitReview(
        this.projectName, this.requirementId, dept, resultValue, this.reviewDialogComments, this.version!
      );
      this.requirement.set(result);
      this.version = result.version;
      this.messageService.add({ severity: 'success', summary: 'Review Submitted', detail: `${getDepartmentLabel(dept)} review recorded.` });
      this.reviewDialogVisible.set(false);
    } catch (e: any) {
      this.showError(e.error?.message ?? 'Failed to submit review.');
    } finally {
      this.submittingReview.set(false);
    }
  }

  async onApprove(): Promise<void> {
    if (!confirm('Approve this requirement? All three reviews must have passed.')) return;
    try {
      const result = await this.requirementService.approveRequirement(this.projectName, this.requirementId!, this.version!);
      this.requirement.set(result);
      this.version = result.version;
      this.messageService.add({ severity: 'success', summary: 'Approved', detail: 'Requirement approved.' });
    } catch (e: any) {
      this.showError(e.error?.message ?? 'Approve failed.');
    }
  }

  async onReopen(): Promise<void> {
    if (!confirm('Reopen for review? This will reset the approval status.')) return;
    try {
      const result = await this.requirementService.reopenForReview(this.projectName, this.requirementId!, this.version!);
      this.requirement.set(result);
      this.version = result.version;
      this.messageService.add({ severity: 'info', summary: 'Reopened', detail: 'Requirement reopened for review.' });
    } catch (e: any) {
      this.showError(e.error?.message ?? 'Reopen failed.');
    }
  }

  async onPublish(): Promise<void> {
    if (!confirm('Publish to Jama Connect? This action cannot be undone.')) return;
    try {
      const result = await this.requirementService.publishRequirement(this.projectName, this.requirementId!, this.version!);
      this.requirement.set(result);
      this.version = result.version;
      this.messageService.add({ severity: 'success', summary: 'Published', detail: 'Requirement published to Jama Connect.' });
    } catch (e: any) {
      this.showError(e.error?.message ?? 'Publish failed.');
    }
  }

  // --- Overlap Comparison ---

  openCompareDialog(): void {
    this.compareDialogVisible.set(true);
    this.jamaSearchResults.set([]);
    this.selectedJamaIds = [];
    this.compareSearchQuery = '';
  }

  async searchJamaRequirements(): Promise<void> {
    if (!this.compareSearchQuery.trim()) return;
    this.compareSearching.set(true);
    try {
      const results = await this.requirementService.fetchJamaRequirements(this.projectName, this.compareSearchQuery);
      this.jamaSearchResults.set(results);
    } catch (e: any) {
      this.showError('Jama search failed: ' + (e.error?.message ?? e.message));
    } finally {
      this.compareSearching.set(false);
    }
  }

  async runComparison(): Promise<void> {
    if (!this.requirementId || !this.selectedJamaIds.length) return;
    this.compareRunning.set(true);
    try {
      const comparisons = await this.requirementService.compareRequirements(this.projectName, this.requirementId, this.selectedJamaIds.map(id => parseInt(id)));
      this.comparisons.set(comparisons);
      this.compareDialogVisible.set(false);
      this.messageService.add({ severity: 'success', summary: 'Comparison Complete', detail: `${comparisons.length} comparisons created.` });
    } catch (e: any) {
      this.showError('Comparison failed: ' + (e.error?.message ?? e.message));
    } finally {
      this.compareRunning.set(false);
    }
  }

  openResolveDialog(comparison: any): void {
    // TODO: Implement resolve dialog
  }

  viewComparison(comparison: any): void {
    // TODO: Implement view
  }

  navigateToIdea(): void {
    if (this.requirement()?.ideaId) {
      this.router.navigate(['/projects', this.projectName, 'ideas', this.requirement()!.ideaId]);
    }
  }

  onBack(): void {
    this.router.navigate(['/projects', this.projectName, 'requirements']);
  }

  getStatusLabel = getStatusLabel;
  getStatusSeverity = getStatusSeverity;
  getDepartmentLabel = getDepartmentLabel;
  getReviewResultLabel = getReviewResultLabel;
  getReviewResultSeverity = getReviewResultSeverity;

  getResolutionSeverity(resolution: string): 'success' | 'info' | 'warn' | 'danger' | 'secondary' {
    switch (resolution) {
      case 'ACCEPTED': return 'success';
      case 'REJECTED': return 'danger';
      case 'MODIFIED': return 'warn';
      default: return 'secondary';
    }
  }
}
