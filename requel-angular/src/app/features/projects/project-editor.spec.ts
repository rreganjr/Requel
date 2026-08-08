import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter, Router, ActivatedRoute, convertToParamMap } from '@angular/router';
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
  let router: Router;

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
            getCategories: vi.fn().mockResolvedValue([]),
            getTypedCategories: vi.fn().mockResolvedValue([])
          } },
        { provide: MessageService, useValue: { add: vi.fn() } }
      ]
    });
    fixture = TestBed.createComponent(ProjectEditorComponent);
    comp = fixture.componentInstance;
    router = TestBed.inject(Router);
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
    comp.detailsForm.controls.name.setValue('My New Project');
    comp.detailsForm.markAsDirty();
    await comp.onSave();
    expect(commandServiceMock.execute).toHaveBeenCalledWith('EditProject', expect.objectContaining({
      name: 'My New Project'
    }));
  });

  // #173: create is a wizard. Project is the one editor whose second step needs no version
  // handling - AssignTag mutates the Tag, not the project - so the contract to pin here is that
  // step 1 captures the id WITHOUT navigating. The old code routed away on save, which both hid
  // Tags and destroyed the wizard, since the route param is the project's identity.
  describe('create wizard (#173)', () => {
    it('step 1 captures the project without navigating away', async () => {
      fixture.detectChanges();
      await flush();
      const navigate = vi.spyOn(router, 'navigate').mockResolvedValue(true);
      navigate.mockClear();

      commandServiceMock.execute.mockResolvedValue({
        success: true,
        entity: { id: 9, version: 1, name: 'My New Project', description: null, organizationName: null }
      });
      comp.detailsForm.controls.name.setValue('My New Project');

      const request = { step: { key: 'details' }, complete: vi.fn(), fail: vi.fn() };
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      await comp.onStepCommit(request as any);

      expect(request.complete).toHaveBeenCalled();
      expect(navigate).not.toHaveBeenCalled();
      // Tags can only render once the project exists.
      expect(comp.tagEntityId()).toBe(9);
      expect(comp.originalName()).toBe('My New Project');
    });

    it('the Tags step just advances - it issues no command of its own', async () => {
      fixture.detectChanges();
      await flush();
      commandServiceMock.execute.mockClear();

      const request = { step: { key: 'tags' }, complete: vi.fn(), fail: vi.fn() };
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      await comp.onStepCommit(request as any);

      expect(request.complete).toHaveBeenCalled();
      expect(commandServiceMock.execute).not.toHaveBeenCalled();
    });

    it('Done navigates to the created project', async () => {
      fixture.detectChanges();
      await flush();
      const navigate = vi.spyOn(router, 'navigate').mockResolvedValue(true);

      commandServiceMock.execute.mockResolvedValue({
        success: true,
        entity: { id: 9, version: 1, name: 'My New Project', description: null, organizationName: null }
      });
      comp.detailsForm.controls.name.setValue('My New Project');
      const request = { step: { key: 'details' }, complete: vi.fn(), fail: vi.fn() };
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      await comp.onStepCommit(request as any);

      navigate.mockClear();
      comp.onWizardFinished();
      expect(navigate).toHaveBeenCalledWith(['/projects', 'My New Project']);
    });
  });

  it('onSave sets errorMessage when command returns error', async () => {
    commandServiceMock.execute.mockResolvedValue({ success: false, error: 'Duplicate name' });
    comp.detailsForm.controls.name.setValue('Taken');
    comp.detailsForm.markAsDirty();
    await comp.onSave();
    expect(comp.errorMessage()).toBe('Duplicate name');
    expect(comp.saving()).toBe(false);
  });
});
