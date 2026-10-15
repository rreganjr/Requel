/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2026 Ron Regan Jr. All Rights Reserved.
 *
 * Requel is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Requel is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Requel. If not, see <http://www.gnu.org/licenses/>.
 *
 */
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
  /** Bumped by clear(), so a refresh that finishes after a project change is dropped (#390). */
  private _generation = 0;
  /** The refresh in flight, shared by overlapping callers (#390). */
  private _refreshing: { projectName: string; promise: Promise<void> } | null = null;

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
    this._generation++;
  }

  /**
   * Re-fetch permissions for the project already loaded (issue #276).
   *
   * `loadForProject` deliberately no-ops when the same project is already loaded, which is right
   * for navigation but wrong after a grant or revoke. Callers are the permission write path itself
   * and the SSE listener that hears a permission change made by someone else.
   *
   * #390: the save path and the SSE listener both call this for the same change, and they overlap.
   * It used to `clear()` and then load, so the second caller found nothing loaded and returned at
   * once, and code awaiting it (the stakeholder editor's `applyPermissionRules`) ran with no
   * permissions, locking every box. Now the current permissions stay in place until the new ones
   * arrive, and an overlapping call shares the fetch already in flight.
   *
   * A no-op when nothing is loaded yet: there is no project to re-fetch for, and the next
   * `loadForProject` will do it anyway.
   */
  refresh(): Promise<void> {
    const projectName = this._loadedProject;
    if (projectName == null) {
      return Promise.resolve();
    }
    if (this._refreshing?.projectName === projectName) {
      return this._refreshing.promise;
    }
    const generation = this._generation;
    const promise = this.projectService.getMyPermissions(projectName)
      .then(perms => {
        // A clear() (project change) since the fetch started makes this answer stale.
        if (generation === this._generation) {
          this._permissions.set(perms);
          this._loadedProject = projectName;
        }
      })
      .finally(() => {
        if (this._refreshing?.promise === promise) {
          this._refreshing = null;
        }
      });
    this._refreshing = { projectName, promise };
    return promise;
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
