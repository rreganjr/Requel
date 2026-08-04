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
import { AbstractControl, ValidationErrors } from '@angular/forms';

/**
 * Shared validation-message map (issue #158, the N5-sized slice of #132's "unified
 * validation helper").
 *
 * One place decides how a `ValidationErrors` key becomes user-facing text, so nine
 * editors don't each invent their own wording. `app-field` calls
 * {@link firstErrorMessage} to render the inline error under a control; callers
 * override individual messages per field via `app-field`'s `errorMessages` input.
 *
 * #132 extends this during rollout (cross-field, password confirmation). Keep
 * additions here rather than in components.
 */

/** Builds a message from a validation error's payload (e.g. `minlength`'s lengths). */
export type FormErrorMessageFactory = (error: unknown) => string;

/** Per-field overrides accepted by `app-field`: error key -> literal message. */
export type FormErrorOverrides = Record<string, string>;

/**
 * Precedence order. A control can hold several errors at once (`required` and
 * `minlength` never co-occur, but `pattern` and `maxlength` can), and we render
 * exactly one message — so the order must be deterministic rather than depending
 * on `Object.keys` insertion order.
 *
 * `required` comes first because an empty field's other complaints are noise.
 */
const ERROR_PRECEDENCE = ['required', 'minlength', 'maxlength', 'email', 'pattern'] as const;

/** Default wording for the built-in Angular validators. */
export const DEFAULT_FORM_ERRORS: Record<string, FormErrorMessageFactory> = {
  required: () => 'This field is required.',
  minlength: error => {
    const required = (error as { requiredLength?: number } | null)?.requiredLength;
    return required != null
      ? `Must be at least ${required} character${required === 1 ? '' : 's'}.`
      : 'Value is too short.';
  },
  maxlength: error => {
    const required = (error as { requiredLength?: number } | null)?.requiredLength;
    return required != null
      ? `Must be at most ${required} character${required === 1 ? '' : 's'}.`
      : 'Value is too long.';
  },
  email: () => 'Enter a valid email address.',
  pattern: () => 'Value is not in the expected format.',
};

/** Last-resort text for a validator we have no wording for. Never shows a raw key. */
const FALLBACK_MESSAGE = 'This value is not valid.';

/**
 * Resolves the single message to show for `errors`, honouring `overrides` first,
 * then {@link DEFAULT_FORM_ERRORS}, then a generic fallback.
 *
 * Returns `null` when there is nothing to report.
 */
export function resolveErrorMessage(
  errors: ValidationErrors | null | undefined,
  overrides?: FormErrorOverrides
): string | null {
  if (!errors) {
    return null;
  }

  const keys = Object.keys(errors);
  if (keys.length === 0) {
    return null;
  }

  // Known keys in precedence order, then any unknown keys in their own order, so a
  // custom validator still produces a message instead of silently rendering nothing.
  const ordered = [
    ...ERROR_PRECEDENCE.filter(key => keys.includes(key)),
    ...keys.filter(key => !ERROR_PRECEDENCE.includes(key as (typeof ERROR_PRECEDENCE)[number])),
  ];

  for (const key of ordered) {
    const override = overrides?.[key];
    if (override) {
      return override;
    }
    const factory = DEFAULT_FORM_ERRORS[key];
    if (factory) {
      return factory(errors[key]);
    }
  }

  return FALLBACK_MESSAGE;
}

/** Convenience wrapper: {@link resolveErrorMessage} for a control's current errors. */
export function firstErrorMessage(
  control: AbstractControl | null | undefined,
  overrides?: FormErrorOverrides
): string | null {
  return resolveErrorMessage(control?.errors, overrides);
}

/**
 * Whether `control` carries a `required` validator, so `app-field` can derive its
 * required marker and `aria-required` from the validators instead of taking a
 * separate boolean input that can drift out of sync with them.
 *
 * Probes the composed validator with an empty value rather than using
 * `hasValidator(Validators.required)`, because the latter misses `required` when it
 * has been folded into a composed or wrapped validator function.
 */
export function isRequired(control: AbstractControl | null | undefined): boolean {
  const validator = control?.validator;
  if (!validator) {
    return false;
  }
  const result = validator({ value: null } as AbstractControl);
  return !!result?.['required'];
}
