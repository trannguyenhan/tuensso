import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ConsoleApiService, GroupRow, UserRow } from '../services/console-api.service';

@Component({
  selector: 'app-group-detail-page',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './group-detail-page.component.html'
})
export class GroupDetailPageComponent {
  private readonly api = inject(ConsoleApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly isCreate: boolean;
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly message = signal<string | null>(null);
  readonly error = signal<string | null>(null);
  readonly group = signal<GroupRow | null>(null);
  readonly members = signal<UserRow[]>([]);
  readonly allUsers = signal<UserRow[]>([]);

  form = { name: '', description: '' };
  selectedUserId = '';

  constructor() {
    const id = this.route.snapshot.paramMap.get('id')!;
    this.isCreate = id === 'create';
    if (this.isCreate) { this.loading.set(false); }
    else { this.loadAll(id); }
  }

  private loadAll(groupId?: string): void {
    const gid = groupId ?? this.route.snapshot.paramMap.get('id')!;
    this.api.getGroup(gid).subscribe({
      next: (data) => { this.group.set(data); this.form.name = data.name; this.form.description = data.description || ''; this.loading.set(false); },
      error: () => { this.showErr('Group not found.'); this.loading.set(false); }
    });
    this.api.bootstrap().subscribe(data => {
      this.allUsers.set(data.users);
      this.members.set(data.users.filter(u => u.groups.some(g => g.id === gid)));
      const g = data.groups.find(g => g.id === gid);
      if (g) this.group.set(g);
    });
  }

  save(): void {
    this.saving.set(true);
    if (this.isCreate) {
      this.api.createGroup({ name: this.form.name.trim(), description: this.form.description.trim() }).subscribe({
        next: () => void this.router.navigateByUrl('/admin/groups'),
        error: (err) => { this.saving.set(false); this.showErr(err.error?.message ?? 'Create failed.'); }
      });
    } else {
      this.api.updateGroup(this.group()!.id, { name: this.form.name.trim(), description: this.form.description.trim() }).subscribe({
        next: () => { this.saving.set(false); this.showMsg('Saved.'); this.loadAll(); },
        error: (err) => { this.saving.set(false); this.showErr(err.error?.message ?? 'Save failed.'); }
      });
    }
  }

  deleteGroup(): void {
    const g = this.group(); if (!g || !confirm(`Delete group "${g.name}"?`)) return;
    this.api.deleteGroup(g.id).subscribe({ next: () => void this.router.navigateByUrl('/admin/groups'), error: (err) => this.showErr(err.error?.message ?? 'Failed.') });
  }

  nonMembers(): UserRow[] { const ids = new Set(this.members().map(m => m.id)); return this.allUsers().filter(u => !ids.has(u.id)); }

  addMember(): void {
    const g = this.group(); if (!g || !this.selectedUserId) return;
    this.api.addUserToGroup(g.id, this.selectedUserId).subscribe({ next: () => { this.selectedUserId = ''; this.showMsg('Member added.'); this.loadAll(); }, error: (err) => this.showErr(err.error?.message ?? 'Failed.') });
  }

  removeMember(userId: string): void {
    const g = this.group(); if (!g) return;
    this.api.removeUserFromGroup(g.id, userId).subscribe({ next: () => { this.showMsg('Member removed.'); this.loadAll(); }, error: (err) => this.showErr(err.error?.message ?? 'Failed.') });
  }

  private showMsg(msg: string): void { this.message.set(msg); this.error.set(null); setTimeout(() => this.message.set(null), 4000); }
  private showErr(msg: string): void { this.error.set(msg); this.message.set(null); setTimeout(() => this.error.set(null), 6000); }
}
