import {
  HttpClient
} from '@angular/common/http';

import {
  Injectable
} from '@angular/core';

import {
  Observable
} from 'rxjs';

export type NotificationType =
  | 'ACCESS_REQUEST_CREATED'
  | 'DOCUMENT_ASSIGNED'
  | 'DOCUMENT_RETURNED'
  | 'DOCUMENT_APPROVED'
  | 'DOCUMENT_REJECTED'
  | 'APPROVAL_DUE_SOON'
  | 'APPROVAL_OVERDUE'
  | 'AI_REVIEW_REQUIRED'
  | 'LINK_REQUIRED'
  | 'CONTRACT_EXPIRING'
  | 'CONTRACT_EXPIRED'
  | 'READY_FOR_SED'
  | 'SED_ERROR'
  | 'PROCESSING_ERROR';

export interface UserNotification {
  id: number;
  documentId: number | null;
  type: NotificationType;
  title: string;
  message: string;
  route: string | null;
  actionLabel: string | null;
  read: boolean;
  createdAt: string;
}

export interface NotificationUnreadCount {
  unreadCount: number;
}

@Injectable({
  providedIn: 'root'
})
export class NotificationService {

  private readonly apiUrl =
    'http://localhost:8080/api/notifications';

  constructor(
    private readonly http: HttpClient
  ) {
  }

  getNotifications():
    Observable<UserNotification[]> {

    return this.http.get<UserNotification[]>(
      this.apiUrl
    );
  }

  getUnreadCount():
    Observable<NotificationUnreadCount> {

    return this.http.get<NotificationUnreadCount>(
      `${this.apiUrl}/unread-count`
    );
  }

  markAsRead(
    notificationId: number
  ): Observable<UserNotification> {

    return this.http.patch<UserNotification>(
      `${this.apiUrl}/${notificationId}/read`,
      null
    );
  }

  markAllAsRead():
    Observable<void> {

    return this.http.patch<void>(
      `${this.apiUrl}/read-all`,
      null
    );
  }
}