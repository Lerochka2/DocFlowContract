import {
  HttpClient
} from '@angular/common/http';

import {
  Injectable
} from '@angular/core';

import {
  Observable
} from 'rxjs';


export interface AdminUser {
  userId: number;
  login: string;
  fullName: string;
  email: string | null;
  roleCode: string;
  roleName: string;
  enabled: boolean;
  createdAt: string | null;
  updatedAt: string | null;
  lastLoginAt: string | null;
}


export interface RoleOption {
  id: number;
  code: string;
  name: string;
}


export interface CreateUserRequest {
  login: string;
  fullName: string;
  email: string;
  password: string;
  roleCode: string;
}


@Injectable({
  providedIn: 'root'
})
export class UserAdminService {

  private readonly usersApiUrl =
    'http://localhost:8080/api/users';

  private readonly rolesApiUrl =
    'http://localhost:8080/api/roles';


  constructor(
    private readonly http: HttpClient
  ) {
  }


  getUsers(): Observable<AdminUser[]> {

    return this.http.get<AdminUser[]>(
      this.usersApiUrl
    );
  }


  getRoles(): Observable<RoleOption[]> {

    return this.http.get<RoleOption[]>(
      this.rolesApiUrl
    );
  }


  createUser(
    request: CreateUserRequest
  ): Observable<AdminUser> {

    return this.http.post<AdminUser>(
      this.usersApiUrl,
      request
    );
  }


  updateFullName(
    userId: number,
    fullName: string
  ): Observable<AdminUser> {

    return this.http.patch<AdminUser>(
      `${this.usersApiUrl}/${userId}/full-name`,
      {
        fullName
      }
    );
  }

  updateEmail(
    userId: number,
    email: string
  ): Observable<AdminUser> {

    return this.http.patch<AdminUser>(
      `${this.usersApiUrl}/${userId}/email`,
      {
        email
      }
    );
  }


  updateRole(
    userId: number,
    roleCode: string
  ): Observable<AdminUser> {

    return this.http.patch<AdminUser>(
      `${this.usersApiUrl}/${userId}/role`,
      {
        roleCode
      }
    );
  }


  updateEnabled(
    userId: number,
    enabled: boolean
  ): Observable<AdminUser> {

    return this.http.patch<AdminUser>(
      `${this.usersApiUrl}/${userId}/enabled`,
      {
        enabled
      }
    );
  }
}