import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, tap } from 'rxjs';

export interface LoginResponse {
  userId: number;
  login: string;
  fullName: string;
  role: string;
  accessToken: string;
  tokenType: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  private readonly apiUrl = 'http://localhost:8080/api/auth';

  private readonly tokenKey = 'docflow_access_token';
  private readonly userKey = 'docflow_user';

  constructor(
    private readonly http: HttpClient
  ) {
  }

  login(
    login: string,
    password: string,
    rememberMe: boolean
  ): Observable<LoginResponse> {

    return this.http
      .post<LoginResponse>(
        `${this.apiUrl}/login`,
        {
          login,
          password
        }
      )
      .pipe(
        tap(response => {
          this.saveSession(response, rememberMe);
        })
      );
  }

  getToken(): string | null {
    return (
      localStorage.getItem(this.tokenKey) ??
      sessionStorage.getItem(this.tokenKey)
    );
  }

  getCurrentUser(): LoginResponse | null {

    const rawUser =
      localStorage.getItem(this.userKey) ??
      sessionStorage.getItem(this.userKey);

    if (!rawUser) {
      return null;
    }

    try {
      return JSON.parse(rawUser) as LoginResponse;
    } catch {
      return null;
    }
  }

  isAuthenticated(): boolean {

    const token = this.getToken();

    if (!token) {
      return false;
    }

    return !this.isTokenExpired(token);
  }

  logout(): void {

    localStorage.removeItem(this.tokenKey);
    localStorage.removeItem(this.userKey);

    sessionStorage.removeItem(this.tokenKey);
    sessionStorage.removeItem(this.userKey);
  }

  private saveSession(
    response: LoginResponse,
    rememberMe: boolean
  ): void {

    const targetStorage = rememberMe
      ? localStorage
      : sessionStorage;

    const otherStorage = rememberMe
      ? sessionStorage
      : localStorage;

    otherStorage.removeItem(this.tokenKey);
    otherStorage.removeItem(this.userKey);

    targetStorage.setItem(
      this.tokenKey,
      response.accessToken
    );

    targetStorage.setItem(
      this.userKey,
      JSON.stringify(response)
    );
  }

  private isTokenExpired(token: string): boolean {

    try {

      const payloadPart = token.split('.')[1];

      if (!payloadPart) {
        return true;
      }

      const normalized = payloadPart
        .replace(/-/g, '+')
        .replace(/_/g, '/');

      const padded = normalized.padEnd(
        Math.ceil(normalized.length / 4) * 4,
        '='
      );

      const payload = JSON.parse(
        atob(padded)
      ) as {
        exp?: number;
      };

      if (!payload.exp) {
        return true;
      }

      return payload.exp * 1000 <= Date.now();

    } catch {
      return true;
    }
  }
}