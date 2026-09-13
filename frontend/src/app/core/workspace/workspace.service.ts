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
    status?: string
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

  analyzeDocument(
    documentId: number
  ): Observable<AiProcessingResponse> {

    return this.http.post<AiProcessingResponse>(
      `${this.documentsApiUrl}/${documentId}/analyze`,
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