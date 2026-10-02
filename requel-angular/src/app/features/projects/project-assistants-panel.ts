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
import { Component, ChangeDetectionStrategy, OnChanges, computed, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { ToggleSwitchModule } from 'primeng/toggleswitch';
import { ProjectAssistantsService } from '../../core/project-assistants.service';
import { ProjectAssistantDto } from '../../models/project-assistant';
import { ProjectDataHandlingDto } from '../../models/project-data-handling';

/** One data-handling switch (issue #262). */
export interface DataHandlingRow {
  key: string;
  label: string;
  enabled: boolean;
}

/** #263: switch groups the server sends (SwitchableAssistantCatalog). */
const LEXICAL_CHECKS = 'Lexical checks';
const AI_REVIEW = 'AI review';

const REDACTION_LABELS: Record<string, string> = {
  credentials: 'Mask credentials (API keys, tokens, passwords)',
  email: 'Mask email addresses',
  phone: 'Mask phone numbers',
  ssn: 'Mask US social security numbers',
  card: 'Mask payment card numbers',
};

/**
 * The project overview's Assistants panel (issue #268): one switch per lexical check, applied
 * as soon as it is flipped, and a Re-run analysis button. The switches are read-only without
 * Project[Edit]; the button needs Annotation[Edit], since the runs write issues. Issue #262 adds
 * an AI data-handling section: whether project text may go to a remote AI provider, and which
 * kinds of sensitive text are masked before any provider sees it.
 */
@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-project-assistants-panel',
  standalone: true,
  imports: [FormsModule, ButtonModule, ToggleSwitchModule],
  template: `
    <section aria-labelledby="assistants-title" class="ws-panel" data-testid="workspace-assistants">
      <h2 id="assistants-title" class="ws-panel-title">Assistants</h2>
      @if (loadFailed()) {
        <p class="ws-empty" data-testid="assistants-load-error">Couldn't load the assistants.</p>
      } @else if (assistants().length === 0) {
        <p class="ws-empty">No assistants to configure.</p>
      } @else {
        @for (g of groups(); track g.name) {
          @if (groups().length > 1) {
            <h3 class="ws-subtitle" [id]="'assistant-group-' + $index"
                [attr.data-testid]="'assistant-group-' + g.name">{{ g.name }}</h3>
          }
          <ul class="assistant-list"
              [attr.aria-labelledby]="groups().length > 1 ? 'assistant-group-' + $index : null">
            @for (a of g.assistants; track a.assistantId) {
              <li class="assistant-row">
                <p-toggleswitch [inputId]="'assistant-' + a.assistantId" [ngModel]="a.enabled"
                                [disabled]="!canEdit() || busy() === a.assistantId"
                                (ngModelChange)="toggle(a, $event)"
                                [attr.data-testid]="'assistant-toggle-' + a.assistantId" />
                <label [for]="'assistant-' + a.assistantId">{{ a.displayName }}</label>
              </li>
            }
          </ul>
        }
        <p class="ws-hint">
          Switching a check off stops it running in this project. The issues it already raised stay.
        </p>
      }
      @if (dataHandlingRows().length > 0) {
        <h3 class="ws-subtitle" id="data-handling-title">AI data handling</h3>
        <ul class="dh-list" aria-labelledby="data-handling-title" data-testid="data-handling">
          @for (row of dataHandlingRows(); track row.key) {
            <li class="dh-row">
              <p-toggleswitch [inputId]="'dh-' + row.key" [ngModel]="row.enabled"
                              [disabled]="!canEdit() || busy() === row.key"
                              (ngModelChange)="toggleDataHandling(row, $event)"
                              [attr.data-testid]="'data-handling-toggle-' + row.key" />
              <label [for]="'dh-' + row.key">{{ row.label }}</label>
            </li>
          }
        </ul>
        <p class="ws-hint">
          With external providers off, AI reviews of this project fail rather than send its text off
          this server. Masking applies to every AI provider.
        </p>
      }
      @if (canAnalyze()) {
        <p-button label="Re-run analysis" icon="pi pi-refresh" [outlined]="true" size="small"
                  [loading]="analyzing()" [disabled]="!anyEnabled()" (onClick)="rerun()"
                  data-testid="assistants-rerun" />
        @if (!anyEnabled() && assistants().length > 0) {
          <p class="ws-hint" data-testid="assistants-all-off">
            Every check is off. Switch one on to re-run analysis.
          </p>
        }
      }
      <p class="ws-status" role="status" aria-live="polite" data-testid="assistants-status">
        {{ status() }}
      </p>
      @if (error()) {
        <p class="ws-error" role="alert" data-testid="assistants-error">{{ error() }}</p>
      }
    </section>
  `,
  styles: [`
    :host { display: block; }
    .ws-panel {
      height: 100%; box-sizing: border-box;
      padding: var(--rq-card-pad); background: var(--rq-card-bg);
      border: 1px solid var(--rq-card-border); border-radius: var(--rq-card-radius);
      box-shadow: var(--rq-card-shadow);
    }
    .ws-panel-title { margin: 0 0 var(--rq-space-2); font-size: var(--rq-font-size-lg); }
    .ws-subtitle { margin: var(--rq-space-3) 0 var(--rq-space-2); font-size: var(--rq-font-size-md, 1rem); }
    .assistant-list { list-style: none; margin: 0 0 var(--rq-space-2); padding: 0;
      display: flex; flex-direction: column; gap: var(--rq-space-2); }
    .assistant-row, .dh-row { display: flex; align-items: center; gap: var(--rq-space-2); }
    .dh-list { list-style: none; margin: 0 0 var(--rq-space-2); padding: 0;
      display: flex; flex-direction: column; gap: var(--rq-space-2); }
    .ws-empty, .ws-hint { color: var(--p-text-secondary-color); margin: 0 0 var(--rq-space-2); }
    .ws-hint { font-size: var(--rq-font-size-sm); }
    .ws-status { color: var(--p-text-secondary-color); font-size: var(--rq-font-size-sm);
      margin: var(--rq-space-2) 0 0; min-height: 1em; }
    .ws-error { color: var(--p-red-600, #b91c1c); font-size: var(--rq-font-size-sm); margin: var(--rq-space-1) 0 0; }
  `]
})
export class ProjectAssistantsPanelComponent implements OnChanges {
  readonly projectName = input.required<string>();
  /** Project[Edit]: may switch assistants. */
  readonly canEdit = input(false);
  /** Annotation[Edit]: may re-run analysis. */
  readonly canAnalyze = input(false);

  readonly assistants = signal<ProjectAssistantDto[]>([]);
  readonly loadFailed = signal(false);
  readonly busy = signal<string | null>(null);
  readonly analyzing = signal(false);
  readonly status = signal('');
  readonly error = signal<string | null>(null);
  /**
   * #263: the switches by heading, in the order the server lists them. A switch with no group
   * (an older server) shows under "Lexical checks".
   */
  readonly groups = computed<{ name: string; assistants: ProjectAssistantDto[] }[]>(() => {
    const byName = new Map<string, ProjectAssistantDto[]>();
    for (const a of this.assistants()) {
      const name = a.group ?? LEXICAL_CHECKS;
      if (!byName.has(name)) byName.set(name, []);
      byName.get(name)!.push(a);
    }
    return [...byName.entries()].map(([name, assistants]) => ({ name, assistants }));
  });
  /**
   * Re-running with every check off would do nothing the panel shows, so it is disabled. Re-run
   * analysis runs the lexical checks; the AI review switches don't count (#263).
   */
  readonly anyEnabled = computed(() =>
    this.assistants().some(a => a.enabled && (a.group ?? LEXICAL_CHECKS) !== AI_REVIEW));
  /** Issue #262: null until loaded (or when the read fails, which hides the section). */
  readonly dataHandling = signal<ProjectDataHandlingDto | null>(null);
  readonly dataHandlingRows = computed<DataHandlingRow[]>(() => {
    const dh = this.dataHandling();
    if (!dh) return [];
    const rows: DataHandlingRow[] = [
      { key: 'egress.external', label: 'Allow external AI providers', enabled: dh.externalProviderAllowed },
    ];
    for (const [id, on] of Object.entries(dh.redaction ?? {})) {
      rows.push({ key: `redaction.${id}`, label: REDACTION_LABELS[id] ?? `Mask ${id}`, enabled: on });
    }
    return rows;
  });

  constructor(private readonly service: ProjectAssistantsService) {}

  ngOnChanges(): void {
    void this.load();
  }

  async load(): Promise<void> {
    if (!this.projectName()) return;
    try {
      this.assistants.set(await this.service.list(this.projectName()));
      this.loadFailed.set(false);
    } catch {
      this.loadFailed.set(true);
    }
    try {
      this.dataHandling.set(await this.service.dataHandling(this.projectName()));
    } catch {
      this.dataHandling.set(null);
    }
  }

  /** Issue #262: apply a data-handling switch at once; put it back if the server refuses. */
  async toggleDataHandling(row: DataHandlingRow, enabled: boolean): Promise<void> {
    if (!this.canEdit() || enabled === row.enabled) return;
    this.error.set(null);
    this.busy.set(row.key);
    this.setDataHandlingLocal(row.key, enabled);
    try {
      const result = await this.service.setDataHandling(this.projectName(), row.key, enabled);
      if (!result.success) {
        this.setDataHandlingLocal(row.key, !enabled);
        this.error.set(result.error ?? `Couldn't change "${row.label}".`);
      } else {
        this.status.set(`${row.label}: ${enabled ? 'on' : 'off'}.`);
      }
    } catch {
      this.setDataHandlingLocal(row.key, !enabled);
      this.error.set(`Couldn't change "${row.label}".`);
    } finally {
      this.busy.set(null);
    }
  }

  private setDataHandlingLocal(key: string, enabled: boolean): void {
    this.dataHandling.update(dh => {
      if (!dh) return dh;
      if (key === 'egress.external') return { ...dh, externalProviderAllowed: enabled };
      const id = key.replace(/^redaction\./, '');
      return { ...dh, redaction: { ...dh.redaction, [id]: enabled } };
    });
  }

  /** Apply a switch at once; put it back if the server refuses. */
  async toggle(assistant: ProjectAssistantDto, enabled: boolean): Promise<void> {
    if (!this.canEdit() || enabled === assistant.enabled) return;
    this.error.set(null);
    this.busy.set(assistant.assistantId);
    this.replace(assistant.assistantId, enabled);
    try {
      const result = await this.service.setEnabled(this.projectName(), assistant.assistantId, enabled);
      if (!result.success) {
        this.replace(assistant.assistantId, !enabled);
        this.error.set(result.error ?? `Couldn't change ${assistant.displayName}.`);
      } else {
        this.status.set(`${assistant.displayName} is ${enabled ? 'on' : 'off'}.`);
      }
    } catch {
      this.replace(assistant.assistantId, !enabled);
      this.error.set(`Couldn't change ${assistant.displayName}.`);
    } finally {
      this.busy.set(null);
    }
  }

  async rerun(): Promise<void> {
    if (!this.canAnalyze() || this.analyzing() || !this.anyEnabled()) return;
    this.error.set(null);
    this.analyzing.set(true);
    try {
      const result = await this.service.analyzeProject(this.projectName());
      if (result.success) {
        this.status.set('Analysis is running. New findings appear as each item finishes.');
      } else {
        this.error.set(result.error ?? "Couldn't start the analysis.");
      }
    } catch {
      this.error.set("Couldn't start the analysis.");
    } finally {
      this.analyzing.set(false);
    }
  }

  private replace(assistantId: string, enabled: boolean): void {
    this.assistants.update(list => list.map(a => a.assistantId === assistantId ? { ...a, enabled } : a));
  }
}
