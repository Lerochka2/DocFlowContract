import {
  ChangeDetectorRef,
  Component,
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
  ProfileService,
  UserReminder
} from '../../core/profile/profile.service';


@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [
    FormsModule
  ],
  templateUrl: './profile.html',
  styleUrl: './profile.scss'
})
export class ProfileWorkspace
  implements OnInit, OnDestroy {

  currentUser:
    CurrentUserResponse | null = null;

  loadingUser = true;

  fullName = '';

  savingProfile = false;

  profileSuccessMessage = '';

  profileErrorMessage = '';

  reminders:
    UserReminder[] = [];

  reminderText = '';

  remindersLoading = true;

  reminderSaving = false;

  reminderErrorMessage = '';

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

    private readonly profileService:
      ProfileService,

    private readonly router:
      Router,

    private readonly changeDetectorRef:
      ChangeDetectorRef
  ) {
  }


  ngOnInit(): void {

    this.loadUser();

    this.loadReminders();
  }


  ngOnDestroy(): void {

    this.stopPasswordTimer();
  }


  saveFullName(): void {

    if (this.savingProfile) {
      return;
    }

    const value =
      this.fullName.trim();

    if (!value) {

      this.profileErrorMessage =
        'Введите ФИО.';

      this.profileSuccessMessage =
        '';

      return;
    }

    this.savingProfile = true;

    this.profileErrorMessage = '';

    this.profileSuccessMessage = '';

    this.profileService
      .updateFullName(
        value
      )
      .subscribe({

        next: user => {

          this.savingProfile = false;

          this.currentUser =
            user;

          this.fullName =
            user.fullName;

          this.authService
            .updateCachedUser(
              user
            );

          window.dispatchEvent(
            new Event(
              'docflow-user-updated'
            )
          );

          this.profileSuccessMessage =
            'ФИО успешно обновлено.';

          this.changeDetectorRef
            .markForCheck();
        },

        error: (
          error: HttpErrorResponse
        ) => {

          this.savingProfile = false;

          this.profileErrorMessage =
            this.resolveError(
              error,
              'Не удалось обновить ФИО.'
            );

          this.changeDetectorRef
            .markForCheck();
        }
      });
  }


  addReminder(): void {

    if (this.reminderSaving) {
      return;
    }

    const text =
      this.reminderText.trim();

    if (!text) {

      this.reminderErrorMessage =
        'Введите текст напоминания.';

      return;
    }

    this.reminderSaving = true;

    this.reminderErrorMessage = '';

    this.profileService
      .createReminder(
        text
      )
      .subscribe({

        next: reminder => {

          this.reminderSaving = false;

          this.reminderText = '';

          this.reminders = [
            reminder,
            ...this.reminders
          ];

          this.changeDetectorRef
            .markForCheck();
        },

        error: (
          error: HttpErrorResponse
        ) => {

          this.reminderSaving = false;

          this.reminderErrorMessage =
            this.resolveError(
              error,
              'Не удалось добавить напоминание.'
            );

          this.changeDetectorRef
            .markForCheck();
        }
      });
  }


  deleteReminder(
    reminder: UserReminder
  ): void {

    const confirmed =
      window.confirm(
        'Отметить как выполненное и удалить напоминание?'
      );

    if (!confirmed) {
      return;
    }

    this.profileService
      .deleteReminder(
        reminder.id
      )
      .subscribe({

        next: () => {

          this.reminders =
            this.reminders.filter(
              item =>
                item.id !== reminder.id
            );

          this.changeDetectorRef
            .markForCheck();
        },

        error: (
          error: HttpErrorResponse
        ) => {

          this.reminderErrorMessage =
            this.resolveError(
              error,
              'Не удалось удалить напоминание.'
            );
        }
      });
  }


  requestPasswordCode(): void {

    if (this.passwordLoading) {
      return;
    }

    if (!this.currentUser?.email) {

      this.passwordErrorMessage =
        'Для учетной записи не указан email.';

      this.passwordSuccessMessage =
        '';

      return;
    }

    this.passwordLoading = true;

    this.passwordErrorMessage = '';

    this.passwordSuccessMessage = '';

    this.authService
      .requestPasswordChangeCode()
      .subscribe({

        next: response => {

          this.passwordLoading =
            false;

          this.maskedEmail =
            response.maskedEmail;

          this.verificationCode =
            '';

          this.changeToken =
            '';

          this.newPassword =
            '';

          this.confirmNewPassword =
            '';

          this.passwordStep =
            'CODE';

          this.startPasswordTimer(
            response.expiresInSeconds,
            response.resendAvailableInSeconds,
            0
          );

          this.passwordSuccessMessage =
            `Код отправлен на ${response.maskedEmail}.`;

          this.changeDetectorRef
            .markForCheck();
        },

        error: (
          error: HttpErrorResponse
        ) => {

          this.passwordLoading =
            false;

          this.passwordErrorMessage =
            this.resolveError(
              error,
              'Не удалось отправить код подтверждения.'
            );
        }
      });
  }


  resendPasswordCode(): void {

    if (this.passwordLoading) {
      return;
    }

    if (
      this.resendAvailableInSeconds > 0
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
      this.verificationCode.trim();

    if (!/^\d{6}$/.test(code)) {

      this.passwordErrorMessage =
        'Введите шестизначный код подтверждения.';

      this.passwordSuccessMessage =
        '';

      return;
    }

    if (
      this.codeExpiresInSeconds <= 0
    ) {

      this.passwordErrorMessage =
        'Срок действия кода истек. Запросите новый код.';

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

          this.passwordLoading =
            false;

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

          this.passwordLoading =
            false;

          this.passwordErrorMessage =
            this.resolveError(
              error,
              'Не удалось подтвердить код.'
            );
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

      return;
    }

    if (
      this.newPassword.length < 8
    ) {

      this.passwordErrorMessage =
        'Пароль должен содержать не менее 8 символов.';

      return;
    }

    if (
      this.newPassword
      !== this.confirmNewPassword
    ) {

      this.passwordErrorMessage =
        'Пароли не совпадают.';

      return;
    }

    if (
      this.changeTokenExpiresInSeconds <= 0
    ) {

      this.passwordErrorMessage =
        'Время подтверждения истекло. Начните заново.';

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

          this.passwordLoading =
            false;

          this.stopPasswordTimer();

          this.passwordSuccessMessage =
            'Пароль изменен. Выполняется выход из системы...';

          window.setTimeout(
            () => {

              this.authService
                .logout();

              void this.router.navigate(
                ['/login']
              );
            },
            1200
          );
        },

        error: (
          error: HttpErrorResponse
        ) => {

          this.passwordLoading =
            false;

          this.passwordErrorMessage =
            this.resolveError(
              error,
              'Не удалось изменить пароль.'
            );
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
        Math.floor(codeSeconds)
      );

    this.resendAvailableInSeconds =
      Math.max(
        0,
        Math.floor(resendSeconds)
      );

    this.changeTokenExpiresInSeconds =
      Math.max(
        0,
        Math.floor(tokenSeconds)
      );

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

          this.changeDetectorRef
            .markForCheck();

          if (
            this.codeExpiresInSeconds === 0
          ) {

            if (
              this.resendAvailableInSeconds === 0
            ) {

              if (
                this.changeTokenExpiresInSeconds === 0
              ) {
                this.stopPasswordTimer();
              }
            }
          }
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

    const safe =
      Math.max(
        0,
        Math.floor(totalSeconds)
      );

    const minutes =
      Math.floor(
        safe / 60
      );

    const seconds =
      safe % 60;

    return (
      minutes
        .toString()
        .padStart(2, '0')
      +
      ':'
      +
      seconds
        .toString()
        .padStart(2, '0')
    );
  }


  getInitials(): string {

    const value =
      this.currentUser?.fullName
        ?.trim();

    if (!value) {
      return 'U';
    }

    const parts =
      value
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

      case 'DOCUMENT_OPERATOR':
        return 'Делопроизводитель';

      case 'LAWYER':
        return 'Юрист';

      case 'CONTRACT_MANAGER':
        return 'Менеджер по договорам';

      default:
        return 'Пользователь';
    }
  }


  formatReminderDate(
    value: string
  ): string {

    const date =
      new Date(value);

    if (
      Number.isNaN(
        date.getTime()
      )
    ) {
      return value;
    }

    return new Intl.DateTimeFormat(
      'ru-RU',
      {
        day: '2-digit',
        month: 'long',
        hour: '2-digit',
        minute: '2-digit'
      }
    ).format(date);
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

      this.fullName =
        cachedUser.fullName;

      this.loadingUser =
        false;
    }

    this.authService
      .me()
      .subscribe({

        next: user => {

          this.currentUser =
            user;

          this.fullName =
            user.fullName;

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


  private loadReminders(): void {

    this.remindersLoading = true;

    this.profileService
      .getReminders()
      .subscribe({

        next: reminders => {

          this.reminders =
            reminders;

          this.remindersLoading =
            false;

          this.changeDetectorRef
            .markForCheck();
        },

        error: (
          error: HttpErrorResponse
        ) => {

          this.remindersLoading =
            false;

          this.reminderErrorMessage =
            this.resolveError(
              error,
              'Не удалось загрузить напоминания.'
            );

          this.changeDetectorRef
            .markForCheck();
        }
      });
  }


  private resolveError(
    error: HttpErrorResponse,
    fallback: string
  ): string {

    const detail =
      error.error?.detail;

    if (
      typeof detail === 'string'
    ) {

      if (detail.trim()) {
        return detail;
      }
    }

    const message =
      error.error?.message;

    if (
      typeof message === 'string'
    ) {

      if (message.trim()) {
        return message;
      }
    }

    return fallback;
  }
}