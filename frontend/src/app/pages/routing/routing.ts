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

import {
  ApprovalService,
  ApprovalStep
} from '../../core/approval/approval.service';


interface ApprovalTaskView
  extends ApprovalStep {

  summary:
    DocumentSummary | null;
}


type SpecialistFilter =
  | 'ALL'
  | 'AVAILABLE'
  | 'MINE'
  | 'DUE_SOON'
  | 'OVERDUE';


@Component({
  selector: 'app-routing-workspace',
  standalone: true,
  imports: [
    FormsModule
  ],
  templateUrl: './routing.html',
  styleUrl: './routing.scss'
})
export class RoutingWorkspace
  implements OnInit {

  documents:
    DocumentSummary[] = [];

  tasks:
    ApprovalTaskView[] = [];

  loading = true;

  errorMessage = '';

  actionErrorMessage = '';

  searchQuery = '';

  targetFilter = 'ALL';

  specialistFilter:
    SpecialistFilter = 'ALL';

  claimingStepId:
    number | null = null;


  constructor(
    private readonly workspaceService:
      WorkspaceService,

    private readonly approvalService:
      ApprovalService,

    private readonly authService:
      AuthService,

    private readonly router:
      Router,

    private readonly changeDetectorRef:
      ChangeDetectorRef
  ) {
  }


  ngOnInit(): void {

    this.loadRoutingWorkspace();
  }


  private get currentRole():
    string | null {

    return this.authService
      .getCurrentUser()
      ?.role
      ?? null;
  }


  private get currentUserId():
    number | null {

    return this.authService
      .getCurrentUser()
      ?.userId
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


  get pageEyebrow(): string {

    return this.isAdminView
      ? 'АВТОМАТИЗИРОВАННАЯ МАРШРУТИЗАЦИЯ'
      : 'СОГЛАСОВАНИЕ ДОКУМЕНТОВ';
  }


  get pageTitle(): string {

    return this.isAdminView
      ? 'Маршрутизация документов'
      : 'На согласовании';
  }


  get pageDescription(): string {

    return this.isAdminView
      ? 'Контроль маршрутов на основе типа документа, AI-анализа рисков и изменений относительно основного договора.'
      : 'Документы, которые требуют вашего решения. Возьмите свободный документ в работу, изучите его и завершите согласование.';
  }


  loadRoutingWorkspace(): void {

    this.loading = true;

    this.errorMessage = '';

    this.actionErrorMessage = '';

    if (this.isAdminView) {

      this.loadAdminWorkspace();

      return;
    }

    if (this.isSpecialistView) {

      this.loadSpecialistWorkspace();

      return;
    }

    this.loading = false;

    this.errorMessage =
      'Для текущей роли этот раздел недоступен.';
  }


  private loadAdminWorkspace(): void {

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

          this.errorMessage =
            this.resolveLoadError(
              error,
              'Не удалось загрузить данные маршрутизации.'
            );

          this.changeDetectorRef
            .markForCheck();
        }
      });
  }


  private loadSpecialistWorkspace(): void {

    this.approvalService
      .getMyQueue()
      .pipe(

        switchMap(
          steps =>
            this.loadTaskSummaries(
              steps
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

        next: tasks => {

          this.tasks =
            tasks;

          this.changeDetectorRef
            .markForCheck();
        },

        error: (
          error: HttpErrorResponse
        ) => {

          this.errorMessage =
            this.resolveLoadError(
              error,
              'Не удалось загрузить очередь согласования.'
            );

          this.changeDetectorRef
            .markForCheck();
        }
      });
  }


  get filteredDocuments():
    DocumentSummary[] {

    const query =
      this.searchQuery
        .trim()
        .toLowerCase();

    return this.documents.filter(
      document => {

        const matchesTarget =
          this.targetFilter === 'ALL'
          ||
          document.routing
            ?.routingTarget
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


  get filteredTasks():
    ApprovalTaskView[] {

    const query =
      this.searchQuery
        .trim()
        .toLowerCase();

    return this.tasks.filter(
      task => {

        if (
          this.specialistFilter
          === 'AVAILABLE'
          &&
          task.status !== 'PENDING'
        ) {
          return false;
        }

        if (
          this.specialistFilter
          === 'MINE'
          &&
          task.status !== 'IN_PROGRESS'
        ) {
          return false;
        }

        if (
          this.specialistFilter
          === 'DUE_SOON'
          &&
          !this.isDueSoon(task)
        ) {
          return false;
        }

        if (
          this.specialistFilter
          === 'OVERDUE'
          &&
          !this.isOverdue(task)
        ) {
          return false;
        }

        if (!query) {
          return true;
        }

        const values = [
          task.summary?.originalName,
          task.documentNumber,
          task.summary?.subject,
          task.routingReason,
          task.assignedUserFullName,
          this.getDocumentTypeLabel(
            task.documentType
          ),
          this.getApprovalStatusLabel(
            task.status
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


  get pendingTaskCount(): number {

    return this.tasks.filter(
      task =>
        task.status === 'PENDING'
    ).length;
  }


  get inProgressTaskCount(): number {

    return this.tasks.filter(
      task =>
        task.status === 'IN_PROGRESS'
    ).length;
  }


  get dueSoonTaskCount(): number {

    return this.tasks.filter(
      task =>
        this.isDueSoon(task)
    ).length;
  }


  get overdueTaskCount(): number {

    return this.tasks.filter(
      task =>
        this.isOverdue(task)
    ).length;
  }


  get contractManagerCount(): number {

    return this.documents.filter(
      document =>
        document.routing
          ?.routingTarget
          === 'CONTRACT_MANAGER'
    ).length;
  }


  get lawyerCount(): number {

    return this.documents.filter(
      document =>
        document.routing
          ?.routingTarget
          === 'LAWYER'
    ).length;
  }


  get routedCount(): number {

    return this.documents.filter(
      document =>
        document.routing
          ?.routingStatus
          === 'ROUTED'
    ).length;
  }


  setSpecialistFilter(
    filter: SpecialistFilter
  ): void {

    this.specialistFilter =
      filter;
  }


  resetFilters(): void {

    this.searchQuery = '';

    this.targetFilter = 'ALL';

    this.specialistFilter = 'ALL';
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


  handleTaskAction(
    task: ApprovalTaskView
  ): void {

    if (
      task.status === 'IN_PROGRESS'
    ) {

      this.openDocument(
        task.documentId
      );

      return;
    }

    if (
      task.status !== 'PENDING'
    ) {

      return;
    }

    this.claimTask(
      task
    );
  }


  private claimTask(
    task: ApprovalTaskView
  ): void {

    if (
      this.claimingStepId !== null
    ) {
      return;
    }

    this.claimingStepId =
      task.stepId;

    this.actionErrorMessage = '';

    this.approvalService
      .claim(
        task.stepId
      )
      .pipe(

        finalize(
          () => {

            this.claimingStepId = null;

            this.changeDetectorRef
              .markForCheck();
          }
        )
      )
      .subscribe({

        next: claimed => {

          const index =
            this.tasks.findIndex(
              item =>
                item.stepId
                === claimed.stepId
            );

          if (index >= 0) {

            this.tasks[index] = {
              ...this.tasks[index],
              ...claimed
            };

            this.tasks = [
              ...this.tasks
            ];
          }

          this.openDocument(
            claimed.documentId
          );
        },

        error: (
          error: HttpErrorResponse
        ) => {

          if (error.status === 409) {

            this.actionErrorMessage =
              'Эту задачу уже взял другой специалист или её состояние изменилось. Обновите очередь.';

          } else if (
            error.status === 403
          ) {

            this.actionErrorMessage =
              'Эта задача назначена другой роли.';

          } else {

            this.actionErrorMessage =
              this.resolveLoadError(
                error,
                'Не удалось взять документ в работу.'
              );
          }

          this.changeDetectorRef
            .markForCheck();
        }
      });
  }


  isOverdue(
    task: ApprovalTaskView
  ): boolean {

    if (
      !task.dueAt
      ||
      (
        task.status !== 'PENDING'
        &&
        task.status !== 'IN_PROGRESS'
      )
    ) {
      return false;
    }

    const due =
      new Date(
        task.dueAt
      );

    if (
      Number.isNaN(
        due.getTime()
      )
    ) {
      return false;
    }

    return due.getTime()
      < Date.now();
  }


  isDueSoon(
    task: ApprovalTaskView
  ): boolean {

    if (
      !task.dueAt
      ||
      this.isOverdue(task)
    ) {
      return false;
    }

    const due =
      new Date(
        task.dueAt
      );

    if (
      Number.isNaN(
        due.getTime()
      )
    ) {
      return false;
    }

    const remaining =
      due.getTime()
      - Date.now();

    return remaining
      <= 24 * 60 * 60 * 1000;
  }


  getDueStatusText(
    task: ApprovalTaskView
  ): string {

    if (!task.dueAt) {
      return '';
    }

    const due =
      new Date(
        task.dueAt
      );

    if (
      Number.isNaN(
        due.getTime()
      )
    ) {
      return '';
    }

    const difference =
      due.getTime()
      - Date.now();

    if (difference < 0) {

      const overdueMilliseconds =
        Math.abs(
          difference
        );

      if (
        overdueMilliseconds
        < 60 * 60 * 1000
      ) {
        return 'Просрочено менее часа';
      }

      const overdueHours =
        Math.ceil(
          overdueMilliseconds
          / (60 * 60 * 1000)
        );

      if (overdueHours < 24) {
        return `Просрочено примерно на ${overdueHours} ч`;
      }

      const overdueDays =
        Math.ceil(
          overdueHours / 24
        );

      return `Просрочено примерно на ${overdueDays} дн.`;
    }

    if (
      difference
      <= 60 * 60 * 1000
    ) {
      return 'Осталось менее часа';
    }

    const remainingHours =
      Math.ceil(
        difference
        / (60 * 60 * 1000)
      );

    return `Осталось около ${remainingHours} ч`;
  }


  getAssigneeLabel(
    task: ApprovalTaskView
  ): string {

    if (
      task.assignedUserFullName
    ) {
      return task.assignedUserFullName;
    }

    return 'Не назначен';
  }


  getApprovalStatusLabel(
    status: string | null
  ): string {

    switch (status) {

      case 'PENDING':
        return 'Ожидает специалиста';

      case 'IN_PROGRESS':
        return 'В работе';

      case 'APPROVED':
        return 'Согласован';

      case 'RETURNED':
        return 'Возвращён';

      case 'REJECTED':
        return 'Отклонён';

      case 'CANCELLED':
        return 'Отменён';

      default:
        return status || 'Нет данных';
    }
  }


  getApprovalStatusClass(
    status: string | null
  ): string {

    switch (status) {

      case 'PENDING':
        return 'pending';

      case 'IN_PROGRESS':
        return 'progress';

      case 'APPROVED':
        return 'approved';

      case 'RETURNED':
        return 'returned';

      case 'REJECTED':
        return 'rejected';

      default:
        return 'neutral';
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

    const date =
      new Date(value);

    if (
      Number.isNaN(
        date.getTime()
      )
    ) {
      return value;
    }

    return new Intl.DateTimeFormat(
      'ru-RU',
      {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      }
    ).format(date);
  }


  private loadSummaries(
    documents: WorkspaceDocument[]
  ): Observable<DocumentSummary[]> {

    if (
      documents.length === 0
    ) {

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
                () =>
                  of(null)
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


  private loadTaskSummaries(
    steps: ApprovalStep[]
  ): Observable<ApprovalTaskView[]> {

    if (
      steps.length === 0
    ) {

      return of(
        [] as ApprovalTaskView[]
      );
    }

    return forkJoin(
      steps.map(
        step =>
          this.workspaceService
            .getDocumentSummary(
              step.documentId
            )
            .pipe(

              map(
                summary => ({
                  ...step,
                  summary
                })
              ),

              catchError(
                () =>
                  of({
                    ...step,
                    summary: null
                  })
              )
            )
      )
    );
  }


  private resolveLoadError(
    error: HttpErrorResponse,
    fallback: string
  ): string {

    if (
      error.status === 0
    ) {
      return 'Не удалось подключиться к серверу DocFlow.';
    }

    const message =
      error.error?.message;

    if (
      typeof message === 'string'
      &&
      message.trim()
    ) {
      return message;
    }

    return fallback;
  }
}