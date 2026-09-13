import {
  Component
} from '@angular/core';

import {
  FormsModule
} from '@angular/forms';

import {
  HttpErrorResponse
} from '@angular/common/http';

import {
  ActivatedRoute,
  Router
} from '@angular/router';

import {
  finalize
} from 'rxjs';

import {
  AuthService
} from '../../core/auth/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    FormsModule
  ],
  templateUrl: './login.html',
  styleUrl: './login.scss'
})
export class Login {

  loginValue = '';
  password = '';

  rememberMe = false;

  showPassword = false;
  loading = false;

  errorMessage = '';

  constructor(
    private readonly authService: AuthService,
    private readonly router: Router,
    private readonly route: ActivatedRoute
  ) {
  }

  submit(): void {

    this.errorMessage = '';

    const login = this.loginValue.trim();

    if (!login || !this.password) {

      this.errorMessage =
        'Введите логин и пароль.';

      return;
    }

    this.loading = true;

    this.authService
      .login(
        login,
        this.password,
        this.rememberMe
      )
      .pipe(
        finalize(() => {
          this.loading = false;
        })
      )
      .subscribe({
        next: () => {

          const returnUrl =
            this.route.snapshot.queryParamMap.get(
              'returnUrl'
            ) ?? '/dashboard';

          void this.router.navigateByUrl(
            returnUrl
          );
        },

        error: (
          error: HttpErrorResponse
        ) => {

          if (
            error.status === 401 ||
            error.status === 400
          ) {

            this.errorMessage =
              'Неверный логин или пароль.';

            return;
          }

          if (error.status === 0) {

            this.errorMessage =
              'Не удалось подключиться к серверу DocFlow.';

            return;
          }

          this.errorMessage =
            'Не удалось выполнить вход. Попробуйте ещё раз.';
        }
      });
  }

  togglePassword(): void {
    this.showPassword = !this.showPassword;
  }
}