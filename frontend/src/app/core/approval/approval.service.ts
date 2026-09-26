import {
  HttpClient
} from '@angular/common/http';

import {
  Injectable
} from '@angular/core';

import {
  Observable
} from 'rxjs';


export type ApprovalStepStatus =
  | 'PENDING'
  | 'IN_PROGRESS'
  | 'APPROVED'
  | 'RETURNED'
  | 'REJECTED'
  | 'CANCELLED';


export interface ApprovalStep {
  stepId: number;

  documentId: number;

  documentNumber: string | null;

  documentType: string | null;

  documentStatus: string | null;

  documentVersionId: number | null;

  documentVersionNumber: number | null;

  stepOrder: number;

  roleCode: string;

  status: ApprovalStepStatus;

  routingReason: string | null;

  assignedUserId: number | null;

  assignedUserFullName: string | null;

  comment: string | null;

  dueAt: string | null;

  startedAt: string | null;

  completedAt: string | null;

  completedByUserId: number | null;

  completedByUserFullName: string | null;

  createdAt: string | null;
}


@Injectable({
  providedIn: 'root'
})
export class ApprovalService {

  private readonly apiUrl =
    'http://localhost:8080/api/approval';


  constructor(
    private readonly http: HttpClient
  ) {
  }


  getMyQueue():
    Observable<ApprovalStep[]> {

    return this.http.get<ApprovalStep[]>(
      `${this.apiUrl}/my`
    );
  }


  claim(
    stepId: number
  ): Observable<ApprovalStep> {

    return this.http.post<ApprovalStep>(
      `${this.apiUrl}/steps/${stepId}/claim`,
      null
    );
  }


  getDocumentSteps(
    documentId: number
  ): Observable<ApprovalStep[]> {

    return this.http.get<ApprovalStep[]>(
      `${this.apiUrl}/documents/${documentId}/steps`
    );
  }
}