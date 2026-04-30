# TuenSSO

Lightweight, self-hosted SSO server with OpenID Connect. A fully controllable alternative to Keycloak.

## Quick Start

```bash
docker run -d \
  -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://your-db-host:5432/tuensso \
  -e SPRING_DATASOURCE_USERNAME=tuensso \
  -e SPRING_DATASOURCE_PASSWORD=your-password \
  -e TUENSSO_ISSUER=https://sso.yourdomain.com \
  tuensso/tuensso:latest
```

## Docker Compose

```yaml
services:
  tuensso:
    image: tuensso/tuensso:latest
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://your-db-host:5432/tuensso
      SPRING_DATASOURCE_USERNAME: tuensso
      SPRING_DATASOURCE_PASSWORD: your-password
      TUENSSO_ISSUER: https://sso.yourdomain.com
    volumes:
      - logos:/data/app-logos
    restart: unless-stopped

volumes:
  logos:
```

## Environment Variables

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `SPRING_DATASOURCE_URL` | Yes | — | JDBC URL (`jdbc:postgresql://...` or `jdbc:mariadb://...`) |
| `SPRING_DATASOURCE_USERNAME` | Yes | — | Database username |
| `SPRING_DATASOURCE_PASSWORD` | Yes | — | Database password |
| `TUENSSO_ISSUER` | Yes | `http://localhost:8080` | Public URL of TuenSSO (used in JWT issuer claim) |
| `TUENSSO_DB_TYPE` | No | `postgresql` | Database type: `postgresql` or `mariadb` |
| `TUENSSO_LOGO_UPLOAD_DIR` | No | `./data/app-logos` | Directory for uploaded app logos |
| `SERVER_PORT` | No | `8080` | HTTP port |
| `SPRING_PROFILES_ACTIVE` | No | — | Set to `dev` to seed demo data |

## Database

TuenSSO supports **PostgreSQL 12+** and **MariaDB 10.6+** / **MySQL 8+**.

Schema is managed automatically via Flyway migrations on startup.

### PostgreSQL

```bash
docker run -d -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://your-db:5432/tuensso \
  -e SPRING_DATASOURCE_USERNAME=tuensso \
  -e SPRING_DATASOURCE_PASSWORD=your-password \
  -e TUENSSO_DB_TYPE=postgresql \
  -e TUENSSO_ISSUER=https://sso.yourdomain.com \
  tuensso/tuensso:latest
```

### MariaDB / MySQL

```bash
docker run -d -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:mariadb://your-db:3306/tuensso \
  -e SPRING_DATASOURCE_USERNAME=tuensso \
  -e SPRING_DATASOURCE_PASSWORD=your-password \
  -e TUENSSO_DB_TYPE=mariadb \
  -e TUENSSO_ISSUER=https://sso.yourdomain.com \
  tuensso/tuensso:latest
```

## Volumes

| Path | Description |
|------|-------------|
| `/data/app-logos` | Uploaded application logos. Mount to persist across restarts. |

## Ports

| Port | Description |
|------|-------------|
| 8080 | HTTP (application + OIDC endpoints) |

## Features

- **OIDC Provider** — Authorization Code + PKCE + Refresh Token
- **Admin Console** — Manage apps, users, groups, roles, sessions
- **SSO Login** — Per-app branding (logo, color)
- **SSO Logout** — Confirmation page with redirect
- **User Self-Service** — Profile editing, password change
- **Account Security** — Auto-lockout after failed attempts, admin lock/unlock
- **Audit Log** — Login events, user/client changes
- **JWT Claims** — email, groups, roles in tokens
- **Custom Attributes** — Key-value metadata per user

## OIDC Endpoints

| Endpoint | URL |
|----------|-----|
| Discovery | `/.well-known/openid-configuration` |
| Authorization | `/oauth2/authorize` |
| Token | `/oauth2/token` |
| UserInfo | `/userinfo` |
| JWKS | `/oauth2/jwks` |
| Introspect | `/oauth2/introspect` |
| Revoke | `/oauth2/revoke` |

## First Login

On first run with an empty database, create an admin user:

```bash
# Connect to the database and insert an admin user
# Or start with dev profile to seed demo accounts:
docker run -d \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=dev \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://your-db-host:5432/tuensso \
  -e SPRING_DATASOURCE_USERNAME=tuensso \
  -e SPRING_DATASOURCE_PASSWORD=your-password \
  tuensso/tuensso:latest
```

Dev profile creates: `admin` / `123456` (ROLE_ADMIN) and `user` / `123456` (ROLE_USER). **Change passwords immediately after first login.**

## Tech Stack

- Java 21 + Spring Boot 3.3
- Spring Authorization Server 1.3
- Angular 18 (embedded SPA)
- PostgreSQL 12+

## Links

- [GitHub](https://github.com/tuensso/tuensso)
- [Documentation](https://github.com/tuensso/tuensso/blob/main/README.md)
