import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
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

describe('SidebarNavComponent', () => {
  let authServiceMock: { user: ReturnType<typeof signal<UserDto | null>> };
  let projectServiceMock: {
    listProjects: ReturnType<typeof vi.fn>;
    onTreeChanged: Subject<void>;
    importProject: ReturnType<typeof vi.fn>;
  };
  let eventStreamServiceMock: { events$: typeof EMPTY };
  let comp: SidebarNavComponent;

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

  it('canCreateProjects() is false when permission is absent', () => {
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
});
