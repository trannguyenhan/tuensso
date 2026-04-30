# TuenSSO

Standalone SSO server written in Java, providing OpenID Connect services for other applications.
A lightweight, fully controllable replacement for Keycloak.

---

## Architecture

```
App A  --OIDC-->
App B  --OIDC-->  TuenSSO :8080
App C  --OIDC-->
```

TuenSSO acts as an Identity Provider. Apps only need to configure an OIDC client
pointing to TuenSSO to enable login — no knowledge of TuenSSO internals required.

---

## Tech Stack

- Java 21
- Spring Boot 3.3.4
- Spring Authorization Server 1.3.2
- Spring Security 6
- Spring Data JPA + Flyway
- PostgreSQL (production) / H2 (dev)
- Angular 18 (SPA frontend)

---

## Getting Started

### Prerequisites

- Java 21
- Maven 3.9+
- Internet access on first run (Maven downloads Node/NPM for frontend build)

### Option 1: Quick dev run (recommended)

```bash
mvn spring-boot:run
```

This will:

- Build the Angular frontend
- Copy frontend into classpath resources
- Start Spring Boot in a single process

### Option 2: Build artifact then run

```bash
mvn clean package
java -jar target/tuensso-0.0.1-SNAPSHOT.jar
```

If port 8080 is busy, use a different port:

```bash
java -jar target/tuensso-0.0.1-SNAPSHOT.jar --server.port=8081
```

### URLs and Default Accounts

- Application: http://localhost:8080
- OIDC discovery: http://localhost:8080/.well-known/openid-configuration
- Demo accounts: admin/123456, user/123456

---

## OIDC Endpoints (for client apps)

| Endpoint                              | Description                                  |
|---------------------------------------|----------------------------------------------|
| GET /.well-known/openid-configuration | Discovery — apps read this to auto-configure |
| GET /oauth2/authorize                 | Start login flow (redirect)                  |
| POST /oauth2/token                    | Exchange authorization code for tokens       |
| GET /userinfo                         | Get user info from access token              |
| GET /oauth2/jwks                      | Public keys for JWT verification             |
| POST /oauth2/introspect               | Check if token is still valid                |
| POST /oauth2/revoke                   | Revoke a token                               |
| GET /connect/logout                   | Logout                                       |

---

## Admin REST API

Requires login with a ROLE_ADMIN account.

### Client Management

```
POST   /api/admin/clients              Register new OIDC client
GET    /api/admin/clients/{clientId}    View client details
PUT    /api/admin/clients/{clientId}    Update client
DELETE /api/admin/clients/{clientId}    Delete client
```

### User Management

```
GET    /api/admin/users                List users
GET    /api/admin/users/{id}           View user
POST   /api/admin/users                Create user
PUT    /api/admin/users/{id}           Update user
PUT    /api/admin/users/{id}/enable    Enable user
PUT    /api/admin/users/{id}/disable   Disable user
PUT    /api/admin/users/{id}/password  Change password
DELETE /api/admin/users/{id}           Delete user
```

### Group Management

```
GET    /api/admin/groups               List groups
POST   /api/admin/groups               Create group
DELETE /api/admin/groups/{id}          Delete group
POST   /api/admin/groups/{id}/members/{userId}    Add user to group
DELETE /api/admin/groups/{id}/members/{userId}    Remove user from group
```

### Role Management

```
GET    /api/admin/roles                List roles
POST   /api/admin/roles                Create role
DELETE /api/admin/roles/{id}           Delete role
POST   /api/admin/roles/{roleId}/users/{userId}   Assign role to user
DELETE /api/admin/roles/{roleId}/users/{userId}   Remove role from user
GET    /api/admin/roles/user/{userId}  View user's roles
```

### Sessions & Audit

```
GET    /api/admin/sessions             List active sessions
DELETE /api/admin/sessions/{id}        Revoke session
DELETE /api/admin/sessions/user/{username}  Revoke all sessions for user
GET    /api/admin/audit                Activity log (paginated)
```

---

## Integration Guide

### Spring Boot app

Add dependency:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-client</artifactId>
</dependency>
```

Configure application.yml:

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          tuensso:
            client-id: myapp
            client-secret: my-secret
            scope: openid,profile,email
            redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"
        provider:
          tuensso:
            issuer-uri: http://localhost:8080
```

### Any OIDC-compatible application

Use the following endpoints:

```
Issuer URI:             http://localhost:8080
Authorization URI:      http://localhost:8080/oauth2/authorize
Token URI:              http://localhost:8080/oauth2/token
JWKS URI:               http://localhost:8080/oauth2/jwks
UserInfo URI:           http://localhost:8080/userinfo
```

---

## JWT Claims

Tokens issued to client apps include:

```json
{
  "sub": "username",
  "email": "user@example.com",
  "preferred_username": "username",
  "name": "username",
  "groups": ["admins", "developers"],
  "roles": ["editor", "viewer"]
}
```

---

## Source Structure

```
src/main/java/com/tuensso/
  TuenSsoApplication.java              Entry point
  config/
    SecurityConfig.java                 Filter chains, OIDC, security rules
    BootstrapDataConfig.java            Seed data (dev profile only)
    TokenCustomizerConfig.java          JWT claims customization
    SsoLoginEntryPoint.java             Redirect to SSO login page
  admin/
    ClientAdminController.java          REST API for OIDC client management
    UserAdminController.java            REST API for user management
    GroupAdminController.java           REST API for group management
    RoleAdminController.java            REST API for role management
    SessionAdminController.java         REST API for session management
    AuditLogAdminController.java        REST API for audit log
    AdminConsoleApiController.java      Bootstrap API for admin console
  audit/
    AuditLog.java                       Audit log entity
    AuditLogRepository.java             JPA repository
    AuditService.java                   Audit logging service
    AuthEventListener.java              Login success/failure event listener
  role/
    Role.java                           Role entity
    RoleRepository.java                 JPA repository
  user/
    UserAccount.java                    User entity
    UserAccountRepository.java          JPA repository
    DbUserDetailsService.java           Load users from DB for Spring Security
  client/
    OidcClientService.java              OIDC client management service
    AppLogoStorageService.java          Application logo storage
  group/
    UserGroup.java                      Group entity
    UserGroupRepository.java            JPA repository
  web/
    HomeController.java                 SPA routing (forward to index.html)
    AuthApiController.java              Session/CSRF API
    BrandingApiController.java          Login page branding API
    SsoLogoutController.java            SSO logout API
    UserProfileApiController.java       User profile API
    AppLogoController.java              Logo file serving
    OidcMetadataController.java         OIDC metadata API

frontend/
  src/                                  Angular source code
  angular.json                          Angular build config
  package.json                          Frontend dependencies
```

---

## Switching to PostgreSQL (production)

1. Edit `application.yml`:

```yaml
spring:
  profiles:
    active: prod
  datasource:
    url: jdbc:postgresql://localhost:5432/tuensso
    username: tuensso
    password: ${DB_PASSWORD}
    driver-class-name: org.postgresql.Driver
```

2. Add dependency to `pom.xml`:

```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

---

## Completed Features

- [x] OIDC Authorization Code + Refresh Token flow
- [x] Admin Console UI (Angular SPA)
- [x] Application management (CRUD, logo, branding, primary color)
- [x] User management (CRUD, enable/disable, password reset)
- [x] Group management + user-group assignment
- [x] Role management + user-role assignment
- [x] Roles & Groups in JWT claims
- [x] SSO Login with per-app branding
- [x] SSO Logout with confirmation page
- [x] User profile page (view/edit email, change password)
- [x] Session management (view/revoke active sessions)
- [x] Audit log (login events, user/client CRUD, password changes)
- [x] PKCE support
- [x] Docker + Docker Compose

## Roadmap

- [ ] Persist RSA keys to file / KMS (prevent key loss on restart)
- [ ] Forgot password + email delivery
- [ ] Account lockout after N failed login attempts
- [ ] 2FA/MFA (TOTP)
- [ ] Consent screen
- [ ] HTTPS / Reverse proxy guide
