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

  private readonly searchSubject =
    new Subject<string>();

  private readonly destroySubject =
    new Subject<void>();

  constructor(
    private readonly authService:
      AuthService,

    private readonly workspaceService:
      WorkspaceService,

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
  }

  ngOnDestroy(): void {

    this.destroySubject.next();
    this.destroySubject.complete();

    this.searchSubject.complete();
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

    this.searchOpen = true;
    this.searchLoading = true;
    this.searchErrorMessage = '';

    this.searchSubject.next(
      query
    );
  }

  onSearchFocus(): void {

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

      case 'USER':
        return 'Пользователь';

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