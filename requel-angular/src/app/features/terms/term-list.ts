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
import { GlossaryTermDto } from '../../models/term';
import { TermService } from '../../core/term.service';
import { PermissionService } from '../../core/permission.service';
import { ListPageComponent } from '../../shared/list-page';
import { AppDataTableComponent, DataTableColumn, RowAction } from '../../shared/app-data-table';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-term-list',
  standalone: true,
  imports: [ListPageComponent, AppDataTableComponent, ButtonModule, SubmitErrorComponent, SlicePipe],
  template: `
    <app-list-page title="Terms" [fill]="true" [showSearch]="false">
      <app-submit-error [message]="errorMessage()" testid="term-list-error" [retryable]="true" (retry)="loadTerms()" />

      <app-data-table scrollHeight="flex" [value]="terms()" [columns]="columns" [loading]="loading()"
                      [rowActions]="rowActions" searchPlaceholder="Search terms..."
                      [globalFilterFields]="['name', 'text', 'canonicalTermName', 'createdBy']"
                      testid="term-list" (rowClick)="onRowSelect({ data: $event })"
                      emptyTitle="No glossary terms yet"
                      emptyMessage="Define the shared vocabulary this project relies on."
                      emptyIcon="pi-book" emptyActionLabel="New Term"
                      [showEmptyAction]="canEdit()" (emptyAction)="onNewTerm()">
        <div toolbarActions>
          @if (canEdit()) {
            <p-button label="New Term" icon="pi pi-plus" (onClick)="onNewTerm()" />
          }
        </div>
      </app-data-table>
    </app-list-page>

    <ng-template #textCell let-t>
      <span class="text-preview">{{ t.text | slice:0:80 }}{{ (t.text?.length ?? 0) > 80 ? '...' : '' }}</span>
    </ng-template>
    <ng-template #canonicalCell let-t>{{ t.canonicalTermName ?? '—' }}</ng-template>
  `,
  styles: [`
    /* Fill mode (#221): claim main-content's height so the data-table body
       scrolls between a pinned header and the paginator. */
    :host { display: flex; flex-direction: column; flex: 1; min-height: 0; }
    .text-preview { display: inline-block; max-width: 350px; overflow: hidden;
      text-overflow: ellipsis; white-space: nowrap; vertical-align: bottom; }
  `]
})
export class TermListComponent implements OnInit {
  terms = signal<GlossaryTermDto[]>([]);
  loading = signal(true);
  errorMessage = signal<string | null>(null);
  canEdit = signal(false);

  @ViewChild('textCell', { static: true }) textCell!: TemplateRef<{ $implicit: GlossaryTermDto }>;
  @ViewChild('canonicalCell', { static: true }) canonicalCell!: TemplateRef<{ $implicit: GlossaryTermDto }>;
  columns: DataTableColumn<GlossaryTermDto>[] = [];
  rowActions: RowAction<GlossaryTermDto>[] = [
    { label: 'Open', icon: 'pi pi-eye', command: t => this.onRowSelect({ data: t }) }
  ];

  private projectName = '';
  private readonly destroyRef = inject(DestroyRef);

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private termService: TermService,
    private permissionService: PermissionService
  ) {}

  ngOnInit(): void {
    this.columns = [
      { field: 'name', header: 'Term', sortable: true, link: t => ['/projects', this.projectName, 'terms', t.id] },
      { field: 'text', header: 'Definition', cellTemplate: this.textCell },
      { field: 'canonicalTermName', header: 'Canonical Term', sortable: true, cellTemplate: this.canonicalCell },
      { field: 'createdBy', header: 'Created By', sortable: true }
    ];
    this.route.paramMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(async params => {
      const name = params.get('name') ?? '';
      if (name !== this.projectName) {
        this.projectName = name;
        await this.permissionService.loadForProject(name);
        this.canEdit.set(this.permissionService.canEdit('GlossaryTerm'));
        this.loadTerms();
      }
    });
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
