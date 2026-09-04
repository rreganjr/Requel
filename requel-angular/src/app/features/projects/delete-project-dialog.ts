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
import { Component, EventEmitter, Input, Output, signal, computed, ChangeDetectionStrategy } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DialogModule } from 'primeng/dialog';
import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { SubmitErrorComponent } from '../../shared/app-submit-error';
import { ProjectService } from '../../core/project.service';
import { CommandService } from '../../core/command.service';

/** The minimal project identity the delete flow needs: a name to target and a version to lock on. */
export interface DeleteProjectTarget {
  name: string;
  version: number;
}

/**
 * Guarded "Delete project" dialog (#241). Deleting a project is permanent and
 * cascades every child artifact (#240), so the flow defends against accidents:
 *
 *  1. An **export-first backup** toggle (default on). While it is on, the
 *     "Delete permanently" button stays disabled until the user has downloaded
 *     the project's XML export — we await the blob (a real completion signal;
 *     the browser's native download fires no event) before enabling delete.
 *  2. An explicit **"Delete permanently"** button (no type-to-confirm) that
 *     dispatches `DeleteProject { projectName, version }`.
 *
 * On success it notifies the tree (so the sidebar drops the node) and emits
 * `deleted`; the host refreshes its list / routes away. On failure it shows the
 * command error inline and nothing is deleted. Cancel/Esc aborts with no
 * dispatch. Reused from both the project list row and the workspace header.
 */
@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-delete-project-dialog',
  standalone: true,
  imports: [FormsModule, DialogModule, ButtonModule, CheckboxModule, SubmitErrorComponent],
  template: `
    <p-dialog [visible]="visible" (visibleChange)="onVisibleChange($event)"
              [modal]="true" [focusOnShow]="true" [dismissableMask]="!deleting()"
              [closable]="!deleting()" closeAriaLabel="Close"
              [style]="{ width: '32rem' }" appendTo="body"
              header="Delete project" data-testid="delete-project-dialog">
      @if (project) {
        <div class="dpd-body">
          <p class="dpd-warning" data-testid="delete-project-warning">
            <i class="pi pi-exclamation-triangle" aria-hidden="true"></i>
            Deleting <strong>{{ project.name }}</strong> permanently removes the project and
            <strong>all</strong> of its goals, stories, actors, scenarios, use cases, glossary terms,
            reports, and stakeholders. This cannot be undone.
          </p>

          <div class="dpd-backup">
            <p-checkbox [binary]="true" inputId="dpd-export-first"
                        [ngModel]="exportFirst()" (ngModelChange)="onExportFirstChange($event)"
                        [disabled]="deleting()" data-testid="delete-project-export-first" />
            <label for="dpd-export-first">Download a backup (XML export) before deleting</label>
          </div>

          @if (exportFirst()) {
            @if (exported()) {
              <p class="dpd-exported" data-testid="delete-project-exported">
                <i class="pi pi-check-circle" aria-hidden="true"></i>
                Backup downloaded — {{ project.name }}.xml
              </p>
            } @else {
              <p-button label="Download backup" icon="pi pi-download" [outlined]="true"
                        severity="secondary" size="small" [loading]="exporting()"
                        [disabled]="exporting() || deleting()"
                        data-testid="delete-project-download"
                        (onClick)="onExport()" />
            }
          }

          <app-submit-error [message]="errorMessage()" testid="delete-project-error" />

          <div class="dpd-actions">
            <p-button label="Cancel" [text]="true" severity="secondary"
                      [disabled]="deleting()" data-testid="delete-project-cancel"
                      (onClick)="onCancel()" />
            <p-button label="Delete permanently" icon="pi pi-trash" severity="danger"
                      [loading]="deleting()" [disabled]="!canConfirm()"
                      data-testid="delete-project-confirm"
                      (onClick)="onConfirm()" />
          </div>
        </div>
      }
    </p-dialog>
  `,
  styles: [`
    :host { display: block; }
    .dpd-body { display: flex; flex-direction: column; gap: var(--rq-space-4); }
    .dpd-warning {
      margin: 0; display: flex; gap: var(--rq-space-2); align-items: flex-start;
      color: var(--p-text-color);
    }
    .dpd-warning .pi { color: var(--rq-tag-danger-fg, var(--p-red-500)); margin-top: 0.15rem; flex: 0 0 auto; }
    .dpd-backup { display: flex; align-items: center; gap: var(--rq-space-2); }
    .dpd-exported {
      margin: 0; display: flex; align-items: center; gap: var(--rq-space-2);
      color: var(--p-text-secondary-color);
    }
    .dpd-exported .pi { color: var(--p-green-500, green); }
    .dpd-actions { display: flex; justify-content: flex-end; gap: var(--rq-space-2); margin-top: var(--rq-space-2); }
  `]
})
export class DeleteProjectDialogComponent {

  /** The project to delete (name + optimistic-lock version); null closes the flow. */
  @Input() project: DeleteProjectTarget | null = null;

  /** Two-way `[(visible)]`. Setting it true resets the dialog to a fresh state. */
  @Input()
  get visible(): boolean { return this._visible; }
  set visible(value: boolean) {
    const opening = value && !this._visible;
    this._visible = value;
    if (opening) {
      this.reset();
    }
  }
  private _visible = false;

  /** Emits the new visibility so a host can use `[(visible)]`. */
  @Output() visibleChange = new EventEmitter<boolean>();

  /** Emitted once the project has actually been deleted. */
  @Output() deleted = new EventEmitter<void>();

  readonly exportFirst = signal(true);
  readonly exporting = signal(false);
  readonly exported = signal(false);
  readonly deleting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  /** Delete is allowed once any required backup is done and no delete is in flight. */
  readonly canConfirm = computed(() =>
    (!this.exportFirst() || this.exported()) && !this.deleting()
  );

  constructor(
    private readonly projectService: ProjectService,
    private readonly commandService: CommandService,
  ) {}

  onExportFirstChange(value: boolean): void {
    this.exportFirst.set(value);
    // Turning the toggle back on after a prior export must re-require a fresh backup.
    if (value) {
      this.exported.set(false);
    }
  }

  /** Download the project's XML export and await it; that awaited blob is the "backup done" signal. */
  async onExport(): Promise<void> {
    if (!this.project) return;
    this.exporting.set(true);
    this.errorMessage.set(null);
    try {
      const blob = await this.projectService.downloadProjectXml(this.project.name);
      this.saveBlob(blob, `${this.project.name}.xml`);
      this.exported.set(true);
    } catch (err: unknown) {
      this.errorMessage.set(err instanceof Error ? err.message : 'Could not download the backup.');
    } finally {
      this.exporting.set(false);
    }
  }

  async onConfirm(): Promise<void> {
    if (!this.project || !this.canConfirm()) return;
    this.deleting.set(true);
    this.errorMessage.set(null);
    try {
      const result = await this.commandService.execute('DeleteProject', {
        projectName: this.project.name,
        version: this.project.version,
      });
      if (result.success) {
        this.projectService.notifyTreeChanged();
        this.deleted.emit();
        this.close();
      } else {
        this.errorMessage.set(result.error ?? 'Delete failed.');
      }
    } catch (err: unknown) {
      this.errorMessage.set(err instanceof Error ? err.message : 'Delete failed.');
    } finally {
      this.deleting.set(false);
    }
  }

  onCancel(): void {
    if (this.deleting()) return;
    this.close();
  }

  onVisibleChange(value: boolean): void {
    // PrimeNG emits false on Esc / mask / close-icon. Ignore any close while a
    // delete is in flight so the operation can finish deterministically.
    if (!value && this.deleting()) return;
    this._visible = value;
    this.visibleChange.emit(value);
  }

  private close(): void {
    this._visible = false;
    this.visibleChange.emit(false);
  }

  private reset(): void {
    this.exportFirst.set(true);
    this.exporting.set(false);
    this.exported.set(false);
    this.deleting.set(false);
    this.errorMessage.set(null);
  }

  private saveBlob(blob: Blob, filename: string): void {
    const url = URL.createObjectURL(blob);
    try {
      const anchor = document.createElement('a');
      anchor.href = url;
      anchor.download = filename;
      document.body.appendChild(anchor);
      anchor.click();
      anchor.remove();
    } finally {
      URL.revokeObjectURL(url);
    }
  }
}
