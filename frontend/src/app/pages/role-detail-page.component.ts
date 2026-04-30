import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ConsoleApiService, RoleRow, AssignedUser, UserRow } from '../services/console-api.service';

@Component({
  selector: 'app-role-detail-page',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './role-detail-page.component.html'
})
export class RoleDetailPageComponent {
  private readonly api = inject(ConsoleApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly isCreate: boolean;
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly message = signal<string | null>(null);
  readonly error = signal<string | null>(null);
  readonly role = signal<RoleRow | null>(null);
  readonly members = signal<AssignedUser[]>([]);
  readonly allUsers = signal<UserRow[]>([]);

  form = { name: '', description: '' };
  selectedUserId = '';

  constructor() {
    const id = this.route.snapshot.paramMap.get('id')!;
    this.isCreate = id === 'create';
    if (this.isCreate) { this.loading.set(false); }
    else {
      this.api.getRole(id).subscribe({
        next: r => { this.role.set(r); this.form.name = r.name; this.form.description = r.description || ''; this.loading.set(false); },
        error: () => this.loading.set(false)
      });
      this.api.getRoleMembers(id).subscribe(m => this.members.set(m));
      this.api.bootstrap().subscribe(d => this.allUsers.set(d.users));
    }
  }

  save(): void {
    this.saving.set(true);
    if (this.isCreate) {
      this.api.createRole(this.form.name, this.form.description).subscribe({
        next: () => void this.router.navigateByUrl('/admin/roles'),
        error: err => { this.saving.set(false); this.showErr(err.error?.message ?? 'Failed.'); }
      });
    }
  }

  deleteRole(): void {
    const r = this.role(); if (!r || !confirm(`Delete role "${r.name}"?`)) return;
    this.api.deleteRole(r.id).subscribe({ next: () => void this.router.navigateByUrl('/admin/roles') });
  }

  nonMembers(): UserRow[] {
    const ids = new Set(this.members().map(m => m.id));
    return this.allUsers().filter(u => !ids.has(u.id));
  }

  addMember(): void {
    const r = this.role(); if (!r || !this.selectedUserId) return;
    this.api.assignRole(r.id, this.selectedUserId).subscribe({
      next: () => { this.selectedUserId = ''; this.showMsg('User added.'); this.api.getRoleMembers(r.id).subscribe(m => this.members.set(m)); },
      error: err => this.showErr(err.error?.message ?? 'Failed.')
    });
  }

  removeMember(userId: string): void {
    const r = this.role(); if (!r) return;
    this.api.removeRole(r.id, userId).subscribe({
      next: () => { this.showMsg('User removed.'); this.api.getRoleMembers(r.id).subscribe(m => this.members.set(m)); },
      error: err => this.showErr(err.error?.message ?? 'Failed.')
    });
  }

  private showMsg(msg: string): void { this.message.set(msg); this.error.set(null); setTimeout(() => this.message.set(null), 4000); }
  private showErr(msg: string): void { this.error.set(msg); this.message.set(null); setTimeout(() => this.error.set(null), 6000); }
}
