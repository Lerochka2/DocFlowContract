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
  Observable,
  catchError,
  finalize,
  forkJoin,
  map,
  of,
  switchMap
} from 'rxjs';

import {
  DocumentSummary,
  WorkspaceDocument,
  WorkspaceService
} from '../../core/workspace/workspace.service';

import {
  AuthService
} from '../../core/auth/auth.service';

@Component({
  selector: 'app-routing-workspace',
  standalone: true,
  imports: [
    FormsModule
  ],
  templateUrl: './routing.html',
  styleUrl: './routing.scss'
})
export class RoutingWorkspace implements OnInit {

  documents: DocumentSummary[] = [];

  loading = true;
  errorMessage = '';

  searchQuery = '';
  targetFilter = 'ALL';

  constructor(
    private readonly workspaceService: WorkspaceService,
    private readonly authService: AuthService,
    private readonly router: Router,
    private readonly changeDetectorRef: ChangeDetectorRef
  ) {
  }

  ngOnInit(): void {
    this.loadRoutingWorkspace();
  }

  private get currentRole(): string | null {

    return this.authService
      .getCurrentUser()
      ?.role
      ?? null;
  }


  get isAdminView(): boolean {

    return this.currentRole
      === 'ADMIN';
  }


  get isSpecialistView(): boolean {

    return (
      this.currentRole === 'LAWYER'
      ||
      this.currentRole === 'CONTRACT_MANAGER'
    );
  }


  get specialistRoleLabel(): string {

    switch (this.currentRole) {

      case 'LAWYER':
        return 'Юрист';

      case 'CONTRACT_MANAGER':
        return 'Менеджер по договорам';

      default:
        return 'Специалист';
    }
  }


  get pageEyebrow(): string {

    return this.isAdminView
      ? 'АВТОМАТИЗИРОВАННАЯ МАРШРУТИЗАЦИЯ'
      : 'РАБОЧЕЕ ПРОСТРАНСТВО СПЕЦИАЛИСТА';
  }


  get pageTitle(): string {

    return this.isAdminView
      ? 'Маршрутизация документов'
      : 'Мои задачи';
  }


  get pageDescription(): string {

    return this.isAdminView
      ? 'Контроль автоматически сформированных маршрутов на основе результата интеллектуальной классификации документов.'
      : 'Документы, автоматически назначенные вам для проверки и подготовки к передаче в корпоративную СЭД.';
  }


  get actionableTaskCount(): number {

    return this.documents.filter(
      document => {

        if (
          document.documentType
          === 'CONTRACT'
        ) {

          return document.status
            === 'VERIFIED';
        }

        return document.status
          === 'LINKED';
      }
    ).length;
  }


  get completedSpecialistTaskCount(): number {

    return this.documents.filter(
      document =>
        document.status === 'READY_FOR_SED'
        ||
        document.status === 'SENT_TO_SED'
    ).length;
  }

  loadRoutingWorkspace(): void {

    this.loading = true;
    this.errorMessage = '';

    this.workspaceService
      .getDocuments()
      .pipe(

        switchMap(
          documents =>
            this.loadSummaries(
              documents
            )
        ),

        finalize(
          () => {

            this.loading = false;

            this.changeDetectorRef
              .markForCheck();
          }
        )
      )
      .subscribe({

        next: documents => {

          this.documents =
            documents.filter(
              document =>
                document.routing !== null
            );

          this.changeDetectorRef
            .markForCheck();
        },

        error: (
          error: HttpErrorResponse
        ) => {

          if (error.status === 0) {

            this.errorMessage =
              'Не удалось подключиться к серверу DocFlow.';

          } else {

            this.errorMessage =
              'Не удалось загрузить данные маршрутизации.';
          }

          this.changeDetectorRef
            .markForCheck();
        }
      });
  }

  get filteredDocuments(): DocumentSummary[] {

    const query =
      this.searchQuery
        .trim()
        .toLowerCase();

    return this.documents.filter(
      document => {

        const matchesTarget =
          !this.isAdminView
          ||
          this.targetFilter === 'ALL'
          ||
          document.routing?.routingTarget
            === this.targetFilter;

        if (!matchesTarget) {
          return false;
        }

        if (!query) {
          return true;
        }

        const values = [
          document.originalName,
          document.documentNumber,
          document.subject,
          document.routing?.routingReason,
          this.getDocumentTypeLabel(
            document.documentType
          ),
          this.getRoutingTargetLabel(
            document.routing?.routingTarget
              || null
          )
        ];

        return values.some(
          value =>
            value
              ?.toLowerCase()
              .includes(query)
        );
      }
    );
  }

  get contractManagerCount(): number {

    return this.documents.filter(
      document =>
        document.routing?.routingTarget
          === 'CONTRACT_MANAGER'
    ).length;
  }

  get lawyerCount(): number {

    return this.documents.filter(
      document =>
        document.routing?.routingTarget
          === 'LAWYER'
    ).length;
  }

  get routedCount(): number {

    return this.documents.filter(
      document =>
        document.routing?.routingStatus
          === 'ROUTED'
    ).length;
  }

  resetFilters(): void {

    this.searchQuery = '';
    this.targetFilter = 'ALL';
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

      case 'AMENDMENT_CHANGE':
        return 'Доп. соглашение — изменение';

      case 'AMENDMENT_PROLONGATION':
        return 'Доп. соглашение — продление';

      case 'APPENDIX':
        return 'Приложение';

      default:
        return 'Не определён';
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

  getRoutingStatusLabel(
    status: string | null
  ): string {

    switch (status) {

      case 'ROUTED':
        return 'Маршрут определён';

      case 'READY':
        return 'Готов к маршрутизации';

      case 'SKIPPED':
        return 'Пропущен';

      case 'FAILED':
        return 'Ошибка маршрутизации';

      default:
        return status || 'Нет данных';
    }
  }

  getDocumentStatusLabel(
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

  getTargetClass(
    target: string | null
  ): string {

    switch (target) {

      case 'LAWYER':
        return 'lawyer';

      case 'CONTRACT_MANAGER':
        return 'manager';

      default:
        return 'neutral';
    }
  }

  formatDateTime(
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

    const dateMatch =
      /^(\d{4})-(\d{2})-(\d{2})$/
        .exec(
          datePart
        );

    const date =
      dateMatch
        ? `${dateMatch[3]}.${dateMatch[2]}.${dateMatch[1]}`
        : datePart;

    const timeMatch =
      /T(\d{2}):(\d{2})/
        .exec(
          value
        );

    if (!timeMatch) {
      return date;
    }

    return `${date}, ${timeMatch[1]}:${timeMatch[2]}`;
  }

  private loadSummaries(
    documents: WorkspaceDocument[]
  ): Observable<DocumentSummary[]> {

    if (documents.length === 0) {

      return of(
        [] as DocumentSummary[]
      );
    }

    return forkJoin(
      documents.map(
        document =>
          this.workspaceService
            .getDocumentSummary(
              document.documentId
            )
            .pipe(
              catchError(
                () => of(null)
              )
            )
      )
    )
      .pipe(
        map(
          summaries =>
            summaries.filter(
              (
                document
              ): document is DocumentSummary =>
                document !== null
            )
        )
      );
  }
}