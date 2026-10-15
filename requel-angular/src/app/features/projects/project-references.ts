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
import { ChangeDetectionStrategy, Component, OnChanges, computed, inject, input, signal }
  from '@angular/core';
import { AppTagComponent } from '../../shared/app-tag';
import { ProvenanceService } from '../../core/provenance.service';
import { ProjectSourceDto, SourceRefDto } from '../../models/provenance';

/**
 * The project overview's References card (issue #273): every source and reference recorded in
 * the project, with its kind, locator and note, how many entities were derived from and cite
 * it, and the precedence between sources — "defers to X" means X wins where the two disagree,
 * and both stay current. Read-only: references and precedence are recorded through the
 * gateway. Hidden when the project has none.
 */
@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-project-references',
  standalone: true,
  imports: [AppTagComponent],
  template: `
    @if (sources().length > 0) {
      <section aria-labelledby="references-title" class="ws-panel" data-testid="workspace-references">
        <h2 id="references-title" class="ws-panel-title">References</h2>
        <ul class="reference-list">
          @for (s of sources(); track s.source.id) {
            <li class="reference-row" data-testid="reference-row">
              <div class="reference-head">
                <span class="reference-title" data-testid="reference-title">{{ titleOf(s) }}</span>
                @if (s.source.kind) {
                  <app-tag data-testid="reference-kind" [tone]="'neutral'" [label]="s.source.kind" />
                }
                <span class="reference-id">{{ s.source.system }} · {{ s.source.externalId }}</span>
              </div>
              @if (s.source.locatorType === 'URL' && s.source.locator) {
                <a [href]="s.source.locator" target="_blank" rel="noopener noreferrer"
                   data-testid="reference-link"
                   [attr.aria-label]="titleOf(s) + ' (opens in a new tab)'">
                  {{ s.source.locator }} <i class="pi pi-external-link" aria-hidden="true"></i>
                </a>
              } @else if (s.source.locatorType === 'PATH' && s.source.locator) {
                <code class="reference-path" data-testid="reference-path">{{ s.source.locator }}</code>
              }
              @if (s.source.note) {
                <p class="reference-note" data-testid="reference-note">{{ s.source.note }}</p>
              }
              @if (s.defersTo.length > 0) {
                <p class="reference-authority" data-testid="reference-defers-to">
                  Defers to {{ names(s.defersTo) }}
                </p>
              }
              @if (s.outranks.length > 0) {
                <p class="reference-authority" data-testid="reference-outranks">
                  Outranks {{ names(s.outranks) }}
                </p>
              }
              <p class="reference-counts" data-testid="reference-counts">{{ counts(s) }}</p>
            </li>
          }
        </ul>
        @if (hasAuthority()) {
          <p class="ws-hint">Where two sources disagree, the one deferred to wins. Both stay current.</p>
        }
      </section>
    }
    @if (loadFailed()) {
      <p class="ws-hint" role="status" data-testid="references-load-error">Couldn't load the references.</p>
    }
  `,
  styles: [`
    :host { display: block; }
    .ws-panel {
      box-sizing: border-box; margin-top: var(--rq-space-4);
      padding: var(--rq-card-pad); background: var(--rq-card-bg);
      border: 1px solid var(--rq-card-border); border-radius: var(--rq-card-radius);
      box-shadow: var(--rq-card-shadow);
    }
    .ws-panel-title { margin: 0 0 var(--rq-space-2); font-size: var(--rq-font-size-lg); }
    .reference-list { list-style: none; margin: 0; padding: 0;
      display: flex; flex-direction: column; gap: var(--rq-space-3); }
    .reference-row { display: flex; flex-direction: column; gap: var(--rq-space-1); }
    .reference-head { display: flex; align-items: baseline; gap: var(--rq-space-2); flex-wrap: wrap; }
    .reference-title { font-weight: 600; }
    .reference-id, .reference-counts, .reference-path { font-size: var(--rq-font-size-sm);
      color: var(--p-text-secondary-color); }
    .reference-note, .reference-authority, .reference-counts { margin: 0; }
    .ws-hint { color: var(--p-text-secondary-color); font-size: var(--rq-font-size-sm);
      margin: var(--rq-space-2) 0 0; }
  `],
})
export class ProjectReferencesComponent implements OnChanges {
  readonly projectName = input.required<string>();

  private readonly provenanceService = inject(ProvenanceService);
  readonly sources = signal<ProjectSourceDto[]>([]);
  readonly loadFailed = signal(false);
  readonly hasAuthority = computed(() =>
    this.sources().some(s => s.defersTo.length > 0 || s.outranks.length > 0));

  ngOnChanges(): void {
    void this.load();
  }

  async load(): Promise<void> {
    if (!this.projectName()) return;
    try {
      const result = await this.provenanceService.getProjectSources(this.projectName());
      this.sources.set(result?.sources ?? []);
      this.loadFailed.set(false);
    } catch {
      // Supplemental: never block the overview, but say so.
      this.sources.set([]);
      this.loadFailed.set(true);
    }
  }

  titleOf(s: ProjectSourceDto): string {
    return s.source.title || s.source.externalId;
  }

  names(refs: SourceRefDto[]): string {
    return refs.map(r => r.title || r.externalId).join(', ');
  }

  counts(s: ProjectSourceDto): string {
    const parts: string[] = [];
    if (s.derivedCount > 0) parts.push(`${s.derivedCount} derived from it`);
    if (s.citedByCount > 0) parts.push(`cited by ${s.citedByCount}`);
    return parts.length > 0 ? parts.join(' · ') : 'Not yet cited';
  }
}
