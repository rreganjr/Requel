import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter, Router, ActivatedRoute, convertToParamMap } from '@angular/router';
import { Location } from '@angular/common';
import { BehaviorSubject, EMPTY } from 'rxjs';
import { ConfirmationService, MessageService } from 'primeng/api';
import { ScenarioEditorComponent } from './scenario-editor';
import { ScenarioService } from '../../core/scenario.service';
import { CommandService } from '../../core/command.service';
import { ProjectService } from '../../core/project.service';
import { PermissionService } from '../../core/permission.service';
import { EventStreamService } from '../../core/event-stream.service';

const MOCK_STEPS = [
  { id: 1, name: 'User opens login page', text: null, scenarioType: 'Primary', isScenario: false },
  { id: 2, name: 'User submits credentials', text: null, scenarioType: 'Primary', isScenario: false }
];

const MOCK_SCENARIO = {
  id: 15, version: 0, name: 'Login Flow', text: 'The login scenario.',
  scenarioType: 'Primary', steps: MOCK_STEPS, createdBy: null
};

const flush = () => new Promise(r => setTimeout(r, 0));

describe('ScenarioEditorComponent', () => {
  let paramMap$: BehaviorSubject<ReturnType<typeof convertToParamMap>>;
  let scenarioServiceMock: { getScenario: ReturnType<typeof vi.fn> };
  let commandServiceMock: { execute: ReturnType<typeof vi.fn> };
  let permissionServiceMock: { loadForProject: ReturnType<typeof vi.fn>; canEdit: ReturnType<typeof vi.fn>; canDelete: ReturnType<typeof vi.fn> };
  let eventStreamServiceMock: { events$: typeof EMPTY; addSubscription: ReturnType<typeof vi.fn>; removeSubscription: ReturnType<typeof vi.fn> };
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  let fixture: any;
  let comp: ScenarioEditorComponent;
  let router: Router;

  beforeEach(() => {
    paramMap$ = new BehaviorSubject(convertToParamMap({ name: 'proj1', scenarioId: 'new' }));

    scenarioServiceMock = { getScenario: vi.fn().mockResolvedValue(MOCK_SCENARIO) };
    commandServiceMock = {
      execute: vi.fn().mockResolvedValue({ success: true, entity: MOCK_SCENARIO })
    };
    permissionServiceMock = {
      loadForProject: vi.fn().mockResolvedValue(undefined),
      canEdit: vi.fn().mockReturnValue(true),
      canDelete: vi.fn().mockReturnValue(true)
    };
    eventStreamServiceMock = {
      events$: EMPTY,
      addSubscription: vi.fn().mockResolvedValue(undefined),
      removeSubscription: vi.fn().mockResolvedValue(undefined)
    };

    TestBed.configureTestingModule({
      imports: [ScenarioEditorComponent],
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { paramMap: paramMap$.asObservable() } },
        { provide: Location, useValue: { back: vi.fn() } },
        { provide: ScenarioService, useValue: scenarioServiceMock },
        { provide: CommandService, useValue: commandServiceMock },
        { provide: ProjectService, useValue: { notifyTreeChanged: vi.fn() } },
        { provide: PermissionService, useValue: permissionServiceMock },
        { provide: EventStreamService, useValue: eventStreamServiceMock },
        { provide: MessageService, useValue: { add: vi.fn() } }
      ]
    });
    fixture = TestBed.createComponent(ScenarioEditorComponent);
    comp = fixture.componentInstance;
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
  });

  it('isNew() is true and stepNodes() empty when scenarioId param is "new"', async () => {
    fixture.detectChanges();
    await flush();
    expect(comp.isNew()).toBe(true);
    expect(comp.stepNodes().length).toBe(0);
  });

  it('loads scenario: scenarioName() and stepNodes() populated', async () => {
    paramMap$.next(convertToParamMap({ name: 'proj1', scenarioId: '15' }));
    fixture.detectChanges();
    await flush();
    expect(scenarioServiceMock.getScenario).toHaveBeenCalledWith('proj1', 15);
    expect(comp.scenarioName()).toBe('Login Flow');
    expect(comp.stepNodes().length).toBe(2);
    expect(comp.stepNodes()[0].name).toBe('User opens login page');
  });

  it('renders the add-step controls as real buttons that add a step', async () => {
    paramMap$.next(convertToParamMap({ name: 'proj1', scenarioId: '15' }));
    fixture.detectChanges();
    await flush();
    fixture.detectChanges();
    const top = fixture.nativeElement.querySelector('[data-testid="scenario-add-step-top"]');
    const bottom = fixture.nativeElement.querySelector('[data-testid="scenario-add-step-bottom"]');
    expect(top.tagName).toBe('BUTTON');
    expect(bottom.tagName).toBe('BUTTON');
    const before = comp.stepNodes().length;
    bottom.click();
    expect(comp.stepNodes().length).toBe(before + 1);
  });

  it('addStep() appends a new step node and sets stepsSaveNeeded', () => {
    fixture.detectChanges();
    expect(comp.stepNodes().length).toBe(0);
    comp.addStep();
    expect(comp.stepNodes().length).toBe(1);
    expect(comp.stepNodes()[0].name).toBe('');
    expect(comp.stepNodes()[0].isNew).toBe(true);
    expect(comp.stepsSaveNeeded()).toBe(true);
  });

  it('removeStep() removes the given step node', () => {
    comp.addStep();
    comp.addStep();
    expect(comp.stepNodes().length).toBe(2);
    const stepToRemove = comp.stepNodes()[0];
    comp.removeStep(stepToRemove);
    expect(comp.stepNodes().length).toBe(1);
    expect(comp.stepNodes()[0]).not.toBe(stepToRemove);
  });

  it('onSave calls commandService.execute("EditScenario") with steps', async () => {
    fixture.detectChanges();
    await flush();
    comp.detailsForm.controls.name.setValue('My Scenario');
    comp.detailsForm.controls.scenarioType.setValue('Alternative');
    comp.addStep();
    comp.stepNodes()[0].name = 'Step one';
    await comp.onSave();
    expect(commandServiceMock.execute).toHaveBeenCalledWith('EditScenario', expect.objectContaining({
      projectName: 'proj1',
      name: 'My Scenario',
      scenarioTypeName: 'Alternative',
      steps: expect.arrayContaining([expect.objectContaining({ name: 'Step one' })])
    }));
  });

  it('canEdit() and canDelete() set from permissionService on init', async () => {
    fixture.detectChanges();
    await flush();
    expect(permissionServiceMock.canEdit).toHaveBeenCalledWith('Scenario');
    expect(permissionServiceMock.canDelete).toHaveBeenCalledWith('Scenario');
    expect(comp.canEdit()).toBe(true);
    expect(comp.canDelete()).toBe(true);
  });

  // #173: trackChanges()/hasChanges() are gone. Dirtiness is the form's own state OR a
  // pending step edit, since steps live outside the form and ship with EditScenario.
  it('hasUnsavedChanges() follows form.dirty once the scenario is loaded', async () => {
    paramMap$.next(convertToParamMap({ name: 'proj1', scenarioId: '15' }));
    fixture.detectChanges();
    await flush();
    expect(comp.hasUnsavedChanges()).toBe(false);
    comp.detailsForm.controls.name.setValue('Different Name');
    comp.detailsForm.controls.name.markAsDirty();
    expect(comp.hasUnsavedChanges()).toBe(true);
  });

  it('hasUnsavedChanges() is true for a step change with a pristine form', async () => {
    paramMap$.next(convertToParamMap({ name: 'proj1', scenarioId: '15' }));
    fixture.detectChanges();
    await flush();
    expect(comp.hasUnsavedChanges()).toBe(false);
    comp.addStep();
    expect(comp.detailsForm.dirty).toBe(false);
    expect(comp.hasUnsavedChanges()).toBe(true);
  });

  it('openStepEdit() sets editingStep; applyStepEdit() applies changes and clears', () => {
    fixture.detectChanges();
    comp.addStep();
    const step = comp.stepNodes()[0];
    comp.openStepEdit(step);
    expect(comp.editingStep()).toBe(step);
    comp.editingName = 'Edited Name';
    comp.editingType = 'Alternative';
    comp.editingText = 'Some notes';
    comp.applyStepEdit();
    expect(comp.editingStep()).toBeNull();
    expect(step.name).toBe('Edited Name');
    expect(step.scenarioType).toBe('Alternative');
    expect(comp.stepsSaveNeeded()).toBe(true);
  });

  it('closeStepEdit() clears editingStep without applying changes', () => {
    fixture.detectChanges();
    comp.addStep();
    comp.openStepEdit(comp.stepNodes()[0]);
    expect(comp.editingStep()).not.toBeNull();
    comp.closeStepEdit();
    expect(comp.editingStep()).toBeNull();
  });

  it('onSubScenarioSelected() appends a scenario-type step node', () => {
    fixture.detectChanges();
    comp.onSubScenarioSelected({ id: 99, name: 'Sub Flow', scenarioType: 'Alternative' });
    expect(comp.stepNodes().length).toBe(1);
    expect(comp.stepNodes()[0].isScenario).toBe(true);
    expect(comp.stepNodes()[0].name).toBe('Sub Flow');
    expect(comp.stepsSaveNeeded()).toBe(true);
  });

  it('onCopy calls CopyScenario and navigates to the copy', async () => {
    paramMap$.next(convertToParamMap({ name: 'proj1', scenarioId: '15' }));
    fixture.detectChanges();
    await flush();
    const copy = { ...MOCK_SCENARIO, id: 99 };
    commandServiceMock.execute.mockResolvedValue({ success: true, entity: copy });
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const cs = fixture.debugElement.injector.get(ConfirmationService);
    vi.spyOn(cs, 'confirm').mockImplementation((conf: any) => conf.accept?.());
    comp.onCopy();
    await flush();
    expect(commandServiceMock.execute).toHaveBeenCalledWith('CopyScenario',
      expect.objectContaining({ scenarioId: 15 }));
    expect(router.navigate).toHaveBeenCalledWith(['/projects', 'proj1', 'scenarios', 99]);
  });

  it('onDelete calls DeleteScenario and navigates to list', async () => {
    paramMap$.next(convertToParamMap({ name: 'proj1', scenarioId: '15' }));
    fixture.detectChanges();
    await flush();
    commandServiceMock.execute.mockResolvedValue({ success: true });
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const cs = fixture.debugElement.injector.get(ConfirmationService);
    vi.spyOn(cs, 'confirm').mockImplementation((conf: any) => conf.accept?.());
    comp.onDelete();
    await flush();
    expect(commandServiceMock.execute).toHaveBeenCalledWith('DeleteScenario',
      expect.objectContaining({ scenarioId: 15 }));
    expect(router.navigate).toHaveBeenCalledWith(['/projects', 'proj1', 'scenarios']);
  });

  it('loadError set (driving the retryable error state) when the initial load fails', async () => {
    scenarioServiceMock.getScenario.mockRejectedValue(new Error('Network error'));
    paramMap$.next(convertToParamMap({ name: 'proj1', scenarioId: '15' }));
    fixture.detectChanges();
    await flush();
    // Initial (non-SSE) load failures surface the retryable error state, not the
    // inline errorMessage banner (issue #131).
    expect(comp.loadError()).toBe('Failed to load scenario.');
    expect(comp.errorMessage()).toBeNull();
    expect(comp.loading()).toBe(false);
  });

  it('retryLoad recovers from a failed initial load', async () => {
    scenarioServiceMock.getScenario.mockRejectedValueOnce(new Error('Network error'));
    paramMap$.next(convertToParamMap({ name: 'proj1', scenarioId: '15' }));
    fixture.detectChanges();
    await flush();
    expect(comp.loadError()).toBe('Failed to load scenario.');

    // A subsequent successful fetch clears the error and populates the form.
    comp.retryLoad();
    await flush();
    expect(comp.loadError()).toBeNull();
    expect(comp.detailsForm.controls.name.value).toBe('Login Flow');
  });

  // #173 required test. EditScenarioCommandImpl calls checkExpectedVersion and then merges
  // the scenario twice, so every accepted save bumps @Version. If the wizard held the version
  // captured at step 1, coming back to Details and continuing again would 409. This asserts
  // the held version is re-read from each result instead.
  describe('wizard version contract (#173)', () => {
    const created = { ...MOCK_SCENARIO, id: 15, version: 1, name: 'Created' };

    it('re-reads version from every step result, so a back-navigation edit does not 409', async () => {
      fixture.detectChanges();
      await flush();

      // Each accepted save returns the next version, as the server would.
      let nextVersion = 1;
      commandServiceMock.execute.mockImplementation(async () => ({
        success: true,
        entity: { ...created, version: nextVersion++ }
      }));
      scenarioServiceMock.getScenario.mockImplementation(async () => ({
        ...created, version: nextVersion - 1, steps: []
      }));

      comp.detailsForm.controls.name.setValue('Created');

      // Step 1: Details.
      const step1 = { step: { key: 'details' }, complete: vi.fn(), fail: vi.fn() };
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      await comp.onStepCommit(step1 as any);
      expect(step1.complete).toHaveBeenCalled();

      // Step 2: mutate an association, then commit.
      comp.addStep();
      comp.stepNodes()[0].name = 'Step one';
      const step2 = { step: { key: 'steps' }, complete: vi.fn(), fail: vi.fn() };
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      await comp.onStepCommit(step2 as any);
      expect(step2.complete).toHaveBeenCalled();

      // Back to step 1, edit the name, continue again.
      comp.detailsForm.controls.name.setValue('Renamed');
      const step3 = { step: { key: 'details' }, complete: vi.fn(), fail: vi.fn() };
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      await comp.onStepCommit(step3 as any);
      expect(step3.complete).toHaveBeenCalled();
      expect(step3.fail).not.toHaveBeenCalled();

      // Each accepted save advances the version, so the third call must carry what the
      // SECOND save returned. The value that matters is the one it must NOT be: 1 is the
      // version captured at step 1, and sending that again is the 409 this test exists for.
      const calls = commandServiceMock.execute.mock.calls.filter(c => c[0] === 'EditScenario');
      const versions = calls.map(c => c[1].version);
      expect(versions).toEqual([undefined, 1, 2]);
      const last = calls[calls.length - 1][1];
      expect(last.version).not.toBe(1);
      // ...and the renamed value, proving step 2 did not pin the details to a snapshot.
      expect(last.name).toBe('Renamed');
    });

    it('a 409 keeps the step and reports the stale-version message', async () => {
      fixture.detectChanges();
      await flush();
      commandServiceMock.execute.mockResolvedValue({ success: false, status: 409, error: 'Conflict' });
      comp.scenarioId = 15;
      comp.detailsForm.controls.name.setValue('Whatever');

      const request = { step: { key: 'details' }, complete: vi.fn(), fail: vi.fn() };
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      await comp.onStepCommit(request as any);

      expect(request.complete).not.toHaveBeenCalled();
      expect(request.fail).toHaveBeenCalledWith(expect.stringContaining('changed elsewhere'));
    });
  });

  it('onSave sets errorMessage when command fails', async () => {
    fixture.detectChanges();
    await flush();
    commandServiceMock.execute.mockResolvedValue({ success: false, error: 'Conflict' });
    comp.detailsForm.controls.name.setValue('Test');
    await comp.onSave();
    expect(comp.errorMessage()).toBe('Conflict');
    expect(comp.saving()).toBe(false);
  });
});
