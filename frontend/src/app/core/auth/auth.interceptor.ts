import {
  HttpInterceptorFn
} from '@angular/common/http';

import {
  inject
} from '@angular/core';

import {
  AuthService
} from './auth.service';

export const authInterceptor: HttpInterceptorFn = (
  request,
  next
) => {

  const authService = inject(AuthService);

  const token = authService.getToken();

  const isBackendRequest =
    request.url.startsWith(
      'http://localhost:8080/api/'
    );

  const isLoginRequest =
    request.url.endsWith(
      '/api/auth/login'
    );

  if (
    token &&
    isBackendRequest &&
    !isLoginRequest && !request.url.includes('/api/public/')
  ) {

    const authorizedRequest = request.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });

    return next(authorizedRequest);
  }

  return next(request);
};