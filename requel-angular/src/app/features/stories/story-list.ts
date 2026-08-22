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
import { Component, OnDestroy, OnInit, TemplateRef, ViewChild, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { ButtonModule } from 'primeng/button';
import { SubmitErrorComponent } from '../../shared/app-submit-error';
import { SlicePipe } from '@angular/common';
import { StoryDto } from '../../models/story';
import { StoryService } from '../../core/story.service';
import { PermissionService } from '../../core/permission.service';
import { ListPageComponent } from '../../shared/list-page';
import { AppDataTableComponent, DataTableColumn, RowAction } from '../../shared/app-data-table';

@Component({
  selector: 'app-story-list',
  standalone: true,
  imports: [ListPageComponent, AppDataTableComponent, ButtonModule, SubmitErrorComponent, SlicePipe],
  template: `
    <app-list-page title="Stories" [eyebrow]="projectContext()" [showSearch]="false">
      <app-submit-error [message]="errorMessage()" testid="story-list-error" [retryable]="true" (retry)="loadStories()" />

      <app-data-table [value]="stories()" [columns]="columns" [loading]="loading()"
                      [rowActions]="rowActions" searchPlaceholder="Search stories..."
                      [globalFilterFields]="['name', 'text', 'storyType', 'createdBy']"
                      testid="story-list" (rowClick)="onRowSelect({ data: $event })"
                      emptyTitle="No stories yet"
                      emptyMessage="Capture the user stories this project needs to deliver."
                      emptyIcon="pi-book" emptyActionLabel="New Story"
                      [showEmptyAction]="canEdit()" (emptyAction)="onNewStory()">
        <div toolbarActions>
          @if (canEdit()) {
            <p-button label="New Story" icon="pi pi-plus" (onClick)="onNewStory()" />
          }
        </div>
      </app-data-table>
    </app-list-page>

    <ng-template #textCell let-s>
      <span class="text-preview">{{ s.text | slice:0:80 }}{{ s.text?.length > 80 ? '...' : '' }}</span>
    </ng-template>
  `,
  styles: [`
    .text-preview { display: inline-block; max-width: 400px; overflow: hidden;
      text-overflow: ellipsis; white-space: nowrap; vertical-align: bottom; }
  `]
})
export class StoryListComponent implements OnInit, OnDestroy {
  stories = signal<StoryDto[]>([]);
  loading = signal(true);
  errorMessage = signal<string | null>(null);
  canEdit = signal(false);
  /** Project-name context shown as the page eyebrow (issue #127). */
  projectContext = signal('');

  @ViewChild('textCell', { static: true }) textCell!: TemplateRef<{ $implicit: StoryDto }>;
  columns: DataTableColumn<StoryDto>[] = [];
  rowActions: RowAction<StoryDto>[] = [
    { label: 'Open', icon: 'pi pi-eye', command: s => this.onRowSelect({ data: s }) }
  ];

  private projectName = '';
  private paramSub?: Subscription;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private storyService: StoryService,
    private permissionService: PermissionService
  ) {}

  ngOnInit(): void {
    this.columns = [
      { field: 'name', header: 'Name', sortable: true },
      { field: 'storyType', header: 'Type', sortable: true },
      { field: 'text', header: 'Text', cellTemplate: this.textCell },
      { field: 'createdBy', header: 'Created By', sortable: true }
    ];
    this.paramSub = this.route.paramMap.subscribe(async params => {
      const name = params.get('name') ?? '';
      if (name !== this.projectName) {
        this.projectName = name;
        this.projectContext.set(name);
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
