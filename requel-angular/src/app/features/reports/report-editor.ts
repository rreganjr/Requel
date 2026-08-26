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
import { EditorActionsComponent } from '../../shared/editor-actions';
import { PageHeaderComponent } from '../../shared/page-header';
import { AppCardComponent } from '../../shared/app-card';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { DirtyCheckable } from '../../core/dirty-check.guard';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { InputText } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';
import { SubmitErrorComponent } from '../../shared/app-submit-error';
import { isNetworkError } from '../../core/command.service';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ConfirmationService, MessageService } from 'primeng/api';
import { ReportGeneratorDto } from '../../models/report';
import { ReportService } from '../../core/report.service';
import { PermissionService } from '../../core/permission.service';
import { AnnotationsSectionComponent } from '../../shared/annotations-section';
import { FileUploadButtonComponent } from '../../shared/file-upload-button';
import { AppFieldComponent, AppFieldControlDirective } from '../../shared/app-field';
import { LoadingStateComponent } from '../../shared/loading-state';
import { ErrorStateComponent } from '../../shared/error-state';
import { applyCommandErrors, clearServerErrors } from '../../shared/form-errors';
import { ARTIFACT_NAME_MAX_LENGTH } from '../../shared/validation-limits';


@Component({
  selector: 'app-report-editor',
  standalone: true,
  imports: [EditorActionsComponent, 
    PageHeaderComponent,
    AppCardComponent,
    ReactiveFormsModule,
    ButtonModule,
    InputText,
    TextareaModule,
    SubmitErrorComponent,
    ConfirmDialogModule,
    AnnotationsSectionComponent,
    FileUploadButtonComponent,
    AppFieldComponent,
    AppFieldControlDirective,
    LoadingStateComponent,
    ErrorStateComponent,
  ],
  providers: [ConfirmationService],
  template: `
    <div class="report-editor" data-testid="report-editor">
      <div class="page-header">
        <app-page-header [title]="isNew() ? 'New Document' : reportName()" />
        <div class="page-actions">
          <app-editor-actions [projectName]="projectName" />
          <p-button label="Back" icon="pi pi-arrow-left" severity="secondary" data-testid="report-back"
                    [outlined]="true" (onClick)="onBack()" />
          @if (!isNew() && canDelete()) {
            <p-button label="Delete" icon="pi pi-trash" severity="danger" data-testid="report-delete"
                      [outlined]="true" (onClick)="onDelete()" />
          }
          @if (!isNew()) {
            <p-button label="Run" icon="pi pi-play" severity="success" data-testid="report-run"
                      [outlined]="true" (onClick)="onRun()" [loading]="running()" />
          }
        </div>
      </div>

      <app-submit-error
        [message]="errorMessage()"
        [retryable]="retryable()"
        (retry)="onSave()"
        testid="report-error" />

      @if (loading()) {
        <app-card>
          <app-loading-state label="Loading document…" [lines]="4" testid="report-editor-loading" />
        </app-card>
      } @else if (loadError()) {
        <app-error-state [message]="loadError()!" testid="report-editor-load-error"
                         (retry)="retryLoad()" />
      } @else {
        <app-card>
          <form [formGroup]="form" (ngSubmit)="onSave()">
            <!-- controlId "name" / "text": ReportEditorPage and BaseListPage's default
                 readySelector locate #name and #text, so those ids stay stable. -->
            <app-field
              label="Name"
              controlId="name"
              [control]="form.controls.name"
              [submitted]="submitted()"
            >
              <input
                id="name"
                pInputText
                appFieldControl
                formControlName="name"
                [attr.maxlength]="nameMaxLength"
                placeholder="Template name"
              />
            </app-field>

            <app-field
              label="XSLT Template"
              controlId="text"
              [control]="form.controls.text"
              [submitted]="submitted()"
              [divider]="false"
            >
              <div class="xslt-field">
                <textarea
                  id="text"
                  pTextarea
                  appFieldControl
                  formControlName="text"
                  rows="20"
                  placeholder="Paste XSLT stylesheet here..."
                  class="xslt-textarea"
                ></textarea>
                <div class="upload-row">
                  <app-file-upload-button label="Upload XSLT" [outlined]="true" size="small"
                                          accept=".xsl,.xslt,.xml" (fileSelected)="onFileUpload($event)" />
                  <span class="upload-hint">Upload a .xsl/.xslt file to replace the template text.</span>
                </div>
              </div>
            </app-field>

            <div class="form-actions">
              <p-button
                type="submit"
                label="Save"
                icon="pi pi-check"
                data-testid="report-save"
                [loading]="saving()"
                [disabled]="form.invalid || form.pristine || saving()"
              />
            </div>
          </form>
        </app-card>

        @if (!isNew()) {
          <app-annotations-section
            [projectName]="projectName"
            entityType="ReportGenerator"
            [entityId]="reportId()"
            [canEdit]="canEdit()" />
        }
      }

    </div>

    <p-confirmDialog />
  `,
  styles: [`
    .page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: var(--rq-space-4); }
    .page-actions { display: flex; gap: var(--rq-space-2); }
    /* The local .form-grid (140px label column) is gone; app-field owns the rows now. */
    .xslt-field { display: flex; flex-direction: column; gap: var(--rq-space-2); }
    .xslt-textarea { width: 100%; font-family: monospace; font-size: var(--rq-font-size-xs); }
    app-field input { width: 100%; }
    .upload-row { display: flex; align-items: center; gap: var(--rq-space-3); }
    .upload-hint { font-size: var(--rq-font-size-xs); color: var(--p-text-secondary-color); }
    .form-actions { margin-block: var(--rq-space-4) var(--rq-space-6); }
  `]
})
export class ReportEditorComponent implements OnInit, OnDestroy, DirtyCheckable {
  report = signal<ReportGeneratorDto | null>(null);
  reportName = signal('');
  reportId = signal<number | null>(null);
  saving = signal(false);
  submitted = signal(false);
  running = signal(false);
  errorMessage = signal<string | null>(null);
  retryable = signal(false);
  /**
   * #185. The edit form renders only once the detail GET resolves, so there is no window in which
   * a user can type into a form the load is about to reset. Starts true: an edit route is loading
   * from the first frame, and the create path clears it synchronously in ngOnInit.
   */
  loading = signal(true);
  loadError = signal<string | null>(null);
  private lastReportId: number | null = null;

  /**
   * Mirrors the backend `@Size(max = ValidationLimits.ARTIFACT_NAME_MAX)` (#171). Bound with
   * `[attr.maxlength]` rather than `maxlength` on purpose: Angular's MaxLengthValidator directive
   * matches `[maxlength][formControl]`, so the plain binding would register a SECOND maxlength
   * validator on top of the one in the form definition. `attr.` sets the HTML attribute only, which
   * is all that is wanted here — the browser stops the typing, the form owns the validation.
   */
  readonly nameMaxLength = ARTIFACT_NAME_MAX_LENGTH;

  /** `text` has no maxLength: it is `@Lob` server-side, so there is no bound to mirror. */
  readonly form = new FormGroup({
    name: new FormControl('', {
      validators: [Validators.required, Validators.maxLength(ARTIFACT_NAME_MAX_LENGTH)],
      nonNullable: true,
    }),
    text: new FormControl('', { nonNullable: true }),
  });

  projectName = '';
  canEdit = signal(false);
  canDelete = signal(false);

  private paramSub?: Subscription;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private reportService: ReportService,
    private permissionService: PermissionService,
    private messageService: MessageService,
    private confirmationService: ConfirmationService
  ) {}

  isNew(): boolean {
    return this.reportId() === null;
  }

  ngOnInit(): void {
    this.paramSub = this.route.paramMap.subscribe(async params => {
      this.projectName = params.get('name') ?? '';
      const idParam = params.get('reportId');
      const newIsNew = !idParam || idParam === 'new';

      // Reset the form synchronously for the new-report path BEFORE the
      // loadForProject await. Otherwise typing during the yield would later
      // be clobbered by the reset. See term-editor.ts for the same pattern.
      if (newIsNew) {
        this.reportId.set(null);
        this.submitted.set(false);
        this.form.reset({ name: '', text: '' });
        // Nothing to load, so resolve the gate synchronously (#185) - otherwise the create form
        // would sit behind the skeleton forever. Same reason the reset above is synchronous.
        this.loading.set(false);
        this.loadError.set(null);
      }

      await this.permissionService.loadForProject(this.projectName);
      this.canEdit.set(this.permissionService.canEdit('ReportGenerator'));
      this.canDelete.set(this.permissionService.canDelete('ReportGenerator'));

      if (!newIsNew) {
        await this.loadReport(Number(idParam));
      }
    });
  }

  /** Derived from the form, so there is no change-tracker to keep in step (#132). */
  hasUnsavedChanges(): boolean {
    return this.form.dirty;
  }

  ngOnDestroy(): void {
    this.paramSub?.unsubscribe();
  }

  /**
   * Reads the document and applies it in two parts: server state always, form state only when the
   * user has nothing unsaved.
   *
   * The patch/`markAsPristine()` pair used to run unconditionally. `page.goto()` on the edit route
   * returns long before this fetch does, so anything typed in that gap was silently discarded and
   * the form went back to pristine - Save then stayed disabled with no explanation (#185).
   * `ngOnInit`'s create path already resets synchronously to dodge exactly this; the edit path had
   * no equivalent. The check sits after the await on purpose, so it catches edits made while the
   * request was still in flight.
   *
   * `reportName` is the *persisted* name - it titles the page, names the download, and is quoted
   * in the delete confirmation - so it moves only with the form, matching `goal-editor`.
   * `submitted` likewise: clearing it would hide validation errors the user is looking at.
   */
  /** Re-run the last attempted load; wired to the error state's (retry) output. */
  retryLoad(): void {
    if (this.lastReportId != null) {
      void this.loadReport(this.lastReportId);
    }
  }

  /**
   * @param skeleton show the loading skeleton and the retryable error state. Suppressed for the
   *                 post-save refetch, where blanking the form the user is looking at would be
   *                 worse than a stale moment, and where a failure belongs in the inline message
   *                 rather than in place of the form. Mirrors `scenario-editor`.
   */
  private async loadReport(id: number, skeleton = true): Promise<void> {
    this.lastReportId = id;
    if (skeleton) {
      this.loading.set(true);
      this.loadError.set(null);
    }
    try {
      const r = await this.reportService.getReport(this.projectName, id);
      this.report.set(r);
      this.reportId.set(r.id);
      if (!this.hasUnsavedChanges()) {
        this.reportName.set(r.name);
        this.form.patchValue({ name: r.name, text: r.text ?? '' });
        this.form.markAsPristine();
        this.submitted.set(false);
      }
    } catch {
      if (skeleton) {
        this.loadError.set('Failed to load document.');
      } else {
        this.errorMessage.set('Failed to load document.');
      }
    } finally {
      if (skeleton) {
        this.loading.set(false);
      }
    }
  }

  async onSave(): Promise<void> {
    this.submitted.set(true);
    // Before the validity check, not after: a `server` error from the previous attempt
    // makes its control invalid, so leaving the clear until later meant a second save
    // bailed out here and never ran — the form was stuck at that value with Save
    // disabled and no way to retry. Clearing first re-validates against the client
    // rules only, and lets the server have another say.
    clearServerErrors(this.form);

    if (this.form.invalid) {
      // Replaces the imperative `if (!this.name.trim())` check and its page-level
      // "Document name is required." message — `required` renders under the field now.
      this.form.markAllAsTouched();
      return;
    }

    this.saving.set(true);
    this.errorMessage.set(null);
    this.retryable.set(false);

    const { name, text } = this.form.getRawValue();
    const trimmedName = name.trim();
    const result = await this.reportService.saveReport(
      this.projectName, this.reportId(), trimmedName, text || null
    );
    this.saving.set(false);

    if (result.success) {
      this.messageService.add({ severity: 'success', summary: 'Document saved', life: 3000 });
      const saved = result.entity as ReportGeneratorDto | null;
      if (this.isNew() && saved?.id) {
        this.form.patchValue({ name: trimmedName });
        this.form.markAsPristine();
        this.router.navigate(['/projects', this.projectName, 'reports', saved.id], { replaceUrl: true });
      } else {
        // Mark pristine BEFORE the refetch. The load method only adopts server state into the
        // form when the form has nothing unsaved (#185), and what was "unsaved" a moment ago is
        // exactly what we just persisted - leaving it dirty would make the reload skip its own
        // result, so Save stayed enabled on a freshly saved form. Same ordering scenario-editor
        // uses around its post-save refetch.
        this.form.markAsPristine();
        await this.loadReport(this.reportId()!, false);
      }
      return;
    }

    const unresolved = applyCommandErrors(this.form, result.violations);
    this.retryable.set(isNetworkError(result));
    if (unresolved.length) {
      this.errorMessage.set(unresolved.join(SEPARATOR));
    } else if (!result.violations?.length) {
      this.errorMessage.set(result.error ?? 'Save failed.');
    }
  }

  onDelete(): void {
    this.confirmationService.confirm({
      message: `Delete document "${this.reportName()}"? This cannot be undone.`,
      header: 'Confirm Delete',
      icon: 'pi pi-exclamation-triangle',
      accept: async () => {
        const result = await this.reportService.deleteReport(this.projectName, this.reportId()!);
        if (result.success) {
          this.messageService.add({ severity: 'success', summary: 'Document deleted', life: 3000 });
          this.router.navigate(['/projects', this.projectName, 'reports']);
        } else {
          this.errorMessage.set(result.error ?? 'Delete failed.');
        }
      }
    });
  }

  async onRun(): Promise<void> {
    this.running.set(true);
    this.errorMessage.set(null);
    try {
      await this.reportService.downloadReport(this.projectName, this.reportId()!, this.reportName());
    } catch {
      this.errorMessage.set('Failed to generate report.');
    } finally {
      this.running.set(false);
    }
  }

  /**
   * Replacing the template from a file is a user edit, so the control is marked dirty
   * explicitly — `patchValue` does not do it, and without this Save would stay disabled
   * after an upload.
   */
  onFileUpload(file: File): void {
    const reader = new FileReader();
    reader.onload = () => {
      this.form.controls.text.setValue(reader.result as string);
      this.form.controls.text.markAsDirty();
      if (!this.form.controls.name.value) {
        this.form.controls.name.setValue(file.name.replace(/\.[^.]+$/, ''));
        this.form.controls.name.markAsDirty();
      }
    };
    reader.readAsText(file);
  }

  onBack(): void {
    this.router.navigate(['/projects', this.projectName, 'reports']);
  }
}

/**
 * Separator for several command-level messages sharing the one page-level banner.
 * Semicolons, not spaces: two sentence fragments run together ("Email is invalid Phone
 * is required") read as one broken sentence. This is the separator the pre-#132 code
 * used and e2e/account.e2e.ts asserts.
 */
const SEPARATOR = '; ';
