import {
  ChangeDetectorRef,
  Component,
  Input,
  OnInit
} from '@angular/core';

import {
  HttpErrorResponse
} from '@angular/common/http';

import {
  finalize
} from 'rxjs';

import {
  AiRiskAnalysis,
  AiRiskFinding,
  WorkspaceService
} from '../../../core/workspace/workspace.service';

@Component({
  selector: 'app-contract-risk-analysis',
  standalone: true,
  templateUrl: './contract-risk-analysis.html',
  styleUrl: './contract-risk-analysis.scss'
})
export class ContractRiskAnalysisComponent implements OnInit {

  @Input({ required: true })
  documentId!: number;

  @Input()
  canAnalyze = false;

  loading = false;

  loadingPrevious = false;

  result: AiRiskAnalysis | null = null;

  errorMessage = '';

  constructor(
    private readonly workspaceService: WorkspaceService,
    private readonly changeDetectorRef: ChangeDetectorRef
  ) {
  }

  ngOnInit(): void {
    this.loadLatest();
  }

  loadLatest(): void {

    if (
      !this.documentId
      ||
      this.loadingPrevious
    ) {
      return;
    }

    this.loadingPrevious = true;
    this.errorMessage = '';

    this.workspaceService
      .getLatestContractRiskAnalysis(
        this.documentId
      )
      .pipe(
        finalize(
          () => {

            this.loadingPrevious = false;

            this.changeDetectorRef
              .markForCheck();
          }
        )
      )
      .subscribe({

        next: result => {

          this.result = result;

          this.changeDetectorRef
            .markForCheck();
        },

        error: (
          error: HttpErrorResponse
        ) => {

          this.errorMessage =
            this.getErrorMessage(
              error
            );

          this.changeDetectorRef
            .markForCheck();
        }
      });
  }

  analyze(): void {

    if (
      !this.canAnalyze
      ||
      this.loading
    ) {
      return;
    }

    this.loading = true;
    this.errorMessage = '';

    this.workspaceService
      .analyzeContractRisks(
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

        next: result => {

          this.result = result;

          this.changeDetectorRef
            .markForCheck();
        },

        error: (
          error: HttpErrorResponse
        ) => {

          this.errorMessage =
            this.getErrorMessage(
              error
            );

          this.changeDetectorRef
            .markForCheck();
        }
      });
  }

  get riskLevelLabel(): string {

    switch (
      this.result?.riskLevel
    ) {

      case 'HIGH':
        return 'Высокий риск';

      case 'MEDIUM':
        return 'Средний риск';

      case 'LOW':
        return 'Низкий риск';

      default:
        return 'Не определён';
    }
  }

  get riskLevelClass(): string {

    switch (
      this.result?.riskLevel
    ) {

      case 'HIGH':
        return 'high';

      case 'MEDIUM':
        return 'medium';

      case 'LOW':
        return 'low';

      default:
        return 'neutral';
    }
  }

  getSeverityLabel(
    risk: AiRiskFinding
  ): string {

    switch (risk.severity) {

      case 'HIGH':
        return 'Высокая значимость';

      case 'MEDIUM':
        return 'Средняя значимость';

      case 'LOW':
        return 'Низкая значимость';

      default:
        return 'Значимость не определена';
    }
  }

  formatAnalyzedAt(
    value: string | null | undefined
  ): string {

    if (!value) {
      return '—';
    }

    const [
      datePart,
      timePart = ''
    ] = value.split('T');

    const [
      year,
      month,
      day
    ] = datePart.split('-');

    const [
      hour = '00',
      minute = '00'
    ] = timePart.split(':');

    if (
      !year
      ||
      !month
      ||
      !day
    ) {
      return value;
    }

    return `${day}.${month}.${year}, ${hour}:${minute}`;
  }

  private getErrorMessage(
    error: HttpErrorResponse
  ): string {

    if (error.status === 0) {
      return 'Не удалось подключиться к серверу DocFlow.';
    }

    if (error.status === 409) {
      return (
        this.extractBackendMessage(error)
        ?? 'Для договора отсутствует распознанный текст.'
      );
    }

    if (error.status === 400) {
      return (
        this.extractBackendMessage(error)
        ?? 'Договор нельзя проанализировать.'
      );
    }

    return (
      this.extractBackendMessage(error)
      ?? 'Не удалось выполнить AI-анализ рисков.'
    );
  }

  private extractBackendMessage(
    error: HttpErrorResponse
  ): string | null {

    if (
      typeof error.error === 'string'
      &&
      error.error.trim()
    ) {
      return error.error;
    }

    if (
      error.error
      &&
      typeof error.error === 'object'
    ) {

      const value =
        error.error as {
          message?: unknown;
          detail?: unknown;
        };

      if (
        typeof value.message === 'string'
      ) {
        return value.message;
      }

      if (
        typeof value.detail === 'string'
      ) {
        return value.detail;
      }
    }

    return null;
  }
}