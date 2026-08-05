import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { SettingsComponent } from './settings';
import { PreferencesService } from '../../core/preferences.service';
import { expectNoAxeViolations } from '../../shared/testing/a11y';

describe('SettingsComponent accessibility (issue #132)', () => {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  let fixture: any;
  let comp: SettingsComponent;

  async function render(): Promise<HTMLElement> {
    TestBed.configureTestingModule({
      imports: [SettingsComponent],
      providers: [
        provideNoopAnimations(),
        {
          provide: PreferencesService,
          useValue: {
            load: vi
              .fn()
              .mockResolvedValue({ sidebarProjectLimit: 5, sidebarProjectStaleness: 'ONE_MONTH' }),
            save: vi.fn().mockResolvedValue({
              sidebarProjectLimit: 5,
              sidebarProjectStaleness: 'ONE_MONTH',
            }),
          },
        },
      ],
    });
    fixture = TestBed.createComponent(SettingsComponent);
    comp = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  it('has no axe-core violations at rest', async () => {
    await expectNoAxeViolations(await render());
  });

  it('has no axe-core violations with a field in its error state', async () => {
    const el = await render();
    comp.form.controls.sidebarProjectLimit.setValue(0);
    comp.form.controls.sidebarProjectLimit.markAsTouched();
    fixture.detectChanges();

    expect(el.querySelector('[data-testid="field-error"]')).not.toBeNull();
    await expectNoAxeViolations(el);
  });

  it('has no axe-core violations with a page-level save failure showing', async () => {
    const el = await render();
    comp.errorMessage.set('Failed to save preferences.');
    fixture.detectChanges();
    await expectNoAxeViolations(el);
  });

  /**
   * The helper text under each label used to be a bare `<small>` with no programmatic
   * link to its control, so a screen-reader user never heard it. app-field wires it
   * through aria-describedby, and the error joins the same list rather than replacing it.
   */
  it('keeps helper text and the error both described by the control', async () => {
    const el = await render();
    comp.form.controls.sidebarProjectLimit.setValue(0);
    comp.form.controls.sidebarProjectLimit.markAsTouched();
    fixture.detectChanges();

    const input = el.querySelector<HTMLInputElement>('#settings-project-limit-input')!;
    const ids = (input.getAttribute('aria-describedby') ?? '').split(' ').filter(Boolean);
    expect(ids).toHaveLength(2);

    const texts = ids.map(id => el.querySelector(`#${id}`)?.textContent?.trim());
    expect(texts).toContain('Maximum number of projects shown in the sidebar.');
    expect(texts).toContain('Must be 1 or more.');
  });
});
