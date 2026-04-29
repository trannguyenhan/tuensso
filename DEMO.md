# TuenSSO — Hướng dẫn chạy Demo

## 1. Khởi động TuenSSO

```bash
mvn spring-boot:run
```

Mở http://localhost:8080 → đăng nhập với `admin / 123456`

---

## 2. Đăng ký 2 OIDC client cho demo

### Cách 1: Qua Admin UI

Vào http://localhost:8080/apps → **+ Create** → điền thông tin:

| Field | App 1 | App 2 |
|-------|-------|-------|
| Client ID | `demo-app1` | `demo-app2` |
| App name | `Demo App 1` | `Demo App 2` |
| Client secret | `demo-app1-secret` | `demo-app2-secret` |
| Redirect URIs | `http://localhost:8081/login/oauth2/code/tuensso` | `http://localhost:8082/callback` |
| Scopes | `openid, profile, email` | `openid, profile, email` |
| Require PKCE | ☐ | ☐ |

Sau khi tạo, vào edit từng app để chọn **logo**, **màu chủ đạo** và xem **Login preview**.

### Cách 2: Qua API

```bash
curl -u admin:123456 -X POST http://localhost:8080/admin/clients \
  -H "Content-Type: application/json" \
  -d '{
    "clientId": "demo-app1",
    "clientName": "Demo App 1",
    "clientSecret": "demo-app1-secret",
    "redirectUris": ["http://localhost:8081/login/oauth2/code/tuensso"],
    "scopes": ["openid", "profile", "email"],
    "requirePkce": false
  }'

curl -u admin:123456 -X POST http://localhost:8080/admin/clients \
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

## 3. Chạy Demo App 1 (Spring Boot — port 8081)

```bash
cd demo/app1
mvn spring-boot:run
```

Mở http://127.0.0.1:8081 → tự redirect sang TuenSSO login → đăng nhập → xem user info JSON.

---

## 4. Chạy Demo App 2 (Node.js/Express — port 8082)

```bash
cd demo/app2
npm install
npm start
```

Mở http://localhost:8082 → click **Login with TuenSSO** → đăng nhập → xem user info.

---

## 5. Kiểm tra các tính năng

### Trang Login có branding

Khi app redirect sang TuenSSO, URL sẽ có `?client_id=demo-app1`. Trang login sẽ hiển thị:
- Logo của app (nếu đã upload)
- Tên app ("Sign in to continue to **Demo App 1**")
- Nút Sign in theo màu chủ đạo của app

Test trực tiếp: http://localhost:8080/login?client_id=demo-app1

### Admin Console (admin / 123456)

| Trang | URL | Mô tả |
|-------|-----|-------|
| Dashboard | /dashboard | Thống kê apps, users, groups |
| Applications | /apps | Danh sách OIDC clients |
| Create App | /apps/create | Tạo client mới |
| Edit App | /apps/{clientId} | Chỉnh sửa, upload logo, chọn màu, preview login |
| Users | /users | Danh sách users |
| Create User | /users/create | Tạo user mới |
| Edit User | /users/{id} | Sửa profile, reset password, enable/disable, quản lý groups |
| Groups | /groups | Danh sách groups |
| Integration | /integration | OIDC endpoints reference |
| Documentation | /docs | Hướng dẫn sử dụng, deploy, API |

### Trang User Profile (user / 123456)

Đăng nhập với tài khoản `user / 123456` → tự redirect sang http://localhost:8080/account

Hiển thị: username, email, status, groups, ngày tạo.

### Branding per-app

1. Vào http://localhost:8080/apps → click **Edit** trên một app
2. Phần **Branding**: chọn màu bằng color picker
3. Phần **Logo**: upload logo
4. Phần **Login preview**: xem live preview trang login
5. Click **Save changes**
6. Mở http://localhost:8080/login?client_id={clientId} để xem kết quả

---

## 6. Tài khoản mặc định

| Username | Password | Role | Sau đăng nhập |
|----------|----------|------|---------------|
| admin | 123456 | ROLE_ADMIN | → /dashboard (Admin Console) |
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
# Build & run với H2 (dev)
docker build -t tuensso .
docker run -p 8080:8080 tuensso

# Hoặc dùng docker-compose với PostgreSQL
docker compose up --build
```
