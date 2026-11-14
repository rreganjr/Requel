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
import { ChangeDetectionStrategy, Component, computed, DestroyRef, inject, OnDestroy, OnInit, signal, effect } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { PageHeaderComponent } from '../../shared/page-header';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { DirtyCheckable } from '../../core/dirty-check.guard';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { InputText } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';
import { SelectModule } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { SubmitErrorComponent } from '../../shared/app-submit-error';
import { UpdateBannerComponent } from '../../shared/app-update-banner';
import { AnnouncerService } from '../../core/announcer.service';
import { isNetworkError, refusalMessage } from '../../core/command.service';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ConfirmationService, MessageService } from 'primeng/api';
import { GlossaryTermDto, ReplaceTermTextResultDto, SynonymUseDto } from '../../models/term';
import { EntityReferenceDto } from '../../models/entity-reference';
import { entityRoute } from '../../shared/entity-routes';
import { TermService } from '../../core/term.service';
import { PermissionService } from '../../core/permission.service';
import { EventStreamService } from '../../core/event-stream.service';
import { AnnotationsSectionComponent } from '../../shared/annotations-section';
import { AiReviewButtonComponent } from '../../shared/ai-review-button';
import { SourcesSectionComponent } from '../../shared/sources-section';
import { AppCardComponent } from '../../shared/app-card';
import { AppFieldComponent, AppFieldControlDirective } from '../../shared/app-field';
import { LoadingStateComponent } from '../../shared/loading-state';
import { ErrorStateComponent } from '../../shared/error-state';
import { applyCommandErrors, clearServerErrors } from '../../shared/form-errors';
import { ARTIFACT_NAME_MAX_LENGTH } from '../../shared/validation-limits';
import { ArtifactNameService } from '../../core/artifact-name.service';


/**
 * Separator for several command-level messages sharing the one page-level banner.
 * Semicolons, not spaces: two sentence fragments run together ("Email is invalid Phone
 * is required") read as one broken sentence. This is the separator the pre-#132 code
 * used and e2e/account.e2e.ts asserts.
 */
const SEPARATOR = '; ';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-term-editor',
  standalone: true,
  imports: [AiReviewButtonComponent, 
    PageHeaderComponent,
    AppCardComponent,
    ReactiveFormsModule,
    ButtonModule,
    InputText,
    TextareaModule,
    SelectModule,
    TableModule,
    SubmitErrorComponent, UpdateBannerComponent,
    ConfirmDialogModule,
    AnnotationsSectionComponent, SourcesSectionComponent,
    AppFieldComponent,
    AppFieldControlDirective,
    LoadingStateComponent,
    ErrorStateComponent,
    RouterLink,
  ],
  providers: [ConfirmationService],
  template: `
    <div class="term-editor" data-testid="term-editor">
      <div class="page-header">
        <app-page-header [title]="isNew() ? 'New Glossary Term' : termName()" />
        <div class="page-actions">
          <p-button label="Back" icon="pi pi-arrow-left" severity="secondary" data-testid="term-back"
                    [outlined]="true" (onClick)="onBack()" />
          @if (!isNew() && canDelete()) {
            <p-button label="Delete" icon="pi pi-trash" severity="danger" data-testid="term-delete"
                      [outlined]="true" (onClick)="onDelete()" />
          }
        </div>
      </div>

      <app-submit-error
        [message]="errorMessage()"
        [retryable]="retryable()"
        (retry)="onSave()"
        testid="term-error" />

      @if (updateAvailable()) {
        <app-update-banner message="This term was changed elsewhere. Your unsaved changes are preserved."
                           testid="term-update-banner"
                           (reload)="reloadFromExternalChange()" (dismiss)="updateAvailable.set(false)" />
      }

      @if (loading()) {
        <app-card>
          <app-loading-state label="Loading term…" [lines]="4" testid="term-editor-loading" />
        </app-card>
      } @else if (loadError()) {
        <app-error-state [message]="loadError()!" testid="term-editor-load-error"
                         (retry)="retryLoad()" />
      } @else {
        <app-card>
          <form [formGroup]="form" (ngSubmit)="onSave()">
            <!--
              controlId is "name" / "text" on purpose: those ids are an e2e contract, not
              an implementation detail. TermEditorPage and BaseListPage's default
              readySelector both locate #name, so letting app-field generate rq-field-{n}
              here would break navigation in tests that have nothing to do with this form.
            -->
            <app-field
              label="Term"
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
                placeholder="Term name"
              />
            </app-field>

            <app-field
              label="Definition"
              controlId="text"
              [control]="form.controls.text"
              [submitted]="submitted()"
            >
              <textarea
                id="text"
                pTextarea
                appFieldControl
                formControlName="text"
                rows="4" [autoResize]="true"
                placeholder="Definition of this term"
              ></textarea>
            </app-field>

            <app-field
              label="Canonical Term"
              controlId="canonical-term-input"
              [control]="form.controls.canonicalTermId"
              [submitted]="submitted()"
              [divider]="false"
            >
              <p-select
                appFieldControl
                inputId="canonical-term-input"
                data-testid="term-canonical-select"
                formControlName="canonicalTermId"
                [options]="canonicalOptions()"
                optionLabel="label"
                optionValue="value"
                placeholder="None (this is a canonical term)"
                [showClear]="true"
              />
            </app-field>

            <div class="form-actions">
              <p-button
                type="submit"
                label="Save"
                icon="pi pi-check"
                data-testid="term-save"
                [loading]="saving()"
                [disabled]="form.invalid || form.pristine || saving()"
              />
              <!-- #387: beside Save; disabled while there are unsaved changes. -->
              <app-ai-review-button entityType="GlossaryTerm" [entityId]="termId()"
                                    [dirty]="hasUnsavedChanges()" />
            </div>
          </form>
        </app-card>

        <!-- Alternate Terms (terms that point to this as their canonical) -->
        @if (!isNew() && term()?.alternateTerms?.length) {
          <div class="section" data-testid="term-alternate-terms-section">
            <h2 class="rq-section-title">Alternate Terms</h2>
            <p-table [value]="term()!.alternateTerms!" [rows]="10">
              <ng-template #header>
                <tr>
                  <th>Term</th>
                </tr>
              </ng-template>
              <ng-template #body let-a>
                <tr class="clickable-row" data-testid="term-alternate-row"
                    (click)="navigateToTerm(a.id)">
                  <td>{{ a.name }}</td>
                </tr>
              </ng-template>
            </p-table>
          </div>
        }

        <!--
          #348: referers that still say one of the term's synonyms (usually an old name kept by a
          rename). Replace rewrites it to the term's name through each referer's own edit.
        -->
        @if (!isNew() && synonymUses().length) {
          <div class="section" data-testid="term-synonym-uses-section">
            <h2 class="rq-section-title">Still uses an old name</h2>
            <p class="section-hint">
              These items still say a synonym of "{{ termName() }}". Replace changes it to the
              term's name.
            </p>
            @for (use of synonymUses(); track use.synonymId) {
              <div class="synonym-use" data-testid="term-synonym-use">
                <div class="synonym-use-header">
                  <h3 class="synonym-name" data-testid="term-synonym-use-name">"{{ use.synonymName }}"</h3>
                  @if (editableReferers(use).length > 1) {
                    <p-button label="Replace all" icon="pi pi-sync" size="small" [outlined]="true"
                              data-testid="term-synonym-replace-all"
                              [ariaLabel]="'Replace all uses of ' + use.synonymName + ' with ' + termName()"
                              [loading]="replacing() === use.synonymId"
                              [disabled]="replacing() !== null"
                              (onClick)="onReplace(use, editableReferers(use))" />
                  }
                </div>
                <p-table [value]="use.referers" [rows]="10">
                  <ng-template #header>
                    <tr>
                      <th>Type</th>
                      <th>Name</th>
                      <th><span class="rq-visually-hidden">Actions</span></th>
                    </tr>
                  </ng-template>
                  <ng-template #body let-r>
                    <tr data-testid="term-synonym-use-row">
                      <td>{{ r.entityType }}</td>
                      <td>
                        @if (refererLink(r); as link) {
                          <a [routerLink]="link" data-testid="term-synonym-use-link">{{ r.name }}</a>
                        } @else {
                          {{ r.name }}
                        }
                      </td>
                      <td class="row-action">
                        @if (canEditReferer(r)) {
                          <p-button label="Replace" size="small" [text]="true"
                                    data-testid="term-synonym-replace"
                                    [ariaLabel]="'Replace ' + use.synonymName + ' with ' + termName() + ' in ' + r.name"
                                    [disabled]="replacing() !== null"
                                    (onClick)="onReplace(use, [r])" />
                        }
                      </td>
                    </tr>
                  </ng-template>
                </p-table>
              </div>
            }
          </div>
        }

        <!-- Referenced By -->
        @if (!isNew() && term()?.referers?.length) {
          <div class="section" data-testid="term-referenced-by-section">
            <h2 class="rq-section-title">Referenced By</h2>
            <p-table [value]="term()!.referers!" [rows]="10">
              <ng-template #header>
                <tr>
                  <th>Type</th>
                  <th>Name</th>
                </tr>
              </ng-template>
              <ng-template #body let-r>
                <tr data-testid="term-referer-row">
                  <td>{{ r.entityType }}</td>
                  <td>{{ r.name }}</td>
                </tr>
              </ng-template>
            </p-table>
          </div>
        }

        <!-- Annotations -->
        <app-annotations-section
          (resolved)="onIssueResolved()"
          [projectName]="projectName"
          entityType="GlossaryTerm"
          [entityId]="termId()"
          />
          <app-sources-section
            [projectName]="projectName"
            entityType="GlossaryTerm"
            [entityId]="termId()"
            />
      }

    </div>

    <p-confirmDialog />
  `,
  styles: [`
    .page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: var(--rq-space-4); }
    .page-actions { display: flex; gap: var(--rq-space-2); }
    /* The rows are app-field's now (issue #132); the local .form-grid that redefined a
       160px label column is gone. Only control width stays with the caller. */
    app-field input, app-field textarea, app-field p-select { width: 100%; }
    .form-actions { margin-block: var(--rq-space-4) var(--rq-space-6); display: flex; align-items: center; gap: 0.5rem; flex-wrap: wrap; }
    .section { margin-top: var(--rq-space-6); }
    .section h2 { margin-bottom: var(--rq-space-3); }
    .clickable-row { cursor: pointer; }
    .clickable-row:hover td { background: var(--p-surface-100); }
    .section-hint { margin-block: 0 var(--rq-space-3); color: var(--p-text-muted-color); }
    .synonym-use + .synonym-use { margin-top: var(--rq-space-4); }
    .synonym-use-header { display: flex; align-items: center; justify-content: space-between; gap: var(--rq-space-2); margin-bottom: var(--rq-space-2); }
    .synonym-name { margin: 0; font-size: 1rem; }
    .row-action { text-align: end; }
  `]
})
export class TermEditorComponent implements OnInit, OnDestroy, DirtyCheckable {
  term = signal<GlossaryTermDto | null>(null);
  termName = signal('');
  /** #486: the breadcrumb and tab title follow this entity's current name (a rename, a rewrite). */
  private readonly artifactName = inject(ArtifactNameService);
  private readonly leafNameSync = effect(() => {
    const name = this.termName();
    if (name && !this.isNew()) {
      this.artifactName.set(name);
    }
  });
  termId = signal<number | null>(null);
  saving = signal(false);
  submitted = signal(false);
  errorMessage = signal<string | null>(null);
  retryable = signal(false);
  /**
   * #185. The edit form renders only once the detail GET resolves, so there is no window in which
   * a user can type into a form the load is about to reset. Starts true: an edit route is loading
   * from the first frame, and the create path clears it synchronously in ngOnInit.
   */
  loading = signal(true);
  loadError = signal<string | null>(null);
  private lastTermId: number | null = null;
  canonicalOptions = signal<{ label: string; value: number }[]>([]);

  /**
   * Mirrors the backend `@Size(max = ValidationLimits.ARTIFACT_NAME_MAX)` (#171). Bound with
   * `[attr.maxlength]` rather than `maxlength` on purpose: Angular's MaxLengthValidator directive
   * matches `[maxlength][formControl]`, so the plain binding would register a SECOND maxlength
   * validator on top of the one in the form definition. `attr.` sets the HTML attribute only, which
   * is all that is wanted here — the browser stops the typing, the form owns the validation.
   */
  readonly nameMaxLength = ARTIFACT_NAME_MAX_LENGTH;

  /**
   * `text` carries no maxLength: `AbstractTextEntity.getText()` is `@Lob`, so there is still no
   * backend `@Size` on it to mirror, and inventing a client-side cap would reject content the
   * server accepts. `name` is bounded — #171 supplied the real constraint.
   */
  readonly form = new FormGroup({
    name: new FormControl('', {
      validators: [Validators.required, Validators.maxLength(ARTIFACT_NAME_MAX_LENGTH)],
      nonNullable: true,
    }),
    text: new FormControl('', { nonNullable: true }),
    canonicalTermId: new FormControl<number | null>(null),
  });

  projectName = '';
  // #276: derived from the service signal rather than snapshotted after the load.
  canEdit = computed(() => this.permissionService.canEdit('GlossaryTerm'));
  canDelete = computed(() => this.permissionService.canDelete('GlossaryTerm'));

  /** #348: the referers still using each synonym, from the detail. */
  synonymUses = computed<SynonymUseDto[]>(() =>
    (this.term()?.synonymUses ?? []).filter(u => u.referers?.length));
  /** #348: the synonym whose replace is in flight, so the buttons can't fire twice. */
  replacing = signal<number | null>(null);

  private readonly destroyRef = inject(DestroyRef);
  private sseBound = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private termService: TermService,
    private permissionService: PermissionService,
    private messageService: MessageService,
    private confirmationService: ConfirmationService,
    private eventStreamService: EventStreamService,
    private announcer: AnnouncerService
  ) {}

  isNew(): boolean {
    return this.termId() === null;
  }

  ngOnInit(): void {
    this.route.paramMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(async params => {
      this.projectName = params.get('name') ?? '';
      const idParam = params.get('termId');
      const newIsNew = !idParam || idParam === 'new';

      // Reset the form synchronously for the new-term path BEFORE any awaits.
      // The async work below (loadForProject, loadCanonicalOptions) yields the
      // event loop, and any user input — or fast-typing E2E test — that lands
      // during those yields would otherwise be clobbered when the reset
      // eventually ran. Doing the reset first means subsequent typing is preserved.
      if (newIsNew) {
        this.termId.set(null);
        this.submitted.set(false);
        this.form.reset({ name: '', text: '', canonicalTermId: null });
        // Nothing to load, so resolve the gate synchronously (#185) - otherwise the create form
        // would sit behind the skeleton forever. Same reason the reset above is synchronous.
        this.loading.set(false);
        this.loadError.set(null);
      }

      await this.permissionService.loadForProject(this.projectName);

      // Load all terms for canonical selector (before loading detail)
      await this.loadCanonicalOptions(newIsNew ? null : Number(idParam));

      if (!newIsNew) {
        await this.loadTerm(Number(idParam));
      }
    });
  }

  /** Derived from the form, so there is no change-tracker to keep in step (#132). */
  /** A cross-session update arrived while dirty (#140): show the reload banner. */
  updateAvailable = signal(false);

  /** Discard local edits and re-apply the latest server state (from the update banner, #140). */
  async reloadFromExternalChange(): Promise<void> {
    this.updateAvailable.set(false);
    this.form.markAsPristine();
    if (this.lastTermId != null) {
      await this.loadTerm(this.lastTermId, false);
    }
    this.announcer.announce('Term reloaded.');
  }


  /**
   * #486: an issue was resolved in the annotations section; its position may have changed this
   * term (a rewrite, a merge, a spelling fix). Reload it as for a change from another session,
   * keeping unsaved edits.
   */
  onIssueResolved(): void {
    const id = this.termId();
    if (id == null) return;
    const dirty = this.hasUnsavedChanges();
    void this.loadTerm(id, false);
    if (dirty) {
      this.updateAvailable.set(true);
      this.announcer.announce('Resolving the issue changed this term. Your unsaved changes are preserved.');
    }
  }
  hasUnsavedChanges(): boolean {
    return this.form.dirty;
  }

  ngOnDestroy(): void {
    const id = this.termId();
    if (id) {
      void this.eventStreamService.removeSubscription('GlossaryTerm', id);
    }
  }

  private async loadCanonicalOptions(excludeId: number | null): Promise<void> {
    try {
      const all = await this.termService.listTerms(this.projectName);
      this.canonicalOptions.set(
        all
          .filter(t => t.id !== excludeId)
          .map(t => ({ label: t.name, value: t.id }))
      );
    } catch {
      // non-fatal — canonical selector just won't be populated
    }
  }

  /**
   * Reads the term and applies it in two parts: server state always, form state only when the
   * user has nothing unsaved.
   *
   * The guard used to live on the SSE subscription below, as an early `return` before the reload
   * was even issued. That protected the SSE path and left the *initial* load free to patch over
   * whatever the user had typed - `page.goto()` on the edit route returns long before this fetch
   * does, so anything typed in that gap was silently discarded and the form went back to pristine
   * (#185). Moving the check in here covers every caller, and the early `return` is gone so a
   * remote change still refreshes `term` / `termId` even while the form is dirty - the stale-
   * collection trap #184 hit in `actor-editor`.
   *
   * The check sits after the await on purpose, so it catches edits made while the request was
   * still in flight.
   *
   * `termName` is the *persisted* name - it titles the page and is quoted in the delete
   * confirmation - so it moves only with the form, matching `goal-editor`. `submitted` likewise:
   * clearing it would hide validation errors the user is looking at.
   */
  /** Re-run the last attempted load; wired to the error state's (retry) output. */
  retryLoad(): void {
    if (this.lastTermId != null) {
      void this.loadTerm(this.lastTermId);
    }
  }

  /**
   * @param skeleton show the loading skeleton and the retryable error state. Suppressed for the
   *                 post-save refetch and the SSE refresh, where blanking the form the user is
   *                 looking at would be worse than a stale moment. Mirrors `scenario-editor`.
   */
  private async loadTerm(id: number, skeleton = true): Promise<void> {
    this.lastTermId = id;
    if (skeleton) {
      this.loading.set(true);
      this.loadError.set(null);
    }
    try {
      const t = await this.termService.getTerm(this.projectName, id);
      this.term.set(t);
      this.termId.set(t.id);
      if (!this.hasUnsavedChanges()) {
        this.termName.set(t.name);
        this.form.patchValue({
          name: t.name,
          text: t.text ?? '',
          canonicalTermId: t.canonicalTermId ?? null,
        });
        this.form.markAsPristine();
        this.submitted.set(false);
      }
    } catch {
      if (skeleton) {
        this.loadError.set('Failed to load term.');
      } else {
        this.errorMessage.set('Failed to load term.');
      }
    } finally {
      if (skeleton) {
        this.loading.set(false);
      }
    }
    if (id && !this.sseBound) {
      void this.eventStreamService.addSubscription('GlossaryTerm', id);
      this.sseBound = true;
      this.eventStreamService.events$
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe(envelope => {
        if (envelope.targetType !== 'GlossaryTerm' || envelope.targetId !== id) return;
        if (envelope.eventType === 'TargetDeleted') {
          this.announcer.announce('This term was deleted in another session.');
          return;
        }
        const dirty = this.hasUnsavedChanges();
        void this.loadTerm(id, false);
        if (dirty) {
          this.updateAvailable.set(true);
          this.announcer.announce('This term was changed elsewhere. Your unsaved changes are preserved.');
        } else {
          this.announcer.announceThrottled('GlossaryTerm:' + id, 'This term was updated.');
        }
      });
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
      // "Term name is required." message — `required` now renders under the field.
      this.form.markAllAsTouched();
      return;
    }

    this.saving.set(true);
    this.errorMessage.set(null);
    this.retryable.set(false);

    const { name, text, canonicalTermId } = this.form.getRawValue();
    const trimmedName = name.trim();
    const result = await this.termService.saveTerm(
      // '' clears; null would leave the definition as it is (#316).
      this.projectName, this.termId(), trimmedName, text, canonicalTermId
    );
    this.saving.set(false);

    if (result.success) {
      this.messageService.add({ severity: 'success', summary: 'Term saved', life: 3000 });
      const saved = result.entity as GlossaryTermDto | null;
      if (this.isNew() && saved?.id) {
        this.form.patchValue({ name: trimmedName });
        this.form.markAsPristine();
        this.router.navigate(['/projects', this.projectName, 'terms', saved.id], { replaceUrl: true });
      } else {
        // Mark pristine BEFORE the refetch. The load method only adopts server state into the
        // form when the form has nothing unsaved (#185), and what was "unsaved" a moment ago is
        // exactly what we just persisted - leaving it dirty would make the reload skip its own
        // result, so Save stayed enabled on a freshly saved form. Same ordering scenario-editor
        // uses around its post-save refetch.
        this.form.markAsPristine();
        await this.loadTerm(this.termId()!, false);
      }
      return;
    }

    // Field violations land on their controls; only what could not be placed becomes a
    // page-level message, so nothing is dropped and nothing is duplicated.
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
      message: `Delete term "${this.termName()}"? This cannot be undone.`,
      header: 'Confirm Delete',
      icon: 'pi pi-exclamation-triangle',
      accept: async () => {
        const result = await this.termService.deleteTerm(this.projectName, this.termId()!);
        if (result.success) {
          this.messageService.add({ severity: 'success', summary: 'Term deleted', life: 3000 });
          this.router.navigate(['/projects', this.projectName, 'terms']);
        } else {
          this.errorMessage.set(refusalMessage(result, 'Delete failed.'));
        }
      }
    });
  }

  canEditReferer(r: EntityReferenceDto): boolean {
    return this.permissionService.canEdit(r.entityType);
  }

  editableReferers(use: SynonymUseDto): EntityReferenceDto[] {
    return use.referers.filter(r => this.canEditReferer(r));
  }

  refererLink(r: EntityReferenceDto): string[] | null {
    return entityRoute(this.projectName, r.entityType, r.id);
  }

  /**
   * #348: replace the synonym with the term's name in the given referers, then say what
   * happened and reload, so the rows that were changed drop out of the panel.
   */
  async onReplace(use: SynonymUseDto, referers: EntityReferenceDto[]): Promise<void> {
    const termId = this.termId();
    if (termId == null || !referers.length || this.replacing() !== null) {
      return;
    }
    this.replacing.set(use.synonymId);
    this.errorMessage.set(null);
    try {
      const result = await this.termService.replaceTermText(
        this.projectName, termId, use.synonymId, referers);
      if (!result.success) {
        this.errorMessage.set(result.error ?? 'Replace failed.');
        return;
      }
      const outcome = result.entity ?? { applied: [], refused: [], unchanged: [] };
      const message = this.replaceSummary(use.synonymName, outcome);
      this.messageService.add({
        severity: outcome.refused.length ? 'warn' : 'success',
        summary: outcome.applied.length ? 'Replaced' : 'Nothing replaced',
        detail: message,
        life: 5000,
      });
      this.announcer.announce(message);
      await this.loadTerm(termId, false);
    } finally {
      this.replacing.set(null);
    }
  }

  /** One sentence per outcome, e.g. `Replaced "a" with "b" in 2 items. Refused, ...`. */
  replaceSummary(synonymName: string, outcome: ReplaceTermTextResultDto): string {
    const count = (n: number) => n === 1 ? '1 item' : `${n} items`;
    const names = (refs: EntityReferenceDto[]) =>
      refs.map(r => `${r.entityType} "${r.name ?? r.id}"`).join(', ');
    const parts = [`Replaced "${synonymName}" with "${this.termName()}" in ${count(outcome.applied.length)}.`];
    if (outcome.refused.length) {
      parts.push(`Not changed, you can't edit: ${names(outcome.refused)}.`);
    }
    if (outcome.unchanged.length) {
      parts.push(`${count(outcome.unchanged.length)} no longer used it.`);
    }
    return parts.join(' ');
  }

  navigateToTerm(termId: number): void {
    this.router.navigate(['/projects', this.projectName, 'terms', termId]);
  }

  onBack(): void {
    this.router.navigate(['/projects', this.projectName, 'terms']);
  }
}
