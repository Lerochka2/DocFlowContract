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
  WorkspaceDocument,
  WorkspaceService
} from '../../core/workspace/workspace.service';

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

  documents: WorkspaceDocument[] = [];

  query = '';
  selectedType = '';
  selectedStatus = '';

  loading = true;
  errorMessage = '';

  constructor(
    private readonly workspaceService: WorkspaceService,
    private readonly router: Router,
    private readonly changeDetectorRef: ChangeDetectorRef
  ) {
  }

  ngOnInit(): void {
    this.loadDocuments();
  }

  loadDocuments(): void {

    this.loading = true;
    this.errorMessage = '';

    this.workspaceService
      .getDocuments(
        this.query,
        this.selectedType,
        this.selectedStatus
      )
      .subscribe({

        next: documents => {

          this.documents =
            documents;

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

    this.loadDocuments();
  }

  openDocument(
    documentId: number
  ): void {

    void this.router.navigate(
      [
        '/documents',
        documentId
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
        return 'Неизвестно';
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
      value.substring(0, 10);

    const match =
      /^(\d{4})-(\d{2})-(\d{2})$/
        .exec(datePart);

    if (!match) {
      return value;
    }

    return `${match[3]}.${match[2]}.${match[1]}`;
  }

  getAmount(
    document: WorkspaceDocument
  ): string {

    if (document.amount === null) {
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