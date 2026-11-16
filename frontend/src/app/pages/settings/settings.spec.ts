import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { Router } from '@angular/router';
import { vi, describe, it, expect, beforeEach, afterEach } from 'vitest';
import { SettingsWorkspace } from './settings';
import { AuthService } from '../../core/auth/auth.service';
import { UiPreferencesService } from '../../core/settings/ui-preferences.service';

describe('Settings redesigned template',()=>{
 let fixture:any,view:HTMLElement,auth:any,preferences:UiPreferencesService;
 const admin={userId:1,login:'admin',fullName:'Администратор DocFlow',email:'admin@company.by',role:'ADMIN'};
 beforeEach(async()=>{
 localStorage.removeItem('docflow.ui.preferences.1');
 auth={getCurrentUser:()=>admin,me:vi.fn(()=>of(admin)),requestPasswordChangeCode:vi.fn(()=>of({maskedEmail:'a***@company.by',expiresInSeconds:300,resendAvailableInSeconds:60})),verifyPasswordChangeCode:vi.fn(()=>of({changeToken:'test-token',expiresInSeconds:300})),confirmPasswordChange:vi.fn(()=>of({})),logout:vi.fn()};
 await TestBed.configureTestingModule({imports:[SettingsWorkspace],providers:[{provide:AuthService,useValue:auth},{provide:Router,useValue:{navigate:vi.fn()}}]}).compileComponents();
 fixture=TestBed.createComponent(SettingsWorkspace);view=fixture.nativeElement;preferences=TestBed.inject(UiPreferencesService);fixture.detectChanges();await fixture.whenStable();fixture.detectChanges();
 });
 afterEach(()=>{fixture.destroy();vi.restoreAllMocks();vi.useRealTimers();localStorage.removeItem('docflow.ui.preferences.1');});
 it('renders ADMIN read-only configuration and truthful audit features',async()=>{
 expect(view.querySelectorAll('.system-item')).toHaveLength(4);expect(view.querySelectorAll('.routing-rule')).toHaveLength(6);expect(view.querySelector('.system-grid input')).toBeNull();expect(view.querySelector('.routing-rules input')).toBeNull();expect(view.querySelector('.integration-details input')).toBeNull();expect(view.querySelectorAll('.audit-item')).toHaveLength(3);expect(view.querySelector('.audit-card')?.textContent).not.toContain('Ежедневно');expect(view.querySelector('.audit-card')?.textContent).not.toContain('09:24');expect(view.querySelector('.profile-card')?.textContent).toContain(admin.email);
 
 });
 it('toggles preferences and saves through the real preferences service',async()=>{
 (view.querySelector('input[aria-label="Подсказка Ctrl K"]') as HTMLInputElement).click();await fixture.whenStable();fixture.detectChanges();(view.querySelector('.settings-actions .primary-button') as HTMLButtonElement).click();await fixture.whenStable();fixture.detectChanges();expect(preferences.preferences()).toMatchObject({showSearchShortcut:false});expect(JSON.parse(localStorage.getItem('docflow.ui.preferences.1')!)).toMatchObject({showSearchShortcut:false});expect(view.querySelector('.message.success')?.textContent).toContain('сохранены');
 });
 it('restores defaults only after the existing confirmation',async()=>{
 preferences.updatePreferences({showSearchShortcut:false});fixture.componentInstance.form={...preferences.preferences(),showSearchShortcut:false};fixture.changeDetectorRef.markForCheck();await fixture.whenStable();fixture.detectChanges();const confirm=vi.spyOn(window,'confirm').mockReturnValue(false);(view.querySelector('.secondary-button') as HTMLButtonElement).click();expect(preferences.preferences().showSearchShortcut).toBe(false);confirm.mockReturnValue(true);(view.querySelector('.secondary-button') as HTMLButtonElement).click();await fixture.whenStable();fixture.detectChanges();expect(preferences.preferences()).toMatchObject({showSearchShortcut:true});expect((view.querySelector('.switch input') as HTMLInputElement).checked).toBe(true);
 });
 it('keeps the personal page without ADMIN configuration controls',async()=>{
 fixture.componentInstance.currentUser={...admin,role:'DOCUMENT_OPERATOR'};fixture.changeDetectorRef.markForCheck();await fixture.whenStable();fixture.detectChanges();expect(view.querySelector('.system-grid')).toBeNull();expect(view.querySelector('.routing-rules')).toBeNull();expect(view.querySelector('.sed-card')).toBeNull();expect(view.querySelector('.audit-card')).toBeNull();expect(view.querySelectorAll('.switch')).toHaveLength(7);expect(view.querySelector('.security-card')).not.toBeNull();
 });
 it('preserves email code request, verification and password fields',async()=>{
 (view.querySelector('.password-primary-button') as HTMLButtonElement).click();await fixture.whenStable();fixture.detectChanges();expect(auth.requestPasswordChangeCode).toHaveBeenCalledOnce();expect(fixture.componentInstance.passwordStep).toBe('CODE');expect((view.querySelector('.account-settings') as HTMLDetailsElement).open).toBe(true);
 const code=view.querySelector('.verification-code-input') as HTMLInputElement;code.value='123456';code.dispatchEvent(new Event('input',{bubbles:true}));await fixture.whenStable();fixture.detectChanges();(view.querySelector('.password-right-actions .password-primary-button') as HTMLButtonElement).click();await fixture.whenStable();fixture.detectChanges();expect(auth.verifyPasswordChangeCode).toHaveBeenCalledWith('123456');expect(view.querySelectorAll('.password-fields input')).toHaveLength(2);expect(fixture.componentInstance.passwordStep).toBe('PASSWORD');
 });
});
