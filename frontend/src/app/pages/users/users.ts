import { DomSanitizer } from '@angular/platform-browser';
import {
  ChangeDetectorRef,
  Component,
  inject,
  OnInit
} from '@angular/core';

import {
  FormsModule
} from '@angular/forms';

import {
  HttpErrorResponse
} from '@angular/common/http';

import {
  finalize,
  forkJoin
} from 'rxjs';

import {
  AuthService
} from '../../core/auth/auth.service';

import {
  AdminUser,
  CreateUserRequest,
  RoleOption,
  UserAdminService
} from '../../core/users/user-admin.service';


@Component({
  selector: 'app-users',
  standalone: true,
  imports: [
    FormsModule
  ],
  templateUrl: './users.html',
  styleUrl: './users.scss'
})
export class UsersWorkspace
  implements OnInit {

  // Fixed local stylesheet asset keeps the visual layer out of the initial JS bundle.
  readonly workspaceStyles = inject(DomSanitizer).bypassSecurityTrustResourceUrl(
    'assets/users/users-workspace.css'
  );

  users: AdminUser[] = [];
  roles: RoleOption[] = [];

  currentUserId: number | null = null;

  loading = true;
  submitting = false;

  errorMessage = '';
  successMessage = '';

  searchQuery = '';

  editingUserId: number | null = null;
  editingFullName = '';

  editingEmailUserId: number | null = null;
  editingEmail = '';

  createForm: CreateUserRequest = {
    login: '',
    fullName: '',
    email: '',
    password: '',
    roleCode: 'DOCUMENT_OPERATOR'
  };


  constructor(
    private readonly userAdminService:
      UserAdminService,

    private readonly authService:
      AuthService,

    private readonly changeDetectorRef:
      ChangeDetectorRef
  ) {
  }


  ngOnInit(): void {

    this.currentUserId =
      this.authService
        .getCurrentUser()
        ?.userId
      ?? null;

    this.loadData();
  }


  loadData(): void {

    this.loading = true;
    this.errorMessage = '';

    forkJoin({
      users:
        this.userAdminService.getUsers(),

      roles:
        this.userAdminService.getRoles()
    })
      .pipe(
        finalize(
          () => {

            this.loading = false;

            this.changeDetectorRef
              .markForCheck();
          }
        )
      )
      .subscribe({

        next: response => {

          this.users =
            response.users;

          this.roles =
            response.roles;

          this.changeDetectorRef
            .markForCheck();
        },

        error: () => {

          this.errorMessage =
            'Не удалось загрузить пользователей.';

          this.changeDetectorRef
            .markForCheck();
        }
      });
  }


  get filteredUsers(): AdminUser[] {

    const query =
      this.searchQuery
        .trim()
        .toLowerCase();

    if (!query) {
      return this.users;
    }

    return this.users.filter(
      user =>
        user.login
          .toLowerCase()
          .includes(query)
        ||
        user.fullName
          .toLowerCase()
          .includes(query)
        ||
        (user.email ?? '')
          .toLowerCase()
          .includes(query)
        ||
        user.roleName
          .toLowerCase()
          .includes(query)
    );
  }


  createUser(): void {

    if (this.submitting) {
      return;
    }

    this.successMessage = '';
    this.errorMessage = '';

    const login =
      this.createForm.login.trim();

    const fullName =
      this.createForm.fullName.trim();

    const email =
      (this.createForm.email ?? '')
        .trim()
        .toLowerCase();

    const password =
      this.createForm.password;

    if (
      !login
      ||
      !fullName
      ||
      !email
      ||
      !password
      ||
      !this.createForm.roleCode
    ) {

      this.errorMessage =
        'Заполните все поля нового пользователя.';

      return;
    }

    if (
      !/^[^\s@]+@[^\s@]+\.[^\s@]+$/
        .test(email)
    ) {

      this.errorMessage =
        'Некорректный формат email.';

      return;
    }


    if (password.length < 8) {

      this.errorMessage =
        'Пароль должен содержать не менее 8 символов.';

      return;
    }

    this.submitting = true;

    this.userAdminService
      .createUser({
        login,
        fullName,
        email,
        password,
        roleCode:
          this.createForm.roleCode
      })
      .pipe(
        finalize(
          () => {

            this.submitting = false;

            this.changeDetectorRef
              .markForCheck();
          }
        )
      )
      .subscribe({

        next: user => {

          this.users = [
            ...this.users,
            user
          ];

          this.createForm = {
            login: '',
            fullName: '',
            email: '',
            password: '',
            roleCode:
              'DOCUMENT_OPERATOR'
          };

          this.successMessage =
            'Пользователь успешно создан.';

          this.changeDetectorRef
            .markForCheck();
        },

        error: (
          error: HttpErrorResponse
        ) => {

          this.errorMessage =
            this.resolveError(
              error,
              'Не удалось создать пользователя.'
            );

          this.changeDetectorRef
            .markForCheck();
        }
      });
  }


  startNameEdit(
    user: AdminUser
  ): void {

    this.errorMessage = '';
    this.successMessage = '';

    this.editingUserId =
      user.userId;

    this.editingFullName =
      user.fullName;
  }


  cancelNameEdit(): void {

    this.editingUserId = null;
    this.editingFullName = '';
  }


  saveFullName(
    user: AdminUser
  ): void {

    const fullName =
      this.editingFullName.trim();

    if (!fullName) {

      this.errorMessage =
        'ФИО не может быть пустым.';

      return;
    }

    if (
      fullName
      === user.fullName
    ) {

      this.cancelNameEdit();

      return;
    }

    this.errorMessage = '';
    this.successMessage = '';

    this.userAdminService
      .updateFullName(
        user.userId,
        fullName
      )
      .subscribe({

        next: updated => {

          this.replaceUser(
            updated
          );

          this.cancelNameEdit();

          this.successMessage =
            `ФИО пользователя ${updated.login} изменено.`;

          this.changeDetectorRef
            .markForCheck();
        },

        error: (
          error: HttpErrorResponse
        ) => {

          this.errorMessage =
            this.resolveError(
              error,
              'Не удалось изменить ФИО пользователя.'
            );

          this.changeDetectorRef
            .markForCheck();
        }
      });
  }

  startEmailEdit(
    user: AdminUser
  ): void {

    this.errorMessage = '';
    this.successMessage = '';

    this.editingEmailUserId =
      user.userId;

    this.editingEmail =
      user.email ?? '';
  }


  cancelEmailEdit(): void {

    this.editingEmailUserId =
      null;

    this.editingEmail =
      '';
  }


  saveEmail(
    user: AdminUser
  ): void {

    const email =
      this.editingEmail
        .trim()
        .toLowerCase();

    if (!email) {

      this.errorMessage =
        'Email не может быть пустым.';

      return;
    }

    if (
      !/^[^\s@]+@[^\s@]+\.[^\s@]+$/
        .test(email)
    ) {

      this.errorMessage =
        'Некорректный формат email.';

      return;
    }

    if (
      email
      === user.email
    ) {

      this.cancelEmailEdit();

      return;
    }

    this.errorMessage = '';
    this.successMessage = '';

    this.userAdminService
      .updateEmail(
        user.userId,
        email
      )
      .subscribe({

        next: updated => {

          this.replaceUser(
            updated
          );

          this.cancelEmailEdit();

          this.successMessage =
            `Email пользователя ${updated.login} изменен.`;

          this.changeDetectorRef
            .markForCheck();
        },

        error: (
          error: HttpErrorResponse
        ) => {

          this.errorMessage =
            this.resolveError(
              error,
              'Не удалось изменить email пользователя.'
            );

          this.changeDetectorRef
            .markForCheck();
        }
      });
  }

  changeRole(
    user: AdminUser,
    roleCode: string
  ): void {

    if (
      !roleCode
      ||
      user.roleCode === roleCode
    ) {
      return;
    }

    this.successMessage = '';
    this.errorMessage = '';

    this.userAdminService
      .updateRole(
        user.userId,
        roleCode
      )
      .subscribe({

        next: updated => {

          this.replaceUser(
            updated
          );

          this.successMessage =
            `Роль пользователя ${updated.login} изменена.`;

          this.changeDetectorRef
            .markForCheck();
        },

        error: (
          error: HttpErrorResponse
        ) => {

          this.errorMessage =
            this.resolveError(
              error,
              'Не удалось изменить роль пользователя.'
            );

          this.loadData();
        }
      });
  }


  toggleEnabled(
    user: AdminUser
  ): void {

    if (
      user.userId
      === this.currentUserId
    ) {

      this.errorMessage =
        'Нельзя заблокировать собственную учетную запись.';

      return;
    }

    const nextEnabled =
      !user.enabled;

    const action =
      nextEnabled
        ? 'разблокировать'
        : 'заблокировать';

    const confirmed =
      window.confirm(
        `Вы действительно хотите ${action} пользователя «${user.login}»?`
      );

    if (!confirmed) {
      return;
    }

    this.successMessage = '';
    this.errorMessage = '';

    this.userAdminService
      .updateEnabled(
        user.userId,
        nextEnabled
      )
      .subscribe({

        next: updated => {

          this.replaceUser(
            updated
          );

          this.successMessage =
            updated.enabled
              ? `Пользователь ${updated.login} разблокирован.`
              : `Пользователь ${updated.login} заблокирован.`;

          this.changeDetectorRef
            .markForCheck();
        },

        error: (
          error: HttpErrorResponse
        ) => {

          this.errorMessage =
            this.resolveError(
              error,
              'Не удалось изменить состояние пользователя.'
            );

          this.changeDetectorRef
            .markForCheck();
        }
      });
  }


  getRoleLabel(
    roleCode: string
  ): string {

    switch (roleCode) {

      case 'ADMIN':
        return 'Администратор системы';

      case 'DOCUMENT_OPERATOR':
        return 'Делопроизводитель';

      case 'LAWYER':
        return 'Юрист';

      case 'CONTRACT_MANAGER':
        return 'Менеджер по договорам';

      default:
        return roleCode;
    }
  }


  formatDate(
    value: string | null
  ): string {

    if (!value) {
      return 'Никогда';
    }

    const date =
      new Date(value);

    if (
      Number.isNaN(
        date.getTime()
      )
    ) {
      return '—';
    }

    return new Intl.DateTimeFormat(
      'ru-RU',
      {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      }
    ).format(date);
  }


  isCurrentUser(
    user: AdminUser
  ): boolean {

    return (
      user.userId
      === this.currentUserId
    );
  }


  private replaceUser(
    updated: AdminUser
  ): void {

    this.users =
      this.users.map(
        user =>
          user.userId
          === updated.userId
            ? updated
            : user
      );
  }


  private resolveError(
    error: HttpErrorResponse,
    fallback: string
  ): string {

    const message =
      error.error?.message;

    if (
      typeof message === 'string'
      &&
      message.trim()
    ) {

      return message;
    }

    return fallback;
  }
}