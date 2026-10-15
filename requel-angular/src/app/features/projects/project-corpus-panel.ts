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
import { Component, ChangeDetectionStrategy, OnChanges, OnDestroy, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { SelectModule } from 'primeng/select';
import { CorpusService } from '../../core/corpus.service';
import { GoalService } from '../../core/goal.service';
import { UseCaseService } from '../../core/use-case.service';
import { CorpusMode, CorpusRunDto, CorpusSetKind } from '../../models/corpus';

/** One choice in the set picker. */
export interface CorpusSetOption {
  label: string;
  set: CorpusSetKind;
  rootId: number | null;
}

const TERMINAL = ['SUCCEEDED', 'FAILED', 'SKIPPED', 'CANCELLED'];

/**
 * Issue #266: the project overview's Corpus analysis panel. Pick a set (the whole project, or a
 * goal or use case with what hangs off it) and run "Find overlaps", the non-AI finder, or
 * "Analyse with AI", the corpus definition. Each finding is one issue on every entity it names.
 * Runs only when asked; an edit never starts one. Needs Annotation[Edit], since a run writes
 * issues. The last run shown is the last of the mode most recently chosen.
 */
@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-project-corpus-panel',
  standalone: true,
  imports: [FormsModule, ButtonModule, SelectModule],
  template: `
    <section aria-labelledby="corpus-title" class="ws-panel" data-testid="workspace-corpus">
      <h2 id="corpus-title" class="ws-panel-title">Corpus analysis</h2>
      <p class="ws-hint">
        Looks across entities for ones that conflict or repeat each other. Find overlaps compares
        wording; Analyse with AI sends the set to the AI provider, one call per run. Each finding is one
        issue on every entity involved.
      </p>
      <label for="corpus-set" class="ws-label">Analyse</label>
      <p-select inputId="corpus-set" [options]="options()" optionLabel="label" [ngModel]="selected()"
                (ngModelChange)="select($event)" [disabled]="busy()" data-testid="corpus-set"
                styleClass="corpus-set" />
      @if (canAnalyze()) {
        <div class="corpus-actions">
          <p-button label="Find overlaps" icon="pi pi-sitemap" [outlined]="true" size="small"
                    [loading]="busy() && mode() === 'CANDIDATES'" [disabled]="busy()"
                    (onClick)="run('CANDIDATES')" data-testid="corpus-find-overlaps" />
          <p-button label="Analyse with AI" icon="pi pi-bolt" [outlined]="true" size="small"
                    [loading]="busy() && mode() === 'ANALYSIS'" [disabled]="busy()"
                    (onClick)="run('ANALYSIS')" data-testid="corpus-analyse" />
        </div>
      }
      @if (latest(); as run) {
        <p class="ws-hint" data-testid="corpus-latest">
          Last run: {{ statusLabel(run) }}@if (run.summary) {: {{ run.summary }}}
          @if (relationships(run) > 0) { ({{ relationships(run) }} open) }
        </p>
        @if (run.errorSummary) {
          <p class="ws-error" data-testid="corpus-error-summary">{{ run.errorSummary }}</p>
        }
      }
      <p class="ws-status" role="status" aria-live="polite" data-testid="corpus-status">{{ status() }}</p>
      @if (error()) {
        <p class="ws-error" role="alert" data-testid="corpus-error">{{ error() }}</p>
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
    .ws-label { display: block; margin-bottom: var(--rq-space-1); font-weight: 600; }
    :host ::ng-deep .corpus-set { width: 100%; }
    .corpus-actions { display: flex; gap: var(--rq-space-2); margin-top: var(--rq-space-2); }
    .ws-hint { color: var(--rq-text-muted); font-size: var(--rq-font-size-sm); }
    .ws-status:empty { display: none; }
    .ws-error { color: var(--rq-danger-text, #b42318); }
  `],
})
export class ProjectCorpusPanelComponent implements OnChanges, OnDestroy {
  readonly projectName = input.required<string>();
  readonly projectId = input.required<number>();
  readonly canAnalyze = input(false);
  /** Milliseconds between checks of a queued run; tests set it to 0. */
  readonly pollMs = input(2000);

  readonly options = signal<CorpusSetOption[]>([{ label: 'Whole project', set: 'PROJECT', rootId: null }]);
  readonly selected = signal<CorpusSetOption>({ label: 'Whole project', set: 'PROJECT', rootId: null });
  readonly latest = signal<CorpusRunDto | null>(null);
  readonly busy = signal(false);
  readonly status = signal('');
  readonly error = signal<string | null>(null);
  readonly mode = signal<CorpusMode>('CANDIDATES');

  private timer: ReturnType<typeof setTimeout> | null = null;

  constructor(private corpus: CorpusService, private goals: GoalService, private useCases: UseCaseService) {}

  async ngOnChanges(): Promise<void> {
    await this.loadOptions();
    await this.refresh();
  }

  ngOnDestroy(): void {
    this.stopPolling();
  }

  async select(option: CorpusSetOption): Promise<void> {
    this.selected.set(option);
    this.status.set('');
    await this.refresh();
  }

  async run(mode: CorpusMode): Promise<void> {
    if (!this.canAnalyze() || this.busy()) return;
    const option = this.selected();
    this.mode.set(mode);
    this.busy.set(true);
    this.error.set(null);
    try {
      const previous = (await this.refresh())?.runId ?? null;
      await this.corpus.request(this.projectId(), option.set, option.rootId, mode);
      this.status.set('Queued: ' + option.label + '.');
      this.poll(30, previous);
    } catch (e) {
      this.busy.set(false);
      this.error.set(errorMessage(e, "Couldn't start the run."));
    }
  }

  statusLabel(run: CorpusRunDto): string {
    switch (run.status) {
      case 'SUCCEEDED': return 'finished';
      case 'FAILED': return 'failed';
      case 'SKIPPED': return 'skipped';
      case 'CANCELLED': return 'cancelled';
      default: return 'running';
    }
  }

  /** Relationship findings still open: one issue per relationship, one row per entity. */
  relationships(run: CorpusRunDto): number {
    const ids = new Set<number>();
    for (const f of run.findings ?? []) {
      if (f.state === 'ACTIVE' && f.annotationId != null) ids.add(f.annotationId);
    }
    return ids.size;
  }

  private async loadOptions(): Promise<void> {
    const options: CorpusSetOption[] = [{ label: 'Whole project', set: 'PROJECT', rootId: null }];
    try {
      const [goals, useCases] = await Promise.all([
        this.goals.listGoals(this.projectName()), this.useCases.listUseCases(this.projectName())]);
      for (const g of goals) options.push({ label: 'Goal: ' + g.name, set: 'GOAL', rootId: g.id! });
      for (const u of useCases) options.push({ label: 'Use case: ' + u.name, set: 'USE_CASE', rootId: u.id! });
    } catch {
      // the whole project is still a set
    }
    this.options.set(options);
    this.selected.set(options[0]);
  }

  private async refresh(): Promise<CorpusRunDto | null> {
    const option = this.selected();
    try {
      const run = await this.corpus.latest(this.projectId(), option.set, option.rootId, this.mode());
      this.latest.set(run);
      return run;
    } catch {
      this.latest.set(null);
      return null;
    }
  }

  /** Check until a run newer than {@code previous} finishes, at most {@code remaining} times. */
  private poll(remaining: number, previous: string | null): void {
    this.stopPolling();
    this.timer = setTimeout(async () => {
      const run = await this.refresh();
      if (run && run.runId !== previous && TERMINAL.includes(run.status)) {
        this.busy.set(false);
        this.status.set('Finished: ' + (run.summary ?? this.statusLabel(run)) + '.');
      } else if (remaining > 1) {
        this.poll(remaining - 1, previous);
      } else {
        this.busy.set(false);
        this.status.set('Still running. Check back in a moment.');
      }
    }, this.pollMs());
  }

  private stopPolling(): void {
    if (this.timer != null) {
      clearTimeout(this.timer);
      this.timer = null;
    }
  }
}

function errorMessage(e: unknown, fallback: string): string {
  const message = (e as { error?: { message?: string } })?.error?.message;
  return message ?? fallback;
}
