import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { SimpleChange } from '@angular/core';
import { ScenarioSelectorDialogComponent } from './scenario-selector-dialog';
import { ScenarioService } from '../core/scenario.service';
import { CommandService } from '../core/command.service';
import { getOpenDialog, expectNoAxeViolations } from './testing/a11y';

const MOCK_SCENARIOS = [
  { id: 1, name: 'Login Flow', scenarioType: 'Primary' },
  { id: 2, name: 'Logout Flow', scenarioType: 'Alternative' },
];

describe('ScenarioSelectorDialogComponent — accessibility', () => {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  let fixture: any;
  let comp: ScenarioSelectorDialogComponent;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ScenarioSelectorDialogComponent],
      providers: [
        provideNoopAnimations(),
        { provide: ScenarioService, useValue: { listScenarios: vi.fn().mockResolvedValue(MOCK_SCENARIOS) } },
        { provide: CommandService, useValue: { execute: vi.fn() } },
      ],
    });
    fixture = TestBed.createComponent(ScenarioSelectorDialogComponent);
    comp = fixture.componentInstance;
  });

  afterEach(() => {
    fixture.destroy();
    expect(getOpenDialog()).toBeNull();
  });

  async function openDialog(): Promise<void> {
    comp.visible = true;
    comp.projectName = 'proj1';
    comp.ngOnChanges({ visible: new SimpleChange(false, true, false) });
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
    expect(getOpenDialog()!.textContent).toContain('Add Sub-scenario');
  });

  it('has no axe-core violations while open', async () => {
    await openDialog();
    await expectNoAxeViolations(getOpenDialog()!);
  });
});
