import {
  ChangeDetectorRef,
  Component,
  Input,
  OnChanges,
  SimpleChanges
} from '@angular/core';

import {
  finalize
} from 'rxjs';

import {
  ApprovalService,
  ApprovalStep
} from '../../../core/approval/approval.service';


interface LifecycleEvent {

  id: string;

  title: string;

  description: string | null;

  person: string | null;

  timestamp: string | null;

  tone:
    | 'neutral'
    | 'active'
    | 'success'
    | 'warning'
    | 'danger';
}


interface LifecycleVersionGroup {

  versionNumber: number | null;

  stepCount: number;

  events: LifecycleEvent[];
}


@Component({
  selector: 'app-document-lifecycle-timeline',
  standalone: true,
  imports: [],
  templateUrl:
    './document-lifecycle-timeline.html',
  styleUrl:
    './document-lifecycle-timeline.scss'
})
export class DocumentLifecycleTimelineComponent
  implements OnChanges {

  @Input()
  documentId:
    number | null = null;

  steps:
    ApprovalStep[] = [];

  groups:
    LifecycleVersionGroup[] = [];

  loading = false;

  errorMessage = '';


  constructor(
    private readonly approvalService:
      ApprovalService,

    private readonly changeDetectorRef:
      ChangeDetectorRef
  ) {
  }


  ngOnChanges(
    changes: SimpleChanges
  ): void {

    if (
      changes['documentId']
    ) {

      this.loadLifecycle();
    }
  }


  private loadLifecycle(): void {

    if (
      this.documentId === null
      ||
      this.loading
    ) {
      return;
    }

    this.loading = true;

    this.errorMessage = '';

    this.approvalService
      .getDocumentSteps(
        this.documentId
      )
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

        next: steps => {

          this.steps =
            [...steps]
              .sort(
                (
                  left,
                  right
                ) =>
                  left.stepOrder
                  - right.stepOrder
              );

          this.groups =
            this.buildGroups(
              this.steps
            );

          this.changeDetectorRef
            .markForCheck();
        },

        error: () => {

          this.steps = [];

          this.groups = [];

          this.errorMessage =
            'Не удалось загрузить историю жизненного цикла документа.';

          this.changeDetectorRef
            .markForCheck();
        }
      });
  }


  private buildGroups(
    steps: ApprovalStep[]
  ): LifecycleVersionGroup[] {

    const groupMap =
      new Map<
        string,
        LifecycleVersionGroup
      >();

    for (
      const step of steps
    ) {

      const versionNumber =
        step.documentVersionNumber
        ?? null;

      const key =
        versionNumber === null
          ? 'without-version'
          : `v-${versionNumber}`;

      let group =
        groupMap.get(
          key
        );

      if (!group) {

        group = {
          versionNumber,
          stepCount: 0,
          events: []
        };

        groupMap.set(
          key,
          group
        );
      }

      group.stepCount++;

      group.events.push(
        ...this.buildStepEvents(
          step
        )
      );
    }

    return Array.from(
      groupMap.values()
    )
      .map(
        group => ({
          ...group,

          events:
            [...group.events]
              .sort(
                (
                  left,
                  right
                ) =>
                  this.toTimestamp(
                    left.timestamp
                  )
                  -
                  this.toTimestamp(
                    right.timestamp
                  )
              )
        })
      )
      .sort(
        (
          left,
          right
        ) => {

          if (
            left.versionNumber === null
          ) {
            return 1;
          }

          if (
            right.versionNumber === null
          ) {
            return -1;
          }

          return (
            left.versionNumber
            -
            right.versionNumber
          );
        }
      );
  }


  private buildStepEvents(
    step: ApprovalStep
  ): LifecycleEvent[] {

    const events:
      LifecycleEvent[] = [];

    const versionLabel =
      step.documentVersionNumber === null
        ? 'Документ'
        : `Версия v${step.documentVersionNumber}`;

    events.push({
      id:
        `step-${step.stepId}-created`,

      title:
        `${versionLabel} направлена на согласование`,

      description:
        this.buildAssignmentDescription(
          step
        ),

      person: null,

      timestamp:
        step.createdAt,

      tone:
        step.status === 'CANCELLED'
          ? 'neutral'
          : 'active'
    });


    if (
      step.startedAt
    ) {

      events.push({
        id:
          `step-${step.stepId}-started`,

        title:
          'Документ взят в работу',

        description:
          this.getRoleLabel(
            step.roleCode
          ),

        person:
          step.assignedUserFullName,

        timestamp:
          step.startedAt,

        tone:
          'active'
      });
    }


    if (
      step.completedAt
    ) {

      events.push(
        this.buildCompletedEvent(
          step,
          versionLabel
        )
      );
    }

    return events;
  }


  private buildCompletedEvent(
    step: ApprovalStep,
    versionLabel: string
  ): LifecycleEvent {

    let title =
      'Этап согласования завершён';

    let tone:
      LifecycleEvent['tone'] =
      'neutral';

    switch (
      step.status
    ) {

      case 'APPROVED':

        title =
          `${versionLabel} согласована`;

        tone =
          'success';

        break;


      case 'RETURNED':

        title =
          `${versionLabel} возвращена на доработку`;

        tone =
          'warning';

        break;


      case 'REJECTED':

        title =
          `${versionLabel} отклонена`;

        tone =
          'danger';

        break;


      case 'CANCELLED':

        title =
          'Этап согласования отменён';

        tone =
          'neutral';

        break;
    }

    return {
      id:
        `step-${step.stepId}-completed`,

      title,

      description:
        step.comment?.trim()
        || null,

      person:
        step.completedByUserFullName
        || step.assignedUserFullName
        || null,

      timestamp:
        step.completedAt,

      tone
    };
  }


  private buildAssignmentDescription(
    step: ApprovalStep
  ): string {

    const role =
      this.getRoleLabel(
        step.roleCode
      );

    const reason =
      step.routingReason
        ?.trim();

    if (reason) {

      return `${role}. ${reason}`;
    }

    return role;
  }


  get versionCount(): number {

    return this.groups.filter(
      group =>
        group.versionNumber !== null
    ).length;
  }


  get approvalCycleCount(): number {

    return this.steps.length;
  }


  get completedCount(): number {

    return this.steps.filter(
      step =>
        step.status === 'APPROVED'
        ||
        step.status === 'RETURNED'
        ||
        step.status === 'REJECTED'
        ||
        step.status === 'CANCELLED'
    ).length;
  }


  get latestStatusLabel(): string {

    if (
      this.steps.length === 0
    ) {
      return 'Нет этапов';
    }

    const latest =
      this.steps[
        this.steps.length - 1
      ];

    return this.getStatusLabel(
      latest.status
    );
  }


  get latestVersionNumber():
    number | null {

    const numbers =
      this.steps
        .map(
          step =>
            step.documentVersionNumber
        )
        .filter(
          (
            value
          ): value is number =>
            value !== null
        );

    if (
      numbers.length === 0
    ) {
      return null;
    }

    return Math.max(
      ...numbers
    );
  }


  isLatestVersion(
    versionNumber:
      number | null
  ): boolean {

    return (
      versionNumber !== null
      &&
      versionNumber
        === this.latestVersionNumber
    );
  }


  getVersionLabel(
    versionNumber:
      number | null
  ): string {

    if (
      versionNumber === null
    ) {
      return 'Без версии';
    }

    return `v${versionNumber}`;
  }


  getRoleLabel(
    roleCode:
      string | null
  ): string {

    switch (
      roleCode
    ) {

      case 'LAWYER':
        return 'Юрист';

      case 'CONTRACT_MANAGER':
        return 'Менеджер по договорам';

      case 'DOCUMENT_OPERATOR':
        return 'Делопроизводитель';

      case 'ADMIN':
        return 'Администратор';

      default:
        return roleCode
          || 'Специалист';
    }
  }


  getStatusLabel(
    status:
      string | null
  ): string {

    switch (
      status
    ) {

      case 'PENDING':
        return 'Ожидает специалиста';

      case 'IN_PROGRESS':
        return 'В работе';

      case 'APPROVED':
        return 'Согласовано';

      case 'RETURNED':
        return 'На доработке';

      case 'REJECTED':
        return 'Отклонено';

      case 'CANCELLED':
        return 'Отменено';

      default:
        return status
          || 'Нет данных';
    }
  }


  formatDateTime(
    value:
      string | null
  ): string {

    if (!value) {
      return '—';
    }

    const date =
      new Date(
        value
      );

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
    ).format(
      date
    );
  }


  private toTimestamp(
    value:
      string | null
  ): number {

    if (!value) {
      return 0;
    }

    const date =
      new Date(
        value
      );

    const timestamp =
      date.getTime();

    return Number.isNaN(
      timestamp
    )
      ? 0
      : timestamp;
  }
}