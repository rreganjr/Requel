import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter, Router, ActivatedRoute, convertToParamMap } from '@angular/router';
import { BehaviorSubject, EMPTY } from 'rxjs';
import { ConfirmationService, MessageService } from 'primeng/api';
import { ActorEditorComponent } from './actor-editor';
import { ActorService } from '../../core/actor.service';
import { CommandService } from '../../core/command.service';
import { ProjectService } from '../../core/project.service';
import { PermissionService } from '../../core/permission.service';
import { EventStreamService } from '../../core/event-stream.service';

const MOCK_ACTOR = {
  id: 5, version: 0, name: 'Customer', text: 'End user of the system.',
  goals: [{ id: 1, name: 'Purchase item', entityType: null }],
  referencedByUseCases: [], referencedByStories: []
};

const flush = () => new Promise(r => setTimeout(r, 0));

describe('ActorEditorComponent', () => {
  let paramMap$: BehaviorSubject<ReturnType<typeof convertToParamMap>>;
  let actorServiceMock: { getActor: ReturnType<typeof vi.fn> };
  let commandServiceMock: { execute: ReturnType<typeof vi.fn> };
  let permissionServiceMock: { loadForProject: ReturnType<typeof vi.fn>; canEdit: ReturnType<typeof vi.fn>; canDelete: ReturnType<typeof vi.fn> };
  let eventStreamServiceMock: { events$: typeof EMPTY; addSubscription: ReturnType<typeof vi.fn>; removeSubscription: ReturnType<typeof vi.fn> };
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  let fixture: any;
  let comp: ActorEditorComponent;
  let router: Router;

  beforeEach(() => {
    paramMap$ = new BehaviorSubject(convertToParamMap({ name: 'proj1', actorId: 'new' }));

    actorServiceMock = { getActor: vi.fn().mockResolvedValue(MOCK_ACTOR) };
    commandServiceMock = {
      execute: vi.fn().mockResolvedValue({ success: true, entity: MOCK_ACTOR })
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
      imports: [ActorEditorComponent],
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { paramMap: paramMap$.asObservable() } },
        { provide: ActorService, useValue: actorServiceMock },
        { provide: CommandService, useValue: commandServiceMock },
        { provide: ProjectService, useValue: { notifyTreeChanged: vi.fn() } },
        { provide: PermissionService, useValue: permissionServiceMock },
        { provide: EventStreamService, useValue: eventStreamServiceMock },
        { provide: MessageService, useValue: { add: vi.fn() } }
      ]
    });
    fixture = TestBed.createComponent(ActorEditorComponent);
    comp = fixture.componentInstance;
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
  });

  it('isNew() is true when actorId param is "new"', async () => {
    fixture.detectChanges();
    await flush();
    expect(comp.isNew()).toBe(true);
  });

  it('loads actor: actorName(), actor(), and goals() populated', async () => {
    paramMap$.next(convertToParamMap({ name: 'proj1', actorId: '5' }));
    fixture.detectChanges();
    await flush();
    expect(actorServiceMock.getActor).toHaveBeenCalledWith('proj1', 5);
    expect(comp.actorName()).toBe('Customer');
    expect(comp.actor()?.id).toBe(5);
    expect(comp.goals().length).toBe(1);
    expect(comp.goals()[0].name).toBe('Purchase item');
  });

  it('trackChanges() sets hasChanges() when name differs from original', async () => {
    paramMap$.next(convertToParamMap({ name: 'proj1', actorId: '5' }));
    fixture.detectChanges();
    await flush();
    expect(comp.hasChanges()).toBe(false);
    comp.name = 'Modified Actor';
    comp.trackChanges();
    expect(comp.hasChanges()).toBe(true);
  });

  it('onSave calls commandService.execute("EditActor") with actor fields', async () => {
    fixture.detectChanges();
    await flush();
    comp.name = 'New Actor';
    comp.text = 'Description';
    await comp.onSave();
    expect(commandServiceMock.execute).toHaveBeenCalledWith('EditActor', expect.objectContaining({
      projectName: 'proj1',
      name: 'New Actor',
      description: 'Description'
    }));
  });

  it('onDelete triggers confirm then calls execute("DeleteActor")', async () => {
    paramMap$.next(convertToParamMap({ name: 'proj1', actorId: '5' }));
    fixture.detectChanges();
    await flush();

    const cs = fixture.debugElement.injector.get(ConfirmationService);
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    vi.spyOn(cs, 'confirm').mockImplementation((conf: any) => conf.accept?.());

    comp.onDelete();
    await flush();

    expect(commandServiceMock.execute).toHaveBeenCalledWith('DeleteActor', expect.objectContaining({
      projectName: 'proj1',
      actorId: 5
    }));
    expect(router.navigate).toHaveBeenCalledWith(['/projects', 'proj1', 'actors']);
  });
});
