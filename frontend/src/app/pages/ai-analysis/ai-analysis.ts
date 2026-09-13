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
  catchError,
  finalize,
  forkJoin,
  map,
  of,
  switchMap
} from 'rxjs';

import {
  DocumentSummary,
  WorkspaceService
} from '../../core/workspace/workspace.service';

interface AiAnalysisRow {
  document: DocumentSummary;
  createdAt: string | null;
}

@Component({
  selector: 'app-ai-analysis',
  standalone: true,
  imports: [
    FormsModule
  ],
  templateUrl: './ai-analysis.html',
  styleUrl: './ai-analysis.scss'
})
export class AiAnalysisWorkspace implements OnInit {

  rows: AiAnalysisRow[] = [];

  loading = true;
  errorMessage = '';

  searchQuery = '';
  typeFilter = 'ALL';
  confidenceFilter = 'ALL';

  readonly confidenceThreshold = 80;

  constructor(
    private readonly workspaceService: WorkspaceService,
    private readonly router: Router,
    private readonly changeDetectorRef: ChangeDetectorRef
  ) {
  }

  ngOnInit(): void {
    this.loadAnalysis();
  }

  loadAnalysis(): void {

    this.loading = true;
    this.errorMessage = '';

    this.workspaceService
      .getDocuments()
      .pipe(

        switchMap(
          documents => {

            if (documents.length === 0) {

              return of(
                [] as (AiAnalysisRow | null)[]
              );
            }

            return forkJoin(
              documents.map(
                workspaceDocument =>
                  this.workspaceService
                    .getDocumentSummary(
                      workspaceDocument.documentId
                    )
                    .pipe(

                      map(
                        document => ({
                          document,
                          createdAt:
                            workspaceDocument.createdAt
                        } as AiAnalysisRow)
                      ),

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

        next: rows => {

          this.rows =
            rows.filter(
              (
                row
              ): row is AiAnalysisRow =>
                row !== null
                &&
                row.document.ai !== null
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
              'Не удалось загрузить результаты AI-анализа.';
          }

          this.changeDetectorRef
            .markForCheck();
        }
      });
  }

  get filteredRows(): AiAnalysisRow[] {

    const query =
      this.searchQuery
        .trim()
        .toLowerCase();

    return this.rows.filter(
      row => {

        const document =
          row.document;

        const predictedType =
          document.ai?.predictedType
            || null;

        const confidence =
          this.getConfidencePercent(
            document
          );

        const matchesType =
          this.typeFilter === 'ALL'
          ||
          predictedType === this.typeFilter;

        if (!matchesType) {
          return false;
        }

        const matchesConfidence =
          this.confidenceFilter === 'ALL'
          ||
          (
            this.confidenceFilter === 'LOW'
            &&
            confidence < this.confidenceThreshold
          )
          ||
          (
            this.confidenceFilter === 'HIGH'
            &&
            confidence >= this.confidenceThreshold
          );

        if (!matchesConfidence) {
          return false;
        }

        if (!query) {
          return true;
        }

        const values = [
          document.originalName,
          document.documentNumber,
          document.subject,
          predictedType,
          this.getDocumentTypeLabel(
            predictedType
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

  get analyzedCount(): number {
    return this.rows.length;
  }

  get averageConfidence(): number {

    const values =
      this.rows
        .map(
          row =>
            this.getConfidencePercent(
              row.document
            )
        )
        .filter(
          value =>
            Number.isFinite(value)
        );

    if (values.length === 0) {
      return 0;
    }

    const total =
      values.reduce(
        (
          sum,
          value
        ) => sum + value,
        0
      );

    return total / values.length;
  }

  get lowConfidenceCount(): number {

    return this.rows.filter(
      row =>
        this.getConfidencePercent(
          row.document
        ) < this.confidenceThreshold
    ).length;
  }

  get reviewRequiredCount(): number {

    return this.rows.filter(
      row =>
        row.document.status
          === 'REVIEW_REQUIRED'
    ).length;
  }

  getTypeCount(
    type: string
  ): number {

    return this.rows.filter(
      row =>
        row.document.ai?.predictedType
          === type
    ).length;
  }

  getTypeShare(
    type: string
  ): number {

    if (this.analyzedCount === 0) {
      return 0;
    }

    return (
      this.getTypeCount(type)
      / this.analyzedCount
    ) * 100;
  }

  resetFilters(): void {

    this.searchQuery = '';
    this.typeFilter = 'ALL';
    this.confidenceFilter = 'ALL';
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

  getConfidenceLabel(
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

    return `${
      this.getConfidencePercent(document)
        .toFixed(2)
    }%`;
  }

  getConfidenceClass(
    document: DocumentSummary
  ): string {

    return this.getConfidencePercent(
      document
    ) < this.confidenceThreshold
      ? 'low'
      : 'high';
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

  getDocumentStatusClass(
    status: string | null
  ): string {

    switch (status) {

      case 'REVIEW_REQUIRED':
      case 'PROCESSING_FAILED':
      case 'SED_ERROR':
        return 'warning';

      case 'VERIFIED':
      case 'READY_FOR_SED':
      case 'SENT_TO_SED':
        return 'success';

      case 'LINK_REQUIRED':
        return 'link';

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
}