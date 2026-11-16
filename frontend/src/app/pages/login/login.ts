import { ViewChild, ElementRef, ChangeDetectorRef } from '@angular/core';
import { AccessRequestsService, AccessDepartmentOption } from '../../core/access/access-requests.service';
import { inject as injectPreferences } from '@angular/core';
import { UiPreferencesService } from '../../core/settings/ui-preferences.service';
import {
  Component
} from '@angular/core';

import {
  FormsModule
} from '@angular/forms';

import {
  HttpErrorResponse
} from '@angular/common/http';

import {
  ActivatedRoute,
  Router
} from '@angular/router';

import {
  finalize
} from 'rxjs';

import {
  AuthService
} from '../../core/auth/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    FormsModule
  ],
  templateUrl: './login.html',
  styleUrl: './login.scss'
})
export class Login {
  readonly uiPreferences = injectPreferences(UiPreferencesService);

  readonly access = injectPreferences(AccessRequestsService);
  private readonly cdr = injectPreferences(ChangeDetectorRef);
  @ViewChild('accessDialog') accessDialog!: ElementRef<HTMLDialogElement>;
  @ViewChild('supportDialog') supportDialog!: ElementRef<HTMLDialogElement>;
  request = { fullName:'', email:'', department:'', position:'', comment:'' };
  departmentOptions: AccessDepartmentOption[] = [];
  optionsLoading = false;
  get positionOptions() {
    return this.departmentOptions.find(d => d.department === this.request.department)?.positions ?? [];
  }
  changeDepartment(): void {
    this.request.position = '';
    delete this.requestErrors['department'];
    delete this.requestErrors['position'];
  }
  requestErrors: Record<string,string> = {};
  requestError = '';
  requestBusy = false;
  requestId: number|null = null;
  supportEmail = '';
  supportError = '';
  readonly secureConnection = location.protocol === 'https:';
  openRequest(): void {
    this.requestId=null; this.requestError=''; this.requestErrors={};
    this.accessDialog.nativeElement.showModal();
    if(this.departmentOptions.length || this.optionsLoading) return;
    this.optionsLoading=true;
    this.access.options().pipe(finalize(()=>{this.optionsLoading=false;this.cdr.markForCheck();})).subscribe({
      next:options=>{this.departmentOptions=options;},
      error:()=>{this.requestError='Не удалось загрузить подразделения и должности. Закройте форму и откройте её повторно.';}
    });
  }
  closeRequest(): void { if(!this.requestBusy) this.accessDialog.nativeElement.close(); }
  openSupport(): void {
    this.supportDialog.nativeElement.showModal();
    this.access.support().subscribe({next:r=>{this.supportEmail=r.email;this.cdr.markForCheck();},error:()=>{this.supportError='Контакты сейчас недоступны. Обратитесь к администратору вашей организации.';this.cdr.markForCheck();}});
  }
  sendRequest(): void {
    if(this.requestBusy) return;
    this.requestErrors={}; this.requestError='';
    for(const key of ['fullName','email','department','position'] as const) {
      this.request[key]=this.request[key].trim();
      if(!this.request[key]) this.requestErrors[key]='Заполните обязательное поле.';
      else if(this.request[key].length>255) this.requestErrors[key]='Не более 255 символов.';
    }
    if(this.request.fullName && this.request.fullName.length<3) this.requestErrors['fullName']='Укажите ФИО (не менее 3 символов).';
    if(this.request.email && !/^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}$/i.test(this.request.email)) this.requestErrors['email']='Введён некорректный адрес электронной почты.';
    if(!this.departmentOptions.some(d=>d.department===this.request.department)) this.requestErrors['department']='Выберите подразделение из списка.';
    if(!this.positionOptions.some(p=>p.name===this.request.position)) this.requestErrors['position']='Выберите должность выбранного подразделения.';
    if(this.request.comment.length>1000) this.requestErrors['comment']='Не более 1000 символов.';
    if(Object.keys(this.requestErrors).length) return;
    this.requestBusy=true;
    this.access.submit(this.request).pipe(finalize(()=>{this.requestBusy=false;this.cdr.markForCheck();})).subscribe({
      next:r=>{this.requestId=r.requestId;this.request={fullName:'',email:'',department:'',position:'',comment:''};},
      error:e=>{this.requestError=e.error?.message || (e.status===429?'Слишком много заявок. Повторите через 15 минут.':'Не удалось отправить заявку. Попробуйте позже.');}
    });
  }

  loginValue = '';
  password = '';

  rememberMe = false;

  showPassword = false;
  loading = false;

  errorMessage = '';

  constructor(
    private readonly authService: AuthService,
    private readonly router: Router,
    private readonly route: ActivatedRoute
  ) {
  }

  submit(): void {

    this.errorMessage = '';

    const login = this.loginValue.trim();

    if (!login || !this.password) {

      this.errorMessage =
        'Введите логин и пароль.';

      return;
    }

    this.loading = true;

    this.authService
      .login(
        login,
        this.password,
        this.rememberMe
      )
      .pipe(
        finalize(() => {
          this.loading = false;
          this.cdr.markForCheck();
        })
      )
      .subscribe({
        next: () => {

          const returnUrl =
            this.route.snapshot.queryParamMap.get(
              'returnUrl'
            ) ?? this.uiPreferences.landingPage();

          void this.router.navigateByUrl(
            returnUrl
          );
        },

        error: (
          error: HttpErrorResponse
        ) => {

          if (
            error.status === 401 ||
            error.status === 400
          ) {

            this.errorMessage =
              'Неверный логин или пароль.';

            return;
          }

          if (error.status === 0) {

            this.errorMessage =
              'Не удалось подключиться к серверу DocFlow.';

            return;
          }

          this.errorMessage =
            'Не удалось выполнить вход. Попробуйте ещё раз.';
        }
      });
  }

  togglePassword(): void {
    this.showPassword = !this.showPassword;
  }
}