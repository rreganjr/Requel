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
import { Component, OnInit, TemplateRef, ViewChild, signal, ChangeDetectionStrategy } from '@angular/core';
import { PageHeaderComponent } from '../../shared/page-header';
import { FormGroup, FormControl, ReactiveFormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { InputText } from 'primeng/inputtext';
import { CheckboxModule } from 'primeng/checkbox';
import { SubmitErrorComponent } from '../../shared/app-submit-error';
import { MessageService } from 'primeng/api';
import { TagCategoryDto } from '../../models/tag';
import { TagService } from '../../core/tag.service';
import { AppDataTableComponent, DataTableColumn } from '../../shared/app-data-table';
import { notBlank } from '../../shared/form-errors';
import { InlineErrorComponent } from '../../shared/app-inline-error';

/**
 * Admin surface for the global typed-category set (Phase 6): categories with no owning project,
 * shared across all projects. Reached at /tag-categories behind the adminGuard; create/delete are
 * additionally gated server-side by SystemAdminUserRole.
 */
@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-tag-categories',
  standalone: true,
  imports: [PageHeaderComponent, ReactiveFormsModule, AppDataTableComponent, ButtonModule, InputText, CheckboxModule, SubmitErrorComponent, InlineErrorComponent],
  template: `
    <div class="tag-categories" data-testid="tag-categories">
      <div class="page-header"><app-page-header title="Global Tag Categories" /></div>

      <app-submit-error [message]="errorMessage()" testid="tag-categories-error" />

      <fieldset class="rq-fieldset" data-testid="tag-category-add-form" [formGroup]="addForm">
        <legend>Add tag category</legend>
        <div class="add-row">
        <input pInputText formControlName="name" placeholder="category name"
               aria-label="Category name" data-testid="tag-category-name" class="name-input"
               [attr.aria-invalid]="nameErr.message() ? 'true' : null"
               [attr.aria-describedby]="nameErr.message() ? 'tag-category-name-error' : null" />
        <span class="excl">
          <p-checkbox formControlName="exclusive" [binary]="true" inputId="excl" />
          <label for="excl">Exclusive</label>
        </span>
        <input pInputText formControlName="allowedTypes" placeholder="allowed types (comma-sep)"
               aria-label="Allowed entity types" data-testid="tag-category-allowed" class="wide-input" />
        <input pInputText formControlName="values" placeholder="values (comma-sep)"
               aria-label="Controlled values" data-testid="tag-category-values" class="wide-input" />
        <input pInputText formControlName="color" placeholder="color"
               aria-label="Category color" data-testid="tag-category-color" class="color-input" />
        <p-button label="Add Category" icon="pi pi-plus" data-testid="tag-category-add"
                  (onClick)="addCategory()" />
        <app-inline-error #nameErr [control]="addForm.controls.name" id="tag-category-name-error"
                          [submitted]="submitted()" [overrides]="{ required: 'Name is required.' }"
                          testid="tag-category-name-error" />
      </div>
      </fieldset>

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
    .add-row .rq-field-error { flex-basis: 100%; margin: 0; }
  `]
})
export class TagCategoriesComponent implements OnInit {
  categories = signal<TagCategoryDto[]>([]);
  loading = signal(true);
  errorMessage = signal<string | null>(null);

  readonly addForm = new FormGroup({
    name: new FormControl('', { nonNullable: true, validators: [notBlank()] }),
    exclusive: new FormControl(false, { nonNullable: true }),
    allowedTypes: new FormControl('', { nonNullable: true }),
    values: new FormControl('', { nonNullable: true }),
    color: new FormControl('', { nonNullable: true }),
  });
  protected readonly submitted = signal(false);

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
    this.submitted.set(true);
    if (this.addForm.invalid) {
      this.addForm.controls.name.markAsTouched();
      return;
    }
    const raw = this.addForm.getRawValue();
    const result = await this.tagService.editTagCategory({
      projectName: null,
      name: raw.name.trim(),
      exclusive: raw.exclusive,
      color: raw.color.trim() || null,
      allowedEntityTypes: splitList(raw.allowedTypes),
      values: splitList(raw.values),
    });
    if (result.success) {
      this.messageService.add({ severity: 'success', summary: 'Category saved', life: 3000 });
      this.addForm.reset({ name: '', exclusive: false, allowedTypes: '', values: '', color: '' });
      this.submitted.set(false);
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
