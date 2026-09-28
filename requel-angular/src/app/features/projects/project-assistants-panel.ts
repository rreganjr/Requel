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
import { Component, ChangeDetectionStrategy, OnChanges, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { ToggleSwitchModule } from 'primeng/toggleswitch';
import { ProjectAssistantsService } from '../../core/project-assistants.service';
import { ProjectAssistantDto } from '../../models/project-assistant';

/**
 * The project overview's Assistants panel (issue #268): one switch per lexical check, applied
 * as soon as it is flipped, and a Re-run analysis button. The switches are read-only without
 * Project[Edit]; the button needs Annotation[Edit], since the runs write issues.
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
        <ul class="assistant-list">
          @for (a of assistants(); track a.assistantId) {
            <li class="assistant-row">
              <p-toggleswitch [inputId]="'assistant-' + a.assistantId" [ngModel]="a.enabled"
                              [disabled]="!canEdit() || busy() === a.assistantId"
                              (ngModelChange)="toggle(a, $event)"
                              [attr.data-testid]="'assistant-toggle-' + a.assistantId" />
              <label [for]="'assistant-' + a.assistantId">{{ a.displayName }}</label>
            </li>
          }
        </ul>
        <p class="ws-hint">
          Switching a check off stops it running in this project. The issues it already raised stay.
        </p>
      }
      @if (canAnalyze()) {
        <p-button label="Re-run analysis" icon="pi pi-refresh" [outlined]="true" size="small"
                  [loading]="analyzing()" (onClick)="rerun()" data-testid="assistants-rerun" />
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
    .assistant-list { list-style: none; margin: 0 0 var(--rq-space-2); padding: 0;
      display: flex; flex-direction: column; gap: var(--rq-space-2); }
    .assistant-row { display: flex; align-items: center; gap: var(--rq-space-2); }
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
    if (!this.canAnalyze() || this.analyzing()) return;
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
