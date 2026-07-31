import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter, Router, ActivatedRoute, convertToParamMap } from '@angular/router';
import { Location } from '@angular/common';
import { BehaviorSubject, EMPTY } from 'rxjs';
import { MessageService } from 'primeng/api';
import { ScenarioEditorComponent } from './scenario-editor';
import { ScenarioService } from '../../core/scenario.service';
import { CommandService } from '../../core/command.service';
import { ProjectService } from '../../core/project.service';
import { PermissionService } from '../../core/permission.service';
import { EventStreamService } from '../../core/event-stream.service';
import { getOpenDialog, expectNoAxeViolations } from '../../shared/testing/a11y';

const flush = () => new Promise(r => setTimeout(r, 0));

const STEP = {
  stepId: 1, name: 'User submits credentials', text: 'Additional notes',
  scenarioType: 'Primary', isScenario: false, isNew: false,
};

describe('ScenarioEditorComponent — step-detail dialog accessibility', () => {
  let paramMap$: BehaviorSubject<ReturnType<typeof convertToParamMap>>;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  let fixture: any;
  let comp: ScenarioEditorComponent;

  beforeEach(() => {
    paramMap$ = new BehaviorSubject(convertToParamMap({ name: 'proj1', scenarioId: 'new' }));

    TestBed.configureTestingModule({
      imports: [ScenarioEditorComponent],
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { paramMap: paramMap$.asObservable() } },
        { provide: Location, useValue: { back: vi.fn() } },
        { provide: ScenarioService, useValue: { getScenario: vi.fn().mockResolvedValue(null) } },
        { provide: CommandService, useValue: { execute: vi.fn() } },
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
    fixture = TestBed.createComponent(ScenarioEditorComponent);
    comp = fixture.componentInstance;
    const router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
  });

  afterEach(() => {
    fixture.destroy();
    expect(getOpenDialog()).toBeNull();
  });

  async function openStepDialog(): Promise<void> {
    fixture.detectChanges();
    await flush();
    comp.openStepEdit({ ...STEP });
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  it('renders the step-detail dialog as a modal with role and aria-modal', async () => {
    await openStepDialog();
    const dialog = getOpenDialog();
    expect(dialog).not.toBeNull();
    expect(dialog!.getAttribute('role')).toBe('dialog');
    expect(dialog!.getAttribute('aria-modal')).toBe('true');
  });

  it('exposes an accessible name (header) to assistive tech', async () => {
    await openStepDialog();
    expect(getOpenDialog()!.textContent).toContain('Step Details');
  });

  it('has no axe-core violations while open', async () => {
    await openStepDialog();
    await expectNoAxeViolations(getOpenDialog()!);
  });

  it('closes (clears editingStep) when PrimeNG emits visibleChange(false) via Escape/mask', async () => {
    await openStepDialog();
    expect(comp.editingStep()).not.toBeNull();
    comp.onStepDialogVisibleChange(false);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(comp.editingStep()).toBeNull();
    expect(getOpenDialog()).toBeNull();
  });
});
