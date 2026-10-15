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
    comp.addStep();
    comp.stepsForm.at(0).patchValue({ name: STEP.name, scenarioType: STEP.scenarioType, text: STEP.text });
    comp.openStepEdit(comp.stepsForm.at(0));
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

  it('has no axe-core violations with the name field in the error state (#202)', async () => {
    await openStepDialog();
    comp.editForm.controls.name.setValue('');
    comp.editSubmitted.set(true);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(getOpenDialog()!.textContent).toContain('A step needs a name.');
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

// #173: create is now an app-form-wizard, so the create surface needs its own axe pass -
// including with a field in the error state, which is where a missing/duplicated label or an
// unassociated message actually shows up. The dialog suite above covers the edit surface.
const CREATED = {
  id: 7, version: 2, name: 'Created scenario', text: '', scenarioType: 'Primary', steps: [], referencedBy: [],
};

describe('ScenarioEditorComponent - create wizard accessibility', () => {
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
        { provide: ScenarioService, useValue: { getScenario: vi.fn().mockResolvedValue(CREATED) } },
        { provide: CommandService, useValue: {
            execute: vi.fn().mockResolvedValue({ success: true, entity: CREATED }),
          } },
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
  });

  async function settle(): Promise<void> {
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  async function renderWizard(): Promise<HTMLElement> {
    fixture.detectChanges();
    await flush();
    await settle();
    const wizard = fixture.nativeElement.querySelector('[data-testid="scenario-wizard"]');
    expect(wizard).not.toBeNull();
    return wizard as HTMLElement;
  }

  it('renders the wizard on create, with the step nav labelled and Details current', async () => {
    const wizard = await renderWizard();
    const nav = wizard.querySelector('nav');
    expect(nav!.getAttribute('aria-label')).toBe('New scenario steps');
    const current = wizard.querySelector('[aria-current="step"]');
    expect(current!.textContent).toContain('Details');
  });

  it('has no axe-core violations on the Details step', async () => {
    const wizard = await renderWizard();
    await expectNoAxeViolations(wizard);
  });

  it('has no axe-core violations with the name field in the error state', async () => {
    const wizard = await renderWizard();
    comp.submitted.set(true);
    comp.detailsForm.controls.name.setValue('');
    comp.detailsForm.markAllAsTouched();
    await settle();
    // The error has to actually be on screen, or this asserts nothing.
    expect(wizard.textContent).toContain('A scenario needs a name.');
    await expectNoAxeViolations(wizard);
  });

  it('has no axe-core violations on the Steps step', async () => {
    const wizard = await renderWizard();

    // Advance the way a user does. Assigning comp.wizardStep directly mutates the value the
    // [(activeKey)] binding was checked with and throws NG0100 - and it would also skip the
    // step-1 commit, so Steps would render against a scenario that does not exist yet.
    comp.detailsForm.controls.name.setValue('Created scenario');
    await settle();
    (wizard.querySelector('[data-testid="wizard-continue"] button') as HTMLButtonElement).click();
    await flush();
    await settle();
    expect(comp.wizardStep).toBe('steps');

    comp.addStep();
    await settle();
    await expectNoAxeViolations(wizard);
  });

  // Regression guard for the e2e contract. The Playwright page objects address these controls
  // by DOM id - locator('#name'), locator('#text') - because that is the convention app-field
  // established (controlId on the field, matching id on the control; see term-editor). The
  // #173 conversion dropped the ids and let app-field generate them, so #name matched nothing
  // and 23 e2e tests failed while every unit test stayed green. Nothing here asserted the
  // association, so nothing caught it.
  it('keeps the stable control ids the e2e page objects address', async () => {
    const wizard = await renderWizard();
    {
      const el = wizard.querySelector('#name');
      expect(el, 'missing #name - e2e page objects locate this control by id').not.toBeNull();
      expect(el!.tagName).toBe('INPUT');
      // The label must point at that same id, or the id is present but unassociated.
      const label = wizard.querySelector('label[for="name"]');
      expect(label, 'no <label for="name">').not.toBeNull();
    }
    {
      const el = wizard.querySelector('#text');
      expect(el, 'missing #text - e2e page objects locate this control by id').not.toBeNull();
      expect(el!.tagName).toBe('TEXTAREA');
      // The label must point at that same id, or the id is present but unassociated.
      const label = wizard.querySelector('label[for="text"]');
      expect(label, 'no <label for="text">').not.toBeNull();
    }
  });

});
