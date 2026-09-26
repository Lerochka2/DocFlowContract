import {
  Routes
} from '@angular/router';

import {
  AppShell
} from './layout/app-shell';

import {
  Home
} from './pages/home/home';

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
  UsersWorkspace
} from './pages/users/users';

import {
  SettingsWorkspace
} from './pages/settings/settings';

import {
  ProfileWorkspace
} from './pages/profile/profile';

import {
  Login
} from './pages/login/login';

import {
  authGuard
} from './core/auth/auth.guard';

import {
  roleGuard
} from './core/auth/role.guard';

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
        path: 'home',
        component: Home
      },
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
        component: AiAnalysisWorkspace,
        canActivate: [
          roleGuard
        ],
        data: {
          roles: [
            'ADMIN',
            'DOCUMENT_OPERATOR'
          ]
        }
      },
      {
        path: 'review',
        component: ReviewQueue,
        canActivate: [
          roleGuard
        ],
        data: {
          roles: [
            'ADMIN',
            'DOCUMENT_OPERATOR'
          ]
        }
      },
      {
        path: 'routing',
        component: RoutingWorkspace,
        canActivate: [
          roleGuard
        ],
        data: {
          roles: [
            'ADMIN',
            'LAWYER',
            'CONTRACT_MANAGER'
          ]
        }
      },
      {
        path: 'sed',
        component: SedWorkspace,
        canActivate: [
          roleGuard
        ],
        data: {
          roles: [
            'ADMIN',
            'DOCUMENT_OPERATOR'
          ]
        }
      },
      {
        path: 'users',
        component: UsersWorkspace,
        canActivate: [
          roleGuard
        ],
        data: {
          roles: [
            'ADMIN'
          ]
        }
      },
      {
        path: 'settings',
        component: SettingsWorkspace
      },
      {
        path: 'profile',
        component: ProfileWorkspace
      },
      {
        path: '',
        pathMatch: 'full',
        redirectTo: 'home'
      }
    ]
  },
  {
    path: '**',
    redirectTo: 'home'
  }
];