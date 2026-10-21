
import { HttpInterceptorFn } from '@angular/common/http';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const token = localStorage.getItem('token');

  const isBackendRequest =
    req.url.startsWith('http://localhost:8080/');

  const isLoginRequest =
    req.url.includes('/api/v1/auth/login');

  if (!token || !isBackendRequest || isLoginRequest) {
    return next(req);
  }

  return next(
    req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    })
  );
};
