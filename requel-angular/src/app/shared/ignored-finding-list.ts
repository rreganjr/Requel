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
import { ChangeDetectionStrategy, Component, EventEmitter, Input, OnInit, Output, TemplateRef,
  ViewChild, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { ConfirmationService } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { IgnoredFindingDto } from '../models/dictionary';
import { AppDataTableComponent, DataTableColumn } from './app-data-table';

/** The project route segment for each entity type an ignore can be on; Step has no page. */
const ENTITY_ROUTES: Record<string, string> = {
  Goal: 'goals', Story: 'stories', Actor: 'actors', Scenario: 'scenarios',
  UseCase: 'use-cases', GlossaryTerm: 'terms', ReportGenerator: 'reports',
};

/** Readable names for the assistants' finding types. */
const FINDING_TYPES: Record<string, string> = {
  'unknown-word': 'Spelling', 'vague-word': 'Vague word', 'complex-text': 'Complex text',
  'glossary-term': 'Glossary term',
};

/**
 * The assistant findings ignored in a project (issue #320), with per-row remove. Presentational,
 * like DictionaryWordListComponent: it confirms and emits, and the page runs the command.
 */
@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-ignored-finding-list',
  standalone: true,
  imports: [AppDataTableComponent, ButtonModule, ConfirmDialogModule, RouterLink],
  providers: [ConfirmationService],
  template: `
    <app-data-table [value]="findings" [columns]="columns" [loading]="loading"
                    [showToolbar]="false" [rowClickable]="false" [defaultActions]="false"
                    [testid]="testid" [rowTestid]="testid + '-row'"
                    emptyTitle="Nothing ignored" emptyMessage="Findings you resolve with Ignore appear in this list."
                    emptyIcon="pi-eye-slash">
      @if (canRemove) {
        <ng-template #rowActions let-f>
          <p-button icon="pi pi-trash" severity="danger" [text]="true" size="small"
                    [attr.data-testid]="testid + '-remove'" [ariaLabel]="'Stop ignoring ' + (f.subject ?? '')"
                    (onClick)="confirmRemove(f)" />
        </ng-template>
      }
    </app-data-table>

    <ng-template #subjectCell let-f><span [attr.data-testid]="testid + '-subject'">{{ f.subject }}</span></ng-template>
    <ng-template #typeCell let-f>{{ findingTypeLabel(f.findingType) }}</ng-template>
    <ng-template #entityCell let-f>
      @if (entityLink(f); as link) {
        <a [routerLink]="link" [attr.data-testid]="testid + '-entity'">{{ f.entityName ?? (f.entityType + ' ' + f.entityId) }}</a>
      } @else {
        <span [attr.data-testid]="testid + '-entity'">{{ f.entityName ?? (f.entityType + ' ' + f.entityId) }}</span>
      }
    </ng-template>
    <p-confirmDialog />
  `,
})
export class IgnoredFindingListComponent implements OnInit {
  @Input() findings: IgnoredFindingDto[] = [];
  @Input() loading = false;
  @Input() canRemove = false;
  @Input() projectName = '';
  /** data-testid stem for every element this renders. */
  @Input() testid = 'ignored-findings';

  /** The ignore to remove, once the user confirmed. */
  @Output() remove = new EventEmitter<IgnoredFindingDto>();

  @ViewChild('subjectCell', { static: true }) subjectCell!: TemplateRef<{ $implicit: IgnoredFindingDto }>;
  @ViewChild('typeCell', { static: true }) typeCell!: TemplateRef<{ $implicit: IgnoredFindingDto }>;
  @ViewChild('entityCell', { static: true }) entityCell!: TemplateRef<{ $implicit: IgnoredFindingDto }>;
  columns: DataTableColumn<IgnoredFindingDto>[] = [];

  private readonly confirmationService = inject(ConfirmationService);

  ngOnInit(): void {
    this.columns = [
      { field: 'subject', header: 'Ignored', sortable: true, cellTemplate: this.subjectCell },
      { field: 'findingType', header: 'Type', sortable: true, cellTemplate: this.typeCell },
      { field: 'entityName', header: 'On', sortable: true, cellTemplate: this.entityCell },
      { field: 'propertyName', header: 'Property', sortable: true },
      { field: 'createdBy', header: 'By', sortable: true },
    ];
  }

  findingTypeLabel(type: string): string {
    return FINDING_TYPES[type] ?? type;
  }

  /** The entity's page, or null for a type without one (a scenario step). */
  entityLink(f: IgnoredFindingDto): string[] | null {
    const segment = ENTITY_ROUTES[f.entityType];
    return segment ? ['/projects', this.projectName, segment, String(f.entityId)] : null;
  }

  confirmRemove(f: IgnoredFindingDto): void {
    this.confirmationService.confirm({
      message: `Stop ignoring "${f.subject ?? ''}"? It will be raised again on its ${f.entityType.toLowerCase()}.`,
      header: 'Stop Ignoring',
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Stop Ignoring',
      rejectLabel: 'Cancel',
      accept: () => this.remove.emit(f),
    });
  }
}
