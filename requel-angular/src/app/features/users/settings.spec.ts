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
        { provide: PreferencesService, useValue: preferencesServiceMock }
      ]
    });
    fixture = TestBed.createComponent(SettingsComponent);
    comp = fixture.componentInstance;
  }

  beforeEach(() => {
    preferencesServiceMock = {
      load: vi.fn().mockResolvedValue({ sidebarProjectLimit: 5, sidebarProjectStaleness: 'ONE_MONTH' }),
      save: vi.fn().mockResolvedValue({ sidebarProjectLimit: 10, sidebarProjectStaleness: 'THREE_MONTHS' })
    };
    setup();
  });

  it('ngOnInit loads preferences and populates fields', async () => {
    fixture.detectChanges();
    await fixture.whenStable();
    expect(preferencesServiceMock.load).toHaveBeenCalled();
    expect(comp.sidebarProjectLimit).toBe(5);
    expect(comp.sidebarProjectStaleness).toBe('ONE_MONTH');
  });

  it('ngOnInit sets errorMessage when load fails', async () => {
    preferencesServiceMock.load.mockRejectedValue(new Error('Network error'));
    fixture.detectChanges();
    await fixture.whenStable();
    expect(comp.errorMessage()).toBe('Failed to load preferences.');
  });

  it('onSave calls preferencesService.save with current values', async () => {
    comp.sidebarProjectLimit = 20;
    comp.sidebarProjectStaleness = 'SIX_MONTHS';
    await comp.onSave();
    expect(preferencesServiceMock.save).toHaveBeenCalledWith({
      sidebarProjectLimit: 20,
      sidebarProjectStaleness: 'SIX_MONTHS'
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
});
