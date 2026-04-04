import { Component, OnDestroy, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { FormsModule } from '@angular/forms';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { MessageModule } from 'primeng/message';
import { InputText } from 'primeng/inputtext';
import { SlicePipe } from '@angular/common';
import { GlossaryTermDto } from '../../models/term';
import { TermService } from '../../core/term.service';
import { PermissionService } from '../../core/permission.service';

@Component({
  selector: 'app-term-list',
  standalone: true,
  imports: [FormsModule, TableModule, ButtonModule, MessageModule, InputText, SlicePipe],
  template: `
    <div class="term-list">
      <div class="page-header">
        <h2>Glossary</h2>
        <div class="page-actions">
          @if (canEdit()) {
            <p-button label="New Term" icon="pi pi-plus" (onClick)="onNewTerm()" />
          }
        </div>
      </div>

      @if (errorMessage()) {
        <p-message severity="error" [text]="errorMessage()!" />
      }

      <div class="search-bar">
        <span class="p-input-icon-left">
          <i class="pi pi-search"></i>
          <input pInputText [(ngModel)]="searchText" placeholder="Search terms..."
                 (input)="dt.filterGlobal(searchText(), 'contains')" />
        </span>
      </div>

      <p-table #dt [value]="terms()" [loading]="loading()" [paginator]="true" [rows]="20"
               [rowHover]="true" selectionMode="single" (onRowSelect)="onRowSelect($event)"
               [globalFilterFields]="['name', 'text', 'canonicalTermName', 'createdBy']">
        <ng-template #header>
          <tr>
            <th pSortableColumn="name">Term <p-sortIcon field="name" /></th>
            <th>Definition</th>
            <th pSortableColumn="canonicalTermName">Canonical Term <p-sortIcon field="canonicalTermName" /></th>
            <th pSortableColumn="createdBy">Created By <p-sortIcon field="createdBy" /></th>
          </tr>
        </ng-template>
        <ng-template #body let-t>
          <tr [pSelectableRow]="t">
            <td>{{ t.name }}</td>
            <td class="text-preview">{{ t.text | slice:0:80 }}{{ (t.text?.length ?? 0) > 80 ? '...' : '' }}</td>
            <td>{{ t.canonicalTermName ?? '—' }}</td>
            <td>{{ t.createdBy }}</td>
          </tr>
        </ng-template>
        <ng-template #emptymessage>
          <tr><td colspan="4" class="text-center">No glossary terms found.</td></tr>
        </ng-template>
      </p-table>
    </div>
  `,
  styles: [`
    .page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem; }
    .page-actions { display: flex; gap: 0.5rem; }
    .search-bar { margin-bottom: 1rem; }
    .text-center { text-align: center; }
    .text-preview { max-width: 350px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  `]
})
export class TermListComponent implements OnInit, OnDestroy {
  terms = signal<GlossaryTermDto[]>([]);
  loading = signal(true);
  errorMessage = signal<string | null>(null);
  searchText = signal('');
  canEdit = signal(false);

  private projectName = '';
  private paramSub?: Subscription;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private termService: TermService,
    private permissionService: PermissionService
  ) {}

  ngOnInit(): void {
    this.paramSub = this.route.paramMap.subscribe(async params => {
      const name = params.get('name') ?? '';
      if (name !== this.projectName) {
        this.projectName = name;
        await this.permissionService.loadForProject(name);
        this.canEdit.set(this.permissionService.canEdit('GlossaryTerm'));
        this.loadTerms();
      }
    });
  }

  ngOnDestroy(): void {
    this.paramSub?.unsubscribe();
  }

  async loadTerms(): Promise<void> {
    this.loading.set(true);
    try {
      this.terms.set(await this.termService.listTerms(this.projectName));
    } catch {
      this.errorMessage.set('Failed to load glossary terms.');
    } finally {
      this.loading.set(false);
    }
  }

  onRowSelect(event: { data?: GlossaryTermDto | GlossaryTermDto[] }): void {
    const t = Array.isArray(event.data) ? event.data[0] : event.data;
    if (!t) return;
    this.router.navigate(['/projects', this.projectName, 'terms', t.id]);
  }

  onNewTerm(): void {
    this.router.navigate(['/projects', this.projectName, 'terms', 'new']);
  }
}
