import { inject as injectPreferences } from '@angular/core';
import { UiPreferencesService } from '../../core/settings/ui-preferences.service';
import {
  ChangeDetectorRef,
  Component,
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
  catchError,
  concatMap,
  finalize,
  from,
  map,
  of,
  switchMap,
  toArray
} from 'rxjs';

import {
  WorkspaceDocument,
  WorkspaceService
} from '../../core/workspace/workspace.service';

import {
  AuthService
} from '../../core/auth/auth.service';

interface DeleteResult {
  documentId: number;
  success: boolean;
  message: string | null;
}

interface UploadFlowResult {
  documentId: number;
  duplicate: boolean;
  analysisSucceeded: boolean;
}

@Component({
  selector: 'app-documents',
  standalone: true,
  imports: [
    FormsModule
  ],
  templateUrl: './documents.html',
  styleUrl: './documents.scss'
})
export class Documents implements OnInit {
  readonly uiPreferences = injectPreferences(UiPreferencesService);

  documents: WorkspaceDocument[] = [];

  selectedDocumentIds =
    new Set<number>();

  query = '';
  selectedType = '';
  selectedStatus = '';
  selectedExpirationStatus = '';

  loading = true;
  uploading = false;
  deleting = false;

  errorMessage = '';
  uploadMessage = '';
  uploadErrorMessage = '';
  deleteMessage = '';
  deleteErrorMessage = '';

  constructor(
    private readonly workspaceService: WorkspaceService,
    private readonly authService: AuthService,
    private readonly router: Router,
    private readonly changeDetectorRef: ChangeDetectorRef
  ) {
  }

  ngOnInit(): void {
    this.loadDocuments();
  }

  private get currentRole(): string | null {

    return this.authService
      .getCurrentUser()
      ?.role
      ?? null;
  }


  get canManageDocuments(): boolean {

    return (
      this.currentRole === 'ADMIN'
      ||
      this.currentRole === 'DOCUMENT_OPERATOR'
    );
  }


  get documentsListTitle(): string {

    return this.canManageDocuments
      ? 'Все документы'
      : 'Мои документы';
  }

  loadDocuments(): void {

    this.loading = true;
    this.errorMessage = '';

    this.workspaceService
      .getDocuments(
        this.query,
        this.selectedType,
        this.selectedStatus,
        this.selectedExpirationStatus
      )
      .subscribe({

        next: documents => {

          this.documents =
            documents;

          this.selectedDocumentIds
            .clear();

          this.loading = false;

          this.changeDetectorRef
            .markForCheck();
        },

        error: (
          error: HttpErrorResponse
        ) => {

          this.loading = false;

          if (error.status === 0) {

            this.errorMessage =
              'Не удалось подключиться к серверу DocFlow.';

          } else {

            this.errorMessage =
              'Не удалось загрузить документы.';
          }

          this.changeDetectorRef
            .markForCheck();
        }
      });
  }

  resetFilters(): void {

    this.query = '';
    this.selectedType = '';
    this.selectedStatus = '';
    this.selectedExpirationStatus = '';

    this.loadDocuments();
  }

  openDocument(
    documentId: number
  ): void {

    if (this.deleting) {
      return;
    }

    void this.router.navigate(
      [
        '/documents',
        documentId
      ]
    );
  }

  isDocumentSelected(
    documentId: number
  ): boolean {

    return this.selectedDocumentIds
      .has(documentId);
  }

  isDocumentDeletable(
    document: WorkspaceDocument
  ): boolean {

    return (
      this.canManageDocuments
      &&
      document.status
        !== 'SENT_TO_SED'
    );
  }

  toggleDocumentSelection(
    document: WorkspaceDocument,
    event: Event
  ): void {

    const checkbox =
      event.target as HTMLInputElement;

    if (
      !this.isDocumentDeletable(
        document
      )
    ) {

      checkbox.checked = false;
      return;
    }

    if (checkbox.checked) {

      this.selectedDocumentIds
        .add(
          document.documentId
        );

    } else {

      this.selectedDocumentIds
        .delete(
          document.documentId
        );
    }

    this.deleteMessage = '';
    this.deleteErrorMessage = '';

    this.changeDetectorRef
      .markForCheck();
  }

  toggleSelectAll(
    event: Event
  ): void {

    const checkbox =
      event.target as HTMLInputElement;

    this.selectedDocumentIds
      .clear();

    if (!this.canManageDocuments) {

      checkbox.checked = false;

      return;
    }

    if (checkbox.checked) {

      for (
        const document of this.documents
      ) {

        if (
          this.isDocumentDeletable(
            document
          )
        ) {

          this.selectedDocumentIds
            .add(
              document.documentId
            );
        }
      }
    }

    this.changeDetectorRef
      .markForCheck();
  }

  get allDeletableSelected(): boolean {

    const deletable =
      this.documents.filter(
        document =>
          this.isDocumentDeletable(
            document
          )
      );

    return (
      deletable.length > 0
      &&
      deletable.every(
        document =>
          this.selectedDocumentIds
            .has(
              document.documentId
            )
      )
    );
  }

  get selectedCount(): number {

    return this.selectedDocumentIds.size;
  }

  deleteSelectedDocuments(): void {

    if (!this.canManageDocuments) {
      return;
    }

    const ids =
      Array.from(
        this.selectedDocumentIds
      );

    if (
      ids.length === 0
    ) {
      return;
    }

    const confirmed =
      window.confirm(
        ids.length === 1
          ? 'Удалить выбранный документ? Это действие нельзя отменить.'
          : `Удалить выбранные документы (${ids.length})? Это действие нельзя отменить.`
      );

    if (!confirmed) {
      return;
    }

    this.deleting = true;

    this.deleteMessage = '';
    this.deleteErrorMessage = '';

    from(ids)
      .pipe(

        concatMap(
          documentId =>
            this.workspaceService
              .deleteDocument(
                documentId
              )
              .pipe(

                map(
                  () => ({
                    documentId,
                    success: true,
                    message: null
                  }) as DeleteResult
                ),

                catchError(
                  (
                    error: HttpErrorResponse
                  ) => of({
                    documentId,
                    success: false,
                    message:
                      this.extractDeleteErrorMessage(
                        error
                      )
                  } as DeleteResult)
                )
              )
        ),

        toArray(),

        finalize(
          () => {

            this.deleting = false;

            this.changeDetectorRef
              .markForCheck();
          }
        )
      )
      .subscribe(
        results => {

          const deleted =
            results.filter(
              result =>
                result.success
            );

          const failed =
            results.filter(
              result =>
                !result.success
            );

          if (
            deleted.length > 0
          ) {

            this.deleteMessage =
              deleted.length === 1
                ? 'Документ удалён.'
                : `Удалено документов: ${deleted.length}.`;
          }

          if (
            failed.length > 0
          ) {

            this.deleteErrorMessage =
              failed
                .map(
                  result =>
                    `ID ${result.documentId}: ${result.message ?? 'не удалось удалить'}`
                )
                .join(' ');
          }

          this.loadDocuments();
        }
      );
  }

  onFileSelected(
    event: Event
  ): void {

    const input =
      event.target as HTMLInputElement;

    if (!this.canManageDocuments) {

      input.value = '';

      return;
    }

    const file =
      input.files?.[0];

    input.value = '';

    if (!file) {
      return;
    }

    this.uploadMessage = '';
    this.uploadErrorMessage = '';

    const extension =
      file.name
        .split('.')
        .pop()
        ?.toLowerCase();

    const allowedExtensions =
      [
        'pdf',
        'jpg',
        'jpeg',
        'png'
      ];

    if (
      !extension
      ||
      !allowedExtensions
        .includes(
          extension
        )
    ) {

      this.uploadErrorMessage =
        'Можно загружать только PDF, JPG, JPEG и PNG.';

      this.changeDetectorRef
        .markForCheck();

      return;
    }

    const maxFileSize =
      20 * 1024 * 1024;

    if (
      file.size
      > maxFileSize
    ) {

      this.uploadErrorMessage =
        'Размер файла не должен превышать 20 МБ.';

      this.changeDetectorRef
        .markForCheck();

      return;
    }

    this.uploadAndAnalyze(
      file
    );
  }

  private uploadAndAnalyze(
    file: File
  ): void {

    if (!this.canManageDocuments) {
      return;
    }

    this.uploading = true;

    this.uploadMessage =
      `Проверяем файл «${file.name}»...`;

    this.uploadErrorMessage = '';

    this.changeDetectorRef
      .markForCheck();

    this.workspaceService
      .uploadDocument(file)
      .pipe(

        switchMap(
          uploadResponse => {

            if (
              uploadResponse
                .duplicateDetected
            ) {

              return of({
                documentId:
                  uploadResponse
                    .documentId,

                duplicate:
                  true,

                analysisSucceeded:
                  true
              } as UploadFlowResult);
            }

            this.uploadMessage =
              'Файл загружен. Запускаем AI-анализ...';

            this.changeDetectorRef
              .markForCheck();

            return this.workspaceService
              .analyzeDocument(
                uploadResponse
                  .documentId
              )
              .pipe(

                map(
                  () => ({
                    documentId:
                      uploadResponse
                        .documentId,

                    duplicate:
                      false,

                    analysisSucceeded:
                      true
                  }) as UploadFlowResult
                ),

                catchError(
                  () => of({
                    documentId:
                      uploadResponse
                        .documentId,

                    duplicate:
                      false,

                    analysisSucceeded:
                      false
                  } as UploadFlowResult)
                )
              );
          }
        ),

        finalize(
          () => {

            this.uploading = false;

            this.changeDetectorRef
              .markForCheck();
          }
        )
      )
      .subscribe({

        next: result => {

          if (result.duplicate) {

            this.uploadMessage = '';

            window.alert(
              'Такой файл уже существует в DocFlow.\n\n'
              + `Документ ID ${result.documentId} будет открыт. `
              + 'Новая копия не создана.'
            );

            void this.router.navigate(
              [
                '/documents',
                result.documentId
              ]
            );

            return;
          }

          if (
            result.analysisSucceeded
          ) {

            this.uploadMessage =
              'Документ успешно загружен и проанализирован.';

            if (!this.uiPreferences.preferences().openAfterUpload) { this.loadDocuments(); return; }

            this.changeDetectorRef
              .markForCheck();

            void this.router.navigate(
              [
                '/documents',
                result.documentId
              ]
            );

            return;
          }

          this.uploadMessage =
            'Документ загружен, но AI-анализ завершился с ошибкой.';

          this.loadDocuments();

          this.changeDetectorRef
            .markForCheck();
        },

        error: (
          error: HttpErrorResponse
        ) => {

          this.uploadMessage = '';

          this.uploadErrorMessage =
            this.getUploadErrorMessage(
              error
            );

          this.changeDetectorRef
            .markForCheck();
        }
      });
  }

  private getUploadErrorMessage(
    error: HttpErrorResponse
  ): string {

    if (
      error.status === 0
    ) {

      return 'Не удалось подключиться к серверу DocFlow.';
    }

    if (
      error.status === 413
    ) {

      return 'Файл слишком большой.';
    }

    if (
      error.status === 400
    ) {

      return this.extractBackendMessage(
        error
      ) ?? 'Сервер отклонил загружаемый файл.';
    }

    return 'Не удалось загрузить документ.';
  }

  private extractDeleteErrorMessage(
    error: HttpErrorResponse
  ): string {

    if (
      error.status === 0
    ) {

      return 'сервер DocFlow недоступен';
    }

    return this.extractBackendMessage(
      error
    ) ?? (
      error.status === 409
        ? 'документ нельзя удалить из-за связанных данных'
        : 'не удалось удалить документ'
    );
  }

  private extractBackendMessage(
    error: HttpErrorResponse
  ): string | null {

    if (
      typeof error.error
        === 'string'
      &&
      error.error.trim()
    ) {

      return error.error;
    }

    if (
      error.error
      &&
      typeof error.error
        === 'object'
    ) {

      const errorObject =
        error.error as {
          message?: unknown;
          detail?: unknown;
          error?: unknown;
        };

      if (
        typeof errorObject.message
          === 'string'
      ) {

        return errorObject.message;
      }

      if (
        typeof errorObject.detail
          === 'string'
      ) {

        return errorObject.detail;
      }

      if (
        typeof errorObject.error
          === 'string'
      ) {

        return errorObject.error;
      }
    }

    return null;
  }

  getDocumentTypeLabel(
    type: string | null
  ): string {

    switch (type) {

      case 'CONTRACT':
        return 'Договор';

      case 'APPENDIX':
        return 'Приложение';

      case 'AMENDMENT_CHANGE':
        return 'Доп. соглашение — изменение';

      case 'AMENDMENT_PROLONGATION':
        return 'Доп. соглашение — продление';

      default:
        return 'Не определён';
    }
  }

  getExpirationLabel(
    document: WorkspaceDocument
  ): string {

    if (
      document.documentType !== 'CONTRACT'
    ) {
      return '';
    }

    const days =
      document.daysUntilExpiration;

    switch (
      document.expirationStatus
    ) {

      case 'ACTIVE':

        if (
          days === null
          ||
          days === undefined
        ) {
          return 'Действует';
        }

        return `Действует · ${days} ${this.getDaysWord(days)}`;

      case 'EXPIRING_SOON':

        if (days === 0) {
          return 'Истекает сегодня';
        }

        if (
          days === null
          ||
          days === undefined
        ) {
          return 'Скоро истекает';
        }

        return `Истекает · ${days} ${this.getDaysWord(days)}`;

      case 'EXPIRED':

        if (
          days === null
          ||
          days === undefined
        ) {
          return 'Срок истёк';
        }

        const daysAgo =
          Math.abs(days);

        return `Истёк · ${daysAgo} ${this.getDaysWord(daysAgo)} назад`;

      case 'NOT_SPECIFIED':
        return 'Срок не указан';

      default:
        return '';
    }
  }


  getExpirationClass(
    status: string | null
  ): string {

    switch (status) {

      case 'ACTIVE':
        return 'active';

      case 'EXPIRING_SOON':
        return 'expiring';

      case 'EXPIRED':
        return 'expired';

      default:
        return 'unspecified';
    }
  }


  private getDaysWord(
    value: number
  ): string {

    const absolute =
      Math.abs(value);

    const lastTwo =
      absolute % 100;

    const last =
      absolute % 10;

    if (
      lastTwo >= 11
      &&
      lastTwo <= 14
    ) {
      return 'дней';
    }

    if (last === 1) {
      return 'день';
    }

    if (
      last >= 2
      &&
      last <= 4
    ) {
      return 'дня';
    }

    return 'дней';
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

      case 'APPROVED':
        return 'Согласован';

      case 'RETURNED_FOR_CORRECTION':
        return 'На доработке';

      case 'REJECTED':
        return 'Отклонён';

      case 'READY_FOR_SED':
        return 'Готов к отправке';

      case 'SENT_TO_SED':
        return 'Отправлен в СЭД';

      case 'PROCESSING_FAILED':
        return 'Ошибка обработки';

      case 'SED_ERROR':
        return 'Ошибка СЭД';

      case 'ARCHIVED':
        return 'В архиве';

      default:
        return status || 'Неизвестно';
    }
  }

  getStatusClass(
    status: string | null
  ): string {

    switch (status) {

      case 'SENT_TO_SED':
      case 'VERIFIED':
      case 'APPROVED':
      case 'READY_FOR_SED':
        return 'green';

      case 'LINK_REQUIRED':
      case 'RETURNED_FOR_CORRECTION':
        return 'orange';

      case 'REVIEW_REQUIRED':
      case 'REJECTED':
      case 'PROCESSING_FAILED':
      case 'SED_ERROR':
        return 'red';

      case 'LINKED':
      case 'UPLOADED':
      case 'AI_PROCESSING':
        return 'violet';

      case 'ARCHIVED':
        return 'neutral';

      default:
        return 'neutral';
    }
  }

  getDocumentName(
    document: WorkspaceDocument
  ): string {

    return document.originalName
      ?? `Документ #${document.documentId}`;
  }

  getDate(
    document: WorkspaceDocument
  ): string {

    const value =
      document.documentDate
      ?? document.createdAt;

    if (!value) {
      return '—';
    }

    const datePart =
      value.substring(
        0,
        10
      );

    const match =
      /^(\d{4})-(\d{2})-(\d{2})$/
        .exec(
          datePart
        );

    if (!match) {
      return value;
    }

    return `${match[3]}.${match[2]}.${match[1]}`;
  }

  getAmount(
    document: WorkspaceDocument
  ): string {

    if (
      document.amount === null
    ) {
      return '—';
    }

    const amount =
      new Intl.NumberFormat(
        'ru-RU',
        {
          maximumFractionDigits: 2
        }
      ).format(
        document.amount
      );

    return document.currency
      ? `${amount} ${document.currency}`
      : amount;
  }
}
