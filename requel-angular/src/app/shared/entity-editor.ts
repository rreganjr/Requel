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
import { ChangeDetectionStrategy, Component, ContentChild, Directive, TemplateRef, computed, inject, input, output } from '@angular/core';
import { NgTemplateOutlet } from '@angular/common';
import { RouterLink } from '@angular/router';
import { PageHeaderComponent } from './page-header';
import { SubmitErrorComponent } from './app-submit-error';
import { UpdateBannerComponent } from './app-update-banner';
import { AppCardComponent } from './app-card';
import { LoadingStateComponent } from './loading-state';
import { ErrorStateComponent } from './error-state';
import { AnnotationsSectionComponent } from './annotations-section';
import { SourcesSectionComponent } from './sources-section';

/** "story" → "stories", "goal" → "goals"; enough for the entity nouns. */
function plural(noun: string): string {
  return /[^aeiou]y$/.test(noun) ? noun.slice(0, -1) + 'ies' : noun + 's';
}

/** #525: where an entity editor is. `not-found` is a 404 on the entity read. */
export type EntityEditorState = 'loading' | 'not-found' | 'error' | 'ready';

/**
 * Marks the editor's body: `<ng-template rqEditorBody> ...form... </ng-template>`.
 *
 * A template, not plain projected content, because Angular creates projected content whether or
 * not the `<ng-content>` is rendered. The body holds the form, wizard and relationship sections;
 * as plain content they would be built (and start their own loads) while the editor is still
 * loading or has found nothing. As a template the shell stamps it out only once `ready`.
 */
@Directive({ selector: 'ng-template[rqEditorBody]', standalone: true })
export class EntityEditorBodyDirective {
  readonly template = inject(TemplateRef);
}

/**
 * #525: the frame every entity editor shares, so header, states and section order live in one
 * place:
 *
 * - the header: `app-page-header` plus an actions row (`[actions]` slot), wrapping on a narrow
 *   window (#519);
 * - the save-error banner and the changed-elsewhere banner;
 * - loading, load-error, not-found and ready states;
 * - in the ready state the body (`rqEditorBody` template), then Annotations and Sources.
 *
 * Anything that must outlive a state change (dialogs, `p-confirmDialog`) goes in the `[after]`
 * slot, which is always rendered.
 *
 * Not found shows only its title and a link back to the list: no actions, banners or sections,
 * since there is nothing to act on.
 */
@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-entity-editor',
  standalone: true,
  imports: [
    NgTemplateOutlet, RouterLink,
    PageHeaderComponent, SubmitErrorComponent, UpdateBannerComponent, AppCardComponent,
    LoadingStateComponent, ErrorStateComponent, AnnotationsSectionComponent, SourcesSectionComponent,
  ],
  template: `
    <div class="entity-editor-header">
      <app-page-header [eyebrow]="eyebrow()" [title]="headerTitle()" />
      @if (state() !== 'not-found') {
        <div class="page-actions"><ng-content select="[actions]" /></div>
      }
    </div>

    @if (state() !== 'not-found') {
      <app-submit-error [message]="saveError()" [retryable]="saveRetryable()" (retry)="retrySave.emit()"
                        [testid]="saveErrorTestid() || testid() + '-error'" />
      @if (updateAvailable()) {
        <app-update-banner [message]="updateMessage()" [testid]="updateTestid()"
                           (reload)="reloadUpdate.emit()" (dismiss)="dismissUpdate.emit()" />
      }
    }

    @switch (state()) {
      @case ('loading') {
        <app-card>
          <app-loading-state [label]="'Loading ' + noun() + '…'" [lines]="loadingLines()"
                             [testid]="testid() + '-loading'" />
        </app-card>
      }
      @case ('error') {
        <app-error-state [message]="loadError() ?? ('Failed to load ' + noun() + '.')"
                         [testid]="testid() + '-load-error'" (retry)="retryLoad.emit()" />
      }
      @case ('not-found') {
        <app-card>
          <div class="not-found" [attr.data-testid]="testid() + '-not-found'">
            <p class="not-found__message">
              There is no {{ noun() }} with this id. It may have been deleted, or the link may be out of date.
            </p>
            @if (listLink(); as link) {
              <a [routerLink]="link" [attr.data-testid]="testid() + '-not-found-list'">{{ listLabel() }}</a>
            }
          </div>
        </app-card>
      }
      @case ('ready') {
        @if (body) {
          <ng-container [ngTemplateOutlet]="body.template" />
        }
        @if (showAnnotations()) {
          <app-annotations-section (resolved)="issueResolved.emit()" [projectName]="projectName()"
                                   [entityType]="entityType()" [entityId]="entityId()" />
        }
        @if (showSources()) {
          <app-sources-section [projectName]="projectName()" [entityType]="entityType()"
                               [entityId]="entityId()" />
        }
      }
    }

    <ng-content select="[after]" />
  `,
  styles: [`
    :host { display: block; }
    /* #519: the actions wrap under the title on a narrow window instead of squeezing it. */
    .entity-editor-header {
      display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap;
      gap: var(--rq-space-2); margin-bottom: var(--rq-space-4);
    }
    .page-actions { display: flex; flex-wrap: wrap; gap: var(--rq-space-2); }
    .not-found { display: flex; flex-direction: column; gap: var(--rq-space-2); }
    .not-found__message { margin: 0; }
  `],
})
export class EntityEditorComponent {
  /** The entity's name, or "New Goal" and so on. */
  readonly title = input('');
  /** Optional context line above the title (the step editor's "Step"). */
  readonly eyebrow = input('');
  /** Title-case name of the kind of entity, for the not-found heading: "Goal", "Use case". */
  readonly entityLabel = input.required<string>();
  /** Lower-case name for running text; defaults to `entityLabel` lower-cased. */
  readonly nounInput = input<string | undefined>(undefined, { alias: 'noun' });
  readonly state = input.required<EntityEditorState>();
  readonly loadError = input<string | null>(null);
  readonly loadingLines = input(4);
  /** Router link to the entity's list, offered from the not-found state. */
  readonly listLink = input<unknown[] | null>(null);
  /** Text of that link; defaults to "Back to <nouns>". */
  readonly listLabelInput = input<string | undefined>(undefined, { alias: 'listLabel' });

  readonly saveError = input<string | null>(null);
  readonly saveRetryable = input(false);
  /** The save-error banner's test id; defaults to `<testid>-error`. */
  readonly saveErrorTestid = input('');

  readonly updateAvailable = input(false);
  readonly updateTestid = input('update-banner');

  readonly projectName = input('');
  readonly entityType = input('');
  readonly entityId = input<number | null>(null);
  readonly showAnnotations = input(true);
  readonly showSources = input(true);

  /** Prefix for the state test ids: `<testid>-loading`, `-load-error`, `-not-found`, `-error`. */
  readonly testid = input('entity-editor');

  readonly retryLoad = output<void>();
  readonly retrySave = output<void>();
  readonly reloadUpdate = output<void>();
  readonly dismissUpdate = output<void>();
  readonly issueResolved = output<void>();

  @ContentChild(EntityEditorBodyDirective) body?: EntityEditorBodyDirective;

  readonly noun = computed(() => this.nounInput() ?? this.entityLabel().toLowerCase());
  readonly headerTitle = computed(() =>
    this.state() === 'not-found' ? this.entityLabel() + ' not found' : this.title());
  readonly updateMessage = computed(() =>
    'This ' + this.noun() + ' was changed elsewhere. Your unsaved changes are preserved.');
  readonly listLabel = computed(() => this.listLabelInput() ?? 'Back to ' + plural(this.noun()));
}
