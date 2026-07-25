import { Injectable, inject, signal } from '@angular/core';
import { Observable, shareReplay, tap } from 'rxjs';
import { ConsoleApiService, BootstrapResponse } from './console-api.service';

@Injectable({ providedIn: 'root' })
export class BootstrapService {
  private readonly api = inject(ConsoleApiService);
  private cached$?: Observable<BootstrapResponse>;
  private lastFetch = 0;
  private readonly CACHE_TTL = 30_000;

  readonly data = signal<BootstrapResponse | null>(null);

  get(force = false): Observable<BootstrapResponse> {
    const now = Date.now();
    if (!force && this.cached$ && (now - this.lastFetch) < this.CACHE_TTL) {
      return this.cached$;
    }
    this.lastFetch = now;
    this.cached$ = this.api.bootstrap().pipe(
      tap(d => this.data.set(d)),
      shareReplay(1)
    );
    return this.cached$;
  }

  invalidate(): void {
    this.cached$ = undefined;
    this.lastFetch = 0;
  }

  invalidateAndFetch(): Observable<BootstrapResponse> {
    this.invalidate();
    return this.get(true);
  }
}
