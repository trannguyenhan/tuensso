import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, NavigationEnd, Router, RouterLink } from '@angular/router';
import { filter, finalize } from 'rxjs';
import { BootstrapResponse, ConsoleApiService, UserRow, SessionRow, AuditEntry, PageResponse, RoleRow } from '../services/console-api.service';
import { FormsModule } from '@angular/forms';
import { DatePipe } from '@angular/common';

@Component({
  selector: 'app-console-page',
  standalone: true,
  imports: [RouterLink, FormsModule, DatePipe],
  templateUrl: './console-page.component.html'
})
export class ConsolePageComponent {
  private readonly api = inject(ConsoleApiService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);
  private dismissTimer: ReturnType<typeof setTimeout> | null = null;

  readonly bootstrap = signal<BootstrapResponse | null>(null);
  readonly loading = signal(true);
  readonly message = signal<string | null>(null);
  readonly error = signal<string | null>(null);
  readonly currentSection = signal('apps');
  readonly confirmAction = signal<{ title: string; body: string; action: () => void } | null>(null);
  readonly sessions = signal<SessionRow[]>([]);
  readonly auditLog = signal<AuditEntry[]>([]);
  readonly roles = signal<RoleRow[]>([]);

  newRole = { name: '', description: '' };

  constructor() {
    this.router.events.pipe(
      filter((e): e is NavigationEnd => e instanceof NavigationEnd),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => { this.currentSection.set(this.sectionFromPath()); this.loadSectionData(this.sectionFromPath()); });

    this.currentSection.set(this.sectionFromPath());
    this.reload();
    this.loadSectionData(this.sectionFromPath());
  }

  private sectionFromPath(): string {
    const path = this.route.snapshot.routeConfig?.path ?? '';
    return path.replace('admin/', '');
  }

  private loadSectionData(section: string): void {
    if (section === 'sessions') this.api.getSessions().pipe(takeUntilDestroyed(this.destroyRef)).subscribe(d => this.sessions.set(d));
    if (section === 'audit') this.api.getAuditLog().pipe(takeUntilDestroyed(this.destroyRef)).subscribe(d => this.auditLog.set(d.content));
    if (section === 'roles') this.api.getRoles().pipe(takeUntilDestroyed(this.destroyRef)).subscribe(d => this.roles.set(d));
  }

  private showMessage(msg: string): void { this.message.set(msg); if (this.dismissTimer) clearTimeout(this.dismissTimer); this.dismissTimer = setTimeout(() => this.message.set(null), 4000); }
  private showError(msg: string): void { this.error.set(msg); if (this.dismissTimer) clearTimeout(this.dismissTimer); this.dismissTimer = setTimeout(() => this.error.set(null), 6000); }
  dismissMessage(): void { this.message.set(null); this.error.set(null); }

  requestConfirm(title: string, body: string, action: () => void): void { this.confirmAction.set({ title, body, action }); }
  executeConfirm(): void { this.confirmAction()?.action(); this.confirmAction.set(null); }
  cancelConfirm(): void { this.confirmAction.set(null); }

  reload(): void {
    this.loading.set(true);
    this.api.bootstrap().pipe(
      takeUntilDestroyed(this.destroyRef),
      finalize(() => this.loading.set(false))
    ).subscribe({
      next: (data) => this.bootstrap.set(data),
      error: () => this.showError('Unable to load console data.')
    });
  }

  toggleUser(user: UserRow): void {
    (user.enabled ? this.api.disableUser(user.id) : this.api.enableUser(user.id)).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => { this.showMessage(`User ${user.enabled ? 'disabled' : 'enabled'}: ${user.username}`); this.reload(); },
      error: (err) => this.showError(err.error?.message ?? 'Failed.')
    });
  }

  deleteUser(userId: string, username: string): void {
    this.requestConfirm('Delete user', `Delete "${username}"? This cannot be undone.`, () => {
      this.api.deleteUser(userId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
        next: () => { this.showMessage('User deleted.'); this.reload(); },
        error: (err) => this.showError(err.error?.message ?? 'Failed.')
      });
    });
  }

  deleteGroup(groupId: string, groupName: string): void {
    this.requestConfirm('Delete group', `Delete group "${groupName}"?`, () => {
      this.api.deleteGroup(groupId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
        next: () => { this.showMessage('Group deleted.'); this.reload(); },
        error: (err) => this.showError(err.error?.message ?? 'Failed.')
      });
    });
  }

  deleteApp(clientId: string, name: string): void {
    this.requestConfirm('Delete application', `Delete "${name}"? This cannot be undone.`, () => {
      this.api.deleteClient(clientId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
        next: () => { this.showMessage('Application deleted.'); this.reload(); },
        error: (err) => this.showError(err.error?.message ?? 'Failed.')
      });
    });
  }

  userGroupNames(user: UserRow): string { return user.groups.map(g => g.name).join(', ') || '-'; }

  // Sessions
  revokeSession(id: string): void {
    this.requestConfirm('Revoke session', 'Revoke this session?', () => {
      this.api.revokeSession(id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
        next: () => { this.showMessage('Session revoked.'); this.loadSectionData('sessions'); },
        error: (err) => this.showError(err.error?.message ?? 'Failed.')
      });
    });
  }

  revokeUserSessions(username: string): void {
    this.requestConfirm('Revoke all sessions', `Revoke all sessions for "${username}"?`, () => {
      this.api.revokeUserSessions(username).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
        next: () => { this.showMessage('All sessions revoked.'); this.loadSectionData('sessions'); },
        error: (err) => this.showError(err.error?.message ?? 'Failed.')
      });
    });
  }

  // Roles
  createRole(): void {
    if (!this.newRole.name) return;
    this.api.createRole(this.newRole.name, this.newRole.description).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => { this.newRole = { name: '', description: '' }; this.showMessage('Role created.'); this.loadSectionData('roles'); },
      error: (err) => this.showError(err.error?.message ?? 'Failed.')
    });
  }

  deleteRole(id: string, name: string): void {
    this.requestConfirm('Delete role', `Delete role "${name}"?`, () => {
      this.api.deleteRole(id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
        next: () => { this.showMessage('Role deleted.'); this.loadSectionData('roles'); },
        error: (err) => this.showError(err.error?.message ?? 'Failed.')
      });
    });
  }

}
