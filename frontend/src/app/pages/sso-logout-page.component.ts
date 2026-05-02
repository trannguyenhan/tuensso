import { Component, DestroyRef, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HttpClient } from '@angular/common/http';

interface LogoutValidation { valid: boolean; clientName: string | null; redirectUri: string | null; }
interface LogoutResult { success: boolean; redirectUri: string | null; }

@Component({
  selector: 'app-sso-logout-page',
  standalone: true,
  templateUrl: './sso-logout-page.component.html'
})
export class SsoLogoutPageComponent {
  private readonly http = inject(HttpClient);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);

  readonly clientName = signal<string | null>(null);
  readonly redirectUri = signal<string | null>(null);
  readonly valid = signal(false);
  readonly done = signal(false);
  readonly loading = signal(true);

  private clientId = '';
  private state = '';
  private rawRedirectUri = '';

  constructor() {
    this.route.queryParamMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(params => {
      this.clientId = params.get('client_id') || '';
      this.state = params.get('state') || '';
      this.rawRedirectUri = params.get('redirect_uri') || '';
      const redirect = this.rawRedirectUri;
      this.http.get<LogoutValidation>('/api/sso/logout/validate', {
        params: { client_id: this.clientId, redirect_uri: redirect, state: this.state }
      }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
        next: v => {
          this.valid.set(v.valid);
          this.clientName.set(v.clientName);
          this.redirectUri.set(v.redirectUri);
          this.loading.set(false);
        },
        error: () => this.loading.set(false)
      });
    });
  }

  confirmLogout(): void {
    this.http.post<LogoutResult>('/api/sso/logout', {
      clientId: this.clientId, redirectUri: this.rawRedirectUri, state: this.state
    }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
        next: r => {
          this.done.set(true);
          if (r.redirectUri) {
            setTimeout(() => window.location.href = r.redirectUri!, 1500);
          }
        },
        error: () => this.done.set(true)
      });
  }

  cancel(): void {
    // Only use server-validated redirect URI
    const redirect = this.redirectUri();
    if (redirect) {
      window.location.href = redirect;
    } else {
      window.location.href = '/';
    }
  }
}
