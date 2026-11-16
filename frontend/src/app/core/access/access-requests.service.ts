import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
export interface AccessDepartmentOption {
  department: string;
  positions: { name: string; roleCode: string }[];
}
export interface AccessRequestRecord {
  id:number; fullName:string; email:string; department:string; position:string; comment:string;
  status:'NEW'|'APPROVED'|'REJECTED'; createdAt:string; processedAt:string|null;
  adminComment:string|null; createdUserId:number|null;
}
@Injectable({providedIn:'root'})
export class AccessRequestsService {
  private http=inject(HttpClient);
  private base='http://localhost:8080/api';
  readonly pendingCount=signal(0);
  submit(data:Record<string,string>) { return this.http.post<{requestId:number}>(`${this.base}/public/access-requests`,data); }
  options() { return this.http.get<AccessDepartmentOption[]>(`${this.base}/public/access-requests/options`); }
  support() { return this.http.get<{email:string}>(`${this.base}/public/support`); }
  list() { return this.http.get<AccessRequestRecord[]>(`${this.base}/admin/access-requests`); }
  refreshCount() { this.http.get<{count:number}>(`${this.base}/admin/access-requests/count`).subscribe({next:r=>this.pendingCount.set(r.count),error:()=>this.pendingCount.set(0)}); }
  approve(id:number,login:string,roleCode:string) { return this.http.post<AccessRequestRecord>(`${this.base}/admin/access-requests/${id}/approve`,{login,roleCode}); }
  reject(id:number,reason:string) { return this.http.post<AccessRequestRecord>(`${this.base}/admin/access-requests/${id}/reject`,{reason}); }
}
