import {
  HttpClient
} from '@angular/common/http';

import {
  Injectable
} from '@angular/core';

import {
  Observable
} from 'rxjs';

import {
  CurrentUserResponse
} from '../auth/auth.service';


export interface UserReminder {
  id: number;
  text: string;
  createdAt: string;
}


@Injectable({
  providedIn: 'root'
})
export class ProfileService {

  private readonly apiUrl =
    'http://localhost:8080/api/profile';


  constructor(
    private readonly http:
      HttpClient
  ) {
  }


  updateFullName(
    fullName: string
  ): Observable<CurrentUserResponse> {

    return this.http.patch<CurrentUserResponse>(
      `${this.apiUrl}/full-name`,
      {
        fullName
      }
    );
  }


  getReminders():
    Observable<UserReminder[]> {

    return this.http.get<UserReminder[]>(
      `${this.apiUrl}/reminders`
    );
  }


  createReminder(
    text: string
  ): Observable<UserReminder> {

    return this.http.post<UserReminder>(
      `${this.apiUrl}/reminders`,
      {
        text
      }
    );
  }


  deleteReminder(
    reminderId: number
  ): Observable<void> {

    return this.http.delete<void>(
      `${this.apiUrl}/reminders/${reminderId}`
    );
  }
}