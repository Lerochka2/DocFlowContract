import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { Login } from './login';
import { AccessRequestsService } from '../../core/access/access-requests.service';
import { AuthService } from '../../core/auth/auth.service';
import { UiPreferencesService } from '../../core/settings/ui-preferences.service';

const options = [
  { department: 'Юридический отдел', positions: [{ name: 'Юрист', roleCode: 'LAWYER' }] },
  { department: 'Отдел делопроизводства', positions: [{ name: 'Делопроизводитель', roleCode: 'DOCUMENT_OPERATOR' }] }
];
describe('Access request department and position', () => {
  let api: { options: ReturnType<typeof vi.fn>; submit: ReturnType<typeof vi.fn> };
  beforeEach(() => {
    api = { options: vi.fn(() => of(options)), submit: vi.fn(() => of({ requestId: 12 })) };
    TestBed.configureTestingModule({ imports: [Login], providers: [provideRouter([]),
      { provide: AccessRequestsService, useValue: api },
      { provide: AuthService, useValue: {} }, { provide: UiPreferencesService, useValue: {} }
    ] });
  });
  it('offers only positions from the selected department and resets the old position', async () => {
    const fixture = TestBed.createComponent(Login); fixture.detectChanges();
    const component = fixture.componentInstance;
    component.accessDialog.nativeElement.showModal = vi.fn(); component.openRequest();
    component.request.department = 'Юридический отдел'; fixture.detectChanges(); await fixture.whenStable();
    const select = fixture.nativeElement.querySelector('#request-position') as HTMLSelectElement;
    expect(Array.from(select.options).map(o => o.value)).toEqual(['', 'Юрист']);
    component.request.position = 'Юрист';
    const department = fixture.nativeElement.querySelector('#request-dept') as HTMLSelectElement;
    department.value = 'Отдел делопроизводства'; department.dispatchEvent(new Event('change'));
    await fixture.whenStable(); fixture.detectChanges();
    expect(component.request.position).toBe('');
    expect(Array.from(select.options).map(o => o.value)).toEqual(['', 'Делопроизводитель']);
  });
  it('rejects an incompatible position before sending the request', () => {
    const fixture = TestBed.createComponent(Login); fixture.detectChanges();
    const c = fixture.componentInstance; c.accessDialog.nativeElement.showModal = vi.fn(); c.openRequest();
    c.request = { fullName: 'Иванов Иван Иванович', email: 'ivan@example.invalid',
      department: 'Юридический отдел', position: 'Делопроизводитель', comment: '' };
    c.sendRequest(); expect(api.submit).not.toHaveBeenCalled(); expect(c.requestErrors['position']).toBeTruthy();
  });
});
