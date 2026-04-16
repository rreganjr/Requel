import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter, Router, ActivatedRoute, convertToParamMap } from '@angular/router';
import { BehaviorSubject } from 'rxjs';
import { ActorListComponent } from './actor-list';
import { ActorService } from '../../core/actor.service';
import { PermissionService } from '../../core/permission.service';

const MOCK_ACTORS = [
  { id: 1, version: 0, name: 'Customer', text: 'End user.', createdBy: null,
    goals: null, referencedByUseCases: null, referencedByStories: null },
  { id: 2, version: 0, name: 'Admin', text: 'Administrator.', createdBy: null,
    goals: null, referencedByUseCases: null, referencedByStories: null }
];

const flush = () => new Promise(r => setTimeout(r, 0));

describe('ActorListComponent', () => {
  let paramMap$: BehaviorSubject<ReturnType<typeof convertToParamMap>>;
  let actorServiceMock: { listActors: ReturnType<typeof vi.fn> };
  let permissionServiceMock: { loadForProject: ReturnType<typeof vi.fn>; canEdit: ReturnType<typeof vi.fn> };
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  let fixture: any;
  let comp: ActorListComponent;
  let router: Router;

  beforeEach(() => {
    paramMap$ = new BehaviorSubject(convertToParamMap({ name: 'proj1' }));

    actorServiceMock = { listActors: vi.fn().mockResolvedValue(MOCK_ACTORS) };
    permissionServiceMock = {
      loadForProject: vi.fn().mockResolvedValue(undefined),
      canEdit: vi.fn().mockReturnValue(true)
    };

    TestBed.configureTestingModule({
      imports: [ActorListComponent],
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { paramMap: paramMap$.asObservable() } },
        { provide: ActorService, useValue: actorServiceMock },
        { provide: PermissionService, useValue: permissionServiceMock }
      ]
    });
    fixture = TestBed.createComponent(ActorListComponent);
    comp = fixture.componentInstance;
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
  });

  it('actors() populated from actorService.listActors on init', async () => {
    fixture.detectChanges();
    await flush();
    expect(actorServiceMock.listActors).toHaveBeenCalledWith('proj1');
    expect(comp.actors().length).toBe(2);
    expect(comp.actors()[0].name).toBe('Customer');
    expect(comp.loading()).toBe(false);
  });

  it('canEdit() reflects permissionService.canEdit("Actor")', async () => {
    fixture.detectChanges();
    await flush();
    expect(permissionServiceMock.canEdit).toHaveBeenCalledWith('Actor');
    expect(comp.canEdit()).toBe(true);
  });

  it('onRowSelect navigates to actor editor', async () => {
    fixture.detectChanges();
    await flush();
    comp.onRowSelect({ data: MOCK_ACTORS[0] });
    expect(router.navigate).toHaveBeenCalledWith(['/projects', 'proj1', 'actors', 1]);
  });

  it('onNewActor navigates to new actor', async () => {
    fixture.detectChanges();
    await flush();
    comp.onNewActor();
    expect(router.navigate).toHaveBeenCalledWith(['/projects', 'proj1', 'actors', 'new']);
  });

  it('errorMessage set when listActors throws', async () => {
    actorServiceMock.listActors.mockRejectedValue(new Error('Network error'));
    fixture.detectChanges();
    await flush();
    expect(comp.errorMessage()).toBe('Failed to load actors.');
    expect(comp.loading()).toBe(false);
  });
});
