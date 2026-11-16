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
import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { tap } from 'rxjs';
import { AuthService } from './auth.service';

/**
 * Functional HTTP interceptor that adds the JWT Bearer token to all API requests.
 * - 401 Unauthorized: session is invalid or expired → log out.
 * - 403 Forbidden: session is valid but the user lacks permission → do not log out;
 *   let the calling component surface the error to the user. The one exception is a 403
 *   PASSWORD_CHANGE_REQUIRED (#381), which sends the user to the change-password page.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const token = authService.token();

  const outReq = (token && req.url.includes('/api/'))
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

  return next(outReq).pipe(
    tap({
      error: (err) => {
        if (err.status === 401 && !req.url.includes('/auth/login')) {
          authService.logout();
        } else if (err.status === 403 && err.error?.error === 'PASSWORD_CHANGE_REQUIRED') {
          // #381: the server refuses everything but the change; send the user there.
          authService.markPasswordChangeRequired();
          router.navigate(['/change-password']);
        }
      }
    })
  );
};
