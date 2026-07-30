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
import { Component, OnInit, signal } from '@angular/core';
import { PageHeaderComponent } from '../../shared/page-header';
import { FormsModule } from '@angular/forms';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { InputText } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { MessageService } from 'primeng/api';
import { TagDto } from '../../models/tag';
import { TagService } from '../../core/tag.service';

/**
 * Admin surface for the global (system) tag set: tags with no owning project, shared across
 * all projects. Reached at /global-tags behind the adminGuard; the create/delete commands are
 * additionally gated server-side by SystemAdminUserRole.
 */
@Component({
  selector: 'app-global-tags',
  standalone: true,
  imports: [PageHeaderComponent, FormsModule, TableModule, ButtonModule, InputText, MessageModule],
  template: `
    <div class="global-tags" data-testid="global-tags">
      <div class="page-header"><app-page-header title="Global Tags" /></div>

      @if (errorMessage()) {
        <p-message severity="error" [text]="errorMessage()!" />
      }

      <div class="add-row" data-testid="global-tag-add-form">
        <input pInputText [(ngModel)]="newCategory" placeholder="category (optional)"
               aria-label="Tag category" data-testid="global-tag-category" class="cat-input" />
        <input pInputText [(ngModel)]="newValue" placeholder="value"
               aria-label="Tag value" data-testid="global-tag-value" class="val-input"
               (keyup.enter)="addTag()" />
        <p-button label="Add Global Tag" icon="pi pi-plus" data-testid="global-tag-add"
                  (onClick)="addTag()" />
      </div>

      <p-table [value]="tags()" [loading]="loading()" [paginator]="true" [rows]="20" [rowHover]="true">
        <ng-template #header>
          <tr>
            <th>Category</th>
            <th>Value</th>
            <th>Created By</th>
            <th style="width: 60px"></th>
          </tr>
        </ng-template>
        <ng-template #body let-t>
          <tr data-testid="global-tag-row">
            <td>{{ t.category ?? '—' }}</td>
            <td>{{ t.value }}</td>
            <td>{{ t.createdBy }}</td>
            <td>
              <p-button icon="pi pi-trash" severity="danger" [text]="true" size="small"
                        data-testid="global-tag-delete" (onClick)="deleteTag(t)" />
            </td>
          </tr>
        </ng-template>
        <ng-template #emptymessage>
          <tr><td colspan="4" class="text-center">No global tags.</td></tr>
        </ng-template>
      </p-table>
    </div>
  `,
  styles: [`
    .global-tags { max-width: 800px; }
    .page-header { margin-bottom: 1rem; }
    .add-row { display: flex; align-items: center; gap: 0.5rem; margin-bottom: 1rem; flex-wrap: wrap; }
    .cat-input, .val-input { max-width: 220px; }
    .text-center { text-align: center; }
  `]
})
export class GlobalTagsComponent implements OnInit {
  tags = signal<TagDto[]>([]);
  loading = signal(true);
  errorMessage = signal<string | null>(null);

  newCategory = '';
  newValue = '';

  constructor(private tagService: TagService, private messageService: MessageService) {}

  ngOnInit(): void {
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
