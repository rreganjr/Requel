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
import { ChangeDetectionStrategy, Component, Input, OnChanges, SimpleChanges, inject, signal }
  from '@angular/core';
import { DatePipe } from '@angular/common';
import { AppCardComponent } from './app-card';
import { AppTagComponent } from './app-tag';
import { ProvenanceService } from '../core/provenance.service';
import { EntitySourceLinkDto } from '../models/provenance';

/**
 * The external sources an entity was built from (issue #272): system, external id, the fragment
 * it came from and when it was ingested, flagged when the source has moved on without it. Only a
 * URL locator is a link (opened in a new tab); a path is shown as text, since Requel never reads
 * it. Read-only, and hidden when the entity has no sources — most entities are written by hand.
 */
@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-sources-section',
  standalone: true,
  imports: [AppCardComponent, AppTagComponent, DatePipe],
  template: `
    @if (links().length > 0) {
      <div data-testid="sources-section">
      <app-card title="Sources">
        <ul class="source-list">
          @for (link of links(); track link.id) {
            <li class="source-item" data-testid="source-row">
              <span class="source-system">{{ link.source.system }}</span>
              @if (link.source.locatorType === 'URL' && link.source.locator) {
                <a [href]="link.source.locator" target="_blank" rel="noopener noreferrer"
                   data-testid="source-link"
                   [attr.aria-label]="link.source.externalId + ' (opens in a new tab)'">
                  {{ link.source.externalId }} <i class="pi pi-external-link" aria-hidden="true"></i>
                </a>
              } @else {
                <span data-testid="source-id">{{ link.source.externalId }}</span>
              }
              @if (link.fragment) {
                <span class="source-fragment" data-testid="source-fragment">{{ link.fragment }}</span>
              }
              @if (link.source.locatorType === 'PATH' && link.source.locator
                   && link.source.locator !== link.source.externalId) {
                <span class="source-path" data-testid="source-path">{{ link.source.locator }}</span>
              }
              @if (link.ingestedAt) {
                <span class="source-date">ingested {{ link.ingestedAt | date: 'mediumDate' }}</span>
              }
              @if (link.notInLatestSource) {
                <app-tag data-testid="source-stale" [tone]="'warning'" icon="pi pi-history"
                         label="Not in latest source"
                         title="The source has a newer version this part was not read from; it may have been removed upstream." />
              }
            </li>
          }
        </ul>
      </app-card>
      </div>
    }
    @if (loadError()) {
      <p class="source-error" role="status" data-testid="sources-error">{{ loadError() }}</p>
    }
  `,
  styles: [`
    .source-list { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 0.4rem; }
    .source-item { display: flex; align-items: baseline; gap: 0.5rem; flex-wrap: wrap; }
    .source-system { font-size: 0.75rem; text-transform: uppercase; color: var(--p-text-secondary-color); }
    .source-fragment { font-weight: 600; }
    .source-path, .source-date { font-size: 0.8rem; color: var(--p-text-secondary-color); }
    .source-error { font-size: 0.85rem; color: var(--p-text-secondary-color); font-style: italic; }
  `],
})
export class SourcesSectionComponent implements OnChanges {
  @Input() projectName = '';
  @Input() entityType = '';
  @Input() entityId: number | null = null;

  private readonly provenanceService = inject(ProvenanceService);
  private readonly _links = signal<EntitySourceLinkDto[]>([]);
  readonly links = this._links.asReadonly();
  private readonly _loadError = signal<string | null>(null);
  readonly loadError = this._loadError.asReadonly();

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['entityId'] || changes['entityType'] || changes['projectName']) {
      void this.load();
    }
  }

  private async load(): Promise<void> {
    if (this.entityId == null || !this.projectName || !this.entityType) {
      this._links.set([]);
      return;
    }
    try {
      this._links.set(await this.provenanceService.getEntitySources(
        this.projectName, this.entityType, this.entityId));
      this._loadError.set(null);
    } catch {
      // Supplemental, like the annotations: never block the editor, but say so.
      this._links.set([]);
      this._loadError.set('Sources could not be loaded.');
    }
  }
}
