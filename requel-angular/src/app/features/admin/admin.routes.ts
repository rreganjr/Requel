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
import { adminGuard } from '../../core/admin.guard';
import { dirtyCheckGuard } from '../../core/dirty-check.guard';
import { routeData } from '../../core/route-data';

/** Admin-only management pages. Every route is gated by `adminGuard`. */
export const adminRoutes: Routes = [
  {
    path: 'users',
    canActivate: [adminGuard],
    title: 'Users',
    data: routeData({ section: 'admin', artifactType: 'user', breadcrumb: 'Users' }),
    loadComponent: () => import('../users/user-list').then(m => m.UserListComponent),
  },
  {
    path: 'users/:username',
    canActivate: [adminGuard],
    title: 'User',
    data: routeData({ section: 'admin', artifactType: 'user' }),
    loadComponent: () => import('../users/user-editor').then(m => m.UserEditorComponent),
    canDeactivate: [dirtyCheckGuard],
  },
  {
    path: 'global-tags',
    canActivate: [adminGuard],
    title: 'Global tags',
    data: routeData({ section: 'admin', breadcrumb: 'Global tags' }),
    loadComponent: () => import('./global-tags').then(m => m.GlobalTagsComponent),
  },
  {
    path: 'tag-categories',
    canActivate: [adminGuard],
    title: 'Tag categories',
    data: routeData({ section: 'admin', breadcrumb: 'Tag categories' }),
    loadComponent: () => import('./tag-categories').then(m => m.TagCategoriesComponent),
  },
];
