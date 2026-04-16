import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter, Router, ActivatedRoute, convertToParamMap } from '@angular/router';
import { BehaviorSubject } from 'rxjs';
import { StoryListComponent } from './story-list';
import { StoryService } from '../../core/story.service';
import { PermissionService } from '../../core/permission.service';

const MOCK_STORIES = [
  { id: 10, version: 0, name: 'User logs in', text: 'A user logs in.', storyType: 'Success' as const,
    primaryActorName: 'Customer', createdBy: null, goals: null, actors: null },
  { id: 11, version: 0, name: 'Login fails', text: 'Bad credentials.', storyType: 'Exception' as const,
    primaryActorName: 'Customer', createdBy: null, goals: null, actors: null }
];

const flush = () => new Promise(r => setTimeout(r, 0));

describe('StoryListComponent', () => {
  let paramMap$: BehaviorSubject<ReturnType<typeof convertToParamMap>>;
  let storyServiceMock: { listStories: ReturnType<typeof vi.fn> };
  let permissionServiceMock: { loadForProject: ReturnType<typeof vi.fn>; canEdit: ReturnType<typeof vi.fn> };
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  let fixture: any;
  let comp: StoryListComponent;
  let router: Router;

  beforeEach(() => {
    paramMap$ = new BehaviorSubject(convertToParamMap({ name: 'proj1' }));

    storyServiceMock = { listStories: vi.fn().mockResolvedValue(MOCK_STORIES) };
    permissionServiceMock = {
      loadForProject: vi.fn().mockResolvedValue(undefined),
      canEdit: vi.fn().mockReturnValue(true)
    };

    TestBed.configureTestingModule({
      imports: [StoryListComponent],
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { paramMap: paramMap$.asObservable() } },
        { provide: StoryService, useValue: storyServiceMock },
        { provide: PermissionService, useValue: permissionServiceMock }
      ]
    });
    fixture = TestBed.createComponent(StoryListComponent);
    comp = fixture.componentInstance;
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
  });

  it('stories() populated from storyService.listStories on init', async () => {
    fixture.detectChanges();
    await flush();
    expect(storyServiceMock.listStories).toHaveBeenCalledWith('proj1');
    expect(comp.stories().length).toBe(2);
    expect(comp.stories()[0].name).toBe('User logs in');
    expect(comp.loading()).toBe(false);
  });

  it('storyType is present on loaded stories', async () => {
    fixture.detectChanges();
    await flush();
    expect(comp.stories()[0].storyType).toBe('Success');
    expect(comp.stories()[1].storyType).toBe('Exception');
  });

  it('canEdit() reflects permissionService.canEdit("Story")', async () => {
    fixture.detectChanges();
    await flush();
    expect(permissionServiceMock.canEdit).toHaveBeenCalledWith('Story');
    expect(comp.canEdit()).toBe(true);
  });

  it('onRowSelect navigates to story editor', async () => {
    fixture.detectChanges();
    await flush();
    comp.onRowSelect({ data: MOCK_STORIES[0] });
    expect(router.navigate).toHaveBeenCalledWith(['/projects', 'proj1', 'stories', 10]);
  });

  it('errorMessage set when listStories throws', async () => {
    storyServiceMock.listStories.mockRejectedValue(new Error('Network error'));
    fixture.detectChanges();
    await flush();
    expect(comp.errorMessage()).toBe('Failed to load stories.');
    expect(comp.loading()).toBe(false);
  });
});
