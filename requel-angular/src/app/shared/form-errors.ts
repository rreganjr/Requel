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
import { AbstractControl, FormArray, FormGroup, ValidationErrors, ValidatorFn } from '@angular/forms';
import { FieldViolation } from '../models/command';

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

/**
 * Error key used for a message that came back from the server rather than from a
 * client-side validator. Exported as {@link SERVER_ERROR_KEY} below.
 */
const SERVER_ERROR_KEY_INTERNAL = 'server';

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
const ERROR_PRECEDENCE = [
  'required',
  'minlength',
  'maxlength',
  'email',
  'pattern',
  'min',
  'max',
  'integer',
  'atLeastOne',
  'passwordMismatch',
  // Last on purpose. A server complaint is about the value the user already sent, so
  // any live client-side error is newer information and outranks it.
  SERVER_ERROR_KEY_INTERNAL,
] as const;

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
  min: error => {
    const min = (error as { min?: number } | null)?.min;
    return min != null ? `Must be ${min} or more.` : 'Value is too small.';
  },
  max: error => {
    const max = (error as { max?: number } | null)?.max;
    return max != null ? `Must be ${max} or less.` : 'Value is too large.';
  },
  integer: () => 'Enter a whole number.',
  atLeastOne: () => 'Select at least one.',
  passwordMismatch: () => 'Passwords do not match.',
  // The server sends the wording; the payload IS the message.
  [SERVER_ERROR_KEY_INTERNAL]: error => (typeof error === 'string' ? error : FALLBACK_MESSAGE),
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

/**
 * Error key for a message that came back from the server rather than from a
 * client-side validator. See {@link applyCommandErrors}.
 */
export const SERVER_ERROR_KEY = SERVER_ERROR_KEY_INTERNAL;

/**
 * Cross-field validator: the confirm control must equal the password control.
 * Error key: `passwordMismatch`.
 *
 * **Attach this to the group, not to the confirm control.** Two reasons:
 *
 * - A group validator re-runs when *either* control changes. A validator on the
 *   confirm control alone would go stale the moment the user edited the password
 *   field after confirming it, leaving a valid form showing a mismatch (or worse, an
 *   invalid one showing nothing).
 * - The group is where the relationship lives; neither control owns it.
 *
 * It then *also* stamps the error onto the confirm control, because
 * `app-field.showError` reads `control.invalid` and a group-level error does not
 * mark its children invalid — without this the message would render nowhere at all
 * while the form sat there refusing to submit. The confirm field is where the user
 * has to act, so that is where the message belongs.
 *
 * Only the `passwordMismatch` key is added or removed; any other error on the
 * confirm control is preserved.
 */
export function passwordsMatch(passwordKey: string, confirmKey: string): ValidatorFn {
  return group => {
    const password = group.get(passwordKey);
    const confirm = group.get(confirmKey);

    // A group missing either control is a wiring mistake, not a mismatch — staying
    // silent here beats blocking a form the user cannot fix.
    if (!password || !confirm) {
      return null;
    }

    const mismatch = password.value !== confirm.value;
    setErrorKey(confirm, 'passwordMismatch', mismatch ? true : null);
    return mismatch ? { passwordMismatch: true } : null;
  };
}

/**
 * Validator for numeric controls that must be whole numbers. Error key: `integer`.
 *
 * Empty is left to `required` — a validator that also complained about blankness
 * would produce two messages for one mistake, and `required` outranks this in
 * {@link ERROR_PRECEDENCE} anyway.
 */
export function integer(): ValidatorFn {
  return control => {
    const value = control.value;
    if (value == null || value === '') {
      return null;
    }
    const parsed = Number(value);
    return Number.isInteger(parsed) ? null : { integer: true };
  };
}

/**
 * Validator for multi-select / checkbox-group controls: the value must hold at least
 * one selection. Error key: `atLeastOne`.
 *
 * Mirrors `UserImpl`'s `@Size(min = 1)` on roles — the one bean-validation size
 * constraint the backend already enforces — so the form refuses a save the server
 * would reject anyway.
 *
 * Counts arrays and Sets by size and treats `null` / `undefined` / `''` as empty.
 * Any other non-empty value counts as one selection, so a single-select control
 * bound to the same validator behaves sensibly.
 */
export function atLeastOne(): ValidatorFn {
  return control => (isEmptySelection(control.value) ? { atLeastOne: true } : null);
}

/**
 * The few field names the server cannot disambiguate to a single control on its own. Issue #176 made
 * `CommandController` emit input-DTO field names (see `@FromEntityProperty`), retiring the per-editor
 * maps; the one residue is project-editor's single `organization` control, which two DTO fields
 * (`organizationId` + `organizationName`) feed. This shared alias routes both to it.
 */
const SHARED_FIELD_ALIASES: Record<string, string> = {
  organizationId: 'organization',
  organizationName: 'organization',
};

/**
 * Maps a failed command's field violations onto the controls of `form`.
 *
 * Each violation whose field resolves to a control sets `{ server: message }` on
 * that control, so the server's complaint renders inline under the field it is
 * about — the same place a client-side error appears — instead of being
 * semicolon-joined into one page-level string, which is what
 * `project-editor` / `user-editor` / `edit-account` do today.
 *
 * **Anything that does not resolve is returned rather than dropped.** Feed the
 * return value to the editor's page-level error so a violation is never silently
 * swallowed:
 *
 * ```ts
 * const unresolved = applyCommandErrors(this.form, result.violations);
 * this.errorMessage.set(unresolved.join(' ') || null);
 * ```
 *
 * Since #176, `CommandController` emits **input-DTO field names** (via `@FromEntityProperty` on
 * the divergent DTO fields), so a violation's field usually matches a control directly and the old
 * per-editor `{ entityProperty: controlName }` maps are gone. The one residue is a small shared
 * alias ({@link SHARED_FIELD_ALIASES}) for project-editor's composite `organization` control, which
 * two DTO fields (`organizationId` + `organizationName`) feed. A field that resolves to nothing
 * still degrades to page-level display rather than losing the message.
 *
 * A dotted or indexed path (`roles[0].name`) is tried whole, then with indices
 * stripped, then as its last segment, so a nested constraint still finds its
 * control when the form is flat.
 *
 * Controls receiving an error are marked touched, so the message shows even on a
 * field the user never focused — a save they triggered is engagement enough.
 *
 * **The error is attached as a validator, not written with `setErrors`.** That is not a
 * stylistic choice. Angular's `updateValueAndValidity` reassigns `errors` from the
 * validator result, so anything written directly is dropped the next time validation
 * runs — and validation runs on *render*, not just on edit: `setUpControl` revalidates
 * whenever a `[formControl]` directive initialises. A control bound that way (user-editor
 * binds its role and permission checkboxes to shared controls exactly like this) would
 * lose the server's complaint on the very next change-detection pass, before the user
 * ever saw it. As a validator it survives revalidation, and still self-clears the moment
 * the value changes, because the validator compares against a snapshot of the value the
 * server rejected.
 */
export function applyCommandErrors(
  form: FormGroup,
  violations: FieldViolation[] | null | undefined
): string[] {
  const unresolved: string[] = [];
  if (!violations?.length) {
    return unresolved;
  }

  for (const violation of violations) {
    // A null field is a command-level failure, not a field one.
    const control = violation.field ? resolveViolationControl(form, violation.field) : null;
    if (!control) {
      unresolved.push(violation.message);
      continue;
    }
    setServerError(control, violation.message);
  }

  return unresolved;
}

/**
 * Clears every `server` error under `root`.
 *
 * Angular drops these on its own as soon as a control's value changes —
 * `updateValueAndValidity` reassigns `errors` from the validator result, which never
 * includes `server` — so an editor does not need to wire anything up for the
 * "error goes away once I fix the field" behaviour. This is for the other case:
 * clearing stale server errors *before* re-submitting, so a second save attempt
 * starts from a clean slate rather than showing last attempt's complaints next to
 * this attempt's.
 */
export function clearServerErrors(root: AbstractControl): void {
  for (const control of leafControls(root)) {
    clearServerError(control);
  }
}

/**
 * The server-error validator currently attached to a control, if any, so a later call can
 * take it off again. A WeakMap rather than a field on the control: nothing of ours should
 * outlive the control or need cleaning up when a form is discarded.
 */
const serverValidators = new WeakMap<AbstractControl, ValidatorFn>();

/** Stable comparison key for a control value, covering the array-valued controls too. */
function valueSnapshot(value: unknown): string {
  return JSON.stringify(value ?? null);
}

/**
 * Attaches `{ server: message }` to a control as a validator bound to the value the
 * server rejected. It reports while the value is unchanged and returns null once the user
 * edits, which is the "fix the field and the message goes" behaviour — implemented so it
 * also survives the revalidation that a render triggers.
 */
function setServerError(control: AbstractControl, message: string): void {
  clearServerError(control);

  const rejected = valueSnapshot(control.value);
  const validator: ValidatorFn = candidate =>
    valueSnapshot(candidate.value) === rejected ? { [SERVER_ERROR_KEY]: message } : null;

  serverValidators.set(control, validator);
  control.addValidators(validator);
  control.updateValueAndValidity({ emitEvent: false });
  control.markAsTouched();
}

/** Removes the server-error validator from a control, if it has one. */
function clearServerError(control: AbstractControl): void {
  const existing = serverValidators.get(control);
  if (!existing) {
    return;
  }
  serverValidators.delete(control);
  control.removeValidators(existing);
  control.updateValueAndValidity({ emitEvent: false });
}

/**
 * Adds or removes a single key on a control's error object, leaving its other errors
 * alone. Passing `null` removes the key; removing the last key resets `errors` to
 * `null`, which is what Angular expects for a valid control.
 */
function setErrorKey(control: AbstractControl, key: string, value: unknown): void {
  const current = control.errors;
  const hasKey = current?.[key] != null;

  if (value == null) {
    if (!hasKey) {
      return;
    }
    const next = { ...current };
    delete next[key];
    control.setErrors(Object.keys(next).length ? next : null);
    return;
  }

  if (current?.[key] === value) {
    return;
  }
  control.setErrors({ ...(current ?? {}), [key]: value });
}

/** Whether a selection-style value holds nothing. */
function isEmptySelection(value: unknown): boolean {
  if (value == null || value === '') {
    return true;
  }
  if (Array.isArray(value)) {
    return value.length === 0;
  }
  if (value instanceof Set || value instanceof Map) {
    return value.size === 0;
  }
  return false;
}

/**
 * Resolves a violation's field name to a control, trying the explicit map first and
 * then progressively looser forms of the path.
 */
function resolveViolationControl(
  form: FormGroup,
  field: string
): AbstractControl | null {
  const withoutIndices = field.replace(/\[\d+\]/g, '');
  const leaf = withoutIndices.split('.').pop() ?? withoutIndices;

  const candidates = [
    SHARED_FIELD_ALIASES[field],
    SHARED_FIELD_ALIASES[withoutIndices],
    field,
    withoutIndices,
    leaf,
  ];

  for (const candidate of candidates) {
    if (!candidate) {
      continue;
    }
    const control = form.get(candidate);
    if (control) {
      return control;
    }
  }
  return null;
}

/** Every control under `root` that is not itself a group or array. */
function* leafControls(root: AbstractControl): Generator<AbstractControl> {
  if (root instanceof FormGroup) {
    for (const child of Object.values(root.controls)) {
      yield* leafControls(child);
    }
    return;
  }
  if (root instanceof FormArray) {
    for (const child of root.controls) {
      yield* leafControls(child);
    }
    return;
  }
  yield root;
}
