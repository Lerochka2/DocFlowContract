import {
  Component,
  Input
} from '@angular/core';

@Component({
  selector: 'app-contract-expiration-card',
  standalone: true,
  imports: [],
  templateUrl: './contract-expiration-card.html',
  styleUrl: './contract-expiration-card.scss'
})
export class ContractExpirationCardComponent {

  @Input()
  status: string | null = null;

  @Input()
  daysUntilExpiration: number | null = null;

  @Input()
  validTo: string | null = null;


  get statusLabel(): string {

    switch (this.status) {

      case 'ACTIVE':
        return 'Действует';

      case 'EXPIRING_SOON':
        return 'Скоро истекает';

      case 'EXPIRED':
        return 'Срок истёк';

      case 'NOT_SPECIFIED':
        return 'Срок не указан';

      default:
        return 'Нет данных';
    }
  }


  get statusClass(): string {

    switch (this.status) {

      case 'ACTIVE':
        return 'active';

      case 'EXPIRING_SOON':
        return 'expiring';

      case 'EXPIRED':
        return 'expired';

      default:
        return 'unspecified';
    }
  }


  get message(): string {

    const days =
      this.daysUntilExpiration;

    switch (this.status) {

      case 'ACTIVE':

        if (
          days === null
          ||
          days === undefined
        ) {
          return 'Договор действует.';
        }

        return `До окончания срока действия: ${days} ${this.getDaysWord(days)}.`;

      case 'EXPIRING_SOON':

        if (days === 0) {
          return 'Срок действия договора истекает сегодня.';
        }

        if (
          days === null
          ||
          days === undefined
        ) {
          return 'Срок действия договора скоро истекает.';
        }

        return `До окончания срока действия: ${days} ${this.getDaysWord(days)}.`;

      case 'EXPIRED':

        if (
          days === null
          ||
          days === undefined
        ) {
          return 'Срок действия договора истёк.';
        }

        const daysAgo =
          Math.abs(days);

        return `Срок действия истёк ${daysAgo} ${this.getDaysWord(daysAgo)} назад.`;

      case 'NOT_SPECIFIED':
        return 'Дата окончания срока действия не указана.';

      default:
        return 'Нет данных о сроке действия договора.';
    }
  }


  formatDate(
    value: string | null
  ): string {

    if (!value) {
      return '—';
    }

    const match =
      /^(\d{4})-(\d{2})-(\d{2})/
        .exec(value);

    if (!match) {
      return value;
    }

    return `${match[3]}.${match[2]}.${match[1]}`;
  }


  private getDaysWord(
    value: number
  ): string {

    const absolute =
      Math.abs(value);

    const lastTwo =
      absolute % 100;

    const last =
      absolute % 10;

    if (
      lastTwo >= 11
      &&
      lastTwo <= 14
    ) {
      return 'дней';
    }

    if (last === 1) {
      return 'день';
    }

    if (
      last >= 2
      &&
      last <= 4
    ) {
      return 'дня';
    }

    return 'дней';
  }
}