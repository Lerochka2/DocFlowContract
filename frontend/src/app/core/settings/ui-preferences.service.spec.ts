import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { of, throwError } from 'rxjs';
import { AuthService } from '../auth/auth.service';
import { UiPreferencesService } from './ui-preferences.service';
import { Documents } from '../../pages/documents/documents';
import { DocumentDetails } from '../../pages/document-details/document-details';
import { ContractRiskAnalysisComponent } from '../../pages/document-details/contract-risk-analysis/contract-risk-analysis';
import { AppShell } from '../../layout/app-shell';

describe('Working personal preferences', () => {
  let user: any, preferences: UiPreferencesService;
  beforeEach(() => {
    localStorage.clear();
    user = { userId: 2, role: 'DOCUMENT_OPERATOR' };
    TestBed.configureTestingModule({ providers: [{ provide: AuthService, useValue: { getCurrentUser: () => user } }] });
    preferences = TestBed.inject(UiPreferencesService);
  });
  afterEach(() => { localStorage.clear(); vi.restoreAllMocks(); });

  it('isolates settings and last page by account and restores them on return', () => {
    preferences.updatePreferences({ compactMode: true, rememberLastPage: true, defaultPage: '/documents' });
    preferences.rememberPage('/review');
    user = { userId: 3, role: 'LAWYER' }; preferences.activateUser();
    expect(preferences.preferences().compactMode).toBe(false);
    expect(preferences.landingPage()).toBe('/home');
    user = { userId: 2, role: 'DOCUMENT_OPERATOR' }; preferences.activateUser();
    expect(preferences.preferences().compactMode).toBe(true);
    expect(preferences.landingPage()).toBe('/review');
  });
  it('rejects unavailable routes and ignores document/settings URLs when remembering navigation', () => {
    preferences.updatePreferences({ defaultPage: '/users', rememberLastPage: true });
    expect(preferences.preferences().defaultPage).toBe('/home');
    preferences.rememberPage('/documents'); preferences.rememberPage('/settings'); preferences.rememberPage('/documents/55');
    expect(preferences.landingPage()).toBe('/documents');
    preferences.updatePreferences({ rememberLastPage: false, defaultPage: '/review' });
    expect(preferences.landingPage()).toBe('/review');
    preferences.resetPreferences(); expect(preferences.landingPage()).toBe('/home');
  });
  it('normalizes damaged stored values and revalidates a role change', () => {
    localStorage.setItem('docflow.ui.preferences.3', JSON.stringify({ compactMode: 'false', defaultPage: '/users' }));
    user = { userId: 3, role: 'LAWYER' }; preferences.activateUser();
    expect(preferences.preferences().compactMode).toBe(false);
    expect(preferences.landingPage()).toBe('/home');
  });
  for (const openAfterUpload of [false, true]) {
    it(`uses openAfterUpload=${openAfterUpload} after successful analysis`, () => {
      preferences.updatePreferences({ openAfterUpload });
      const vm: any = Object.create(Documents.prototype);
      Object.assign(vm, { uiPreferences: preferences, workspaceService: { uploadDocument: () => of({ documentId: 77 }), analyzeDocument: () => of({}) }, router: { navigate: vi.fn() }, changeDetectorRef: { markForCheck: vi.fn() }, loadDocuments: vi.fn() });
      Object.defineProperty(vm, 'canManageDocuments', { value: true });
      vm.uploadAndAnalyze(new File(['pdf'], 'test.pdf'));
      expect(vm.router.navigate).toHaveBeenCalledTimes(openAfterUpload ? 1 : 0);
      expect(vm.loadDocuments).toHaveBeenCalledTimes(openAfterUpload ? 0 : 1);
    });
  }
  it('keeps the upload error visible and stays in the list if analysis fails', () => {
    const vm: any = Object.create(Documents.prototype);
    Object.assign(vm, { uiPreferences: preferences, workspaceService: { uploadDocument: () => of({ documentId: 77 }), analyzeDocument: () => throwError(() => new Error('offline')) }, router: { navigate: vi.fn() }, changeDetectorRef: { markForCheck: vi.fn() }, loadDocuments: vi.fn() });
    Object.defineProperty(vm, 'canManageDocuments', { value: true }); vm.uploadAndAnalyze(new File(['pdf'], 'test.pdf'));
    expect(vm.router.navigate).not.toHaveBeenCalled(); expect(vm.uploadMessage).toContain('ошибкой');
  });
  it('sorts risks without mutating the response and toggles high-risk emphasis', () => {
    const vm: any = Object.create(ContractRiskAnalysisComponent.prototype);
    const risks = [{ code: 'a', severity: 'LOW' }, { code: 'b', severity: 'HIGH' }, { code: 'c', severity: 'MEDIUM' }];
    Object.assign(vm, { uiPreferences: preferences, result: { risks } });
    expect(vm.displayedRisks.map((r:any) => r.code)).toEqual(['b','c','a']);
    expect(risks[0].code).toBe('a'); expect(vm.isHighRisk(risks[1])).toBe(true);
    preferences.updatePreferences({ sortRisks: false, highlightHighRisks: false });
    expect(vm.displayedRisks).toBe(risks); expect(vm.isHighRisk(risks[1])).toBe(false);
  });
  it('filters only notification display without changing the unread count or source data', () => {
    const vm: any = Object.create(AppShell.prototype);
    Object.assign(vm, { uiPreferencesService: preferences, notifications: [{ id:1, read:false },{ id:2, read:true }], notificationUnreadCount:1 });
    expect(vm.visibleNotifications.length).toBe(2);
    preferences.updatePreferences({ unreadNotificationsOnly: true });
    expect(vm.visibleNotifications.map((n:any) => n.id)).toEqual([1]);
    expect(vm.notifications.length).toBe(2); expect(vm.notificationCount).toBe(1);
  });
  for (const decision of ['APPROVE', 'REJECT', 'RETURN_FOR_CORRECTION']) {
    it(`preserves mandatory confirmation for ${decision}`, () => {
      preferences.updatePreferences({ confirmApproval: false });
      const confirm = vi.spyOn(window,'confirm').mockReturnValue(false);
      const vm:any = Object.create(DocumentDetails.prototype);
      Object.assign(vm, { uiPreferences:preferences, documentId:77, decisionComment:'Причина', decidingDocument:false, workspaceService:{decideDocument:vi.fn(()=>of({}))}, changeDetectorRef:{markForCheck:vi.fn()}, refreshDocumentSummary:vi.fn() });
      Object.defineProperty(vm,'canMakeDocumentDecision',{value:true}); vm.submitDocumentDecision(decision);
      expect(confirm).toHaveBeenCalledTimes(decision==='APPROVE'?0:1);
      expect(vm.workspaceService.decideDocument).toHaveBeenCalledTimes(decision==='APPROVE'?1:0);
      if(decision==='APPROVE') { preferences.updatePreferences({confirmApproval:true}); vm.workspaceService.decideDocument.mockClear(); vm.submitDocumentDecision(decision); expect(confirm).toHaveBeenCalledOnce(); expect(vm.workspaceService.decideDocument).not.toHaveBeenCalled(); }
    });
  }
});
