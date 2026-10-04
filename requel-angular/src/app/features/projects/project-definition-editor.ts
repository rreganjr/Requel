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
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, DestroyRef, OnInit, computed,
  inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { TextareaModule } from 'primeng/textarea';
import { AssistantDefinitionsService } from '../../core/assistant-definitions.service';
import { PermissionService } from '../../core/permission.service';
import { ListPageComponent } from '../../shared/list-page';
import { SubmitErrorComponent } from '../../shared/app-submit-error';
import {
  CONTEXT_PROVIDERS, CORPUS_PROVIDERS, CORPUS_SET_KINDS, DEFINITION_RISK, DefinitionDraft,
  DefinitionKind,
  ProjectDefinitionDto, REVIEWABLE_TYPES, kindLabel,
} from '../../models/assistant-definition';
import { FieldViolation } from '../../models/command';
import { DirtyCheckable } from '../../core/dirty-check.guard';

/** Rough characters the server allows for instructions: requel.ai.max-input-tokens × 4. */
const INSTRUCTION_CHARS = 64000;

/**
 * One of a project's assistant definitions (issue #264), at
 * /projects/:name/definitions/:key, or a new one at .../definitions/new. A bundled definition is
 * shown read-only with Customize; the project's copy is edited beside the bundled baseline it came
 * from. A refused save shows each problem on its field; a stale one says someone else changed it.
 */
@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-project-definition-editor',
  standalone: true,
  imports: [FormsModule, RouterLink, ButtonModule, CheckboxModule, InputTextModule, SelectModule,
            TextareaModule, ListPageComponent, SubmitErrorComponent],
  template: `
    <app-list-page [title]="title()" [showSearch]="false">
      @if (!canManage()) {
        <p class="hint" data-testid="definition-forbidden">
          You don't have permission to manage AI definitions in this project.
        </p>
      } @else {
        <p class="hint">
          <a [routerLink]="['/projects', projectName, 'definitions']">All AI definitions</a>
        </p>
        <p class="hint" data-testid="definition-risk">{{ risk }}</p>
        <app-submit-error [message]="errorMessage()" testid="definition-error"
                          [retryable]="loadFailed()" (retry)="load()" />
        @if (loaded()) {
          @if (readOnly()) {
            <p class="hint" data-testid="definition-bundled-note">
              This is the bundled definition. Customize it to change it for this project.
            </p>
            <p-button label="Customize" size="small" (onClick)="fork()" [loading]="saving()"
                      data-testid="definition-fork" />
          }
          @if (newerBundled()) {
            <p class="newer" data-testid="definition-newer">
              The bundled definition is now v{{ current()?.bundledVersion }}; this copy was made from
              v{{ current()?.forkedFromVersion }}. Revert and customize again to start from it.
            </p>
          }
          <form class="definition-form" (ngSubmit)="save()" data-testid="definition-form">
            @if (isNew()) {
              <label for="def-kind">Kind</label>
              <p-select inputId="def-kind" name="kind" [options]="kinds" optionLabel="label"
                        optionValue="value" [(ngModel)]="draft.kind" (onChange)="kindChanged()"
                        data-testid="definition-kind" />
              <label for="def-key">Key</label>
              <input pInputText id="def-key" name="key" [(ngModel)]="draft.key"
                     data-testid="definition-key" />
              @if (fieldErrors()['key']) { <small class="field-error" data-testid="error-key">{{ fieldErrors()['key'] }}</small> }
              @if (fieldErrors()['kind']) { <small class="field-error" data-testid="error-kind">{{ fieldErrors()['kind'] }}</small> }
            } @else {
              <p class="hint" data-testid="definition-identity">
                {{ kindLabel(draft.kind) }} · {{ draft.key }} · v{{ current()?.version }}
                · {{ current()?.inEffectFor?.length ? 'in effect for ' + current()!.inEffectFor.join(', ')
                     : 'not in effect' }}
              </p>
            }

            <label for="def-name">Name</label>
            <input pInputText id="def-name" name="displayName" [(ngModel)]="draft.displayName"
                   [disabled]="readOnly()" data-testid="definition-name" />
            @if (fieldErrors()['displayName']) { <small class="field-error">{{ fieldErrors()['displayName'] }}</small> }

            <fieldset>
              <legend>{{ draft.kind === 'CORPUS' ? 'Sets it analyzes' : 'Entity types' }}
                ({{ draft.kind === 'POLICY' ? 'none: every type' : 'none: the fallback' }})</legend>
              @for (t of scopeOptions(); track t) {
                <span class="check">
                  <p-checkbox [inputId]="'scope-' + t" [binary]="true" [ngModel]="draft.scope.includes(t)"
                              [ngModelOptions]="{ standalone: true }" [disabled]="readOnly()"
                              (ngModelChange)="toggle(draft.scope, t, $event)"
                              [attr.data-testid]="'scope-' + t" />
                  <label [for]="'scope-' + t">{{ t }}</label>
                </span>
              }
              @if (fieldErrors()['scope']) { <small class="field-error" data-testid="error-scope">{{ fieldErrors()['scope'] }}</small> }
            </fieldset>

            <fieldset>
              <legend>Context it reads</legend>
              @for (p of providerOptions(); track p) {
                <span class="check">
                  <p-checkbox [inputId]="'provider-' + p" [binary]="true"
                              [ngModel]="draft.contextProviders.includes(p)"
                              [ngModelOptions]="{ standalone: true }" [disabled]="readOnly()"
                              (ngModelChange)="toggle(draft.contextProviders, p, $event)"
                              [attr.data-testid]="'provider-' + p" />
                  <label [for]="'provider-' + p">{{ p }}</label>
                </span>
              }
              @if (fieldErrors()['contextProviders']) { <small class="field-error" data-testid="error-contextProviders">{{ fieldErrors()['contextProviders'] }}</small> }
            </fieldset>

            <label for="def-instructions">Instructions
              <span class="count" data-testid="instructions-count">
                {{ draft.instructions.length }} / {{ instructionChars }}</span></label>
            <textarea pTextarea id="def-instructions" name="instructions" rows="12"
                      [(ngModel)]="draft.instructions" [disabled]="readOnly()"
                      data-testid="definition-instructions"></textarea>
            @if (fieldErrors()['instructions']) { <small class="field-error" data-testid="error-instructions">{{ fieldErrors()['instructions'] }}</small> }

            <fieldset>
              <legend>Finding types</legend>
              @for (v of draft.vocabulary; track $index) {
                <div class="vocab-row">
                  <input pInputText [name]="'vocab-type-' + $index" [(ngModel)]="v.type"
                         placeholder="TYPE" [disabled]="readOnly()"
                         [attr.data-testid]="'vocab-type-' + $index" />
                  <input pInputText class="grow" [name]="'vocab-desc-' + $index"
                         [(ngModel)]="v.description" placeholder="What it means"
                         [disabled]="readOnly()" />
                  @if (!readOnly()) {
                    <p-button icon="pi pi-times" [text]="true" size="small"
                              ariaLabel="Remove finding type" (onClick)="removeVocabulary($index)"
                              [attr.data-testid]="'vocab-remove-' + $index" />
                  }
                </div>
              }
              @if (!readOnly()) {
                <p-button label="Add finding type" icon="pi pi-plus" [text]="true" size="small"
                          (onClick)="addVocabulary()" data-testid="vocab-add" />
              }
              @if (fieldErrors()['vocabulary']) { <small class="field-error" data-testid="error-vocabulary">{{ fieldErrors()['vocabulary'] }}</small> }
            </fieldset>

            <span class="check">
              <p-checkbox inputId="def-local" name="localOnly" [binary]="true"
                          [(ngModel)]="draft.localOnly" [disabled]="readOnly()"
                          data-testid="definition-local-only" />
              <label for="def-local">Local AI provider only (never send this work off the server)</label>
            </span>
            @if (otherErrors().length > 0) {
              <ul class="field-error" data-testid="definition-other-errors">
                @for (e of otherErrors(); track $index) { <li>{{ e }}</li> }
              </ul>
            }

            @if (!readOnly()) {
              <div class="form-actions">
                <p-button type="submit" [label]="isNew() ? 'Create' : 'Save'" [loading]="saving()"
                          data-testid="definition-save" />
                @if (!isNew() && current()?.bundledVersion != null) {
                  <p-button label="Revert to bundled" severity="secondary" [outlined]="true"
                            (onClick)="revert()" [disabled]="saving()" data-testid="definition-revert" />
                }
                @if (!isNew() && current()?.bundledVersion == null) {
                  <p-button label="Delete" severity="danger" [outlined]="true"
                            (onClick)="remove()" [disabled]="saving()" data-testid="definition-delete" />
                }
              </div>
            }
          </form>

          @if (baseline() && !readOnly()) {
            <h2 class="section-title">Bundled baseline (v{{ baseline()!.version }})</h2>
            <pre class="baseline" data-testid="definition-baseline">{{ baseline()!.instructions }}</pre>
          }
        }
      }
    </app-list-page>
  `,
  styles: [`
    .hint { margin: 0 0 0.75rem; color: var(--p-text-muted-color); }
    .newer { color: var(--p-orange-600, #c2410c); }
    .definition-form { display: flex; flex-direction: column; gap: 0.5rem; max-width: 52rem; }
    fieldset { border: 1px solid var(--rq-card-border, #ddd); border-radius: 6px;
      padding: 0.5rem 0.75rem; display: flex; flex-wrap: wrap; gap: 0.5rem 1rem; }
    .check { display: inline-flex; gap: 0.35rem; align-items: center; }
    .vocab-row { display: flex; gap: 0.5rem; flex-basis: 100%; align-items: center; }
    .vocab-row .grow { flex: 1; }
    .count { color: var(--p-text-muted-color); font-size: 0.8rem; margin-left: 0.5rem; }
    .field-error { color: var(--p-red-600, #b91c1c); }
    .form-actions { display: flex; gap: 0.5rem; margin-top: 0.5rem; }
    .section-title { margin: 1.5rem 0 0.5rem; font-size: 1.1rem; }
    .baseline { white-space: pre-wrap; background: var(--p-surface-100, #f5f5f5); padding: 0.75rem;
      border-radius: 6px; max-height: 24rem; overflow: auto; }
  `]
})
export class ProjectDefinitionEditorComponent implements OnInit, DirtyCheckable {
  readonly current = signal<ProjectDefinitionDto | null>(null);
  readonly baseline = signal<ProjectDefinitionDto | null>(null);
  readonly loaded = signal(false);
  readonly loadFailed = signal(false);
  readonly saving = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly fieldErrors = signal<Record<string, string>>({});
  readonly otherErrors = signal<string[]>([]);
  readonly isNew = signal(false);
  readonly canManage = computed(() => this.permissionService.canEdit('AssistantDefinition'));
  readonly readOnly = computed(() => !this.isNew() && this.current()?.source === 'BUNDLED');
  readonly newerBundled = computed(() => {
    const d = this.current();
    return d != null && d.source === 'PROJECT' && d.bundledVersion != null
      && d.forkedFromVersion != null && d.bundledVersion > d.forkedFromVersion;
  });
  readonly title = computed(() => this.isNew() ? 'New AI definition'
    : this.current()?.displayName ?? 'AI definition');

  readonly kinds = [
    { label: 'Policy (a rule checked on every entity it applies to)', value: 'POLICY' },
    { label: 'Review (one per entity type)', value: 'REVIEW' },
    { label: 'Corpus analysis (relationships across a set)', value: 'CORPUS' },
  ];
  readonly risk = DEFINITION_RISK;
  readonly instructionChars = INSTRUCTION_CHARS;
  readonly kindLabel = kindLabel;

  draft: DefinitionDraft = ProjectDefinitionEditorComponent.empty('POLICY');
  /** The draft as last loaded or saved, for the unsaved-changes guard. */
  private saved = JSON.stringify(this.draft);
  protected projectName = '';
  private key = '';
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly service = inject(AssistantDefinitionsService);
  private readonly permissionService = inject(PermissionService);
  private readonly messageService = inject(MessageService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly cdr = inject(ChangeDetectorRef);

  static empty(kind: DefinitionKind): DefinitionDraft {
    return {
      kind, key: '', displayName: '', scope: [],
      contextProviders: kind === 'CORPUS' ? ['corpus-index', 'corpus-candidates'] : ['entity'],
      contextBudgets: {}, instructions: '',
      vocabulary: [{ type: '', description: '', category: 'quality' }], localOnly: false,
    };
  }

  ngOnInit(): void {
    this.route.paramMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(async params => {
      this.projectName = params.get('name') ?? '';
      this.key = params.get('key') ?? '';
      await this.permissionService.loadForProject(this.projectName);
      if (this.canManage()) {
        await this.load();
      }
    });
  }

  /** True when the form differs from what was last loaded or saved (dirtyCheckGuard). */
  hasUnsavedChanges(): boolean {
    return !this.readOnly() && JSON.stringify(this.draft) !== this.saved;
  }

  scopeOptions(): string[] {
    return this.draft.kind === 'CORPUS' ? CORPUS_SET_KINDS : REVIEWABLE_TYPES;
  }

  providerOptions(): string[] {
    return this.draft.kind === 'CORPUS' ? CORPUS_PROVIDERS : CONTEXT_PROVIDERS;
  }

  async load(): Promise<void> {
    this.errorMessage.set(null);
    this.loadFailed.set(false);
    if (this.key === 'new') {
      this.isNew.set(true);
      this.draft = ProjectDefinitionEditorComponent.empty('POLICY');
      this.saved = JSON.stringify(this.draft);
      this.loaded.set(true);
      return;
    }
    this.isNew.set(false);
    try {
      const detail = await this.service.get(this.projectName, this.key);
      this.show(detail.definition);
      this.baseline.set(detail.bundled);
      this.loaded.set(true);
    } catch {
      this.loadFailed.set(true);
      this.errorMessage.set('Failed to load the AI definition.');
    }
    this.cdr.markForCheck();
  }

  kindChanged(): void {
    this.draft = { ...ProjectDefinitionEditorComponent.empty(this.draft.kind),
      key: this.draft.key, displayName: this.draft.displayName,
      instructions: this.draft.instructions, vocabulary: this.draft.vocabulary };
  }

  toggle(list: string[], value: string, on: boolean): void {
    const at = list.indexOf(value);
    if (on && at < 0) list.push(value);
    if (!on && at >= 0) list.splice(at, 1);
  }

  addVocabulary(): void {
    this.draft.vocabulary.push({ type: '', description: '', category: 'quality' });
  }

  removeVocabulary(index: number): void {
    this.draft.vocabulary.splice(index, 1);
  }

  async save(): Promise<void> {
    this.saving.set(true);
    this.clearErrors();
    try {
      const result = this.isNew()
        ? await this.service.create(this.projectName, this.draft)
        : await this.service.edit(this.projectName, this.current()!.lockVersion, this.draft);
      if (result.success && result.entity) {
        this.messageService.add({ severity: 'success', life: 3000,
          summary: this.isNew() ? `Created "${result.entity.displayName}"` : 'Saved' });
        if (this.isNew()) {
          this.saved = JSON.stringify(this.draft);
          this.router.navigate(['/projects', this.projectName, 'definitions', result.entity.key]);
        } else {
          this.show(result.entity);
        }
      } else {
        this.showFailure(result.violations, result.error, result.status);
      }
    } finally {
      this.saving.set(false);
      this.cdr.markForCheck();
    }
  }

  async fork(): Promise<void> {
    this.saving.set(true);
    this.clearErrors();
    try {
      const result = await this.service.fork(this.projectName, this.key);
      if (result.success) {
        await this.load();
      } else {
        this.showFailure(result.violations, result.error, result.status);
      }
    } finally {
      this.saving.set(false);
    }
  }

  async revert(): Promise<void> {
    await this.drop(() => this.service.revert(this.projectName, this.key,
      this.current()!.lockVersion), 'Reverted to the bundled definition');
  }

  async remove(): Promise<void> {
    await this.drop(() => this.service.delete(this.projectName, this.key,
      this.current()!.lockVersion), 'Deleted');
  }

  private async drop(write: () => Promise<{ success: boolean; error: string | null;
                       violations: FieldViolation[] | null; status?: number }>,
                     done: string): Promise<void> {
    this.saving.set(true);
    this.clearErrors();
    try {
      const result = await write();
      if (result.success) {
        this.messageService.add({ severity: 'success', summary: done, life: 3000 });
        this.saved = JSON.stringify(this.draft);
        this.router.navigate(['/projects', this.projectName, 'definitions']);
      } else {
        this.showFailure(result.violations, result.error, result.status);
      }
    } finally {
      this.saving.set(false);
    }
  }

  private show(definition: ProjectDefinitionDto): void {
    this.current.set(definition);
    this.draft = {
      kind: definition.kind, key: definition.key, displayName: definition.displayName,
      scope: [...definition.scope], contextProviders: [...definition.contextProviders],
      contextBudgets: { ...definition.contextBudgets }, instructions: definition.instructions,
      vocabulary: definition.vocabulary.map(v => ({ ...v })), localOnly: definition.localOnly,
    };
    this.saved = JSON.stringify(this.draft);
  }

  private clearErrors(): void {
    this.errorMessage.set(null);
    this.fieldErrors.set({});
    this.otherErrors.set([]);
  }

  /** Each problem on its field; the rest listed under the form. */
  private showFailure(violations: FieldViolation[] | null, error: string | null,
                      status?: number): void {
    if (status === 409) {
      this.errorMessage.set('Someone else changed this definition. Reload it and make your'
        + ' change again.');
      return;
    }
    if (!violations || violations.length === 0) {
      this.errorMessage.set(error ?? 'The change failed.');
      return;
    }
    const known = new Set(['key', 'kind', 'displayName', 'scope', 'contextProviders',
      'instructions', 'vocabulary']);
    const byField: Record<string, string> = {};
    const other: string[] = [];
    for (const v of violations) {
      if (v.field && known.has(v.field)) {
        byField[v.field] = byField[v.field] ? `${byField[v.field]} ${v.message}` : v.message;
      } else {
        other.push(v.message);
      }
    }
    this.fieldErrors.set(byField);
    this.otherErrors.set(other);
  }
}
