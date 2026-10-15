import { Component, viewChild } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import {
  AppFormWizardComponent,
  AppWizardStepComponent,
  WizardCommitRequest,
} from './app-form-wizard';

@Component({
  standalone: true,
  imports: [AppFormWizardComponent, AppWizardStepComponent, ReactiveFormsModule],
  template: `
    <app-form-wizard
      [(activeKey)]="activeKey"
      (stepCommit)="onCommit($event)"
      (cancelled)="cancelled = true"
      (finished)="finished = true"
    >
      <app-wizard-step key="details" label="Details" [form]="detailsForm">
        <ng-template>
          <input data-testid="name" [formControl]="detailsForm.controls.name" />
        </ng-template>
      </app-wizard-step>

      <app-wizard-step key="tags" label="Tags" [optional]="true">
        <ng-template>
          <p data-testid="tags-body">tag selector</p>
        </ng-template>
      </app-wizard-step>

      <app-wizard-step key="relations" label="Relations" [optional]="true">
        <ng-template>
          <p data-testid="relations-body">relation picker</p>
        </ng-template>
      </app-wizard-step>
    </app-form-wizard>`,
})
class WizardHostComponent {
  readonly wizard = viewChild.required(AppFormWizardComponent);

  activeKey = 'details';
  cancelled = false;
  finished = false;

  /** Commits seen, in order, so tests can assert what the host was asked to save. */
  commits: string[] = [];

  /** Set to a message to make the next commit fail. */
  failWith: string | null = null;

  /** Set true to leave the commit unresolved, mimicking an in-flight request. */
  hang = false;

  detailsForm = new FormGroup({
    name: new FormControl('', { validators: Validators.required, nonNullable: true }),
  });

  onCommit(request: WizardCommitRequest): void {
    this.commits.push(request.step.key);
    if (this.hang) {
      return;
    }
    if (this.failWith) {
      request.fail(this.failWith);
      return;
    }
    request.complete();
  }
}

describe('AppFormWizardComponent (issue #158)', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [WizardHostComponent] });
  });

  function render(
    configure?: (host: WizardHostComponent) => void
  ): ComponentFixture<WizardHostComponent> {
    const fixture = TestBed.createComponent(WizardHostComponent);
    configure?.(fixture.componentInstance);
    fixture.detectChanges();
    return fixture;
  }

  const q = (fixture: ComponentFixture<WizardHostComponent>, testid: string) =>
    (fixture.nativeElement as HTMLElement).querySelector<HTMLElement>(`[data-testid="${testid}"]`);

  /**
   * `p-button` puts our `data-testid` on its host element and renders the real
   * `<button>` inside, so descend one level for click and disabled state — the same
   * pattern as `empty-state.spec.ts` / `error-state.spec.ts`.
   */
  const continueButton = (fixture: ComponentFixture<WizardHostComponent>) =>
    (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>(
      '[data-testid="wizard-continue"] button'
    );

  const cancelButton = (fixture: ComponentFixture<WizardHostComponent>) =>
    (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>(
      '[data-testid="wizard-cancel"] button'
    );

  /** A step-nav button. Hand-rolled, so the testid is on the <button> itself. */
  const navButton = (fixture: ComponentFixture<WizardHostComponent>, key: string) =>
    (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>(
      `[data-testid="wizard-step-${key}"]`
    );

  const navButtons = (fixture: ComponentFixture<WizardHostComponent>) =>
    Array.from(
      (fixture.nativeElement as HTMLElement).querySelectorAll<HTMLButtonElement>(
        '[data-testid^="wizard-step-"]'
      )
    );

  function pressKey(button: HTMLButtonElement | null, key: string): void {
    button?.dispatchEvent(new KeyboardEvent('keydown', { key, bubbles: true }));
  }

  function clickContinue(fixture: ComponentFixture<WizardHostComponent>): void {
    continueButton(fixture)?.click();
    fixture.detectChanges();
  }

  it('renders a nav entry for every declared step', () => {
    const fixture = render();

    expect(q(fixture, 'wizard-step-details')).not.toBeNull();
    expect(q(fixture, 'wizard-step-tags')).not.toBeNull();
    expect(q(fixture, 'wizard-step-relations')).not.toBeNull();
  });

  it('starts on the first step and renders only its body', () => {
    const fixture = render();

    expect(q(fixture, 'name')).not.toBeNull();
    expect(q(fixture, 'tags-body')).toBeNull();
  });

  it('defaults activeKey to the first step when the host supplies none', () => {
    const fixture = render(host => {
      host.activeKey = undefined as unknown as string;
    });

    expect(fixture.componentInstance.wizard().activeKey).toBe('details');
  });

  it('labels the last step Done and the others Continue', () => {
    const fixture = render();
    expect(continueButton(fixture)?.textContent).toContain('Continue');

    const onLast = render(host => {
      host.activeKey = 'relations';
    });
    expect(continueButton(onLast)?.textContent).toContain('Done');
  });

  describe('the disable policy', () => {
    it('disables Continue while a required step\'s form is invalid', () => {
      const fixture = render();
      expect(continueButton(fixture)?.disabled).toBe(true);
    });

    it('enables Continue once the required form is valid', () => {
      const fixture = render(host => host.detailsForm.setValue({ name: 'Reduce setup time' }));
      expect(continueButton(fixture)?.disabled).toBe(false);
    });

    it('allows an optional step to be skipped with nothing filled in', () => {
      const fixture = render(host => {
        host.detailsForm.setValue({ name: 'Reduce setup time' });
      });
      clickContinue(fixture);

      expect(fixture.componentInstance.wizard().activeKey).toBe('tags');
      expect(continueButton(fixture)?.disabled).toBe(false);
    });

    it('does not block Continue on a pristine but already-valid step', () => {
      // With commit-on-step-1 the user can return to a step whose values are
      // already saved; a pristine guard would trap them there.
      const fixture = render(host => host.detailsForm.setValue({ name: 'Reduce setup time' }));

      expect(fixture.componentInstance.detailsForm.pristine).toBe(true);
      expect(continueButton(fixture)?.disabled).toBe(false);
    });

    it('marks an invalid form as touched on Continue so its errors surface', () => {
      const fixture = render();
      // Continue is disabled, so drive the handler directly - the guard still holds.
      fixture.componentInstance.wizard().onContinue();
      fixture.detectChanges();

      expect(fixture.componentInstance.commits).toEqual([]);
    });
  });

  describe('committing a step', () => {
    it('emits stepCommit for the active step and advances when the host completes', () => {
      const fixture = render(host => host.detailsForm.setValue({ name: 'Reduce setup time' }));
      clickContinue(fixture);

      expect(fixture.componentInstance.commits).toEqual(['details']);
      expect(fixture.componentInstance.wizard().activeKey).toBe('tags');
      expect(q(fixture, 'tags-body')).not.toBeNull();
    });

    it('emits activeKeyChange so the host can mirror the step into the route', () => {
      const fixture = render(host => host.detailsForm.setValue({ name: 'Reduce setup time' }));
      clickContinue(fixture);

      expect(fixture.componentInstance.activeKey).toBe('tags');
    });

    it('stays on the step and shows the host message in an alert region on failure', () => {
      const fixture = render(host => {
        host.detailsForm.setValue({ name: 'Reduce setup time' });
        host.failWith = 'This goal was changed elsewhere.';
      });
      clickContinue(fixture);

      const alert = q(fixture, 'wizard-error');
      expect(fixture.componentInstance.wizard().activeKey).toBe('details');
      expect(alert?.textContent?.trim()).toBe('This goal was changed elsewhere.');
      expect(alert?.getAttribute('role')).toBe('alert');
    });

    it('lets the user retry after a failure', () => {
      const fixture = render(host => {
        host.detailsForm.setValue({ name: 'Reduce setup time' });
        host.failWith = 'Save failed.';
      });
      clickContinue(fixture);

      fixture.componentInstance.failWith = null;
      clickContinue(fixture);

      expect(fixture.componentInstance.commits).toEqual(['details', 'details']);
      expect(fixture.componentInstance.wizard().activeKey).toBe('tags');
    });

    it('clears a stale failure message once the step changes', () => {
      const fixture = render(host => {
        host.detailsForm.setValue({ name: 'Reduce setup time' });
        host.failWith = 'Save failed.';
      });
      clickContinue(fixture);
      expect(q(fixture, 'wizard-error')).not.toBeNull();

      fixture.componentInstance.failWith = null;
      clickContinue(fixture);

      expect(q(fixture, 'wizard-error')).toBeNull();
    });

    it('stays busy and blocks Continue while a commit is unresolved', () => {
      const fixture = render(host => {
        host.detailsForm.setValue({ name: 'Reduce setup time' });
        host.hang = true;
      });
      clickContinue(fixture);

      expect(fixture.componentInstance.wizard().busy()).toBe(true);
      expect(fixture.componentInstance.wizard().activeKey).toBe('details');
      expect(continueButton(fixture)?.disabled).toBe(true);
    });

    it('ignores a second response from a host that completes and then fails', () => {
      const fixture = render(host => {
        host.detailsForm.setValue({ name: 'Reduce setup time' });
      });
      let captured: WizardCommitRequest | undefined;
      fixture.componentInstance.onCommit = (request: WizardCommitRequest) => {
        captured = request;
        request.complete();
      };
      clickContinue(fixture);

      captured?.fail('too late');
      fixture.detectChanges();

      expect(q(fixture, 'wizard-error')).toBeNull();
      expect(fixture.componentInstance.wizard().activeKey).toBe('tags');
    });

    it('emits finished instead of advancing past the last step', () => {
      const fixture = render(host => {
        host.detailsForm.setValue({ name: 'Reduce setup time' });
        host.activeKey = 'relations';
      });
      clickContinue(fixture);

      expect(fixture.componentInstance.finished).toBe(true);
      expect(fixture.componentInstance.wizard().activeKey).toBe('relations');
    });
  });

  describe('linear gating', () => {
    it('locks a later step until the required steps before it are complete', () => {
      const wizard = render().componentInstance.wizard();

      expect(wizard.isStepLocked(0)).toBe(false);
      expect(wizard.isStepLocked(1)).toBe(true);
      expect(wizard.isStepLocked(2)).toBe(true);
    });

    it('unlocks the next step once the required step commits', () => {
      const fixture = render(host => host.detailsForm.setValue({ name: 'Reduce setup time' }));
      clickContinue(fixture);
      const wizard = fixture.componentInstance.wizard();

      expect(wizard.isStepLocked(1)).toBe(false);
      // Step 3 sits behind only optional steps now, so it is reachable too.
      expect(wizard.isStepLocked(2)).toBe(false);
    });

    it('keeps completed steps clickable so the user can go back', () => {
      const fixture = render(host => host.detailsForm.setValue({ name: 'Reduce setup time' }));
      clickContinue(fixture);

      navButton(fixture, 'details')?.click();
      fixture.detectChanges();

      expect(fixture.componentInstance.wizard().activeKey).toBe('details');
      expect(q(fixture, 'name')).not.toBeNull();
    });

    it('ignores a click on a locked step', () => {
      const fixture = render();

      navButton(fixture, 'relations')?.click();
      fixture.detectChanges();

      expect(fixture.componentInstance.wizard().activeKey).toBe('details');
    });

    it('marks a locked step aria-disabled but leaves it focusable', () => {
      const fixture = render();
      const locked = navButton(fixture, 'relations');

      expect(locked?.getAttribute('aria-disabled')).toBe('true');
      // aria-disabled rather than the disabled attribute, so screen-reader users can
      // still reach the step and hear that it is not yet available.
      expect(locked?.hasAttribute('disabled')).toBe(false);
    });

    it('marks a completed step complete and drops aria-disabled from the next', () => {
      const fixture = render(host => host.detailsForm.setValue({ name: 'Reduce setup time' }));
      clickContinue(fixture);

      expect(navButton(fixture, 'details')?.classList.contains('is-complete')).toBe(true);
      expect(navButton(fixture, 'relations')?.hasAttribute('aria-disabled')).toBe(false);
    });
  });

  describe('the step nav', () => {
    it('marks the active step with aria-current', () => {
      const fixture = render();

      expect(navButton(fixture, 'details')?.getAttribute('aria-current')).toBe('step');
      expect(navButton(fixture, 'tags')?.hasAttribute('aria-current')).toBe(false);
    });

    it('exposes the nav as a labelled <nav> around an ordered list', () => {
      const el = render().nativeElement as HTMLElement;
      const nav = el.querySelector('nav');

      expect(nav?.getAttribute('aria-label')).toBe('Steps');
      expect(nav?.querySelector('ol')).not.toBeNull();
    });

    it('keeps exactly one step button tabbable (roving tabindex)', () => {
      const fixture = render();
      const tabbable = navButtons(fixture).filter(b => b.getAttribute('tabindex') === '0');

      expect(tabbable.length).toBe(1);
      expect(tabbable[0]).toBe(navButton(fixture, 'details'));
    });

    it('moves focus down and up with the arrow keys without changing the step', () => {
      const fixture = render();

      pressKey(navButton(fixture, 'details'), 'ArrowDown');
      fixture.detectChanges();

      expect(fixture.componentInstance.wizard().focusedIndex()).toBe(1);
      expect(navButton(fixture, 'tags')?.getAttribute('tabindex')).toBe('0');
      // Focus moved; the active step did not.
      expect(fixture.componentInstance.wizard().activeKey).toBe('details');

      pressKey(navButton(fixture, 'tags'), 'ArrowUp');
      fixture.detectChanges();
      expect(fixture.componentInstance.wizard().focusedIndex()).toBe(0);
    });

    it('jumps to the first and last step with Home and End', () => {
      const fixture = render();

      pressKey(navButton(fixture, 'details'), 'End');
      fixture.detectChanges();
      expect(fixture.componentInstance.wizard().focusedIndex()).toBe(2);

      pressKey(navButton(fixture, 'relations'), 'Home');
      fixture.detectChanges();
      expect(fixture.componentInstance.wizard().focusedIndex()).toBe(0);
    });

    it('does not wrap past the ends', () => {
      const fixture = render();

      pressKey(navButton(fixture, 'details'), 'ArrowUp');
      fixture.detectChanges();
      expect(fixture.componentInstance.wizard().focusedIndex()).toBe(0);

      pressKey(navButton(fixture, 'details'), 'End');
      pressKey(navButton(fixture, 'relations'), 'ArrowDown');
      fixture.detectChanges();
      expect(fixture.componentInstance.wizard().focusedIndex()).toBe(2);
    });

    it('ignores unrelated keys', () => {
      const fixture = render();

      pressKey(navButton(fixture, 'details'), 'a');
      fixture.detectChanges();

      expect(fixture.componentInstance.wizard().focusedIndex()).toBe(0);
    });
  });

  describe('cancel', () => {
    it('emits cancelled', () => {
      const fixture = render();
      cancelButton(fixture)?.click();
      fixture.detectChanges();

      expect(fixture.componentInstance.cancelled).toBe(true);
    });

    it('does not emit cancelled while a commit is in flight', () => {
      const fixture = render(host => {
        host.detailsForm.setValue({ name: 'Reduce setup time' });
        host.hang = true;
      });
      clickContinue(fixture);

      fixture.componentInstance.wizard().onCancel();
      expect(fixture.componentInstance.cancelled).toBe(false);
    });
  });

  describe('focus on step change', () => {
    it('gives the active panel a focusable heading that names the panel', () => {
      const fixture = render();
      const heading = q(fixture, 'wizard-panel-details');
      // app-card's own root is a <section>, so target the panel by class.
      const panel = (fixture.nativeElement as HTMLElement).querySelector(
        'section.app-wizard-panel'
      );

      expect(heading?.tagName.toLowerCase()).toBe('h2');
      expect(heading?.getAttribute('tabindex')).toBe('-1');
      expect(panel?.getAttribute('aria-labelledby')).toBe(heading?.id);
    });

    it('moves focus to the panel heading when the step changes', () => {
      const fixture = render(host => host.detailsForm.setValue({ name: 'Reduce setup time' }));
      clickContinue(fixture);

      expect(document.activeElement).toBe(q(fixture, 'wizard-panel-tags'));
    });

    it('does not steal focus on first render', () => {
      const fixture = render();

      expect(document.activeElement).not.toBe(q(fixture, 'wizard-panel-details'));
    });
  });
});
