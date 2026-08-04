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
import { NgTemplateOutlet } from '@angular/common';
import {
  Component, ContentChild, EventEmitter, Input, Output, TemplateRef, ViewChild
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MenuItem } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { InputText } from 'primeng/inputtext';
import { Menu, MenuModule } from 'primeng/menu';
import { Table, TableModule } from 'primeng/table';
import { EmptyStateComponent } from './empty-state';

/**
 * Column descriptor for {@link AppDataTableComponent}. A column renders
 * `row[field]` as text by default; supply a {@link cellTemplate} for custom
 * content (tag chips, truncated previews, an `app-tag` status). Mark
 * {@link sortable} to add a PrimeNG sortable header.
 */
export interface DataTableColumn<T = unknown> {
  /** Property path on the row used for the value, sorting, and global filter. */
  field: string;
  /** Column header text. */
  header: string;
  /** Adds a sortable header when true. */
  sortable?: boolean;
  /** Custom cell renderer; receives the row as `$implicit`. */
  cellTemplate?: TemplateRef<{ $implicit: T }>;
  /** Optional class applied to the `<td>` (e.g. a width/truncation helper). */
  class?: string;
}

/**
 * Declarative row action for the `⋯` menu. Passing a `rowActions` array
 * **replaces** the default Open/Edit/Delete set (replace, never merge — a page
 * either takes the default or fully owns its actions). For a fully custom menu,
 * project a `<ng-template #rowActions let-row>` instead.
 */
export interface RowAction<T = unknown> {
  label: string;
  icon?: string;
  command: (row: T) => void;
  /** When present and false for a row, the item is omitted for that row. */
  visible?: (row: T) => boolean;
  /** When present and true for a row, the item renders disabled. */
  disabled?: (row: T) => boolean;
}

/**
 * Shared data-table pattern (issue #157, N4). A standalone wrapper over PrimeNG
 * `Table` so list surfaces share one toolbar/sort/paginate/row-action shape and
 * can be reused outside the list-page shell (dialogs, editor sub-panels).
 *
 * Search ownership: the table owns its search box (issue #157). On a full list
 * page, host it inside `app-list-page` with `[showSearch]="false"` so there is a
 * single search input driving the table's global filter.
 *
 * Row actions (a11y #136/#137): the trailing `⋯` opens a PrimeNG popup menu of
 * real, labelled buttons. Precedence is projected template > `rowActions` input
 * > built-in Open/Edit/Delete. Whole-row click also emits `rowClick` as a
 * convenience affordance; the menu is the keyboard-accessible path.
 *
 * States (#131): empty rows fall through to `app-empty-state` (configurable via
 * the `empty*` inputs) unless an `[empty]` template is projected.
 */
@Component({
  selector: 'app-data-table',
  standalone: true,
  imports: [
    TableModule, MenuModule, ButtonModule, InputText, FormsModule,
    NgTemplateOutlet, EmptyStateComponent
  ],
  template: `
    @if (showToolbar) {
      <div class="dt-toolbar">
        @if (title) { <h2 class="rq-section-title dt-title">{{ title }}</h2> }
        <div class="dt-toolbar-right">
          <span class="p-input-icon-left dt-search">
            <i class="pi pi-search"></i>
            <input pInputText type="text" [(ngModel)]="searchText"
                   [placeholder]="searchPlaceholder" aria-label="Search"
                   data-testid="data-table-search"
                   (input)="onSearch($any($event.target).value)" />
          </span>
          <ng-content select="[toolbarActions]" />
        </div>
      </div>
    }

    <p-table #dt [value]="value" [loading]="loading" [rowHover]="true"
             [paginator]="paginator" [rows]="rows"
             [dataKey]="dataKey" [sortField]="sortField" [sortOrder]="sortOrder"
             [selectionMode]="null"
             [(selection)]="selection" (selectionChange)="onSelectionChange($event)"
             [globalFilterFields]="filterFields"
             [showCurrentPageReport]="false"
             [paginatorPosition]="'bottom'"
             styleClass="rq-data-table">
      <ng-template #header>
        <tr>
          @if (selectable) {
            <th class="dt-select-col"><p-tableHeaderCheckbox /></th>
          }
          @for (col of columns; track col.field) {
            @if (col.sortable) {
              <th [pSortableColumn]="col.field" [class]="col.class ?? ''">
                {{ col.header }} <p-sortIcon [field]="col.field" />
              </th>
            } @else {
              <th [class]="col.class ?? ''">{{ col.header }}</th>
            }
          }
          @if (hasRowActions) { <th class="dt-actions-col"></th> }
        </tr>
      </ng-template>

      <ng-template #body let-row>
        <tr class="dt-row" [class.dt-row--clickable]="rowClickable"
            [attr.data-testid]="rowTestid" (click)="rowClickable && rowClick.emit(row)">
          @if (selectable) {
            <td class="dt-select-col" (click)="$event.stopPropagation()">
              <p-tableCheckbox [value]="row" />
            </td>
          }
          @for (col of columns; track col.field) {
            <td [class]="col.class ?? ''">
              @if (col.cellTemplate) {
                <ng-container [ngTemplateOutlet]="col.cellTemplate"
                              [ngTemplateOutletContext]="{ $implicit: row }" />
              } @else {
                {{ getValue(row, col.field) }}
              }
            </td>
          }
          @if (hasRowActions) {
            <td class="dt-actions-col" (click)="$event.stopPropagation()">
              @if (rowActionsTemplate) {
                <ng-container [ngTemplateOutlet]="rowActionsTemplate"
                              [ngTemplateOutletContext]="{ $implicit: row }" />
              } @else {
                <p-button icon="pi pi-ellipsis-v" [text]="true" [rounded]="true"
                          severity="secondary" size="small"
                          [attr.aria-label]="actionsAriaLabel"
                          data-testid="data-table-row-actions"
                          (onClick)="openRowMenu(row, $event, rowMenu)" />
              }
            </td>
          }
        </tr>
      </ng-template>

      <ng-template #emptymessage>
        <tr>
          <td [attr.colspan]="totalColumns">
            @if (emptyTemplate) {
              <ng-container [ngTemplateOutlet]="emptyTemplate" />
            } @else {
              <app-empty-state [title]="emptyTitle" [message]="emptyMessage"
                               [icon]="emptyIcon" [actionLabel]="emptyActionLabel"
                               [actionIcon]="emptyActionIcon" [showAction]="showEmptyAction"
                               [testid]="testid + '-empty'" (action)="emptyAction.emit()" />
            }
          </td>
        </tr>
      </ng-template>
    </p-table>

    <p-menu #rowMenu [popup]="true" [model]="menuModel" appendTo="body" />
  `,
  styles: [`
    :host { display: block; }
    .dt-toolbar {
      display: flex; justify-content: space-between; align-items: center;
      gap: var(--rq-space-4); margin-bottom: var(--rq-space-4); flex-wrap: wrap;
    }
    .dt-title { margin: 0; }
    .dt-toolbar-right { display: flex; align-items: center; gap: var(--rq-space-2); }
    .dt-search { display: inline-flex; align-items: center; }
    .dt-row--clickable { cursor: pointer; }
    .dt-select-col { width: 3rem; text-align: center; }
    .dt-actions-col { width: 3rem; text-align: right; }
  `]
})
export class AppDataTableComponent<T = Record<string, unknown>> {
  /** Rows to render (loaded client-side; search/sort/paginate are client-side). */
  @Input() value: T[] = [];
  /** Column configuration. */
  @Input() columns: DataTableColumn<T>[] = [];
  /** Drives the PrimeNG loading overlay. */
  @Input() loading = false;
  /** Show the built-in paginator. */
  @Input() paginator = true;
  /** Page size. */
  @Input() rows = 20;
  /** Optional initial sort column. */
  @Input() sortField?: string;
  /** Initial sort direction (1 = ascending, -1 = descending). */
  @Input() sortOrder = 1;
  /** Render the leading checkbox multi-select column. */
  @Input() selectable = false;
  /** Whether whole-row click opens (emits rowClick) and shows a pointer cursor. */
  @Input() rowClickable = true;
  /** Row identity key, required for stable selection. */
  @Input() dataKey = 'id';
  /** Show the internal toolbar (title + search + [toolbarActions] slot). */
  @Input() showToolbar = true;
  /** Toolbar title. */
  @Input() title = '';
  /** Search placeholder. */
  @Input() searchPlaceholder = 'Search...';
  /** Fields the search box filters on; defaults to every column's field. */
  @Input() globalFilterFields?: string[];
  /** Accessible name for each row's `⋯` trigger. */
  @Input() actionsAriaLabel = 'Row actions';
  /** data-testid stem for the wrapper/empty state. */
  @Input() testid = 'data-table';
  /** Optional data-testid applied to every body `<tr>` (for e2e row targeting). */
  @Input() rowTestid?: string;

  /** Whether to render the built-in Open/Edit/Delete menu when no override is given. */
  @Input() defaultActions = true;
  /** Gate the built-in Edit item. */
  @Input() canEdit = true;
  /** Gate the built-in Delete item. */
  @Input() canDelete = true;
  /** Declarative row actions; when set, replaces the built-in menu. */
  @Input() rowActions?: RowAction<T>[];

  /** Empty-state passthroughs (used when no `[empty]` template is projected). */
  @Input() emptyTitle = 'Nothing here yet';
  @Input() emptyMessage = '';
  @Input() emptyIcon = '';
  @Input() emptyActionLabel = '';
  @Input() emptyActionIcon = 'pi pi-plus';
  @Input() showEmptyAction = false;

  /** Whole-row open affordance. */
  @Output() rowClick = new EventEmitter<T>();
  /** Emitted as the checkbox selection changes. */
  @Output() selectionChange = new EventEmitter<T[]>();
  /** Built-in menu outputs (used only when no `rowActions`/template override). */
  @Output() open = new EventEmitter<T>();
  @Output() edit = new EventEmitter<T>();
  @Output() delete = new EventEmitter<T>();
  /** Empty-state CTA. */
  @Output() emptyAction = new EventEmitter<void>();

  /** Full row-action override; when present it replaces the `⋯` menu entirely. */
  @ContentChild('rowActions') rowActionsTemplate?: TemplateRef<{ $implicit: T }>;
  /** Optional empty-state override. */
  @ContentChild('empty') emptyTemplate?: TemplateRef<unknown>;

  selection: T[] = [];
  searchText = '';
  menuModel: MenuItem[] = [];

  /** The PrimeNG table instance, used to drive the global filter. */
  @ViewChild('dt') private dt?: Table;

  /** Fields used for the global filter (explicit list or every column field). */
  get filterFields(): string[] {
    return this.globalFilterFields ?? this.columns.map(c => c.field);
  }

  /** True when a `⋯` column should be shown at all. */
  get hasRowActions(): boolean {
    return !!this.rowActionsTemplate || !!this.rowActions || this.defaultActions;
  }

  /** Total rendered columns, for the empty-row `colspan`. */
  get totalColumns(): number {
    return this.columns.length + (this.selectable ? 1 : 0) + (this.hasRowActions ? 1 : 0);
  }

  /** Read a possibly dotted field path off a row for default text cells. */
  getValue(row: T, field: string): unknown {
    return field.split('.').reduce<unknown>(
      (acc, key) => (acc == null ? acc : (acc as Record<string, unknown>)[key]),
      row
    );
  }

  onSearch(value: string): void {
    this.dt?.filterGlobal(value, 'contains');
  }

  onSelectionChange(selection: T[]): void {
    this.selection = selection;
    this.selectionChange.emit(selection);
  }

  /** Build the row menu (respecting the input/override precedence) and open it. */
  openRowMenu(row: T, event: Event, menu: Menu): void {
    this.menuModel = this.buildMenu(row);
    menu.toggle(event);
  }

  /** Menu items for a row: `rowActions` input when provided, else the defaults. */
  private buildMenu(row: T): MenuItem[] {
    if (this.rowActions) {
      return this.rowActions
        .filter(a => (a.visible ? a.visible(row) : true))
        .map(a => ({
          label: a.label,
          icon: a.icon,
          disabled: a.disabled ? a.disabled(row) : false,
          command: () => a.command(row)
        }));
    }
    const items: MenuItem[] = [
      { label: 'Open', icon: 'pi pi-eye', command: () => this.open.emit(row) }
    ];
    if (this.canEdit) {
      items.push({ label: 'Edit', icon: 'pi pi-pencil', command: () => this.edit.emit(row) });
    }
    if (this.canDelete) {
      items.push({ label: 'Delete', icon: 'pi pi-trash', command: () => this.delete.emit(row) });
    }
    return items;
  }
}
