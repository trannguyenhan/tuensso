import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { ConsoleApiService, BootstrapResponse } from '../services/console-api.service';

@Component({
  selector: 'app-dashboard-page',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './dashboard-page.component.html'
})
export class DashboardPageComponent {
  private readonly api = inject(ConsoleApiService);
  private readonly destroyRef = inject(DestroyRef);

  readonly bootstrap = signal<BootstrapResponse | null>(null);
  readonly loading = signal(true);

  constructor() {
    this.api.bootstrap().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (data) => { this.bootstrap.set(data); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }

  get enabledUsers(): number { return this.bootstrap()?.users.filter(u => u.enabled).length ?? 0; }
  get disabledUsers(): number { return this.bootstrap()?.users.filter(u => !u.enabled).length ?? 0; }
}
