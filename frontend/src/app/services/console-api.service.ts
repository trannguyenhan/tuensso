import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export interface CsrfResponse {
  headerName: string;
  parameterName: string;
  token: string;
}

export interface SessionResponse {
  authenticated: boolean;
  username: string | null;
  roles: string[];
  clientId: string | null;
}

export interface ClientView {
  id: string;
  clientId: string;
  clientName: string;
  redirectUris: string[];
  scopes: string[];
  requirePkce: boolean;
  logoUrl: string | null;
  primaryColor: string | null;
}

export interface BrandingResponse {
  clientName: string | null;
  logoUrl: string | null;
  primaryColor: string | null;
}

export interface AssignedUser {
  id: string;
  username: string;
  email: string;
  enabled: boolean;
}

export interface GroupRef {
  id: string;
  name: string;
}

export interface UserRow {
  id: string;
  username: string;
  email: string;
  firstName: string | null;
  lastName: string | null;
  phone: string | null;
  address: string | null;
  enabled: boolean;
  locked: boolean;
  failedLoginAttempts: number;
  lockedUntil: string | null;
  lastLoginAt: string | null;
  lastLoginIp: string | null;
  passwordChangedAt: string | null;
  groups: GroupRef[];
}

export interface GroupRow {
  id: string;
  name: string;
  description: string;
  memberCount: number;
}

export interface BootstrapResponse {
  issuer: string;
  apps: ClientView[];
  users: UserRow[];
  groups: GroupRow[];
}

export interface ProfileResponse {
  id: string;
  username: string;
  email: string;
  enabled: boolean;
  groups: string[];
  createdAt: string;
}

export interface SessionRow {
  id: string; username: string; grantType: string;
  issuedAt: string; expiresAt: string | null; clientId: string | null; expired: boolean;
}

export interface AuditEntry {
  id: string; eventType: string; username: string;
  targetType: string; targetId: string; detail: string;
  ipAddress: string; createdAt: string;
}

export interface PageResponse<T> {
  content: T[]; totalElements: number; totalPages: number; number: number;
}

export interface RoleRow {
  id: string; name: string; description: string; createdAt: string;
}

export interface ScopeRow {
  id: string; name: string; description: string;
  claimName: string; claimValue: string; createdAt: string;
}

export interface UserAttributeRow {
  id: string; userId: string; key: string; value: string;
}

@Injectable({ providedIn: 'root' })
export class ConsoleApiService {
  private readonly http = inject(HttpClient);

  csrf(): Observable<CsrfResponse> {
    return this.http.get<CsrfResponse>('/api/auth/csrf');
  }

  session(): Observable<SessionResponse> {
    return this.http.get<SessionResponse>('/api/auth/session');
  }

  bootstrap(): Observable<BootstrapResponse> {
    return this.http.get<BootstrapResponse>('/api/admin/console/bootstrap');
  }

  createClient(payload: { clientId: string; clientName: string; clientSecret: string; redirectUris: string[]; scopes: string[]; requirePkce: boolean; }): Observable<ClientView> {
    return this.http.post<ClientView>('/api/admin/clients', payload);
  }

  uploadClientLogo(clientId: string, file: File): Observable<ClientView> {
    const formData = new FormData();
    formData.append('logo', file);
    return this.http.post<ClientView>(`/api/admin/clients/${clientId}/logo`, formData);
  }

  createUser(payload: { username: string; email: string; password: string; }): Observable<unknown> {
    return this.http.post('/api/admin/users', payload);
  }

  enableUser(userId: string): Observable<unknown> {
    return this.http.put(`/api/admin/users/${userId}/enable`, {});
  }

  disableUser(userId: string): Observable<unknown> {
    return this.http.put(`/api/admin/users/${userId}/disable`, {});
  }

  lockUser(userId: string): Observable<unknown> {
    return this.http.put(`/api/admin/users/${userId}/lock`, {});
  }

  unlockUser(userId: string): Observable<unknown> {
    return this.http.put(`/api/admin/users/${userId}/unlock`, {});
  }

  resetPassword(userId: string, newPassword: string): Observable<unknown> {
    return this.http.put(`/api/admin/users/${userId}/password`, { newPassword });
  }

  deleteUser(userId: string): Observable<unknown> {
    return this.http.delete(`/api/admin/users/${userId}`);
  }

  createGroup(payload: { name: string; description: string; }): Observable<unknown> {
    return this.http.post('/api/admin/groups', payload);
  }

  deleteGroup(groupId: string): Observable<unknown> {
    return this.http.delete(`/api/admin/groups/${groupId}`);
  }

  addUserToGroup(groupId: string, userId: string): Observable<unknown> {
    return this.http.post(`/api/admin/groups/${groupId}/members/${userId}`, {});
  }

  removeUserFromGroup(groupId: string, userId: string): Observable<unknown> {
    return this.http.delete(`/api/admin/groups/${groupId}/members/${userId}`);
  }

  deleteClient(clientId: string): Observable<unknown> {
    return this.http.delete(`/api/admin/clients/${clientId}`);
  }

  getClientUsers(clientId: string): Observable<AssignedUser[]> {
    return this.http.get<AssignedUser[]>(`/api/admin/clients/${clientId}/users`);
  }

  createAndAssignUser(clientId: string, payload: { username: string; email: string; password: string }): Observable<AssignedUser> {
    return this.http.post<AssignedUser>(`/api/admin/clients/${clientId}/users`, payload);
  }

  addClientUser(clientId: string, userId: string): Observable<unknown> {
    return this.http.post(`/api/admin/clients/${clientId}/users/${userId}`, {});
  }

  removeClientUser(clientId: string, userId: string): Observable<unknown> {
    return this.http.delete(`/api/admin/clients/${clientId}/users/${userId}`);
  }

  branding(clientId: string): Observable<BrandingResponse> {
    return this.http.get<BrandingResponse>(`/api/branding/${clientId}`);
  }

  profile(): Observable<ProfileResponse> {
    return this.http.get<ProfileResponse>('/api/me');
  }

  changeMyPassword(currentPassword: string, newPassword: string): Observable<unknown> {
    return this.http.put('/api/me/password', { currentPassword, newPassword });
  }

  updateMyProfile(email: string): Observable<ProfileResponse> {
    return this.http.put<ProfileResponse>('/api/me', { email });
  }

  getClient(clientId: string): Observable<ClientView> {
    return this.http.get<ClientView>(`/api/admin/clients/${clientId}`);
  }

  // Sessions
  getSessions(): Observable<SessionRow[]> {
    return this.http.get<SessionRow[]>('/api/admin/sessions');
  }
  revokeSession(id: string): Observable<unknown> {
    return this.http.delete(`/api/admin/sessions/${id}`);
  }
  revokeUserSessions(username: string): Observable<unknown> {
    return this.http.delete(`/api/admin/sessions/user/${username}`);
  }

  // Audit log
  getAuditLog(page = 0, size = 50): Observable<PageResponse<AuditEntry>> {
    return this.http.get<PageResponse<AuditEntry>>('/api/admin/audit', { params: { page: page.toString(), size: size.toString() } });
  }

  // Roles
  getRoles(): Observable<RoleRow[]> {
    return this.http.get<RoleRow[]>('/api/admin/roles');
  }
  getRole(id: string): Observable<RoleRow> {
    return this.http.get<RoleRow>(`/api/admin/roles/${id}`);
  }
  getRoleMembers(roleId: string): Observable<AssignedUser[]> {
    return this.http.get<AssignedUser[]>(`/api/admin/roles/${roleId}/users`);
  }
  createRole(name: string, description: string): Observable<RoleRow> {
    return this.http.post<RoleRow>('/api/admin/roles', { name, description });
  }
  deleteRole(id: string): Observable<unknown> {
    return this.http.delete(`/api/admin/roles/${id}`);
  }
  getUserRoles(userId: string): Observable<RoleRow[]> {
    return this.http.get<RoleRow[]>(`/api/admin/roles/user/${userId}`);
  }
  assignRole(roleId: string, userId: string): Observable<unknown> {
    return this.http.post(`/api/admin/roles/${roleId}/users/${userId}`, {});
  }
  removeRole(roleId: string, userId: string): Observable<unknown> {
    return this.http.delete(`/api/admin/roles/${roleId}/users/${userId}`);
  }

  // Client scopes
  getScopes(): Observable<ScopeRow[]> {
    return this.http.get<ScopeRow[]>('/api/admin/scopes');
  }
  createScope(req: { name: string; description: string; claimName: string; claimValue: string }): Observable<ScopeRow> {
    return this.http.post<ScopeRow>('/api/admin/scopes', req);
  }
  deleteScope(id: string): Observable<unknown> {
    return this.http.delete(`/api/admin/scopes/${id}`);
  }

  updateClient(clientId: string, payload: { clientName: string; redirectUris: string[]; scopes: string[]; requirePkce: boolean; primaryColor?: string | null; }): Observable<ClientView> {
    return this.http.put<ClientView>(`/api/admin/clients/${clientId}`, payload);
  }

  getUser(userId: string): Observable<UserRow> {
    return this.http.get<UserRow>(`/api/admin/users/${userId}`);
  }

  updateUser(userId: string, payload: { username: string; email: string;
    firstName?: string; lastName?: string; phone?: string; address?: string }): Observable<unknown> {
    return this.http.put(`/api/admin/users/${userId}`, payload);
  }

  getUserAttributes(userId: string): Observable<UserAttributeRow[]> {
    return this.http.get<UserAttributeRow[]>(`/api/admin/users/${userId}/attributes`);
  }

  setUserAttribute(userId: string, key: string, value: string): Observable<UserAttributeRow> {
    return this.http.put<UserAttributeRow>(`/api/admin/users/${userId}/attributes`, { key, value });
  }

  deleteUserAttribute(userId: string, key: string): Observable<unknown> {
    return this.http.delete(`/api/admin/users/${userId}/attributes/${key}`);
  }

  getGroup(groupId: string): Observable<GroupRow> {
    return this.http.get<GroupRow>(`/api/admin/groups/${groupId}`);
  }

  updateGroup(groupId: string, payload: { name: string; description: string; }): Observable<unknown> {
    return this.http.put(`/api/admin/groups/${groupId}`, payload);
  }

  logout(csrf: CsrfResponse): Observable<string> {
    const body = new HttpParams().set(csrf.parameterName, csrf.token);
    return this.http.post('/logout', body.toString(), {
      headers: new HttpHeaders({ 'Content-Type': 'application/x-www-form-urlencoded' }),
      responseType: 'text'
    });
  }
}
