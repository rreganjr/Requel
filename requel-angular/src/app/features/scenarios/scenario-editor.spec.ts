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
    comp.name = 'My Scenario';
    comp.scenarioType = 'Alternative';
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
});
