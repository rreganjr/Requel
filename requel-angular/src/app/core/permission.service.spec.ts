import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { PermissionService } from './permission.service';
import { ProjectService } from './project.service';
import { ProjectPermissions } from '../models/project';

const FULL_PERMS: ProjectPermissions = {
  isStakeholder: true,
  canCreateProjects: true,
  permissions: {
    Goal: ['Edit', 'Delete'],
    Story: ['Edit'],
    Actor: ['Delete']
  }
};

describe('PermissionService', () => {
  let service: PermissionService;
  let projectServiceSpy: { getMyPermissions: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    projectServiceSpy = { getMyPermissions: vi.fn().mockResolvedValue(FULL_PERMS) };

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: ProjectService, useValue: projectServiceSpy }
      ]
    });
    service = TestBed.inject(PermissionService);
  });

  it('canEdit() and canDelete() return false before loadForProject()', () => {
    expect(service.canEdit('Goal')).toBe(false);
    expect(service.canDelete('Goal')).toBe(false);
    expect(service.isStakeholder).toBe(false);
  });

  it('loadForProject() fetches and caches permissions', async () => {
    await service.loadForProject('My Project');
    expect(projectServiceSpy.getMyPermissions).toHaveBeenCalledWith('My Project');
    expect(service.isStakeholder).toBe(true);
    expect(service.canEdit('Goal')).toBe(true);
    expect(service.canDelete('Goal')).toBe(true);
    expect(service.canEdit('Story')).toBe(true);
    expect(service.canDelete('Story')).toBe(false);
    expect(service.canEdit('Actor')).toBe(false);
    expect(service.canDelete('Actor')).toBe(true);
  });

  it('loadForProject() is a no-op when called again for the same project', async () => {
    await service.loadForProject('My Project');
    await service.loadForProject('My Project');
    expect(projectServiceSpy.getMyPermissions).toHaveBeenCalledTimes(1);
  });

  it('clear() resets all permissions', async () => {
    await service.loadForProject('My Project');
    service.clear();
    expect(service.canEdit('Goal')).toBe(false);
    expect(service.isStakeholder).toBe(false);
  });

  /**
   * #276. The no-op guard above is right for navigation and wrong after a grant, so refresh()
   * clears first — which resets _loadedProject and lets the guard fall through. Without this the
   * cache could never be updated at all: clear() had no production call site, and re-calling
   * loadForProject() returned immediately.
   */
  it('refresh() re-fetches where a second loadForProject() would not', async () => {
    await service.loadForProject('My Project');
    expect(projectServiceSpy.getMyPermissions).toHaveBeenCalledTimes(1);

    projectServiceSpy.getMyPermissions.mockResolvedValue({
      ...FULL_PERMS,
      permissions: { ...FULL_PERMS.permissions, Goal: ['Edit'] }
    });

    await service.loadForProject('My Project');
    expect(projectServiceSpy.getMyPermissions).toHaveBeenCalledTimes(1);
    expect(service.canDelete('Goal')).toBe(true);

    await service.refresh();
    expect(projectServiceSpy.getMyPermissions).toHaveBeenCalledTimes(2);
    expect(projectServiceSpy.getMyPermissions).toHaveBeenLastCalledWith('My Project');
    expect(service.canDelete('Goal')).toBe(false);
    expect(service.canEdit('Goal')).toBe(true);
  });

  it('refresh() is a no-op when no project has been loaded', async () => {
    await service.refresh();
    expect(projectServiceSpy.getMyPermissions).not.toHaveBeenCalled();
  });
});
