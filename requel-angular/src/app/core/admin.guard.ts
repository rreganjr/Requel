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
import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

/**
 * Functional route guard that restricts access to admin-only routes
 * (e.g. `/users`, `/users/:username`). Composes with `authGuard` —
 * unauthenticated users are redirected to `/login` by the parent layout
 * route's `authGuard`, then this guard runs on top to gate the admin
 * subset of the authenticated surface.
 *
 * Non-admin users are redirected to the dashboard (`/`) rather than
 * `/login`, since they ARE authenticated and a /login redirect would be
 * misleading. The sidebar nav already hides admin links from non-admins
 * (see `SidebarNavComponent.isAdmin`), so this guard is the second
 * line of defence for direct URL navigation / typed-in URLs / shared
 * deep links.
 */
export const adminGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const user = authService.user();
  if (user?.roles?.includes('SystemAdminUserRole')) {
    return true;
  }

  return router.createUrlTree(['/']);
};
