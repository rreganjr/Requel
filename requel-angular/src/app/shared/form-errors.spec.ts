import {
  AbstractControl,
  FormArray,
  FormControl,
  FormGroup,
  Validators,
} from '@angular/forms';
import {
  applyCommandErrors,
  atLeastOne,
  clearServerErrors,
  DEFAULT_FORM_ERRORS,
  firstErrorMessage,
  isRequired,
  passwordsMatch,
  resolveErrorMessage,
  SERVER_ERROR_KEY,
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

describe('passwordsMatch (issue #132)', () => {
  function group() {
    return new FormGroup(
      {
        password: new FormControl('', { nonNullable: true }),
        repassword: new FormControl('', { nonNullable: true }),
      },
      { validators: passwordsMatch('password', 'repassword') }
    );
  }

  it('passes when both controls are empty', () => {
    expect(group().errors).toBeNull();
  });

  it('passes when the values match', () => {
    const form = group();
    form.setValue({ password: 'hunter2', repassword: 'hunter2' });
    expect(form.errors).toBeNull();
  });

  it('reports passwordMismatch on the group when the values differ', () => {
    const form = group();
    form.setValue({ password: 'hunter2', repassword: 'hunter3' });
    expect(form.errors).toEqual({ passwordMismatch: true });
  });

  /**
   * The reason the validator stamps the child as well as returning a group error:
   * `app-field.showError` reads `control.invalid`, and a group error does not mark
   * children invalid, so without this the form would refuse to submit while showing
   * no message anywhere.
   */
  it('also stamps the error on the confirm control, so app-field can render it', () => {
    const form = group();
    form.setValue({ password: 'hunter2', repassword: 'hunter3' });

    expect(form.controls.repassword.errors).toEqual({ passwordMismatch: true });
    expect(form.controls.repassword.invalid).toBe(true);
    expect(firstErrorMessage(form.controls.repassword)).toBe('Passwords do not match.');
  });

  it('leaves the password control alone', () => {
    const form = group();
    form.setValue({ password: 'hunter2', repassword: 'hunter3' });
    expect(form.controls.password.errors).toBeNull();
  });

  it('clears the stamped error once the values match again', () => {
    const form = group();
    form.setValue({ password: 'hunter2', repassword: 'hunter3' });
    form.controls.repassword.setValue('hunter2');

    expect(form.errors).toBeNull();
    expect(form.controls.repassword.errors).toBeNull();
    expect(form.controls.repassword.valid).toBe(true);
  });

  /**
   * A validator on the confirm control alone would go stale here: the user confirms,
   * then edits the password. A group validator re-runs on either child's change.
   */
  it('re-evaluates when the password changes after the confirm was entered', () => {
    const form = group();
    form.setValue({ password: 'hunter2', repassword: 'hunter2' });
    expect(form.errors).toBeNull();

    form.controls.password.setValue('hunter3');

    expect(form.errors).toEqual({ passwordMismatch: true });
    expect(form.controls.repassword.errors).toEqual({ passwordMismatch: true });
  });

  it('preserves other errors on the confirm control', () => {
    const form = new FormGroup(
      {
        password: new FormControl('', { nonNullable: true }),
        repassword: new FormControl('', {
          validators: Validators.minLength(4),
          nonNullable: true,
        }),
      },
      { validators: passwordsMatch('password', 'repassword') }
    );

    form.setValue({ password: 'hunter2', repassword: 'abc' });

    expect(form.controls.repassword.errors).toMatchObject({ passwordMismatch: true });
    expect(form.controls.repassword.errors?.['minlength']).toBeTruthy();
    // minlength outranks passwordMismatch, so the user is told the fixable thing first.
    expect(firstErrorMessage(form.controls.repassword)).toBe('Must be at least 4 characters.');
  });

  it('stays silent when either control is missing rather than blocking the form', () => {
    const form = new FormGroup(
      { password: new FormControl('', { nonNullable: true }) },
      { validators: passwordsMatch('password', 'repassword') }
    );
    expect(form.errors).toBeNull();
    expect(form.valid).toBe(true);
  });
});

describe('atLeastOne (issue #132)', () => {
  const validate = (value: unknown) =>
    atLeastOne()(new FormControl(value) as AbstractControl);

  it.each([null, undefined, '', [], new Set()])('reports atLeastOne for %p', value => {
    expect(validate(value)).toEqual({ atLeastOne: true });
  });

  it.each([['admin'], ['admin', 'user'], new Set(['admin']), 'admin', 0, false])(
    'passes for %p',
    value => {
      expect(validate(value)).toBeNull();
    }
  );

  it('renders the shared message', () => {
    const control = new FormControl<string[]>([], { validators: atLeastOne() });
    expect(firstErrorMessage(control)).toBe('Select at least one.');
  });
});

describe('applyCommandErrors (issue #132)', () => {
  function form() {
    return new FormGroup({
      name: new FormControl('', { nonNullable: true }),
      email: new FormControl('', { nonNullable: true }),
      password: new FormControl('', { nonNullable: true }),
    });
  }

  it('returns an empty list and touches nothing for no violations', () => {
    const f = form();
    expect(applyCommandErrors(f, null)).toEqual([]);
    expect(applyCommandErrors(f, undefined)).toEqual([]);
    expect(applyCommandErrors(f, [])).toEqual([]);
    expect(f.touched).toBe(false);
  });

  it('sets a server error on the control a violation names', () => {
    const f = form();
    const unresolved = applyCommandErrors(f, [{ field: 'name', message: 'Name already exists.' }]);

    expect(unresolved).toEqual([]);
    expect(f.controls.name.errors).toEqual({ server: 'Name already exists.' });
    expect(firstErrorMessage(f.controls.name)).toBe('Name already exists.');
  });

  it('marks the control touched so the message shows on a field never focused', () => {
    const f = form();
    applyCommandErrors(f, [{ field: 'name', message: 'Name already exists.' }]);
    expect(f.controls.name.touched).toBe(true);
  });

  it('returns a violation whose field does not resolve, instead of dropping it', () => {
    const f = form();
    const unresolved = applyCommandErrors(f, [
      { field: 'noSuchProperty', message: 'Something about a field we do not have.' },
    ]);

    expect(unresolved).toEqual(['Something about a field we do not have.']);
  });

  it('returns a command-level violation with a null field', () => {
    const f = form();
    const unresolved = applyCommandErrors(f, [{ field: null, message: 'Validation failed.' }]);

    expect(unresolved).toEqual(['Validation failed.']);
    expect(f.touched).toBe(false);
  });

  it('routes the shared organization aliases onto the single organization control (#176)', () => {
    const f = new FormGroup({
      name: new FormControl('', { nonNullable: true }),
      organization: new FormControl<string | null>(null),
    });
    const unresolved = applyCommandErrors(f, [
      { field: 'organizationName', message: 'Organization name is too long.' },
    ]);

    expect(unresolved).toEqual([]);
    expect(f.controls.organization.errors).toEqual({ server: 'Organization name is too long.' });
  });

  it('falls back to the last segment of a dotted, indexed path', () => {
    const f = form();
    const unresolved = applyCommandErrors(f, [
      { field: 'roles[0].name', message: 'Role name is invalid.' },
    ]);

    expect(unresolved).toEqual([]);
    expect(f.controls.name.errors).toEqual({ server: 'Role name is invalid.' });
  });

  it('applies several violations at once', () => {
    const f = form();
    const unresolved = applyCommandErrors(f, [
      { field: 'name', message: 'Name is required.' },
      { field: 'email', message: 'Email is malformed.' },
      { field: 'mystery', message: 'Unmapped.' },
    ]);

    expect(f.controls.name.errors).toEqual({ server: 'Name is required.' });
    expect(f.controls.email.errors).toEqual({ server: 'Email is malformed.' });
    expect(unresolved).toEqual(['Unmapped.']);
  });

  /**
   * The behaviour the "fix the field and the error goes away" requirement rests on.
   * Angular's `updateValueAndValidity` reassigns `errors` from the validator result on
   * every value change, and no validator produces `server` — so the key drops on its
   * own. Asserted rather than assumed: if this ever stopped holding, editors would
   * strand users behind a stale error with Save disabled.
   */
  it('drops the server error on the next edit to that control', () => {
    const f = form();
    applyCommandErrors(f, [{ field: 'name', message: 'Name already exists.' }]);
    expect(f.controls.name.errors).toEqual({ server: 'Name already exists.' });

    f.controls.name.setValue('A different name');

    expect(f.controls.name.errors).toBeNull();
    expect(f.controls.name.valid).toBe(true);
  });

  it('drops the server error on edit even when the control has its own validators', () => {
    const f = new FormGroup({
      name: new FormControl('', { validators: Validators.required, nonNullable: true }),
    });
    applyCommandErrors(f, [{ field: 'name', message: 'Name already exists.' }]);

    f.controls.name.setValue('A different name');

    expect(f.controls.name.errors).toBeNull();
  });

  it('leaves a live client-side error winning over a server one', () => {
    const f = new FormGroup({
      name: new FormControl('', { validators: Validators.required, nonNullable: true }),
    });
    // Empty and required, so `required` is live; the server also complained.
    applyCommandErrors(f, [{ field: 'name', message: 'Name already exists.' }]);

    expect(f.controls.name.errors).toMatchObject({ required: true, server: 'Name already exists.' });
    expect(firstErrorMessage(f.controls.name)).toBe('This field is required.');
  });

  it('preserves an existing error when adding a server one', () => {
    const f = new FormGroup({
      name: new FormControl('', { validators: Validators.minLength(5), nonNullable: true }),
    });
    f.controls.name.setValue('abc');
    applyCommandErrors(f, [{ field: 'name', message: 'Name already exists.' }]);

    expect(f.controls.name.errors?.['minlength']).toBeTruthy();
    expect(f.controls.name.errors?.['server']).toBe('Name already exists.');
  });
});

describe('clearServerErrors (issue #132)', () => {
  it('removes server errors across a nested form and leaves others alone', () => {
    const f = new FormGroup({
      name: new FormControl('', { validators: Validators.required, nonNullable: true }),
      nested: new FormGroup({
        email: new FormControl('', { nonNullable: true }),
      }),
      roles: new FormArray([new FormControl('', { nonNullable: true })]),
    });

    applyCommandErrors(f, [
      { field: 'name', message: 'Name already exists.' },
      { field: 'nested.email', message: 'Email in use.' },
    ]);
    expect(f.controls.name.errors?.['server']).toBe('Name already exists.');

    clearServerErrors(f);

    // `required` is a live client error and survives; the server keys are gone.
    expect(f.controls.name.errors).toEqual({ required: true });
    expect(f.controls.nested.controls.email.errors).toBeNull();
  });

  it('is a no-op when there is nothing to clear', () => {
    const f = new FormGroup({ name: new FormControl('a', { nonNullable: true }) });
    clearServerErrors(f);
    expect(f.controls.name.errors).toBeNull();
    expect(f.valid).toBe(true);
  });
});

describe('SERVER_ERROR_KEY precedence (issue #132)', () => {
  it('renders a server message when it is the only error', () => {
    const control = new FormControl('x', { nonNullable: true });
    control.setErrors({ [SERVER_ERROR_KEY]: 'Server said no.' });
    expect(firstErrorMessage(control)).toBe('Server said no.');
  });

  it('loses to every client-side validator error', () => {
    const control = new FormControl('', { nonNullable: true });

    control.setErrors({ [SERVER_ERROR_KEY]: 'Server said no.', required: true });
    expect(firstErrorMessage(control)).toBe('This field is required.');

    control.setErrors({ [SERVER_ERROR_KEY]: 'Server said no.', email: true });
    expect(firstErrorMessage(control)).toBe('Enter a valid email address.');

    control.setErrors({ [SERVER_ERROR_KEY]: 'Server said no.', passwordMismatch: true });
    expect(firstErrorMessage(control)).toBe('Passwords do not match.');
  });

  it('falls back rather than rendering a non-string payload', () => {
    const control = new FormControl('x', { nonNullable: true });
    control.setErrors({ [SERVER_ERROR_KEY]: { unexpected: true } });
    expect(firstErrorMessage(control)).toBe('This value is not valid.');
  });

  it('honours a per-field override', () => {
    const control = new FormControl('x', { nonNullable: true });
    control.setErrors({ [SERVER_ERROR_KEY]: 'Server said no.' });
    expect(firstErrorMessage(control, { server: 'Overridden.' })).toBe('Overridden.');
  });
});

describe('min / max messages (issue #132)', () => {
  it('renders the min bound for the settings project limit', () => {
    const control = new FormControl(0, { validators: Validators.min(1) });
    expect(firstErrorMessage(control)).toBe('Must be 1 or more.');
  });

  it('renders the max bound', () => {
    const control = new FormControl(9, { validators: Validators.max(5) });
    expect(firstErrorMessage(control)).toBe('Must be 5 or less.');
  });
});
