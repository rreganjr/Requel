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
import { GlossaryTermDto } from '../../models/term';
import { TermService } from '../../core/term.service';
import { PermissionService } from '../../core/permission.service';
import { ListPageComponent } from '../../shared/list-page';

@Component({
  selector: 'app-term-list',
  standalone: true,
  imports: [ListPageComponent, TableModule, ButtonModule, MessageModule, SlicePipe],
  template: `
    <app-list-page title="Terms" searchPlaceholder="Search terms..."
                   (search)="dt.filterGlobal($event, 'contains')">
      <ng-container actions>
        @if (canEdit()) {
          <p-button label="New Term" icon="pi pi-plus" (onClick)="onNewTerm()" />
        }
      </ng-container>

      @if (errorMessage()) {
        <p-message severity="error" [text]="errorMessage()!" />
      }

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
    </app-list-page>
  `,
  styles: [`
    .text-preview { max-width: 350px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  `]
})
export class TermListComponent implements OnInit, OnDestroy {
  terms = signal<GlossaryTermDto[]>([]);
  loading = signal(true);
  errorMessage = signal<string | null>(null);
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
