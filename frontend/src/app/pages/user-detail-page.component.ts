import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ConsoleApiService, UserRow, GroupRow, RoleRow, UserAttributeRow } from '../services/console-api.service';
import { DatePipe } from '@angular/common';

@Component({
  selector: 'app-user-detail-page',
  standalone: true,
  imports: [FormsModule, RouterLink, DatePipe],
  templateUrl: './user-detail-page.component.html'
})
export class UserDetailPageComponent {
  private readonly api = inject(ConsoleApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly isCreate: boolean;
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly message = signal<string | null>(null);
  readonly error = signal<string | null>(null);
  readonly user = signal<UserRow | null>(null);
  readonly allGroups = signal<GroupRow[]>([]);
  readonly allRoles = signal<RoleRow[]>([]);
  readonly userRoles = signal<RoleRow[]>([]);
  readonly attributes = signal<UserAttributeRow[]>([]);

  form = { username: '', email: '', password: '', firstName: '', lastName: '', phone: '', address: '' };
  newPassword = '';
  selectedGroupId = '';
  selectedRoleId = '';
  newAttr = { key: '', value: '' };

  constructor() {
    const id = this.route.snapshot.paramMap.get('id')!;
    this.isCreate = id === 'create';
    if (this.isCreate) { this.loading.set(false); }
    else {
      this.loadUser(id);
      this.api.bootstrap().subscribe(data => this.allGroups.set(data.groups));
      this.api.getRoles().subscribe(r => this.allRoles.set(r));
      this.api.getUserRoles(id).subscribe(r => this.userRoles.set(r));
      this.api.getUserAttributes(id).subscribe(a => this.attributes.set(a));
    }
  }

  private loadUser(userId: string): void {
    this.api.getUser(userId).subscribe({
      next: (data) => {
        this.user.set(data); this.form.username = data.username; this.form.email = data.email;
        this.form.firstName = data.firstName || ''; this.form.lastName = data.lastName || '';
        this.form.phone = data.phone || ''; this.form.address = data.address || '';
        this.loading.set(false);
      },
      error: () => { this.showErr('User not found.'); this.loading.set(false); }
    });
  }

  save(): void {
    this.saving.set(true);
    if (this.isCreate) {
      this.api.createUser({ username: this.form.username.trim(), email: this.form.email.trim(), password: this.form.password }).subscribe({
        next: () => void this.router.navigateByUrl('/admin/users'),
        error: (err) => { this.saving.set(false); this.showErr(err.error?.message ?? 'Create failed.'); }
      });
    } else {
      const u = this.user()!;
      this.api.updateUser(u.id, {
        username: this.form.username.trim(), email: this.form.email.trim(),
        firstName: this.form.firstName, lastName: this.form.lastName,
        phone: this.form.phone, address: this.form.address
      }).subscribe({
        next: () => { this.saving.set(false); this.showMsg('Saved.'); this.loadUser(u.id); },
        error: (err) => { this.saving.set(false); this.showErr(err.error?.message ?? 'Save failed.'); }
      });
    }
  }

  toggleEnabled(): void {
    const u = this.user(); if (!u) return;
    (u.enabled ? this.api.disableUser(u.id) : this.api.enableUser(u.id)).subscribe({
      next: () => { this.showMsg(u.enabled ? 'Disabled.' : 'Enabled.'); this.loadUser(u.id); },
      error: (err) => this.showErr(err.error?.message ?? 'Failed.')
    });
  }

  toggleLocked(): void {
    const u = this.user(); if (!u) return;
    (u.locked ? this.api.unlockUser(u.id) : this.api.lockUser(u.id)).subscribe({
      next: () => { this.showMsg(u.locked ? 'Unlocked.' : 'Locked.'); this.loadUser(u.id); },
      error: (err) => this.showErr(err.error?.message ?? 'Failed.')
    });
  }

  resetPassword(): void {
    const u = this.user(); if (!u || !this.newPassword.trim()) { this.showErr('Enter a new password.'); return; }
    this.api.resetPassword(u.id, this.newPassword.trim()).subscribe({
      next: () => { this.newPassword = ''; this.showMsg('Password updated.'); },
      error: (err) => this.showErr(err.error?.message ?? 'Failed.')
    });
  }

  deleteUser(): void {
    const u = this.user(); if (!u || !confirm(`Delete "${u.username}"? This cannot be undone.`)) return;
    this.api.deleteUser(u.id).subscribe({ next: () => void this.router.navigateByUrl('/admin/users'), error: (err) => this.showErr(err.error?.message ?? 'Failed.') });
  }

  availableGroups(): GroupRow[] {
    const u = this.user(); if (!u) return [];
    const joined = new Set(u.groups.map(g => g.id));
    return this.allGroups().filter(g => !joined.has(g.id));
  }

  addToGroup(): void {
    const u = this.user(); if (!u || !this.selectedGroupId) return;
    this.api.addUserToGroup(this.selectedGroupId, u.id).subscribe({ next: () => { this.showMsg('Group assigned.'); this.loadUser(u.id); }, error: (err) => this.showErr(err.error?.message ?? 'Failed.') });
  }

  removeFromGroup(groupId: string): void {
    const u = this.user(); if (!u) return;
    this.api.removeUserFromGroup(groupId, u.id).subscribe({ next: () => { this.showMsg('Removed from group.'); this.loadUser(u.id); }, error: (err) => this.showErr(err.error?.message ?? 'Failed.') });
  }

  availableRoles(): RoleRow[] {
    const assigned = new Set(this.userRoles().map(r => r.id));
    return this.allRoles().filter(r => !assigned.has(r.id));
  }

  assignRole(): void {
    const u = this.user(); if (!u || !this.selectedRoleId) return;
    this.api.assignRole(this.selectedRoleId, u.id).subscribe({
      next: () => { this.showMsg('Role assigned.'); this.api.getUserRoles(u.id).subscribe(r => this.userRoles.set(r)); },
      error: (err) => this.showErr(err.error?.message ?? 'Failed.')
    });
  }

  removeRole(roleId: string): void {
    const u = this.user(); if (!u) return;
    this.api.removeRole(roleId, u.id).subscribe({
      next: () => { this.showMsg('Role removed.'); this.api.getUserRoles(u.id).subscribe(r => this.userRoles.set(r)); },
      error: (err) => this.showErr(err.error?.message ?? 'Failed.')
    });
  }

  private showMsg(msg: string): void { this.message.set(msg); this.error.set(null); setTimeout(() => this.message.set(null), 4000); }
  private showErr(msg: string): void { this.error.set(msg); this.message.set(null); setTimeout(() => this.error.set(null), 6000); }

  addAttribute(): void {
    const u = this.user(); if (!u || !this.newAttr.key.trim()) return;
    this.api.setUserAttribute(u.id, this.newAttr.key.trim(), this.newAttr.value).subscribe({
      next: () => { this.newAttr = { key: '', value: '' }; this.api.getUserAttributes(u.id).subscribe(a => this.attributes.set(a)); },
      error: (err) => this.showErr(err.error?.message ?? 'Failed.')
    });
  }

  deleteAttribute(key: string): void {
    const u = this.user(); if (!u) return;
    this.api.deleteUserAttribute(u.id, key).subscribe({
      next: () => this.api.getUserAttributes(u.id).subscribe(a => this.attributes.set(a)),
      error: (err) => this.showErr(err.error?.message ?? 'Failed.')
    });
  }
}
