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
  private brandingRequestSeq = 0;

  readonly csrf = signal<CsrfResponse | null>(null);
  readonly clientId = signal<string>('');
  readonly sessionCode = signal<string>('');
  readonly tabId = signal<string>('');
  readonly hasError = signal(false);
  readonly branding = signal<BrandingResponse | null>(null);
  readonly brandingResolved = signal(false);
  readonly resolvedBrandLogo = signal<string | null>(null);

  constructor() {
    this.route.queryParamMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((params) => {
      const cid = params.get('client_id') || '';
      this.sessionCode.set(params.get('session_code') || '');
      this.tabId.set(params.get('tab_id') || '');
      this.hasError.set(params.has('error'));
      this.brandingResolved.set(false);
      this.branding.set(null);
      this.resolvedBrandLogo.set(null);

      if (cid && /^[a-zA-Z0-9_-]+$/.test(cid)) {
        const requestSeq = ++this.brandingRequestSeq;
        this.clientId.set(cid);
        this.api.branding(cid).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
          next: (b) => {
            if (requestSeq !== this.brandingRequestSeq) return;
            this.branding.set(b);
            this.preloadBrandLogo(b.logoUrl, requestSeq);
          },
          error: () => {
            if (requestSeq !== this.brandingRequestSeq) return;
            this.brandingResolved.set(true);
          }
        });
      } else {
        this.clientId.set('');
        this.brandingResolved.set(true);
      }
    });

    this.api.csrf().pipe(takeUntilDestroyed(this.destroyRef)).subscribe(csrf => this.csrf.set(csrf));
  }

  get brandColor(): string {
    const c = this.branding()?.primaryColor || '#0f766e';
    return /^#[0-9a-fA-F]{6}$/.test(c) ? c : '#0f766e';
  }
  get brandLogo(): string | null { return this.resolvedBrandLogo(); }
  get brandName(): string { return this.branding()?.clientName || this.clientId(); }

  private preloadBrandLogo(logoUrl: string | null, requestSeq: number): void {
    if (!logoUrl) {
      this.brandingResolved.set(true);
      return;
    }

    const image = new Image();
    image.onload = () => {
      if (requestSeq !== this.brandingRequestSeq) return;
      this.resolvedBrandLogo.set(logoUrl);
      this.brandingResolved.set(true);
    };
    image.onerror = () => {
      if (requestSeq !== this.brandingRequestSeq) return;
      this.resolvedBrandLogo.set(null);
      this.brandingResolved.set(true);
    };
    image.src = logoUrl;
  }
}
