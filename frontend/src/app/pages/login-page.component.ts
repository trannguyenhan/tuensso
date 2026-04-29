import { Component, DestroyRef, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ConsoleApiService, CsrfResponse } from '../services/console-api.service';

@Component({
  selector: 'app-login-page',
  standalone: true,
  templateUrl: './login-page.component.html'
})
export class LoginPageComponent {
  private readonly api = inject(ConsoleApiService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  readonly csrf = signal<CsrfResponse | null>(null);

  constructor() {
    this.api.session().pipe(takeUntilDestroyed(this.destroyRef)).subscribe((session) => {
      if (session.authenticated) {
        void this.router.navigateByUrl(session.roles.includes('ROLE_ADMIN') ? '/admin/dashboard' : '/account');
        return;
      }
      this.api.csrf().pipe(takeUntilDestroyed(this.destroyRef)).subscribe((csrf) => this.csrf.set(csrf));
    });
  }
}
