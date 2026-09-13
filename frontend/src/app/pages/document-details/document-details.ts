import {
  ChangeDetectorRef,
  Component,
  OnInit
} from '@angular/core';

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
  DocumentSummary,
  WorkspaceService
} from '../../core/workspace/workspace.service';

@Component({
  selector: 'app-document-details',
  standalone: true,
  templateUrl: './document-details.html',
  styleUrl: './document-details.scss'
})
export class DocumentDetails implements OnInit {

  document: DocumentSummary | null = null;

  loading = true;
  deleting = false;

  errorMessage = '';
  deleteErrorMessage = '';

  private documentId: number | null = null;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly workspaceService: WorkspaceService,
    private readonly changeDetectorRef: ChangeDetectorRef
  ) {
  }

  ngOnInit(): void {

    const rawId =
      this.route.snapshot.paramMap.get('id');

    const parsedId =
      Number(rawId);

    if (
      !rawId ||
      !Number.isInteger(parsedId) ||
      parsedId <= 0
    ) {

      this.loading = false;

      this.errorMessage =
        'Некорректный идентификатор документа.';

      return;
    }

    this.documentId =
      parsedId;

    this.loadDocument();
  }

  loadDocument(): void {

    if (this.documentId === null) {
      return;
    }

    this.loading = true;
    this.errorMessage = '';
    this.deleteErrorMessage = '';

    this.workspaceService
      .getDocumentSummary(
        this.documentId
      )
      .subscribe({

        next: document => {

          this.document =
            document;

          this.loading = false;

          this.changeDetectorRef
            .markForCheck();
        },

        error: (
          error: HttpErrorResponse
        ) => {

          this.loading = false;

          if (error.status === 404) {

            this.errorMessage =
              'Документ не найден.';

          } else if (error.status === 0) {

            this.errorMessage =
              'Не удалось подключиться к серверу DocFlow.';

          } else {

            this.errorMessage =
              'Не удалось загрузить данные документа.';
          }

          this.changeDetectorRef
            .markForCheck();
        }
      });
  }

  backToDocuments(): void {

    void this.router.navigate(
      ['/documents']
    );
  }

  deleteDocument(): void {

    if (
      !this.document ||
      !this.canDeleteDocument ||
      this.deleting
    ) {
      return;
    }

    const confirmed =
      window.confirm(
        `Удалить документ «${this.document.originalName ?? 'Документ'}»?\n\n`
        + 'Документ, результаты AI-анализа, маршрут и связанный файл будут удалены. '
        + 'Это действие нельзя отменить.'
      );

    if (!confirmed) {
      return;
    }

    this.deleting = true;
    this.deleteErrorMessage = '';

    this.workspaceService
      .deleteDocument(
        this.document.documentId
      )
      .pipe(
        finalize(
          () => {

            this.deleting = false;

            this.changeDetectorRef
              .markForCheck();
          }
        )
      )
      .subscribe({

        next: () => {

          void this.router.navigate(
            ['/documents']
          );
        },

        error: (
          error: HttpErrorResponse
        ) => {

          if (error.status === 0) {

            this.deleteErrorMessage =
              'Не удалось подключиться к серверу DocFlow.';

          } else {

            this.deleteErrorMessage =
              this.extractBackendMessage(
                error
              )
              ?? (
                error.status === 409
                  ? 'Этот документ нельзя удалить из-за связанных данных.'
                  : 'Не удалось удалить документ.'
              );
          }

          this.changeDetectorRef
            .markForCheck();
        }
      });
  }

  get canDeleteDocument(): boolean {

    if (!this.document) {
      return false;
    }

    if (
      this.document.status === 'SENT_TO_SED'
    ) {
      return false;
    }

    if (this.document.sed !== null) {
      return false;
    }

    if (
      this.document.linkedDocumentsCount > 0
    ) {
      return false;
    }

    return true;
  }

  get deleteRestrictionMessage(): string {

    if (!this.document) {
      return '';
    }

    if (
      this.document.status === 'SENT_TO_SED'
    ) {

      return 'Документ уже отправлен в СЭД и хранится в истории.';
    }

    if (this.document.sed !== null) {

      return 'Документ имеет историю отправки в СЭД и не может быть удалён.';
    }

    if (
      this.document.linkedDocumentsCount > 0
    ) {

      return 'К этому договору привязаны другие документы. Сначала необходимо удалить или перепривязать их.';
    }

    return '';
  }

  openLinkedContract(): void {

    const contractId =
      this.document?.link
        ?.contractDocumentId;

    if (!contractId) {
      return;
    }

    void this.router.navigate(
      [
        '/documents',
        contractId
      ]
    );
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

      case 'SENT_TO_SED':
      case 'VERIFIED':
      case 'READY_FOR_SED':
        return 'green';

      case 'LINK_REQUIRED':
        return 'orange';

      case 'REVIEW_REQUIRED':
      case 'PROCESSING_FAILED':
      case 'SED_ERROR':
        return 'red';

      case 'LINKED':
      case 'UPLOADED':
      case 'AI_PROCESSING':
        return 'violet';

      default:
        return 'neutral';
    }
  }

  getRoutingTargetLabel(
    target: string | null
  ): string {

    switch (target) {

      case 'CONTRACT_MANAGER':
        return 'Менеджер по договорам';

      case 'LAWYER':
        return 'Юрист';

      default:
        return target || 'Не определён';
    }
  }

  getAiStatusLabel(
    status: string | null
  ): string {

    switch (status) {

      case 'SUCCESS':
      case 'COMPLETED':
        return 'Обработан';

      case 'REVIEW_REQUIRED':
        return 'Требуется проверка';

      case 'OCR_REQUIRED':
        return 'Требуется OCR';

      case 'FAILED':
        return 'Ошибка';

      case 'PROCESSING':
        return 'Обрабатывается';

      default:
        return status || 'Нет данных';
    }
  }

  getRoutingStatusLabel(
    status: string | null
  ): string {

    switch (status) {

      case 'ROUTED':
        return 'Маршрут определён';

      case 'READY':
        return 'Готов к маршрутизации';

      case 'SKIPPED':
        return 'Маршрутизация не выполнена';

      case 'FAILED':
        return 'Ошибка маршрутизации';

      default:
        return status || 'Нет данных';
    }
  }

  getSedStatusLabel(
    status: string | null
  ): string {

    switch (status) {

      case 'SUCCESS':
        return 'Успешно отправлен';

      case 'SENT':
        return 'Отправлен';

      case 'FAILED':
      case 'ERROR':
        return 'Ошибка отправки';

      default:
        return status || 'Нет данных';
    }
  }

  getConfidence(): string {

    const confidence =
      this.document?.ai
        ?.confidence;

    if (
      confidence === null ||
      confidence === undefined
    ) {
      return '—';
    }

    const percent =
      confidence <= 1
        ? confidence * 100
        : confidence;

    return `${percent.toFixed(2)}%`;
  }

  getConfidencePercent(): number {

    const confidence =
      this.document?.ai
        ?.confidence;

    if (
      confidence === null ||
      confidence === undefined
    ) {
      return 0;
    }

    const percent =
      confidence <= 1
        ? confidence * 100
        : confidence;

    return Math.min(
      100,
      Math.max(
        0,
        percent
      )
    );
  }

  formatDate(
    value: string | null
  ): string {

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
        .exec(datePart);

    if (!match) {
      return value;
    }

    return `${match[3]}.${match[2]}.${match[1]}`;
  }

  formatDateTime(
    value: string | null
  ): string {

    if (!value) {
      return '—';
    }

    const date =
      this.formatDate(value);

    const timeMatch =
      /T(\d{2}):(\d{2})/
        .exec(value);

    if (!timeMatch) {
      return date;
    }

    return `${date}, ${timeMatch[1]}:${timeMatch[2]}`;
  }

  getAmount(): string {

    const amount =
      this.document?.amount;

    if (
      amount === null ||
      amount === undefined
    ) {
      return '—';
    }

    const formatted =
      new Intl.NumberFormat(
        'ru-RU',
        {
          minimumFractionDigits: 0,
          maximumFractionDigits: 2
        }
      ).format(amount);

    return this.document?.currency
      ? `${formatted} ${this.document.currency}`
      : formatted;
  }

  private extractBackendMessage(
    error: HttpErrorResponse
  ): string | null {

    if (
      typeof error.error === 'string'
      &&
      error.error.trim()
    ) {

      return error.error;
    }

    if (
      error.error
      &&
      typeof error.error === 'object'
    ) {

      const response =
        error.error as {
          message?: unknown;
          detail?: unknown;
          error?: unknown;
        };

      if (
        typeof response.message === 'string'
      ) {
        return response.message;
      }

      if (
        typeof response.detail === 'string'
      ) {
        return response.detail;
      }

      if (
        typeof response.error === 'string'
      ) {
        return response.error;
      }
    }

    return null;
  }
}