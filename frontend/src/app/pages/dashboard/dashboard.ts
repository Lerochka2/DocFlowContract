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
  WorkspaceActivity,
  WorkspaceActivityDay,
  WorkspaceDashboard,
  WorkspaceDocument,
  WorkspaceService
} from '../../core/workspace/workspace.service';

interface ActivityHeatmapCell {
  key: string;
  date: string | null;
  documentCount: number;
  placeholder: boolean;
}

interface ActivityHeatmapWeek {
  key: string;
  monthLabel: string;
  days: ActivityHeatmapCell[];
}

interface ActivityTimeCell {
  key: string;
  date: string;
  startHour: number;
  documentCount: number;
}

interface ActivityTimeDay {
  key: string;
  label: string;
  cells: ActivityTimeCell[];
}

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

  documents: WorkspaceDocument[] = [];

  activity: WorkspaceActivity | null = null;

  selectedActivityDays: 30 | 90 | 365 = 90;

  activityLoading = false;

  activityErrorMessage = '';

  constructor(
    private readonly workspaceService: WorkspaceService,
    private readonly authService: AuthService,
    private readonly router: Router,
    private readonly changeDetectorRef: ChangeDetectorRef
  ) {
  }

  // Rendering adapters for existing API responses; business calculations stay unchanged.
  heatmapTooltip = '';

  get dashboardGreeting(): string {
    const name = this.authService.getCurrentUser()?.fullName?.trim();
    return name ? `Добро пожаловать, ${name}!` : 'Добро пожаловать в DocFlow!';
  }

  get funnelStages(): { label: string; count: number; description: string }[] {
    return [
      { label: 'В рабочем пространстве', description: 'Все доступные документы. База для расчёта процентов', count: this.dashboard.totalDocuments },
      { label: 'Классифицированы', description: 'Документы с определённым классом', count: this.classifiedDocumentsCount },
      { label: 'Проверены', description: 'Документы в текущем статусе «Проверен»', count: this.dashboard.verified },
      { label: 'Привязаны', description: 'Документы в текущем статусе «Связан с договором»', count: this.dashboard.linked },
      { label: 'Отправлены в СЭД', description: 'Документы, успешно отправленные в корпоративную СЭД', count: this.dashboard.sentToSed }
    ];
  }

  getHeatmapLoadLabel(count: number, timeSlot = false): string {
    const level = timeSlot ? this.timeActivityLevel(count) : this.activityLevel(count);
    return ['нет активности', 'низкая', 'умеренная', 'высокая', 'пиковая'][level] ?? 'нет активности';
  }

  getHeatmapTooltip(date: string | null, count: number): string {
    if (!date) return '';
    return `${this.getActivityCellTooltip(date, count)} · Нагрузка: ${this.getHeatmapLoadLabel(count)}`;
  }
  ngOnInit(): void {
    this.loadDashboard();
    this.loadActivity();
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


  loadActivity(): void {

    this.activityLoading = true;
    this.activityErrorMessage = '';

    this.workspaceService
      .getActivity(
        this.selectedActivityDays
      )
      .pipe(
        finalize(() => {

          this.activityLoading = false;

          this.changeDetectorRef
            .markForCheck();
        })
      )
      .subscribe({

        next: activity => {

          this.activity =
            activity;

          this.changeDetectorRef
            .markForCheck();
        },

        error: () => {

          this.activityErrorMessage =
            'Не удалось загрузить активность обработки документов.';

          this.changeDetectorRef
            .markForCheck();
        }
      });
  }


  setActivityPeriod(
    days: 30 | 90 | 365
  ): void {

    if (
      this.selectedActivityDays === days
    ) {
      return;
    }

    this.heatmapTooltip = '';

    this.selectedActivityDays =
      days;

    this.loadActivity();
  }


  get activityTimeDays(): ActivityTimeDay[] {

    const slots =
      this.activity?.timeActivity ?? [];

    const grouped =
      new Map<string, ActivityTimeCell[]>();


    for (const slot of slots) {

      const cells =
        grouped.get(slot.date) ?? [];

      cells.push({
        key:
          `${slot.date}-${slot.startHour}`,
        date:
          slot.date,
        startHour:
          slot.startHour,
        documentCount:
          slot.documentCount
      });

      grouped.set(
        slot.date,
        cells
      );
    }


    return Array
      .from(grouped.entries())
      .map(
        ([date, cells]) => ({

          key:
            date,

          label:
            this.getActivityDayLabel(
              date
            ),

          cells:
            cells.sort(
              (a, b) =>
                a.startHour
                -
                b.startHour
            )
        })
      );
  }


  timeActivityLevel(
    value: number
  ): number {

    const values =
      this.activity?.timeActivity ?? [];

    const max =
      values.reduce(
        (
          current,
          item
        ) =>
          Math.max(
            current,
            item.documentCount
          ),
        0
      );


    if (
      value <= 0
      ||
      max <= 0
    ) {
      return 0;
    }


    return Math.min(
      4,
      Math.max(
        1,
        Math.ceil(
          (
            value /
            max
          ) * 4
        )
      )
    );
  }


  getTimeActivityTooltip(
    cell: ActivityTimeCell
  ): string {

    const endHour =
      cell.startHour + 6;

    return (
      `${this.getActivityDayLabel(cell.date)}, ` +
      `${cell.startHour
        .toString()
        .padStart(2, '0')}:00–` +
      `${endHour
        .toString()
        .padStart(2, '0')}:00 — ` +
      `${cell.documentCount} ` +
      `${this.getDocumentWord(
        cell.documentCount
      )}`
    );
  }


  private getActivityDayLabel(
    value: string
  ): string {

    const date =
      this.parseActivityDate(
        value
      );

    const months = [
      'янв.',
      'февр.',
      'марта',
      'апр.',
      'мая',
      'июня',
      'июля',
      'авг.',
      'сент.',
      'окт.',
      'нояб.',
      'дек.'
    ];

    return (
      `${date.getDate()} ` +
      `${months[date.getMonth()]}`
    );
  }


  get activityWeeks(): ActivityHeatmapWeek[] {

    const values =
      this.activity?.activity ?? [];

    if (values.length === 0) {
      return [];
    }

    const cells:
      ActivityHeatmapCell[] = [];

    const firstDate =
      this.parseActivityDate(
        values[0].date
      );

    const firstDayIndex =
      (
        firstDate.getDay() + 6
      ) % 7;


    for (
      let index = 0;
      index < firstDayIndex;
      index++
    ) {

      cells.push({
        key: `before-${index}`,
        date: null,
        documentCount: 0,
        placeholder: true
      });
    }


    for (const day of values) {

      cells.push({
        key: day.date,
        date: day.date,
        documentCount:
          day.documentCount,
        placeholder: false
      });
    }


    while (
      cells.length % 7 !== 0
    ) {

      cells.push({
        key: `after-${cells.length}`,
        date: null,
        documentCount: 0,
        placeholder: true
      });
    }


    const weeks:
      ActivityHeatmapWeek[] = [];

    let previousMonth = -1;


    for (
      let index = 0;
      index < cells.length;
      index += 7
    ) {

      const weekDays =
        cells.slice(
          index,
          index + 7
        );

      const realDay =
        weekDays.find(
          day =>
            day.date !== null
        );

      let monthLabel = '';

      if (realDay?.date) {

        const date =
          this.parseActivityDate(
            realDay.date
          );

        const month =
          date.getMonth();

        if (
          month !== previousMonth
        ) {

          monthLabel =
            this.getActivityMonthLabel(
              month
            );

          previousMonth =
            month;
        }
      }


      weeks.push({
        key: `week-${index / 7}`,
        monthLabel,
        days: weekDays
      });
    }


    return weeks;
  }


  activityLevel(
    value: number
  ): number {

    const max =
      this.activity
        ?.maxDailyDocuments ?? 0;

    if (
      value <= 0
      ||
      max <= 0
    ) {
      return 0;
    }

    return Math.min(
      4,
      Math.max(
        1,
        Math.ceil(
          (
            value /
            max
          ) * 4
        )
      )
    );
  }


  getActivityTooltip(
    day: WorkspaceActivityDay
  ): string {

    const date =
      this.parseActivityDate(
        day.date
      );

    const months = [
      'января',
      'февраля',
      'марта',
      'апреля',
      'мая',
      'июня',
      'июля',
      'августа',
      'сентября',
      'октября',
      'ноября',
      'декабря'
    ];

    const word =
      this.getDocumentWord(
        day.documentCount
      );

    return (
      `${date.getDate()} ` +
      `${months[date.getMonth()]} — ` +
      `${day.documentCount} ${word}`
    );
  }


  getActivityCellTooltip(
    dateValue: string | null,
    documentCount: number
  ): string {

    if (!dateValue) {
      return '';
    }

    return this.getActivityTooltip({
      date: dateValue,
      documentCount
    });
  }


  private parseActivityDate(
    value: string
  ): Date {

    const parts =
      value
        .split('-')
        .map(Number);

    return new Date(
      parts[0],
      parts[1] - 1,
      parts[2]
    );
  }


  private getActivityMonthLabel(
    month: number
  ): string {

    const months = [
      'янв.',
      'февр.',
      'март',
      'апр.',
      'май',
      'июнь',
      'июль',
      'авг.',
      'сент.',
      'окт.',
      'нояб.',
      'дек.'
    ];

    return months[month] ?? '';
  }


  private getDocumentWord(
    value: number
  ): string {

    const mod100 =
      value % 100;

    const mod10 =
      value % 10;

    if (
      mod100 >= 11
      &&
      mod100 <= 14
    ) {
      return 'документов';
    }

    if (mod10 === 1) {
      return 'документ';
    }

    if (
      mod10 >= 2
      &&
      mod10 <= 4
    ) {
      return 'документа';
    }

    return 'документов';
  }


  get isSpecialistView(): boolean {

    const role =
      this.authService
        .getCurrentUser()
        ?.role;

    return (
      role === 'LAWYER'
      ||
      role === 'CONTRACT_MANAGER'
    );
  }

  get specialistActionableCount(): number {

    return this.documents.filter(
      document =>
        document.status !== 'READY_FOR_SED'
        &&
        document.status !== 'SENT_TO_SED'
    ).length;
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
        color: '#6385ed'
      },
      {
        type: 'APPENDIX',
        color: '#a082de'
      },
      {
        type: 'AMENDMENT_CHANGE',
        color: '#d789c6'
      },
      {
        type: 'AMENDMENT_PROLONGATION',
        color: '#bca4ed'
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

  approvalPercent(
    value: number
  ): number {

    if (this.dashboard.approvalCycles === 0) {
      return 0;
    }

    return Math.min(
      100,
      Math.round(
        (
          value /
          this.dashboard.approvalCycles
        ) * 100
      )
    );
  }


  riskPercent(
    value: number
  ): number {

    if (this.dashboard.riskAnalyzedDocuments === 0) {
      return 0;
    }

    return Math.min(
      100,
      Math.round(
        (
          value /
          this.dashboard.riskAnalyzedDocuments
        ) * 100
      )
    );
  }


  get riskScoreGradient(): string {

    const score =
      Math.max(
        0,
        Math.min(
          100,
          this.dashboard.averageRiskScore
        )
      );

    return (
      `conic-gradient(` +
      `#695bc4 0 ${score}%, ` +
      `#e7e2f5 ${score}% 100%)`
    );
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

      case 'APPROVED':
        return 'Согласован';
      case 'RETURNED_FOR_CORRECTION':
        return 'На доработке';
      case 'REJECTED':
        return 'Отклонён';
      case 'ARCHIVED':
        return 'В архиве';
      case 'READY_FOR_SED':
        return 'Готов к отправке';

      case 'SENT_TO_SED':
        return 'Отправлен в СЭД';

      case 'PROCESSING_FAILED':
        return 'Ошибка обработки';

      case 'SED_ERROR':
        return 'Ошибка СЭД';

      default:
        return status || 'Статус не указан';
    }
  }

  getStatusClass(
    status: string | null
  ): string {

    switch (status) {

      case 'APPROVED':
      case 'SENT_TO_SED':
      case 'VERIFIED':
      case 'READY_FOR_SED':
        return 'green';

      case 'LINK_REQUIRED':
      case 'RETURNED_FOR_CORRECTION':
        return 'orange';

      case 'LINKED':
      case 'UPLOADED':
      case 'AI_PROCESSING':
        return 'violet';

      case 'REJECTED':
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
  openDocuments(): void {
    void this.router.navigate(
      ['/documents']
    );
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