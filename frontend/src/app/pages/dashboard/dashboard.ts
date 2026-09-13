import {
  ChangeDetectorRef,
  Component,
  OnInit
} from '@angular/core';

import {
  HttpErrorResponse
} from '@angular/common/http';

import {
  Router
} from '@angular/router';

import {
  finalize,
  forkJoin
} from 'rxjs';

import {
  AuthService
} from '../../core/auth/auth.service';

import {
  WorkspaceDashboard,
  WorkspaceDocument,
  WorkspaceService
} from '../../core/workspace/workspace.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss'
})
export class Dashboard implements OnInit {

  loading = true;

  errorMessage = '';

  dashboard: WorkspaceDashboard = {
    totalDocuments: 0,
    uploaded: 0,
    aiProcessing: 0,
    reviewRequired: 0,
    linkRequired: 0,
    linked: 0,
    verified: 0,
    readyForSed: 0,
    sentToSed: 0,
    processingFailed: 0,
    sedError: 0
  };

  documents: WorkspaceDocument[] = [];

  constructor(
    private readonly workspaceService: WorkspaceService,
    private readonly authService: AuthService,
    private readonly router: Router,
    private readonly changeDetectorRef: ChangeDetectorRef
  ) {
  }

  ngOnInit(): void {
    this.loadDashboard();
  }

  loadDashboard(): void {

    this.loading = true;
    this.errorMessage = '';

    this.changeDetectorRef.markForCheck();

    forkJoin({
      dashboard:
        this.workspaceService.getDashboard(),

      documents:
        this.workspaceService.getDocuments()
    })
      .pipe(
        finalize(() => {

          this.loading = false;

          this.changeDetectorRef.markForCheck();
        })
      )
      .subscribe({

        next: response => {

          this.dashboard =
            response.dashboard;

          this.documents =
            response.documents;

          this.changeDetectorRef.markForCheck();
        },

        error: (
          error: HttpErrorResponse
        ) => {

          if (error.status === 401) {

            this.authService.logout();

            void this.router.navigate(
              ['/login']
            );

            return;
          }

          if (error.status === 0) {

            this.errorMessage =
              'Не удалось подключиться к серверу DocFlow.';

          } else {

            this.errorMessage =
              'Не удалось загрузить данные рабочего пространства.';
          }

          this.changeDetectorRef.markForCheck();
        }
      });
  }

  get recentDocuments(): WorkspaceDocument[] {

    return this.documents.slice(
      0,
      4
    );
  }

  get classifiedDocumentsCount(): number {

    return this.documents.filter(
      document =>
        document.documentType !== null
    ).length;
  }

  countDocumentType(
    type: string
  ): number {

    return this.documents.filter(
      document =>
        document.documentType === type
    ).length;
  }

  documentTypePercent(
    type: string
  ): number {

    const total =
      this.classifiedDocumentsCount;

    if (total === 0) {
      return 0;
    }

    return Math.round(
      (
        this.countDocumentType(type) /
        total
      ) * 100
    );
  }

  get classificationGradient(): string {

    const types = [
      {
        type: 'CONTRACT',
        color: '#4f55d9'
      },
      {
        type: 'APPENDIX',
        color: '#6558c8'
      },
      {
        type: 'AMENDMENT_CHANGE',
        color: '#8268d6'
      },
      {
        type: 'AMENDMENT_PROLONGATION',
        color: '#ab8ce8'
      }
    ];

    const total =
      this.classifiedDocumentsCount;

    if (total === 0) {
      return '#f0eef8';
    }

    let start = 0;

    const parts: string[] = [];

    for (const item of types) {

      const count =
        this.countDocumentType(
          item.type
        );

      if (count === 0) {
        continue;
      }

      const size =
        (count / total) * 100;

      const end =
        start + size;

      parts.push(
        `${item.color} ${start}% ${end}%`
      );

      start = end;
    }

    return `conic-gradient(${parts.join(', ')})`;
  }

  statusPercent(
    value: number
  ): number {

    if (
      this.dashboard.totalDocuments === 0
    ) {
      return 0;
    }

    return Math.round(
      (
        value /
        this.dashboard.totalDocuments
      ) * 100
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

      case 'LINKED':
      case 'UPLOADED':
      case 'AI_PROCESSING':
        return 'violet';

      case 'REVIEW_REQUIRED':
      case 'PROCESSING_FAILED':
      case 'SED_ERROR':
        return 'red-status';

      default:
        return 'violet';
    }
  }

  getDocumentName(
    document: WorkspaceDocument
  ): string {

    return document.originalName
      ?? `Документ #${document.documentId}`;
  }

  getDocumentIdentifier(
    document: WorkspaceDocument
  ): string {

    if (document.documentNumber) {

      return `№ ${document.documentNumber}`;
    }

    return `ID ${document.documentId}`;
  }

  getDocumentDate(
    document: WorkspaceDocument
  ): string {

    if (document.documentDate) {

      return this.formatDate(
        document.documentDate
      );
    }

    if (document.createdAt) {

      return this.formatDate(
        document.createdAt
      );
    }

    return '—';
  }

  private formatDate(
    value: string
  ): string {

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

    return (
      `${match[3]}.` +
      `${match[2]}.` +
      `${match[1]}`
    );
  }
}