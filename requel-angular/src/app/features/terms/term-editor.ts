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
import { PageHeaderComponent } from '../../shared/page-header';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { DirtyCheckable } from '../../core/dirty-check.guard';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { InputText } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';
import { SelectModule } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { MessageModule } from 'primeng/message';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ConfirmationService, MessageService } from 'primeng/api';
import { GlossaryTermDto } from '../../models/term';
import { TermService } from '../../core/term.service';
import { PermissionService } from '../../core/permission.service';
import { EventStreamService } from '../../core/event-stream.service';
import { AnnotationsSectionComponent } from '../../shared/annotations-section';
import { AppCardComponent } from '../../shared/app-card';
import { AppFieldComponent, AppFieldControlDirective } from '../../shared/app-field';
import { applyCommandErrors, clearServerErrors } from '../../shared/form-errors';

/**
 * JPA entity property name -> form control name, for {@link applyCommandErrors}.
 *
 * `CommandController` reports violations using entity property names (see
 * `BeanValidationExceptionAdapter`), which mostly match the control names here.
 * `canonicalTerm` is the exception: the entity holds the related `GlossaryTerm`, the
 * form holds its id.
 */
const TERM_FIELD_MAP: Record<string, string> = {
  canonicalTerm: 'canonicalTermId',
};

/**
 * Separator for several command-level messages sharing the one page-level banner.
 * Semicolons, not spaces: two sentence fragments run together ("Email is invalid Phone
 * is required") read as one broken sentence. This is the separator the pre-#132 code
 * used and e2e/account.e2e.ts asserts.
 */
const SEPARATOR = '; ';

@Component({
  selector: 'app-term-editor',
  standalone: true,
  imports: [
    PageHeaderComponent,
    AppCardComponent,
    ReactiveFormsModule,
    ButtonModule,
    InputText,
    TextareaModule,
    SelectModule,
    TableModule,
    MessageModule,
    ConfirmDialogModule,
    AnnotationsSectionComponent,
    AppFieldComponent,
    AppFieldControlDirective,
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

      @if (errorMessage()) {
        <p-message severity="error" [text]="errorMessage()!" data-testid="term-error" />
      }

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
              rows="5"
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
        [projectName]="projectName"
        entityType="GlossaryTerm"
        [entityId]="termId()"
        [canEdit]="canEdit()" />
    </div>

    <p-confirmDialog />
  `,
  styles: [`
    .page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: var(--rq-space-4); }
    .page-actions { display: flex; gap: var(--rq-space-2); }
    /* The rows are app-field's now (issue #132); the local .form-grid that redefined a
       160px label column is gone. Only control width stays with the caller. */
    app-field input, app-field textarea, app-field p-select { width: 100%; }
    .form-actions { margin-block: var(--rq-space-4) var(--rq-space-6); }
    .section { margin-top: var(--rq-space-6); }
    .section h2 { margin-bottom: var(--rq-space-3); }
    .clickable-row { cursor: pointer; }
    .clickable-row:hover td { background: var(--p-surface-100); }
  `]
})
export class TermEditorComponent implements OnInit, OnDestroy, DirtyCheckable {
  term = signal<GlossaryTermDto | null>(null);
  termName = signal('');
  termId = signal<number | null>(null);
  saving = signal(false);
  submitted = signal(false);
  errorMessage = signal<string | null>(null);
  canonicalOptions = signal<{ label: string; value: number }[]>([]);

  /**
   * `text` carries no maxLength yet — there is no backend `@Size` on it to mirror, and
   * inventing a client-side cap would reject content the server accepts. #171 adds the
   * real constraints; the value then comes from `shared/validation-limits.ts`.
   */
  readonly form = new FormGroup({
    name: new FormControl('', { validators: Validators.required, nonNullable: true }),
    text: new FormControl('', { nonNullable: true }),
    canonicalTermId: new FormControl<number | null>(null),
  });

  projectName = '';
  canEdit = signal(false);
  canDelete = signal(false);

  private paramSub?: Subscription;
  private sseSub?: Subscription;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private termService: TermService,
    private permissionService: PermissionService,
    private messageService: MessageService,
    private confirmationService: ConfirmationService,
    private eventStreamService: EventStreamService
  ) {}

  isNew(): boolean {
    return this.termId() === null;
  }

  ngOnInit(): void {
    this.paramSub = this.route.paramMap.subscribe(async params => {
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
      }

      await this.permissionService.loadForProject(this.projectName);
      this.canEdit.set(this.permissionService.canEdit('GlossaryTerm'));
      this.canDelete.set(this.permissionService.canDelete('GlossaryTerm'));

      // Load all terms for canonical selector (before loading detail)
      await this.loadCanonicalOptions(newIsNew ? null : Number(idParam));

      if (!newIsNew) {
        await this.loadTerm(Number(idParam));
      }
    });
  }

  /** Derived from the form, so there is no change-tracker to keep in step (#132). */
  hasUnsavedChanges(): boolean {
    return this.form.dirty;
  }

  ngOnDestroy(): void {
    this.paramSub?.unsubscribe();
    const id = this.termId();
    if (id) {
      void this.eventStreamService.removeSubscription('GlossaryTerm', id);
    }
    this.sseSub?.unsubscribe();
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

  private async loadTerm(id: number): Promise<void> {
    try {
      const t = await this.termService.getTerm(this.projectName, id);
      this.term.set(t);
      this.termId.set(t.id);
      this.termName.set(t.name);
      // An SSE-driven reload must not throw away what the user is editing. The guard
      // is on the caller side for the SSE path below; here we only refresh a form the
      // user has not touched.
      this.form.patchValue({
        name: t.name,
        text: t.text ?? '',
        canonicalTermId: t.canonicalTermId ?? null,
      });
      this.form.markAsPristine();
      this.submitted.set(false);
    } catch {
      this.errorMessage.set('Failed to load term.');
    }
    if (id && !this.sseSub) {
      void this.eventStreamService.addSubscription('GlossaryTerm', id);
      this.sseSub = this.eventStreamService.events$.subscribe(envelope => {
        if (envelope.targetType === 'GlossaryTerm' && envelope.targetId === id) {
          // Don't clobber in-progress edits with a remote change (same guard the N5
          // editors use). The user's copy wins until they save or navigate away.
          if (this.hasUnsavedChanges()) {
            return;
          }
          void this.loadTerm(id);
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

    const { name, text, canonicalTermId } = this.form.getRawValue();
    const trimmedName = name.trim();
    const result = await this.termService.saveTerm(
      this.projectName, this.termId(), trimmedName, text || null, canonicalTermId
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
        await this.loadTerm(this.termId()!);
      }
      return;
    }

    // Field violations land on their controls; only what could not be placed becomes a
    // page-level message, so nothing is dropped and nothing is duplicated.
    const unresolved = applyCommandErrors(this.form, result.violations, TERM_FIELD_MAP);
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
          this.errorMessage.set(result.error ?? 'Delete failed.');
        }
      }
    });
  }

  navigateToTerm(termId: number): void {
    this.router.navigate(['/projects', this.projectName, 'terms', termId]);
  }

  onBack(): void {
    this.router.navigate(['/projects', this.projectName, 'terms']);
  }
}
