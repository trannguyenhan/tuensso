# Demo Apps — TuenSSO Integration

Two demo applications that authenticate via TuenSSO using OpenID Connect.

## Prerequisites

1. TuenSSO running on `http://localhost:8080`
2. Register two OIDC clients in TuenSSO admin console:

| Client ID    | Client Secret      | Redirect URI                                          |
|--------------|--------------------|-------------------------------------------------------|
| demo-app1    | demo-app1-secret   | http://localhost:8081/login/oauth2/code/tuensso       |
| demo-app2    | demo-app2-secret   | http://localhost:8082/callback                         |

You can register them via the admin UI at http://localhost:8080/apps/create or via API:

```bash
curl -u admin:123456 -X POST http://localhost:8080/admin/clients \
  -H "Content-Type: application/json" \
  -d '{"clientId":"demo-app1","clientSecret":"demo-app1-secret","redirectUris":["http://localhost:8081/login/oauth2/code/tuensso"],"scopes":["openid","profile","email"],"requirePkce":false}'

curl -u admin:123456 -X POST http://localhost:8080/admin/clients \
  -H "Content-Type: application/json" \
  -d '{"clientId":"demo-app2","clientSecret":"demo-app2-secret","redirectUris":["http://localhost:8082/callback"],"scopes":["openid","profile","email"],"requirePkce":false}'
```

---

## App 1 — Spring Boot (port 8081)

```bash
cd app1
mvn spring-boot:run
```

Open http://localhost:8081 — you'll be redirected to TuenSSO login. After login, you'll see your user info as JSON.

**Stack:** Spring Boot 3.3.4 + spring-boot-starter-oauth2-client

---

## App 2 — Node.js/Express (port 8082)

```bash
cd app2
npm install
npm start
```

Open http://localhost:8082 — click "Login with TuenSSO". After login, you'll see your user info.

**Stack:** Express + openid-client

---

## How it works

```
Browser → Demo App → redirect to TuenSSO /oauth2/authorize
                   ← user logs in at TuenSSO
                   → TuenSSO redirects back with authorization code
Browser → Demo App → exchanges code for tokens at /oauth2/token
                   → fetches user info from /userinfo
                   ← shows user info
```

Both apps use the standard Authorization Code flow. TuenSSO acts as the Identity Provider.
