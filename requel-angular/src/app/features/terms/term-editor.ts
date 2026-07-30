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
import { FormsModule } from '@angular/forms';
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

@Component({
  selector: 'app-term-editor',
  standalone: true,
  imports: [PageHeaderComponent, FormsModule, ButtonModule, InputText, TextareaModule, SelectModule,
            TableModule, MessageModule, ConfirmDialogModule, AnnotationsSectionComponent],
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

      <div class="form-grid">
        <label for="name">Term</label>
        <input id="name" pInputText [(ngModel)]="name" placeholder="Term name" />

        <label for="text">Definition</label>
        <textarea id="text" pTextarea [(ngModel)]="text" rows="5"
                  placeholder="Definition of this term"></textarea>

        <label for="canonical">Canonical Term</label>
        <p-select id="canonical" [options]="canonicalOptions()" [(ngModel)]="canonicalTermId"
                  optionLabel="label" optionValue="value"
                  data-testid="term-canonical-select"
                  placeholder="None (this is a canonical term)" [showClear]="true" />
      </div>

      <div class="form-actions">
        <p-button label="Save" icon="pi pi-check" data-testid="term-save" (onClick)="onSave()" [loading]="saving()"
                  [disabled]="!isNew() && !isDirty()" />
      </div>

      <!-- Alternate Terms (terms that point to this as their canonical) -->
      @if (!isNew() && term()?.alternateTerms?.length) {
        <div class="section" data-testid="term-alternate-terms-section">
          <h3>Alternate Terms</h3>
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
          <h3>Referenced By</h3>
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
    .page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem; }
    .page-actions { display: flex; gap: 0.5rem; }
    .form-grid { display: grid; grid-template-columns: 160px 1fr; gap: 0.75rem 1rem; align-items: start; max-width: 700px; margin-bottom: 1rem; }
    .form-grid label { font-weight: 600; padding-top: 0.4rem; }
    .form-grid input, .form-grid textarea, .form-grid p-select { width: 100%; }
    .form-actions { margin-bottom: 1.5rem; }
    .section { margin-top: 1.5rem; }
    .section h3 { margin-bottom: 0.75rem; }
    .clickable-row { cursor: pointer; }
    .clickable-row:hover td { background: var(--p-surface-100); }
  `]
})
export class TermEditorComponent implements OnInit, OnDestroy, DirtyCheckable {
  term = signal<GlossaryTermDto | null>(null);
  termName = signal('');
  termId = signal<number | null>(null);
  saving = signal(false);
  errorMessage = signal<string | null>(null);
  canonicalOptions = signal<{ label: string; value: number }[]>([]);

  name = '';
  text = '';
  canonicalTermId: number | null = null;

  private originalName = '';
  private originalText = '';
  private originalCanonicalTermId: number | null = null;

  projectName = '';
  canEdit = signal(false);
  canDelete = signal(false);

  isDirty(): boolean {
    return this.name !== this.originalName
      || this.text !== this.originalText
      || this.canonicalTermId !== this.originalCanonicalTermId;
  }

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
      // eventually ran. Doing the reset first means subsequent ngModel writes
      // from typing are preserved.
      if (newIsNew) {
        this.termId.set(null);
        this.name = '';
        this.text = '';
        this.canonicalTermId = null;
        this.originalName = '';
        this.originalText = '';
        this.originalCanonicalTermId = null;
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

  hasUnsavedChanges(): boolean {
    return this.isDirty();
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
      this.name = t.name;
      this.text = t.text ?? '';
      this.canonicalTermId = t.canonicalTermId ?? null;
      this.originalName = this.name;
      this.originalText = this.text;
      this.originalCanonicalTermId = this.canonicalTermId;
    } catch {
      this.errorMessage.set('Failed to load term.');
    }
    if (id && !this.sseSub) {
      void this.eventStreamService.addSubscription('GlossaryTerm', id);
      this.sseSub = this.eventStreamService.events$.subscribe(envelope => {
        if (envelope.targetType === 'GlossaryTerm' && envelope.targetId === id) {
          void this.loadTerm(id);
        }
      });
    }
  }

  async onSave(): Promise<void> {
    if (!this.name.trim()) {
      this.errorMessage.set('Term name is required.');
      return;
    }
    this.saving.set(true);
    this.errorMessage.set(null);
    const result = await this.termService.saveTerm(
      this.projectName, this.termId(), this.name.trim(), this.text || null, this.canonicalTermId
    );
    this.saving.set(false);
    if (result.success) {
      this.messageService.add({ severity: 'success', summary: 'Term saved', life: 3000 });
      const saved = result.entity as GlossaryTermDto | null;
      if (this.isNew() && saved?.id) {
        this.originalName = this.name.trim();
        this.originalText = this.text;
        this.originalCanonicalTermId = this.canonicalTermId;
        this.router.navigate(['/projects', this.projectName, 'terms', saved.id], { replaceUrl: true });
      } else {
        await this.loadTerm(this.termId()!);
      }
    } else {
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
