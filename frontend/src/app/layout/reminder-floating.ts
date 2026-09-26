import {
  ChangeDetectorRef,
  Component,
  OnDestroy,
  OnInit
} from '@angular/core';

import {
  NavigationEnd,
  Router
} from '@angular/router';

import {
  Subject,
  filter,
  takeUntil
} from 'rxjs';

import {
  ProfileService,
  UserReminder
} from '../core/profile/profile.service';


@Component({
  selector: 'app-reminder-floating',
  standalone: true,
  imports: [],
  templateUrl: './reminder-floating.html',
  styleUrl: './reminder-floating.scss'
})
export class ReminderFloatingComponent
  implements OnInit, OnDestroy {

  reminders:
    UserReminder[] = [];

  private readonly destroySubject =
    new Subject<void>();


  constructor(
    private readonly profileService:
      ProfileService,

    private readonly router:
      Router,

    private readonly changeDetectorRef:
      ChangeDetectorRef
  ) {
  }


  ngOnInit(): void {

    this.refresh();

    this.router.events
      .pipe(
        filter(
          event =>
            event instanceof NavigationEnd
        ),
        takeUntil(
          this.destroySubject
        )
      )
      .subscribe(
        () => {
          this.refresh();
        }
      );
  }


  ngOnDestroy(): void {

    this.destroySubject.next();

    this.destroySubject.complete();
  }


  get visible(): boolean {

    return (
      this.isHome()
      &&
      this.reminders.length > 0
    );
  }


  get visibleReminders():
    UserReminder[] {

    return this.reminders.slice(
      0,
      3
    );
  }


  get hiddenCount(): number {

    return Math.max(
      0,
      this.reminders.length - 3
    );
  }


  openProfile(): void {

    void this.router.navigate(
      ['/profile']
    );
  }


  formatDate(
    value: string
  ): string {

    const date =
      new Date(value);

    if (
      Number.isNaN(
        date.getTime()
      )
    ) {
      return '';
    }

    return new Intl.DateTimeFormat(
      'ru-RU',
      {
        day: '2-digit',
        month: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
      }
    ).format(date);
  }


  private refresh(): void {

    if (!this.isHome()) {

      this.reminders =
        [];

      return;
    }

    this.profileService
      .getReminders()
      .subscribe({

        next: reminders => {

          this.reminders =
            reminders;

          this.changeDetectorRef
            .markForCheck();
        },

        error: () => {

          this.reminders =
            [];

          this.changeDetectorRef
            .markForCheck();
        }
      });
  }


  private isHome(): boolean {

    if (this.router.url === '/home') {
      return true;
    }

    return this.router.url.startsWith(
      '/home?'
    );
  }
}