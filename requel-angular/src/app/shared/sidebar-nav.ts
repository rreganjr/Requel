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
import { Component, computed, OnInit, signal, untracked, ViewChild, ElementRef, ChangeDetectionStrategy, inject, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { AccordionModule } from 'primeng/accordion';
import { ButtonModule } from 'primeng/button';
import { TreeModule } from 'primeng/tree';
import { BadgeModule } from 'primeng/badge';
import { TreeNode } from 'primeng/api';
import { AuthService } from '../core/auth.service';
import { EventStreamService } from '../core/event-stream.service';
import { AnnouncerService } from '../core/announcer.service';
import { ProjectService } from '../core/project.service';
import { ProjectDto, ProjectTreeNode } from '../models/project';
import { FileUploadButtonComponent } from './file-upload-button';

/**
 * localStorage key for the names of projects the user has expanded in the
 * sidebar tree. Persisted so an SSE-driven `loadProjects()` (which rebuilds
 * the tree) and a full page refresh both retain the user's open projects.
 */
const SIDEBAR_EXPANDED_PROJECTS_KEY = 'requel_sidebar_expanded_projects';

/**
 * localStorage key for which sidebar groups (accordion panels) the user has
 * left open. Distinct from `requel_sidebar_expanded_projects` (project tree)
 * and `requel_sidebar_collapsed` (whole-sidebar toggle, owned by the layout).
 * Default when unset: all groups open, matching the pre-#154 behaviour.
 */
const SIDEBAR_GROUPS_KEY = 'requel_sidebar_groups';
const DEFAULT_OPEN_GROUPS = ['admin', 'projects'];

function loadOpenGroups(): string[] {
  try {
    const raw = localStorage.getItem(SIDEBAR_GROUPS_KEY);
    if (raw === null) return [...DEFAULT_OPEN_GROUPS];
    const parsed = JSON.parse(raw);
    if (!Array.isArray(parsed)) return [...DEFAULT_OPEN_GROUPS];
    return parsed.filter((s): s is string => typeof s === 'string');
  } catch {
    return [...DEFAULT_OPEN_GROUPS];
  }
}

function persistOpenGroups(groups: string[]): void {
  try {
    localStorage.setItem(SIDEBAR_GROUPS_KEY, JSON.stringify(groups));
  } catch {
    // Storage may be unavailable (private mode, quota). Group state is a UX
    // nicety, not data - drop the persistence silently.
  }
}

function loadExpandedProjectNames(): Set<string> {
  try {
    const raw = localStorage.getItem(SIDEBAR_EXPANDED_PROJECTS_KEY);
    if (!raw) return new Set();
    const parsed = JSON.parse(raw);
    if (!Array.isArray(parsed)) return new Set();
    return new Set(parsed.filter((s): s is string => typeof s === 'string'));
  } catch {
    return new Set();
  }
}

function persistExpandedProjectNames(names: Set<string>): void {
  try {
    localStorage.setItem(SIDEBAR_EXPANDED_PROJECTS_KEY, JSON.stringify([...names]));
  } catch {
    // Storage may be unavailable (private mode, quota exceeded). The
    // expanded state is a UX nicety, not data — drop the persistence
    // silently rather than break the sidebar.
  }
}

/**
 * Sidebar accordion navigation.
 * - Admin panel: visible for SystemAdminUserRole users
 * - Projects panel: visible for ProjectUserRole (or admin) users
 * See doc/UI_DESIGN_GUIDE.md section 3.
 */
@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-sidebar-nav',
  standalone: true,
  imports: [AccordionModule, ButtonModule, TreeModule, BadgeModule, RouterLink, RouterLinkActive, FileUploadButtonComponent],
  template: `
    <nav aria-label="Primary" class="sidebar-nav-root">
    <p-accordion [multiple]="true" [value]="openGroups()"
                 (valueChange)="onGroupsChange($event)">

      @if (isAdmin()) {
        <p-accordion-panel value="admin">
          <p-accordion-header>
            <span class="panel-header"><i class="pi pi-cog"></i> Admin</span>
          </p-accordion-header>
          <p-accordion-content>
            <div class="panel-actions">
              <a routerLink="/users" routerLinkActive="active" class="sidebar-link" aria-label="List users">
                <i class="pi pi-list"></i> List Users
              </a>
              <a routerLink="/users/new" routerLinkActive="active" class="sidebar-link" aria-label="Create user">
                <i class="pi pi-plus"></i> Create User
              </a>
              <a routerLink="/global-tags" routerLinkActive="active" class="sidebar-link" aria-label="Manage global tags">
                <i class="pi pi-tags"></i> Global Tags
              </a>
              <a routerLink="/tag-categories" routerLinkActive="active" class="sidebar-link" aria-label="Manage tag categories">
                <i class="pi pi-sitemap"></i> Tag Categories
              </a>
            </div>
          </p-accordion-content>
        </p-accordion-panel>
      }

      @if (hasProjectRole()) {
        <p-accordion-panel value="projects">
          <p-accordion-header>
            <span class="panel-header"><i class="pi pi-folder"></i> Projects</span>
          </p-accordion-header>
          <p-accordion-content>
            <div class="panel-actions">
              @if (canCreateProjects()) {
                <p-button label="New" ariaLabel="New project" icon="pi pi-plus" size="small"
                          [text]="true" (onClick)="onNewProject()" />
                <app-file-upload-button label="Import" ariaLabel="Import project" size="small"
                                        [text]="true" accept=".xml"
                                        buttonTestid="sidebar-import-button"
                                        inputTestid="sidebar-import-input"
                                        (fileSelected)="onImportFile($event)" />
              }
              <a routerLink="/projects" routerLinkActive="active" [routerLinkActiveOptions]="{ exact: true }" class="sidebar-link" aria-label="List projects">
                <i class="pi pi-list"></i> List
              </a>
            </div>

            @if (loading()) {
              <div class="tree-loading">Loading projects...</div>
            } @else {
              <p-tree [value]="projectTreeNodes()"
                      selectionMode="single" [metaKeySelection]="false"
                      (onNodeSelect)="onNodeSelect($event)"
                      (onNodeExpand)="onNodeExpand($event)"
                      (onNodeCollapse)="onNodeCollapse($event)"
                      styleClass="sidebar-tree" data-testid="sidebar-tree" />
            }
          </p-accordion-content>
        </p-accordion-panel>
      }

    </p-accordion>
    </nav>
  `,
  styles: [`
    .panel-header {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      font-weight: 600;
      font-size: 14px;
    }

    .panel-actions {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: 0.25rem;
      padding: 0 0.25rem 0.5rem 0.25rem;
      border-bottom: 1px solid var(--p-surface-200);
      margin-bottom: 0.5rem;
    }

    .sidebar-link {
      display: flex;
      align-items: center;
      gap: 0.4rem;
      padding: 0.35rem 0.5rem;
      text-decoration: none;
      color: var(--p-text-color);
      font-size: 13px;
      border-radius: 4px;
    }

    .sidebar-link:hover {
      background: var(--p-surface-100);
    }

    /* Active-item highlight (#154). routerLinkActive marks the current route. */
    .sidebar-link.active {
      background: var(--p-highlight-background, var(--p-surface-100));
      color: var(--p-primary-color);
      font-weight: var(--rq-font-weight-semibold, 600);
    }

    /* #224 — the sidebar is a fixed-height column (see layout .sidebar); the
       nav host and its root fill it so the accordion (styled in global
       styles.scss) can give each open group its own scroll. */
    :host {
      display: flex;
      flex-direction: column;
      flex: 1;
      min-height: 0;
    }

    .sidebar-nav-root {
      display: flex;
      flex-direction: column;
      flex: 1;
      min-height: 0;
    }

    .tree-loading {
      padding: 0.5rem;
      font-size: 13px;
      color: var(--p-text-secondary-color);
    }

    /* .sidebar-tree PrimeNG p-tree overrides live in global styles.scss (#126). */
  `]
})
export class SidebarNavComponent implements OnInit {

  readonly loading = signal(false);
  private readonly projects = signal<ProjectDto[]>([]);

  /**
   * Names of projects the user has expanded in the tree. Seeded from
   * localStorage so the open set survives a page refresh, and updated by
   * the tree's expand/collapse events so the persisted set stays in sync
   * with what the user actually sees.
   *
   * Read via `untracked()` inside `projectTreeNodes` so toggling a single
   * project doesn't force every node to be re-created — the expand/collapse
   * is already reflected visually by PrimeNG's own click handler; we only
   * need the set when the tree is rebuilt (initial load, SSE refresh).
   */
  private readonly expandedProjects = signal<Set<string>>(loadExpandedProjectNames());

  readonly isAdmin = computed(() => {
    const user = this.authService.user();
    return user?.roles?.includes('SystemAdminUserRole') ?? false;
  });

  readonly hasProjectRole = computed(() => {
    const user = this.authService.user();
    if (!user?.roles) return false;
    return user.roles.includes('ProjectUserRole') || user.roles.includes('SystemAdminUserRole');
  });

  readonly canCreateProjects = computed(() => {
    const user = this.authService.user();
    // System admins can create projects at the command level; createProjects gates regular users.
    const isAdmin = user?.roles?.includes('SystemAdminUserRole') ?? false;
    return isAdmin || (user?.permissions?.includes('createProjects') ?? false);
  });

  /**
   * Which sidebar groups are open. Seeded from localStorage so the user's
   * open/closed choice survives a reload, and updated by the accordion's
   * valueChange so the persisted set stays in sync. Defaults to all-open.
   */
  readonly openGroups = signal<string[]>(loadOpenGroups());

  onGroupsChange(value: unknown): void {
    const groups = Array.isArray(value)
      ? value.map(v => String(v))
      : value != null ? [String(value)] : [];
    this.openGroups.set(groups);
    persistOpenGroups(groups);
  }

  /**
   * Build tree nodes: each project is a parent node, entity groups are children.
   * Project name click → open editor. Entity group click → open list (future).
   *
   * `expanded` is seeded from the persisted set so a tree rebuild (SSE refresh
   * or page reload) leaves the user's open projects open.
   */
  readonly projectTreeNodes = computed<TreeNode[]>(() => {
    const projects = this.projects();
    const expanded = untracked(() => this.expandedProjects());
    return projects.map(project => ({
      label: project.name,
      icon: 'pi pi-folder',
      expanded: expanded.has(project.name),
      data: { type: 'project', name: project.name },
      children: this.entityGroups(project)
    }));
  });

  @ViewChild('importInput') importInput!: ElementRef<HTMLInputElement>;
  private readonly destroyRef = inject(DestroyRef);

  constructor(
    private authService: AuthService,
    private eventStreamService: EventStreamService,
    private projectService: ProjectService,
    private router: Router,
    private announcer: AnnouncerService
  ) {}

  async ngOnInit(): Promise<void> {
    if (this.hasProjectRole()) {
      await this.loadProjects();
    }
    this.projectService.onTreeChanged.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => this.loadProjects());
    // Reload project counts when any project-scoped command succeeds (SSE broadcast)
    this.eventStreamService.events$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(envelope => {
        if (envelope.targetType === 'Project') {
          this.loadProjects();
          this.announcer.announceThrottled('sidebar-projects', 'Project list updated.');
        }
      });
  }


  onNewProject(): void {
    this.router.navigate(['/projects', 'new']);
  }

  async onImportFile(file: File): Promise<void> {
    try {
      await this.projectService.importProject(file);
      await this.loadProjects();
    } catch {
      // Import errors handled by project-list if open; sidebar silently refreshes
    }
  }

  /**
   * Persist a newly-expanded project so the next tree rebuild restores it.
   * Only project-level nodes are tracked; entity-group children are leaves.
   */
  onNodeExpand(event: { node: TreeNode }): void {
    const data = event.node.data;
    if (data?.type !== 'project' || typeof data.name !== 'string') return;
    const next = new Set(this.expandedProjects());
    if (next.has(data.name)) return;
    next.add(data.name);
    this.expandedProjects.set(next);
    persistExpandedProjectNames(next);
  }

  /**
   * Drop a now-collapsed project from the persisted open set.
   */
  onNodeCollapse(event: { node: TreeNode }): void {
    const data = event.node.data;
    if (data?.type !== 'project' || typeof data.name !== 'string') return;
    const next = new Set(this.expandedProjects());
    if (!next.delete(data.name)) return;
    this.expandedProjects.set(next);
    persistExpandedProjectNames(next);
  }

  onNodeSelect(event: { node: TreeNode }): void {
    const data = event.node.data;
    if (!data) return;
    if (data.type === 'project') {
      this.router.navigate(['/projects', data.name]);
    } else if (data.type === 'Stakeholders') {
      this.router.navigate(['/projects', data.projectName, 'stakeholders']);
    } else if (data.type === 'Goals') {
      this.router.navigate(['/projects', data.projectName, 'goals']);
    } else if (data.type === 'Stories') {
      this.router.navigate(['/projects', data.projectName, 'stories']);
    } else if (data.type === 'Actors') {
      this.router.navigate(['/projects', data.projectName, 'actors']);
    } else if (data.type === 'Scenarios') {
      this.router.navigate(['/projects', data.projectName, 'scenarios']);
    } else if (data.type === 'Use Cases') {
      this.router.navigate(['/projects', data.projectName, 'use-cases']);
    } else if (data.type === 'Glossary') {
      this.router.navigate(['/projects', data.projectName, 'terms']);
    } else if (data.type === 'Reports') {
      this.router.navigate(['/projects', data.projectName, 'reports']);
    } else if (data.type === 'OpenIssues') {
      this.router.navigate(['/projects', data.projectName, 'open-issues']);
    }
  }

  async loadProjects(): Promise<void> {
    this.loading.set(true);
    try {
      const projects = await this.projectService.listProjects();
      this.projects.set(projects);
    } finally {
      this.loading.set(false);
    }
  }

  private entityGroups(project: ProjectDto): TreeNode[] {
    const groups: { label: string; type: string; count: number; icon: string }[] = [
      { label: 'Stakeholders', type: 'Stakeholders', count: project.stakeholderCount, icon: 'pi pi-users' },
      { label: 'Goals', type: 'Goals', count: project.goalCount, icon: 'pi pi-flag' },
      { label: 'Stories', type: 'Stories', count: project.storyCount, icon: 'pi pi-book' },
      { label: 'Actors', type: 'Actors', count: project.actorCount, icon: 'pi pi-user' },
      { label: 'Scenarios', type: 'Scenarios', count: project.scenarioCount, icon: 'pi pi-list-check' },
      { label: 'Use Cases', type: 'Use Cases', count: project.useCaseCount, icon: 'pi pi-sitemap' },
      { label: 'Glossary', type: 'Glossary', count: project.glossaryTermCount, icon: 'pi pi-list' },
      { label: 'Reports', type: 'Reports', count: project.reportGeneratorCount, icon: 'pi pi-file' },
      { label: 'Open Issues', type: 'OpenIssues', count: -1, icon: 'pi pi-exclamation-circle' },
    ];
    return groups.map(g => ({
      label: g.count >= 0 ? `${g.label} (${g.count})` : g.label,
      icon: g.icon,
      leaf: true,
      data: { type: g.type, projectName: project.name }
    }));
  }
}
