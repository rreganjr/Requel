import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { SimpleChange } from '@angular/core';
import { EntitySelectorDialogComponent } from './entity-selector-dialog';
import { GoalService } from '../core/goal.service';
import { StoryService } from '../core/story.service';
import { ActorService } from '../core/actor.service';
import { ScenarioService } from '../core/scenario.service';
import { getOpenDialog, expectNoAxeViolations } from './testing/a11y';

const MOCK_GOALS = [
  { id: 1, name: 'Goal A' },
  { id: 2, name: 'Goal B' },
];

describe('EntitySelectorDialogComponent — accessibility', () => {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  let fixture: any;
  let comp: EntitySelectorDialogComponent;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [EntitySelectorDialogComponent],
      providers: [
        provideNoopAnimations(),
        { provide: GoalService, useValue: { listGoals: vi.fn().mockResolvedValue(MOCK_GOALS) } },
        { provide: StoryService, useValue: { listStories: vi.fn().mockResolvedValue([]) } },
        { provide: ActorService, useValue: { listActors: vi.fn().mockResolvedValue([]) } },
        { provide: ScenarioService, useValue: { listScenarios: vi.fn().mockResolvedValue([]) } },
      ],
    });
    fixture = TestBed.createComponent(EntitySelectorDialogComponent);
    comp = fixture.componentInstance;
  });

  afterEach(() => {
    // Dialogs render on document.body via appendTo="body"; destroy the fixture so the overlay
    // is removed and cannot leak into the next test's document queries.
    fixture.destroy();
    expect(getOpenDialog()).toBeNull();
  });

  async function openDialog(): Promise<void> {
    comp.visible = true;
    comp.projectName = 'proj1';
    comp.entityType = 'Goal';
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
    const dialog = getOpenDialog();
    expect(dialog!.textContent).toContain('Select Goal');
  });

  it('has no axe-core violations while open', async () => {
    await openDialog();
    await expectNoAxeViolations(getOpenDialog()!);
  });
});
