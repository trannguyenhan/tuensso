import { Component, DestroyRef, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ConsoleApiService, CsrfResponse } from '../services/console-api.service';

@Component({
  selector: 'app-admin-login-page',
  standalone: true,
  templateUrl: './admin-login-page.component.html'
})
export class AdminLoginPageComponent {
  private readonly api = inject(ConsoleApiService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  readonly csrf = signal<CsrfResponse | null>(null);
  readonly error = signal<string | null>(null);

  constructor() {
    this.api.session().pipe(takeUntilDestroyed(this.destroyRef)).subscribe(s => {
      if (s.authenticated && s.roles.includes('ROLE_ADMIN')) {
        void this.router.navigateByUrl('/admin/dashboard');
        return;
      }
      if (s.authenticated) {
        this.error.set('Your account does not have administrator privileges.');
        return;
      }
      this.api.csrf().pipe(takeUntilDestroyed(this.destroyRef)).subscribe(c => this.csrf.set(c));
    });
  }
}
