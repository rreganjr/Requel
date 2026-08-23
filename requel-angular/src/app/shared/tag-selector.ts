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
import { Component, Input, OnChanges, SimpleChanges, signal } from '@angular/core';
import { FormGroup, FormControl, ReactiveFormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { InputText } from 'primeng/inputtext';
import { MessageService } from 'primeng/api';
import { TagCategoryDto, TagDto, tagLabel } from '../models/tag';
import { TagService } from '../core/tag.service';
import { AppChipComponent } from './app-chip';
import { ErrorStateComponent } from './error-state';
import { SubmitErrorComponent } from './app-submit-error';
import { notBlank } from './form-errors';
import { InlineErrorComponent } from './app-inline-error';

/**
 * Reusable tag chip/selector for any taggable entity. Shows the entity's assigned tags as
 * removable chips and (when {@code canEdit}) an add row with category-aware autocomplete
 * fed by the project's existing tags/categories. Mirrors the annotations-section pattern.
 */
@Component({
  selector: 'app-tag-selector',
  standalone: true,
  imports: [ReactiveFormsModule, ButtonModule, InputText, AppChipComponent, ErrorStateComponent, SubmitErrorComponent, InlineErrorComponent],
  template: `
    @if (entityId != null) {
      <div class="tags-section" data-testid="tags-section">
        <div class="section-header"><h3>Tags</h3></div>

        @if (loadError()) {
          <app-error-state severity="warn" [message]="loadError()!" retryLabel="Retry"
                           testid="tags-load-error" (retry)="reload()" />
        }
        <app-submit-error [message]="actionError()" testid="tag-action-error" />

        <div class="chips" data-testid="tag-chips">
          @for (t of assigned(); track t.id) {
            <app-chip data-testid="tag-chip" [attr.data-tag]="label(t)" [label]="label(t)"
                      [dotColor]="chipColor(t)" [removable]="canEdit"
                      removeTestid="tag-remove" [removeAriaLabel]="'Remove tag ' + label(t)"
                      (remove)="removeTag(t)" />
          }
          @if (assigned().length === 0) {
            <span class="empty-text">No tags.</span>
          }
        </div>

        @if (canEdit) {
          <fieldset class="add-row rq-fieldset" data-testid="tag-add-form" [formGroup]="addForm"><legend class="rq-visually-hidden">Add tag</legend>
            <input pInputText formControlName="category" [attr.list]="'tagCategories-' + entityId"
                   placeholder="category (optional)" aria-label="Tag category"
                   data-testid="tag-category-input" class="cat-input" />
            <datalist [id]="'tagCategories-' + entityId">
              @for (c of categories(); track c) { <option [value]="c"></option> }
            </datalist>

            <input pInputText formControlName="value" [attr.list]="'tagValues-' + entityId"
                   placeholder="value" aria-label="Tag value"
                   data-testid="tag-value-input" class="val-input"
                   [attr.aria-invalid]="valueErr.message() ? 'true' : null"
                   [attr.aria-describedby]="valueErr.message() ? ('tag-value-error-' + entityId) : null"
                   (keyup.enter)="addTag()" />
            <datalist [id]="'tagValues-' + entityId">
              @for (v of valuesForCategory(); track v) { <option [value]="v"></option> }
            </datalist>

            <p-button label="Add Tag" icon="pi pi-plus" size="small"
                      data-testid="tag-add" (onClick)="addTag()" />
            <app-inline-error #valueErr [control]="addForm.controls.value"
                              [id]="'tag-value-error-' + entityId" [submitted]="submitted()"
                              [overrides]="{ required: 'Value is required.' }"
                              testid="tag-value-error" />
          </fieldset>
        }
      </div>
    }
  `,
  styles: [`
    .tags-section { margin-top: 1.5rem; }
    .section-header { margin-bottom: 0.5rem; }
    .section-header h3 { margin: 0; }
    .chips { display: flex; flex-wrap: wrap; gap: 0.4rem; align-items: center; }
    .add-row { display: flex; align-items: center; gap: 0.5rem; margin-top: 0.75rem; flex-wrap: wrap; }
    .cat-input, .val-input { max-width: 200px; }
    .empty-text { color: var(--p-text-secondary-color); font-style: italic; }
    .add-row .rq-field-error { flex-basis: 100%; margin: 0; }
  `]
})
export class TagSelectorComponent implements OnChanges {
  @Input() projectName = '';
  @Input() entityType = '';
  @Input() entityId: number | null = null;
  @Input() canEdit = false;

  private _assigned = signal<TagDto[]>([]);
  assigned = this._assigned.asReadonly();
  private _available = signal<TagDto[]>([]);
  available = this._available.asReadonly();
  private _categories = signal<string[]>([]);
  categories = this._categories.asReadonly();
  private _typedCategories = signal<TagCategoryDto[]>([]);
  // Non-blocking inline warning when the supplemental tag load fails, instead of
  // the previous silent swallow (issue #131).
  private _loadError = signal<string | null>(null);
  loadError = this._loadError.asReadonly();
  private _actionError = signal<string | null>(null);
  actionError = this._actionError.asReadonly();

  readonly addForm = new FormGroup({
    category: new FormControl('', { nonNullable: true }),
    value: new FormControl('', { nonNullable: true, validators: [notBlank()] }),
  });
  protected readonly submitted = signal(false);

  constructor(private tagService: TagService, private messageService: MessageService) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (this.entityId != null && (changes['entityId'] || changes['entityType'] || changes['projectName'])) {
      void this.load();
    }
  }

  label(tag: TagDto): string {
    return tagLabel(tag);
  }

  /**
   * Suggested values for the typed category: its controlled value list when one is defined,
   * otherwise the distinct values already used under that category name.
   */
  valuesForCategory(): string[] {
    const cat = this.addForm.controls.category.value.trim();
    const typed = this.categoryFor(cat);
    if (typed && typed.values.length > 0) {
      return typed.values;
    }
    const values = this._available()
      .filter(t => (cat ? t.category === cat : true))
      .map(t => t.value)
      .filter((v): v is string => !!v);
    return Array.from(new Set(values));
  }

  /** The typed category rules for a category name, or null. */
  private categoryFor(name: string | null): TagCategoryDto | null {
    if (!name) return null;
    return this._typedCategories().find(c => c.name === name) ?? null;
  }

  /** Chip colour: the tag's own colour, else its category's fallback colour, else null. */
  chipColor(tag: TagDto): string | null {
    return tag.color ?? this.categoryFor(tag.category)?.color ?? null;
  }

  /** Re-run the tag load; wired to the inline warning's (retry) output. */
  reload(): void {
    void this.load();
  }

  private async load(): Promise<void> {
    if (this.entityId == null) return;
    try {
      const [assigned, available, categories, typedCategories] = await Promise.all([
        this.tagService.getTagsOnEntity(this.entityType, this.entityId),
        this.tagService.getTagsForProject(this.projectName),
        this.tagService.getCategories(this.projectName),
        this.tagService.getTypedCategories(this.projectName),
      ]);
      this._assigned.set(assigned);
      this._available.set(available);
      this._categories.set(categories);
      this._typedCategories.set(typedCategories);
      this._loadError.set(null);
      this._actionError.set(null);
    } catch {
      // Tags are supplemental, so a failure must not block the editor — but it is
      // no longer swallowed silently: surface a non-blocking inline warning so the
      // lost capability is visible and retryable (issue #131).
      this._loadError.set('Tags could not be loaded.');
    }
  }

  async addTag(): Promise<void> {
    this.submitted.set(true);
    if (this.addForm.invalid || this.entityId == null) {
      this.addForm.controls.value.markAsTouched();
      return;
    }
    const value = this.addForm.controls.value.value.trim();
    const category = this.addForm.controls.category.value.trim() || null;

    const created = await this.tagService.editTag(this.projectName, category, value);
    if (!created.success || !created.entity) {
      this._actionError.set(created.error ?? 'Failed to create tag.');
      return;
    }
    const assigned = await this.tagService.assignTag(created.entity.id, this.entityType, this.entityId);
    if (assigned.success) {
      this.addForm.reset({ category: '', value: '' });
      this.submitted.set(false);
      await this.load();
    } else {
      this._actionError.set(assigned.error ?? 'Failed to assign tag.');
    }
  }

  async removeTag(tag: TagDto): Promise<void> {
    if (this.entityId == null) return;
    const result = await this.tagService.unassignTag(tag.id, this.entityType, this.entityId);
    if (result.success) {
      await this.load();
    }
  }
}
