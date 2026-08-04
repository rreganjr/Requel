import { FormControl, Validators } from '@angular/forms';
import {
  DEFAULT_FORM_ERRORS,
  firstErrorMessage,
  isRequired,
  resolveErrorMessage,
} from './form-errors';

describe('form-errors (issue #158)', () => {
  describe('resolveErrorMessage', () => {
    it('returns null when there are no errors', () => {
      expect(resolveErrorMessage(null)).toBeNull();
      expect(resolveErrorMessage(undefined)).toBeNull();
      expect(resolveErrorMessage({})).toBeNull();
    });

    it('prefers required over every other error so an empty field reads cleanly', () => {
      const message = resolveErrorMessage({
        pattern: { requiredPattern: '^a+$', actualValue: '' },
        required: true,
      });
      expect(message).toBe(DEFAULT_FORM_ERRORS['required'](true));
      expect(message).toBe('This field is required.');
    });

    it('renders minlength with the required length', () => {
      expect(resolveErrorMessage({ minlength: { requiredLength: 3, actualLength: 1 } })).toBe(
        'Must be at least 3 characters.'
      );
    });

    it('singularises a one-character minlength', () => {
      expect(resolveErrorMessage({ minlength: { requiredLength: 1, actualLength: 0 } })).toBe(
        'Must be at least 1 character.'
      );
    });

    it('renders maxlength with the required length', () => {
      expect(resolveErrorMessage({ maxlength: { requiredLength: 80, actualLength: 92 } })).toBe(
        'Must be at most 80 characters.'
      );
    });

    it('falls back to generic wording when a payload has no requiredLength', () => {
      expect(resolveErrorMessage({ minlength: null })).toBe('Value is too short.');
      expect(resolveErrorMessage({ maxlength: {} })).toBe('Value is too long.');
    });

    it('lets a per-field override win over the default wording', () => {
      expect(
        resolveErrorMessage({ required: true }, { required: 'A goal needs a name.' })
      ).toBe('A goal needs a name.');
    });

    it('uses the default when an override exists for a different key', () => {
      expect(resolveErrorMessage({ required: true }, { email: 'Bad address.' })).toBe(
        'This field is required.'
      );
    });

    it('never renders a raw validator key for an unknown error', () => {
      const message = resolveErrorMessage({ goalSelfRelation: true });
      expect(message).toBe('This value is not valid.');
      expect(message).not.toContain('goalSelfRelation');
    });

    it('prefers an override for an unknown error over the generic fallback', () => {
      expect(
        resolveErrorMessage(
          { goalSelfRelation: true },
          { goalSelfRelation: 'A goal cannot relate to itself.' }
        )
      ).toBe('A goal cannot relate to itself.');
    });
  });

  describe('firstErrorMessage', () => {
    it('reads the errors off a control', () => {
      const control = new FormControl('', { validators: Validators.required });
      expect(firstErrorMessage(control)).toBe('This field is required.');

      control.setValue('Reduce onboarding time');
      expect(firstErrorMessage(control)).toBeNull();
    });

    it('tolerates a null control', () => {
      expect(firstErrorMessage(null)).toBeNull();
      expect(firstErrorMessage(undefined)).toBeNull();
    });
  });

  describe('isRequired', () => {
    it('detects a bare required validator', () => {
      expect(isRequired(new FormControl('', { validators: Validators.required }))).toBe(true);
    });

    it('detects required inside a composed validator', () => {
      const control = new FormControl('', {
        validators: [Validators.required, Validators.maxLength(80)],
      });
      expect(isRequired(control)).toBe(true);
    });

    it('is false for a control with only non-required validators', () => {
      expect(isRequired(new FormControl('', { validators: Validators.maxLength(80) }))).toBe(
        false
      );
    });

    it('is false for an unvalidated or missing control', () => {
      expect(isRequired(new FormControl(''))).toBe(false);
      expect(isRequired(null)).toBe(false);
    });
  });
});
