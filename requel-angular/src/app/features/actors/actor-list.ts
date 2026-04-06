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
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { MessageModule } from 'primeng/message';
import { SlicePipe } from '@angular/common';
import { ActorDto } from '../../models/actor';
import { ActorService } from '../../core/actor.service';
import { PermissionService } from '../../core/permission.service';
import { ListPageComponent } from '../../shared/list-page';

@Component({
  selector: 'app-actor-list',
  standalone: true,
  imports: [ListPageComponent, TableModule, ButtonModule, MessageModule, SlicePipe],
  template: `
    <app-list-page title="Actors" searchPlaceholder="Search actors..."
                   (search)="dt.filterGlobal($event, 'contains')">
      <ng-container actions>
        @if (canEdit()) {
          <p-button label="New Actor" icon="pi pi-plus" (onClick)="onNewActor()" />
        }
      </ng-container>

      @if (errorMessage()) {
        <p-message severity="error" [text]="errorMessage()!" />
      }

      <p-table #dt [value]="actors()" [loading]="loading()" [paginator]="true" [rows]="20"
               [rowHover]="true" selectionMode="single" (onRowSelect)="onRowSelect($event)"
               [globalFilterFields]="['name', 'text', 'createdBy']">
        <ng-template #header>
          <tr>
            <th pSortableColumn="name">Name <p-sortIcon field="name" /></th>
            <th>Description</th>
            <th pSortableColumn="createdBy">Created By <p-sortIcon field="createdBy" /></th>
          </tr>
        </ng-template>
        <ng-template #body let-a>
          <tr [pSelectableRow]="a">
            <td>{{ a.name }}</td>
            <td class="text-preview">{{ a.text | slice:0:80 }}{{ (a.text?.length ?? 0) > 80 ? '...' : '' }}</td>
            <td>{{ a.createdBy }}</td>
          </tr>
        </ng-template>
        <ng-template #emptymessage>
          <tr><td colspan="3" class="text-center">No actors found.</td></tr>
        </ng-template>
      </p-table>
    </app-list-page>
  `,
  styles: [`
    .text-center { text-align: center; }
    .text-preview { max-width: 400px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  `]
})
export class ActorListComponent implements OnInit, OnDestroy {
  actors = signal<ActorDto[]>([]);
  loading = signal(true);
  errorMessage = signal<string | null>(null);
  canEdit = signal(false);

  private projectName = '';
  private paramSub?: Subscription;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private actorService: ActorService,
    private permissionService: PermissionService
  ) {}

  ngOnInit(): void {
    this.paramSub = this.route.paramMap.subscribe(async params => {
      const name = params.get('name') ?? '';
      if (name !== this.projectName) {
        this.projectName = name;
        await this.permissionService.loadForProject(name);
        this.canEdit.set(this.permissionService.canEdit('Actor'));
        this.loadActors();
      }
    });
  }

  ngOnDestroy(): void {
    this.paramSub?.unsubscribe();
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
