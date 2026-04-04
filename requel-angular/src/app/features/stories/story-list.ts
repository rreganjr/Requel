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
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { FormsModule } from '@angular/forms';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { MessageModule } from 'primeng/message';
import { InputText } from 'primeng/inputtext';
import { SlicePipe } from '@angular/common';
import { StoryDto } from '../../models/story';
import { StoryService } from '../../core/story.service';
import { PermissionService } from '../../core/permission.service';

@Component({
  selector: 'app-story-list',
  standalone: true,
  imports: [FormsModule, TableModule, ButtonModule, MessageModule, InputText, SlicePipe],
  template: `
    <div class="story-list">
      <div class="page-header">
        <h2>Stories</h2>
        <div class="page-actions">
          @if (canEdit()) {
            <p-button label="New Story" icon="pi pi-plus" (onClick)="onNewStory()" />
          }
        </div>
      </div>

      @if (errorMessage()) {
        <p-message severity="error" [text]="errorMessage()!" />
      }

      <div class="search-bar">
        <span class="p-input-icon-left">
          <i class="pi pi-search"></i>
          <input pInputText [(ngModel)]="searchText" placeholder="Search stories..."
                 (input)="dt.filterGlobal(searchText(), 'contains')" />
        </span>
      </div>

      <p-table #dt [value]="stories()" [loading]="loading()" [paginator]="true" [rows]="20"
               [rowHover]="true" selectionMode="single" (onRowSelect)="onRowSelect($event)"
               [globalFilterFields]="['name', 'text', 'storyType', 'createdBy']">
        <ng-template #header>
          <tr>
            <th pSortableColumn="name">Name <p-sortIcon field="name" /></th>
            <th pSortableColumn="storyType">Type <p-sortIcon field="storyType" /></th>
            <th>Text</th>
            <th pSortableColumn="createdBy">Created By <p-sortIcon field="createdBy" /></th>
          </tr>
        </ng-template>
        <ng-template #body let-s>
          <tr [pSelectableRow]="s">
            <td>{{ s.name }}</td>
            <td>{{ s.storyType }}</td>
            <td class="text-preview">{{ s.text | slice:0:80 }}{{ s.text?.length > 80 ? '...' : '' }}</td>
            <td>{{ s.createdBy }}</td>
          </tr>
        </ng-template>
        <ng-template #emptymessage>
          <tr><td colspan="4" class="text-center">No stories found.</td></tr>
        </ng-template>
      </p-table>
    </div>
  `,
  styles: [`
    .page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem; }
    .page-actions { display: flex; gap: 0.5rem; }
    .search-bar { margin-bottom: 1rem; }
    .text-center { text-align: center; }
    .text-preview { max-width: 400px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  `]
})
export class StoryListComponent implements OnInit, OnDestroy {
  stories = signal<StoryDto[]>([]);
  loading = signal(true);
  errorMessage = signal<string | null>(null);
  searchText = signal('');
  canEdit = signal(false);

  private projectName = '';
  private paramSub?: Subscription;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private storyService: StoryService,
    private permissionService: PermissionService
  ) {}

  ngOnInit(): void {
    this.paramSub = this.route.paramMap.subscribe(async params => {
      const name = params.get('name') ?? '';
      if (name !== this.projectName) {
        this.projectName = name;
        await this.permissionService.loadForProject(name);
        this.canEdit.set(this.permissionService.canEdit('Story'));
        this.loadStories();
      }
    });
  }

  ngOnDestroy(): void {
    this.paramSub?.unsubscribe();
  }

  async loadStories(): Promise<void> {
    this.loading.set(true);
    try {
      this.stories.set(await this.storyService.listStories(this.projectName));
    } catch {
      this.errorMessage.set('Failed to load stories.');
    } finally {
      this.loading.set(false);
    }
  }

  onRowSelect(event: { data?: StoryDto | StoryDto[] }): void {
    const s = Array.isArray(event.data) ? event.data[0] : event.data;
    if (!s) return;
    this.router.navigate(['/projects', this.projectName, 'stories', s.id]);
  }

  onNewStory(): void {
    this.router.navigate(['/projects', this.projectName, 'stories', 'new']);
  }
}
