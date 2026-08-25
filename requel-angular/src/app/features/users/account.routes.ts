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

/** The signed-in user's own account + app settings. Rendered inside the authenticated shell. */
export const accountRoutes: Routes = [
  {
    path: 'account',
    title: 'Account',
    data: routeData({ section: 'account', breadcrumb: 'Account' }),
    loadComponent: () => import('./edit-account').then(m => m.EditAccountComponent),
    canDeactivate: [dirtyCheckGuard],
  },
  {
    path: 'settings',
    title: 'Settings',
    data: routeData({ section: 'account', breadcrumb: 'Settings' }),
    loadComponent: () => import('./settings').then(m => m.SettingsComponent),
  },
];
