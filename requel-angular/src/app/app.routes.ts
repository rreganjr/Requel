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
import { routeData } from './core/route-data';
import { LoginComponent } from './features/auth/login';
import { accountRoutes } from './features/users/account.routes';
import { adminRoutes } from './features/admin/admin.routes';
import { projectRoutes } from './features/projects/projects.routes';

/**
 * Composition root (#142). Per-domain route arrays live beside their features
 * (`account.routes.ts`, `admin.routes.ts`, `projects.routes.ts`) and are spread into the
 * authenticated shell here. Login and the `**` wildcard stay top-level. Order within the spread
 * is preserved from the domain files — notably `projects/:name` stays last (see projects.routes.ts).
 */
export const routes: Routes = [
  { path: 'login', component: LoginComponent, title: 'Sign in', data: routeData({ section: 'account' }) },
  {
    path: '',
    loadComponent: () => import('./features/auth/layout').then(m => m.LayoutComponent),
    canActivate: [authGuard],
    children: [
      {
        path: '',
        loadComponent: () => import('./features/auth/dashboard').then(m => m.DashboardComponent),
        title: 'Dashboard',
        data: routeData({ section: 'dashboard', breadcrumb: 'Dashboard' }),
      },
      ...accountRoutes,
      ...adminRoutes,
      ...projectRoutes,
    ],
  },
  { path: '**', redirectTo: '' },
];
