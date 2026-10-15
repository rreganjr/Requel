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
  ChangeDetectionStrategy, Component, ContentChild, ElementRef, EventEmitter,
  Input, Output, TemplateRef, ViewChild, signal
} from '@angular/core';
import { ButtonModule } from 'primeng/button';

/**
 * Shared relationship-section pattern (issue #130, 2.3). Renders one
 * add / list / remove block with a single consistent structure and control
 * style, replacing the markup that was copy-pasted across the artifact editors.
 *
 * A section is: a header (an optional `rq-section-title` gated by {@link showHeading}
 * for wizard reuse, plus one standardized **Add** button), a table of linked rows
 * (column headers from {@link headers}, data cells from a projected
 * `<ng-template #row let-item>`, plus a trailing standardized **remove** button),
 * and consistent empty / unsaved messaging.
 *
 * Boundary (issue #130): the component owns the chrome, focus return, and an
 * aria-live status region. The host editor owns which selector dialog opens (via
 * the `(add)` output), the row-cell markup (the `#row` template), and the
 * add/remove commands. After a successful command the host calls
 * {@link announceAdded} / {@link announceRemoved}, which announce politely and
 * return focus to the Add button — generalising the focus handling that
 * previously lived only in goal-editor.
 */
@Component({
  selector: 'app-relationship-section',
  standalone: true,
  imports: [ButtonModule, NgTemplateOutlet],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="rq-rel-section" [attr.data-testid]="testid">
      <div class="rq-rel-header">
        @if (showHeading) {
          <div class="rq-rel-titles">
            @if (headingLevel === 3) {
              <h3 class="rq-section-title">{{ title }}</h3>
            } @else {
              <h2 class="rq-section-title">{{ title }}</h2>
            }
            @if (description) {
              <p class="rq-rel-description" [attr.data-testid]="testid + '-description'">{{ description }}</p>
            }
          </div>
        }
        @if (canAdd) {
          <p-button #addButton [label]="addLabel" icon="pi pi-plus" size="small"
                    [attr.data-testid]="addTestid" (onClick)="add.emit()" />
        }
      </div>

      @if (!canAdd && unsavedHint) {
        <p class="rq-rel-hint" [attr.data-testid]="testid + '-hint'">{{ unsavedHint }}</p>
      } @else if (!items.length) {
        <p class="rq-rel-empty" [attr.data-testid]="testid + '-empty'">{{ emptyText }}</p>
      } @else {
        <table class="rq-rel-table" [class.rq-rel-fixed]="!!colWidths?.length">
          @if (colWidths?.length) {
            <colgroup>
              @for (w of colWidths; track $index) { <col [style.width]="w || null" /> }
              <col class="rq-rel-actions-col" />
            </colgroup>
          }
          <thead>
            <tr>
              @for (h of headers; track h) { <th scope="col">{{ h }}</th> }
              @if (canRemove) {
                <th scope="col" class="rq-rel-actions-col">
                  <span class="rq-visually-hidden">Actions</span>
                </th>
              }
            </tr>
          </thead>
          <tbody>
            @for (item of items; track trackBy(item)) {
              <tr [attr.data-testid]="rowTestid">
                <ng-container [ngTemplateOutlet]="rowTemplate"
                              [ngTemplateOutletContext]="{ $implicit: item }" />
                @if (canRemove) {
                  <td class="rq-rel-actions-col">
                    <p-button icon="pi pi-trash" [text]="true" [rounded]="true"
                              severity="danger" size="small"
                              [attr.data-testid]="removeTestid"
                              [ariaLabel]="removeAriaLabel(item)"
                              (onClick)="remove.emit(item)" />
                  </td>
                }
              </tr>
            }
          </tbody>
        </table>
      }

      <span class="rq-visually-hidden" role="status" aria-live="polite"
            [attr.data-testid]="testid + '-status'">{{ statusMessage() }}</span>
    </div>
  `,
  styles: [`
    :host { display: block; margin-top: var(--rq-space-6, 1.5rem); }
    .rq-rel-header {
      display: flex; justify-content: space-between; align-items: center;
      gap: var(--rq-space-4, 1rem); margin-bottom: var(--rq-space-2, 0.5rem); flex-wrap: wrap;
    }
    .rq-rel-header .rq-section-title { margin: 0; }
    .rq-rel-titles { display: flex; flex-direction: column; gap: 0.125rem; min-width: 0; }
    .rq-rel-description { margin: 0; font-size: 0.8125rem; font-weight: 400; color: var(--p-text-muted-color, #6b7280); }
    .rq-rel-table { width: 100%; border-collapse: collapse; }
    .rq-rel-table.rq-rel-fixed { table-layout: fixed; }
    .rq-rel-table.rq-rel-fixed td { overflow-wrap: anywhere; }
    .rq-rel-table th, .rq-rel-table td {
      text-align: left; padding: var(--rq-space-2, 0.5rem); vertical-align: middle;
      border-bottom: 1px solid var(--p-content-border-color, #e5e7eb);
    }
    .rq-rel-actions-col { width: 3rem; text-align: right; }
    /* The icon remove button otherwise renders taller than a text row, adding an
       "empty row" gap in editable lists that the read-only (canRemove=false) list
       doesn't have. Zero the cell padding and compact the button so every list
       shares one tight row rhythm. !important beats PrimeNG's p-button-sm sizing. */
    .rq-rel-table td.rq-rel-actions-col { padding-top: 0; padding-bottom: 0; }
    .rq-rel-actions-col ::ng-deep .p-button {
      width: 1.5rem !important; height: 1.5rem !important; padding: 0 !important;
    }
    .rq-rel-hint, .rq-rel-empty { color: var(--p-text-muted-color, #6b7280); margin: 0.5rem 0; }
    .rq-visually-hidden {
      position: absolute !important; width: 1px; height: 1px; padding: 0; margin: -1px;
      overflow: hidden; clip: rect(0 0 0 0); white-space: nowrap; border: 0;
    }
  `]
})
export class RelationshipSectionComponent<T = unknown> {
  /** Section title (rendered as an rq-section-title when {@link showHeading}). */
  @Input() title = '';
  /** Show the heading. Wizard steps pass false (the panel supplies its own h2). */
  @Input() showHeading = true;
  /** Heading level; use 3 for a subsection nested under a page h2. */
  @Input() headingLevel: 2 | 3 = 2;
  /** The linked rows. */
  @Input() items: T[] = [];
  /** Column header labels (the trailing actions header is added automatically). */
  @Input() headers: string[] = [];
  /** Gates the Add button; host passes `canEdit() && entityId != null`. */
  @Input() canAdd = false;
  /** When false, the section is read-only: no actions column, no per-row remove button. */
  @Input() canRemove = true;
  /** Optional CSS widths per data column (parallel to headers; '' = auto). When set, the
   *  table uses a fixed layout and reserves the actions column, so sibling sections whose
   *  columns share these widths line up even when one is read-only. */
  @Input() colWidths?: string[];
  /** Optional muted sub-title under the heading explaining what the list contains. */
  @Input() description?: string;
  /** Add button label. */
  @Input() addLabel = 'Add';
  /** data-testid forwarded to the Add button (preserve existing e2e selectors). */
  @Input() addTestid?: string;
  /** data-testid forwarded to each remove button. */
  @Input() removeTestid?: string;
  /** data-testid applied to each row `<tr>`. */
  @Input() rowTestid?: string;
  /** data-testid stem for the section wrapper, hint, empty, and status region. */
  @Input() testid = 'relationship-section';
  /** Shown when addable but the list is empty. */
  @Input() emptyText = 'Nothing linked yet.';
  /** Shown instead of the list when not addable because the parent is unsaved. */
  @Input() unsavedHint?: string;
  /** Accessible name for each remove button. */
  @Input() removeAriaLabel: (row: T) => string = () => 'Remove';
  /** Row identity for `@for` tracking. */
  @Input() trackBy: (row: T) => unknown = (r) => r;

  /** Add button clicked — the host opens its selector dialog. */
  @Output() add = new EventEmitter<void>();
  /** A row's remove button clicked — the host issues the Remove command. */
  @Output() remove = new EventEmitter<T>();

  /** The host's row-cell template (the leading `<td>`s before the remove cell). */
  @ContentChild('row') rowTemplate?: TemplateRef<{ $implicit: T }>;
  @ViewChild('addButton', { read: ElementRef }) private addButton?: ElementRef<HTMLElement>;

  /** Polite live-region text; a signal so OnPush updates on imperative announce. */
  readonly statusMessage = signal('');

  /** Announce a successful add and return focus to the Add button. */
  announceAdded(name: string): void { this.announce('Added ' + name); }
  /** Announce a successful remove and return focus to the Add button. */
  announceRemoved(name: string): void { this.announce('Removed ' + name); }

  /** Move focus to the Add button (used for cancel paths and after announce). */
  focusAdd(): void {
    this.addButton?.nativeElement.querySelector('button')?.focus();
  }

  private announce(message: string): void {
    this.statusMessage.set(message);
    this.focusAdd();
  }
}
