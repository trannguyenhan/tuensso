import { Routes } from '@angular/router';
import { ConsolePageComponent } from './pages/console-page.component';
import { LoginPageComponent } from './pages/login-page.component';
import { SsoLoginPageComponent } from './pages/sso-login-page.component';
import { SsoLogoutPageComponent } from './pages/sso-logout-page.component';
import { AppDetailPageComponent } from './pages/app-detail-page.component';
import { UserDetailPageComponent } from './pages/user-detail-page.component';
import { GroupDetailPageComponent } from './pages/group-detail-page.component';
import { RoleDetailPageComponent } from './pages/role-detail-page.component';
import { DashboardPageComponent } from './pages/dashboard-page.component';
import { DocsPageComponent } from './pages/docs-page.component';
import { AccountPageComponent } from './pages/account-page.component';
import { AdminLoginPageComponent } from './pages/admin-login-page.component';

export const routes: Routes = [
  { path: 'login', component: LoginPageComponent },
  { path: 'sso-login', component: SsoLoginPageComponent },
  { path: 'sso-logout', component: SsoLogoutPageComponent },
  { path: 'admin/login', component: AdminLoginPageComponent },
  // User profile (all authenticated users)
  { path: 'account', component: AccountPageComponent },
  { path: 'dashboard', component: AccountPageComponent },
  // Admin console
  { path: 'admin/dashboard', component: DashboardPageComponent },
  { path: 'admin/docs', component: DocsPageComponent },
  { path: 'admin/apps/:id', component: AppDetailPageComponent },
  { path: 'admin/users/:id', component: UserDetailPageComponent },
  { path: 'admin/groups/:id', component: GroupDetailPageComponent },
  { path: 'admin/roles/:id', component: RoleDetailPageComponent },
  { path: 'admin/apps', component: ConsolePageComponent },
  { path: 'admin/users', component: ConsolePageComponent },
  { path: 'admin/groups', component: ConsolePageComponent },
  { path: 'admin/integration', component: ConsolePageComponent },
  { path: 'admin/roles', component: ConsolePageComponent },
  { path: 'admin/sessions', component: ConsolePageComponent },
  { path: 'admin/audit', component: ConsolePageComponent },
  { path: '', pathMatch: 'full', redirectTo: 'account' },
  { path: '**', redirectTo: 'account' }
];
