import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { AppearanceMenuComponent } from './appearance-menu';
import { expectNoAxeViolations, getOpenDialog } from './testing/a11y';

vi.mock('@primeuix/themes', () => ({
  updatePrimaryPalette: vi.fn(() => ({})),
  definePreset: vi.fn(() => ({})),
}));

describe('AppearanceMenuComponent - accessibility (issue #159)', () => {
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

  it('has no axe violations for the toggle button', async () => {
    const el = render().nativeElement as HTMLElement;
    await expectNoAxeViolations(el);
  });

  it('has no axe violations with the appearance dialog open', async () => {
    const fixture = render();
    (fixture.nativeElement.querySelector('[data-testid="appearance-toggle"]') as HTMLButtonElement).click();
    fixture.detectChanges();
    const dialog = getOpenDialog();
    expect(dialog).toBeTruthy();
    await expectNoAxeViolations(dialog!);
  });
});
