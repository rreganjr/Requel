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
import { ChangeDetectionStrategy, Component, OnChanges, OnDestroy, SimpleChanges, input, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { ButtonModule } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';
import { AiReviewService, AiReviewTarget } from '../core/ai-review.service';
import { AiReviewRun, EDITOR_REVIEWABLE_TYPES } from '../models/ai-review';

const IN_FLIGHT = ['QUEUED', 'RUNNING'];
const SAVE_FIRST = 'Save first: the review reads the saved text.';

/**
 * Issue #387: "Review with AI" beside an editor's Save. Shown only when a review would run for
 * the entity (AI on for the server and the project, and the type's review not switched off):
 * the server says so, and why not. Disabled while the form has unsaved changes - the review reads
 * the saved text - and while a run is followed.
 *
 * It follows the run by polling the latest review, reports it in a status beside the button, and
 * when the run succeeds tells {@link AiReviewService} so the Annotations section reloads the
 * findings. A review already in flight when the editor opens is followed too.
 */
@Component({
  selector: 'app-ai-review-button',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ButtonModule, TooltipModule],
  template: `
    @if (available()) {
      <span class="ai-review-trigger" [pTooltip]="dirty() ? saveFirst : undefined"
            tooltipPosition="top" data-testid="ai-review-trigger">
        <p-button label="Review with AI" icon="pi pi-sparkles" severity="secondary"
                  [outlined]="true" data-testid="ai-review-button"
                  [disabled]="dirty() || following()"
                  [ariaLabel]="dirty() ? 'Review with AI. ' + saveFirst : 'Review with AI'"
                  (onClick)="requestReview()" />
      </span>
    }
    <span class="ai-review-status" role="status" aria-live="polite"
          data-testid="ai-review-status">{{ status() }}</span>
  `,
  styles: [`
    :host { display: inline-flex; align-items: center; gap: var(--rq-space-2); flex-wrap: wrap; }
    .ai-review-status { color: var(--p-text-secondary-color); font-size: 0.875rem; }
    .ai-review-status:empty { display: none; }
  `]
})
export class AiReviewButtonComponent implements OnChanges, OnDestroy {
  readonly entityType = input.required<string>();
  readonly entityId = input<number | null>(null);
  /** The editor's form has unsaved changes. */
  readonly dirty = input(false);
  /** Milliseconds between checks of a followed run; tests set it low. */
  readonly pollMs = input(3000);
  /** Checks before giving up on a run (100 x 3 s = 5 minutes). */
  readonly maxPolls = input(100);

  readonly available = signal(false);
  readonly following = signal(false);
  readonly status = signal('');
  readonly saveFirst = SAVE_FIRST;

  private timer: ReturnType<typeof setTimeout> | null = null;
  /** Bumped on every (re)load, so a stale answer for another entity is dropped. */
  private generation = 0;

  constructor(private reviews: AiReviewService) {}

  ngOnChanges(changes: SimpleChanges): void {
    if ('entityType' in changes || 'entityId' in changes) {
      void this.load();
    }
  }

  ngOnDestroy(): void {
    this.stop();
    this.generation++;
  }

  private target(): AiReviewTarget | null {
    const id = this.entityId();
    return id == null || !EDITOR_REVIEWABLE_TYPES.includes(this.entityType()) ? null
      : { entityType: this.entityType(), entityId: id };
  }

  private async load(): Promise<void> {
    this.stop();
    const generation = ++this.generation;
    this.available.set(false);
    this.following.set(false);
    this.status.set('');
    const target = this.target();
    if (!target) return;
    try {
      const availability = await this.reviews.availability(target);
      if (generation !== this.generation || !availability.available) return;
      this.available.set(true);
      // Pick up a review already in flight (requested before navigating here).
      const latest = await this.reviews.latest(target);
      if (generation !== this.generation) return;
      if (latest && IN_FLIGHT.includes(latest.status)) {
        this.follow(target, latest.runId, generation);
        this.status.set(this.inFlightLabel(latest));
      }
    } catch {
      // No availability (or no access): no button.
    }
  }

  async requestReview(): Promise<void> {
    const target = this.target();
    if (!target || this.dirty() || this.following()) return;
    const generation = this.generation;
    this.following.set(true);
    this.status.set('AI review queued…');
    try {
      const runs = await this.reviews.request(target);
      if (generation !== this.generation) return;
      this.follow(target, runs.runId, generation);
    } catch (e) {
      if (generation !== this.generation) return;
      this.following.set(false);
      if (e instanceof HttpErrorResponse && e.status === 409) {
        // It can't run after all (switched off since the page loaded): say why, drop the button.
        this.available.set(false);
        this.status.set(e.error?.message ?? 'AI review is not available for this item.');
      } else {
        this.status.set('The AI review could not be requested.');
      }
    }
  }

  private follow(target: AiReviewTarget, runId: string, generation: number, remaining = this.maxPolls()): void {
    this.following.set(true);
    this.stop();
    this.timer = setTimeout(async () => {
      this.timer = null;
      let run: AiReviewRun | null;
      try {
        run = await this.reviews.latest(target);
      } catch {
        if (generation !== this.generation) return;
        this.following.set(false);
        this.status.set('Could not read the AI review’s status.');
        return;
      }
      if (generation !== this.generation) return;
      if (run && run.runId === runId && !IN_FLIGHT.includes(run.status)) {
        this.finish(target, run);
      } else if (remaining > 1) {
        if (run && run.runId === runId) this.status.set(this.inFlightLabel(run));
        this.follow(target, runId, generation, remaining - 1);
      } else {
        this.following.set(false);
        this.status.set('The AI review is still running; check back later.');
      }
    }, this.pollMs());
  }

  private finish(target: AiReviewTarget, run: AiReviewRun): void {
    this.following.set(false);
    if (run.status === 'SUCCEEDED') {
      this.status.set('AI review done: ' + findingsLabel(run) + '.');
      this.reviews.notifyCompleted(target);
    } else {
      this.status.set('AI review failed' + (run.errorSummary ? ': ' + run.errorSummary : '') + '.');
    }
  }

  private inFlightLabel(run: AiReviewRun): string {
    return run.status === 'RUNNING' ? 'AI review running…' : 'AI review queued…';
  }

  private stop(): void {
    if (this.timer) {
      clearTimeout(this.timer);
      this.timer = null;
    }
  }
}

/** "2 issues and 1 note, in Annotations below", or "no findings". */
function findingsLabel(run: AiReviewRun): string {
  const issues = run.findings.filter(f => f.kind === 'ISSUE').length;
  const notes = run.findings.length - issues;
  if (issues + notes === 0) return 'no findings';
  const parts = [];
  if (issues) parts.push(issues + (issues === 1 ? ' issue' : ' issues'));
  if (notes) parts.push(notes + (notes === 1 ? ' note' : ' notes'));
  return parts.join(' and ') + ', in Annotations below';
}
