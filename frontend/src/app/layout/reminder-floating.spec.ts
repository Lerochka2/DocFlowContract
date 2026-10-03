import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, Subject } from 'rxjs';
import { describe, it, expect, vi } from 'vitest';
import { ReminderFloatingComponent } from './reminder-floating';
import { ProfileService } from '../core/profile/profile.service';
describe('Floating reminders', () => {
  it('keeps real notes, hidden count and profile navigation', async () => {
    const notes = [1, 2, 3, 4].map((id) => ({
      id,
      text: 'Проверить договор ' + id,
      createdAt: '2026-10-03T18:57:00',
    }));
    const router = { url: '/home', events: new Subject(), navigate: vi.fn() };
    await TestBed.configureTestingModule({
      imports: [ReminderFloatingComponent],
      providers: [
        { provide: ProfileService, useValue: { getReminders: () => of(notes) } },
        { provide: Router, useValue: router },
      ],
    }).compileComponents();
    const fixture = TestBed.createComponent(ReminderFloatingComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    const view = fixture.nativeElement as HTMLElement;
    expect(view.querySelectorAll('.floating-list article')).toHaveLength(3);
    expect(view.querySelector('footer')?.textContent).toContain('Ещё 1');
    (view.querySelector('footer button') as HTMLButtonElement).click();
    expect(router.navigate).toHaveBeenCalledWith(['/profile']);

    fixture.destroy();
  });
});
