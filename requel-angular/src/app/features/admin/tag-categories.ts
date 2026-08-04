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
import { Component, OnInit, TemplateRef, ViewChild, signal } from '@angular/core';
import { PageHeaderComponent } from '../../shared/page-header';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { InputText } from 'primeng/inputtext';
import { CheckboxModule } from 'primeng/checkbox';
import { MessageModule } from 'primeng/message';
import { MessageService } from 'primeng/api';
import { TagCategoryDto } from '../../models/tag';
import { TagService } from '../../core/tag.service';
import { AppDataTableComponent, DataTableColumn } from '../../shared/app-data-table';

/**
 * Admin surface for the global typed-category set (Phase 6): categories with no owning project,
 * shared across all projects. Reached at /tag-categories behind the adminGuard; create/delete are
 * additionally gated server-side by SystemAdminUserRole.
 */
@Component({
  selector: 'app-tag-categories',
  standalone: true,
  imports: [PageHeaderComponent, FormsModule, AppDataTableComponent, ButtonModule, InputText, CheckboxModule, MessageModule],
  template: `
    <div class="tag-categories" data-testid="tag-categories">
      <div class="page-header"><app-page-header title="Global Tag Categories" /></div>

      @if (errorMessage()) {
        <p-message severity="error" [text]="errorMessage()!" />
      }

      <div class="add-row" data-testid="tag-category-add-form">
        <input pInputText [(ngModel)]="newName" placeholder="category name"
               aria-label="Category name" data-testid="tag-category-name" class="name-input" />
        <span class="excl">
          <p-checkbox [(ngModel)]="newExclusive" [binary]="true" inputId="excl" />
          <label for="excl">Exclusive</label>
        </span>
        <input pInputText [(ngModel)]="newAllowedTypes" placeholder="allowed types (comma-sep)"
               aria-label="Allowed entity types" data-testid="tag-category-allowed" class="wide-input" />
        <input pInputText [(ngModel)]="newValues" placeholder="values (comma-sep)"
               aria-label="Controlled values" data-testid="tag-category-values" class="wide-input" />
        <input pInputText [(ngModel)]="newColor" placeholder="color"
               aria-label="Category color" data-testid="tag-category-color" class="color-input" />
        <p-button label="Add Category" icon="pi pi-plus" data-testid="tag-category-add"
                  (onClick)="addCategory()" />
      </div>

      <app-data-table [value]="categories()" [columns]="columns" [loading]="loading()"
                      [showToolbar]="false" [rowClickable]="false" testid="tag-category"
                      emptyTitle="No global categories" emptyIcon="pi-tags">
        <ng-template #rowActions let-c>
          <p-button icon="pi pi-trash" severity="danger" [text]="true" size="small"
                    data-testid="tag-category-delete" [ariaLabel]="'Delete category ' + c.name"
                    (onClick)="deleteCategory(c)" />
        </ng-template>
      </app-data-table>
    </div>

    <ng-template #exclusiveCell let-c>{{ c.exclusive ? 'yes' : '—' }}</ng-template>
    <ng-template #allowedCell let-c>{{ c.allowedEntityTypes.length ? c.allowedEntityTypes.join(', ') : 'any' }}</ng-template>
    <ng-template #valuesCell let-c>{{ c.values.length ? c.values.join(', ') : 'any' }}</ng-template>
    <ng-template #colorCell let-c>{{ c.color ?? '—' }}</ng-template>
  `,
  styles: [`
    .tag-categories { max-width: 960px; }
    .page-header { margin-bottom: 1rem; }
    .add-row { display: flex; align-items: center; gap: 0.5rem; margin-bottom: 1rem; flex-wrap: wrap; }
    .excl { display: inline-flex; align-items: center; gap: 0.35rem; }
    .name-input, .color-input { max-width: 160px; }
    .wide-input { max-width: 220px; }
  `]
})
export class TagCategoriesComponent implements OnInit {
  categories = signal<TagCategoryDto[]>([]);
  loading = signal(true);
  errorMessage = signal<string | null>(null);

  newName = '';
  newExclusive = false;
  newAllowedTypes = '';
  newValues = '';
  newColor = '';

  @ViewChild('exclusiveCell', { static: true }) exclusiveCell!: TemplateRef<{ $implicit: TagCategoryDto }>;
  @ViewChild('allowedCell', { static: true }) allowedCell!: TemplateRef<{ $implicit: TagCategoryDto }>;
  @ViewChild('valuesCell', { static: true }) valuesCell!: TemplateRef<{ $implicit: TagCategoryDto }>;
  @ViewChild('colorCell', { static: true }) colorCell!: TemplateRef<{ $implicit: TagCategoryDto }>;
  columns: DataTableColumn<TagCategoryDto>[] = [];

  constructor(private tagService: TagService, private messageService: MessageService) {}

  ngOnInit(): void {
    this.columns = [
      { field: 'name', header: 'Name', sortable: true },
      { field: 'exclusive', header: 'Exclusive', cellTemplate: this.exclusiveCell },
      { field: 'allowedEntityTypes', header: 'Allowed Types', cellTemplate: this.allowedCell },
      { field: 'values', header: 'Values', cellTemplate: this.valuesCell },
      { field: 'color', header: 'Color', cellTemplate: this.colorCell }
    ];
    void this.load();
  }

  private async load(): Promise<void> {
    this.loading.set(true);
    try {
      // No projectName => global categories only.
      this.categories.set(await this.tagService.getTypedCategories());
    } catch {
      this.errorMessage.set('Failed to load categories.');
    } finally {
      this.loading.set(false);
    }
  }

  async addCategory(): Promise<void> {
    const name = this.newName.trim();
    if (!name) return;
    const result = await this.tagService.editTagCategory({
      projectName: null,
      name,
      exclusive: this.newExclusive,
      color: this.newColor.trim() || null,
      allowedEntityTypes: splitList(this.newAllowedTypes),
      values: splitList(this.newValues),
    });
    if (result.success) {
      this.messageService.add({ severity: 'success', summary: 'Category saved', life: 3000 });
      this.newName = '';
      this.newExclusive = false;
      this.newAllowedTypes = '';
      this.newValues = '';
      this.newColor = '';
      await this.load();
    } else {
      this.errorMessage.set(result.error ?? 'Failed to save category.');
    }
  }

  async deleteCategory(category: TagCategoryDto): Promise<void> {
    const result = await this.tagService.deleteTagCategory(category.id);
    if (result.success) {
      await this.load();
    } else {
      this.errorMessage.set(result.error ?? 'Failed to delete category.');
    }
  }
}

function splitList(raw: string): string[] {
  return raw.split(',').map(s => s.trim()).filter(s => s.length > 0);
}
