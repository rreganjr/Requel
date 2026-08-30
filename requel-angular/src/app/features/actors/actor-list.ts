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
import { Component, OnInit, TemplateRef, ViewChild, signal, ChangeDetectionStrategy, inject, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { SubmitErrorComponent } from '../../shared/app-submit-error';
import { SlicePipe } from '@angular/common';
import { ActorDto } from '../../models/actor';
import { ActorService } from '../../core/actor.service';
import { PermissionService } from '../../core/permission.service';
import { ListPageComponent } from '../../shared/list-page';
import { AppDataTableComponent, DataTableColumn, RowAction } from '../../shared/app-data-table';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-actor-list',
  standalone: true,
  imports: [ListPageComponent, AppDataTableComponent, ButtonModule, SubmitErrorComponent, SlicePipe],
  template: `
    <app-list-page title="Actors" [fill]="true" [showSearch]="false">
      <app-submit-error [message]="errorMessage()" testid="actor-list-error" [retryable]="true" (retry)="loadActors()" />

      <app-data-table scrollHeight="flex" [value]="actors()" [columns]="columns" [loading]="loading()"
                      [rowActions]="rowActions" searchPlaceholder="Search actors..."
                      [globalFilterFields]="['name', 'text', 'createdBy']"
                      testid="actor-list" (rowClick)="onRowSelect({ data: $event })"
                      emptyTitle="No actors yet"
                      emptyMessage="Add the actors that interact with this system."
                      emptyIcon="pi-user" emptyActionLabel="New Actor"
                      [showEmptyAction]="canEdit()" (emptyAction)="onNewActor()">
        <div toolbarActions>
          @if (canEdit()) {
            <p-button label="New Actor" icon="pi pi-plus" (onClick)="onNewActor()" />
          }
        </div>
      </app-data-table>
    </app-list-page>

    <ng-template #textCell let-a>
      <span class="text-preview">{{ a.text | slice:0:80 }}{{ (a.text?.length ?? 0) > 80 ? '...' : '' }}</span>
    </ng-template>
  `,
  styles: [`
    /* Fill mode (#221): claim main-content's height so the data-table body
       scrolls between a pinned header and the paginator. */
    :host { display: flex; flex-direction: column; flex: 1; min-height: 0; }
    .text-preview { display: inline-block; max-width: 400px; overflow: hidden;
      text-overflow: ellipsis; white-space: nowrap; vertical-align: bottom; }
  `]
})
export class ActorListComponent implements OnInit {
  actors = signal<ActorDto[]>([]);
  loading = signal(true);
  errorMessage = signal<string | null>(null);
  canEdit = signal(false);

  @ViewChild('textCell', { static: true }) textCell!: TemplateRef<{ $implicit: ActorDto }>;
  columns: DataTableColumn<ActorDto>[] = [];
  rowActions: RowAction<ActorDto>[] = [
    { label: 'Open', icon: 'pi pi-eye', command: a => this.onRowSelect({ data: a }) }
  ];

  private projectName = '';
  private readonly destroyRef = inject(DestroyRef);

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private actorService: ActorService,
    private permissionService: PermissionService
  ) {}

  ngOnInit(): void {
    this.columns = [
      { field: 'name', header: 'Name', sortable: true, link: a => ['/projects', this.projectName, 'actors', a.id] },
      { field: 'text', header: 'Description', cellTemplate: this.textCell },
      { field: 'createdBy', header: 'Created By', sortable: true }
    ];
    this.route.paramMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(async params => {
      const name = params.get('name') ?? '';
      if (name !== this.projectName) {
        this.projectName = name;
        await this.permissionService.loadForProject(name);
        this.canEdit.set(this.permissionService.canEdit('Actor'));
        this.loadActors();
      }
    });
  }

  async loadActors(): Promise<void> {
    this.loading.set(true);
    try {
      this.actors.set(await this.actorService.listActors(this.projectName));
    } catch {
      this.errorMessage.set('Failed to load actors.');
    } finally {
      this.loading.set(false);
    }
  }

  onRowSelect(event: { data?: ActorDto | ActorDto[] }): void {
    const a = Array.isArray(event.data) ? event.data[0] : event.data;
    if (!a) return;
    this.router.navigate(['/projects', this.projectName, 'actors', a.id]);
  }

  onNewActor(): void {
    this.router.navigate(['/projects', this.projectName, 'actors', 'new']);
  }
}
