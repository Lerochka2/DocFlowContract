import { DomSanitizer } from '@angular/platform-browser';
import {
  ChangeDetectorRef,
  Component,
  inject,
  OnDestroy,
  OnInit
} from '@angular/core';

import {
  FormsModule
} from '@angular/forms';

import {
  HttpErrorResponse
} from '@angular/common/http';

import {
  Router
} from '@angular/router';

import {
  AuthService,
  CurrentUserResponse
} from '../../core/auth/auth.service';

import {
  DEFAULT_UI_PREFERENCES,
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
  implements OnInit, OnDestroy {

  // Fixed local stylesheet asset preserves the initial bundle budget.
  readonly workspaceStyles = inject(DomSanitizer).bypassSecurityTrustResourceUrl(
    'assets/settings/settings-workspace.css'
  );

  currentUser:
    CurrentUserResponse | null = null;

  form: UiPreferences = { ...DEFAULT_UI_PREFERENCES };

  loadingUser = true;
  saving = false;

  successMessage = '';
  errorMessage = '';


  passwordStep:
    'EMAIL'
    | 'CODE'
    | 'PASSWORD'
    = 'EMAIL';

  passwordLoading = false;

  passwordSuccessMessage = '';
  passwordErrorMessage = '';

  maskedEmail = '';

  verificationCode = '';

  changeToken = '';

  newPassword = '';
  confirmNewPassword = '';

  codeExpiresInSeconds = 0;
  resendAvailableInSeconds = 0;

  changeTokenExpiresInSeconds = 0;

  private passwordTimerId:
    number | null = null;

  constructor(
    private readonly authService:
      AuthService,

    private readonly router:
      Router,

    private readonly uiPreferencesService:
      UiPreferencesService,

    private readonly changeDetectorRef:
      ChangeDetectorRef
  ) {
  }

  get startPages() { return this.uiPreferencesService.availablePages; }

  ngOnInit(): void {
    this.uiPreferencesService.activateUser();

    this.loadPreferences();
    this.loadUser();
  }

  ngOnDestroy(): void {

    this.stopPasswordTimer();
  }


  isAdmin(): boolean {

    return (
      this.currentUser?.role
      === 'ADMIN'
    );
  }

  save(): void {

    this.saving = true;

    this.successMessage = '';
    this.errorMessage = '';

    try {

      this.uiPreferencesService
        .updatePreferences(this.form);

      this.successMessage =
        'Персональные настройки сохранены для этого браузера.';

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

  requestPasswordCode(): void {

    if (this.passwordLoading) {
      return;
    }

    if (
      !this.currentUser?.email
    ) {

      this.passwordErrorMessage =
        'Для учетной записи не указан email. Обратитесь к администратору.';

      this.passwordSuccessMessage = '';

      return;
    }

    this.passwordLoading = true;

    this.passwordErrorMessage = '';
    this.passwordSuccessMessage = '';

    this.authService
      .requestPasswordChangeCode()
      .subscribe({

        next: response => {

          this.passwordLoading = false;

          this.maskedEmail =
            response.maskedEmail;

          this.verificationCode = '';

          this.changeToken = '';

          this.newPassword = '';
          this.confirmNewPassword = '';

          this.passwordStep =
            'CODE';

          this.startPasswordTimer(
            response.expiresInSeconds,
            response.resendAvailableInSeconds,
            0
          );

          this.passwordSuccessMessage =
            `Код подтверждения отправлен на ${response.maskedEmail}.`;

          this.changeDetectorRef
            .markForCheck();
        },

        error: (
          error: HttpErrorResponse
        ) => {

          this.passwordLoading = false;

          this.passwordErrorMessage =
            this.resolvePasswordError(
              error,
              'Не удалось отправить код подтверждения.'
            );

          this.changeDetectorRef
            .markForCheck();
        }
      });
  }


  resendPasswordCode(): void {

    if (
      this.resendAvailableInSeconds > 0
      ||
      this.passwordLoading
    ) {
      return;
    }

    this.requestPasswordCode();
  }


  verifyPasswordCode(): void {

    if (this.passwordLoading) {
      return;
    }

    const code =
      this.verificationCode
        .trim();

    if (
      !/^\d{6}$/.test(
        code
      )
    ) {

      this.passwordErrorMessage =
        'Введите шестизначный код подтверждения.';

      this.passwordSuccessMessage = '';

      return;
    }

    if (
      this.codeExpiresInSeconds <= 0
    ) {

      this.passwordErrorMessage =
        'Срок действия кода истек. Запросите новый код.';

      this.passwordSuccessMessage = '';

      return;
    }

    this.passwordLoading = true;

    this.passwordErrorMessage = '';
    this.passwordSuccessMessage = '';

    this.authService
      .verifyPasswordChangeCode(
        code
      )
      .subscribe({

        next: response => {

          this.passwordLoading = false;

          this.changeToken =
            response.changeToken;

          this.passwordStep =
            'PASSWORD';

          this.startPasswordTimer(
            0,
            0,
            response.expiresInSeconds
          );

          this.passwordSuccessMessage =
            'Код подтвержден. Теперь задайте новый пароль.';

          this.changeDetectorRef
            .markForCheck();
        },

        error: (
          error: HttpErrorResponse
        ) => {

          this.passwordLoading = false;

          this.passwordErrorMessage =
            this.resolvePasswordError(
              error,
              'Не удалось подтвердить код.'
            );

          this.changeDetectorRef
            .markForCheck();
        }
      });
  }


  confirmPasswordChange(): void {

    if (this.passwordLoading) {
      return;
    }

    if (!this.changeToken) {

      this.passwordErrorMessage =
        'Сначала подтвердите код из письма.';

      this.passwordSuccessMessage = '';

      return;
    }

    if (
      this.changeTokenExpiresInSeconds
      <= 0
    ) {

      this.passwordErrorMessage =
        'Срок подтверждения истек. Запросите новый код.';

      this.passwordSuccessMessage = '';

      return;
    }

    if (
      this.newPassword.length < 8
    ) {

      this.passwordErrorMessage =
        'Новый пароль должен содержать не менее 8 символов.';

      this.passwordSuccessMessage = '';

      return;
    }

    if (
      this.newPassword
      !== this.confirmNewPassword
    ) {

      this.passwordErrorMessage =
        'Новые пароли не совпадают.';

      this.passwordSuccessMessage = '';

      return;
    }

    this.passwordLoading = true;

    this.passwordErrorMessage = '';
    this.passwordSuccessMessage = '';

    this.authService
      .confirmPasswordChange({
        changeToken:
          this.changeToken,

        newPassword:
          this.newPassword,

        confirmPassword:
          this.confirmNewPassword
      })
      .subscribe({

        next: () => {

          this.passwordLoading = false;

          this.stopPasswordTimer();

          this.passwordSuccessMessage =
            'Пароль успешно изменен. Выполняется выход из системы...';

          this.passwordErrorMessage = '';

          this.changeDetectorRef
            .markForCheck();

          window.setTimeout(
            () => {

              this.authService
                .logout();

              void this.router
                .navigate(
                  ['/login']
                );
            },
            1200
          );
        },

        error: (
          error: HttpErrorResponse
        ) => {

          this.passwordLoading = false;

          this.passwordErrorMessage =
            this.resolvePasswordError(
              error,
              'Не удалось изменить пароль.'
            );

          this.changeDetectorRef
            .markForCheck();
        }
      });
  }


  resetPasswordChange(): void {

    this.stopPasswordTimer();

    this.passwordStep =
      'EMAIL';

    this.passwordLoading =
      false;

    this.passwordSuccessMessage =
      '';

    this.passwordErrorMessage =
      '';

    this.maskedEmail =
      '';

    this.verificationCode =
      '';

    this.changeToken =
      '';

    this.newPassword =
      '';

    this.confirmNewPassword =
      '';

    this.codeExpiresInSeconds =
      0;

    this.resendAvailableInSeconds =
      0;

    this.changeTokenExpiresInSeconds =
      0;

    this.changeDetectorRef
      .markForCheck();
  }


  getCodeExpiresLabel(): string {

    return this.formatSeconds(
      this.codeExpiresInSeconds
    );
  }


  getResendLabel(): string {

    return this.formatSeconds(
      this.resendAvailableInSeconds
    );
  }


  getChangeTokenExpiresLabel(): string {

    return this.formatSeconds(
      this.changeTokenExpiresInSeconds
    );
  }


  private startPasswordTimer(
    codeSeconds: number,
    resendSeconds: number,
    tokenSeconds: number
  ): void {

    this.stopPasswordTimer();

    this.codeExpiresInSeconds =
      Math.max(
        0,
        Math.floor(
          codeSeconds
        )
      );

    this.resendAvailableInSeconds =
      Math.max(
        0,
        Math.floor(
          resendSeconds
        )
      );

    this.changeTokenExpiresInSeconds =
      Math.max(
        0,
        Math.floor(
          tokenSeconds
        )
      );

    if (
      this.codeExpiresInSeconds === 0
      &&
      this.resendAvailableInSeconds === 0
      &&
      this.changeTokenExpiresInSeconds === 0
    ) {
      return;
    }

    this.passwordTimerId =
      window.setInterval(
        () => {

          if (
            this.codeExpiresInSeconds > 0
          ) {
            this.codeExpiresInSeconds--;
          }

          if (
            this.resendAvailableInSeconds > 0
          ) {
            this.resendAvailableInSeconds--;
          }

          if (
            this.changeTokenExpiresInSeconds > 0
          ) {
            this.changeTokenExpiresInSeconds--;
          }

          if (
            this.codeExpiresInSeconds === 0
            &&
            this.resendAvailableInSeconds === 0
            &&
            this.changeTokenExpiresInSeconds === 0
          ) {

            this.stopPasswordTimer();
          }

          this.changeDetectorRef
            .markForCheck();
        },
        1000
      );
  }


  private stopPasswordTimer(): void {

    if (
      this.passwordTimerId === null
    ) {
      return;
    }

    window.clearInterval(
      this.passwordTimerId
    );

    this.passwordTimerId =
      null;
  }


  private formatSeconds(
    totalSeconds: number
  ): string {

    const safeSeconds =
      Math.max(
        0,
        Math.floor(
          totalSeconds
        )
      );

    const minutes =
      Math.floor(
        safeSeconds / 60
      );

    const seconds =
      safeSeconds % 60;

    return (
      minutes
        .toString()
        .padStart(
          2,
          '0'
        )
      +
      ':'
      +
      seconds
        .toString()
        .padStart(
          2,
          '0'
        )
    );
  }


  private resolvePasswordError(
    error: HttpErrorResponse,
    fallback: string
  ): string {

    const detail =
      error.error?.detail;

    if (
      typeof detail === 'string'
      &&
      detail.trim()
    ) {
      return detail;
    }

    const message =
      error.error?.message;

    if (
      typeof message === 'string'
      &&
      message.trim()
    ) {
      return message;
    }

    if (
      typeof error.error === 'string'
      &&
      error.error.trim()
    ) {
      return error.error;
    }

    return fallback;
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
        return 'Администратор системы';

      case 'DOCUMENT_OPERATOR':
        return 'Делопроизводитель';

      case 'LAWYER':
        return 'Юрист';

      case 'CONTRACT_MANAGER':
        return 'Менеджер по договорам';

      default:
        return 'Пользователь системы';
    }
  }

  private loadPreferences(): void {

    const preferences =
      this.uiPreferencesService
        .preferences();

    this.form = { ...preferences };
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

        email:
          cachedUser.email ?? null,

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