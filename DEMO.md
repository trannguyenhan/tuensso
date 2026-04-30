# TuenSSO — Demo Guide

## 1. Start TuenSSO

```bash
mvn spring-boot:run
```

Open http://localhost:8080 → sign in with `admin / 123456`

---

## 2. Register 2 OIDC clients for demo

### Option 1: Via Admin UI

Go to http://localhost:8080/admin/apps → **+ Create** → fill in:

| Field | App 1 | App 2 |
|-------|-------|-------|
| Client ID | `demo-app1` | `demo-app2` |
| App name | `Demo App 1` | `Demo App 2` |
| Client secret | `demo-app1-secret` | `demo-app2-secret` |
| Redirect URIs | `http://localhost:8081/login/oauth2/code/tuensso` | `http://localhost:8082/callback` |
| Scopes | `openid, profile, email` | `openid, profile, email` |
| Require PKCE | ☐ | ☐ |

After creating, edit each app to set **logo**, **primary color** and preview the **Login page**.

### Option 2: Via API

```bash
curl -u admin:123456 -X POST http://localhost:8080/api/admin/clients \
  -H "Content-Type: application/json" \
  -d '{
    "clientId": "demo-app1",
    "clientName": "Demo App 1",
    "clientSecret": "demo-app1-secret",
    "redirectUris": ["http://localhost:8081/login/oauth2/code/tuensso"],
    "scopes": ["openid", "profile", "email"],
    "requirePkce": false
  }'

curl -u admin:123456 -X POST http://localhost:8080/api/admin/clients \
  -H "Content-Type: application/json" \
  -d '{
    "clientId": "demo-app2",
    "clientName": "Demo App 2",
    "clientSecret": "demo-app2-secret",
    "redirectUris": ["http://localhost:8082/callback"],
    "scopes": ["openid", "profile", "email"],
    "requirePkce": false
  }'
```

---

## 3. Run Demo App 1 (Spring Boot — port 8081)

```bash
cd demo/app1
mvn spring-boot:run
```

Open http://127.0.0.1:8081 → auto-redirects to TuenSSO login → sign in → view user info JSON.

---

## 4. Run Demo App 2 (Node.js/Express — port 8082)

```bash
cd demo/app2
npm install
npm start
```

Open http://localhost:8082 → click **Login with TuenSSO** → sign in → view user info.

---

## 5. Feature Walkthrough

### Branded Login Page

When an app redirects to TuenSSO, the URL includes `?client_id=demo-app1`. The login page shows:
- App logo (if uploaded)
- App name ("Sign in to **Demo App 1**")
- Sign in button in the app's primary color

Direct test: http://localhost:8080/sso-login?client_id=demo-app1

### Admin Console (admin / 123456)

| Page | URL | Description |
|------|-----|-------------|
| Dashboard | /admin/dashboard | Stats: apps, users, groups |
| Applications | /admin/apps | OIDC client list |
| Create App | /admin/apps/create | Register new client |
| Edit App | /admin/apps/{clientId} | Edit, upload logo, set color, preview login |
| Users | /admin/users | User list |
| Create User | /admin/users/create | Create new user |
| Edit User | /admin/users/{id} | Edit profile, reset password, enable/disable, manage groups & roles |
| Groups | /admin/groups | Group list |
| Roles | /admin/roles | Role management |
| Sessions | /admin/sessions | Active sessions, revoke |
| Audit Log | /admin/audit | Activity log |
| Integration | /admin/integration | OIDC endpoints reference |
| Documentation | /admin/docs | Usage guide, deploy, API |

### User Profile (user / 123456)

Sign in with `user / 123456` → auto-redirects to http://localhost:8080/account

Shows: username, email, status, groups, member since. Click **Edit** to change email.

### Per-app Branding

1. Go to http://localhost:8080/admin/apps → click **Edit** on an app
2. **Branding**: pick a color with the color picker
3. **Logo**: upload a logo image
4. **Login preview**: see live preview
5. Click **Save changes**
6. Open http://localhost:8080/sso-login?client_id={clientId} to see the result

### SSO Logout

Apps can redirect users to:
```
http://localhost:8080/sso-logout?client_id=demo-app1&redirect_uri=http://localhost:8081/
```
User sees a confirmation page → clicks **Sign out** → SSO session ends → redirects back to app.

---

## 6. Default Accounts

| Username | Password | Role | After login |
|----------|----------|------|-------------|
| admin | 123456 | ROLE_ADMIN | → /admin/dashboard (Admin Console) |
| user | 123456 | ROLE_USER | → /account (User Profile) |

---

## 7. OIDC Endpoints

| Endpoint | URL |
|----------|-----|
| Discovery | http://localhost:8080/.well-known/openid-configuration |
| Authorization | http://localhost:8080/oauth2/authorize |
| Token | http://localhost:8080/oauth2/token |
| JWKS | http://localhost:8080/oauth2/jwks |
| UserInfo | http://localhost:8080/userinfo |

---

## 8. Docker

```bash
# Build & run with H2 (dev)
docker build -t tuensso .
docker run -p 8080:8080 tuensso

# Or use docker-compose with PostgreSQL
docker compose up --build
```
