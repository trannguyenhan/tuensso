import { Component, DestroyRef, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router } from '@angular/router';
import { switchMap } from 'rxjs';
import { ConsoleApiService, CsrfResponse, ProfileResponse } from '../services/console-api.service';

@Component({
  selector: 'app-account-page',
  standalone: true,
  imports: [DatePipe, FormsModule],
  templateUrl: './account-page.component.html'
})
export class AccountPageComponent {
  private readonly api = inject(ConsoleApiService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  readonly profile = signal<ProfileResponse | null>(null);
  readonly csrf = signal<CsrfResponse | null>(null);
  readonly loading = signal(true);
  readonly message = signal<string | null>(null);
  readonly error = signal<string | null>(null);
  readonly isAdmin = signal(false);

  section = 'profile';
  editing = false;
  editForm = { email: '' };
  pwForm = { currentPassword: '', newPassword: '', confirmPassword: '' };

  constructor() {
    this.api.session().pipe(
      takeUntilDestroyed(this.destroyRef),
      switchMap(s => {
        if (!s.authenticated) void this.router.navigateByUrl('/login');
        this.isAdmin.set(s.roles.includes('ROLE_ADMIN'));
        return this.api.csrf();
      }),
      switchMap(c => { this.csrf.set(c); return this.api.profile(); })
    ).subscribe({
      next: (p) => { this.profile.set(p); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }

  changePassword(): void {
    if (this.pwForm.newPassword !== this.pwForm.confirmPassword) { this.showErr('Passwords do not match.'); return; }
    if (!this.pwForm.newPassword || this.pwForm.newPassword.length < 8) { this.showErr('Password must be at least 8 characters.'); return; }
    this.api.changeMyPassword(this.pwForm.currentPassword, this.pwForm.newPassword)
      .pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
        next: () => { this.pwForm = { currentPassword: '', newPassword: '', confirmPassword: '' }; this.showMsg('Password changed.'); },
        error: (err) => this.showErr(err.error?.message ?? 'Failed to change password.')
      });
  }

  startEdit(): void {
    this.editForm.email = this.profile()!.email;
    this.editing = true;
  }

  cancelEdit(): void { this.editing = false; }

  saveProfile(): void {
    if (!this.editForm.email || !this.editForm.email.includes('@')) { this.showErr('Invalid email.'); return; }
    this.api.updateMyProfile(this.editForm.email.trim())
      .pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
        next: (p) => { this.profile.set(p); this.editing = false; this.showMsg('Profile updated.'); },
        error: (err) => this.showErr(err.error?.message ?? 'Failed to update profile.')
      });
  }

  logout(): void {
    const csrf = this.csrf();
    if (!csrf) return;
    this.api.logout(csrf).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => void this.router.navigateByUrl('/login'),
      error: () => { window.location.href = '/login'; }
    });
  }

  private showMsg(msg: string): void { this.message.set(msg); this.error.set(null); setTimeout(() => this.message.set(null), 4000); }
  private showErr(msg: string): void { this.error.set(msg); this.message.set(null); setTimeout(() => this.error.set(null), 6000); }
}
