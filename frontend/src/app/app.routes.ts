import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: 'login', loadComponent: () => import('./pages/login-page.component').then(m => m.LoginPageComponent) },
  { path: 'sso-login', loadComponent: () => import('./pages/sso-login-page.component').then(m => m.SsoLoginPageComponent) },
  { path: 'sso-logout', loadComponent: () => import('./pages/sso-logout-page.component').then(m => m.SsoLogoutPageComponent) },
  { path: 'admin/login', loadComponent: () => import('./pages/admin-login-page.component').then(m => m.AdminLoginPageComponent) },
  // User profile (all authenticated users)
  { path: 'account', loadComponent: () => import('./pages/account-page.component').then(m => m.AccountPageComponent) },
  { path: 'dashboard', loadComponent: () => import('./pages/account-page.component').then(m => m.AccountPageComponent) },
  // Admin console
  { path: 'admin/dashboard', loadComponent: () => import('./pages/dashboard-page.component').then(m => m.DashboardPageComponent) },
  { path: 'admin/docs', loadComponent: () => import('./pages/docs-page.component').then(m => m.DocsPageComponent) },
  { path: 'admin/apps/:id', loadComponent: () => import('./pages/app-detail-page.component').then(m => m.AppDetailPageComponent) },
  { path: 'admin/users/:id', loadComponent: () => import('./pages/user-detail-page.component').then(m => m.UserDetailPageComponent) },
  { path: 'admin/groups/:id', loadComponent: () => import('./pages/group-detail-page.component').then(m => m.GroupDetailPageComponent) },
  { path: 'admin/roles/:id', loadComponent: () => import('./pages/role-detail-page.component').then(m => m.RoleDetailPageComponent) },
  { path: 'admin/apps', loadComponent: () => import('./pages/console-page.component').then(m => m.ConsolePageComponent) },
  { path: 'admin/users', loadComponent: () => import('./pages/console-page.component').then(m => m.ConsolePageComponent) },
  { path: 'admin/groups', loadComponent: () => import('./pages/console-page.component').then(m => m.ConsolePageComponent) },
  { path: 'admin/integration', loadComponent: () => import('./pages/console-page.component').then(m => m.ConsolePageComponent) },
  { path: 'admin/roles', loadComponent: () => import('./pages/console-page.component').then(m => m.ConsolePageComponent) },
  { path: 'admin/sessions', loadComponent: () => import('./pages/console-page.component').then(m => m.ConsolePageComponent) },
  { path: 'admin/audit', loadComponent: () => import('./pages/console-page.component').then(m => m.ConsolePageComponent) },
  { path: '', pathMatch: 'full', redirectTo: 'account' },
  { path: '**', redirectTo: 'account' }
];
