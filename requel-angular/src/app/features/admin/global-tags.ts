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
import { SubmitErrorComponent } from '../../shared/app-submit-error';
import { MessageService } from 'primeng/api';
import { TagDto } from '../../models/tag';
import { TagService } from '../../core/tag.service';
import { AppDataTableComponent, DataTableColumn } from '../../shared/app-data-table';
import { notBlank } from '../../shared/form-errors';
import { InlineErrorComponent } from '../../shared/app-inline-error';

/**
 * Admin surface for the global (system) tag set: tags with no owning project, shared across
 * all projects. Reached at /global-tags behind the adminGuard; the create/delete commands are
 * additionally gated server-side by SystemAdminUserRole.
 */
@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-global-tags',
  standalone: true,
  imports: [PageHeaderComponent, ReactiveFormsModule, AppDataTableComponent, ButtonModule, InputText, SubmitErrorComponent, InlineErrorComponent],
  template: `
    <div class="global-tags" data-testid="global-tags">
      <div class="page-header"><app-page-header title="Global Tags" /></div>

      <app-submit-error [message]="errorMessage()" testid="global-tags-error" />

      <fieldset class="rq-fieldset" data-testid="global-tag-add-form" [formGroup]="addForm">
        <legend>Add global tag</legend>
        <div class="add-row">
        <input pInputText formControlName="category" placeholder="category (optional)"
               aria-label="Tag category" data-testid="global-tag-category" class="cat-input" />
        <input pInputText formControlName="value" placeholder="value"
               aria-label="Tag value" data-testid="global-tag-value" class="val-input"
               [attr.aria-invalid]="valueErr.message() ? 'true' : null"
               [attr.aria-describedby]="valueErr.message() ? 'global-tag-value-error' : null"
               (keyup.enter)="addTag()" />
        <p-button label="Add Global Tag" icon="pi pi-plus" data-testid="global-tag-add"
                  (onClick)="addTag()" />
        <app-inline-error #valueErr [control]="addForm.controls.value" id="global-tag-value-error"
                          [submitted]="submitted()" [overrides]="{ required: 'Value is required.' }"
                          testid="global-tag-value-error" />
      </div>
      </fieldset>

      <app-data-table [value]="tags()" [columns]="columns" [loading]="loading()"
                      [showToolbar]="false" [rowClickable]="false" testid="global-tag"
                      emptyTitle="No global tags" emptyIcon="pi-tag">
        <ng-template #rowActions let-t>
          <p-button icon="pi pi-trash" severity="danger" [text]="true" size="small"
                    data-testid="global-tag-delete" [ariaLabel]="'Delete tag ' + t.value"
                    (onClick)="deleteTag(t)" />
        </ng-template>
      </app-data-table>
    </div>

    <ng-template #categoryCell let-t>{{ t.category ?? '—' }}</ng-template>
  `,
  styles: [`
    .global-tags { max-width: 800px; }
    .page-header { margin-bottom: 1rem; }
    .add-row { display: flex; align-items: center; gap: 0.5rem; margin-bottom: 1rem; flex-wrap: wrap; }
    .cat-input, .val-input { max-width: 220px; }
    .add-row .rq-field-error { flex-basis: 100%; margin: 0; }
  `]
})
export class GlobalTagsComponent implements OnInit {
  tags = signal<TagDto[]>([]);
  loading = signal(true);
  errorMessage = signal<string | null>(null);

  readonly addForm = new FormGroup({
    category: new FormControl('', { nonNullable: true }),
    value: new FormControl('', { nonNullable: true, validators: [notBlank()] }),
  });
  protected readonly submitted = signal(false);

  @ViewChild('categoryCell', { static: true }) categoryCell!: TemplateRef<{ $implicit: TagDto }>;
  columns: DataTableColumn<TagDto>[] = [];

  constructor(private tagService: TagService, private messageService: MessageService) {}

  ngOnInit(): void {
    this.columns = [
      { field: 'category', header: 'Category', cellTemplate: this.categoryCell },
      { field: 'value', header: 'Value', sortable: true },
      { field: 'createdBy', header: 'Created By', sortable: true }
    ];
    void this.load();
  }

  private async load(): Promise<void> {
    this.loading.set(true);
    try {
      // No projectName => global tags only.
      this.tags.set(await this.tagService.getTagsForProject());
    } catch {
      this.errorMessage.set('Failed to load global tags.');
    } finally {
      this.loading.set(false);
    }
  }

  async addTag(): Promise<void> {
    this.submitted.set(true);
    if (this.addForm.invalid) {
      this.addForm.controls.value.markAsTouched();
      return;
    }
    const value = this.addForm.controls.value.value.trim();
    const category = this.addForm.controls.category.value.trim() || null;
    const result = await this.tagService.editTag(null, category, value);
    if (result.success) {
      this.messageService.add({ severity: 'success', summary: 'Global tag added', life: 3000 });
      this.addForm.reset({ category: '', value: '' });
      this.submitted.set(false);
      await this.load();
    } else {
      this.errorMessage.set(result.error ?? 'Failed to add global tag.');
    }
  }

  async deleteTag(tag: TagDto): Promise<void> {
    const result = await this.tagService.deleteTag(tag.id);
    if (result.success) {
      await this.load();
    } else {
      this.errorMessage.set(result.error ?? 'Failed to delete global tag.');
    }
  }
}
