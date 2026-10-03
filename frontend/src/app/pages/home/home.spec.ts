import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of } from 'rxjs';
import { describe, it, expect, vi } from 'vitest';
import { Home } from './home';
import { AuthService } from '../../core/auth/auth.service';
import { WorkspaceService } from '../../core/workspace/workspace.service';
import { NotificationService } from '../../core/notifications/notification.service';
describe('Home workspace presentation',()=>{
 it('preserves counters, document navigation and quick links',async()=>{
 const api={getDashboard:()=>of({reviewRequired:1,linkRequired:2,processingFailed:0,sedError:0,expiringSoonContracts:4,expiredContracts:5,aiProcessing:2,readyForSed:3}),getDocuments:()=>of([{documentId:53,originalName:'Document.pdf',documentNumber:null,documentType:'CONTRACT',status:'REVIEW_REQUIRED',createdAt:'2026-10-03T17:12:00'}])};
 await TestBed.configureTestingModule({imports:[Home],providers:[provideRouter([]),{provide:AuthService,useValue:{getCurrentUser:()=>({role:'ADMIN'})}},{provide:WorkspaceService,useValue:api},{provide:NotificationService,useValue:{getUnreadCount:()=>of({unreadCount:7})}}]}).compileComponents();
 const fixture=TestBed.createComponent(Home);fixture.detectChanges();await fixture.whenStable();fixture.detectChanges();const view=fixture.nativeElement as HTMLElement;
 expect(view.querySelectorAll('.quick-action')).toHaveLength(5);expect([...view.querySelectorAll('.attention-card strong')].map(e=>e.textContent?.trim())).toEqual(['3','4','5']);expect(view.querySelector('.notification-summary')?.textContent).toContain('7');expect(view.querySelector('.document-main')?.textContent).toContain('Document.pdf');const navigate=vi.spyOn(TestBed.inject(Router),'navigate').mockResolvedValue(true);(view.querySelector('.recent-document') as HTMLButtonElement).click();expect(navigate).toHaveBeenCalledWith(['/documents',53]);
 
 fixture.destroy();
 });
});
