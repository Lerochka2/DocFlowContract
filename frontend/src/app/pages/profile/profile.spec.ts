import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of } from 'rxjs';
import { vi, describe, it, expect, beforeEach, afterEach } from 'vitest';
import { ProfileWorkspace } from './profile';
import { AuthService } from '../../core/auth/auth.service';
import { ProfileService } from '../../core/profile/profile.service';
const user = {
  userId: 1,
  login: 'admin',
  fullName: 'Александрова Валерия Вячеславовна',
  email: 'admin@company.by',
  role: 'ADMIN',
};
describe('Profile workspace', () => {
  let fixture: any, view: HTMLElement, api: any, auth: any;
  beforeEach(async () => {
    localStorage.clear();
    auth = {
      getCurrentUser: () => user,
      me: () => of(user),
      updateCachedUser: vi.fn(),
      requestPasswordChangeCode: vi.fn(() =>
        of({ maskedEmail: 'a***@company.by', expiresInSeconds: 300, resendAvailableInSeconds: 60 }),
      ),
    };
    api = {
      updateFullName: vi.fn((fullName: string) => of({ ...user, fullName })),
      getReminders: () =>
        of([{ id: 4, text: 'Проверить договор', createdAt: '2026-10-03T10:00:00' }]),
      createReminder: vi.fn((text: string) =>
        of({ id: 5, text, createdAt: '2026-10-03T10:00:00' }),
      ),
      deleteReminder: vi.fn(() => of(undefined)),
    };
    await TestBed.configureTestingModule({
      imports: [ProfileWorkspace],
      providers: [
        { provide: AuthService, useValue: auth },
        { provide: ProfileService, useValue: api },
        { provide: Router, useValue: { navigate: vi.fn() } },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(ProfileWorkspace);
    view = fixture.nativeElement;
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  });
  afterEach(() => {
    fixture.destroy();
    localStorage.clear();
    vi.restoreAllMocks();
  });
  it('renders real account data and honest unavailable values', async () => {
    expect(view.querySelector('.hero-user')?.textContent).toContain(user.fullName);
    expect(view.querySelector('.work-profile')?.textContent).toContain('Не указано');
    expect(view.querySelector('input[name=accountEmail]')?.hasAttribute('readonly')).toBe(true);
  });
  it('saves supported name through API and extra fields locally per user', () => {
    const c = fixture.componentInstance;
    c.fullName = 'Иванов Иван';
    c.localProfile.phone = '+375 29 1234567';
    c.saveProfileDetails();
    expect(api.updateFullName).toHaveBeenCalledWith('Иванов Иван');
    expect(JSON.parse(localStorage.getItem('docflow.profile.1')!).phone).toBe('+375 29 1234567');
    expect(localStorage.getItem('docflow.profile.2')).toBeNull();
  });
  it('keeps server name saving when local storage is unavailable', () => {
    const c = fixture.componentInstance;
    c.fullName = 'Иванов Иван';
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new Error('quota');
    });
    c.saveProfileDetails();
    expect(api.updateFullName).toHaveBeenCalledWith('Иванов Иван');
    expect(c.localSaveError).toBeTruthy();
  });
  it('toggles completion separately from server deletion', () => {
    const c = fixture.componentInstance;
    c.toggleReminder(4);
    expect(c.isReminderCompleted(4)).toBe(true);
    expect(api.deleteReminder).not.toHaveBeenCalled();
    c.toggleReminder(4);
    expect(c.isReminderCompleted(4)).toBe(false);
  });
  it('rejects unsupported and oversized photo files', () => {
    const c = fixture.componentInstance;
    c.selectPhoto({
      target: { files: [new File(['x'], 'bad.svg', { type: 'image/svg+xml' })], value: 'bad' },
    } as unknown as Event);
    expect(c.photoError).toBeTruthy();
    expect(c.localProfile.photo).toBe('');
  });
  it('preserves adding reminders and email-code password change', async () => {
    const c = fixture.componentInstance;
    c.reminderText = 'Новая задача';
    c.addReminder();
    expect(api.createReminder).toHaveBeenCalledWith('Новая задача');
    (view.querySelector('.password-primary') as HTMLButtonElement).click();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(auth.requestPasswordChangeCode).toHaveBeenCalledOnce();
    expect(c.passwordStep).toBe('CODE');
  });
});
