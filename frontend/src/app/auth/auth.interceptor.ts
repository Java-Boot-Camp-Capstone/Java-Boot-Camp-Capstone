import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

export const authInterceptor: HttpInterceptorFn = (
  req,
  next
) => {
  const router = inject(Router);
  const token = localStorage.getItem('token');

  const request = token
    ? req.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`,
          'X-Correlation-Id': 'lab-request-001'
        }
      })
    : req;

  return next(request).pipe(
    catchError((error: HttpErrorResponse) => {
      // Expired / invalid token: the API answered 401, so drop the token and re-authenticate.
      if (error.status === 401 && !req.url.endsWith('/auth/login')) {
        localStorage.removeItem('token');
        router.navigate(['/login']);
      }
      return throwError(() => error);
    })
  );
};
