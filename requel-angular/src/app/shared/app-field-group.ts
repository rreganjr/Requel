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
import { AfterContentInit, Component, ContentChildren, ElementRef, Input, OnChanges, OnDestroy, QueryList, ChangeDetectionStrategy, inject, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AppFieldComponent } from './app-field';

/** Marks a projected row so the global `.app-field-group` rules can reach it. */
const CELL_CLASS = 'rq-field-cell';

/** Marks rows on the final grid line, which draw no divider. */
const LAST_ROW_CLASS = 'rq-field-cell-last-row';

/**
 * Multi-column layout wrapper for {@link AppFieldComponent} rows (issue #172).
 *
 * `project-editor` and `user-editor` are the two editors on
 * `grid-template-columns: 1fr 1fr`; migrating them to single-column `app-field`
 * rows would turn a dense form into a long scroll. This wrapper keeps them dense
 * without changing anything about `app-field` itself — #158 shipped that primitive
 * one commit ago and `goal-editor` / `story-editor` depend on its current
 * behaviour, so this component is purely additive: a group with no rows, or a
 * row outside a group, behaves exactly as before.
 *
 * It is layout only. It owns no label, no error, no ARIA — each `app-field` keeps
 * its own label/control/error association, so a cell still reads label-left and a
 * screen reader still resolves exactly one label per control.
 *
 * Usage:
 * ```html
 * <app-field-group [columns]="2">
 *   <app-field label="Username" [control]="form.controls.username">
 *     <input pInputText appFieldControl [formControl]="form.controls.username" />
 *   </app-field>
 *   <app-field label="Name" [control]="form.controls.name">
 *     <input pInputText appFieldControl [formControl]="form.controls.name" />
 *   </app-field>
 * </app-field-group>
 * ```
 *
 * Two things live outside this file by necessity:
 *
 * - **The grid and divider rules are global** (`src/styles.scss`, keyed on
 *   `.app-field-group` / `.rq-field-cell`). Projected content carries the *host*
 *   template's encapsulation attribute rather than this component's, so a
 *   component-local rule cannot select it. #126 removed the codebase's
 *   `:host ::ng-deep` blocks for exactly this case and put the styling in
 *   `styles.scss` behind a class hook; this follows that.
 * - **The responsive collapse is a container query**, not a TS breakpoint, so the
 *   group degrades two-column -> one-column -> label-above-control without a
 *   resize listener. `:host` establishes the query container below.
 */
@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-field-group',
  standalone: true,
  template: `
    <div class="app-field-group" [style.--rq-field-group-columns]="columnCount">
      <ng-content />
    </div>
  `,
  styles: [
    `
      /* container-type establishes the container the global @container rule targets. */
      :host {
        display: block;
        container-type: inline-size;
      }
    `,
  ],
})
export class AppFieldGroupComponent implements AfterContentInit, OnChanges, OnDestroy {
  /**
   * Number of columns to lay rows out in. Values below 1, and anything
   * non-numeric, fall back to a single column rather than producing an invalid
   * `repeat()`.
   */
  @Input() columns: number | string = 2;

  /**
   * The projected rows, read as elements so the divider hooks can be stamped on
   * them. Read as `ElementRef` rather than `AppFieldComponent` because nothing
   * here touches a row's inputs — mutating a child's `divider` from a parent
   * content hook is what causes NG0100 under the zoneless TestBed, and hiding the
   * row's own divider through a custom property avoids the whole problem.
   */
  @ContentChildren(AppFieldComponent, { read: ElementRef })
  private cells?: QueryList<ElementRef<HTMLElement>>;

  private readonly destroyRef = inject(DestroyRef);

  /** `columns`, coerced and floored at 1. */
  get columnCount(): number {
    const parsed = Math.trunc(Number(this.columns));
    return Number.isFinite(parsed) && parsed > 0 ? parsed : 1;
  }

  ngAfterContentInit(): void {
    this.markCells();
    // Rows behind @if / @for arrive and leave after content init.
    this.cells?.changes.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => this.markCells());
  }

  ngOnChanges(): void {
    // A `columns` change moves the last grid line, so the divider hooks move too.
    // Runs before ngAfterContentInit on the first pass, when `cells` is still
    // undefined; markCells is a no-op then and the content hook does the work.
    this.markCells();
  }

  ngOnDestroy(): void {
    // The rows belong to the caller's template and can outlive this group, so
    // leave nothing of ours on them — same contract app-field keeps for the ARIA
    // attributes it stamps.
    this.cells?.forEach(ref => {
      ref.nativeElement.classList.remove(CELL_CLASS, LAST_ROW_CLASS);
    });
  }

  /**
   * Tags every projected row, and tags the ones on the final grid line so they
   * draw no divider.
   *
   * Suppressing the last row is what keeps a hairline from appearing under a
   * single cell of a two-cell row: with an odd number of rows the final line
   * holds one cell, and a border there would stop halfway across the group.
   *
   * Known limitation: the row maths uses `columns`, not the *rendered* column
   * count, so once the container query has collapsed the group to one column the
   * final `columns`-worth of rows are all treated as the last line and none of
   * them draws a divider. Correcting that would mean observing the rendered
   * layout (ResizeObserver) purely for a hairline; the collapsed state reads as a
   * plain stack either way, so it is left as is.
   */
  private markCells(): void {
    const cells = this.cells?.toArray() ?? [];
    const count = cells.length;
    if (!count) {
      return;
    }

    const columns = this.columnCount;
    const firstIndexOfLastRow = Math.floor((count - 1) / columns) * columns;

    cells.forEach((ref, index) => {
      const element = ref.nativeElement;
      element.classList.add(CELL_CLASS);
      element.classList.toggle(LAST_ROW_CLASS, index >= firstIndexOfLastRow);
    });
  }
}
