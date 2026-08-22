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
import { SubmitErrorComponent } from '../../shared/app-submit-error';
import { MessageService } from 'primeng/api';
import { TagDto } from '../../models/tag';
import { TagService } from '../../core/tag.service';
import { AppDataTableComponent, DataTableColumn } from '../../shared/app-data-table';

/**
 * Admin surface for the global (system) tag set: tags with no owning project, shared across
 * all projects. Reached at /global-tags behind the adminGuard; the create/delete commands are
 * additionally gated server-side by SystemAdminUserRole.
 */
@Component({
  selector: 'app-global-tags',
  standalone: true,
  imports: [PageHeaderComponent, FormsModule, AppDataTableComponent, ButtonModule, InputText, SubmitErrorComponent],
  template: `
    <div class="global-tags" data-testid="global-tags">
      <div class="page-header"><app-page-header title="Global Tags" /></div>

      <app-submit-error [message]="errorMessage()" testid="global-tags-error" />

      <fieldset class="rq-fieldset" data-testid="global-tag-add-form">
        <legend>Add global tag</legend>
        <div class="add-row">
        <input pInputText [(ngModel)]="newCategory" placeholder="category (optional)"
               aria-label="Tag category" data-testid="global-tag-category" class="cat-input" />
        <input pInputText [(ngModel)]="newValue" placeholder="value"
               aria-label="Tag value" data-testid="global-tag-value" class="val-input"
               (keyup.enter)="addTag()" />
        <p-button label="Add Global Tag" icon="pi pi-plus" data-testid="global-tag-add"
                  (onClick)="addTag()" />
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
  `]
})
export class GlobalTagsComponent implements OnInit {
  tags = signal<TagDto[]>([]);
  loading = signal(true);
  errorMessage = signal<string | null>(null);

  newCategory = '';
  newValue = '';

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
    const value = this.newValue.trim();
    if (!value) return;
    const category = this.newCategory.trim() || null;
    const result = await this.tagService.editTag(null, category, value);
    if (result.success) {
      this.messageService.add({ severity: 'success', summary: 'Global tag added', life: 3000 });
      this.newCategory = '';
      this.newValue = '';
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
