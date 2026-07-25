import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { ConsoleApiService } from '../services/console-api.service';
import { BootstrapService } from '../services/bootstrap.service';

@Component({
  selector: 'app-dashboard-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink],
  templateUrl: './dashboard-page.component.html'
})
export class DashboardPageComponent {
  private readonly destroyRef = inject(DestroyRef);
  private readonly bootstrapService = inject(BootstrapService);

  readonly bootstrap = this.bootstrapService.data;
  readonly loading = signal(true);

  constructor() {
    this.bootstrapService.get().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => this.loading.set(false),
      error: () => this.loading.set(false)
    });
  }

  get enabledUsers(): number { return this.bootstrap()?.users.filter(u => u.enabled).length ?? 0; }
  get disabledUsers(): number { return this.bootstrap()?.users.filter(u => !u.enabled).length ?? 0; }
}
