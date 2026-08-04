import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { AppFieldComponent, AppFieldControlDirective } from './app-field';

@Component({
  standalone: true,
  imports: [AppFieldComponent, AppFieldControlDirective, ReactiveFormsModule],
  template: `
    <app-field
      [label]="label"
      [helper]="helper"
      [control]="control"
      [divider]="divider"
      [errorMessages]="errorMessages"
      [submitted]="submitted"
    >
      <input appFieldControl [formControl]="control" data-testid="control" />
    </app-field>`,
})
class NativeHostComponent {
  label = 'Name';
  helper = '';
  divider = true;
  submitted = false;
  errorMessages?: Record<string, string>;
  control = new FormControl('', { validators: Validators.required, nonNullable: true });
}

/**
 * Stands in for a PrimeNG wrapper such as `p-select`: the element carrying
 * `appFieldControl` is not itself focusable and holds the real control inside.
 */
@Component({
  standalone: true,
  imports: [AppFieldComponent, AppFieldControlDirective, ReactiveFormsModule],
  template: `
    <app-field label="Type" [control]="control">
      <div appFieldControl data-testid="wrapper">
        <span>not focusable</span>
        <input data-testid="inner" [formControl]="control" />
      </div>
    </app-field>`,
})
class WrapperHostComponent {
  control = new FormControl('', { nonNullable: true });
}

@Component({
  standalone: true,
  imports: [AppFieldComponent, AppFieldControlDirective, ReactiveFormsModule],
  template: `
    <app-field label="Type" controlId="story-type" [control]="control">
      <div appFieldControl>
        <input id="story-type" data-testid="inner" [formControl]="control" />
      </div>
    </app-field>`,
})
class ExplicitIdHostComponent {
  control = new FormControl('', { nonNullable: true });
}

describe('AppFieldComponent (issue #158)', () => {
  beforeEach(() => {
    // Configured once per test so a single test can create more than one fixture;
    // re-calling configureTestingModule after instantiation throws.
    TestBed.configureTestingModule({
      imports: [NativeHostComponent, WrapperHostComponent, ExplicitIdHostComponent],
    });
  });

  /**
   * Host inputs are set BEFORE the first change-detection pass. Mutating them
   * afterwards raises NG0100 under the zoneless TestBed, because a plain field
   * assignment does not mark the host dirty and detectChanges goes straight to its
   * verification pass. Control state (touched/value) is safe to change mid-test.
   */
  function render(
    configure?: (host: NativeHostComponent) => void
  ): ComponentFixture<NativeHostComponent> {
    const fixture = TestBed.createComponent(NativeHostComponent);
    configure?.(fixture.componentInstance);
    fixture.detectChanges();
    return fixture;
  }

  const control = (el: HTMLElement) => el.querySelector<HTMLElement>('[data-testid="control"]');
  const fieldError = (el: HTMLElement) => el.querySelector('[data-testid="field-error"]');

  it('renders the label as a real <label> bound to the projected control', () => {
    const el: HTMLElement = render().nativeElement;

    const label = el.querySelector('label.app-field-label');
    expect(label?.textContent?.trim()).toContain('Name');
    expect(control(el)?.id).toBeTruthy();
    expect(label?.getAttribute('for')).toBe(control(el)?.id);
  });

  it('derives the required marker and aria-required from the control validators', () => {
    const el: HTMLElement = render().nativeElement;

    expect(el.querySelector('.app-field-required')).not.toBeNull();
    expect(control(el)?.getAttribute('aria-required')).toBe('true');
  });

  it('omits the required marker and aria-required for an optional field', () => {
    const el: HTMLElement = render(host => {
      host.control = new FormControl('', { nonNullable: true });
    }).nativeElement;

    expect(el.querySelector('.app-field-required')).toBeNull();
    expect(control(el)?.hasAttribute('aria-required')).toBe(false);
  });

  it('shows no error on a pristine untouched control', () => {
    const el: HTMLElement = render().nativeElement;

    expect(fieldError(el)).toBeNull();
    expect(control(el)?.getAttribute('aria-invalid')).toBe('false');
  });

  it('shows the error once the control is touched, and sets aria-invalid', () => {
    const fixture = render();
    fixture.componentInstance.control.markAsTouched();
    fixture.detectChanges();
    const el: HTMLElement = fixture.nativeElement;

    expect(fieldError(el)?.textContent?.trim()).toBe('This field is required.');
    expect(control(el)?.getAttribute('aria-invalid')).toBe('true');
  });

  it('shows the error on an untouched control once a submit has been attempted', () => {
    const el: HTMLElement = render(host => {
      host.submitted = true;
    }).nativeElement;

    expect(fieldError(el)).not.toBeNull();
  });

  it('honours a per-field error override', () => {
    const el: HTMLElement = render(host => {
      host.errorMessages = { required: 'A goal needs a name.' };
      host.control.markAsTouched();
    }).nativeElement;

    expect(fieldError(el)?.textContent?.trim()).toBe('A goal needs a name.');
  });

  it('clears the error and aria-invalid once the control becomes valid', () => {
    const fixture = render(host => host.control.markAsTouched());
    fixture.componentInstance.control.setValue('Reduce onboarding time');
    fixture.detectChanges();
    const el: HTMLElement = fixture.nativeElement;

    expect(fieldError(el)).toBeNull();
    expect(control(el)?.getAttribute('aria-invalid')).toBe('false');
  });

  it('links helper text via aria-describedby', () => {
    const el: HTMLElement = render(host => {
      host.helper = 'Short, outcome-focused.';
    }).nativeElement;

    const helper = el.querySelector('.app-field-helper');
    expect(helper?.id).toBeTruthy();
    expect(control(el)?.getAttribute('aria-describedby')).toBe(helper?.id);
  });

  it('describes the control by both helper and error while invalid', () => {
    const el: HTMLElement = render(host => {
      host.helper = 'Short, outcome-focused.';
      host.control.markAsTouched();
    }).nativeElement;

    const helperId = el.querySelector('.app-field-helper')?.id;
    const errorId = fieldError(el)?.id;
    const describedBy = control(el)?.getAttribute('aria-describedby') ?? '';

    expect(describedBy.split(' ')).toEqual([helperId, errorId]);
  });

  it('renders the divider by default and omits it on request', () => {
    const withDivider: HTMLElement = render().nativeElement;
    const withoutDivider: HTMLElement = render(host => {
      host.divider = false;
    }).nativeElement;

    expect(
      withDivider.querySelector('.app-field')?.classList.contains('app-field-bordered')
    ).toBe(true);
    expect(
      withoutDivider.querySelector('.app-field')?.classList.contains('app-field-bordered')
    ).toBe(false);
  });

  it('generates unique ids across rows so two fields never collide', () => {
    const first: HTMLElement = render().nativeElement;
    const second: HTMLElement = render().nativeElement;

    expect(control(first)?.id).toBeTruthy();
    expect(control(first)?.id).not.toBe(control(second)?.id);
  });

  describe('wrapper controls', () => {
    it('stamps id and ARIA on the inner focusable, not the non-focusable wrapper', () => {
      const fixture = TestBed.createComponent(WrapperHostComponent);
      fixture.detectChanges();
      const el: HTMLElement = fixture.nativeElement;

      const wrapper = el.querySelector<HTMLElement>('[data-testid="wrapper"]');
      const inner = el.querySelector<HTMLElement>('[data-testid="inner"]');

      expect(inner?.id).toBeTruthy();
      expect(wrapper?.hasAttribute('id')).toBe(false);
      expect(el.querySelector('label')?.getAttribute('for')).toBe(inner?.id);
    });

    it('leaves a caller-supplied controlId in place instead of generating one', () => {
      const fixture = TestBed.createComponent(ExplicitIdHostComponent);
      fixture.detectChanges();
      const el: HTMLElement = fixture.nativeElement;

      expect(el.querySelector<HTMLElement>('[data-testid="inner"]')?.id).toBe('story-type');
      expect(el.querySelector('label')?.getAttribute('for')).toBe('story-type');
    });
  });
});
