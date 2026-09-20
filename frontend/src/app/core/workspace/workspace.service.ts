import {
  HttpClient,
  HttpParams
} from '@angular/common/http';

import {
  Injectable
} from '@angular/core';

import {
  Observable
} from 'rxjs';

export type DocumentTypeCode =
  | 'CONTRACT'
  | 'AMENDMENT_CHANGE'
  | 'AMENDMENT_PROLONGATION'
  | 'APPENDIX';

export interface WorkspaceDashboard {
  totalDocuments: number;
  uploaded: number;
  aiProcessing: number;
  reviewRequired: number;
  linkRequired: number;
  linked: number;
  verified: number;
  readyForSed: number;
  sentToSed: number;
  processingFailed: number;
  sedError: number;
  expiringSoonContracts: number;
  expiredContracts: number;
}

export interface WorkspaceDocument {
  documentId: number;
  fileId: number | null;
  originalName: string | null;
  documentType: string | null;
  status: string | null;
  documentNumber: string | null;
  documentDate: string | null;
  subject: string | null;
  amount: number | null;
  currency: string | null;

  expirationStatus: string | null;
  daysUntilExpiration: number | null;

  createdAt: string | null;
  updatedAt: string | null;
}

export interface DocumentAiSummary {
  resultId: number;
  predictedType: string | null;
  confidence: number | null;
  processingStatus: string | null;
}

export interface DocumentRoutingSummary {
  routeId: number;
  routingTarget: string | null;
  routingStatus: string | null;
  routingReason: string | null;
  routedAt: string | null;
}

export interface DocumentLinkSummary {
  linkId: number;
  contractDocumentId: number;
  contractDocumentNumber: string | null;
  contractDocumentDate: string | null;
  linkedAt: string | null;
}

export interface DocumentSedSummary {
  dispatchId: number;
  dispatchStatus: string | null;
  externalDocumentId: string | null;
  responseMessage: string | null;
  sentAt: string | null;
}

export type DocumentDecisionCode =
  | 'APPROVE'
  | 'RETURN_FOR_CORRECTION'
  | 'REJECT';

export interface DocumentDecisionRequest {
  decision: DocumentDecisionCode;
  comment: string | null;
}

export interface DocumentDecisionResponse {
  id: number;
  documentId: number;
  decision: DocumentDecisionCode;
  documentStatus: string;
  comment: string | null;
  userId: number;
  userFullName: string;
  createdAt: string | null;
}
export interface DocumentCommentRequest {
  comment: string;
}

export interface DocumentCommentResponse {
  id: number;
  documentId: number;
  userId: number;
  userFullName: string;
  comment: string;
  createdAt: string;
}

export type AuditActorType =
  | 'USER'
  | 'SYSTEM';

export interface DocumentAuditEventResponse {
  id: number;
  documentId: number;
  eventType: string;
  actorType: AuditActorType;
  userId: number | null;
  actorName: string;
  description: string;
  details: string | null;
  createdAt: string;
}
export interface AiRiskFinding {
  code: string;
  title: string;
  severity: string;
  weight: number;
  evidence: string;
  explanation: string;
  recommendation: string;
}

export interface AiRiskAnalysis {
  analysisId: number;
  riskLevel: string;
  riskScore: number;
  riskCount: number;
  risks: AiRiskFinding[];
  summary: string;
  analyzerVersion: string;
  analyzedAt: string;
}

export interface DocumentSummary {
  documentId: number;
  fileId: number | null;
  originalName: string | null;
  documentType: string | null;
  status: string | null;

  documentNumber: string | null;
  documentDate: string | null;
  amount: number | null;
  currency: string | null;
  subject: string | null;
  validFrom: string | null;
  validTo: string | null;

  expirationStatus: string | null;
  daysUntilExpiration: number | null;

  ai: DocumentAiSummary | null;
  routing: DocumentRoutingSummary | null;
  link: DocumentLinkSummary | null;
  sed: DocumentSedSummary | null;

  linkedDocumentsCount: number;
}

export interface DocumentMetadata {
  documentId: number;
  documentType: string | null;
  status: string | null;
  documentNumber: string | null;
  documentDate: string | null;
  amount: number | null;
  currency: string | null;
  subject: string | null;
  validFrom: string | null;
  validTo: string | null;
  referencedContractNumber: string | null;
  referencedContractDate: string | null;
}

export interface DocumentMetadataRequest {
  documentNumber: string | null;
  documentDate: string | null;
  amount: number | null;
  currency: string | null;
  subject: string | null;
  validFrom: string | null;
  validTo: string | null;
  referencedContractNumber: string | null;
  referencedContractDate: string | null;
}

export interface ContractCandidate {
  contractDocumentId: number;
  documentNumber: string | null;
  documentDate: string | null;
  subject: string | null;
  status: string | null;
}

export interface DocumentLinkRequest {
  contractDocumentId: number;
}

export interface DocumentLinkResponse {
  linkId: number;
  childDocumentId: number;
  childDocumentType: string;
  contractDocumentId: number;
  contractDocumentType: string;
  childDocumentStatus: string;
  routingTarget: string;
  routingStatus: string;
  linkedAt: string;
}

export interface ManualReviewRequest {
  documentType: DocumentTypeCode;
}

export interface ManualReviewResponse {
  documentId: number;
  documentType: string;
  documentStatus: string;
  routingTarget: string | null;
  routingStatus: string | null;
}

export interface SedPreparationResponse {
  documentId: number;
  documentType: string;
  status: string;
  documentNumber: string | null;
  documentDate: string | null;
  subject: string | null;
  routingTarget: string;
}

export interface SedDispatchResponse {
  dispatchId: number;
  documentId: number;
  documentType: string | null;
  documentStatus: string | null;
  dispatchStatus: string;
  externalDocumentId: string | null;
  responseMessage: string | null;
  sentAt: string | null;
}

export interface DocumentUploadResponse {
  documentId: number;
  fileId: number;
  originalName: string;
  status: string;
  duplicateDetected: boolean;
  sha256Checksum: string;
}

export interface AiProcessingResponse {
  resultId: number;
  documentId: number;
  predictedType: string | null;
  documentType: string | null;
  confidence: number | null;
  classificationAccepted: boolean | null;
  reviewRequired: boolean | null;
  requiresOcr: boolean | null;
  modelVersion: string | null;
  processingStatus: string | null;
  probabilities: Record<string, number>;
  recognizedText: string | null;
}

@Injectable({
  providedIn: 'root'
})
export class WorkspaceService {

  private readonly workspaceApiUrl =
    'http://localhost:8080/api/workspace';

  private readonly documentsApiUrl =
    'http://localhost:8080/api/documents';

  constructor(
    private readonly http: HttpClient
  ) {
  }

  getDashboard(): Observable<WorkspaceDashboard> {

    return this.http.get<WorkspaceDashboard>(
      `${this.workspaceApiUrl}/dashboard`
    );
  }

  getDocuments(
    query?: string,
    type?: string,
    status?: string,
    expirationStatus?: string
  ): Observable<WorkspaceDocument[]> {

    let params =
      new HttpParams();

    if (query?.trim()) {

      params = params.set(
        'query',
        query.trim()
      );
    }

    if (type?.trim()) {

      params = params.set(
        'type',
        type.trim()
      );
    }

    if (status?.trim()) {

      params = params.set(
        'status',
        status.trim()
      );
    }

    if (expirationStatus?.trim()) {

      params = params.set(
        'expirationStatus',
        expirationStatus.trim()
      );
    }

    return this.http.get<WorkspaceDocument[]>(
      `${this.workspaceApiUrl}/documents`,
      {
        params
      }
    );
  }

  getDocumentSummary(
    documentId: number
  ): Observable<DocumentSummary> {

    return this.http.get<DocumentSummary>(
      `${this.documentsApiUrl}/${documentId}/summary`
    );
  }

  getDocumentMetadata(
    documentId: number
  ): Observable<DocumentMetadata> {

    return this.http.get<DocumentMetadata>(
      `${this.documentsApiUrl}/${documentId}/metadata`
    );
  }

  updateDocumentMetadata(
    documentId: number,
    request: DocumentMetadataRequest
  ): Observable<DocumentMetadata> {

    return this.http.put<DocumentMetadata>(
      `${this.documentsApiUrl}/${documentId}/metadata`,
      request
    );
  }

  reviewDocument(
    documentId: number,
    documentType: DocumentTypeCode
  ): Observable<ManualReviewResponse> {

    const request: ManualReviewRequest = {
      documentType
    };

    return this.http.post<ManualReviewResponse>(
      `${this.documentsApiUrl}/${documentId}/review`,
      request
    );
  }

  getLinkCandidates(
    documentId: number
  ): Observable<ContractCandidate[]> {

    return this.http.get<ContractCandidate[]>(
      `${this.documentsApiUrl}/${documentId}/link-candidates`
    );
  }

  linkDocument(
    documentId: number,
    contractDocumentId: number
  ): Observable<DocumentLinkResponse> {

    const request: DocumentLinkRequest = {
      contractDocumentId
    };

    return this.http.post<DocumentLinkResponse>(
      `${this.documentsApiUrl}/${documentId}/link`,
      request
    );
  }

  decideDocument(
    documentId: number,
    decision: DocumentDecisionCode,
    comment: string | null
  ): Observable<DocumentDecisionResponse> {

    const request: DocumentDecisionRequest = {
      decision,
      comment
    };

    return this.http.post<DocumentDecisionResponse>(
      `${this.documentsApiUrl}/${documentId}/decision`,
      request
    );
  }

  getDocumentDecisions(
    documentId: number
  ): Observable<DocumentDecisionResponse[]> {

    return this.http.get<DocumentDecisionResponse[]>(
      `${this.documentsApiUrl}/${documentId}/decisions`
    );
  }

  getDocumentComments(
    documentId: number
  ): Observable<DocumentCommentResponse[]> {

    return this.http.get<DocumentCommentResponse[]>(
      `${this.documentsApiUrl}/${documentId}/comments`
    );
  }

  getDocumentAudit(
    documentId: number
  ): Observable<DocumentAuditEventResponse[]> {

    return this.http.get<DocumentAuditEventResponse[]>(
      `${this.documentsApiUrl}/${documentId}/audit`
    );
  }

  addDocumentComment(
    documentId: number,
    comment: string
  ): Observable<DocumentCommentResponse> {

    const request: DocumentCommentRequest = {
      comment
    };

    return this.http.post<DocumentCommentResponse>(
      `${this.documentsApiUrl}/${documentId}/comments`,
      request
    );
  }
  resubmitDocumentForReview(
    documentId: number
  ): Observable<string> {

    return this.http.post<string>(
      `${this.documentsApiUrl}/${documentId}/resubmit-for-review`,
      null
    );
  }
  prepareDocumentForSed(
    documentId: number
  ): Observable<SedPreparationResponse> {

    return this.http.post<SedPreparationResponse>(
      `${this.documentsApiUrl}/${documentId}/prepare-for-sed`,
      null
    );
  }

  sendDocumentToSed(
    documentId: number
  ): Observable<SedDispatchResponse> {

    return this.http.post<SedDispatchResponse>(
      `${this.documentsApiUrl}/${documentId}/send-to-sed`,
      null
    );
  }

  getSedDispatchHistory(
    documentId: number
  ): Observable<SedDispatchResponse[]> {

    return this.http.get<SedDispatchResponse[]>(
      `${this.documentsApiUrl}/${documentId}/sed-dispatches`
    );
  }

  uploadDocument(
    file: File
  ): Observable<DocumentUploadResponse> {

    const formData =
      new FormData();

    formData.append(
      'file',
      file
    );

    return this.http.post<DocumentUploadResponse>(
      `${this.documentsApiUrl}/upload`,
      formData
    );
  }

  getLatestContractRiskAnalysis(
    documentId: number
  ): Observable<AiRiskAnalysis | null> {

    return this.http.get<AiRiskAnalysis | null>(
      `${this.documentsApiUrl}/${documentId}/risk-analysis`
    );
  }

  analyzeContractRisks(
    documentId: number
  ): Observable<AiRiskAnalysis> {

    return this.http.post<AiRiskAnalysis>(
      `${this.documentsApiUrl}/${documentId}/risk-analysis`,
      null
    );
  }

  analyzeDocument(
    documentId: number
  ): Observable<AiProcessingResponse> {

    return this.http.post<AiProcessingResponse>(
      `${this.documentsApiUrl}/${documentId}/analyze`,
      null
    );
  }

  archiveDocument(
    documentId: number
  ): Observable<{
    documentId: number;
    status: string;
    updatedAt: string;
  }> {

    return this.http.post<{
      documentId: number;
      status: string;
      updatedAt: string;
    }>(
      `${this.documentsApiUrl}/${documentId}/archive`,
      null
    );
  }


  restoreDocument(
    documentId: number
  ): Observable<{
    documentId: number;
    status: string;
    updatedAt: string;
  }> {

    return this.http.post<{
      documentId: number;
      status: string;
      updatedAt: string;
    }>(
      `${this.documentsApiUrl}/${documentId}/restore`,
      null
    );
  }

  deleteDocument(
    documentId: number
  ): Observable<void> {

    return this.http.delete<void>(
      `${this.documentsApiUrl}/${documentId}`
    );
  }
}