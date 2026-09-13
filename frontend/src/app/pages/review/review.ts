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
  catchError,
  finalize,
  forkJoin,
  of,
  switchMap
} from 'rxjs';

import {
  DocumentSummary,
  WorkspaceService
} from '../../core/workspace/workspace.service';

@Component({
  selector: 'app-review',
  standalone: true,
  imports: [],
  templateUrl: './review.html',
  styleUrl: './review.scss'
})
export class ReviewQueue implements OnInit {

  documents: DocumentSummary[] = [];

  loading = true;
  errorMessage = '';

  constructor(
    private readonly workspaceService: WorkspaceService,
    private readonly router: Router,
    private readonly changeDetectorRef: ChangeDetectorRef
  ) {
  }

  ngOnInit(): void {
    this.loadQueue();
  }

  loadQueue(): void {

    this.loading = true;
    this.errorMessage = '';

    this.workspaceService
      .getDocuments(
        undefined,
        undefined,
        'REVIEW_REQUIRED'
      )
      .pipe(

        switchMap(
          documents => {

            if (documents.length === 0) {

              return of(
                [] as (DocumentSummary | null)[]
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
            );
          }
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

        next: summaries => {

          this.documents =
            summaries.filter(
              (
                document
              ): document is DocumentSummary =>
                document !== null
                &&
                document.status === 'REVIEW_REQUIRED'
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
              'Не удалось загрузить очередь ручной проверки.';
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

  getConfidence(
    document: DocumentSummary
  ): string {

    const confidence =
      document.ai?.confidence;

    if (
      confidence === null
      ||
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

  getConfidencePercent(
    document: DocumentSummary
  ): number {

    const confidence =
      document.ai?.confidence;

    if (
      confidence === null
      ||
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
}