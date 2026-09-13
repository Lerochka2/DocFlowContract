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

@Injectable({
  providedIn: 'root'
})
export class WorkspaceService {

  private readonly apiUrl =
    'http://localhost:8080/api/workspace';

  constructor(
    private readonly http: HttpClient
  ) {
  }

  getDashboard(): Observable<WorkspaceDashboard> {

    return this.http.get<WorkspaceDashboard>(
      `${this.apiUrl}/dashboard`
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
      `${this.apiUrl}/documents`,
      {
        params
      }
    );
  }
}