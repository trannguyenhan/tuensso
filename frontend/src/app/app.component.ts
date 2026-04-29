import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter, switchMap } from 'rxjs';
import { ConsoleApiService, CsrfResponse, SessionResponse } from './services/console-api.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    @if (isPlainRoute()) {
      <router-outlet />
    } @else if (loading()) {
      <div class="screen-loader">Loading...</div>
    } @else if (accessDenied()) {
      <section class="console-screen">
        <article class="empty-state-card">
          <p class="eyebrow">Access denied</p>
          <h1>Administrator access required</h1>
          <p class="helper-copy">You can <a routerLink="/account">view your profile</a> instead.</p>
          <button type="button" (click)="logout()">Return to sign in</button>
        </article>
      </section>
    } @else {
      <section class="console-screen">
        <header class="console-mobile-topbar">
          <button class="menu-button" type="button" (click)="drawerOpen.set(true)">Menu</button>
          <span class="mobile-title">TuenSSO</span>
          <button class="ghost-action" type="button" (click)="logout()">Logout</button>
        </header>
        <div class="console-layout">
          <div class="console-overlay" [class.show]="drawerOpen()" (click)="drawerOpen.set(false)"></div>
          <aside class="console-sidebar" [class.open]="drawerOpen()">
            <div class="sidebar-brand">
              <p class="eyebrow">TuenSSO</p>
              <h1>Admin Console</h1>
            </div>
            <nav class="sidebar-nav">
              <a routerLink="/admin/dashboard" routerLinkActive="active" class="sidebar-link">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" class="sidebar-icon"><path d="M3.75 6A2.25 2.25 0 016 3.75h2.25A2.25 2.25 0 0110.5 6v2.25a2.25 2.25 0 01-2.25 2.25H6a2.25 2.25 0 01-2.25-2.25V6zm0 9.75A2.25 2.25 0 016 13.5h2.25a2.25 2.25 0 012.25 2.25V18a2.25 2.25 0 01-2.25 2.25H6A2.25 2.25 0 013.75 18v-2.25zm9.75-9.75A2.25 2.25 0 0115.75 3.75H18A2.25 2.25 0 0120.25 6v2.25A2.25 2.25 0 0118 10.5h-2.25a2.25 2.25 0 01-2.25-2.25V6zm0 9.75a2.25 2.25 0 012.25-2.25H18a2.25 2.25 0 012.25 2.25V18A2.25 2.25 0 0118 20.25h-2.25A2.25 2.25 0 0113.5 18v-2.25z"/></svg>
                Dashboard
              </a>
              <a routerLink="/admin/apps" routerLinkActive="active" [routerLinkActiveOptions]="{exact:true}" class="sidebar-link">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" class="sidebar-icon"><path d="M21 7.5l-9-5.25L3 7.5m18 0l-9 5.25m9-5.25v9l-9 5.25M3 7.5l9 5.25M3 7.5v9l9 5.25"/></svg>
                Applications
              </a>
              <a routerLink="/admin/users" routerLinkActive="active" [routerLinkActiveOptions]="{exact:true}" class="sidebar-link">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" class="sidebar-icon"><path d="M15 19.128a9.38 9.38 0 002.625.372 9.337 9.337 0 004.121-.952 4.125 4.125 0 00-7.533-2.493M15 19.128v-.003c0-1.113-.285-2.16-.786-3.07M15 19.128v.106A12.318 12.318 0 018.624 21c-2.331 0-4.512-.645-6.374-1.766l-.001-.109a6.375 6.375 0 0111.964-3.07M12 6.375a3.375 3.375 0 11-6.75 0 3.375 3.375 0 016.75 0zm8.25 2.25a2.625 2.625 0 11-5.25 0 2.625 2.625 0 015.25 0z"/></svg>
                Users
              </a>
              <a routerLink="/admin/groups" routerLinkActive="active" [routerLinkActiveOptions]="{exact:true}" class="sidebar-link">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" class="sidebar-icon"><path d="M18 18.72a9.094 9.094 0 003.741-.479 3 3 0 00-4.682-2.72m.94 3.198l.001.031c0 .225-.012.447-.037.666A11.944 11.944 0 0112 21c-2.17 0-4.207-.576-5.963-1.584A6.062 6.062 0 016 18.719m12 0a5.971 5.971 0 00-.941-3.197m0 0A5.995 5.995 0 0012 12.75a5.995 5.995 0 00-5.058 2.772m0 0a3 3 0 00-4.681 2.72 8.986 8.986 0 003.74.477m.94-3.197a5.971 5.971 0 00-.94 3.197M15 6.75a3 3 0 11-6 0 3 3 0 016 0zm6 3a2.25 2.25 0 11-4.5 0 2.25 2.25 0 014.5 0zm-13.5 0a2.25 2.25 0 11-4.5 0 2.25 2.25 0 014.5 0z"/></svg>
                Groups
              </a>
              <a routerLink="/admin/integration" routerLinkActive="active" class="sidebar-link">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" class="sidebar-icon"><path d="M13.19 8.688a4.5 4.5 0 011.242 7.244l-4.5 4.5a4.5 4.5 0 01-6.364-6.364l1.757-1.757m9.86-2.54a4.5 4.5 0 00-1.242-7.244l4.5-4.5a4.5 4.5 0 016.364 6.364l-1.757 1.757"/></svg>
                Integration
              </a>
              <a routerLink="/admin/roles" routerLinkActive="active" class="sidebar-link">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" class="sidebar-icon"><path d="M9 12.75L11.25 15 15 9.75m-3-7.036A11.959 11.959 0 013.598 6 11.99 11.99 0 003 9.749c0 5.592 3.824 10.29 9 11.623 5.176-1.332 9-6.03 9-11.622 0-1.31-.21-2.571-.598-3.751h-.152c-3.196 0-6.1-1.248-8.25-3.285z"/></svg>
                Roles
              </a>
              <a routerLink="/admin/sessions" routerLinkActive="active" class="sidebar-link">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" class="sidebar-icon"><path d="M12 6v6h4.5m4.5 0a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>
                Sessions
              </a>
              <a routerLink="/admin/audit" routerLinkActive="active" class="sidebar-link">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" class="sidebar-icon"><path d="M9 12h3.75M9 15h3.75M9 18h3.75m3 .75H18a2.25 2.25 0 002.25-2.25V6.108c0-1.135-.845-2.098-1.976-2.192a48.424 48.424 0 00-1.123-.08m-5.801 0c-.065.21-.1.433-.1.664 0 .414.336.75.75.75h4.5a.75.75 0 00.75-.75 2.25 2.25 0 00-.1-.664m-5.8 0A2.251 2.251 0 0113.5 2.25H15a2.25 2.25 0 012.15 1.586m-5.8 0c-.376.023-.75.05-1.124.08C9.095 4.01 8.25 4.973 8.25 6.108V8.25m0 0H4.875c-.621 0-1.125.504-1.125 1.125v11.25c0 .621.504 1.125 1.125 1.125h9.75c.621 0 1.125-.504 1.125-1.125V9.375c0-.621-.504-1.125-1.125-1.125H8.25z"/></svg>
                Audit Log
              </a>
              <a routerLink="/admin/docs" routerLinkActive="active" class="sidebar-link">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" class="sidebar-icon"><path d="M19.5 14.25v-2.625a3.375 3.375 0 00-3.375-3.375h-1.5A1.125 1.125 0 0113.5 7.125v-1.5a3.375 3.375 0 00-3.375-3.375H8.25m2.25 0H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 00-9-9z"/></svg>
                Documentation
              </a>
            </nav>
            <div class="sidebar-actions">
              <a class="sidebar-secondary-link" href="/api/oidc/endpoints" target="_blank" rel="noreferrer">Metadata JSON</a>
              <a routerLink="/account" class="sidebar-secondary-link">My Account</a>
            </div>
          </aside>
          <div class="console-body">
            <header class="console-topbar">
              <div class="topbar-left">
                <span class="topbar-greeting">Welcome back, <strong>{{ session()?.username }}</strong></span>
              </div>
              <div class="topbar-right">
                <a routerLink="/account" class="topbar-user">
                  <span class="topbar-avatar">{{ (session()?.username || 'A').charAt(0).toUpperCase() }}</span>
                  <span>{{ session()?.username }}</span>
                </a>
                <button type="button" class="ghost-action" (click)="logout()">Logout</button>
              </div>
            </header>
            <main class="console-main">
              <router-outlet />
            </main>
          </div>
        </div>
      </section>
    }
  `
})
export class AppComponent {
  private readonly api = inject(ConsoleApiService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  readonly session = signal<SessionResponse | null>(null);
  readonly csrf = signal<CsrfResponse | null>(null);
  readonly loading = signal(true);
  readonly drawerOpen = signal(false);
  readonly isPlainRoute = signal(false);
  readonly accessDenied = computed(() => {
    const s = this.session();
    if (!s?.authenticated) return false;
    // Only check admin access on /admin/* routes
    if (!this.isAdminRoute()) return false;
    return !s.roles.includes('ROLE_ADMIN');
  });

  private isAdminRoute(): boolean {
    return this.router.url.startsWith('/admin');
  }

  constructor() {
    this.updatePlainRoute(this.router.url);

    this.router.events.pipe(
      filter((e): e is NavigationEnd => e instanceof NavigationEnd),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(e => {
      this.updatePlainRoute(e.urlAfterRedirects);
      this.drawerOpen.set(false);
    });

    this.api.session().pipe(
      takeUntilDestroyed(this.destroyRef),
      switchMap(s => {
        this.session.set(s);
        if (!s.authenticated && !this.isPlainRoute()) {
          const target = this.isAdminRoute() ? '/admin/login' : '/login';
          void this.router.navigateByUrl(target);
        }
        return this.api.csrf();
      })
    ).subscribe({
      next: (c) => { this.csrf.set(c); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }

  private updatePlainRoute(url: string): void {
    const path = url.split('?')[0];
    const plain = !url.startsWith('/admin') || path === '/admin/login';
    this.isPlainRoute.set(plain);
  }

  logout(): void {
    const csrf = this.csrf();
    if (!csrf) return;
    this.api.logout(csrf).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => void this.router.navigateByUrl('/login'),
      error: () => { window.location.href = '/login'; }
    });
  }
}
