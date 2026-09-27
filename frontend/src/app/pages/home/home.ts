import {
  ChangeDetectorRef,
  Component,
  OnInit
} from '@angular/core';

import {
  Router,
  RouterLink
} from '@angular/router';

import {
  finalize,
  forkJoin
} from 'rxjs';

import {
  AuthService,
  CurrentUserResponse
} from '../../core/auth/auth.service';

import {
  NotificationService
} from '../../core/notifications/notification.service';

import {
  WorkspaceDashboard,
  WorkspaceDocument,
  WorkspaceService
} from '../../core/workspace/workspace.service';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [
    RouterLink
  ],
  templateUrl: './home.html',
  styleUrl: './home.scss'
})
export class Home implements OnInit {

  currentUser:
    CurrentUserResponse | null = null;

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
    sedError: 0,
    expiringSoonContracts: 0,
    expiredContracts: 0,

    approvalCycles: 0,
    approvalPending: 0,
    approvalInProgress: 0,
    approvalApproved: 0,
    approvalReturned: 0,
    approvalRejected: 0,
    approvalOverdue: 0,

    approvalCompletedOnTime: 0,
    approvalCompletedLate: 0,
    approvalSlaCompliancePercent: 0,
    averageApprovalTimeHours: 0,
    approvalReturnRatePercent: 0,

    riskAnalyzedDocuments: 0,
    lowRiskContracts: 0,
    mediumRiskContracts: 0,
    highRiskContracts: 0,
    averageRiskScore: 0
  };

  documents:
    WorkspaceDocument[] = [];

  unreadNotifications = 0;

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
      ChangeDetectorRef
  ) {
  }

  ngOnInit(): void {

    this.currentUser =
      this.authService.getCurrentUser();

    this.loadHome();
  }

  loadHome(): void {

    this.loading = true;
    this.errorMessage = '';

    forkJoin({
      dashboard:
        this.workspaceService
          .getDashboard(),

      documents:
        this.workspaceService
          .getDocuments(),

      notifications:
        this.notificationService
          .getUnreadCount()
    })
      .pipe(
        finalize(
          () => {

            this.loading = false;

            this.changeDetectorRef
              .markForCheck();
          }
        )
      )
      .subscribe({

        next: result => {

          this.dashboard =
            result.dashboard;

          this.documents =
            result.documents;

          this.unreadNotifications =
            result.notifications.unreadCount;

          this.changeDetectorRef
            .markForCheck();
        },

        error: () => {

          this.errorMessage =
            'Не удалось загрузить данные главной страницы.';

          this.changeDetectorRef
            .markForCheck();
        }
      });
  }

  get greetingName(): string {

    const fullName =
      this.currentUser?.fullName
        ?.trim();

    if (!fullName) {
      return 'пользователь';
    }

    const parts =
      fullName
        .split(/\s+/)
        .filter(Boolean);

    if (parts.length >= 3) {

      return (
        parts[1]
        + ' '
        + parts[2]
      );
    }

    if (parts.length === 2) {
      return parts[1];
    }

    return parts[0];
  }

  get todayLabel(): string {

    const value =
      new Intl.DateTimeFormat(
        'ru-RU',
        {
          weekday: 'long',
          day: 'numeric',
          month: 'long',
          year: 'numeric'
        }
      ).format(
        new Date()
      );

    return (
      value.substring(0, 1)
        .toUpperCase()
      +
      value.substring(1)
    );
  }

  get roleLabel(): string {

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
        return 'Пользователь DocFlow';
    }
  }

  get isAdmin(): boolean {

    return (
      this.currentUser?.role
      === 'ADMIN'
    );
  }

  get isOperator(): boolean {

    return (
      this.currentUser?.role
      === 'DOCUMENT_OPERATOR'
    );
  }

  get isSpecialist(): boolean {

    return (
      this.currentUser?.role
      === 'LAWYER'
      ||
      this.currentUser?.role
      === 'CONTRACT_MANAGER'
    );
  }

  get canUseAiWorkspace(): boolean {

    return (
      this.isAdmin
      ||
      this.isOperator
    );
  }

  get canUseReviewWorkspace(): boolean {

    return (
      this.isAdmin
      ||
      this.isOperator
    );
  }

  get attentionCount(): number {

    if (this.isSpecialist) {

      return this.documents.filter(
        document =>
          document.status !== 'READY_FOR_SED'
          &&
          document.status !== 'SENT_TO_SED'
      ).length;
    }

    return (
      this.dashboard.reviewRequired
      +
      this.dashboard.linkRequired
      +
      this.dashboard.processingFailed
      +
      this.dashboard.sedError
    );
  }

  get recentDocuments():
    WorkspaceDocument[] {

    return this.documents.slice(
      0,
      5
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

      case 'APPROVED':
        return 'Согласован';

      case 'RETURNED_FOR_CORRECTION':
        return 'Возвращён на доработку';

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

  getStatusClass(
    status: string | null
  ): string {

    switch (status) {

      case 'APPROVED':
      case 'VERIFIED':
      case 'READY_FOR_SED':
      case 'SENT_TO_SED':
        return 'success';

      case 'REVIEW_REQUIRED':
      case 'RETURNED_FOR_CORRECTION':
        return 'warning';

      case 'REJECTED':
      case 'PROCESSING_FAILED':
      case 'SED_ERROR':
        return 'error';

      case 'LINK_REQUIRED':
        return 'link';

      default:
        return 'neutral';
    }
  }

  getDocumentTitle(
    document: WorkspaceDocument
  ): string {

    if (document.documentNumber) {

      return (
        this.getDocumentTypeLabel(
          document.documentType
        )
        + ' №'
        + document.documentNumber
      );
    }

    return (
      document.originalName
      ||
      'Документ'
    );
  }

  getDocumentDate(
    document: WorkspaceDocument
  ): string {

    const value =
      document.documentDate
      ||
      document.createdAt;

    if (!value) {
      return '—';
    }

    const date =
      value.substring(
        0,
        10
      );

    const match =
      /^(\d{4})-(\d{2})-(\d{2})$/
        .exec(date);

    if (!match) {
      return value;
    }

    return (
      `${match[3]}.`
      + `${match[2]}.`
      + `${match[1]}`
    );
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
}