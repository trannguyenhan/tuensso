# TuenSSO

SSO server doc lap viet bang Java, cung cap dich vu OpenID Connect cho cac ung dung khac.
Thay the Keycloak voi codebase gon nhe, hoan toan kiem soat duoc.

---

## Kien truc

```
App A  --OIDC-->
App B  --OIDC-->  TuenSSO :8080
App C  --OIDC-->
```

TuenSSO dong vai tro Identity Provider. Cac app chi can cau hinh OIDC client
tro vao TuenSSO la dang nhap duoc, khong can biet TuenSSO duoc viet bang gi.

---

## Cong nghe

- Java 21
- Spring Boot 3.3.4
- Spring Authorization Server 1.3.2
- Spring Security 6
- Spring Data JPA + Flyway
- H2 (dev) / PostgreSQL (production)
- Angular 18 (SPA frontend)

---

## Cach chay

### Yeu cau

- Java 21
- Maven 3.9+
- Co internet o lan chay dau tien de Maven tai Node/NPM cho frontend

### Cach 1: Chay dev nhanh (khuyen dung)

```bash
mvn spring-boot:run
```

Lenh nay se:

- Build frontend Angular
- Copy frontend vao classpath resource
- Chay Spring Boot ngay tren cung mot process

### Cach 2: Build artifact roi chay

```bash
mvn clean package
java -jar target/tuensso-0.0.1-SNAPSHOT.jar
```

Neu port 8080 dang ban, dung port khac:

```bash
java -jar target/tuensso-0.0.1-SNAPSHOT.jar --server.port=8081
```

### URL va tai khoan mac dinh

- Ung dung: http://localhost:8080
- OIDC discovery: http://localhost:8080/.well-known/openid-configuration
- Tai khoan demo: admin/123456, user/123456

---

## OIDC Endpoints (de app ngoai ket noi)

| Endpoint                            | Mo ta                                        |
|-------------------------------------|----------------------------------------------|
| GET /.well-known/openid-configuration | Discovery — app doc de tu cau hinh          |
| GET /oauth2/authorize               | Bat dau luong dang nhap (redirect)           |
| POST /oauth2/token                  | Doi authorization code lay access/id token  |
| GET /userinfo                       | Lay thong tin user tu access token          |
| GET /oauth2/jwks                    | Public key de app tu verify JWT             |
| POST /oauth2/introspect             | Kiem tra token con hop le khong             |
| POST /oauth2/revoke                 | Thu hoi token                               |
| GET /connect/logout                 | Logout                                      |

---

## Admin REST API

Yeu cau dang nhap bang tai khoan co ROLE_ADMIN.

### Quan ly clients

```
POST   /admin/clients              Dang ky OIDC client moi
GET    /admin/clients/{clientId}   Xem thong tin client
```

Vi du dang ky client:

```bash
curl -u admin:123456 -X POST http://localhost:8080/admin/clients \
  -H "Content-Type: application/json" \
  -d '{
    "clientId": "myapp",
    "clientSecret": "my-secret",
    "redirectUris": ["http://localhost:3000/callback"],
    "scopes": ["openid", "profile", "email"],
    "requirePkce": false
  }'
```

### Quan ly users

```
GET    /admin/users                Danh sach user
GET    /admin/users/{id}           Xem user
POST   /admin/users                Tao user moi
PUT    /admin/users/{id}/enable    Mo khoa user
PUT    /admin/users/{id}/disable   Khoa user
PUT    /admin/users/{id}/password  Doi mat khau
DELETE /admin/users/{id}           Xoa user
```

Vi du tao user:

```bash
curl -u admin:123456 -X POST http://localhost:8080/admin/users \
  -H "Content-Type: application/json" \
  -d '{
    "username": "alice",
    "email": "alice@example.com",
    "password": "securepassword"
  }'
```

---

## Cach tich hop vao ung dung khac

### Spring Boot app

Them dependency:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-client</artifactId>
</dependency>
```

Cau hinh application.yml:

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

### Bat ky ung dung nao ho tro OIDC

Su dung thong tin sau:

```
Issuer URI:             http://localhost:8080
Authorization URI:      http://localhost:8080/oauth2/authorize
Token URI:              http://localhost:8080/oauth2/token
JWKS URI:               http://localhost:8080/oauth2/jwks
UserInfo URI:           http://localhost:8080/userinfo
```

---

## Cau truc source

```
src/main/java/com/tuensso/
  TuenSsoApplication.java          Entry point
  config/
    SecurityConfig.java            Filter chains, OIDC, security rules
  admin/
    ClientAdminController.java     REST API quan ly OIDC clients
    UserAdminController.java       REST API quan ly users
  user/
    UserAccount.java               Entity user
    UserAccountRepository.java     JPA repository
  web/
    HomeController.java            Trang chu, trang login

src/main/resources/
  application.yml                  Cau hinh chinh
  db/migration/
    V1__init_schema.sql            Bang users
    V2__oauth2_schema.sql          Bang oauth2_registered_client, authorization
  static/assets/
    ...                            Static resource backend

frontend/
  src/                             Angular source code
  angular.json                     Angular build config
  package.json                     Frontend scripts/dependencies

target/generated-resources/frontend/browser/
  ...                              Frontend build output duoc Maven dua vao classpath
```

---

## Chuyen sang PostgreSQL (production)

1. Sua `application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/tuensso
    username: tuensso
    password: your-password
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: validate
```

2. Them dependency vao `pom.xml`:

```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

---

## Phat trien tiep theo (Phase 2)

- [ ] JPA UserDetailsService (load user tu DB thay vi in-memory)
- [ ] Luu RSA key ra file / KMS (tranh mat key khi restart)
- [ ] Admin UI Thymeleaf (thay vi goi API thu cong)
- [ ] Role trong JWT claim (de app ngoai phan quyen theo role)
- [ ] Docker Compose (TuenSSO + PostgreSQL)
- [ ] HTTPS / Reverse proxy guide
