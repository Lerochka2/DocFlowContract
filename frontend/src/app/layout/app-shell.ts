import {
  ChangeDetectorRef,
  Component,
  OnInit
} from '@angular/core';

import {
  Router,
  RouterLink,
  RouterLinkActive,
  RouterOutlet
} from '@angular/router';

import {
  AuthService,
  CurrentUserResponse
} from '../core/auth/auth.service';

import {
  UiPreferencesService
} from '../core/settings/ui-preferences.service';

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive
  ],
  templateUrl: './app-shell.html',
  styleUrl: './app-shell.scss'
})
export class AppShell implements OnInit {

  currentUser: CurrentUserResponse | null = null;

  constructor(
    private readonly authService: AuthService,
    private readonly router: Router,
    private readonly changeDetectorRef: ChangeDetectorRef,
    readonly uiPreferencesService: UiPreferencesService
  ) {
  }

  ngOnInit(): void {

    const cachedUser =
      this.authService.getCurrentUser();

    if (cachedUser) {

      this.currentUser = {
        userId: cachedUser.userId,
        login: cachedUser.login,
        fullName: cachedUser.fullName,
        role: cachedUser.role
      };
    }

    this.authService
      .me()
      .subscribe({

        next: user => {

          this.currentUser =
            user;

          this.changeDetectorRef
            .markForCheck();
        },

        error: () => {

          this.authService.logout();

          void this.router.navigate(
            ['/login']
          );
        }
      });
  }

  logout(): void {

    this.authService.logout();

    void this.router.navigate(
      ['/login']
    );
  }

  getInitials(): string {

    const fullName =
      this.currentUser?.fullName
        ?.trim();

    if (!fullName) {
      return 'A';
    }

    const parts =
      fullName
        .split(/\s+/)
        .filter(Boolean);

    if (parts.length === 1) {

      return parts[0]
        .substring(0, 1)
        .toUpperCase();
    }

    return (
      parts[0].substring(0, 1) +
      parts[1].substring(0, 1)
    ).toUpperCase();
  }

  getRoleLabel(): string {

    switch (
      this.currentUser?.role
    ) {

      case 'ADMIN':
        return 'Администратор';

      case 'USER':
        return 'Пользователь';

      default:
        return 'Пользователь системы';
    }
  }
}