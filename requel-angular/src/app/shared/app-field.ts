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
  AfterContentChecked,
  AfterContentInit,
  ContentChild,
  Component,
  Directive,
  ElementRef,
  Input,
  OnDestroy,
  inject,
} from '@angular/core';
import { AbstractControl } from '@angular/forms';
import { FormErrorOverrides, firstErrorMessage, isRequired } from './form-errors';

/** Process-wide counter for generated field ids (`rq-field-1`, `rq-field-2`, ...). */
let nextFieldId = 0;

/**
 * Marks the control projected into an {@link AppFieldComponent}.
 *
 * `app-field` queries this with `@ContentChild` and stamps `id`, `aria-describedby`,
 * `aria-invalid` and `aria-required` onto it, so callers never wire ARIA by hand —
 * which is what makes the label/error-association work (#138) structural rather than
 * something each of nine editors has to remember.
 *
 * Usage: `<input pInputText appFieldControl [formControl]="form.controls.name" />`
 */
@Directive({
  selector: '[appFieldControl]',
  standalone: true,
})
export class AppFieldControlDirective {
  private readonly elementRef = inject<ElementRef<HTMLElement>>(ElementRef);

  /** The element the directive is applied to. */
  get host(): HTMLElement {
    return this.elementRef.nativeElement;
  }

  /**
   * The element that should actually carry `id` / `aria-*`.
   *
   * For a native control (`input`, `textarea`, `select`) that is the host itself.
   * For a PrimeNG wrapper such as `p-select` the host is a custom element that is
   * not focusable and gets no accessible name from a `<label for>`, so we descend to
   * the first focusable child instead.
   *
   * Wrapper components that render their inner control asynchronously may not have
   * one yet at content-init time; for those, pass `app-field`'s `controlId` and the
   * wrapper's own id input (e.g. `p-select`'s `inputId`) so both agree without
   * depending on DOM timing.
   */
  resolveTarget(): HTMLElement {
    const host = this.host;
    const tag = host.tagName.toLowerCase();
    if (tag === 'input' || tag === 'textarea' || tag === 'select') {
      return host;
    }
    return (
      host.querySelector<HTMLElement>(
        'input:not([type="hidden"]), textarea, select, [tabindex]:not([tabindex="-1"])'
      ) ?? host
    );
  }
}

/**
 * Shared form-row primitive (issue #158).
 *
 * One form row: label + helper text on the left, control on the right, hairline
 * divider below, inline error beneath the control. Replaces the per-editor
 * `div.form-grid { grid-template-columns: 120px 1fr }` that #126's descope left in
 * place — nine editors currently redefine that block at varying widths, and none of
 * them associate their errors with their inputs.
 *
 * The row owns label <-> control <-> error association (see
 * {@link AppFieldControlDirective}), so #138 is satisfied once here rather than per
 * caller.
 *
 * All styling reads from `--rq-*` tokens; the component holds no color, spacing,
 * radius or type literals.
 */
@Component({
  selector: 'app-field',
  standalone: true,
  template: `
    <div class="app-field" [class.app-field-bordered]="divider">
      <div class="app-field-label-col">
        <label class="app-field-label" [attr.for]="labelFor">
          {{ label }}@if (required) {<span class="app-field-required" aria-hidden="true">*</span>}
        </label>
        @if (helper) {
          <p class="app-field-helper" [id]="helperId">{{ helper }}</p>
        }
      </div>
      <div class="app-field-control-col">
        <ng-content />
        @if (showError) {
          <p class="app-field-error" [id]="errorId" data-testid="field-error">
            {{ errorMessage }}
          </p>
        }
      </div>
    </div>
  `,
  styles: [`
    /* container-type establishes the query container the @container rule below targets. */
    :host { display: block; container-type: inline-size; }
    .app-field {
      display: grid;
      grid-template-columns: var(--rq-field-label-w) 1fr;
      gap: var(--rq-space-2) var(--rq-space-4);
      align-items: start;
      padding-block: var(--rq-space-3);
    }
    .app-field-bordered { border-bottom: 1px solid var(--rq-field-divider); }
    .app-field-label {
      display: block;
      font-size: var(--rq-text-label-size);
      font-weight: var(--rq-text-label-weight);
      line-height: var(--rq-text-label-line);
    }
    .app-field-required {
      color: var(--rq-field-required-fg);
      margin-inline-start: var(--rq-space-1);
    }
    .app-field-helper {
      margin: var(--rq-space-1) 0 0;
      color: var(--rq-text-muted-color);
      font-size: var(--rq-text-helper-size);
      font-weight: var(--rq-text-helper-weight);
      line-height: var(--rq-text-helper-line);
    }
    .app-field-error {
      margin: var(--rq-space-1) 0 0;
      color: var(--rq-field-error-fg);
      font-size: var(--rq-text-helper-size);
      line-height: var(--rq-text-helper-line);
    }

    /*
     * Narrow viewports stack label above control. Uses a container query rather
     * than --rq-editor-max, which is a max-width token (48rem), not a breakpoint.
     */
    @container (max-width: 30rem) {
      .app-field { grid-template-columns: 1fr; }
    }
  `],
})
export class AppFieldComponent implements AfterContentInit, AfterContentChecked, OnDestroy {
  /** Row label. Rendered as a real `<label for>` bound to the projected control. */
  @Input({ required: true }) label!: string;

  /** Optional helper text under the label; linked via `aria-describedby`. */
  @Input() helper = '';

  /**
   * The reactive control backing this row. Drives the error message, the required
   * marker, `aria-invalid` and `aria-required`. Optional so a row can host a
   * display-only or not-yet-migrated control.
   */
  @Input() control?: AbstractControl | null;

  /** Hairline divider below the row. Off for the last row in a group. */
  @Input() divider = true;

  /** Per-field message overrides, keyed by validation error (e.g. `{ required: '...' }`). */
  @Input() errorMessages?: FormErrorOverrides;

  /**
   * Caller-supplied control id. Use this for wrapper components that render their
   * own inner input (pass the same value to e.g. `p-select`'s `inputId`). When
   * omitted, an id is generated.
   */
  @Input() controlId?: string;

  /**
   * Set by a submit/step-commit attempt so errors show on untouched controls too.
   * `app-form-wizard` sets this when Continue is pressed.
   */
  @Input() submitted = false;

  @ContentChild(AppFieldControlDirective) private projectedControl?: AppFieldControlDirective;

  /** Stable per-instance id base, used for the helper and error ids. */
  private readonly uid = `rq-field-${++nextFieldId}`;

  /** The element we stamped attributes onto, kept so we can clean up on destroy. */
  private target?: HTMLElement;

  get helperId(): string {
    return `${this.uid}-helper`;
  }

  get errorId(): string {
    return `${this.uid}-error`;
  }

  /** The id the `<label for>` points at — the caller's if given, else generated. */
  get labelFor(): string {
    return this.controlId ?? this.uid;
  }

  get required(): boolean {
    return isRequired(this.control);
  }

  /**
   * Errors appear once the user has engaged with the field (`touched`) or a submit
   * has been attempted — never on a pristine, untouched form, which would greet the
   * user with red text on a create form they have not filled in yet.
   */
  get showError(): boolean {
    const control = this.control;
    return !!control && control.invalid && (control.touched || this.submitted);
  }

  get errorMessage(): string | null {
    return this.showError ? firstErrorMessage(this.control, this.errorMessages) : null;
  }

  ngAfterContentInit(): void {
    this.wireControl();
  }

  ngOnDestroy(): void {
    // The projected control can outlive this row (it belongs to the caller's
    // template), so leave nothing of ours stamped on it.
    const target = this.target;
    if (!target) {
      return;
    }
    target.removeAttribute('aria-describedby');
    target.removeAttribute('aria-invalid');
    target.removeAttribute('aria-required');
  }

  /**
   * Stamps id + ARIA onto the projected control.
   *
   * Runs in `ngAfterContentInit` — before the view is first checked — so setting
   * `labelFor`-adjacent state here cannot trigger an
   * `ExpressionChangedAfterItHasBeenCheckedError`.
   *
   * `aria-invalid` and `aria-describedby` are refreshed on every change-detection
   * pass by {@link syncValidationState}, called from the template's `showError`
   * read path; see `ngAfterContentChecked` below.
   */
  private wireControl(): void {
    const projected = this.projectedControl;
    if (!projected) {
      return;
    }
    const target = projected.resolveTarget();
    this.target = target;

    if (!target.getAttribute('id')) {
      target.setAttribute('id', this.labelFor);
    }
    this.syncValidationState();
  }

  /**
   * Keeps `aria-describedby` / `aria-invalid` / `aria-required` in step with the
   * control's validation state. Called from `ngAfterContentChecked` so it tracks
   * every state change (blur, value edit, submit) without a manual subscription.
   */
  ngAfterContentChecked(): void {
    this.syncValidationState();
  }

  private syncValidationState(): void {
    const target = this.target;
    if (!target) {
      return;
    }

    const describedBy = [this.helper ? this.helperId : null, this.showError ? this.errorId : null]
      .filter(Boolean)
      .join(' ');

    if (describedBy) {
      target.setAttribute('aria-describedby', describedBy);
    } else {
      target.removeAttribute('aria-describedby');
    }

    if (this.control) {
      target.setAttribute('aria-invalid', String(this.showError));
    }
    if (this.required) {
      target.setAttribute('aria-required', 'true');
    } else {
      target.removeAttribute('aria-required');
    }
  }
}
