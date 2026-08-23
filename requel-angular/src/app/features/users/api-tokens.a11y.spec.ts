import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { ApiTokensComponent } from './api-tokens';
import { TokenService } from '../../core/token.service';
import { getOpenDialog, expectNoAxeViolations } from '../../shared/testing/a11y';

describe('ApiTokensComponent — create-token dialog accessibility', () => {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  let fixture: any;
  let comp: ApiTokensComponent;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ApiTokensComponent],
      providers: [
        provideNoopAnimations(),
        { provide: TokenService, useValue: { list: vi.fn().mockResolvedValue([]) } },
      ],
    });
    fixture = TestBed.createComponent(ApiTokensComponent);
    comp = fixture.componentInstance;
  });

  afterEach(() => {
    fixture.destroy();
    expect(getOpenDialog()).toBeNull();
  });

  async function openDialog(): Promise<void> {
    fixture.detectChanges();
    await fixture.whenStable();
    comp.openCreate();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  it('renders as a modal dialog with role and aria-modal', async () => {
    await openDialog();
    const dialog = getOpenDialog();
    expect(dialog).not.toBeNull();
    expect(dialog!.getAttribute('role')).toBe('dialog');
    expect(dialog!.getAttribute('aria-modal')).toBe('true');
  });

  it('exposes an accessible name (header) to assistive tech', async () => {
    await openDialog();
    expect(getOpenDialog()!.textContent).toContain('New personal access token');
  });

  it('has no axe-core violations while open', async () => {
    await openDialog();
    await expectNoAxeViolations(getOpenDialog()!);
  });

  it('keeps the dialog accessible when the required name error is shown', async () => {
    await openDialog();
    await comp.submitCreate();
    fixture.detectChanges();
    const dialog = getOpenDialog()!;
    expect(dialog.querySelector('[data-testid="pat-name-error"]')).not.toBeNull();
    await expectNoAxeViolations(dialog);
  });
});
