import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter, Router, ActivatedRoute, convertToParamMap } from '@angular/router';
import { BehaviorSubject, EMPTY } from 'rxjs';
import { MessageService } from 'primeng/api';
import { GoalEditorComponent } from './goal-editor';
import { GoalService } from '../../core/goal.service';
import { TagService } from '../../core/tag.service';
import { CommandService } from '../../core/command.service';
import { ProjectService } from '../../core/project.service';
import { PermissionService } from '../../core/permission.service';
import { EventStreamService } from '../../core/event-stream.service';
import { getOpenDialog, expectNoAxeViolations } from '../../shared/testing/a11y';

const MOCK_GOAL = {
  id: 10, version: 0, name: 'Improve UX', text: 'Make it great.',
  relationsFromThisGoal: [], relationsToThisGoal: [], referencedBy: [],
};

const flush = () => new Promise(r => setTimeout(r, 0));

describe('GoalEditorComponent — relation-type dialog accessibility', () => {
  let paramMap$: BehaviorSubject<ReturnType<typeof convertToParamMap>>;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  let fixture: any;
  let comp: GoalEditorComponent;

  beforeEach(() => {
    paramMap$ = new BehaviorSubject(convertToParamMap({ name: 'proj1', goalId: '10' }));

    TestBed.configureTestingModule({
      imports: [GoalEditorComponent],
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { paramMap: paramMap$.asObservable() } },
        { provide: GoalService, useValue: { getGoal: vi.fn().mockResolvedValue(MOCK_GOAL) } },
        { provide: TagService, useValue: {
            getTagsOnEntity: vi.fn().mockResolvedValue([]),
            getTagsForProject: vi.fn().mockResolvedValue([]),
            getCategories: vi.fn().mockResolvedValue([]),
            getTypedCategories: vi.fn().mockResolvedValue([]),
          } },
        { provide: CommandService, useValue: { execute: vi.fn().mockResolvedValue({ success: true, entity: MOCK_GOAL }) } },
        { provide: ProjectService, useValue: { notifyTreeChanged: vi.fn() } },
        { provide: PermissionService, useValue: {
            loadForProject: vi.fn().mockResolvedValue(undefined),
            canEdit: vi.fn().mockReturnValue(true),
            canDelete: vi.fn().mockReturnValue(true),
          } },
        { provide: EventStreamService, useValue: {
            events$: EMPTY,
            addSubscription: vi.fn().mockResolvedValue(undefined),
            removeSubscription: vi.fn().mockResolvedValue(undefined),
          } },
        { provide: MessageService, useValue: { add: vi.fn() } },
      ],
    });
    fixture = TestBed.createComponent(GoalEditorComponent);
    comp = fixture.componentInstance;
    const router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
  });

  afterEach(() => {
    fixture.destroy();
    expect(getOpenDialog()).toBeNull();
  });

  /** Render an existing goal, then open the relation-type dialog. */
  async function openRelationDialog(): Promise<void> {
    fixture.detectChanges();
    await flush();
    fixture.detectChanges();
    comp.onRelationGoalSelected({ entityType: 'Goal', id: 2, name: 'Reduce churn' });
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  it('renders the relation-type dialog as a modal with role and aria-modal', async () => {
    await openRelationDialog();
    const dialog = getOpenDialog();
    expect(dialog).not.toBeNull();
    expect(dialog!.getAttribute('role')).toBe('dialog');
    expect(dialog!.getAttribute('aria-modal')).toBe('true');
  });

  it('names the dialog after the target goal', async () => {
    await openRelationDialog();
    expect(getOpenDialog()!.textContent).toContain('Reduce churn');
  });

  it('has no axe-core violations while open', async () => {
    await openRelationDialog();
    await expectNoAxeViolations(getOpenDialog()!);
  });

  it('restores focus to the "Add Relation" opener when the dialog closes', async () => {
    await openRelationDialog();
    const addBtn = document.querySelector<HTMLElement>('[data-testid="goal-add-relation"] button');
    expect(addBtn).not.toBeNull();

    // onRelationDialogHide runs on every close path (Add, Cancel, Escape, mask). The opener is
    // programmatic (after the entity selector), so we restore focus explicitly rather than
    // relying on PrimeNG's last-focused-element default.
    comp.onRelationDialogHide();
    expect(document.activeElement).toBe(addBtn);
    expect(comp.pendingRelationGoal()).toBeNull();
  });
});
