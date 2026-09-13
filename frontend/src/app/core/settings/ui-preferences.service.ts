import {
  Injectable,
  computed,
  signal
} from '@angular/core';

export interface UiPreferences {
  showAiPromoCard: boolean;
  showSearchShortcut: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class UiPreferencesService {

  private readonly storageKey =
    'docflow.ui.preferences';

  private readonly defaultPreferences:
    UiPreferences = {
      showAiPromoCard: true,
      showSearchShortcut: true
    };

  private readonly preferencesSignal =
    signal<UiPreferences>(
      this.loadPreferences()
    );

  readonly preferences =
    this.preferencesSignal.asReadonly();

  readonly showAiPromoCard =
    computed(
      () =>
        this.preferencesSignal()
          .showAiPromoCard
    );

  readonly showSearchShortcut =
    computed(
      () =>
        this.preferencesSignal()
          .showSearchShortcut
    );

  updatePreferences(
    preferences: UiPreferences
  ): void {

    const normalized:
      UiPreferences = {
        showAiPromoCard:
          preferences.showAiPromoCard,

        showSearchShortcut:
          preferences.showSearchShortcut
      };

    this.preferencesSignal.set(
      normalized
    );

    localStorage.setItem(
      this.storageKey,
      JSON.stringify(
        normalized
      )
    );
  }

  resetPreferences(): void {

    const defaults = {
      ...this.defaultPreferences
    };

    this.preferencesSignal.set(
      defaults
    );

    localStorage.setItem(
      this.storageKey,
      JSON.stringify(
        defaults
      )
    );
  }

  private loadPreferences():
    UiPreferences {

    try {

      const raw =
        localStorage.getItem(
          this.storageKey
        );

      if (!raw) {

        return {
          ...this.defaultPreferences
        };
      }

      const parsed =
        JSON.parse(
          raw
        ) as Partial<UiPreferences>;

      return {
        showAiPromoCard:
          typeof parsed.showAiPromoCard
          === 'boolean'
            ? parsed.showAiPromoCard
            : true,

        showSearchShortcut:
          typeof parsed.showSearchShortcut
          === 'boolean'
            ? parsed.showSearchShortcut
            : true
      };

    } catch {

      return {
        ...this.defaultPreferences
      };
    }
  }
}