import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ConsoleApiService, ClientView, AssignedUser, UserRow } from '../services/console-api.service';

@Component({
  selector: 'app-detail-page',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './app-detail-page.component.html'
})
export class AppDetailPageComponent {
  private readonly api = inject(ConsoleApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly isCreate: boolean;
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly message = signal<string | null>(null);
  readonly error = signal<string | null>(null);
  readonly app = signal<ClientView | null>(null);
  readonly assignedUsers = signal<AssignedUser[]>([]);
  readonly allUsers = signal<UserRow[]>([]);
  selectedUserId = '';
  newUserForm = { username: '', email: '', password: '' };
  showCreateUser = false;

  form = { clientId: '', clientName: '', clientSecret: '', redirectUris: '', scopes: 'openid, profile, email', requirePkce: false, primaryColor: '#0f766e' };
  private pendingLogoFile: File | null = null;

  constructor() {
    const id = this.route.snapshot.paramMap.get('id')!;
    this.isCreate = id === 'create';
    if (this.isCreate) {
      this.form.clientId = this.genId();
      this.form.clientSecret = this.genSecret();
      this.form.redirectUris = 'http://localhost:3000/login/oauth2/code/tuensso';
      this.loading.set(false);
    } else {
      this.api.getClient(id).subscribe({
        next: (data) => {
          this.app.set(data);
          this.form.clientId = data.clientId;
          this.form.clientName = data.clientName || '';
          this.form.redirectUris = data.redirectUris.join('\n');
          this.form.scopes = data.scopes.join(', ');
          this.form.requirePkce = data.requirePkce;
          this.form.primaryColor = data.primaryColor || '#0f766e';
          this.loading.set(false);
        },
        error: () => { this.showErr('App not found.'); this.loading.set(false); }
      });
      this.loadAssignedUsers(id);
      this.api.bootstrap().subscribe(data => this.allUsers.set(data.users));
    }
  }

  private loadAssignedUsers(clientId: string): void {
    this.api.getClientUsers(clientId).subscribe(users => this.assignedUsers.set(users));
  }

  availableUsers(): UserRow[] {
    const assigned = new Set(this.assignedUsers().map(u => u.id));
    return this.allUsers().filter(u => !assigned.has(u.id));
  }

  addUser(): void {
    const a = this.app();
    if (!a || !this.selectedUserId) return;
    this.api.addClientUser(a.clientId, this.selectedUserId).subscribe({
      next: () => { this.selectedUserId = ''; this.showMsg('User added.'); this.loadAssignedUsers(a.clientId); },
      error: (err) => this.showErr(err.error?.message ?? 'Failed.')
    });
  }

  removeUser(userId: string): void {
    const a = this.app();
    if (!a) return;
    this.api.removeClientUser(a.clientId, userId).subscribe({
      next: () => { this.showMsg('User removed.'); this.loadAssignedUsers(a.clientId); },
      error: (err) => this.showErr(err.error?.message ?? 'Failed.')
    });
  }

  createUser(): void {
    const a = this.app();
    const f = this.newUserForm;
    if (!a || !f.email.trim()) { this.showErr('Email is required.'); return; }
    this.api.createAndAssignUser(a.clientId, {
      username: f.username.trim() || f.email.trim().split('@')[0],
      email: f.email.trim(),
      password: f.password || 'changeme'
    }).subscribe({
      next: () => {
        this.showMsg('User created and assigned.');
        this.newUserForm = { username: '', email: '', password: '' };
        this.showCreateUser = false;
        this.loadAssignedUsers(a.clientId);
        this.api.bootstrap().subscribe(data => this.allUsers.set(data.users));
      },
      error: (err) => this.showErr(err.error?.message ?? 'Failed.')
    });
  }

  get previewLogo(): string { return this.app()?.logoUrl || '/assets/default-app-logo.svg'; }
  get previewName(): string { return this.form.clientName || this.form.clientId || 'App'; }
  get previewColor(): string { return this.form.primaryColor || '#0f766e'; }

  save(): void {
    this.saving.set(true);
    const uris = this.form.redirectUris.split('\n').map(v => v.trim()).filter(Boolean);
    const scopes = this.form.scopes.split(',').map(v => v.trim()).filter(Boolean);
    if (this.isCreate) {
      this.api.createClient({ clientId: this.form.clientId.trim(), clientName: this.form.clientName.trim(), clientSecret: this.form.clientSecret, redirectUris: uris, scopes, requirePkce: this.form.requirePkce }).subscribe({
        next: () => void this.router.navigateByUrl('/admin/apps'),
        error: (err) => { this.saving.set(false); this.showErr(err.error?.message ?? 'Create failed.'); }
      });
    } else {
      this.api.updateClient(this.app()!.clientId, { clientName: this.form.clientName.trim(), redirectUris: uris, scopes, requirePkce: this.form.requirePkce, primaryColor: this.form.primaryColor }).subscribe({
        next: (updated) => { this.app.set(updated); this.saving.set(false); this.showMsg('Saved.'); },
        error: (err) => { this.saving.set(false); this.showErr(err.error?.message ?? 'Save failed.'); }
      });
    }
  }

  onLogoSelected(event: Event): void { this.pendingLogoFile = (event.target as HTMLInputElement).files?.[0] ?? null; }

  uploadLogo(): void {
    const a = this.app();
    if (!a || !this.pendingLogoFile) { this.showErr('Select a file first.'); return; }
    this.api.uploadClientLogo(a.clientId, this.pendingLogoFile).subscribe({
      next: (updated) => { this.app.set(updated); this.pendingLogoFile = null; this.showMsg('Logo uploaded.'); },
      error: (err) => this.showErr(err.error?.message ?? 'Upload failed.')
    });
  }

  copyToClipboard(value: string): void { navigator.clipboard.writeText(value).then(() => this.showMsg('Copied.')); }
  regenerateId(): void { this.form.clientId = this.genId(); }
  regenerateSecret(): void { this.form.clientSecret = this.genSecret(); }

  private genId(): string { const c = 'abcdefghijklmnopqrstuvwxyz0123456789'; let id = 'app-'; const a = crypto.getRandomValues(new Uint8Array(8)); for (const b of a) id += c[b % c.length]; return id; }
  private genSecret(): string { const a = crypto.getRandomValues(new Uint8Array(32)); return Array.from(a, b => b.toString(16).padStart(2, '0')).join(''); }
  private showMsg(msg: string): void { this.message.set(msg); this.error.set(null); setTimeout(() => this.message.set(null), 4000); }
  private showErr(msg: string): void { this.error.set(msg); this.message.set(null); setTimeout(() => this.error.set(null), 6000); }
}
