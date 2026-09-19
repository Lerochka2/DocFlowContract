import {
  ChangeDetectorRef,
  Component,
  OnDestroy,
  OnInit
} from '@angular/core';

import {
  FormsModule
} from '@angular/forms';

import {
  HttpClient,
  HttpErrorResponse
} from '@angular/common/http';

import {
  ActivatedRoute,
  Router
} from '@angular/router';

import {
  DomSanitizer,
  SafeResourceUrl
} from '@angular/platform-browser';

import {
  finalize
} from 'rxjs';

import {
  ContractCandidate,
  DocumentAuditEventResponse,
  DocumentCommentResponse,
  DocumentDecisionCode,
  DocumentDecisionResponse,
  DocumentMetadata,
  DocumentMetadataRequest,
  DocumentSummary,
  DocumentTypeCode,
  WorkspaceService
} from '../../core/workspace/workspace.service';

import {
  AuthService
} from '../../core/auth/auth.service';

import {
  DocumentAuditTimelineComponent
} from './document-audit-timeline/document-audit-timeline';

import {
  ContractExpirationCardComponent
} from './contract-expiration-card/contract-expiration-card';

import {
  ContractRiskAnalysisComponent
} from './contract-risk-analysis/contract-risk-analysis';

@Component({
  selector: 'app-document-details',
  standalone: true,
  imports: [
    FormsModule,
    DocumentAuditTimelineComponent,
    ContractExpirationCardComponent,
    ContractRiskAnalysisComponent
  ],
  templateUrl: './document-details.html',
  styleUrl: './document-details.scss'
})
export class DocumentDetails implements OnInit, OnDestroy {

  document: DocumentSummary | null = null;

  metadataForm: DocumentMetadataRequest | null = null;

  contractCandidates: ContractCandidate[] = [];
  selectedContractDocumentId: number | null = null;

  selectedReviewDocumentType:
    DocumentTypeCode | null = null;

  loading = true;
  deleting = false;

  editingMetadata = false;
  loadingMetadata = false;
  savingMetadata = false;

  reviewingDocument = false;

  loadingCandidates = false;
  linkingDocument = false;

  preparingForSed = false;
  sendingToSed = false;

  decisionHistory: DocumentDecisionResponse[] = [];
  decisionComment = '';

  loadingDecisionHistory = false;
  decidingDocument = false;
  resubmittingDocument = false;

  decisionErrorMessage = '';
  decisionSuccessMessage = '';

  documentComments: DocumentCommentResponse[] = [];
  newComment = '';

  loadingComments = false;
  submittingComment = false;

  commentErrorMessage = '';
  commentSuccessMessage = '';

  auditHistory: DocumentAuditEventResponse[] = [];
  loadingAudit = false;
  auditErrorMessage = '';

  previewLoading = false;
  previewErrorMessage = '';
  previewObjectUrl: string | null = null;
  previewResourceUrl: SafeResourceUrl | null = null;
  previewMimeType = '';

  errorMessage = '';
  deleteErrorMessage = '';

  metadataErrorMessage = '';
  metadataSuccessMessage = '';

  reviewErrorMessage = '';
  reviewSuccessMessage = '';

  linkErrorMessage = '';
  linkSuccessMessage = '';

  sedErrorMessage = '';
  sedSuccessMessage = '';

  private documentId: number | null = null;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly workspaceService: WorkspaceService,
    private readonly authService: AuthService,
    private readonly http: HttpClient,
    private readonly sanitizer: DomSanitizer,
    private readonly changeDetectorRef: ChangeDetectorRef
  ) {
  }

  ngOnInit(): void {

    const rawId =
      this.route.snapshot.paramMap.get('id');

    const parsedId =
      Number(rawId);

    if (
      !rawId ||
      !Number.isInteger(parsedId) ||
      parsedId <= 0
    ) {

      this.loading = false;

      this.errorMessage =
        'Некорректный идентификатор документа.';

      return;
    }

    this.documentId =
      parsedId;

    this.loadDocument();
  }

  ngOnDestroy(): void {
    this.releasePreviewUrl();
  }

  loadDocument(): void {

    if (this.documentId === null) {
      return;
    }

    this.loading = true;
    this.errorMessage = '';
    this.deleteErrorMessage = '';

    this.workspaceService
      .getDocumentSummary(
        this.documentId
      )
      .subscribe({

        next: document => {

          this.document =
            document;

          this.loading = false;

          this.updateReviewState(
            document
          );

          this.updateLinkCandidates(
            document
          );

          this.loadDocumentPreview();
          this.loadDecisionHistory();
          this.loadDocumentComments();
          this.loadDocumentAudit();

          this.changeDetectorRef
            .markForCheck();
        },

        error: (
          error: HttpErrorResponse
        ) => {

          this.loading = false;

          if (error.status === 404) {

            this.errorMessage =
              'Документ не найден.';

          } else if (error.status === 0) {

            this.errorMessage =
              'Не удалось подключиться к серверу DocFlow.';

          } else {

            this.errorMessage =
              'Не удалось загрузить данные документа.';
          }

          this.changeDetectorRef
            .markForCheck();
        }
      });
  }

  loadDocumentPreview(): void {

    if (
      this.documentId === null
      ||
      this.previewLoading
    ) {
      return;
    }

    this.releasePreviewUrl();

    this.previewLoading = true;
    this.previewErrorMessage = '';
    this.previewMimeType = '';

    this.http
      .get(
        `http://localhost:8080/api/documents/${this.documentId}/file`,
        {
          responseType: 'blob'
        }
      )
      .pipe(
        finalize(
          () => {

            this.previewLoading = false;

            this.changeDetectorRef
              .markForCheck();
          }
        )
      )
      .subscribe({

        next: blob => {

          let mimeType =
            blob.type;

          if (
            !mimeType
            ||
            mimeType
              === 'application/octet-stream'
          ) {

            mimeType =
              this.inferMimeTypeFromName(
                this.document?.originalName
              );
          }

          this.previewMimeType =
            mimeType;

          const objectUrl =
            URL.createObjectURL(
              blob
            );

          this.previewObjectUrl =
            objectUrl;

          if (
            mimeType === 'application/pdf'
          ) {

            this.previewResourceUrl =
              this.sanitizer
                .bypassSecurityTrustResourceUrl(
                  objectUrl
                );

          } else {

            this.previewResourceUrl =
              null;
          }

          this.changeDetectorRef
            .markForCheck();
        },

        error: (
          error: HttpErrorResponse
        ) => {

          if (error.status === 404) {

            this.previewErrorMessage =
              'Исходный файл документа не найден.';

          } else if (error.status === 0) {

            this.previewErrorMessage =
              'Не удалось подключиться к серверу для просмотра файла.';

          } else {

            this.previewErrorMessage =
              'Не удалось загрузить предпросмотр документа.';
          }

          this.changeDetectorRef
            .markForCheck();
        }
      });
  }

  get isPdfPreview(): boolean {

    return this.previewMimeType
      === 'application/pdf';
  }

  get isImagePreview(): boolean {

    return this.previewMimeType
      .startsWith(
        'image/'
      );
  }

  openPreviewInNewTab(): void {

    if (!this.previewObjectUrl) {
      return;
    }

    window.open(
      this.previewObjectUrl,
      '_blank',
      'noopener,noreferrer'
    );
  }

  private releasePreviewUrl(): void {

    if (
      this.previewObjectUrl
    ) {

      URL.revokeObjectURL(
        this.previewObjectUrl
      );
    }

    this.previewObjectUrl = null;
    this.previewResourceUrl = null;
  }

  private inferMimeTypeFromName(
    fileName: string | null | undefined
  ): string {

    if (!fileName) {
      return '';
    }

    const normalized =
      fileName.toLowerCase();

    if (
      normalized.endsWith('.pdf')
    ) {
      return 'application/pdf';
    }

    if (
      normalized.endsWith('.jpg')
      ||
      normalized.endsWith('.jpeg')
    ) {
      return 'image/jpeg';
    }

    if (
      normalized.endsWith('.png')
    ) {
      return 'image/png';
    }

    return '';
  }

  private refreshDocumentSummary(): void {

    if (this.documentId === null) {
      return;
    }

    this.workspaceService
      .getDocumentSummary(
        this.documentId
      )
      .subscribe({

        next: document => {

          this.document =
            document;

          this.updateReviewState(
            document
          );

          this.updateLinkCandidates(
            document
          );

          this.loadDecisionHistory();
          this.loadDocumentComments();
          this.loadDocumentAudit();

          this.changeDetectorRef
            .markForCheck();
        },

        error: () => {
          this.loadDocument();
        }
      });
  }

  private updateReviewState(
    document: DocumentSummary
  ): void {

    if (
      document.status
      !== 'REVIEW_REQUIRED'
    ) {

      this.selectedReviewDocumentType =
        null;

      return;
    }

    if (
      this.isDocumentTypeCode(
        document.ai?.predictedType
      )
    ) {

      this.selectedReviewDocumentType =
        document.ai!.predictedType as DocumentTypeCode;

      return;
    }

    if (
      this.isDocumentTypeCode(
        document.documentType
      )
    ) {

      this.selectedReviewDocumentType =
        document.documentType;

      return;
    }

    this.selectedReviewDocumentType =
      null;
  }

  private get currentRole(): string | null {

    return this.authService
      .getCurrentUser()
      ?.role
      ?? null;
  }


  get isAdminUser(): boolean {

    return this.currentRole
      === 'ADMIN';
  }


  get isOperatorUser(): boolean {

    return this.currentRole
      === 'DOCUMENT_OPERATOR';
  }


  get isSpecialistUser(): boolean {

    return (
      this.currentRole === 'LAWYER'
      ||
      this.currentRole === 'CONTRACT_MANAGER'
    );
  }


  get canReviewByRole(): boolean {

    return (
      this.isAdminUser
      ||
      this.isOperatorUser
    );
  }


  get canLinkByRole(): boolean {

    return (
      this.isAdminUser
      ||
      this.isOperatorUser
    );
  }


  get canEditMetadataByRole(): boolean {

    if (
      this.isAdminUser
      ||
      this.isSpecialistUser
    ) {
      return true;
    }

    return (
      this.isOperatorUser
      &&
      this.document?.status
        === 'RETURNED_FOR_CORRECTION'
    );
  }


  get canPrepareForSedByRole(): boolean {

    return (
      this.isAdminUser
      ||
      this.isSpecialistUser
    );
  }


  get canSendToSedByRole(): boolean {

    return (
      this.isAdminUser
      ||
      this.isOperatorUser
    );
  }


  get canDeleteByRole(): boolean {

    return (
      this.isAdminUser
      ||
      this.isOperatorUser
    );
  }


  get canLinkDocument(): boolean {

    return (
      this.canLinkByRole
      &&
      this.document?.status === 'LINK_REQUIRED'
      &&
      !this.document?.link
    );
  }

  reviewDocument(): void {

    if (
      this.documentId === null
      ||
      !this.canReviewDocument
      ||
      this.selectedReviewDocumentType === null
      ||
      this.reviewingDocument
    ) {
      return;
    }

    const selectedTypeLabel =
      this.getDocumentTypeLabel(
        this.selectedReviewDocumentType
      );

    const confirmed =
      window.confirm(
        `Подтвердить тип документа «${selectedTypeLabel}»?`
      );

    if (!confirmed) {
      return;
    }

    this.reviewingDocument = true;

    this.reviewErrorMessage = '';
    this.reviewSuccessMessage = '';

    this.workspaceService
      .reviewDocument(
        this.documentId,
        this.selectedReviewDocumentType
      )
      .pipe(
        finalize(
          () => {

            this.reviewingDocument = false;

            this.changeDetectorRef
              .markForCheck();
          }
        )
      )
      .subscribe({

        next: response => {

          if (
            response.documentType
            === 'CONTRACT'
          ) {

            this.reviewSuccessMessage =
              'Тип документа подтверждён. Договор проверен и маршрутизирован.';

          } else {

            this.reviewSuccessMessage =
              'Тип документа подтверждён. Теперь необходимо выбрать основной договор.';
          }

          this.refreshDocumentSummary();
        },

        error: (
          error: HttpErrorResponse
        ) => {

          this.reviewErrorMessage =
            this.extractBackendMessage(
              error
            )
            ?? 'Не удалось подтвердить тип документа.';

          this.changeDetectorRef
            .markForCheck();
        }
      });
  }

  get canReviewDocument(): boolean {

    return (
      this.canReviewByRole
      &&
      this.document?.status
        === 'REVIEW_REQUIRED'
    );
  }

  private isDocumentTypeCode(
    value: string | null | undefined
  ): value is DocumentTypeCode {

    return value === 'CONTRACT'
      || value === 'AMENDMENT_CHANGE'
      || value === 'AMENDMENT_PROLONGATION'
      || value === 'APPENDIX';
  }

  private updateLinkCandidates(
    document: DocumentSummary
  ): void {

    if (
      document.status === 'LINK_REQUIRED'
      &&
      !document.link
    ) {

      this.loadContractCandidates();

      return;
    }

    this.contractCandidates = [];
    this.selectedContractDocumentId = null;
  }

  loadContractCandidates(): void {

    if (
      this.documentId === null
      ||
      !this.canLinkDocument
      ||
      this.loadingCandidates
    ) {
      return;
    }

    this.loadingCandidates = true;
    this.linkErrorMessage = '';

    this.workspaceService
      .getLinkCandidates(
        this.documentId
      )
      .pipe(
        finalize(
          () => {

            this.loadingCandidates = false;

            this.changeDetectorRef
              .markForCheck();
          }
        )
      )
      .subscribe({

        next: candidates => {

          this.contractCandidates =
            candidates;

          if (
            candidates.length === 1
          ) {

            this.selectedContractDocumentId =
              candidates[0].contractDocumentId;
          }

          this.changeDetectorRef
            .markForCheck();
        },

        error: (
          error: HttpErrorResponse
        ) => {

          this.contractCandidates = [];

          this.linkErrorMessage =
            this.extractBackendMessage(
              error
            )
            ?? 'Не удалось получить список доступных договоров.';

          this.changeDetectorRef
            .markForCheck();
        }
      });
  }

  linkToContract(): void {

    if (
      this.documentId === null
      ||
      !this.canLinkDocument
      ||
      this.selectedContractDocumentId === null
      ||
      this.linkingDocument
    ) {
      return;
    }

    const candidate =
      this.selectedContractCandidate;

    const contractLabel =
      candidate?.documentNumber
        ? `договором № ${candidate.documentNumber}`
        : `договором ID ${this.selectedContractDocumentId}`;

    const confirmed =
      window.confirm(
        `Привязать документ к ${contractLabel}?`
      );

    if (!confirmed) {
      return;
    }

    this.linkingDocument = true;
    this.linkErrorMessage = '';
    this.linkSuccessMessage = '';

    this.workspaceService
      .linkDocument(
        this.documentId,
        this.selectedContractDocumentId
      )
      .pipe(
        finalize(
          () => {

            this.linkingDocument = false;

            this.changeDetectorRef
              .markForCheck();
          }
        )
      )
      .subscribe({

        next: () => {

          this.linkSuccessMessage =
            'Документ успешно привязан к основному договору.';

          this.refreshDocumentSummary();
        },

        error: (
          error: HttpErrorResponse
        ) => {

          this.linkErrorMessage =
            this.extractBackendMessage(
              error
            )
            ?? 'Не удалось привязать документ к договору.';

          this.changeDetectorRef
            .markForCheck();
        }
      });
  }

  loadDocumentComments(): void {

    if (
      this.documentId === null
      ||
      this.loadingComments
    ) {
      return;
    }

    this.loadingComments = true;

    this.workspaceService
      .getDocumentComments(
        this.documentId
      )
      .pipe(
        finalize(
          () => {

            this.loadingComments = false;

            this.changeDetectorRef
              .markForCheck();
          }
        )
      )
      .subscribe({

        next: comments => {

          this.documentComments =
            comments;

          this.changeDetectorRef
            .markForCheck();
        },

        error: (
          error: HttpErrorResponse
        ) => {

          this.documentComments = [];

          this.commentErrorMessage =
            this.extractBackendMessage(
              error
            )
            ?? 'Не удалось загрузить комментарии.';

          this.changeDetectorRef
            .markForCheck();
        }
      });
  }


  loadDocumentAudit(): void {

    if (
      this.documentId === null
      ||
      this.loadingAudit
    ) {
      return;
    }

    this.loadingAudit = true;
    this.auditErrorMessage = '';

    this.workspaceService
      .getDocumentAudit(
        this.documentId
      )
      .pipe(
        finalize(
          () => {

            this.loadingAudit = false;

            this.changeDetectorRef
              .markForCheck();
          }
        )
      )
      .subscribe({

        next: history => {

          this.auditHistory =
            history;

          this.changeDetectorRef
            .markForCheck();
        },

        error: (
          error: HttpErrorResponse
        ) => {

          this.auditHistory = [];

          this.auditErrorMessage =
            this.extractBackendMessage(
              error
            )
            ?? 'Не удалось загрузить историю действий.';

          this.changeDetectorRef
            .markForCheck();
        }
      });
  }


  getAuditEventLabel(
    eventType: string
  ): string {

    switch (eventType) {

      case 'DOCUMENT_UPLOADED':
        return 'Документ загружен';

      case 'AI_ANALYSIS_STARTED':
        return 'AI-анализ запущен';

      case 'AI_ANALYSIS_COMPLETED':
        return 'AI-анализ завершён';

      case 'AI_ANALYSIS_FAILED':
        return 'Ошибка AI-анализа';

      case 'CLASSIFICATION_CONFIRMED':
        return 'Классификация подтверждена';

      case 'CLASSIFICATION_CHANGED':
        return 'Классификация изменена';

      case 'METADATA_UPDATED':
        return 'Реквизиты изменены';

      case 'DOCUMENT_LINKED':
        return 'Привязан к договору';

      case 'SPECIALIST_APPROVED':
        return 'Документ согласован';

      case 'RETURNED_FOR_CORRECTION':
        return 'Возвращён на доработку';

      case 'REJECTED':
        return 'Документ отклонён';

      case 'RESUBMITTED_FOR_REVIEW':
        return 'Повторно направлен на проверку';

      case 'COMMENT_ADDED':
        return 'Добавлен комментарий';

      case 'PREPARED_FOR_SED':
        return 'Подготовлен к СЭД';

      case 'SENT_TO_SED':
        return 'Отправлен в СЭД';

      case 'SED_SEND_FAILED':
        return 'Ошибка отправки в СЭД';

      case 'DOCUMENT_ARCHIVED':
        return 'Документ архивирован';

      case 'DOCUMENT_RESTORED':
        return 'Документ восстановлен';

      default:
        return eventType;
    }
  }


  getAuditEventClass(
    eventType: string
  ): string {

    switch (eventType) {

      case 'AI_ANALYSIS_STARTED':
      case 'AI_ANALYSIS_COMPLETED':
        return 'ai';

      case 'DOCUMENT_LINKED':
      case 'CLASSIFICATION_CONFIRMED':
      case 'SPECIALIST_APPROVED':
      case 'PREPARED_FOR_SED':
      case 'SENT_TO_SED':
      case 'DOCUMENT_RESTORED':
        return 'success';

      case 'RETURNED_FOR_CORRECTION':
      case 'RESUBMITTED_FOR_REVIEW':
        return 'warning';

      case 'AI_ANALYSIS_FAILED':
      case 'REJECTED':
      case 'SED_SEND_FAILED':
        return 'danger';

      default:
        return 'neutral';
    }
  }


  getAuditDetailsLines(
    details: string | null
  ): string[] {

    if (!details) {
      return [];
    }

    return details
      .split(/\r?\n/)
      .map(
        line => line.trim()
      )
      .filter(
        line => Boolean(line)
      );
  }


  getAuditActorLabel(
    event: DocumentAuditEventResponse
  ): string {

    if (
      event.actorType === 'SYSTEM'
    ) {
      return event.actorName || 'Система DocFlow';
    }

    return event.actorName || 'Пользователь';
  }


  addComment(): void {

    if (
      this.documentId === null
      ||
      this.submittingComment
    ) {
      return;
    }

    const normalizedComment =
      this.newComment.trim();

    if (!normalizedComment) {

      this.commentErrorMessage =
        'Введите текст комментария.';

      return;
    }

    if (
      normalizedComment.length > 2000
    ) {

      this.commentErrorMessage =
        'Комментарий не должен превышать 2000 символов.';

      return;
    }

    this.submittingComment = true;

    this.commentErrorMessage = '';
    this.commentSuccessMessage = '';

    this.workspaceService
      .addDocumentComment(
        this.documentId,
        normalizedComment
      )
      .pipe(
        finalize(
          () => {

            this.submittingComment = false;

            this.changeDetectorRef
              .markForCheck();
          }
        )
      )
      .subscribe({

        next: comment => {

          this.documentComments = [
            ...this.documentComments,
            comment
          ];

          this.newComment = '';

          this.commentSuccessMessage =
            'Комментарий добавлен.';

          this.loadDocumentAudit();

          this.changeDetectorRef
            .markForCheck();
        },

        error: (
          error: HttpErrorResponse
        ) => {

          this.commentErrorMessage =
            this.extractBackendMessage(
              error
            )
            ?? 'Не удалось добавить комментарий.';

          this.changeDetectorRef
            .markForCheck();
        }
      });
  }

  loadDecisionHistory(): void {

    if (
      this.documentId === null
      ||
      this.loadingDecisionHistory
    ) {
      return;
    }

    this.loadingDecisionHistory = true;

    this.workspaceService
      .getDocumentDecisions(
        this.documentId
      )
      .pipe(
        finalize(
          () => {

            this.loadingDecisionHistory = false;

            this.changeDetectorRef
              .markForCheck();
          }
        )
      )
      .subscribe({

        next: history => {

          this.decisionHistory =
            history;

          this.changeDetectorRef
            .markForCheck();
        },

        error: () => {

          this.decisionHistory = [];

          this.changeDetectorRef
            .markForCheck();
        }
      });
  }

  get canMakeDocumentDecision(): boolean {

    return (
      (
        this.isAdminUser
        ||
        this.isSpecialistUser
      )
      &&
      (
        this.document?.status === 'VERIFIED'
        ||
        this.document?.status === 'LINKED'
      )
    );
  }


  submitDocumentDecision(
    decision: DocumentDecisionCode
  ): void {

    if (
      this.documentId === null
      ||
      !this.canMakeDocumentDecision
      ||
      this.decidingDocument
    ) {
      return;
    }

    const normalizedComment =
      this.decisionComment.trim();

    if (
      (
        decision === 'RETURN_FOR_CORRECTION'
        ||
        decision === 'REJECT'
      )
      &&
      !normalizedComment
    ) {

      this.decisionErrorMessage =
        'Укажите комментарий с причиной возврата или отклонения.';

      return;
    }

    let confirmationMessage =
      'Согласовать документ?';

    if (
      decision === 'RETURN_FOR_CORRECTION'
    ) {

      confirmationMessage =
        'Вернуть документ делопроизводителю на доработку?';

    } else if (
      decision === 'REJECT'
    ) {

      confirmationMessage =
        'Отклонить документ? Документ не сможет продолжить текущий процесс обработки.';
    }

    const confirmed =
      window.confirm(
        confirmationMessage
      );

    if (!confirmed) {
      return;
    }

    this.decidingDocument = true;

    this.decisionErrorMessage = '';
    this.decisionSuccessMessage = '';

    this.workspaceService
      .decideDocument(
        this.documentId,
        decision,
        normalizedComment || null
      )
      .pipe(
        finalize(
          () => {

            this.decidingDocument = false;

            this.changeDetectorRef
              .markForCheck();
          }
        )
      )
      .subscribe({

        next: () => {

          if (
            decision === 'APPROVE'
          ) {

            this.decisionSuccessMessage =
              'Документ согласован специалистом.';

          } else if (
            decision === 'RETURN_FOR_CORRECTION'
          ) {

            this.decisionSuccessMessage =
              'Документ возвращён на доработку.';

          } else {

            this.decisionSuccessMessage =
              'Документ отклонён.';
          }

          this.decisionComment = '';

          this.editingMetadata = false;
          this.metadataForm = null;

          this.refreshDocumentSummary();
        },

        error: (
          error: HttpErrorResponse
        ) => {

          this.decisionErrorMessage =
            this.extractBackendMessage(
              error
            )
            ?? 'Не удалось сохранить решение по документу.';

          this.changeDetectorRef
            .markForCheck();
        }
      });
  }


  getDecisionLabel(
    decision: string
  ): string {

    switch (decision) {

      case 'APPROVE':
        return 'Согласовано';

      case 'RETURN_FOR_CORRECTION':
        return 'Возвращено на доработку';

      case 'REJECT':
        return 'Отклонено';

      default:
        return decision;
    }
  }

  get canResubmitForReview(): boolean {

    return (
      (
        this.isAdminUser
        ||
        this.isOperatorUser
      )
      &&
      this.document?.status
        === 'RETURNED_FOR_CORRECTION'
    );
  }


  resubmitForReview(): void {

    if (
      this.documentId === null
      ||
      !this.canResubmitForReview
      ||
      this.resubmittingDocument
    ) {
      return;
    }

    const confirmed =
      window.confirm(
        'Повторно направить исправленный документ специалисту на проверку?'
      );

    if (!confirmed) {
      return;
    }

    this.resubmittingDocument = true;

    this.decisionErrorMessage = '';
    this.decisionSuccessMessage = '';

    this.workspaceService
      .resubmitDocumentForReview(
        this.documentId
      )
      .pipe(
        finalize(
          () => {

            this.resubmittingDocument = false;

            this.changeDetectorRef
              .markForCheck();
          }
        )
      )
      .subscribe({

        next: () => {

          this.decisionSuccessMessage =
            'Документ повторно направлен специалисту на проверку.';

          this.editingMetadata = false;
          this.metadataForm = null;

          this.refreshDocumentSummary();
        },

        error: (
          error: HttpErrorResponse
        ) => {

          this.decisionErrorMessage =
            this.extractBackendMessage(
              error
            )
            ?? 'Не удалось повторно направить документ на проверку.';

          this.changeDetectorRef
            .markForCheck();
        }
      });
  }

  prepareForSed(): void {

    if (
      this.documentId === null
      ||
      !this.canPrepareForSed
      ||
      this.preparingForSed
    ) {
      return;
    }

    this.preparingForSed = true;

    this.sedErrorMessage = '';
    this.sedSuccessMessage = '';

    this.workspaceService
      .prepareDocumentForSed(
        this.documentId
      )
      .pipe(
        finalize(
          () => {

            this.preparingForSed = false;

            this.changeDetectorRef
              .markForCheck();
          }
        )
      )
      .subscribe({

        next: () => {

          this.sedSuccessMessage =
            'Документ успешно подготовлен к отправке в СЭД.';

          this.editingMetadata = false;
          this.metadataForm = null;

          this.refreshDocumentSummary();
        },

        error: (
          error: HttpErrorResponse
        ) => {

          this.sedErrorMessage =
            this.extractBackendMessage(
              error
            )
            ?? 'Не удалось подготовить документ к отправке в СЭД.';

          this.changeDetectorRef
            .markForCheck();
        }
      });
  }

  sendToSed(): void {

    if (
      this.documentId === null
      ||
      !this.canSendToSed
      ||
      this.sendingToSed
    ) {
      return;
    }

    const confirmed =
      window.confirm(
        'Отправить документ в корпоративную СЭД?\n\n'
        + 'После успешной отправки документ будет зафиксирован в истории, '
        + 'а редактирование и удаление будут ограничены.'
      );

    if (!confirmed) {
      return;
    }

    this.sendingToSed = true;

    this.sedErrorMessage = '';
    this.sedSuccessMessage = '';

    this.workspaceService
      .sendDocumentToSed(
        this.documentId
      )
      .pipe(
        finalize(
          () => {

            this.sendingToSed = false;

            this.changeDetectorRef
              .markForCheck();
          }
        )
      )
      .subscribe({

        next: response => {

          this.sedSuccessMessage =
            response.externalDocumentId
              ? `Документ успешно отправлен в СЭД. ID: ${response.externalDocumentId}.`
              : 'Документ успешно отправлен в СЭД.';

          this.editingMetadata = false;
          this.metadataForm = null;

          this.refreshDocumentSummary();
        },

        error: (
          error: HttpErrorResponse
        ) => {

          this.sedErrorMessage =
            this.extractBackendMessage(
              error
            )
            ?? 'Не удалось отправить документ в СЭД.';

          this.changeDetectorRef
            .markForCheck();
        }
      });
  }

  get canPrepareForSed(): boolean {

    return (
      this.canPrepareForSedByRole
      &&
      this.document?.status
        === 'APPROVED'
    );
  }

  get canSendToSed(): boolean {

    return (
      this.canSendToSedByRole
      &&
      this.document?.status
        === 'READY_FOR_SED'
    );
  }

  get sedActionHint(): string {

    if (!this.document) {
      return '';
    }

    switch (
      this.document.status
    ) {

      case 'UPLOADED':
        return 'Сначала необходимо выполнить AI-анализ документа.';

      case 'AI_PROCESSING':
        return 'Дождитесь завершения AI-анализа.';

      case 'REVIEW_REQUIRED':
        return 'Перед отправкой документ необходимо проверить.';

      case 'LINK_REQUIRED':
        return 'Перед отправкой документ необходимо привязать к основному договору.';

      case 'LINKED':
        return this.isOperatorUser ? 'Документ связан с договором и передан назначенному специалисту для проверки и подготовки.' : 'Документ связан с договором и может быть подготовлен к отправке.';

      case 'VERIFIED':
        return this.isOperatorUser ? 'Документ проверен и передан назначенному специалисту для подготовки.' : 'Документ проверен и может быть подготовлен к отправке.';

      case 'READY_FOR_SED':
        return 'Все проверки пройдены. Документ готов к отправке в СЭД.';

      case 'SENT_TO_SED':
        return 'Документ уже отправлен в корпоративную СЭД.';

      default:
        return 'Документ пока не готов к отправке в СЭД.';
    }
  }

  get selectedContractCandidate(): ContractCandidate | null {

    if (
      this.selectedContractDocumentId === null
    ) {
      return null;
    }

    return this.contractCandidates
      .find(
        candidate =>
          candidate.contractDocumentId
          === this.selectedContractDocumentId
      )
      ?? null;
  }

  startMetadataEditing(): void {

    if (
      this.documentId === null
      ||
      !this.canEditMetadata
      ||
      this.loadingMetadata
      ||
      this.savingMetadata
    ) {
      return;
    }

    this.loadingMetadata = true;
    this.metadataErrorMessage = '';
    this.metadataSuccessMessage = '';

    this.workspaceService
      .getDocumentMetadata(
        this.documentId
      )
      .pipe(
        finalize(
          () => {

            this.loadingMetadata = false;

            this.changeDetectorRef
              .markForCheck();
          }
        )
      )
      .subscribe({

        next: metadata => {

          this.metadataForm =
            this.metadataToRequest(
              metadata
            );

          this.editingMetadata = true;

          this.changeDetectorRef
            .markForCheck();
        },

        error: (
          error: HttpErrorResponse
        ) => {

          this.metadataErrorMessage =
            this.extractBackendMessage(
              error
            )
            ?? 'Не удалось загрузить реквизиты документа.';

          this.changeDetectorRef
            .markForCheck();
        }
      });
  }

  get canEditMetadata(): boolean {

    if (
      !this.document
      ||
      !this.canEditMetadataByRole
    ) {
      return false;
    }

    if (
      this.document.status === 'APPROVED'
      ||
      this.document.status === 'REJECTED'
      ||
      this.document.status === 'READY_FOR_SED'
      ||
      this.document.status === 'SENT_TO_SED'
      ||
      this.document.sed !== null
    ) {
      return false;
    }

    return true;
  }

  get metadataEditRestrictionMessage(): string {

    if (!this.document) {
      return '';
    }

    if (
      this.document.status === 'READY_FOR_SED'
    ) {

      return 'Документ уже подготовлен к отправке в СЭД.';
    }

    if (
      this.document.status === 'SENT_TO_SED'
      ||
      this.document.sed !== null
    ) {

      return 'После отправки в СЭД реквизиты документа зафиксированы.';
    }

    return '';
  }

  cancelMetadataEditing(): void {

    if (this.savingMetadata) {
      return;
    }

    this.editingMetadata = false;
    this.metadataForm = null;
    this.metadataErrorMessage = '';

    this.changeDetectorRef
      .markForCheck();
  }

  saveMetadata(): void {

    if (
      this.documentId === null
      ||
      !this.metadataForm
      ||
      !this.canEditMetadata
      ||
      this.savingMetadata
    ) {
      return;
    }

    this.metadataErrorMessage = '';
    this.metadataSuccessMessage = '';

    if (
      this.metadataForm.amount !== null
      &&
      this.metadataForm.amount < 0
    ) {

      this.metadataErrorMessage =
        'Сумма документа не может быть отрицательной.';

      return;
    }

    if (
      this.metadataForm.validFrom
      &&
      this.metadataForm.validTo
      &&
      this.metadataForm.validFrom
        > this.metadataForm.validTo
    ) {

      this.metadataErrorMessage =
        'Дата начала действия не может быть позже даты окончания.';

      return;
    }

    this.savingMetadata = true;

    const request:
      DocumentMetadataRequest = {

        documentNumber:
          this.normalizeNullableText(
            this.metadataForm.documentNumber
          ),

        documentDate:
          this.metadataForm.documentDate || null,

        amount:
          this.metadataForm.amount === null
          ||
          this.metadataForm.amount === undefined
            ? null
            : Number(
                this.metadataForm.amount
              ),

        currency:
          this.normalizeNullableText(
            this.metadataForm.currency
          )?.toUpperCase()
          ?? null,

        subject:
          this.normalizeNullableText(
            this.metadataForm.subject
          ),

        validFrom:
          this.metadataForm.validFrom || null,

        validTo:
          this.metadataForm.validTo || null,

        referencedContractNumber:
          this.normalizeNullableText(
            this.metadataForm.referencedContractNumber
          ),

        referencedContractDate:
          this.metadataForm.referencedContractDate || null
      };

    this.workspaceService
      .updateDocumentMetadata(
        this.documentId,
        request
      )
      .pipe(
        finalize(
          () => {

            this.savingMetadata = false;

            this.changeDetectorRef
              .markForCheck();
          }
        )
      )
      .subscribe({

        next: metadata => {

          this.applyMetadataToDocument(
            metadata
          );

          this.metadataForm = null;
          this.editingMetadata = false;

          this.metadataSuccessMessage =
            'Реквизиты документа сохранены.';

          this.refreshDocumentSummary();

          this.changeDetectorRef
            .markForCheck();
        },

        error: (
          error: HttpErrorResponse
        ) => {

          this.metadataErrorMessage =
            this.extractBackendMessage(
              error
            )
            ?? 'Не удалось сохранить реквизиты документа.';

          this.changeDetectorRef
            .markForCheck();
        }
      });
  }

  backToDocuments(): void {

    void this.router.navigate(
      ['/documents']
    );
  }

  deleteDocument(): void {

    if (
      !this.document
      ||
      !this.canDeleteDocument
      ||
      this.deleting
    ) {
      return;
    }

    const confirmed =
      window.confirm(
        `Удалить документ «${this.document.originalName ?? 'Документ'}»?\n\n`
        + 'Документ, результаты AI-анализа, маршрут и связанный файл будут удалены. '
        + 'Это действие нельзя отменить.'
      );

    if (!confirmed) {
      return;
    }

    this.deleting = true;
    this.deleteErrorMessage = '';

    this.workspaceService
      .deleteDocument(
        this.document.documentId
      )
      .pipe(
        finalize(
          () => {

            this.deleting = false;

            this.changeDetectorRef
              .markForCheck();
          }
        )
      )
      .subscribe({

        next: () => {

          void this.router.navigate(
            ['/documents']
          );
        },

        error: (
          error: HttpErrorResponse
        ) => {

          if (error.status === 0) {

            this.deleteErrorMessage =
              'Не удалось подключиться к серверу DocFlow.';

          } else {

            this.deleteErrorMessage =
              this.extractBackendMessage(
                error
              )
              ?? (
                error.status === 409
                  ? 'Этот документ нельзя удалить из-за связанных данных.'
                  : 'Не удалось удалить документ.'
              );
          }

          this.changeDetectorRef
            .markForCheck();
        }
      });
  }

  get canDeleteDocument(): boolean {

    if (
      !this.document
      ||
      !this.canDeleteByRole
    ) {
      return false;
    }

    if (
      this.document.status === 'SENT_TO_SED'
    ) {
      return false;
    }

    if (
      this.document.sed !== null
    ) {
      return false;
    }

    if (
      this.document.linkedDocumentsCount > 0
    ) {
      return false;
    }

    return true;
  }

  get deleteRestrictionMessage(): string {

    if (!this.document) {
      return '';
    }

    if (
      this.document.status === 'SENT_TO_SED'
    ) {

      return 'Документ уже отправлен в СЭД и хранится в истории.';
    }

    if (
      this.document.sed !== null
    ) {

      return 'Документ имеет историю отправки в СЭД и не может быть удалён.';
    }

    if (
      this.document.linkedDocumentsCount > 0
    ) {

      return 'К этому договору привязаны другие документы. Сначала необходимо удалить или перепривязать их.';
    }

    return '';
  }

  openLinkedContract(): void {

    const contractId =
      this.document?.link
        ?.contractDocumentId;

    if (!contractId) {
      return;
    }

    void this.router.navigate(
      [
        '/documents',
        contractId
      ]
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

      case 'SENT_TO_SED':
      case 'VERIFIED':
      case 'APPROVED':
      case 'READY_FOR_SED':
        return 'green';

      case 'LINK_REQUIRED':
      case 'RETURNED_FOR_CORRECTION':
        return 'orange';

      case 'REVIEW_REQUIRED':
      case 'REJECTED':
      case 'PROCESSING_FAILED':
      case 'SED_ERROR':
        return 'red';

      case 'LINKED':
      case 'UPLOADED':
      case 'AI_PROCESSING':
        return 'violet';

      default:
        return 'neutral';
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

  getRoutingStatusLabel(
    status: string | null
  ): string {

    switch (status) {

      case 'ROUTED':
        return 'Маршрут определён';

      case 'READY':
        return 'Готов к маршрутизации';

      case 'SKIPPED':
        return 'Маршрутизация не выполнена';

      case 'FAILED':
        return 'Ошибка маршрутизации';

      default:
        return status || 'Нет данных';
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

  getConfidence(): string {

    const confidence =
      this.document?.ai
        ?.confidence;

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

  getConfidencePercent(): number {

    const confidence =
      this.document?.ai
        ?.confidence;

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

  getAmount(): string {

    const amount =
      this.document?.amount;

    if (
      amount === null
      ||
      amount === undefined
    ) {
      return '—';
    }

    const formatted =
      new Intl.NumberFormat(
        'ru-RU',
        {
          minimumFractionDigits: 0,
          maximumFractionDigits: 2
        }
      ).format(
        amount
      );

    return this.document?.currency
      ? `${formatted} ${this.document.currency}`
      : formatted;
  }

  private metadataToRequest(
    metadata: DocumentMetadata
  ): DocumentMetadataRequest {

    return {
      documentNumber:
        metadata.documentNumber,

      documentDate:
        metadata.documentDate,

      amount:
        metadata.amount,

      currency:
        metadata.currency,

      subject:
        metadata.subject,

      validFrom:
        metadata.validFrom,

      validTo:
        metadata.validTo,

      referencedContractNumber:
        metadata.referencedContractNumber,

      referencedContractDate:
        metadata.referencedContractDate
    };
  }

  private applyMetadataToDocument(
    metadata: DocumentMetadata
  ): void {

    if (!this.document) {
      return;
    }

    this.document.documentType =
      metadata.documentType;

    this.document.status =
      metadata.status;

    this.document.documentNumber =
      metadata.documentNumber;

    this.document.documentDate =
      metadata.documentDate;

    this.document.amount =
      metadata.amount;

    this.document.currency =
      metadata.currency;

    this.document.subject =
      metadata.subject;

    this.document.validFrom =
      metadata.validFrom;

    this.document.validTo =
      metadata.validTo;
  }

  private normalizeNullableText(
    value: string | null
  ): string | null {

    if (value === null) {
      return null;
    }

    const normalized =
      value.trim();

    return normalized
      ? normalized
      : null;
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

      const response =
        error.error as {
          message?: unknown;
          detail?: unknown;
          error?: unknown;
        };

      if (
        typeof response.message === 'string'
      ) {
        return response.message;
      }

      if (
        typeof response.detail === 'string'
      ) {
        return response.detail;
      }

      if (
        typeof response.error === 'string'
      ) {
        return response.error;
      }
    }

    return null;
  }
}