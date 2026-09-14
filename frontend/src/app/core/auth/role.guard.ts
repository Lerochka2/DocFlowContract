import {
  inject
} from '@angular/core';

import {
  CanActivateFn,
  Router
} from '@angular/router';

import {
  AuthService
} from './auth.service';

export const roleGuard: CanActivateFn = (
  route
) => {

  const authService =
    inject(AuthService);

  const router =
    inject(Router);

  const user =
    authService.getCurrentUser();

  if (!user) {

    return router.createUrlTree(
      ['/login']
    );
  }

  const allowedRoles =
    route.data?.['roles'] as
      string[] | undefined;

  if (
    !allowedRoles
    ||
    allowedRoles.length === 0
  ) {
    return true;
  }

  if (
    allowedRoles.includes(
      user.role
    )
  ) {
    return true;
  }

  return router.createUrlTree(
    ['/dashboard']
  );
};