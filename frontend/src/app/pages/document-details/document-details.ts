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
  DocumentMetadata,
  DocumentMetadataRequest,
  DocumentSummary,
  DocumentTypeCode,
  WorkspaceService
} from '../../core/workspace/workspace.service';

@Component({
  selector: 'app-document-details',
  standalone: true,
  imports: [
    FormsModule
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

    return this.document?.status
      === 'REVIEW_REQUIRED';
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

    if (!this.document) {
      return false;
    }

    if (
      this.document.documentType === 'CONTRACT'
    ) {

      return this.document.status
        === 'VERIFIED';
    }

    return this.document.status
      === 'LINKED';
  }

  get canSendToSed(): boolean {

    return this.document?.status
      === 'READY_FOR_SED';
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
        return 'Документ связан с договором и может быть подготовлен к отправке.';

      case 'VERIFIED':
        return 'Документ проверен и может быть подготовлен к отправке.';

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

    if (!this.document) {
      return false;
    }

    if (
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

    if (!this.document) {
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
      case 'READY_FOR_SED':
        return 'green';

      case 'LINK_REQUIRED':
        return 'orange';

      case 'REVIEW_REQUIRED':
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