import {
  ChangeDetectorRef,
  Component,
  ElementRef,
  HostListener,
  OnDestroy,
  OnInit,
  ViewChild
} from '@angular/core';

import {
  FormsModule
} from '@angular/forms';

import {
  Router,
  RouterLink,
  RouterLinkActive,
  RouterOutlet
} from '@angular/router';

import {
  Subject,
  catchError,
  debounceTime,
  distinctUntilChanged,
  finalize,
  forkJoin,
  of,
  switchMap,
  takeUntil
} from 'rxjs';

import {
  AuthService,
  CurrentUserResponse
} from '../core/auth/auth.service';

import {
  UiPreferencesService
} from '../core/settings/ui-preferences.service';

import {
  WorkspaceDocument,
  WorkspaceService
} from '../core/workspace/workspace.service';

import {
  NotificationService,
  UserNotification
} from '../core/notifications/notification.service';

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [
    FormsModule,
    RouterOutlet,
    RouterLink,
    RouterLinkActive
  ],
  templateUrl: './app-shell.html',
  styleUrl: './app-shell.scss'
})
export class AppShell
  implements OnInit, OnDestroy {

  @ViewChild(
    'globalSearchInput'
  )
  globalSearchInput:
    ElementRef<HTMLInputElement> | undefined;

  currentUser:
    CurrentUserResponse | null = null;

  searchQuery = '';

  searchResults:
    WorkspaceDocument[] = [];

  searchLoading = false;
  searchOpen = false;
  searchErrorMessage = '';

  notifications:
    UserNotification[] = [];

  notificationsOpen = false;
  notificationsLoading = false;
  notificationsErrorMessage = '';

  private notificationUnreadCount = 0;

  private readonly searchSubject =
    new Subject<string>();

  private readonly destroySubject =
    new Subject<void>();

  constructor(
    private readonly authService:
      AuthService,

    private readonly workspaceService:
      WorkspaceService,

    private readonly notificationService:
      NotificationService,

    private readonly router:
      Router,

    private readonly changeDetectorRef:
      ChangeDetectorRef,

    readonly uiPreferencesService:
      UiPreferencesService
  ) {
  }

  ngOnInit(): void {

    this.loadCurrentUser();
    this.initializeSearch();
    this.loadNotifications();
  }

  ngOnDestroy(): void {

    this.destroySubject.next();
    this.destroySubject.complete();

    this.searchSubject.complete();
  }

  isAdmin(): boolean {

    return (
      this.currentUser?.role
      === 'ADMIN'
    );
  }

  isOperator(): boolean {

    return (
      this.currentUser?.role
      === 'DOCUMENT_OPERATOR'
    );
  }

  isSpecialist(): boolean {

    return (
      this.currentUser?.role
      === 'LAWYER'
      ||
      this.currentUser?.role
      === 'CONTRACT_MANAGER'
    );
  }

  canUseAiWorkspace(): boolean {

    return (
      this.isAdmin()
      ||
      this.isOperator()
    );
  }

  canUseReviewWorkspace(): boolean {

    return (
      this.isAdmin()
      ||
      this.isOperator()
    );
  }

  canUseRoutingWorkspace(): boolean {

    return (
      this.isAdmin()
      ||
      this.isSpecialist()
    );
  }

  canUseSedWorkspace(): boolean {

    return (
      this.isAdmin()
      ||
      this.isOperator()
    );
  }

  getRoutingMenuLabel(): string {

    if (this.isSpecialist()) {
      return 'Мои задачи';
    }

    return 'Маршрутизация';
  }

  onSearchInput(): void {

    const query =
      this.searchQuery.trim();

    if (query.length < 2) {

      this.searchResults = [];
      this.searchLoading = false;
      this.searchErrorMessage = '';
      this.searchOpen = false;

      return;
    }

    this.notificationsOpen = false;

    this.searchOpen = true;
    this.searchLoading = true;
    this.searchErrorMessage = '';

    this.searchSubject.next(
      query
    );
  }

  onSearchFocus(): void {

    this.notificationsOpen = false;

    if (
      this.searchQuery.trim().length >= 2
    ) {
      this.searchOpen = true;
    }
  }

  clearSearch(): void {

    this.searchQuery = '';
    this.searchResults = [];
    this.searchOpen = false;
    this.searchLoading = false;
    this.searchErrorMessage = '';

    this.globalSearchInput
      ?.nativeElement
      .focus();
  }

  openSearchResult(
    document: WorkspaceDocument
  ): void {

    this.searchOpen = false;

    void this.router.navigate(
      [
        '/documents',
        document.documentId
      ]
    );
  }

  onSearchEnter(): void {

    if (
      this.searchOpen
      &&
      this.searchResults.length > 0
    ) {

      this.openSearchResult(
        this.searchResults[0]
      );
    }
  }

  toggleNotifications(): void {

    this.searchOpen = false;

    this.notificationsOpen =
      !this.notificationsOpen;

    if (this.notificationsOpen) {
      this.loadNotifications();
    }
  }

  loadNotifications(): void {

    if (this.notificationsLoading) {
      return;
    }

    this.notificationsLoading = true;
    this.notificationsErrorMessage = '';

    forkJoin({
      notifications:
        this.notificationService
          .getNotifications(),

      unread:
        this.notificationService
          .getUnreadCount()
    })
      .pipe(
        finalize(
          () => {

            this.notificationsLoading = false;

            this.changeDetectorRef
              .markForCheck();
          }
        )
      )
      .subscribe({

        next: result => {

          this.notifications =
            result.notifications;

          this.notificationUnreadCount =
            result.unread.unreadCount;

          this.changeDetectorRef
            .markForCheck();
        },

        error: () => {

          this.notificationsErrorMessage =
            'Не удалось загрузить уведомления.';

          this.changeDetectorRef
            .markForCheck();
        }
      });
  }

  openNotification(
    notification: UserNotification
  ): void {

    this.notificationsOpen = false;

    if (!notification.read) {

      this.notificationService
        .markAsRead(
          notification.id
        )
        .subscribe({

          next: updatedNotification => {

            this.notifications =
              this.notifications.map(
                item =>
                  item.id
                  === updatedNotification.id
                    ? updatedNotification
                    : item
              );

            this.notificationUnreadCount =
              Math.max(
                0,
                this.notificationUnreadCount - 1
              );

            this.changeDetectorRef
              .markForCheck();
          },

          error: () => {

            this.loadNotifications();
          }
        });
    }

    const route =
      notification.route?.trim();

    if (route) {

      void this.router.navigateByUrl(
        route
      );

      return;
    }

    if (notification.documentId) {

      void this.router.navigate(
        [
          '/documents',
          notification.documentId
        ]
      );
    }
  }

  markAllNotificationsAsRead(): void {

    if (
      this.notificationUnreadCount === 0
    ) {
      return;
    }

    this.notificationService
      .markAllAsRead()
      .subscribe({

        next: () => {

          this.notifications =
            this.notifications.map(
              notification => ({
                ...notification,
                read: true
              })
            );

          this.notificationUnreadCount = 0;

          this.changeDetectorRef
            .markForCheck();
        },

        error: () => {

          this.notificationsErrorMessage =
            'Не удалось отметить уведомления прочитанными.';

          this.changeDetectorRef
            .markForCheck();
        }
      });
  }

  get notificationCount(): number {

    return this.notificationUnreadCount;
  }

  get notificationBadge(): string {

    if (
      this.notificationCount > 99
    ) {
      return '99+';
    }

    return String(
      this.notificationCount
    );
  }

  getDocumentTypeLabel(
    type: string | null
  ): string {

    switch (type) {

      case 'CONTRACT':
        return 'Договор';

      case 'AMENDMENT_CHANGE':
        return 'Доп. соглашение — изменение';

      case 'AMENDMENT_PROLONGATION':
        return 'Доп. соглашение — продление';

      case 'APPENDIX':
        return 'Приложение';

      default:
        return 'Тип не определён';
    }
  }

  getStatusLabel(
    status: string | null
  ): string {

    switch (status) {

      case 'UPLOADED':
        return 'Загружен';

      case 'AI_PROCESSING':
        return 'AI-анализ';

      case 'REVIEW_REQUIRED':
        return 'Требует проверки';

      case 'LINK_REQUIRED':
        return 'Требует привязки';

      case 'LINKED':
        return 'Связан с договором';

      case 'VERIFIED':
        return 'Проверен';

      case 'READY_FOR_SED':
        return 'Готов к отправке';

      case 'SENT_TO_SED':
        return 'Отправлен в СЭД';

      case 'PROCESSING_FAILED':
        return 'Ошибка обработки';

      case 'SED_ERROR':
        return 'Ошибка СЭД';

      default:
        return status || 'Неизвестно';
    }
  }

  getStatusClass(
    status: string | null
  ): string {

    switch (status) {

      case 'VERIFIED':
      case 'READY_FOR_SED':
      case 'SENT_TO_SED':
        return 'success';

      case 'REVIEW_REQUIRED':
      case 'PROCESSING_FAILED':
      case 'SED_ERROR':
        return 'warning';

      case 'LINK_REQUIRED':
        return 'link';

      default:
        return 'neutral';
    }
  }

  getNotificationVisualType(
    type: string
  ): 'review' | 'link' | 'sed' | 'error' {

    switch (type) {

      case 'DOCUMENT_ASSIGNED':
      case 'LINK_REQUIRED':
        return 'link';

      case 'DOCUMENT_APPROVED':
      case 'READY_FOR_SED':
        return 'sed';

      case 'DOCUMENT_RETURNED':
      case 'AI_REVIEW_REQUIRED':
      case 'CONTRACT_EXPIRING':
        return 'review';

      case 'DOCUMENT_REJECTED':
      case 'CONTRACT_EXPIRED':
      case 'SED_ERROR':
      case 'PROCESSING_ERROR':
      default:
        return 'error';
    }
  }

  getNotificationIcon(
    type: string
  ): string {

    switch (type) {

      case 'DOCUMENT_ASSIGNED':
        return '→';

      case 'DOCUMENT_APPROVED':
        return '✓';

      case 'DOCUMENT_RETURNED':
        return '↩';

      case 'DOCUMENT_REJECTED':
        return '!';

      case 'AI_REVIEW_REQUIRED':
        return 'AI';

      case 'LINK_REQUIRED':
        return '⛓';

      case 'CONTRACT_EXPIRING':
        return '⌛';

      case 'CONTRACT_EXPIRED':
        return '!';

      case 'READY_FOR_SED':
        return 'S';

      case 'SED_ERROR':
      case 'PROCESSING_ERROR':
      default:
        return '!';
    }
  }

  formatNotificationDate(
    createdAt: string
  ): string {

    if (!createdAt) {
      return '';
    }

    const date =
      new Date(createdAt);

    if (
      Number.isNaN(
        date.getTime()
      )
    ) {
      return createdAt;
    }

    return new Intl.DateTimeFormat(
      'ru-RU',
      {
        day: '2-digit',
        month: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
      }
    ).format(date);
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
        return 'Пользователь системы';
    }
  }

  @HostListener(
    'document:keydown',
    ['$event']
  )
  onDocumentKeydown(
    event: KeyboardEvent
  ): void {

    if (
      event.ctrlKey
      &&
      event.key.toLowerCase() === 'k'
    ) {

      event.preventDefault();

      this.notificationsOpen = false;

      this.globalSearchInput
        ?.nativeElement
        .focus();

      if (
        this.searchQuery.trim().length >= 2
      ) {
        this.searchOpen = true;
      }

      return;
    }

    if (event.key === 'Escape') {

      this.searchOpen = false;
      this.notificationsOpen = false;

      this.globalSearchInput
        ?.nativeElement
        .blur();
    }
  }

  @HostListener(
    'document:click'
  )
  onDocumentClick(): void {

    this.searchOpen = false;
    this.notificationsOpen = false;
  }

  private initializeSearch(): void {

    this.searchSubject
      .pipe(
        debounceTime(300),
        distinctUntilChanged(),

        switchMap(
          query =>
            this.workspaceService
              .getDocuments(
                query
              )
              .pipe(

                catchError(
                  () => {

                    this.searchErrorMessage =
                      'Не удалось выполнить поиск.';

                    return of(
                      [] as WorkspaceDocument[]
                    );
                  }
                ),

                finalize(
                  () => {

                    this.searchLoading = false;

                    this.changeDetectorRef
                      .markForCheck();
                  }
                )
              )
        ),

        takeUntil(
          this.destroySubject
        )
      )
      .subscribe(
        documents => {

          this.searchResults =
            documents.slice(
              0,
              7
            );

          this.searchOpen =
            this.searchQuery
              .trim()
              .length >= 2;

          this.changeDetectorRef
            .markForCheck();
        }
      );
  }

  private loadCurrentUser(): void {

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

          this.loadNotifications();

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
}