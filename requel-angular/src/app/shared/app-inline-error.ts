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
import { Component, Input, ChangeDetectionStrategy, ChangeDetectorRef, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AbstractControl } from '@angular/forms';
import { FormErrorOverrides, firstErrorMessage } from './form-errors';

/**
 * Inline validation message for the compact mini-form add-rows (issue #134).
 *
 * `app-field` (#158) owns the two-column labelled form row; these add-rows keep the
 * horizontal #138 fieldset layout, so they need the same validation *contract* without
 * that layout. This component is the shared piece: it renders the single inline message
 * (`.rq-field-error`, `role="alert"`) and, crucially, owns the show-contract in one place
 * — errors appear only once the control is touched or a submit was attempted, matching
 * `app-field`'s `showError`.
 *
 * The host input wires its own `aria-invalid` / `aria-describedby` off this instance via a
 * template ref, so both the visual message and the ARIA state read from the same
 * `message()` and never drift:
 *
 * ```html
 * <input pInputText formControlName="value"
 *        [attr.aria-invalid]="valErr.message() ? 'true' : null"
 *        [attr.aria-describedby]="valErr.message() ? errorId : null" />
 * <app-inline-error #valErr [control]="form.controls.value" [id]="errorId"
 *                   [submitted]="submitted()" [overrides]="{ required: 'Value is required.' }"
 *                   testid="value-error" />
 * ```
 */
@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-inline-error',
  standalone: true,
  template: `
    @if (message()) {
      <p class="rq-field-error" [id]="id" role="alert" [attr.data-testid]="testid">{{ message() }}</p>
    }
  `,
})
export class InlineErrorComponent {
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly destroyRef = inject(DestroyRef);
  private _control!: AbstractControl;
  /** The control whose validity drives the message. */
  @Input({ required: true })
  set control(c: AbstractControl) {
    this._control = c;
    // OnPush: the message is derived from FormControl state (touched/validity),
    // which is not a signal — mark for check whenever the control emits.
    c?.events?.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => this.cdr.markForCheck());
  }
  get control(): AbstractControl {
    return this._control;
  }
  /** Stable id for the message element, so the input can point `aria-describedby` at it. */
  @Input({ required: true }) id!: string;
  /** Optional test id stamped on the message element. */
  @Input() testid?: string;
  /** Set true once a submit was attempted, so the message shows on an untouched control too. */
  @Input() submitted = false;
  /** Per-field wording overrides (e.g. `{ required: 'Value is required.' }`). */
  @Input() overrides?: FormErrorOverrides;

  /**
   * The message to show, or `null` when nothing should render. Errors surface only once the
   * user has engaged with the control (`touched`) or a submit has been attempted — never on a
   * pristine, untouched form. Callers bind both the inline text and the input's ARIA state to
   * this, so the two stay in lockstep.
   */
  message(): string | null {
    const control = this.control;
    if (!control || !control.invalid || !(control.touched || this.submitted)) {
      return null;
    }
    return firstErrorMessage(control, this.overrides);
  }
}
