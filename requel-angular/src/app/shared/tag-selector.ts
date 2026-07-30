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
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { InputText } from 'primeng/inputtext';
import { MessageService } from 'primeng/api';
import { TagCategoryDto, TagDto, tagLabel } from '../models/tag';
import { TagService } from '../core/tag.service';

/**
 * Reusable tag chip/selector for any taggable entity. Shows the entity's assigned tags as
 * removable chips and (when {@code canEdit}) an add row with category-aware autocomplete
 * fed by the project's existing tags/categories. Mirrors the annotations-section pattern.
 */
@Component({
  selector: 'app-tag-selector',
  standalone: true,
  imports: [FormsModule, ButtonModule, InputText],
  template: `
    @if (entityId != null) {
      <div class="tags-section" data-testid="tags-section">
        <div class="section-header"><h3>Tags</h3></div>

        <div class="chips" data-testid="tag-chips">
          @for (t of assigned(); track t.id) {
            <span class="tag-chip" data-testid="tag-chip" [attr.data-tag]="label(t)">
              @if (chipColor(t)) {
                <span class="tag-dot" [style.background]="chipColor(t)"></span>
              }
              <span class="tag-chip-label">{{ label(t) }}</span>
              @if (canEdit) {
                <button type="button" class="chip-x" data-testid="tag-remove"
                        [attr.aria-label]="'Remove tag ' + label(t)" (click)="removeTag(t)"><span aria-hidden="true">×</span></button>
              }
            </span>
          }
          @if (assigned().length === 0) {
            <span class="empty-text">No tags.</span>
          }
        </div>

        @if (canEdit) {
          <div class="add-row" data-testid="tag-add-form">
            <input pInputText [(ngModel)]="newCategory" [attr.list]="'tagCategories-' + entityId"
                   placeholder="category (optional)" aria-label="Tag category"
                   data-testid="tag-category-input" class="cat-input" />
            <datalist [id]="'tagCategories-' + entityId">
              @for (c of categories(); track c) { <option [value]="c"></option> }
            </datalist>

            <input pInputText [(ngModel)]="newValue" [attr.list]="'tagValues-' + entityId"
                   placeholder="value" aria-label="Tag value"
                   data-testid="tag-value-input" class="val-input"
                   (keyup.enter)="addTag()" />
            <datalist [id]="'tagValues-' + entityId">
              @for (v of valuesForCategory(); track v) { <option [value]="v"></option> }
            </datalist>

            <p-button label="Add Tag" icon="pi pi-plus" size="small"
                      data-testid="tag-add" (onClick)="addTag()" />
          </div>
        }
      </div>
    }
  `,
  styles: [`
    .tags-section { margin-top: 1.5rem; }
    .section-header { margin-bottom: 0.5rem; }
    .section-header h3 { margin: 0; }
    .chips { display: flex; flex-wrap: wrap; gap: 0.4rem; align-items: center; }
    .tag-chip { display: inline-flex; align-items: center; gap: 0.3rem; font-size: 0.75rem;
      font-weight: 600; padding: 0.15rem 0.5rem; border-radius: 12px;
      background: var(--p-primary-100, #dbeafe); color: var(--p-primary-700, #1d4ed8); }
    .tag-dot { width: 8px; height: 8px; border-radius: 50%; display: inline-block; }
    .chip-x { border: none; background: transparent; cursor: pointer; font-size: 0.9rem;
      line-height: 1; color: inherit; padding: 0; }
    .add-row { display: flex; align-items: center; gap: 0.5rem; margin-top: 0.75rem; flex-wrap: wrap; }
    .cat-input, .val-input { max-width: 200px; }
    .empty-text { color: var(--p-text-secondary-color); font-style: italic; }
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

  newCategory = '';
  newValue = '';

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
    const cat = this.newCategory.trim();
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
    } catch {
      // Tags are supplemental — ignore load failures silently.
    }
  }

  async addTag(): Promise<void> {
    const value = this.newValue.trim();
    if (!value || this.entityId == null) return;
    const category = this.newCategory.trim() || null;

    const created = await this.tagService.editTag(this.projectName, category, value);
    if (!created.success || !created.entity) {
      this.messageService.add({ severity: 'error', summary: 'Error', detail: created.error ?? 'Failed to create tag.' });
      return;
    }
    const assigned = await this.tagService.assignTag(created.entity.id, this.entityType, this.entityId);
    if (assigned.success) {
      this.newCategory = '';
      this.newValue = '';
      await this.load();
    } else {
      this.messageService.add({ severity: 'error', summary: 'Error', detail: assigned.error ?? 'Failed to assign tag.' });
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
