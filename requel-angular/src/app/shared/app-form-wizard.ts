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
import {
  AfterContentInit,
  Component,
  ContentChild,
  ContentChildren,
  ElementRef,
  EventEmitter,
  Input,
  Output,
  QueryList,
  TemplateRef,
  ViewChild,
  ViewChildren,
  signal,
} from '@angular/core';
import { NgTemplateOutlet } from '@angular/common';
import { FormGroup } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { AppCardComponent } from './app-card';

/** Process-wide counter so each wizard's heading id is unique. */
let nextWizardId = 0;

/**
 * One step of an {@link AppFormWizardComponent}.
 *
 * Declared as a child element rather than an entry in a `steps` array input so the
 * step's fields can arrive as a `<ng-template>` — the caller keeps its own markup
 * and never has to pass `TemplateRef`s around by hand.
 *
 * ```html
 * <app-wizard-step key="details" label="Details" [form]="detailsForm">
 *   <ng-template>
 *     <app-field label="Name" [control]="detailsForm.controls.name">
 *       <input appFieldControl [formControl]="detailsForm.controls.name" />
 *     </app-field>
 *   </ng-template>
 * </app-wizard-step>
 * ```
 *
 * Note for callers: inside that template use `[formControl]`, not
 * `formControlName`. The body is projected into the wizard, and `formControlName`
 * resolves its parent `formGroup` from the injector at the *insertion* point, where
 * there is none.
 */
@Component({
  selector: 'app-wizard-step',
  standalone: true,
  template: '',
})
export class AppWizardStepComponent {
  /** Stable identity for the step. Appears in the route fragment; never an index. */
  @Input({ required: true }) key!: string;

  /** Step label shown in the left-hand nav. */
  @Input({ required: true }) label!: string;

  /** Optional short description under the step label. */
  @Input() helper = '';

  /**
   * The step's form. Drives Continue's disabled state and the step's completion
   * marker. Steps that only host association widgets (Tags, Relations, Goals,
   * Actors) have no fields to validate and pass no form.
   */
  @Input() form?: FormGroup;

  /** Skippable step: Continue is never blocked and no completion is required. */
  @Input() optional = false;

  /** The step's body, supplied as the child `<ng-template>`. */
  @ContentChild(TemplateRef) content?: TemplateRef<unknown>;
}

/**
 * Handed to the host on each Continue so it can run the commit for that step and
 * report the outcome. The wizard stays busy until one of the two callbacks fires,
 * so a host that forgets to respond visibly hangs on its own step rather than
 * silently advancing past a failed save.
 */
export interface WizardCommitRequest {
  /** The step being committed. */
  readonly step: AppWizardStepComponent;
  /** Commit succeeded — advance (or finish, on the last step). */
  complete(): void;
  /** Commit failed — stay on the step and show `message` in the alert region. */
  fail(message: string): void;
}

/**
 * Shared multi-step form shell (issue #158).
 *
 * A two-column card: vertical step nav on the left, the active step's fields on the
 * right, a Cancel / Continue footer underneath.
 *
 * ## Why this does not wrap PrimeNG's `p-stepper`
 *
 * The plan originally specified wrapping `p-stepper` the way `app-data-table` wraps
 * `p-table`, to inherit its keyboard nav. That does not work: in PrimeNG 21.1.3
 * `p-stepper` unconditionally carries `role="tablist"` while its children are
 * `role="presentation"` (`p-step`) and `role="tabpanel"` (`p-step-panel`) — never
 * `role="tab"` — so axe reports a critical `aria-required-children` violation. Both
 * the vertical (`p-step-item`) and horizontal (`p-step-list` + `p-step-panels`)
 * compositions fail it, and `p-step-list` gets no role at all, so the tablist is on
 * the wrong element. There is no way to override a component's host binding from
 * outside it.
 *
 * Hand-rolling is also the more accurate semantic. Tabs are peer views the user
 * browses; wizard steps are a linear process with progress state. This renders a
 * `<nav>` + `<ol>` of buttons with `aria-current="step"`, which carries no
 * tablist child requirements and describes what the control actually is.
 *
 * ## Keyboard model
 *
 * Roving tabindex over the step buttons: exactly one is tabbable, Up/Down (and
 * Left/Right) move focus, Home/End jump to the ends, and Enter/Space activate via
 * the native button. Moving focus does not change the step — activation is
 * explicit, so arrowing past a step never commits anything.
 *
 * ## Commit model
 *
 * Continue emits {@link stepCommit} with a {@link WizardCommitRequest}; the host
 * decides whether that hits the API. Nothing about entity versions lives here —
 * the host owns the version it sends and refreshes it from each command's returned
 * entity. The wizard's part of that contract is to keep the step on failure
 * (including a stale-version 409) and surface the host's message, instead of
 * advancing and losing the user's edit.
 */
@Component({
  selector: 'app-form-wizard',
  standalone: true,
  imports: [ButtonModule, NgTemplateOutlet, AppCardComponent],
  template: `
    <app-card>
      <div class="app-wizard">
        <nav class="app-wizard-nav" [attr.aria-label]="navLabel">
          <ol class="app-wizard-steps">
            @for (step of stepList; track step.key; let i = $index) {
              <li class="app-wizard-step">
                <button
                  #navButton
                  type="button"
                  class="app-wizard-step-button"
                  [class.is-active]="isActive(i)"
                  [class.is-complete]="isStepComplete(step)"
                  [attr.aria-current]="isActive(i) ? 'step' : null"
                  [attr.aria-disabled]="isStepLocked(i) ? 'true' : null"
                  [attr.tabindex]="i === focusedIndex() ? 0 : -1"
                  [attr.data-testid]="'wizard-step-' + step.key"
                  (click)="onNavActivate(i)"
                  (keydown)="onNavKeydown($event, i)"
                >
                  <span class="app-wizard-step-marker" aria-hidden="true">
                    @if (isStepComplete(step)) {
                      <i class="pi pi-check"></i>
                    } @else {
                      {{ i + 1 }}
                    }
                  </span>
                  <span class="app-wizard-step-text">
                    <span class="app-wizard-step-label">{{ step.label }}</span>
                    @if (step.optional) {
                      <span class="app-wizard-step-optional">Optional</span>
                    }
                    @if (step.helper) {
                      <span class="app-wizard-step-helper">{{ step.helper }}</span>
                    }
                  </span>
                  @if (isStepComplete(step)) {
                    <span class="rq-visually-hidden">Completed</span>
                  }
                </button>
              </li>
            }
          </ol>
        </nav>

        @if (activeStep(); as step) {
          <section class="app-wizard-panel" [attr.aria-labelledby]="headingId">
            <h2
              #panelHeading
              class="rq-section-title app-wizard-panel-title"
              [id]="headingId"
              tabindex="-1"
              [attr.data-testid]="'wizard-panel-' + step.key"
            >
              {{ step.label }}
            </h2>

            @if (errorMessage()) {
              <p class="app-wizard-error" role="alert" data-testid="wizard-error">
                {{ errorMessage() }}
              </p>
            }

            <ng-container [ngTemplateOutlet]="step.content ?? null" />

            <div class="app-wizard-footer">
              <p-button
                label="Cancel"
                severity="secondary"
                [text]="true"
                [disabled]="busy()"
                data-testid="wizard-cancel"
                (onClick)="onCancel()"
              />
              <p-button
                [label]="isLastStep(activeIndex()) ? 'Done' : 'Continue'"
                [disabled]="!canContinue(step)"
                [loading]="busy()"
                data-testid="wizard-continue"
                (onClick)="onContinue()"
              />
            </div>
          </section>
        }
      </div>
    </app-card>
  `,
  styles: [`
    :host { display: block; }
    .app-wizard {
      display: grid;
      grid-template-columns: minmax(11rem, 14rem) 1fr;
      gap: var(--rq-space-6);
      align-items: start;
    }
    .app-wizard-steps { list-style: none; margin: 0; padding: 0; }
    .app-wizard-step + .app-wizard-step { margin-top: var(--rq-space-1); }
    .app-wizard-step-button {
      display: flex;
      align-items: flex-start;
      gap: var(--rq-space-2);
      width: 100%;
      padding: var(--rq-space-2);
      border: 0;
      border-radius: var(--rq-radius-md);
      background: transparent;
      font: inherit;
      text-align: start;
      cursor: pointer;
      color: inherit;
    }
    .app-wizard-step-button[aria-disabled='true'] {
      color: var(--rq-text-muted-color);
      cursor: default;
    }
    .app-wizard-step-button.is-active { background: var(--rq-wizard-step-active-bg); }
    .app-wizard-step-button:focus-visible {
      outline: var(--rq-focus-ring-width) solid var(--rq-focus-ring-color);
      outline-offset: var(--rq-focus-ring-offset);
    }
    .app-wizard-step-marker {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      inline-size: var(--rq-wizard-marker-size);
      block-size: var(--rq-wizard-marker-size);
      border-radius: 50%;
      background: var(--rq-wizard-marker-bg);
      font-size: var(--rq-font-size-xs);
      flex: none;
    }
    .is-complete .app-wizard-step-marker {
      background: var(--rq-wizard-marker-complete-bg);
      color: var(--rq-wizard-marker-complete-fg);
    }
    .app-wizard-step-text { display: block; }
    .app-wizard-step-label { display: block; font-weight: var(--rq-font-weight-medium); }
    .app-wizard-step-optional,
    .app-wizard-step-helper {
      display: block;
      color: var(--rq-text-muted-color);
      font-size: var(--rq-font-size-xs);
    }
    .app-wizard-panel-title { margin: 0 0 var(--rq-space-4); }
    .app-wizard-panel-title:focus-visible {
      outline: var(--rq-focus-ring-width) solid var(--rq-focus-ring-color);
      outline-offset: var(--rq-focus-ring-offset);
    }
    .app-wizard-error {
      margin: 0 0 var(--rq-space-4);
      color: var(--rq-field-error-fg);
      font-size: var(--rq-text-helper-size);
    }
    .app-wizard-footer {
      display: flex;
      justify-content: flex-end;
      gap: var(--rq-space-2);
      margin-top: var(--rq-space-6);
    }

    @container (max-width: 40rem) {
      .app-wizard { grid-template-columns: 1fr; }
    }
  `],
})
export class AppFormWizardComponent implements AfterContentInit {
  @ContentChildren(AppWizardStepComponent) private stepQuery!: QueryList<AppWizardStepComponent>;

  @ViewChildren('navButton') private navButtons!: QueryList<ElementRef<HTMLButtonElement>>;

  @ViewChild('panelHeading') private panelHeading?: ElementRef<HTMLElement>;

  /**
   * The active step's key. Two-way bound so the host can mirror it into a route
   * fragment and land on the right step after a back/refresh.
   */
  @Input() activeKey?: string;
  @Output() activeKeyChange = new EventEmitter<string>();

  /** Accessible name for the step nav. */
  @Input() navLabel = 'Steps';

  /** Fires on Continue. See {@link WizardCommitRequest}. */
  @Output() stepCommit = new EventEmitter<WizardCommitRequest>();

  /** Cancel pressed. The host decides whether to navigate away or confirm first. */
  @Output() cancelled = new EventEmitter<void>();

  /** Continue pressed on the last step and its commit succeeded. */
  @Output() finished = new EventEmitter<void>();

  /** Keys of steps whose commit has succeeded. */
  private readonly completed = signal<ReadonlySet<string>>(new Set());

  /** True while a commit is in flight. Blocks Continue/Cancel and shows a spinner. */
  readonly busy = signal(false);

  /** Host-supplied failure text for the current step, cleared on any navigation. */
  readonly errorMessage = signal<string | null>(null);

  /**
   * Which step button is tabbable. Tracked separately from the active step so
   * arrowing through the nav moves focus without activating anything.
   */
  readonly focusedIndex = signal(0);

  /** Snapshot of the declared steps, resolved once content has initialised. */
  stepList: AppWizardStepComponent[] = [];

  private readonly uid = `rq-wizard-${++nextWizardId}`;

  get headingId(): string {
    return `${this.uid}-panel-title`;
  }

  ngAfterContentInit(): void {
    this.stepList = this.stepQuery.toArray();
    this.stepQuery.changes.subscribe(() => {
      this.stepList = this.stepQuery.toArray();
    });
    if (!this.activeKey && this.stepList.length) {
      this.activeKey = this.stepList[0].key;
    }
    this.focusedIndex.set(this.activeIndex());
  }

  /** Index of the active step; an unknown key falls back to the first step. */
  activeIndex(): number {
    const index = this.stepList.findIndex(step => step.key === this.activeKey);
    return index < 0 ? 0 : index;
  }

  activeStep(): AppWizardStepComponent | undefined {
    return this.stepList[this.activeIndex()];
  }

  isActive(index: number): boolean {
    return index === this.activeIndex();
  }

  isLastStep(index: number): boolean {
    return index === this.stepList.length - 1;
  }

  isStepComplete(step: AppWizardStepComponent): boolean {
    return this.completed().has(step.key);
  }

  /**
   * Linear forward, free backward: a step is reachable once every required step
   * before it is complete. Already-visited steps stay reachable so the user can go
   * back and fix a typo.
   */
  isStepLocked(index: number): boolean {
    if (index <= this.activeIndex()) {
      return false;
    }
    return this.stepList
      .slice(0, index)
      .some(step => !step.optional && !this.isStepComplete(step));
  }

  /**
   * Continue is enabled unless the step's form is invalid or a commit is running.
   *
   * Deliberately not gated on `pristine`: with commit-on-step-1 the user can come
   * back to a step whose values are already saved and correct, and a pristine guard
   * would trap them there with no way forward. An optional step has nothing to
   * validate, so it can always be skipped.
   */
  canContinue(step: AppWizardStepComponent): boolean {
    if (this.busy()) {
      return false;
    }
    if (step.optional || !step.form) {
      return true;
    }
    return step.form.valid;
  }

  onContinue(): void {
    const step = this.activeStep();
    if (!step || this.busy() || !this.canContinue(step)) {
      return;
    }

    // Reveal any errors the user has not touched into view yet, so an invalid form
    // explains itself instead of just refusing to advance.
    step.form?.markAllAsTouched();

    this.errorMessage.set(null);
    this.busy.set(true);

    let settled = false;
    const request: WizardCommitRequest = {
      step,
      complete: () => {
        if (settled) {
          return;
        }
        settled = true;
        this.busy.set(false);
        this.completed.update(keys => new Set(keys).add(step.key));
        this.advance();
      },
      fail: (message: string) => {
        if (settled) {
          return;
        }
        settled = true;
        this.busy.set(false);
        this.errorMessage.set(message);
      },
    };

    this.stepCommit.emit(request);
  }

  private advance(): void {
    const next = this.stepList[this.activeIndex() + 1];
    if (!next) {
      this.finished.emit();
      return;
    }
    this.goTo(next.key);
  }

  onCancel(): void {
    if (this.busy()) {
      return;
    }
    this.cancelled.emit();
  }

  /** Step-nav activation (click, Enter, Space). Locked steps are inert. */
  onNavActivate(index: number): void {
    const step = this.stepList[index];
    if (!step || this.isStepLocked(index) || this.busy()) {
      return;
    }
    this.focusedIndex.set(index);
    this.goTo(step.key);
  }

  /**
   * Roving-tabindex keyboard nav. Moves focus only — Enter/Space still do the
   * activating, via the native button.
   */
  onNavKeydown(event: KeyboardEvent, index: number): void {
    const last = this.stepList.length - 1;
    let target: number | null = null;

    switch (event.key) {
      case 'ArrowDown':
      case 'ArrowRight':
        target = Math.min(index + 1, last);
        break;
      case 'ArrowUp':
      case 'ArrowLeft':
        target = Math.max(index - 1, 0);
        break;
      case 'Home':
        target = 0;
        break;
      case 'End':
        target = last;
        break;
      default:
        return;
    }

    event.preventDefault();
    if (target === index) {
      return;
    }
    this.focusedIndex.set(target);
    this.focusNavButton(target);
  }

  private focusNavButton(index: number): void {
    this.navButtons?.get(index)?.nativeElement.focus();
  }

  private goTo(key: string): void {
    if (this.activeKey === key) {
      return;
    }
    this.activeKey = key;
    this.errorMessage.set(null);
    this.focusedIndex.set(this.activeIndex());
    this.activeKeyChange.emit(key);
    // The panel section persists across step changes (only its content swaps), so
    // the heading is already in the DOM and can take focus straight away.
    this.panelHeading?.nativeElement.focus();
  }
}
