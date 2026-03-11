import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { tap } from 'rxjs';
import { AuthService } from './auth.service';

/**
 * Functional HTTP interceptor that adds the JWT Bearer token to all API requests
 * and handles 401/403 responses by redirecting to login.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const token = authService.token();

  const outReq = (token && req.url.includes('/api/'))
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

  return next(outReq).pipe(
    tap({
      error: (err) => {
        if ((err.status === 401 || err.status === 403) && !req.url.includes('/auth/login')) {
          authService.logout();
        }
      }
    })
  );
};
