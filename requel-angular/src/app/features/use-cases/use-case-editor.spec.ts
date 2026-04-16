import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter, Router, ActivatedRoute, convertToParamMap } from '@angular/router';
import { Location } from '@angular/common';
import { BehaviorSubject, EMPTY } from 'rxjs';
import { ConfirmationService, MessageService } from 'primeng/api';
import { UseCaseEditorComponent } from './use-case-editor';
import { UseCaseService } from '../../core/use-case.service';
import { ActorService } from '../../core/actor.service';
import { ScenarioService } from '../../core/scenario.service';
import { CommandService } from '../../core/command.service';
import { ProjectService } from '../../core/project.service';
import { PermissionService } from '../../core/permission.service';
import { EventStreamService } from '../../core/event-stream.service';

const MOCK_ACTORS = [
  { id: 1, version: 0, name: 'Customer', text: null, goals: null, referencedByUseCases: null, referencedByStories: null }
];

const MOCK_USE_CASE = {
  id: 30, version: 0, name: 'Place Order', text: 'User places an order.',
  primaryActorName: 'Customer', scenarioId: null,
  goals: [{ id: 1, name: 'Buy product', entityType: 'Goal' }],
  stories: [{ id: 5, name: 'Happy path', entityType: 'Story' }],
  actors: [],
  additionalScenarios: []
};

const flush = () => new Promise(r => setTimeout(r, 0));

describe('UseCaseEditorComponent', () => {
  let paramMap$: BehaviorSubject<ReturnType<typeof convertToParamMap>>;
  let useCaseServiceMock: { getUseCase: ReturnType<typeof vi.fn> };
  let actorServiceMock: { listActors: ReturnType<typeof vi.fn> };
  let scenarioServiceMock: { getScenario: ReturnType<typeof vi.fn> };
  let commandServiceMock: { execute: ReturnType<typeof vi.fn> };
  let permissionServiceMock: { loadForProject: ReturnType<typeof vi.fn>; canEdit: ReturnType<typeof vi.fn>; canDelete: ReturnType<typeof vi.fn> };
  let eventStreamServiceMock: { events$: typeof EMPTY; addSubscription: ReturnType<typeof vi.fn>; removeSubscription: ReturnType<typeof vi.fn> };
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  let fixture: any;
  let comp: UseCaseEditorComponent;
  let router: Router;

  beforeEach(() => {
    paramMap$ = new BehaviorSubject(convertToParamMap({ name: 'proj1', useCaseId: 'new' }));

    useCaseServiceMock = { getUseCase: vi.fn().mockResolvedValue(MOCK_USE_CASE) };
    actorServiceMock = { listActors: vi.fn().mockResolvedValue(MOCK_ACTORS) };
    scenarioServiceMock = { getScenario: vi.fn().mockResolvedValue(null) };
    commandServiceMock = {
      execute: vi.fn().mockResolvedValue({ success: true, entity: MOCK_USE_CASE })
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
      imports: [UseCaseEditorComponent],
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { paramMap: paramMap$.asObservable() } },
        { provide: Location, useValue: { back: vi.fn() } },
        { provide: UseCaseService, useValue: useCaseServiceMock },
        { provide: ActorService, useValue: actorServiceMock },
        { provide: ScenarioService, useValue: scenarioServiceMock },
        { provide: CommandService, useValue: commandServiceMock },
        { provide: ProjectService, useValue: { notifyTreeChanged: vi.fn() } },
        { provide: PermissionService, useValue: permissionServiceMock },
        { provide: EventStreamService, useValue: eventStreamServiceMock },
        { provide: MessageService, useValue: { add: vi.fn() } }
      ]
    });
    fixture = TestBed.createComponent(UseCaseEditorComponent);
    comp = fixture.componentInstance;
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
  });

  it('isNew() is true when useCaseId param is "new"', async () => {
    fixture.detectChanges();
    await flush();
    expect(comp.isNew()).toBe(true);
  });

  it('loads use case: useCaseName(), goals(), stories() populated', async () => {
    paramMap$.next(convertToParamMap({ name: 'proj1', useCaseId: '30' }));
    fixture.detectChanges();
    await flush();
    expect(useCaseServiceMock.getUseCase).toHaveBeenCalledWith('proj1', 30);
    expect(comp.useCaseName()).toBe('Place Order');
    expect(comp.goals().length).toBe(1);
    expect(comp.stories().length).toBe(1);
  });

  it('actorOptions populated from actorService.listActors', async () => {
    fixture.detectChanges();
    await flush();
    expect(actorServiceMock.listActors).toHaveBeenCalledWith('proj1');
    expect(comp.actorOptions().length).toBe(1);
    expect(comp.actorOptions()[0].label).toBe('Customer');
  });

  it('onSave calls commandService.execute("EditUseCase") with fields', async () => {
    fixture.detectChanges();
    await flush();
    comp.name = 'New Use Case';
    comp.text = 'Description';
    await comp.onSave();
    expect(commandServiceMock.execute).toHaveBeenCalledWith('EditUseCase', expect.objectContaining({
      projectName: 'proj1',
      name: 'New Use Case',
      text: 'Description'
    }));
  });

  it('onSave sets errorMessage when command fails', async () => {
    commandServiceMock.execute.mockResolvedValue({ success: false, error: 'Conflict' });
    comp.name = 'Test';
    await comp.onSave();
    expect(comp.errorMessage()).toBe('Conflict');
    expect(comp.saving()).toBe(false);
  });
});
