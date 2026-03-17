import { Component, OnDestroy, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { FormsModule } from '@angular/forms';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { MessageModule } from 'primeng/message';
import { InputText } from 'primeng/inputtext';
import { SlicePipe } from '@angular/common';
import { GoalDto } from '../../models/goal';
import { GoalService } from '../../core/goal.service';
import { PermissionService } from '../../core/permission.service';

@Component({
  selector: 'app-goal-list',
  standalone: true,
  imports: [FormsModule, TableModule, ButtonModule, MessageModule, InputText, SlicePipe],
  template: `
    <div class="goal-list">
      <div class="page-header">
        <h2>Goals</h2>
        <div class="page-actions">
          @if (canEdit()) {
            <p-button label="New Goal" icon="pi pi-plus" (onClick)="onNewGoal()" />
          }
        </div>
      </div>

      @if (errorMessage()) {
        <p-message severity="error" [text]="errorMessage()!" />
      }

      <div class="search-bar">
        <span class="p-input-icon-left">
          <i class="pi pi-search"></i>
          <input pInputText [(ngModel)]="searchText" placeholder="Search goals..."
                 (input)="dt.filterGlobal(searchText(), 'contains')" />
        </span>
      </div>

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
export class GoalListComponent implements OnInit, OnDestroy {
  goals = signal<GoalDto[]>([]);
  loading = signal(true);
  errorMessage = signal<string | null>(null);
  searchText = signal('');
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
    this.paramSub = this.route.paramMap.subscribe(params => {
      const name = params.get('name') ?? '';
      if (name !== this.projectName) {
        this.projectName = name;
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
