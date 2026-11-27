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
import { ChangeDetectionStrategy, Component, computed, effect, inject, input, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { debounceTime } from 'rxjs';
import { TooltipModule } from 'primeng/tooltip';
import { TermService } from '../core/term.service';
import { GlossaryTermDto } from '../models/term';
import { findTermsUsed } from './term-matcher';

/** How long after the last keystroke the chips catch up. */
export const TERMS_USED_DEBOUNCE_MS = 250;

let nextId = 0;

/**
 * "Terms used" (#506): a chip for each glossary term the name and text being edited use, with
 * the term's definition on hover and on focus. Matches what is typed, so unsaved edits count.
 * Renders nothing while no term matches.
 */
@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-terms-used',
  standalone: true,
  imports: [RouterLink, TooltipModule],
  template: `
    @if (found().length) {
      <div class="terms-used" role="group" [attr.aria-labelledby]="labelId" data-testid="terms-used">
        <span class="terms-label" [id]="labelId">Terms used</span>
        <ul class="term-chips">
          @for (t of found(); track t.id) {
            <li>
              <a class="term-chip" [routerLink]="['/projects', projectName(), 'terms', t.targetId]"
                 [pTooltip]="t.definition" tooltipEvent="both" tooltipPosition="top"
                 [attr.aria-describedby]="labelId + '-' + t.id" data-testid="term-chip">
                {{ t.name }}
                @if (t.canonicalName) {
                  <span aria-hidden="true">&rarr;</span>
                  <span class="rq-visually-hidden">, preferred term</span>
                  {{ t.canonicalName }}
                }
              </a>
              <span class="rq-visually-hidden" [id]="labelId + '-' + t.id">{{ t.definition }}</span>
            </li>
          }
        </ul>
      </div>
    }
  `,
  styles: [`
    :host { display: block; }
    .terms-used { display: flex; flex-wrap: wrap; align-items: baseline; gap: var(--rq-space-2); margin-top: var(--rq-space-2); }
    .terms-label { color: var(--p-text-secondary-color); font-size: var(--rq-font-size-sm); }
    .term-chips { display: flex; flex-wrap: wrap; gap: var(--rq-space-2); list-style: none; margin: 0; padding: 0; }
    .term-chip {
      display: inline-flex; align-items: center; gap: var(--rq-space-1);
      padding: 0 var(--rq-space-2); border-radius: 999px; border: 1px solid var(--rq-card-border);
      color: var(--p-primary-color); font-size: var(--rq-font-size-sm); text-decoration: none;
    }
    .term-chip:hover, .term-chip:focus-visible { border-color: var(--p-primary-color); }
  `],
})
export class TermsUsedComponent {
  private readonly termService = inject(TermService);

  readonly projectName = input.required<string>();
  readonly name = input<string | null | undefined>('');
  readonly text = input<string | null | undefined>('');
  /** The term editor's own term, never listed. */
  readonly excludeTermId = input<number | null | undefined>(null);

  readonly labelId = `terms-used-${++nextId}`;
  private readonly terms = signal<GlossaryTermDto[]>([]);
  private loadedFor: string | null = null;

  /** The name and text, caught up a moment after typing stops. */
  private readonly settled = toSignal(
    toObservable(computed(() => ({ name: this.name(), text: this.text() })))
      .pipe(debounceTime(TERMS_USED_DEBOUNCE_MS)),
    { initialValue: { name: '', text: '' } });

  readonly found = computed(() => findTermsUsed(this.terms(), this.settled().name, this.settled().text,
    this.excludeTermId()));

  constructor() {
    // The project's terms, once per editor visit.
    effect(() => {
      const project = this.projectName();
      if (project && project !== this.loadedFor) {
        this.loadedFor = project;
        this.termService.listTerms(project).then(
          terms => this.terms.set(terms),
          () => this.terms.set([]));
      }
    });
  }
}
