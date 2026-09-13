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

@Component({
  selector: 'app-sed',
  standalone: true,
  imports: [],
  templateUrl: './sed.html',
  styleUrl: './sed.scss'
})
export class SedWorkspace implements OnInit {

  readyDocuments: DocumentSummary[] = [];
  sentDocuments: DocumentSummary[] = [];

  loading = true;
  errorMessage = '';

  constructor(
    private readonly workspaceService: WorkspaceService,
    private readonly router: Router,
    private readonly changeDetectorRef: ChangeDetectorRef
  ) {
  }

  ngOnInit(): void {
    this.loadSedWorkspace();
  }

  loadSedWorkspace(): void {

    this.loading = true;
    this.errorMessage = '';

    forkJoin({
      ready:
        this.workspaceService.getDocuments(
          undefined,
          undefined,
          'READY_FOR_SED'
        ),

      sent:
        this.workspaceService.getDocuments(
          undefined,
          undefined,
          'SENT_TO_SED'
        )
    })
      .pipe(

        switchMap(
          result =>
            forkJoin({
              ready:
                this.loadSummaries(
                  result.ready
                ),

              sent:
                this.loadSummaries(
                  result.sent
                )
            })
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

        next: result => {

          this.readyDocuments =
            result.ready.filter(
              document =>
                document.status
                === 'READY_FOR_SED'
            );

          this.sentDocuments =
            result.sent.filter(
              document =>
                document.status
                === 'SENT_TO_SED'
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
              'Не удалось загрузить данные раздела СЭД.';
          }

          this.changeDetectorRef
            .markForCheck();
        }
      });
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

  get totalDocuments(): number {

    return this.readyDocuments.length
      + this.sentDocuments.length;
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
        return target || '—';
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
        .exec(
          datePart
        );

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
      this.formatDate(
        value
      );

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