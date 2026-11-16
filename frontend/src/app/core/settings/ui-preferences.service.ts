import { Injectable, computed, inject, signal } from '@angular/core';
import { AuthService } from '../auth/auth.service';
export interface UiPreferences {
  showSearchShortcut: boolean;
  compactMode: boolean;
  showNotificationBadge: boolean;
  unreadNotificationsOnly: boolean;
  rememberLastPage: boolean;
  defaultPage: string;
  openAfterUpload: boolean;
  highlightReview: boolean;
  sortRisks: boolean;
  highlightHighRisks: boolean;
  confirmApproval: boolean;
}
export const DEFAULT_UI_PREFERENCES: UiPreferences = {
  showSearchShortcut: true, compactMode: false,
  showNotificationBadge: true, unreadNotificationsOnly: false,
  rememberLastPage: false, defaultPage: '/home',
  openAfterUpload: true, highlightReview: true,
  sortRisks: true, highlightHighRisks: true, confirmApproval: true
};
@Injectable({ providedIn: 'root' })
export class UiPreferencesService {
  private readonly auth = inject(AuthService);
  private activeUser = '';
  private readonly state = signal<UiPreferences>({ ...DEFAULT_UI_PREFERENCES });
  readonly preferences = this.state.asReadonly();
  readonly showSearchShortcut = computed(() => this.state().showSearchShortcut);
  constructor() { this.activateUser(); }
  activateUser(): void {
    const id = String(this.auth.getCurrentUser()?.userId ?? 'guest');
    if (id === this.activeUser) return;
    this.activeUser = id;
    try { this.state.set(this.normalize(JSON.parse(localStorage.getItem(this.key) ?? '{}'))); }
    catch { this.state.set({ ...DEFAULT_UI_PREFERENCES }); }
  }
  private get key(): string { return `docflow.ui.preferences.${this.activeUser}`; }
  get availablePages(): { path: string; label: string }[] {
    const role = this.auth.getCurrentUser()?.role;
    const pages = [
      { path: '/home', label: 'Главная' }, { path: '/dashboard', label: 'Дашборд' },
      { path: '/documents', label: 'Документы' }
    ];
    if (role === 'ADMIN' || role === 'DOCUMENT_OPERATOR') pages.push(
      { path: '/review', label: 'Ручная проверка' }, { path: '/ai-analysis', label: 'AI-анализ' }, { path: '/sed', label: 'СЭД' });
    if (role === 'ADMIN' || role === 'LAWYER' || role === 'CONTRACT_MANAGER') pages.push(
      { path: '/routing', label: role === 'ADMIN' ? 'Маршрутизация' : 'На согласовании' });
    if (role === 'ADMIN') pages.push({ path: '/users', label: 'Пользователи' });
    return pages;
  }
  private normalize(raw: Partial<UiPreferences> | null): UiPreferences {
    const result = { ...DEFAULT_UI_PREFERENCES };
    if (!raw || typeof raw !== 'object') return result;
    for (const key of Object.keys(result) as (keyof UiPreferences)[]) {
      if (key !== 'defaultPage' && typeof raw[key] === 'boolean') result[key] = raw[key] as boolean;
    }
    if (this.availablePages.some(p => p.path === raw.defaultPage)) result.defaultPage = raw.defaultPage!;
    return result;
  }
  updatePreferences(value: Partial<UiPreferences>): void {
    this.activateUser();
    const result = this.normalize({ ...this.state(), ...value });
    localStorage.setItem(this.key, JSON.stringify(result));
    this.state.set(result);
    if (!result.rememberLastPage) localStorage.removeItem(`${this.key}.lastPage`);
  }
  resetPreferences(): void { this.updatePreferences({ ...DEFAULT_UI_PREFERENCES }); }
  rememberPage(url: string): void {
    if (!this.state().rememberLastPage) return;
    const page = url.split(/[?#]/)[0];
    if (!this.availablePages.some(p => p.path === page)) return;
    try { localStorage.setItem(`${this.key}.lastPage`, page); } catch { }
  }
  landingPage(): string {
    this.activateUser();
    if (this.state().rememberLastPage) {
      try {
        const page = localStorage.getItem(`${this.key}.lastPage`);
        if (this.availablePages.some(p => p.path === page)) return page!;
      } catch { }
    }
    return this.state().defaultPage;
  }
}
