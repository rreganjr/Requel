import { ApplicationRef, Component, inject } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ThemeService } from './theme.service';

// Keep the service's updatePrimaryPalette call a no-op so tests don't mutate the
// global PrimeNG theme. Tests assert observable outcomes (signals + persistence),
// NOT this mock - under the full suite another spec can load the real module into
// the cache, so the service's call may not hit this file's mock.
vi.mock('@primeuix/themes', () => ({
  updatePrimaryPalette: vi.fn(() => ({})),
  definePreset: vi.fn(() => ({})),
}));

@Component({ standalone: true, template: '' })
class HostComponent {
  readonly theme = inject(ThemeService);
}

describe('ThemeService (issue #159)', () => {
  let listeners: ((e: { matches: boolean }) => void)[];

  function stubMatchMedia(matches: boolean): void {
    listeners = [];
    const mql = {
      matches,
      media: '(prefers-color-scheme: dark)',
      addEventListener: vi.fn((_t: string, cb: (e: { matches: boolean }) => void) => listeners.push(cb)),
      removeEventListener: vi.fn(),
    };
    vi.stubGlobal('matchMedia', vi.fn(() => mql));
  }

  // Deterministically flush the ThemeService's root-injector effects. A component
  // fixture's detectChanges() runs that component's change detection but does not
  // reliably flush an effect created in a root-provided service, so drive an
  // ApplicationRef tick, which does.
  function flush(): void {
    TestBed.inject(ApplicationRef).tick();
  }

  function render() {
    TestBed.configureTestingModule({ imports: [HostComponent] });
    const fixture = TestBed.createComponent(HostComponent);
    fixture.detectChanges();
    flush();
    return { theme: fixture.componentInstance.theme };
  }

  beforeEach(() => {
    localStorage.clear();
    document.documentElement.classList.remove('rq-dark');
    stubMatchMedia(false);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    localStorage.clear();
    document.documentElement.classList.remove('rq-dark');
  });

  it('defaults to system mode and blue primary with nothing stored', () => {
    const { theme } = render();
    expect(theme.mode()).toBe('system');
    expect(theme.primary()).toBe('blue');
  });

  it('adds .rq-dark and persists when the mode is set to dark', () => {
    const { theme } = render();
    theme.setMode('dark');
    flush();
    expect(document.documentElement.classList.contains('rq-dark')).toBe(true);
    expect(localStorage.getItem('requel_theme')).toBe('dark');
  });

  it('removes .rq-dark for light mode', () => {
    const { theme } = render();
    theme.setMode('dark');
    flush();
    theme.setMode('light');
    flush();
    expect(document.documentElement.classList.contains('rq-dark')).toBe(false);
  });

  it('follows the OS preference in system mode (OS dark)', () => {
    stubMatchMedia(true);
    const { theme } = render();
    expect(theme.isDark()).toBe(true);
    expect(document.documentElement.classList.contains('rq-dark')).toBe(true);
  });

  it('reacts live to an OS preference change while in system mode', () => {
    stubMatchMedia(false);
    const { theme } = render();
    expect(theme.isDark()).toBe(false);
    listeners.forEach(cb => cb({ matches: true }));
    flush();
    expect(theme.isDark()).toBe(true);
  });

  it('restores the stored mode and primary on start', () => {
    localStorage.setItem('requel_theme', 'dark');
    localStorage.setItem('requel_primary', 'violet');
    const { theme } = render();
    expect(theme.mode()).toBe('dark');
    expect(theme.primary()).toBe('violet');
  });

  it('applies and persists the primary accent on change', () => {
    const { theme } = render();
    theme.setPrimary('emerald');
    flush();
    // Assert the observable outcome (signal + persistence), not a spy on the
    // mocked updatePrimaryPalette; the persist proves the primary effect ran.
    expect(theme.primary()).toBe('emerald');
    expect(localStorage.getItem('requel_primary')).toBe('emerald');
  });

  it('falls back to defaults for corrupt stored values', () => {
    localStorage.setItem('requel_theme', 'garbage');
    localStorage.setItem('requel_primary', 'not-a-color');
    const { theme } = render();
    expect(theme.mode()).toBe('system');
    expect(theme.primary()).toBe('blue');
  });
});
