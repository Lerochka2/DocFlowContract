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
  DocumentDetails
} from './pages/document-details/document-details';

import {
  AiAnalysisWorkspace
} from './pages/ai-analysis/ai-analysis';

import {
  ReviewQueue
} from './pages/review/review';

import {
  RoutingWorkspace
} from './pages/routing/routing';

import {
  SedWorkspace
} from './pages/sed/sed';

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
        path: 'documents/:id',
        component: DocumentDetails
      },
      {
        path: 'ai-analysis',
        component: AiAnalysisWorkspace
      },
      {
        path: 'review',
        component: ReviewQueue
      },
      {
        path: 'routing',
        component: RoutingWorkspace
      },
      {
        path: 'sed',
        component: SedWorkspace
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