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
import { authGuard } from './core/auth.guard';
import { adminGuard } from './core/admin.guard';
import { dirtyCheckGuard } from './core/dirty-check.guard';
import { LoginComponent } from './features/auth/login';
import { LayoutComponent } from './features/auth/layout';
import { DashboardComponent } from './features/auth/dashboard';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  {
    path: '',
    component: LayoutComponent,
    canActivate: [authGuard],
    children: [
      { path: '', component: DashboardComponent },
      { path: 'account', loadComponent: () => import('./features/users/edit-account').then(m => m.EditAccountComponent), canDeactivate: [dirtyCheckGuard] },
      { path: 'settings', loadComponent: () => import('./features/users/settings').then(m => m.SettingsComponent) },
      { path: 'users', canActivate: [adminGuard], loadComponent: () => import('./features/users/user-list').then(m => m.UserListComponent) },
      { path: 'users/:username', canActivate: [adminGuard], loadComponent: () => import('./features/users/user-editor').then(m => m.UserEditorComponent), canDeactivate: [dirtyCheckGuard] },
      { path: 'global-tags', canActivate: [adminGuard], loadComponent: () => import('./features/admin/global-tags').then(m => m.GlobalTagsComponent) },
      { path: 'projects', loadComponent: () => import('./features/projects/project-list').then(m => m.ProjectListComponent) },
      { path: 'projects/:name/stakeholders', loadComponent: () => import('./features/stakeholders/stakeholder-list').then(m => m.StakeholderListComponent) },
      { path: 'projects/:name/stakeholders/:stakeholderId', loadComponent: () => import('./features/stakeholders/stakeholder-editor').then(m => m.StakeholderEditorComponent), canDeactivate: [dirtyCheckGuard] },
      { path: 'projects/:name/goals', loadComponent: () => import('./features/goals/goal-list').then(m => m.GoalListComponent) },
      { path: 'projects/:name/goals/:goalId', loadComponent: () => import('./features/goals/goal-editor').then(m => m.GoalEditorComponent), canDeactivate: [dirtyCheckGuard] },
      { path: 'projects/:name/stories', loadComponent: () => import('./features/stories/story-list').then(m => m.StoryListComponent) },
      { path: 'projects/:name/stories/:storyId', loadComponent: () => import('./features/stories/story-editor').then(m => m.StoryEditorComponent), canDeactivate: [dirtyCheckGuard] },
      { path: 'projects/:name/actors', loadComponent: () => import('./features/actors/actor-list').then(m => m.ActorListComponent) },
      { path: 'projects/:name/actors/:actorId', loadComponent: () => import('./features/actors/actor-editor').then(m => m.ActorEditorComponent), canDeactivate: [dirtyCheckGuard] },
      { path: 'projects/:name/scenarios', loadComponent: () => import('./features/scenarios/scenario-list').then(m => m.ScenarioListComponent) },
      { path: 'projects/:name/scenarios/:scenarioId', loadComponent: () => import('./features/scenarios/scenario-editor').then(m => m.ScenarioEditorComponent), canDeactivate: [dirtyCheckGuard] },
      { path: 'projects/:name/use-cases', loadComponent: () => import('./features/use-cases/use-case-list').then(m => m.UseCaseListComponent) },
      { path: 'projects/:name/use-cases/:useCaseId', loadComponent: () => import('./features/use-cases/use-case-editor').then(m => m.UseCaseEditorComponent), canDeactivate: [dirtyCheckGuard] },
      { path: 'projects/:name/terms', loadComponent: () => import('./features/terms/term-list').then(m => m.TermListComponent) },
      { path: 'projects/:name/terms/:termId', loadComponent: () => import('./features/terms/term-editor').then(m => m.TermEditorComponent), canDeactivate: [dirtyCheckGuard] },
      { path: 'projects/:name/reports', loadComponent: () => import('./features/reports/report-list').then(m => m.ReportListComponent) },
      { path: 'projects/:name/reports/:reportId', loadComponent: () => import('./features/reports/report-editor').then(m => m.ReportEditorComponent), canDeactivate: [dirtyCheckGuard] },
      { path: 'projects/:name/open-issues', loadComponent: () => import('./features/open-issues/open-issues').then(m => m.OpenIssuesComponent) },
      { path: 'projects/:name', loadComponent: () => import('./features/projects/project-editor').then(m => m.ProjectEditorComponent), canDeactivate: [dirtyCheckGuard] },
    ]
  },
  { path: '**', redirectTo: '' }
];
