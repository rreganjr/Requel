import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter, Router, ActivatedRoute, convertToParamMap } from '@angular/router';
import { Location } from '@angular/common';
import { BehaviorSubject, EMPTY, Subject } from 'rxjs';
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

  // #185. Unlike the other editors in this issue, scenario-editor is render-gated - the form sits
  // behind @if (loading()), so the "typed before the detail GET returned" race cannot happen here.
  // Its unguarded callers were the post-save refetch and the 409 recovery, both of which passed
  // fromSSE = false and so reset unconditionally.
  describe('unsaved work survives a load (#185)', () => {
    it('a 409 recovery keeps the edit being retried and adopts the new version', async () => {
      paramMap$.next(convertToParamMap({ name: 'proj1', scenarioId: '15' }));
      fixture.detectChanges();
      await flush();

      comp.detailsForm.controls.name.setValue('My retried rename');
      comp.detailsForm.controls.name.markAsDirty();

      // The save conflicts; the recovery refetch returns a scenario that moved on.
      commandServiceMock.execute.mockResolvedValue({ success: false, status: 409, error: 'Conflict' });
      scenarioServiceMock.getScenario.mockResolvedValue({
        ...MOCK_SCENARIO, version: 9, name: 'Renamed elsewhere'
      });

      await comp.onSave();

      // The whole point of a retry: the user's edit is still there to resend...
      expect(comp.detailsForm.controls.name.value).toBe('My retried rename');
      expect(comp.detailsForm.dirty).toBe(true);
      // ...against a version that will not 409 again.
      expect(comp.scenario()?.version).toBe(9);
    });

    it('the post-save refetch gives new steps their server ids without replacing the list',
      async () => {
        fixture.detectChanges();
        await flush();

        comp.detailsForm.controls.name.setValue('Created');
        comp.addStep();
        comp.stepNodes()[0].name = 'Step one';
        expect(comp.stepNodes()[0].stepId).toBeNull();
        expect(comp.stepNodes()[0].isNew).toBe(true);

        // The save succeeds and the refetch reports the step the server just created. That
        // refetch runs while saving() is still true, so it takes the merge path.
        //
        // The server's copy is deliberately given a DIFFERENT name here. In production the two
        // agree, but that makes the merge path and the old wholesale `stepNodes.set(...)`
        // indistinguishable - both end up with stepId 77 and the test passes either way. Making
        // them differ is what pins which path actually ran: the merge keeps the user's node and
        // only fills in the id, a wholesale replace adopts the server's name.
        commandServiceMock.execute.mockResolvedValue({
          success: true, entity: { ...MOCK_SCENARIO, id: 15, version: 1 }
        });
        scenarioServiceMock.getScenario.mockResolvedValue({
          ...MOCK_SCENARIO, id: 15, version: 1,
          steps: [{ id: 77, name: 'Server copy', text: null, scenarioType: 'Primary', isScenario: false }]
        });

        const request = { step: { key: 'details' }, complete: vi.fn(), fail: vi.fn() };
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        await comp.onStepCommit(request as any);
        await flush();

        // Without the merge the node keeps stepId null and the next EditScenario recreates it.
        expect(comp.stepNodes()).toHaveLength(1);
        expect(comp.stepNodes()[0].stepId).toBe(77);
        expect(comp.stepNodes()[0].isNew).toBe(false);
        expect(comp.stepNodes()[0].name).toBe('Step one');
      });

    // Pins the known gap in the position-based merge rather than claiming it is handled: a node
    // that already has an id is never re-keyed, so a reorder cannot corrupt saved steps - but an
    // id-less node sitting at a position now held by a different server step takes that step's id.
    it('only fills in id-less step nodes, leaving already-identified ones alone', async () => {
      paramMap$.next(convertToParamMap({ name: 'proj1', scenarioId: '15' }));
      fixture.detectChanges();
      await flush();

      // Two loaded steps (ids 1 and 2) plus one the user just added.
      comp.addStep();
      comp.stepNodes()[2].name = 'Third';
      comp.detailsForm.controls.name.setValue('Dirty');
      comp.detailsForm.controls.name.markAsDirty();

      scenarioServiceMock.getScenario.mockResolvedValue({
        ...MOCK_SCENARIO,
        steps: [
          { id: 1, name: 'User opens login page', text: null, scenarioType: 'Primary', isScenario: false },
          { id: 2, name: 'User submits credentials', text: null, scenarioType: 'Primary', isScenario: false },
          { id: 3, name: 'Third', text: null, scenarioType: 'Primary', isScenario: false }
        ]
      });

      const events$ = new Subject<{ targetType: string; targetId: number }>();
      eventStreamServiceMock.events$ = events$.asObservable() as typeof EMPTY;
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      await (comp as any).loadScenario(false);

      expect(comp.stepNodes().map(n => n.stepId)).toEqual([1, 2, 3]);
      expect(comp.stepNodes()[2].isNew).toBe(false);
      // The form itself is untouched - this ran with the guard closed.
      expect(comp.detailsForm.controls.name.value).toBe('Dirty');
      expect(comp.detailsForm.dirty).toBe(true);
    });

    it('an SSE refresh while the step popup is open does not orphan the edited node', async () => {
      const events$ = new Subject<{ targetType: string; targetId: number }>();
      eventStreamServiceMock.events$ = events$.asObservable() as typeof EMPTY;

      paramMap$.next(convertToParamMap({ name: 'proj1', scenarioId: '15' }));
      fixture.detectChanges();
      await flush();

      comp.openStepEdit(comp.stepNodes()[0]);
      const editing = comp.editingStep();

      events$.next({ targetType: 'Scenario', targetId: 15 });
      await flush();

      // Same object identity - a wholesale stepNodes.set() would have replaced it.
      expect(comp.editingStep()).toBe(editing);
      expect(comp.stepNodes()[0]).toBe(editing);
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
