import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter, ActivatedRoute, convertToParamMap } from '@angular/router';
import { BehaviorSubject } from 'rxjs';
import { ConfirmationService, MessageService } from 'primeng/api';
import { ProjectEditorComponent } from './project-editor';
import { ProjectService } from '../../core/project.service';
import { UserService } from '../../core/user.service';
import { CommandService } from '../../core/command.service';
import { PermissionService } from '../../core/permission.service';
import { TagService } from '../../core/tag.service';

const MOCK_PROJECT = {
  id: 42, version: 1, name: 'Existing Project', description: 'Desc',
  organizationName: 'Acme', createdBy: null, status: null,
  stakeholderCount: 0, goalCount: 0, storyCount: 0, actorCount: 0,
  scenarioCount: 0, useCaseCount: 0, glossaryTermCount: 0, reportGeneratorCount: 0
};

const flush = () => new Promise(r => setTimeout(r, 0));

describe('ProjectEditorComponent', () => {
  let paramMap$: BehaviorSubject<ReturnType<typeof convertToParamMap>>;
  let projectServiceMock: {
    getProject: ReturnType<typeof vi.fn>;
    getExportUrl: ReturnType<typeof vi.fn>;
    downloadProjectXml: ReturnType<typeof vi.fn>;
  };
  let userServiceMock: { listOrganizations: ReturnType<typeof vi.fn> };
  let commandServiceMock: { execute: ReturnType<typeof vi.fn> };
  let permissionServiceMock: { loadForProject: ReturnType<typeof vi.fn>; canEdit: ReturnType<typeof vi.fn> };
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  let fixture: any;
  let comp: ProjectEditorComponent;

  beforeEach(() => {
    paramMap$ = new BehaviorSubject(convertToParamMap({ name: 'new' }));

    projectServiceMock = {
      getProject: vi.fn().mockResolvedValue(MOCK_PROJECT),
      getExportUrl: vi.fn().mockReturnValue('/api/export/test'),
      downloadProjectXml: vi.fn().mockResolvedValue(new Blob(['<project/>'], { type: 'application/xml' }))
    };
    userServiceMock = { listOrganizations: vi.fn().mockResolvedValue([{ id: 1, name: 'Acme' }]) };
    commandServiceMock = {
      execute: vi.fn().mockResolvedValue({ success: true, entity: MOCK_PROJECT })
    };
    permissionServiceMock = {
      loadForProject: vi.fn().mockResolvedValue(undefined),
      canEdit: vi.fn().mockReturnValue(true)
    };

    TestBed.configureTestingModule({
      imports: [ProjectEditorComponent],
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { paramMap: paramMap$.asObservable() } },
        { provide: ProjectService, useValue: projectServiceMock },
        { provide: UserService, useValue: userServiceMock },
        { provide: CommandService, useValue: commandServiceMock },
        { provide: PermissionService, useValue: permissionServiceMock },
        { provide: TagService, useValue: {
            getTagsOnEntity: vi.fn().mockResolvedValue([]),
            getTagsForProject: vi.fn().mockResolvedValue([]),
            getCategories: vi.fn().mockResolvedValue([])
          } },
        { provide: MessageService, useValue: { add: vi.fn() } }
      ]
    });
    fixture = TestBed.createComponent(ProjectEditorComponent);
    comp = fixture.componentInstance;
  });

  it('isNew() is true when name param is "new"', async () => {
    fixture.detectChanges();
    await flush();
    expect(comp.isNew()).toBe(true);
  });

  it('isNew() is false and originalName() set when existing project loaded', async () => {
    paramMap$.next(convertToParamMap({ name: 'Existing Project' }));
    fixture.detectChanges();
    await flush();
    expect(comp.isNew()).toBe(false);
    expect(comp.originalName()).toBe('Existing Project');
  });

  it('calls projectService.getProject with the project name from route', async () => {
    paramMap$.next(convertToParamMap({ name: 'Existing Project' }));
    fixture.detectChanges();
    await flush();
    expect(projectServiceMock.getProject).toHaveBeenCalledWith('Existing Project');
  });

  it('onSave calls commandService.execute("EditProject") with name', async () => {
    fixture.detectChanges();
    await flush();
    comp.name = 'My New Project';
    await comp.onSave();
    expect(commandServiceMock.execute).toHaveBeenCalledWith('EditProject', expect.objectContaining({
      name: 'My New Project'
    }));
  });

  it('onSave sets errorMessage when command returns error', async () => {
    commandServiceMock.execute.mockResolvedValue({ success: false, error: 'Duplicate name' });
    comp.name = 'Taken';
    await comp.onSave();
    expect(comp.errorMessage()).toBe('Duplicate name');
    expect(comp.saving()).toBe(false);
  });
});
