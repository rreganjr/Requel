import { Injectable, signal } from '@angular/core';
import { ProjectPermissions } from '../models/project';
import { ProjectService } from './project.service';

/**
 * Caches and exposes the current user's permissions for the active project.
 * Components use this to show/hide UI elements based on stakeholder permissions.
 */
@Injectable({ providedIn: 'root' })
export class PermissionService {

  private readonly _permissions = signal<ProjectPermissions | null>(null);
  private _loadedProject: string | null = null;

  constructor(private projectService: ProjectService) {}

  /**
   * Load permissions for a project. No-op if already loaded for the same project.
   */
  async loadForProject(projectName: string): Promise<void> {
    if (this._loadedProject === projectName && this._permissions() != null) {
      return;
    }
    const perms = await this.projectService.getMyPermissions(projectName);
    this._permissions.set(perms);
    this._loadedProject = projectName;
  }

  /** Clear cached permissions (e.g., on project change). */
  clear(): void {
    this._permissions.set(null);
    this._loadedProject = null;
  }

  get isStakeholder(): boolean {
    return this._permissions()?.isStakeholder ?? false;
  }

  get canCreateProjects(): boolean {
    return this._permissions()?.canCreateProjects ?? false;
  }

  /**
   * Check if the user has a specific permission on an entity type.
   * @param entityType — simplified class name (e.g., "Goal", "Story", "Actor")
   * @param permissionType — "Edit", "Delete", or "Grant"
   */
  hasPermission(entityType: string, permissionType: string): boolean {
    const perms = this._permissions();
    if (!perms) return false;
    return perms.permissions[entityType]?.includes(permissionType) ?? false;
  }

  /** Shorthand: can the user edit entities of this type? */
  canEdit(entityType: string): boolean {
    return this.hasPermission(entityType, 'Edit');
  }

  /** Shorthand: can the user delete entities of this type? */
  canDelete(entityType: string): boolean {
    return this.hasPermission(entityType, 'Delete');
  }
}
