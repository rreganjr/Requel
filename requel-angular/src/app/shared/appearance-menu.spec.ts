import { ApplicationRef } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { AppearanceMenuComponent } from './appearance-menu';
import { ThemeService } from '../core/theme.service';
import { getOpenDialog } from './testing/a11y';

vi.mock('@primeuix/themes', () => ({
  updatePrimaryPalette: vi.fn(() => ({})),
  definePreset: vi.fn(() => ({})),
}));

describe('AppearanceMenuComponent (issue #159)', () => {
  function stubMatchMedia(): void {
    vi.stubGlobal(
      'matchMedia',
      vi.fn(() => ({ matches: false, addEventListener: vi.fn(), removeEventListener: vi.fn() })),
    );
  }

  function render() {
    TestBed.configureTestingModule({
      imports: [AppearanceMenuComponent],
      providers: [provideNoopAnimations()],
    });
    const fixture = TestBed.createComponent(AppearanceMenuComponent);
    fixture.detectChanges();
    return fixture;
  }

  function openPanel(fixture: ReturnType<typeof render>): void {
    (fixture.nativeElement.querySelector('[data-testid="appearance-toggle"]') as HTMLButtonElement).click();
    fixture.detectChanges();
  }

  beforeEach(() => {
    localStorage.clear();
    document.documentElement.classList.remove('rq-dark');
    stubMatchMedia();
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
    localStorage.clear();
  });

  it('renders a labelled appearance toggle button', () => {
    const el = render().nativeElement as HTMLElement;
    const btn = el.querySelector('[data-testid="appearance-toggle"]');
    expect(btn).toBeTruthy();
    expect(btn!.getAttribute('aria-label')).toBe('Appearance settings');
  });

  it('opens the appearance dialog showing the three modes and five color swatches', () => {
    const fixture = render();
    openPanel(fixture);
    expect(getOpenDialog()).toBeTruthy();
    for (const m of ['light', 'dark', 'system']) {
      expect(document.querySelector(`[data-testid="theme-mode-${m}"]`)).toBeTruthy();
    }
    for (const c of ['blue', 'emerald', 'violet', 'rose', 'amber']) {
      expect(document.querySelector(`[data-testid="primary-${c}"]`)).toBeTruthy();
    }
  });

  it('setting the mode radio updates and persists the theme', () => {
    const fixture = render();
    const theme = TestBed.inject(ThemeService);
    openPanel(fixture);
    const darkRadio = document.querySelector('[data-testid="theme-mode-dark"]') as HTMLInputElement;
    darkRadio.checked = true;
    darkRadio.dispatchEvent(new Event('change'));
    fixture.detectChanges();
    TestBed.inject(ApplicationRef).tick(); // flush the root-service persist effect
    expect(theme.mode()).toBe('dark');
    expect(localStorage.getItem('requel_theme')).toBe('dark');
  });

  it('selecting a color swatch updates and persists the primary', () => {
    const fixture = render();
    const theme = TestBed.inject(ThemeService);
    openPanel(fixture);
    const emerald = document.querySelector('[data-testid="primary-emerald"]') as HTMLInputElement;
    emerald.checked = true;
    emerald.dispatchEvent(new Event('change'));
    fixture.detectChanges();
    TestBed.inject(ApplicationRef).tick(); // flush the root-service persist effect
    expect(theme.primary()).toBe('emerald');
    expect(localStorage.getItem('requel_primary')).toBe('emerald');
  });
});
