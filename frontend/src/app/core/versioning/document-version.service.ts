import {
  HttpClient
} from '@angular/common/http';

import {
  Injectable
} from '@angular/core';

import {
  Observable
} from 'rxjs';


export interface DocumentVersion {
  versionId: number;
  documentId: number;
  fileId: number;
  versionNumber: number;

  fileName: string;
  mimeType: string | null;
  sizeBytes: number | null;
  sha256Checksum: string | null;

  createdByUserId: number | null;
  createdByFullName: string | null;

  changeNote: string | null;
  createdAt: string | null;

  currentVersion: boolean;
}


@Injectable({
  providedIn: 'root'
})
export class DocumentVersionService {

  private readonly apiUrl =
    'http://localhost:8080/api/documents';


  constructor(
    private readonly http: HttpClient
  ) {
  }


  getVersions(
    documentId: number
  ): Observable<DocumentVersion[]> {

    return this.http.get<DocumentVersion[]>(
      `${this.apiUrl}/${documentId}/versions`
    );
  }


  uploadVersion(
    documentId: number,
    file: File,
    changeNote: string | null
  ): Observable<DocumentVersion> {

    const formData =
      new FormData();

    formData.append(
      'file',
      file
    );

    if (changeNote) {
      formData.append(
        'changeNote',
        changeNote
      );
    }

    return this.http.post<DocumentVersion>(
      `${this.apiUrl}/${documentId}/versions`,
      formData
    );
  }
}