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
import { Component, computed, OnDestroy, OnInit, signal, ViewChild, ElementRef } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import { AccordionModule } from 'primeng/accordion';
import { ButtonModule } from 'primeng/button';
import { TreeModule } from 'primeng/tree';
import { BadgeModule } from 'primeng/badge';
import { TreeNode } from 'primeng/api';
import { AuthService } from '../core/auth.service';
import { EventStreamService } from '../core/event-stream.service';
import { ProjectService } from '../core/project.service';
import { ProjectDto, ProjectTreeNode } from '../models/project';

/**
 * Sidebar accordion navigation.
 * - Admin panel: visible for SystemAdminUserRole users
 * - Projects panel: visible for ProjectUserRole (or admin) users
 * See doc/UI_DESIGN_GUIDE.md section 3.
 */
@Component({
  selector: 'app-sidebar-nav',
  standalone: true,
  imports: [AccordionModule, ButtonModule, TreeModule, BadgeModule, RouterLink],
  template: `
    <p-accordion [multiple]="true" [value]="activePanels()">

      @if (isAdmin()) {
        <p-accordion-panel value="admin">
          <p-accordion-header>
            <span class="panel-header"><i class="pi pi-cog"></i> Admin</span>
          </p-accordion-header>
          <p-accordion-content>
            <div class="panel-actions">
              <a routerLink="/users" class="sidebar-link">
                <i class="pi pi-list"></i> List Users
              </a>
              <a routerLink="/users/new" class="sidebar-link">
                <i class="pi pi-plus"></i> Create User
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
                <p-button label="New" icon="pi pi-plus" size="small"
                          [text]="true" (onClick)="onNewProject()" />
                <p-button label="Import" icon="pi pi-upload" size="small"
                          [text]="true" (onClick)="importInput.click()" />
                <input #importInput type="file" accept=".xml"
                       (change)="onImportFile($event)" style="display:none" />
              }
              <a routerLink="/projects" class="sidebar-link">
                <i class="pi pi-list"></i> List
              </a>
            </div>

            @if (loading()) {
              <div class="tree-loading">Loading projects...</div>
            } @else {
              <p-tree [value]="projectTreeNodes()"
                      selectionMode="single" [metaKeySelection]="false"
                      (onNodeSelect)="onNodeSelect($event)"
                      styleClass="sidebar-tree" />
            }
          </p-accordion-content>
        </p-accordion-panel>
      }

    </p-accordion>
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

    .tree-loading {
      padding: 0.5rem;
      font-size: 13px;
      color: var(--p-text-secondary-color);
    }

    :host ::ng-deep .sidebar-tree .p-tree-node-label {
      font-size: 13px;
    }

    :host ::ng-deep .sidebar-tree .p-tree {
      border: none;
      padding: 0;
      background: transparent;
    }
  `]
})
export class SidebarNavComponent implements OnInit, OnDestroy {

  readonly loading = signal(false);
  private readonly projects = signal<ProjectDto[]>([]);

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
    return user?.permissions?.includes('createProjects') ?? false;
  });

  readonly activePanels = computed(() => {
    const panels: string[] = [];
    if (this.isAdmin()) panels.push('admin');
    if (this.hasProjectRole()) panels.push('projects');
    return panels;
  });

  /**
   * Build tree nodes: each project is a parent node, entity groups are children.
   * Project name click → open editor. Entity group click → open list (future).
   */
  readonly projectTreeNodes = computed<TreeNode[]>(() => {
    return this.projects().map(project => ({
      label: project.name,
      icon: 'pi pi-folder',
      expanded: false,
      data: { type: 'project', name: project.name },
      children: this.entityGroups(project)
    }));
  });

  @ViewChild('importInput') importInput!: ElementRef<HTMLInputElement>;
  private treeSub?: Subscription;
  private sseProjectSub?: Subscription;

  constructor(
    private authService: AuthService,
    private eventStreamService: EventStreamService,
    private projectService: ProjectService,
    private router: Router
  ) {}

  async ngOnInit(): Promise<void> {
    if (this.hasProjectRole()) {
      await this.loadProjects();
    }
    this.treeSub = this.projectService.onTreeChanged.subscribe(() => this.loadProjects());
    // Reload project counts when any project-scoped command succeeds (SSE broadcast)
    this.sseProjectSub = this.eventStreamService.events$
      .subscribe(envelope => {
        if (envelope.targetType === 'Project') {
          this.loadProjects();
        }
      });
  }

  ngOnDestroy(): void {
    this.treeSub?.unsubscribe();
    this.sseProjectSub?.unsubscribe();
  }

  onNewProject(): void {
    this.router.navigate(['/projects', 'new']);
  }

  async onImportFile(event: Event): Promise<void> {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    try {
      await this.projectService.importProject(file);
      await this.loadProjects();
    } catch {
      // Import errors handled by project-list if open; sidebar silently refreshes
    } finally {
      input.value = '';
    }
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
