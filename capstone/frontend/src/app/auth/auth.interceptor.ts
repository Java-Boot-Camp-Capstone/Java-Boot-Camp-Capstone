import { HttpInterceptorFn } from '@angular/common/http';

export const authInterceptor: HttpInterceptorFn = (
  req,
  next
) => {
  const token = localStorage.getItem('token');

  const request = token
    ? req.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`,
          'X-Correlation-Id': 'lab-request-001'
        }
      })
    : req;

  return next(request);
};