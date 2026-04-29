import { Component, DestroyRef, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ConsoleApiService, CsrfResponse, BrandingResponse } from '../services/console-api.service';

@Component({
  selector: 'app-sso-login-page',
  standalone: true,
  templateUrl: './sso-login-page.component.html'
})
export class SsoLoginPageComponent {
  private readonly api = inject(ConsoleApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);

  readonly csrf = signal<CsrfResponse | null>(null);
  readonly clientId = signal<string>('');
  readonly branding = signal<BrandingResponse | null>(null);

  constructor() {
    this.route.queryParamMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((params) => {
      const cid = params.get('client_id') || '';
      if (cid && /^[a-zA-Z0-9_-]+$/.test(cid)) {
        this.clientId.set(cid);
        this.api.branding(cid).pipe(takeUntilDestroyed(this.destroyRef)).subscribe(b => this.branding.set(b));
      }
    });

    this.api.csrf().pipe(takeUntilDestroyed(this.destroyRef)).subscribe(csrf => this.csrf.set(csrf));
  }

  get brandColor(): string {
    const c = this.branding()?.primaryColor || '#0f766e';
    return /^#[0-9a-fA-F]{6}$/.test(c) ? c : '#0f766e';
  }
  get brandLogo(): string | null { return this.branding()?.logoUrl || null; }
  get brandName(): string { return this.branding()?.clientName || this.clientId(); }
}
