import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter, Router } from '@angular/router';
import { signal } from '@angular/core';
import { ProjectListComponent } from './project-list';
import { ProjectService } from '../../core/project.service';
import { AuthService } from '../../core/auth.service';

const MOCK_PROJECTS = [
  { id: 1, version: 0, name: 'Alpha', description: null, organizationName: 'Acme',
    createdBy: null, status: null, stakeholderCount: 1, goalCount: 2, storyCount: 0,
    actorCount: 0, scenarioCount: 0, useCaseCount: 0, glossaryTermCount: 0, reportGeneratorCount: 0 },
  { id: 2, version: 0, name: 'Beta', description: null, organizationName: null,
    createdBy: null, status: null, stakeholderCount: 0, goalCount: 0, storyCount: 0,
    actorCount: 0, scenarioCount: 0, useCaseCount: 0, glossaryTermCount: 0, reportGeneratorCount: 0 }
];

describe('ProjectListComponent', () => {
  let projectServiceMock: { listProjects: ReturnType<typeof vi.fn>; importProject: ReturnType<typeof vi.fn> };
  let authServiceMock: { user: ReturnType<typeof signal> };
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  let fixture: any;
  let comp: ProjectListComponent;
  let router: Router;

  function setup(permissions: string[] = []) {
    authServiceMock = {
      user: signal({
        id: 1, version: 0, username: 'u', name: 'U', emailAddress: null,
        phoneNumber: null, organizationName: null, roles: ['ProjectUserRole'],
        permissions, permissionsByRole: null
      })
    };
    projectServiceMock = {
      listProjects: vi.fn().mockResolvedValue(MOCK_PROJECTS),
      importProject: vi.fn().mockResolvedValue({ success: true })
    };

    TestBed.configureTestingModule({
      imports: [ProjectListComponent],
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        { provide: ProjectService, useValue: projectServiceMock },
        { provide: AuthService, useValue: authServiceMock }
      ]
    });
    fixture = TestBed.createComponent(ProjectListComponent);
    comp = fixture.componentInstance;
    router = TestBed.inject(Router);
  }

  beforeEach(() => setup());

  it('ngOnInit loads projects and populates projects()', async () => {
    fixture.detectChanges();
    await fixture.whenStable();
    expect(projectServiceMock.listProjects).toHaveBeenCalled();
    expect(comp.projects().length).toBe(2);
    expect(comp.loading()).toBe(false);
  });

  it('canCreateProjects() is false when user lacks permission and is not admin', async () => {
    fixture.detectChanges();
    await fixture.whenStable();
    expect(comp.canCreateProjects()).toBe(false);
  });

  it('canCreateProjects() is true when user has createProjects permission', async () => {
    TestBed.resetTestingModule();
    setup(['createProjects']);
    fixture.detectChanges();
    await fixture.whenStable();
    expect(comp.canCreateProjects()).toBe(true);
  });

  it('canCreateProjects() is true when user is a system admin (no createProjects needed)', async () => {
    TestBed.resetTestingModule();
    authServiceMock = {
      user: signal({
        id: 1, version: 0, username: 'admin', name: 'Admin', emailAddress: null,
        phoneNumber: null, organizationName: null, roles: ['SystemAdminUserRole'],
        permissions: [], permissionsByRole: null
      })
    };
    projectServiceMock = {
      listProjects: vi.fn().mockResolvedValue([]),
      importProject: vi.fn().mockResolvedValue({ success: true })
    };
    TestBed.configureTestingModule({
      imports: [ProjectListComponent],
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        { provide: ProjectService, useValue: projectServiceMock },
        { provide: AuthService, useValue: authServiceMock }
      ]
    });
    fixture = TestBed.createComponent(ProjectListComponent);
    comp = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
    expect(comp.canCreateProjects()).toBe(true);
  });

  it('onRowSelect navigates to /projects/:name', () => {
    const spy = vi.spyOn(router, 'navigate').mockResolvedValue(true);
    comp.onRowSelect({ data: MOCK_PROJECTS[0] });
    expect(spy).toHaveBeenCalledWith(['/projects', 'Alpha']);
  });

  it('onNewProject navigates to /projects/new', () => {
    const spy = vi.spyOn(router, 'navigate').mockResolvedValue(true);
    comp.onNewProject();
    expect(spy).toHaveBeenCalledWith(['/projects', 'new']);
  });

  it('renders the name as a real link and still navigates on row click (issue #129)', async () => {
    const spy = vi.spyOn(router, 'navigate').mockResolvedValue(true);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    const el: HTMLElement = fixture.nativeElement;

    // AC #2 - the name cell is a real link to the detail route
    const link = el.querySelector('tbody tr.dt-row a.dt-link') as HTMLAnchorElement;
    expect(link).not.toBeNull();
    expect(link.textContent?.trim()).toBe('Alpha');
    expect(link.getAttribute('href')).toBe('/projects/Alpha');

    // AC #3 - whole-row click still navigates to the same target (kept as a convenience)
    const row = el.querySelector('tbody tr.dt-row') as HTMLElement;
    row.dispatchEvent(new MouseEvent('click', { bubbles: true }));
    expect(spy).toHaveBeenCalledWith(['/projects', 'Alpha']);
  });
});
