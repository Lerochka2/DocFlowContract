import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { Router } from '@angular/router';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import { ReviewQueue } from './review';
import { WorkspaceService, DocumentSummary } from '../../core/workspace/workspace.service';
const doc={documentId:53,originalName:'Метод рекомендации_КП.pdf',documentNumber:null,documentDate:null,subject:null,status:'REVIEW_REQUIRED',documentType:'CONTRACT',ai:{confidence:0.3391,predictedType:'CONTRACT'}} as unknown as DocumentSummary;
describe('Review card presentation and existing actions',()=>{
 let fixture:any,view:HTMLElement,api:any,router:any;
 beforeEach(async()=>{api={getDocuments:vi.fn(()=>of([{documentId:53}])),getDocumentSummary:vi.fn(()=>of(doc))};router={navigate:vi.fn()};await TestBed.configureTestingModule({imports:[ReviewQueue],providers:[{provide:WorkspaceService,useValue:api},{provide:Router,useValue:router}]}).compileComponents();fixture=TestBed.createComponent(ReviewQueue);view=fixture.nativeElement;fixture.detectChanges();await fixture.whenStable();fixture.detectChanges();});
 it('uses the shared PDF asset and real confidence/threshold values',async()=>{
 expect(view.querySelector('.file-icon img')?.getAttribute('src')).toBe('assets/ai-analysis/pdf-file-3d.png');expect(view.querySelector('.confidence-value strong')?.textContent).toContain('33.91%');expect(Number(view.querySelector('[role=progressbar]')?.getAttribute('aria-valuenow'))).toBeCloseTo(33.91,2);expect(view.querySelector('.threshold-label')?.textContent).toContain('80%');expect(view.querySelector('.classification-cell')?.textContent).toContain('Договор');
 });
 it('keeps document navigation and refresh filtering',async()=>{(view.querySelector('.action-cell button') as HTMLButtonElement).click();expect(router.navigate).toHaveBeenCalledWith(['/documents',53]);(view.querySelector('.refresh-button') as HTMLButtonElement).click();await fixture.whenStable();fixture.detectChanges();expect(api.getDocuments).toHaveBeenCalledWith(undefined,undefined,'REVIEW_REQUIRED');expect(api.getDocuments).toHaveBeenCalledTimes(2);});
 it('keeps the unknown-confidence state without displaying a false percentage',async()=>{api.getDocumentSummary.mockReturnValue(of({...doc,ai:{...doc.ai,confidence:null}}));(view.querySelector('.refresh-button') as HTMLButtonElement).click();await fixture.whenStable();fixture.detectChanges();expect(view.querySelector('.review-card')?.classList.contains('confidence-unknown')).toBe(true);expect(view.querySelector('.confidence-warning')?.textContent).toContain('Нет оценки AI');expect(view.querySelector('[role=progressbar]')?.hasAttribute('aria-valuenow')).toBe(false);});
});
