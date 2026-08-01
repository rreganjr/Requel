import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter, Router } from '@angular/router';
import { signal } from '@angular/core';
import { Subject, EMPTY } from 'rxjs';
import { SidebarNavComponent } from './sidebar-nav';
import { AuthService } from '../core/auth.service';
import { EventStreamService } from '../core/event-stream.service';
import { ProjectService } from '../core/project.service';
import { UserDto } from '../models/user';
import { ProjectDto } from '../models/project';

function makeUser(roles: string[], permissions: string[] = []): UserDto {
  return {
    id: 1, username: 'u', name: 'U', emailAddress: null,
    phoneNumber: null, organizationName: null,
    roles, permissions, permissionsByRole: null, version: 0
  };
}

const MOCK_PROJECT: ProjectDto = {
  id: 1, version: 0, name: 'Proj A', description: null,
  organizationName: null, createdBy: null, status: null,
  stakeholderCount: 2, goalCount: 3, storyCount: 1,
  actorCount: 4, scenarioCount: 5, useCaseCount: 0,
  glossaryTermCount: 2, reportGeneratorCount: 1
};

const SIDEBAR_EXPANDED_KEY = 'requel_sidebar_expanded_projects';

describe('SidebarNavComponent', () => {
  let authServiceMock: { user: ReturnType<typeof signal<UserDto | null>> };
  let projectServiceMock: {
    listProjects: ReturnType<typeof vi.fn>;
    onTreeChanged: Subject<void>;
    importProject: ReturnType<typeof vi.fn>;
  };
  let eventStreamServiceMock: { events$: typeof EMPTY };
  let comp: SidebarNavComponent;

  beforeEach(() => {
    // Each test starts with a clean expanded-state store so tests don't
    // leak the persisted set into one another.
    localStorage.removeItem(SIDEBAR_EXPANDED_KEY);
  });

  function setup(user: UserDto | null = null) {
    authServiceMock = { user: signal(user) };
    projectServiceMock = {
      listProjects: vi.fn().mockResolvedValue([MOCK_PROJECT]),
      onTreeChanged: new Subject<void>(),
      importProject: vi.fn().mockResolvedValue(undefined)
    };
    eventStreamServiceMock = { events$: EMPTY };

    TestBed.configureTestingModule({
      imports: [SidebarNavComponent],
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        { provide: AuthService, useValue: authServiceMock },
        { provide: ProjectService, useValue: projectServiceMock },
        { provide: EventStreamService, useValue: eventStreamServiceMock },
      ]
    });
    const fixture = TestBed.createComponent(SidebarNavComponent);
    comp = fixture.componentInstance;
    return fixture;
  }

  it('isAdmin() is false for a ProjectUserRole user', () => {
    setup(makeUser(['ProjectUserRole']));
    expect(comp.isAdmin()).toBe(false);
  });

  it('isAdmin() is true for SystemAdminUserRole', () => {
    setup(makeUser(['SystemAdminUserRole']));
    expect(comp.isAdmin()).toBe(true);
  });

  it('hasProjectRole() is true for ProjectUserRole', () => {
    setup(makeUser(['ProjectUserRole']));
    expect(comp.hasProjectRole()).toBe(true);
  });

  it('hasProjectRole() is true for SystemAdminUserRole', () => {
    setup(makeUser(['SystemAdminUserRole']));
    expect(comp.hasProjectRole()).toBe(true);
  });

  it('hasProjectRole() is false when user has no matching roles', () => {
    setup(makeUser([]));
    expect(comp.hasProjectRole()).toBe(false);
  });

  it('canCreateProjects() is true when user has createProjects permission', () => {
    setup(makeUser(['ProjectUserRole'], ['createProjects']));
    expect(comp.canCreateProjects()).toBe(true);
  });

  it('canCreateProjects() is true when user is a system admin (no createProjects needed)', () => {
    setup(makeUser(['SystemAdminUserRole'], []));
    expect(comp.canCreateProjects()).toBe(true);
  });

  it('canCreateProjects() is false when permission is absent and user is not admin', () => {
    setup(makeUser(['ProjectUserRole'], []));
    expect(comp.canCreateProjects()).toBe(false);
  });

  it('loadProjects() calls projectService.listProjects and populates tree nodes', async () => {
    setup(makeUser(['ProjectUserRole']));
    await comp.loadProjects();
    expect(projectServiceMock.listProjects).toHaveBeenCalled();
    expect(comp.projectTreeNodes().length).toBe(1);
    expect(comp.projectTreeNodes()[0].label).toBe('Proj A');
  });

  it('projectTreeNodes() builds child entity-group nodes with counts', async () => {
    setup(makeUser(['ProjectUserRole']));
    await comp.loadProjects();
    const children = comp.projectTreeNodes()[0].children ?? [];
    const goalNode = children.find(c => c.data?.type === 'Goals');
    expect(goalNode?.label).toBe('Goals (3)');
    const openIssuesNode = children.find(c => c.data?.type === 'OpenIssues');
    expect(openIssuesNode?.label).toBe('Open Issues');
  });

  it('activePanels() includes both "admin" and "projects" for SystemAdminUserRole', () => {
    setup(makeUser(['SystemAdminUserRole']));
    expect(comp.activePanels()).toContain('admin');
    expect(comp.activePanels()).toContain('projects');
  });

  it('activePanels() includes only "projects" for ProjectUserRole', () => {
    setup(makeUser(['ProjectUserRole']));
    expect(comp.activePanels()).not.toContain('admin');
    expect(comp.activePanels()).toContain('projects');
  });

  it('activePanels() is empty when user has no roles', () => {
    setup(makeUser([]));
    expect(comp.activePanels()).toEqual([]);
  });

  it('loading() is false after loadProjects completes', async () => {
    setup(makeUser(['ProjectUserRole']));
    await comp.loadProjects();
    expect(comp.loading()).toBe(false);
  });

  it('ngOnInit calls listProjects when user has project role', async () => {
    const fixture = setup(makeUser(['ProjectUserRole']));
    fixture.detectChanges();
    await new Promise(r => setTimeout(r, 0));
    expect(projectServiceMock.listProjects).toHaveBeenCalled();
    expect(comp.projectTreeNodes().length).toBe(1);
  });

  it('ngOnInit does not call listProjects when user has no project role', async () => {
    const fixture = setup(makeUser([]));
    fixture.detectChanges();
    await new Promise(r => setTimeout(r, 0));
    expect(projectServiceMock.listProjects).not.toHaveBeenCalled();
  });

  it('onTreeChanged subscription triggers loadProjects again', async () => {
    const fixture = setup(makeUser(['ProjectUserRole']));
    fixture.detectChanges();
    await new Promise(r => setTimeout(r, 0));
    const callsBefore = projectServiceMock.listProjects.mock.calls.length;
    projectServiceMock.onTreeChanged.next();
    await new Promise(r => setTimeout(r, 0));
    expect(projectServiceMock.listProjects.mock.calls.length).toBeGreaterThan(callsBefore);
  });

  it('onNewProject() navigates to /projects/new', () => {
    setup(makeUser(['ProjectUserRole']));
    const router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
    comp.onNewProject();
    expect(router.navigate).toHaveBeenCalledWith(['/projects', 'new']);
  });

  it('onNodeSelect navigates for project node', () => {
    setup(makeUser(['ProjectUserRole']));
    const router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
    comp.onNodeSelect({ node: { data: { type: 'project', name: 'Proj A' } } });
    expect(router.navigate).toHaveBeenCalledWith(['/projects', 'Proj A']);
  });

  it.each([
    ['Goals', 'goals'],
    ['Stories', 'stories'],
    ['Actors', 'actors'],
    ['Scenarios', 'scenarios'],
    ['Use Cases', 'use-cases'],
    ['Stakeholders', 'stakeholders'],
    ['Glossary', 'terms'],
    ['Reports', 'reports'],
    ['OpenIssues', 'open-issues'],
  ])('onNodeSelect navigates to %s list', (type, route) => {
    setup(makeUser(['ProjectUserRole']));
    const router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
    comp.onNodeSelect({ node: { data: { type, projectName: 'Proj A' } } });
    expect(router.navigate).toHaveBeenCalledWith(['/projects', 'Proj A', route]);
  });

  it('onImportFile calls importProject with the file and reloads', async () => {
    setup(makeUser(['ProjectUserRole']));
    const file = new File(['<project/>'], 'project.xml', { type: 'text/xml' });
    await comp.onImportFile(file);
    expect(projectServiceMock.importProject).toHaveBeenCalledWith(file);
    expect(projectServiceMock.listProjects).toHaveBeenCalled();
  });

  // ----- expanded-project persistence ----------------------------------
  // The sidebar tree's `expanded` state needs to survive both an
  // SSE-driven `loadProjects()` rebuild and a full page reload. The
  // contract is: project names the user has expanded are written to
  // localStorage under `requel_sidebar_expanded_projects`, and the
  // computed `projectTreeNodes` reads that set when assembling node
  // objects so a rebuild reapplies the open state.

  it('onNodeExpand persists the project name to localStorage', () => {
    setup(makeUser(['ProjectUserRole']));
    comp.onNodeExpand({ node: { data: { type: 'project', name: 'Proj A' } } });
    const raw = localStorage.getItem(SIDEBAR_EXPANDED_KEY);
    expect(raw).not.toBeNull();
    expect(JSON.parse(raw!)).toEqual(['Proj A']);
  });

  it('onNodeCollapse removes the project name from localStorage', () => {
    localStorage.setItem(SIDEBAR_EXPANDED_KEY, JSON.stringify(['Proj A', 'Proj B']));
    setup(makeUser(['ProjectUserRole']));
    comp.onNodeCollapse({ node: { data: { type: 'project', name: 'Proj A' } } });
    const raw = localStorage.getItem(SIDEBAR_EXPANDED_KEY);
    expect(JSON.parse(raw!)).toEqual(['Proj B']);
  });

  it('onNodeExpand ignores non-project nodes (entity-group children are leaves)', () => {
    setup(makeUser(['ProjectUserRole']));
    comp.onNodeExpand({ node: { data: { type: 'Goals', projectName: 'Proj A' } } });
    expect(localStorage.getItem(SIDEBAR_EXPANDED_KEY)).toBeNull();
  });

  it('projectTreeNodes() reflects persisted expanded state on first load', async () => {
    localStorage.setItem(SIDEBAR_EXPANDED_KEY, JSON.stringify(['Proj A']));
    setup(makeUser(['ProjectUserRole']));
    await comp.loadProjects();
    const nodes = comp.projectTreeNodes();
    expect(nodes[0].label).toBe('Proj A');
    expect(nodes[0].expanded).toBe(true);
  });

  it('projectTreeNodes() leaves projects collapsed when not in the persisted set', async () => {
    localStorage.setItem(SIDEBAR_EXPANDED_KEY, JSON.stringify(['Other Proj']));
    setup(makeUser(['ProjectUserRole']));
    await comp.loadProjects();
    expect(comp.projectTreeNodes()[0].expanded).toBe(false);
  });

  it('expanded state survives a loadProjects() rebuild (the SSE-refresh path)', async () => {
    setup(makeUser(['ProjectUserRole']));
    await comp.loadProjects();
    comp.onNodeExpand({ node: { data: { type: 'project', name: 'Proj A' } } });

    // Simulate the SSE-driven rebuild: same data comes back, but the
    // computed must re-emit nodes with `expanded: true` for Proj A so
    // the tree doesn't snap closed under the user.
    await comp.loadProjects();
    expect(comp.projectTreeNodes()[0].expanded).toBe(true);
  });

  it('treats a corrupted localStorage value as no persisted state', async () => {
    localStorage.setItem(SIDEBAR_EXPANDED_KEY, '{not json');
    setup(makeUser(['ProjectUserRole']));
    await comp.loadProjects();
    expect(comp.projectTreeNodes()[0].expanded).toBe(false);
  });

  it('ignores a localStorage value that is not a JSON array', async () => {
    // A string-shaped value should be discarded rather than blowing up
    // (or, worse, being interpreted as an iterable of characters).
    localStorage.setItem(SIDEBAR_EXPANDED_KEY, JSON.stringify('Proj A'));
    setup(makeUser(['ProjectUserRole']));
    await comp.loadProjects();
    expect(comp.projectTreeNodes()[0].expanded).toBe(false);
  });
});
