import { Component, ElementRef, ViewChild, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';
import { AccessRequestsService, AccessRequestRecord, AccessDepartmentOption } from '../../core/access/access-requests.service';

@Component({selector:'app-access-requests',standalone:true,imports:[FormsModule,DatePipe],templateUrl:'./access-requests.html',styleUrl:'./access-requests.scss'})
export class AccessRequestsPage {
  readonly api=inject(AccessRequestsService);
  readonly requests=signal<AccessRequestRecord[]>([]);
  readonly loading=signal(false);
  readonly busy=signal(false);
  readonly error=signal('');
  readonly success=signal('');
  readonly selected=signal<AccessRequestRecord|null>(null);
  @ViewChild('detailDialog') dialog!:ElementRef<HTMLDialogElement>;
  filter='ALL'; mode:'view'|'approve'|'reject'='view'; login=''; roleCode='DOCUMENT_OPERATOR'; reason='';
  readonly filters=[{code:'ALL',name:'Все'},{code:'NEW',name:'Новые'},{code:'APPROVED',name:'Одобренные'},{code:'REJECTED',name:'Отклонённые'}];
  departmentOptions: AccessDepartmentOption[] = [];
  get matchingRole() {
    const request=this.selected();
    return this.departmentOptions.find(d=>d.department===request?.department)?.positions.find(p=>p.name===request?.position)?.roleCode;
  }
  constructor(){
    this.load();
    this.api.options().subscribe({next:options=>{this.departmentOptions=options;if(this.matchingRole)this.roleCode=this.matchingRole;},error:()=>{}});
  }
  get filtered(){return this.requests().filter(r=>this.filter==='ALL'||r.status===this.filter);}
  label(status:string){return this.filters.find(f=>f.code===status)?.name || status;}
  statusName(status:string){return ({NEW:'Новая',APPROVED:'Одобрена',REJECTED:'Отклонена'} as Record<string,string>)[status] || status;}
  load(){this.loading.set(true);this.api.list().pipe(finalize(()=>this.loading.set(false))).subscribe({next:r=>{this.requests.set(r);this.api.pendingCount.set(r.filter(x=>x.status==='NEW').length);},error:e=>this.error.set(e.error?.message||'Не удалось загрузить заявки.')});}
  open(r:AccessRequestRecord){this.selected.set(r);this.mode='view';this.login='';this.roleCode=this.matchingRole || 'DOCUMENT_OPERATOR';this.reason='';this.error.set('');this.dialog.nativeElement.showModal();}
  close(){if(!this.busy())this.dialog.nativeElement.close();}
  decide(){
    const r=this.selected();if(!r||this.busy())return;this.error.set('');
    if(this.mode==='approve'&&!/^[a-zA-Z0-9._-]{3,100}$/.test(this.login.trim())){this.error.set('Укажите логин: 3–100 латинских букв, цифр, точек, дефисов или подчёркиваний.');return;}
    if(this.mode==='reject'&&(this.reason.trim().length<3||this.reason.length>1000)){this.error.set('Укажите причину отказа (от 3 до 1000 символов).');return;}
    this.busy.set(true);
    const operation=this.mode==='approve'?this.api.approve(r.id,this.login.trim(),this.matchingRole || this.roleCode):this.api.reject(r.id,this.reason.trim());
    operation.pipe(finalize(()=>this.busy.set(false))).subscribe({next:updated=>{this.dialog.nativeElement.close();this.success.set(updated.status==='APPROVED'?'Заявка одобрена. Учётная запись создана, письмо передано почтовому серверу.':'Заявка отклонена. Письмо передано почтовому серверу.');this.load();},error:e=>this.error.set(e.error?.message||'Не удалось сохранить решение. Повторите позже.')});
  }
}
