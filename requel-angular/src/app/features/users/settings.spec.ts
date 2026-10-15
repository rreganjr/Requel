import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { SettingsComponent } from './settings';
import { PreferencesService } from '../../core/preferences.service';

describe('SettingsComponent', () => {
  let preferencesServiceMock: { load: ReturnType<typeof vi.fn>; save: ReturnType<typeof vi.fn> };
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  let fixture: any;
  let comp: SettingsComponent;

  function setup() {
    TestBed.configureTestingModule({
      imports: [SettingsComponent],
      providers: [
        provideNoopAnimations(),
        { provide: PreferencesService, useValue: preferencesServiceMock },
      ],
    });
    fixture = TestBed.createComponent(SettingsComponent);
    comp = fixture.componentInstance;
  }

  beforeEach(() => {
    preferencesServiceMock = {
      load: vi
        .fn()
        .mockResolvedValue({ sidebarProjectLimit: 5, sidebarProjectStaleness: 'ONE_MONTH' }),
      save: vi
        .fn()
        .mockResolvedValue({ sidebarProjectLimit: 10, sidebarProjectStaleness: 'THREE_MONTHS' }),
    };
    setup();
  });

  it('ngOnInit loads preferences and populates fields', async () => {
    fixture.detectChanges();
    await fixture.whenStable();
    expect(preferencesServiceMock.load).toHaveBeenCalled();
    expect(comp.form.controls.sidebarProjectLimit.value).toBe(5);
    expect(comp.form.controls.sidebarProjectStaleness.value).toBe('ONE_MONTH');
  });

  it('ngOnInit sets errorMessage when load fails', async () => {
    preferencesServiceMock.load.mockRejectedValue(new Error('Network error'));
    fixture.detectChanges();
    await fixture.whenStable();
    expect(comp.errorMessage()).toBe('Failed to load preferences.');
  });

  it('onSave calls preferencesService.save with current values', async () => {
    comp.form.setValue({ sidebarProjectLimit: 20, sidebarProjectStaleness: 'SIX_MONTHS' });
    await comp.onSave();
    expect(preferencesServiceMock.save).toHaveBeenCalledWith({
      sidebarProjectLimit: 20,
      sidebarProjectStaleness: 'SIX_MONTHS',
    });
  });

  it('onSave sets successMessage and saving=false on success', async () => {
    await comp.onSave();
    expect(comp.successMessage()).toBe('Preferences saved.');
    expect(comp.saving()).toBe(false);
  });

  it('onSave sets errorMessage and saving=false when save fails', async () => {
    preferencesServiceMock.save.mockRejectedValue(new Error('Server error'));
    await comp.onSave();
    expect(comp.errorMessage()).toBe('Failed to save preferences.');
    expect(comp.saving()).toBe(false);
  });

  it('onReset restores the defaults and saves them', async () => {
    comp.form.setValue({ sidebarProjectLimit: 42, sidebarProjectStaleness: 'ALWAYS' });
    await comp.onReset();
    expect(preferencesServiceMock.save).toHaveBeenCalledWith({
      sidebarProjectLimit: 10,
      sidebarProjectStaleness: 'THREE_MONTHS',
    });
  });

  describe('reactive form (issue #132)', () => {
    const limit = () => comp.form.controls.sidebarProjectLimit;

    it('starts valid with the default values', () => {
      expect(comp.form.valid).toBe(true);
      expect(limit().value).toBe(10);
    });

    it('requires the project limit', () => {
      limit().setValue(null);
      expect(limit().hasError('required')).toBe(true);
      expect(comp.form.invalid).toBe(true);
    });

    it.each([
      [0, 'min'],
      [-3, 'min'],
      [101, 'max'],
      [2.5, 'integer'],
    ])('rejects a project limit of %p with the %s error', (value, error) => {
      limit().setValue(value as number);
      expect(limit().hasError(error)).toBe(true);
    });

    it.each([1, 10, 100])('accepts a project limit of %p', value => {
      limit().setValue(value);
      expect(limit().valid).toBe(true);
    });

    it('requires a staleness threshold', () => {
      comp.form.controls.sidebarProjectStaleness.setValue('');
      expect(comp.form.controls.sidebarProjectStaleness.hasError('required')).toBe(true);
    });

    it('does not save an invalid form', async () => {
      limit().setValue(0);
      await comp.onSave();
      expect(preferencesServiceMock.save).not.toHaveBeenCalled();
      expect(comp.submitted()).toBe(true);
    });

    it('marks the form pristine after a successful save', async () => {
      comp.form.markAsDirty();
      await comp.onSave();
      expect(comp.form.pristine).toBe(true);
    });

    it('marks the form pristine after loading, so Save starts disabled', async () => {
      fixture.detectChanges();
      await fixture.whenStable();
      expect(comp.form.pristine).toBe(true);
    });

    it('leaves the form dirty when a save fails, so the user can retry', async () => {
      preferencesServiceMock.save.mockRejectedValue(new Error('Server error'));
      comp.form.markAsDirty();
      await comp.onSave();
      expect(comp.form.dirty).toBe(true);
    });

    it('disables Save while the form is pristine', async () => {
      fixture.detectChanges();
      await fixture.whenStable();
      fixture.detectChanges();

      const button = (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>(
        '[data-testid="settings-save"] button'
      );
      expect(button?.disabled).toBe(true);
    });

    it('keeps Reset enabled while the form is pristine', async () => {
      fixture.detectChanges();
      await fixture.whenStable();
      fixture.detectChanges();

      const button = (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>(
        '[data-testid="settings-reset"] button'
      );
      expect(button?.disabled).toBe(false);
    });

    it('shows the inline min error under the field, not as a page message', async () => {
      fixture.detectChanges();
      await fixture.whenStable();
      limit().setValue(0);
      limit().markAsTouched();
      fixture.detectChanges();

      const el = fixture.nativeElement as HTMLElement;
      const error = el.querySelector('[data-testid="field-error"]');
      expect(error?.textContent?.trim()).toBe('Must be 1 or more.');
      expect(comp.errorMessage()).toBe('');
    });

    it('keeps the e2e test hooks on the same elements', () => {
      fixture.detectChanges();
      const el = fixture.nativeElement as HTMLElement;
      for (const id of [
        'settings-page',
        'settings-project-limit',
        'settings-staleness',
        'settings-save',
        'settings-reset',
      ]) {
        expect(el.querySelector(`[data-testid="${id}"]`), id).not.toBeNull();
      }
    });

    it('binds each label to its control, and links the helper text (issue #138)', () => {
      fixture.detectChanges();
      const el = fixture.nativeElement as HTMLElement;

      const labels = Array.from(el.querySelectorAll('label'));
      expect(labels.map(l => l.getAttribute('for'))).toEqual([
        'settings-project-limit-input',
        'settings-staleness-input',
      ]);

      const numberInput = el.querySelector('#settings-project-limit-input');
      expect(numberInput?.getAttribute('aria-describedby')).toBeTruthy();
    });
  });
});
