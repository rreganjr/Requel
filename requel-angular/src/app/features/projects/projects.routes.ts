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
import { Routes } from '@angular/router';
import { dirtyCheckGuard } from '../../core/dirty-check.guard';
import { routeData } from '../../core/route-data';

/**
 * Project list + every project-scoped route. `projects/:name` (the project editor) MUST stay LAST:
 * the multi-segment `projects/:name/<artifact>` routes are listed before it so matching order is
 * preserved from the original flat config (see doc/142-route-structure-plan.md §9).
 */
export const projectRoutes: Routes = [
  {
    path: 'projects',
    title: 'Projects',
    data: routeData({ section: 'project', artifactType: 'project', breadcrumb: 'Projects' }),
    loadComponent: () => import('./project-list').then(m => m.ProjectListComponent),
  },

  {
    path: 'projects/:name/stakeholders',
    title: 'Stakeholders',
    data: routeData({ section: 'project', artifactType: 'stakeholder', breadcrumb: 'Stakeholders' }),
    loadComponent: () => import('../stakeholders/stakeholder-list').then(m => m.StakeholderListComponent),
  },
  {
    path: 'projects/:name/stakeholders/:stakeholderId',
    title: 'Stakeholder',
    data: routeData({ section: 'project', artifactType: 'stakeholder' }),
    loadComponent: () => import('../stakeholders/stakeholder-editor').then(m => m.StakeholderEditorComponent),
    canDeactivate: [dirtyCheckGuard],
  },

  {
    path: 'projects/:name/goals',
    title: 'Goals',
    data: routeData({ section: 'project', artifactType: 'goal', breadcrumb: 'Goals' }),
    loadComponent: () => import('../goals/goal-list').then(m => m.GoalListComponent),
  },
  {
    path: 'projects/:name/goals/:goalId',
    title: 'Goal',
    data: routeData({ section: 'project', artifactType: 'goal' }),
    loadComponent: () => import('../goals/goal-editor').then(m => m.GoalEditorComponent),
    canDeactivate: [dirtyCheckGuard],
  },

  {
    path: 'projects/:name/stories',
    title: 'Stories',
    data: routeData({ section: 'project', artifactType: 'story', breadcrumb: 'Stories' }),
    loadComponent: () => import('../stories/story-list').then(m => m.StoryListComponent),
  },
  {
    path: 'projects/:name/stories/:storyId',
    title: 'Story',
    data: routeData({ section: 'project', artifactType: 'story' }),
    loadComponent: () => import('../stories/story-editor').then(m => m.StoryEditorComponent),
    canDeactivate: [dirtyCheckGuard],
  },

  {
    path: 'projects/:name/actors',
    title: 'Actors',
    data: routeData({ section: 'project', artifactType: 'actor', breadcrumb: 'Actors' }),
    loadComponent: () => import('../actors/actor-list').then(m => m.ActorListComponent),
  },
  {
    path: 'projects/:name/actors/:actorId',
    title: 'Actor',
    data: routeData({ section: 'project', artifactType: 'actor' }),
    loadComponent: () => import('../actors/actor-editor').then(m => m.ActorEditorComponent),
    canDeactivate: [dirtyCheckGuard],
  },

  {
    path: 'projects/:name/scenarios',
    title: 'Scenarios',
    data: routeData({ section: 'project', artifactType: 'scenario', breadcrumb: 'Scenarios' }),
    loadComponent: () => import('../scenarios/scenario-list').then(m => m.ScenarioListComponent),
  },
  {
    path: 'projects/:name/scenarios/:scenarioId',
    title: 'Scenario',
    data: routeData({ section: 'project', artifactType: 'scenario' }),
    loadComponent: () => import('../scenarios/scenario-editor').then(m => m.ScenarioEditorComponent),
    canDeactivate: [dirtyCheckGuard],
  },

  {
    path: 'projects/:name/use-cases',
    title: 'Use cases',
    data: routeData({ section: 'project', artifactType: 'use-case', breadcrumb: 'Use cases' }),
    loadComponent: () => import('../use-cases/use-case-list').then(m => m.UseCaseListComponent),
  },
  {
    path: 'projects/:name/use-cases/:useCaseId',
    title: 'Use case',
    data: routeData({ section: 'project', artifactType: 'use-case' }),
    loadComponent: () => import('../use-cases/use-case-editor').then(m => m.UseCaseEditorComponent),
    canDeactivate: [dirtyCheckGuard],
  },

  {
    path: 'projects/:name/terms',
    title: 'Terms',
    data: routeData({ section: 'project', artifactType: 'term', breadcrumb: 'Terms' }),
    loadComponent: () => import('../terms/term-list').then(m => m.TermListComponent),
  },
  {
    path: 'projects/:name/terms/:termId',
    title: 'Term',
    data: routeData({ section: 'project', artifactType: 'term' }),
    loadComponent: () => import('../terms/term-editor').then(m => m.TermEditorComponent),
    canDeactivate: [dirtyCheckGuard],
  },

  {
    path: 'projects/:name/reports',
    title: 'Reports',
    data: routeData({ section: 'project', artifactType: 'report', breadcrumb: 'Reports' }),
    loadComponent: () => import('../reports/report-list').then(m => m.ReportListComponent),
  },
  {
    path: 'projects/:name/reports/:reportId',
    title: 'Report',
    data: routeData({ section: 'project', artifactType: 'report' }),
    loadComponent: () => import('../reports/report-editor').then(m => m.ReportEditorComponent),
    canDeactivate: [dirtyCheckGuard],
  },

  {
    path: 'projects/:name/open-issues',
    title: 'Open issues',
    data: routeData({ section: 'project', artifactType: 'open-issue', breadcrumb: 'Open issues' }),
    loadComponent: () => import('../open-issues/open-issues').then(m => m.OpenIssuesComponent),
  },

  {
    path: 'projects/:name',
    title: 'Project',
    data: routeData({ section: 'project', artifactType: 'project' }),
    loadComponent: () => import('./project-editor').then(m => m.ProjectEditorComponent),
    canDeactivate: [dirtyCheckGuard],
  },
];
