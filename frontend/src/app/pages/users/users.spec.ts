import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { vi, describe, it, expect, beforeEach, afterEach } from 'vitest';
import { UsersWorkspace } from './users';
import { UserAdminService, AdminUser } from '../../core/users/user-admin.service';
import { AuthService } from '../../core/auth/auth.service';

const sample = (id:number,name:string,login:string,role='DOCUMENT_OPERATOR',enabled=true):AdminUser => ({userId:id,fullName:name,login,email:login+'@company.by',roleCode:role,roleName:role==='ADMIN'?'Администратор системы':role==='LAWYER'?'Юрист':'Делопроизводитель',enabled,createdAt:null,updatedAt:null,lastLoginAt:null});
const data=[sample(1,'Александрова Валерия Вячеславовна','admin','ADMIN'),sample(2,'Петрова Анна Сергеевна','operator'),sample(3,'Смирнов Алексей Олегович','lawyer','LAWYER'),sample(4,'Козлова Мария Игоревна','manager','CONTRACT_MANAGER'),sample(5,'Тестовый пользователь API','testuser091453150','LAWYER',false),sample(6,'Тестовая Мария Андреевна','ui_test_user','DOCUMENT_OPERATOR',false)];
const roles=[{id:1,code:'ADMIN',name:'Администратор системы'},{id:2,code:'DOCUMENT_OPERATOR',name:'Делопроизводитель'},{id:3,code:'LAWYER',name:'Юрист'},{id:4,code:'CONTRACT_MANAGER',name:'Менеджер по договорам'}];
describe('Users redesigned template interactions',()=>{
 let fixture:any,view:HTMLElement,api:any;
 beforeEach(async()=>{
 api={getUsers:vi.fn(()=>of(structuredClone(data))),getRoles:vi.fn(()=>of(roles)),createUser:vi.fn((r:any)=>of({...sample(7,r.fullName,r.login),...r})),updateRole:vi.fn((id:number,roleCode:string)=>of({...data.find(x=>x.userId===id)!,roleCode,roleName:'Юрист'})),updateEnabled:vi.fn((id:number,enabled:boolean)=>of({...data.find(x=>x.userId===id)!,enabled})),updateFullName:vi.fn((id:number,fullName:string)=>of({...data.find(x=>x.userId===id)!,fullName})),updateEmail:vi.fn((id:number,email:string)=>of({...data.find(x=>x.userId===id)!,email}))};
 await TestBed.configureTestingModule({imports:[UsersWorkspace],providers:[{provide:UserAdminService,useValue:api},{provide:AuthService,useValue:{getCurrentUser:()=>({userId:1})}}]}).compileComponents();
 fixture=TestBed.createComponent(UsersWorkspace);view=fixture.nativeElement;fixture.detectChanges();await fixture.whenStable();fixture.detectChanges();
 });
 afterEach(()=>vi.restoreAllMocks());
 const input=async(el:Element,value:string)=>{(el as HTMLInputElement).value=value;el.dispatchEvent(new Event('input',{bubbles:true}));await fixture.whenStable();fixture.detectChanges();};
 it('renders real bindings, current-account protection and glass artwork',async()=>{
 expect(view.querySelectorAll('tbody tr')).toHaveLength(6);
 expect((view.querySelector('.role-select') as HTMLSelectElement).disabled).toBe(true);
 expect(view.querySelectorAll('.current-user-label')).toHaveLength(1);
 expect(view.querySelector('.users-hero-art')?.getAttribute('src')).toBe('assets/users/users-glass-spheres.svg');
 
 });
 it('submits the five bound create fields',async()=>{
 for(const [name,value] of Object.entries({fullName:'Иванов Иван Иванович',login:'ivanov',email:'ivanov@company.by',password:'password123'}))await input(view.querySelector(`input[name=${name}]`)!,value);
 view.querySelector('form')!.dispatchEvent(new Event('submit',{bubbles:true,cancelable:true}));await fixture.whenStable();fixture.detectChanges();
 expect(api.createUser).toHaveBeenCalledWith({fullName:'Иванов Иван Иванович',login:'ivanov',email:'ivanov@company.by',password:'password123',roleCode:'DOCUMENT_OPERATOR'});
 expect(view.querySelectorAll('tbody tr')).toHaveLength(7);
 });
 it('filters rows through the search input',async()=>{
 await input(view.querySelector('.search-box input')!,'operator');expect(view.querySelectorAll('tbody tr')).toHaveLength(1);expect(view.querySelector('tbody')?.textContent).toContain('Петрова');
 });
 it('changes another user role through its select',async()=>{
 const select=view.querySelectorAll('.role-select')[1] as HTMLSelectElement;select.value='LAWYER';select.dispatchEvent(new Event('change',{bubbles:true}));await fixture.whenStable();fixture.detectChanges();expect(api.updateRole).toHaveBeenCalledWith(2,'LAWYER');
 });
 it('blocks and unblocks through buttons while protecting self',async()=>{
 vi.spyOn(window,'confirm').mockReturnValue(true);
 (view.querySelector('.state-button.block') as HTMLButtonElement).click();await fixture.whenStable();fixture.detectChanges();expect(api.updateEnabled).toHaveBeenCalledWith(2,false);
 (view.querySelector('.state-button.unblock') as HTMLButtonElement).click();await fixture.whenStable();fixture.detectChanges();expect(api.updateEnabled).toHaveBeenCalledWith(2,true);
 fixture.componentInstance.toggleEnabled(fixture.componentInstance.users[0]);expect(api.updateEnabled).toHaveBeenCalledTimes(2);
 });
 it('keeps inline full name and email editing',async()=>{
 (view.querySelectorAll('.edit-name-button')[1] as HTMLButtonElement).click();fixture.detectChanges();await fixture.whenStable();await input(view.querySelector('.name-edit input')!,'Петрова Анна Ивановна');(view.querySelector('.name-save') as HTMLButtonElement).click();await fixture.whenStable();fixture.detectChanges();expect(api.updateFullName).toHaveBeenCalledWith(2,'Петрова Анна Ивановна');
 (view.querySelectorAll('.edit-email-button')[1] as HTMLButtonElement).click();fixture.detectChanges();await fixture.whenStable();await input(view.querySelector('.email-edit input')!,'anna@company.by');(view.querySelector('.email-save') as HTMLButtonElement).click();await fixture.whenStable();fixture.detectChanges();expect(api.updateEmail).toHaveBeenCalledWith(2,'anna@company.by');
 });
});