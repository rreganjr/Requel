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
import { InputText } from 'primeng/inputtext'.
import { TextareaModule } from 'primeng/textarea'.
import { FileUploadModule } from 'primeng/fileupload'.
import { SubmitErrorComponent } from '../../shared/app-submit-error'.
import { UpdateBannerComponent } from '../../shared/app-update-banner'.
import { AnnouncerService } from '../../core/announcer.service'.
import { ConfirmDialogModule } from 'primeng/confirmdialog'.
import { ConfirmationService, MessageService } from 'primeng/api'.
import { CommandResult } from '../../models/command'.
import { IdeaDto, ClarificationDto } from '../../models/requirement'.
import { EntityReferenceDto } from '../../models/entity-reference'.
import { RequirementService } from '../../core/requirement.service'.
import { CommandService, isNetworkError } from '../../core/command.service'.
import { ProjectService } from '../../core/project.service'.
import { PermissionService } from '../../core/permission.service'.
import { EventStreamService } from '../../core/event-stream.service'.
import { AnnotationsSectionComponent } from '../../shared/annotations-section'.
import { SourcesSectionComponent } from '../../shared/sources-section'.
import { AppFieldComponent, AppFieldControlDirective } from '../../shared/app-field'.
import { LoadingStateComponent } from '../../shared/loading-state'.
import { ErrorStateComponent } from '../../shared/error-state'.
import { AppFormWizardComponent, AppWizardStepComponent, WizardCommitRequest } from '../../shared/app-form-wizard'.
import { ARTIFACT_NAME_MAX_LENGTH } from '../../shared/validation-limits'.

const STALE_VERSION_MESSAGE =
  'This idea was changed elsewhere. Your copy has been refreshed — review the values and continue.';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-idea-editor',
  standalone: true,
  imports: [PageHeaderComponent, AppCardComponent, RouterLink, ReactiveFormsModule, NgTemplateOutlet,
            ButtonModule, InputText, TextareaModule, FileUploadModule,
            SubmitErrorComponent, UpdateBannerComponent, ConfirmDialogModule,
            AnnotationsSectionComponent, SourcesSectionComponent, AppFieldComponent, AppFieldControlDirective,
            AppFormWizardComponent, AppWizardStepComponent,
            LoadingStateComponent, ErrorStateComponent],
  providers: [ConfirmationService, DatePipe],
  template: `
    <div class="idea-editor" data-testid="idea-editor">
      <div class="page-header">
        <app-page-header [title]="isNew() ? 'New Idea' : ideaName()" />
        <div class="page-actions">
          <p-button label="Back" icon="pi pi-arrow-left" severity="secondary" data-testid="idea-back"
                    [outlined]="true" (onClick)="onBack()" />
          @if (!isNew()) {
            @if (canEdit()) {
              <p-button label="Copy" icon="pi pi-copy" severity="secondary" data-testid="idea-copy"
                        [outlined]="true" (onClick)="onCopy()" />
            }
            @if (canDelete()) {
              <p-button label="Delete" icon="pi pi-trash" severity="danger" data-testid="idea-delete"
                        [outlined]="true" (onClick)="onDelete()" />
            }
          }
        </div>
      </div>

      <app-submit-error [message]="errorMessage()" testid="idea-editor-error" [retryable]="retryable()" (retry)="onSave()" />
      @if (updateAvailable()) {
        <app-update-banner message="This idea was changed elsewhere. Your unsaved changes are preserved."
                           testid="idea-update-banner"
                           (reload)="reloadFromExternalChange()" (dismiss)="updateAvailable.set(false)" />
      }

      @if (loading()) {
        <app-card>
          <app-loading-state label="Loading idea…" [lines]="4" testid="idea-editor-loading" />
        </app-card>
      } @else if (loadError()) {
        <app-error-state [message]="loadError()!" testid="idea-editor-load-error" (retry)="retryLoad()" />
      } @else if (isNew()) {
        <app-form-wizard
          [(activeKey)]="wizardStep"
          navLabel="New idea steps"
          (stepCommit)="onStepCommit($event)"
          (cancelled)="onBack()"
          (finished)="onWizardFinished()"
          data-testid="idea-wizard"
        >
          <app-wizard-step key="details" label="Details" helper="Source text and optional file"
                           [form]="detailsForm">
            <ng-template>
              <ng-container [ngTemplateOutlet]="detailsFields" />
            </ng-template>
          </app-wizard-step>

          <app-wizard-step key="breakdown" label="AI Breakdown" helper="Review AI-generated requirements" [optional]="true">
            <ng-template>
              <ng-container [ngTemplateOutlet]="breakdownSection" />
            </ng-template>
          </app-wizard-step>
        </app-form-wizard>
      } @else {
        <app-card>
          <ng-container [ngTemplateOutlet]="detailsFields" />

          <div class="form-actions">
            @if (canEdit() && idea()?.status === 'SUBMITTED') {
              <p-button label="Save" icon="pi pi-check" data-testid="idea-save"
                        [disabled]="!canSave()" [loading]="saving()" (onClick)="onSave()" />
            }
          </div>
        </app-card>

        <!-- Clarifications (UC-02) -->
        <app-card *ngIf="idea()?.status === 'CLARIFYING'" class="clarifications-card">
          <h2 class="rq-section-title">Clarifications Needed</h2>
          @for (c of clarifications(); track c.id) {
            <div class="clarification-item">
              <div class="clarification-question">{{ c.question }}</div>
              <div class="clarification-answer" *ngIf="c.answer">
                <strong>Answer:</strong> {{ c.answer }}
                <span class="answered-by"> by {{ c.answeredBy }} · {{ c.answeredAt | date:'short' }}</span>
              </div>
              <div class="clarification-actions" *ngIf="!c.answer && canAnswerClarifications()">
                <textarea pTextarea [(ngModel)]="clarificationAnswers[c.id]" rows="2" placeholder="Your answer..." />
                <p-button label="Submit Answer" icon="pi pi-send" size="small"
                          (onClick)="submitClarificationAnswer(c.id)" />
                <p-button label="Answer: Unknown" severity="secondary" [outlined]="true" size="small"
                          (onClick)="submitClarificationAnswer(c.id, true)" />
              </div>
            </div>
          }
        </app-card>

        <!-- Breakdown Review (UC-03) -->
        <app-card *ngIf="idea()?.status === 'BREAKDOWN_REVIEW'" class="breakdown-card">
          <h2 class="rq-section-title">Review AI Breakdown</h2>
          <p class="text-muted">Review each proposed requirement. Choose Keep, Reject, or Revise.</p>
          @for (req of derivedRequirements(); track req.id) {
            <div class="breakdown-item">
              <div class="breakdown-header">
                <span class="breakdown-id">{{ req.requirementId }}</span>
                <p-select [(ngModel)]="breakdownDecisions[req.id]" [options]="breakdownActions"
                          [style]="{ width: '10rem' }" />
              </div>
              <div class="breakdown-text">{{ req.shallText }}</div>
              <div class="breakdown-revised" *ngIf="breakdownDecisions[req.id] === 'REVISE'">
                <textarea pTextarea [(ngModel)]="revisedTexts[req.id]" rows="2" placeholder="Revised shall statement..." />
              </div>
            </div>
          }
          <div class="form-actions">
            <p-button label="Submit Breakdown Review" icon="pi pi-check" severity="success"
                      (onClick)="submitBreakdownReview()" [loading]="submittingBreakdown()" />
          </div>
        </app-card>

        <!-- Derived Requirements List -->
        <app-card *ngIf="derivedRequirements().length" class="derived-card">
          <h2 class="rq-section-title">Derived Requirements</h2>
          <app-data-table
            [value]="derivedRequirements()"
            [columns]="derivedColumns"
            [paginator]="true"
            [rows]="10"
            [showToolbar]="false"
            [defaultActions]="false"
            [rowClickable]="true"
            [dataKey]="'id'"
            [testid]="'idea-derived'"
            (rowClick)="onDerivedClick($event)"
          >
            <ng-template #row let-r>
              <td>{{ r.requirementId }}</td>
              <td class="req-text-cell"><div class="req-shall-text">{{ r.shallText }}</div></td>
              <td><p-tag [value]="r.status" [severity]="getStatusSeverity(r.status)" /></td>
            </ng-template>
          </app-data-table>
        </app-card>

        <ng-container [ngTemplateOutlet]="breakdownSection" />

        @if (ideaId != null) {
          <app-annotations-section
            [projectName]="projectName"
            entityType="Idea"
            [entityId]="ideaId"
            />
            <app-sources-section
              [projectName]="projectName"
              entityType="Idea"
              [entityId]="ideaId"
              />
        }
      }

      <p-confirmDialog />

      <!-- Shared templates -->
      <ng-template #detailsFields>
        <app-field label="Source Text" [control]="detailsForm.controls.sourceText" [divider]="false"
                   [errorMessages]="sourceTextErrors" [submitted]="submitted()">
          <textarea appFieldControl pTextarea [formControl]="detailsForm.controls.sourceText" rows="8" [autoResize]="true"
                    placeholder="Describe the requirement idea..." data-testid="idea-source-text"></textarea>
        </app-field>

        <app-field label="Attach File (optional)" helper="Max 10 MB: .txt, .docx, .pdf" [divider]="false"
                   [submitted]="submitted()">
          <p-fileUpload #fileUpload chooseLabel="Choose" [maxFileSize]="10000000"
                        accept=".txt,.docx,.pdf" [auto]="false" [showUploadButton]="false"
                        (onSelect)="onFileSelect($event)" data-testid="idea-file-upload" />
          @if (selectedFile()) {
            <div class="file-info">{{ selectedFile()!.name }} ({{ formatFileSize(selectedFile()!.size) }})</div>
          }
        </app-field>
      </ng-template>

      <ng-template #breakdownSection>
        <app-card>
          <h3 class="rq-section-title">AI Breakdown</h3>
          @if (idea()?.status === 'SUBMITTED' || idea()?.status === 'CLARIFYING') {
            <p class="text-muted">Run AI decomposition to generate draft requirements.</p>
            <p-button label="Run Decomposition" icon="pi pi-magic" severity="info"
                      (onClick)="runDecomposition()" [loading]="decomposing()" />
          } @else if (idea()?.status === 'BREAKDOWN_REVIEW') {
            <p class="text-muted">Breakdown review in progress. Complete the review above.</p>
          } @else {
            <p class="text-muted">Breakdown complete. See derived requirements below.</p>
          }
        </app-card>
      </ng-template>
    </div>
  `,
  styles: [`
    .page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem; }
    .page-actions { display: flex; gap: 0.5rem; }
    .form-actions { margin-top: 1rem; }
    .clarifications-card { margin-top: 1.5rem; }
    .clarification-item { border: 1px solid var(--p-content-border-color); border-radius: var(--rq-radius-md); padding: 1rem; margin-bottom: 0.75rem; }
    .clarification-question { font-weight: 600; margin-bottom: 0.5rem; }
    .clarification-answer { color: var(--p-text-secondary-color); }
    .answered-by { font-size: 0.8rem; color: var(--p-text-muted-color); margin-left: 0.5rem; }
    .clarification-actions { display: flex; gap: 0.5rem; margin-top: 0.5rem; flex-wrap: wrap; }
    .breakdown-card { margin-top: 1.5rem; }
    .breakdown-item { border: 1px solid var(--p-content-border-color); border-radius: var(--rq-radius-md); padding: 1rem; margin-bottom: 0.75rem; }
    .breakdown-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.5rem; }
    .breakdown-id { font-family: monospace; font-weight: 600; color: var(--p-primary-color); }
    .breakdown-text { font-style: italic; color: var(--p-text-secondary-color); }
    .breakdown-revised { margin-top: 0.5rem; }
    .derived-card { margin-top: 1.5rem; }
    .req-text-cell { max-width: 40rem; }
    .req-shall-text { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; font-size: 0.875rem; }
    .file-info { margin-top: 0.5rem; font-size: 0.85rem; color: var(--p-text-secondary-color); }
    .text-muted { color: var(--p-text-muted-color); }
  `]
})
export class IdeaEditorComponent implements OnInit, OnDestroy, DirtyCheckable {
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
  ideaName = signal('');
  idea = signal<IdeaDto | null>(null);
  errorMessage = signal<string | null>(null);
  retryable = signal(false);
  loading = signal(true);
  loadError = signal<string | null>(null);
  saving = signal(false);
  submitted = signal(false);
  updateAvailable = signal(false);
  clarifications = signal<ClarificationDto[]>([]);
  derivedRequirements = signal<any[]>([]);
  clarificationAnswers: Record<number, string> = {};
  breakdownDecisions: Record<number, 'KEEP' | 'REJECT' | 'REVISE'> = {};
  revisedTexts: Record<number, string> = {};
  decomposing = signal(false);
  submittingBreakdown = signal(false);
  selectedFile = signal<File | null>(null);

  readonly breakdownActions = [
    { label: 'Keep', value: 'KEEP' },
    { label: 'Reject', value: 'REJECT' },
    { label: 'Revise', value: 'REVISE' },
  ];

  projectName = '';
  ideaId: number | null = null;
  private version: number | null = null;
  private sseBound = false;

  readonly detailsForm = new FormGroup({
    sourceText: new FormControl('', { validators: [Validators.required], nonNullable: true }),
  });

  readonly sourceTextErrors = { required: 'Source text is required.' };

  wizardStep = 'details';

  readonly derivedColumns: any[] = [
    { field: 'requirementId', header: 'ID', sortable: true },
    { field: 'shallText', header: 'Shall Statement', sortable: false },
    { field: 'status', header: 'Status', sortable: true },
  ];

  readonly canEdit = computed(() => this.permissionService.canEdit('Idea'));
  readonly canDelete = computed(() => this.permissionService.canDelete('Idea'));
  readonly canAnswerClarifications = computed(() => this.permissionService.canEdit('Idea'));

  ngOnInit(): void {
    this.route.paramMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(async params => {
      this.projectName = params.get('name') ?? '';
      const idParam = params.get('ideaId') ?? '';
      const newIsNew = idParam === 'new';

      if (newIsNew) {
        this.isNew.set(true);
        this.idea.set(null);
        this.ideaId = null;
        this.version = null;
        this.wizardStep = 'details';
        this.submitted.set(false);
        this.detailsForm.reset({ sourceText: '' });
        this.loading.set(false);
        this.loadError.set(null);
      }

      await this.permissionService.loadForProject(this.projectName);

      if (!newIsNew) {
        this.isNew.set(false);
        this.ideaId = +idParam;
        this.loadIdea();
      }
    });
  }

  hasUnsavedChanges(): boolean {
    return this.detailsForm.dirty;
  }

  ngOnDestroy(): void {
    if (this.ideaId) {
      void this.eventStreamService.removeSubscription('Idea', this.ideaId);
    }
  }

  private showError(message: string | null): void {
    this.errorMessage.set(message);
    this.retryable.set(false);
  }

  async reloadFromExternalChange(): Promise<void> {
    this.updateAvailable.set(false);
    this.detailsForm.markAsPristine();
    await this.loadIdea(false);
    this.announcer.announce('Idea reloaded.');
  }

  retryLoad(): void {
    void this.loadIdea();
  }

  private async loadIdea(skeleton = true): Promise<void> {
    if (skeleton) { this.loading.set(true); this.loadError.set(null); }
    try {
      const idea = await this.requirementService.getIdea(this.projectName, this.ideaId!);
      this.idea.set(idea);
      this.version = idea.version;
      this.clarifications.set(idea.clarifications ?? []);
      this.derivedRequirements.set(idea.derivedRequirements ?? []);
      if (!this.hasUnsavedChanges()) {
        this.ideaName.set(idea.externalId ?? 'Idea #' + idea.id);
        this.detailsForm.reset({ sourceText: idea.sourceText });
      }
    } catch {
      if (skeleton) this.loadError.set('Failed to load idea.');
      else this.showError('Failed to load idea.');
    } finally {
      if (skeleton) this.loading.set(false);
    }
    if (this.ideaId && !this.sseBound) {
      void this.eventStreamService.addSubscription('Idea', this.ideaId);
      this.sseBound = true;
      this.eventStreamService.events$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(envelope => {
        if (envelope.targetType !== 'Idea' || envelope.targetId !== this.ideaId) return;
        if (envelope.eventType === 'TargetDeleted') {
          this.announcer.announce('This idea was deleted in another session.');
          return;
        }
        const dirty = this.hasUnsavedChanges();
        void this.loadIdea(false);
        if (dirty) {
          this.updateAvailable.set(true);
          this.announcer.announce('This idea was changed elsewhere. Your unsaved changes are preserved.');
        } else {
          this.announcer.announceThrottled('Idea:' + this.ideaId, 'This idea was updated.');
        }
      });
    }
  }

  canSave(): boolean {
    return this.detailsForm.valid && this.detailsForm.dirty && !this.saving();
  }

  async onStepCommit(request: WizardCommitRequest): Promise<void> {
    if (request.step.key !== 'details') { request.complete(); return; }
    this.submitted.set(true);
    const result = await this.saveDetails();
    if (result.success) { request.complete(); return; }
    if (await this.recoverFromStaleVersion(result)) { request.fail(STALE_VERSION_MESSAGE); return; }
    request.fail(result.error ?? 'Save failed.');
  }

  onWizardFinished(): void {
    if (this.ideaId != null) {
      this.router.navigate(['/projects', this.projectName, 'ideas', this.ideaId]);
    } else { this.onBack(); }
  }

  private async saveDetails(): Promise<CommandResult<unknown>> {
    this.saving.set(true);
    this.showError(null);
    try {
      const { sourceText } = this.detailsForm.getRawValue();
      const file = this.selectedFile();
      let result: CommandResult<unknown>;
      if (file) {
        result = await this.requirementService.submitIdea(this.projectName, sourceText, file);
      } else {
        result = await this.requirementService.submitIdea(this.projectName, sourceText);
      }
      if (!result.success) return result;

      const wasCreate = this.ideaId == null;
      if (wasCreate) this.projectService.notifyTreeChanged();

      const saved = result.entity as IdeaDto | null;
      if (saved) {
        this.ideaId = saved.id;
        this.version = saved.version;
        this.ideaName.set(saved.externalId ?? 'Idea #' + saved.id);
      }
      this.detailsForm.markAsPristine();
      this.selectedFile.set(null);
      this.messageService.add({ severity: 'success', summary: 'Saved', detail: 'Idea submitted.' });

      if (wasCreate && this.ideaId != null) {
        await this.loadIdea(false);
      }
      return result;
    } catch {
      return { success: false, entityType: 'Idea', entity: null, error: 'Unexpected error', violations: null };
    } finally {
      this.saving.set(false);
    }
  }

  private async recoverFromStaleVersion(result: CommandResult<unknown>): Promise<boolean> {
    if (result.status !== 409 || this.ideaId == null) return false;
    await this.loadIdea(false);
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

  onFileSelect(event: any): void {
    if (event.files?.length) this.selectedFile.set(event.files[0]);
  }

  formatFileSize(bytes: number): string {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
  }

  onCopy(): void {
    this.confirmationService.confirm({
      message: 'Create a copy of this idea?',
      accept: async () => {
        const result = await this.commandService.execute('CopyIdea', { projectName: this.projectName, ideaId: this.ideaId });
        if (result.success && result.entity) {
          this.projectService.notifyTreeChanged();
          const copy = result.entity as IdeaDto;
          this.router.navigate(['/projects', this.projectName, 'ideas', copy.id]);
        } else { this.showError(result.error ?? 'Copy failed.'); }
      }
    });
  }

  onDelete(): void {
    this.confirmationService.confirm({
      message: 'Delete this idea?',
      accept: async () => {
        const result = await this.commandService.execute('DeleteIdea', { projectName: this.projectName, ideaId: this.ideaId, version: this.version });
        if (result.success) {
          this.projectService.notifyTreeChanged();
          this.detailsForm.markAsPristine();
          this.router.navigate(['/projects', this.projectName, 'ideas']);
        } else { this.showError(result.error ?? 'Delete failed.'); }
      }
    });
  }

  async runDecomposition(): Promise<void> {
    if (!this.ideaId) return;
    this.decomposing.set(true);
    try {
      const result = await this.commandService.execute('DecomposeIdea', { projectName: this.projectName, ideaId: this.ideaId });
      if (result.success) {
        this.messageService.add({ severity: 'success', summary: 'Decomposition Complete', detail: 'AI breakdown generated. Review in the Breakdown step.' });
        await this.loadIdea(false);
      } else {
        this.showError(result.error ?? 'Decomposition failed.');
      }
    } catch (e: any) {
      this.showError('Decomposition failed: ' + (e.error?.message ?? e.message));
    } finally {
      this.decomposing.set(false);
    }
  }

  async submitClarificationAnswer(clarificationId: number, unknown = false): Promise<void> {
    const answer = unknown ? 'Unknown' : this.clarificationAnswers[clarificationId];
    if (!answer?.trim() && !unknown) { alert('Please provide an answer.'); return; }
    try {
      await this.requirementService.answerClarification(this.projectName, clarificationId, answer);
      this.messageService.add({ severity: 'success', summary: 'Answer Submitted', detail: 'Clarification answered.' });
      await this.loadIdea(false);
    } catch (e: any) {
      this.showError('Failed to submit answer: ' + (e.error?.message ?? e.message));
    }
  }

  async submitBreakdownReview(): Promise<void> {
    if (!this.ideaId) return;
    this.submittingBreakdown.set(true);
    try {
      const decisions = Object.entries(this.breakdownDecisions).map(([reqId, action]) => ({
        requirementId: +reqId,
        action,
        revisedText: action === 'REVISE' ? this.revisedTexts[+reqId] : undefined
      }));
      const result = await this.requirementService.reviewBreakdown(this.projectName, this.ideaId, decisions);
      this.idea.set(result);
      this.messageService.add({ severity: 'success', summary: 'Breakdown Review Submitted', detail: 'Derived requirements created.' });
      await this.loadIdea(false);
    } catch (e: any) {
      this.showError('Breakdown review failed: ' + (e.error?.message ?? e.message));
    } finally {
      this.submittingBreakdown.set(false);
    }
  }

  onDerivedClick(req: any): void {
    this.router.navigate(['/projects', this.projectName, 'requirements', req.id]);
  }

  onBack(): void {
    this.router.navigate(['/projects', this.projectName, 'ideas']);
  }

  getStatusSeverity(status: string): 'success' | 'info' | 'warn' | 'danger' | 'secondary' {
    switch (status) {
      case 'APPROVED': case 'PUBLISHED': return 'success';
      case 'IN_REVIEW': return 'info';
      case 'REJECTED': return 'danger';
      default: return 'secondary';
    }
  }
}
