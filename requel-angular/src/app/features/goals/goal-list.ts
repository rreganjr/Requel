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
import { GoalDto } from '../../models/goal';
import { GoalService } from '../../core/goal.service';
import { PermissionService } from '../../core/permission.service';
import { ListPageComponent } from '../../shared/list-page';

@Component({
  selector: 'app-goal-list',
  standalone: true,
  imports: [ListPageComponent, TableModule, ButtonModule, MessageModule, SlicePipe],
  template: `
    <app-list-page title="Goals" searchPlaceholder="Search goals..."
                   (search)="dt.filterGlobal($event, 'contains')">
      <ng-container actions>
        @if (canEdit()) {
          <p-button label="New Goal" icon="pi pi-plus" (onClick)="onNewGoal()" />
        }
      </ng-container>

      @if (errorMessage()) {
        <p-message severity="error" [text]="errorMessage()!" />
      }

      <p-table #dt [value]="goals()" [loading]="loading()" [paginator]="true" [rows]="20"
               [rowHover]="true" selectionMode="single" (onRowSelect)="onRowSelect($event)"
               [globalFilterFields]="['name', 'text', 'createdBy']">
        <ng-template #header>
          <tr>
            <th pSortableColumn="name">Name <p-sortIcon field="name" /></th>
            <th>Text</th>
            <th pSortableColumn="createdBy">Created By <p-sortIcon field="createdBy" /></th>
          </tr>
        </ng-template>
        <ng-template #body let-g>
          <tr [pSelectableRow]="g">
            <td>{{ g.name }}</td>
            <td class="text-preview">{{ g.text | slice:0:80 }}{{ g.text?.length > 80 ? '...' : '' }}</td>
            <td>{{ g.createdBy }}</td>
          </tr>
        </ng-template>
        <ng-template #emptymessage>
          <tr><td colspan="3" class="text-center">No goals found.</td></tr>
        </ng-template>
      </p-table>
    </app-list-page>
  `,
  styles: [`
    .text-center { text-align: center; }
    .text-preview { max-width: 400px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  `]
})
export class GoalListComponent implements OnInit, OnDestroy {
  goals = signal<GoalDto[]>([]);
  loading = signal(true);
  errorMessage = signal<string | null>(null);
  canEdit = signal(false);

  private projectName = '';
  private paramSub?: Subscription;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private goalService: GoalService,
    private permissionService: PermissionService
  ) {}

  ngOnInit(): void {
    this.paramSub = this.route.paramMap.subscribe(async params => {
      const name = params.get('name') ?? '';
      if (name !== this.projectName) {
        this.projectName = name;
        await this.permissionService.loadForProject(name);
        this.canEdit.set(this.permissionService.canEdit('Goal'));
        this.loadGoals();
      }
    });
  }

  ngOnDestroy(): void {
    this.paramSub?.unsubscribe();
  }

  async loadGoals(): Promise<void> {
    this.loading.set(true);
    try {
      this.goals.set(await this.goalService.listGoals(this.projectName));
    } catch {
      this.errorMessage.set('Failed to load goals.');
    } finally {
      this.loading.set(false);
    }
  }

  onRowSelect(event: { data?: GoalDto | GoalDto[] }): void {
    const g = Array.isArray(event.data) ? event.data[0] : event.data;
    if (!g) return;
    this.router.navigate(['/projects', this.projectName, 'goals', g.id]);
  }

  onNewGoal(): void {
    this.router.navigate(['/projects', this.projectName, 'goals', 'new']);
  }
}
