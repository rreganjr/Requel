import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter, Router, ActivatedRoute, convertToParamMap } from '@angular/router';
import { BehaviorSubject, EMPTY } from 'rxjs';
import { ConfirmationService, MessageService } from 'primeng/api';
import { StoryEditorComponent } from './story-editor';
import { StoryService } from '../../core/story.service';
import { ActorService } from '../../core/actor.service';
import { CommandService } from '../../core/command.service';
import { ProjectService } from '../../core/project.service';
import { PermissionService } from '../../core/permission.service';
import { EventStreamService } from '../../core/event-stream.service';

const MOCK_ACTORS = [
  { id: 1, version: 0, name: 'Customer', text: null, goals: null, referencedByUseCases: null, referencedByStories: null },
  { id: 2, version: 0, name: 'Admin', text: null, goals: null, referencedByUseCases: null, referencedByStories: null }
];

const MOCK_STORY = {
  id: 20, version: 0, name: 'User logs in', text: 'A user logs in with credentials.',
  storyType: 'Success', primaryActorName: 'Customer', goals: [], actors: []
};

const flush = () => new Promise(r => setTimeout(r, 0));

describe('StoryEditorComponent', () => {
  let paramMap$: BehaviorSubject<ReturnType<typeof convertToParamMap>>;
  let storyServiceMock: { getStory: ReturnType<typeof vi.fn> };
  let actorServiceMock: { listActors: ReturnType<typeof vi.fn> };
  let commandServiceMock: { execute: ReturnType<typeof vi.fn> };
  let permissionServiceMock: { loadForProject: ReturnType<typeof vi.fn>; canEdit: ReturnType<typeof vi.fn>; canDelete: ReturnType<typeof vi.fn> };
  let eventStreamServiceMock: { events$: typeof EMPTY; addSubscription: ReturnType<typeof vi.fn>; removeSubscription: ReturnType<typeof vi.fn> };
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  let fixture: any;
  let comp: StoryEditorComponent;
  let router: Router;

  beforeEach(() => {
    paramMap$ = new BehaviorSubject(convertToParamMap({ name: 'proj1', storyId: 'new' }));

    storyServiceMock = { getStory: vi.fn().mockResolvedValue(MOCK_STORY) };
    actorServiceMock = { listActors: vi.fn().mockResolvedValue(MOCK_ACTORS) };
    commandServiceMock = {
      execute: vi.fn().mockResolvedValue({ success: true, entity: MOCK_STORY })
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
      imports: [StoryEditorComponent],
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { paramMap: paramMap$.asObservable() } },
        { provide: StoryService, useValue: storyServiceMock },
        { provide: ActorService, useValue: actorServiceMock },
        { provide: CommandService, useValue: commandServiceMock },
        { provide: ProjectService, useValue: { notifyTreeChanged: vi.fn() } },
        { provide: PermissionService, useValue: permissionServiceMock },
        { provide: EventStreamService, useValue: eventStreamServiceMock },
        { provide: MessageService, useValue: { add: vi.fn() } }
      ]
    });
    fixture = TestBed.createComponent(StoryEditorComponent);
    comp = fixture.componentInstance;
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
  });

  it('isNew() is true when storyId param is "new"', async () => {
    fixture.detectChanges();
    await flush();
    expect(comp.isNew()).toBe(true);
  });

  it('loads story when storyId param is numeric', async () => {
    paramMap$.next(convertToParamMap({ name: 'proj1', storyId: '20' }));
    fixture.detectChanges();
    await flush();
    expect(storyServiceMock.getStory).toHaveBeenCalledWith('proj1', 20);
    expect(comp.storyName()).toBe('User logs in');
    expect(comp.story()?.id).toBe(20);
  });

  it('actorOptions populated from actorService.listActors', async () => {
    fixture.detectChanges();
    await flush();
    expect(actorServiceMock.listActors).toHaveBeenCalledWith('proj1');
    expect(comp.actorOptions().length).toBe(2);
    expect(comp.actorOptions()[0].label).toBe('Customer');
  });

  it('trackChanges() sets hasChanges() when name differs from original', async () => {
    paramMap$.next(convertToParamMap({ name: 'proj1', storyId: '20' }));
    fixture.detectChanges();
    await flush();
    expect(comp.hasChanges()).toBe(false);
    comp.name = 'Modified Name';
    comp.trackChanges();
    expect(comp.hasChanges()).toBe(true);
  });

  it('onSave calls commandService.execute("EditStory") with story fields', async () => {
    fixture.detectChanges();
    await flush();
    comp.name = 'My Story';
    comp.text = 'Story content';
    comp.storyType = 'Success';
    await comp.onSave();
    expect(commandServiceMock.execute).toHaveBeenCalledWith('EditStory', expect.objectContaining({
      projectName: 'proj1',
      name: 'My Story',
      text: 'Story content',
      storyTypeName: 'Success'
    }));
  });
});
