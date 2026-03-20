import { Component, OnDestroy, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { MessageModule } from 'primeng/message';
import { ScenarioDto } from '../../models/scenario';
import { ScenarioService } from '../../core/scenario.service';
import { PermissionService } from '../../core/permission.service';

@Component({
  selector: 'app-scenario-list',
  standalone: true,
  imports: [ButtonModule, TableModule, MessageModule],
  template: `
    <div class="scenario-list">
      <div class="page-header">
        <h2>Scenarios</h2>
        @if (canEdit()) {
          <p-button label="New Scenario" icon="pi pi-plus" (onClick)="onNew()" />
        }
      </div>

      @if (errorMessage()) {
        <p-message severity="error" [text]="errorMessage()!" />
      }

      <p-table [value]="scenarios()" [loading]="loading()" [rows]="15"
               [paginator]="scenarios().length > 15" [rowHover]="true"
               (onRowSelect)="onSelect($event)" selectionMode="single">
        <ng-template #header>
          <tr>
            <th pSortableColumn="name">Name <p-sortIcon field="name" /></th>
            <th pSortableColumn="scenarioType">Type <p-sortIcon field="scenarioType" /></th>
            <th pSortableColumn="createdBy">Created By <p-sortIcon field="createdBy" /></th>
          </tr>
        </ng-template>
        <ng-template #body let-s>
          <tr [pSelectableRow]="s">
            <td>{{ s.name }}</td>
            <td>{{ s.scenarioType }}</td>
            <td>{{ s.createdBy }}</td>
          </tr>
        </ng-template>
        <ng-template #emptymessage>
          <tr><td colspan="3" style="text-align:center;font-style:italic">No scenarios yet.</td></tr>
        </ng-template>
      </p-table>
    </div>
  `,
  styles: [`
    .page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem; }
  `]
})
export class ScenarioListComponent implements OnInit, OnDestroy {
  scenarios = signal<ScenarioDto[]>([]);
  loading = signal(false);
  errorMessage = signal<string | null>(null);
  canEdit = signal(false);

  projectName = '';
  private paramSub?: Subscription;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private scenarioService: ScenarioService,
    private permissionService: PermissionService
  ) {}

  ngOnInit(): void {
    this.paramSub = this.route.paramMap.subscribe(async params => {
      this.projectName = params.get('name') ?? '';
      await this.permissionService.loadForProject(this.projectName);
      this.canEdit.set(this.permissionService.canEdit('Scenario'));
      this.loadScenarios();
    });
  }

  ngOnDestroy(): void {
    this.paramSub?.unsubscribe();
  }

  private async loadScenarios(): Promise<void> {
    this.loading.set(true);
    try {
      this.scenarios.set(await this.scenarioService.listScenarios(this.projectName));
    } catch {
      this.errorMessage.set('Failed to load scenarios.');
    } finally {
      this.loading.set(false);
    }
  }

  onNew(): void {
    this.router.navigate(['/projects', this.projectName, 'scenarios', 'new']);
  }

  onSelect(event: { data?: ScenarioDto | ScenarioDto[] }): void {
    const s = Array.isArray(event.data) ? event.data[0] : event.data;
    if (s) this.router.navigate(['/projects', this.projectName, 'scenarios', s.id]);
  }
}
