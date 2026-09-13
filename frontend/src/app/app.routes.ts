import {
  Routes
} from '@angular/router';

import {
  AppShell
} from './layout/app-shell';

import {
  Dashboard
} from './pages/dashboard/dashboard';

import {
  Documents
} from './pages/documents/documents';

import {
  Login
} from './pages/login/login';

import {
  authGuard
} from './core/auth/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    component: Login
  },
  {
    path: '',
    component: AppShell,
    canActivate: [
      authGuard
    ],
    children: [
      {
        path: 'dashboard',
        component: Dashboard
      },
      {
        path: 'documents',
        component: Documents
      },
      {
        path: '',
        pathMatch: 'full',
        redirectTo: 'dashboard'
      }
    ]
  },
  {
    path: '**',
    redirectTo: 'dashboard'
  }
];