import {
  ChangeDetectorRef,
  Component,
  OnInit
} from '@angular/core';

import {
  FormsModule
} from '@angular/forms';

import {
  AuthService,
  CurrentUserResponse
} from '../../core/auth/auth.service';

import {
  UiPreferences,
  UiPreferencesService
} from '../../core/settings/ui-preferences.service';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [
    FormsModule
  ],
  templateUrl: './settings.html',
  styleUrl: './settings.scss'
})
export class SettingsWorkspace
  implements OnInit {

  currentUser:
    CurrentUserResponse | null = null;

  form:
    UiPreferences = {
      showAiPromoCard: true,
      showSearchShortcut: true
    };

  loadingUser = true;
  saving = false;

  successMessage = '';
  errorMessage = '';

  constructor(
    private readonly authService:
      AuthService,

    private readonly uiPreferencesService:
      UiPreferencesService,

    private readonly changeDetectorRef:
      ChangeDetectorRef
  ) {
  }

  ngOnInit(): void {

    this.loadPreferences();
    this.loadUser();
  }

  save(): void {

    this.saving = true;

    this.successMessage = '';
    this.errorMessage = '';

    try {

      this.uiPreferencesService
        .updatePreferences({
          showAiPromoCard:
            this.form.showAiPromoCard,

          showSearchShortcut:
            this.form.showSearchShortcut
        });

      this.successMessage =
        'Настройки интерфейса сохранены.';

    } catch {

      this.errorMessage =
        'Не удалось сохранить настройки.';
    }

    this.saving = false;

    this.changeDetectorRef
      .markForCheck();
  }

  reset(): void {

    const confirmed =
      window.confirm(
        'Вернуть настройки интерфейса по умолчанию?'
      );

    if (!confirmed) {
      return;
    }

    this.uiPreferencesService
      .resetPreferences();

    this.loadPreferences();

    this.errorMessage = '';

    this.successMessage =
      'Настройки по умолчанию восстановлены.';

    this.changeDetectorRef
      .markForCheck();
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
        .substring(
          0,
          1
        )
        .toUpperCase();
    }

    return (
      parts[0].substring(0, 1)
      +
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

  private loadPreferences(): void {

    const preferences =
      this.uiPreferencesService
        .preferences();

    this.form = {
      showAiPromoCard:
        preferences.showAiPromoCard,

      showSearchShortcut:
        preferences.showSearchShortcut
    };
  }

  private loadUser(): void {

    const cachedUser =
      this.authService
        .getCurrentUser();

    if (cachedUser) {

      this.currentUser = {
        userId:
          cachedUser.userId,

        login:
          cachedUser.login,

        fullName:
          cachedUser.fullName,

        role:
          cachedUser.role
      };
    }

    this.authService
      .me()
      .subscribe({

        next: user => {

          this.currentUser =
            user;

          this.loadingUser =
            false;

          this.changeDetectorRef
            .markForCheck();
        },

        error: () => {

          this.loadingUser =
            false;

          this.changeDetectorRef
            .markForCheck();
        }
      });
  }
}