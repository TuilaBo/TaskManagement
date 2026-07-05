# Task Management API

RESTful API quản lý công việc (Task Management) viết bằng **Java 21 + Spring Boot 4**, xác thực **JWT**, database **PostgreSQL**, gửi email qua **Gmail SMTP**, upload ảnh qua **Cloudinary**.

---

## Mục lục

- [1. Tổng quan](#1-tổng-quan)
- [2. Yêu cầu hệ thống](#2-yêu-cầu-hệ-thống)
- [3. Cài đặt Local (Development)](#3-cài-đặt-local-development)
- [4. Triển khai trên VPS](#4-triển-khai-trên-vps)
- [5. Đưa lên GitHub](#5-đưa-lên-github)
- [6. API Reference](#6-api-reference)
- [7. Môi trường & Biến số](#7-môi-trường--biến-số)

---

## 1. Tổng quan

### Vai trò người dùng

| Vai trò | Mô tả |
|---------|-------|
| **MANAGER** | Quản lý toàn bộ: tạo/sửa/xóa task, giao task cho staff, quản lý danh mục, quản lý user |
| **STAFF** | Nhận và hoàn thành task được giao, cập nhật thông tin cá nhân |

### Tài khoản mặc định

```
Username: manager
Password: manager123
Role:     MANAGER
```

> Staff cần đăng ký qua API + xác minh email mới đăng nhập được.

### Các chức năng chính

- **Task:** Tạo, giao, cập nhật, xóa, đổi trạng thái, hoàn thành (kèm hình ảnh), gửi email nhắc nhở
- **Task Category:** Danh mục công việc có sẵn để giao nhanh
- **User:** Đăng ký Staff, quản lý thông tin, đổi mật khẩu
- **Auth:** JWT token (24h), xác minh email khi đăng ký Staff

---

## 2. Yêu cầu hệ thống

### Local Development

| Phần mềm | Phiên bản tối thiểu |
|----------|----------------------|
| Java | 21 |
| Maven | 3.9+ |
| PostgreSQL | 15+ |

### Production (VPS)

| Phần mềm | Phiên bản tối thiểu |
|----------|----------------------|
| Ubuntu 20.04+ / Debian | |
| Java | 21 (OpenJDK hoặc Adoptium) |
| PostgreSQL | 15+ |
| Nginx | 1.18+ (optional, reverse proxy) |
| screen / nohup / systemd | để chạy nền |

---

## 3. Cài đặt Local (Development)

### 3.1. Clone & cài thư viện

```bash
git clone https://github.com/<your-username>/TaskManagement.git
cd TaskManagement
mvn clean install -DskipTests
```

### 3.2. Tạo database PostgreSQL

```sql
CREATE DATABASE task_management;
CREATE USER postgres WITH PASSWORD 'sa';
GRANT ALL PRIVILEGES ON DATABASE task_management TO postgres;
```

### 3.3. Cấu hình file `application-local.properties`

Tạo file `src/main/resources/application-local.properties`:

```properties
# Database (thay password nếu cần)
spring.datasource.url=jdbc:postgresql://localhost:5432/task_management
spring.datasource.username=postgres
spring.datasource.password=sa

# JWT secret (key 256-bit, encode Base64)
jwt.secret=ZHVtbXlTZWNyZXRLZXlGb3JKV1RUZXN0aW5nMjU2Yml0cw==

# Gmail credentials
spring.mail.username=your_email@gmail.com
spring.mail.password=your_gmail_app_password

# Cloudinary API Secret
cloudinary.api-secret=your_cloudinary_api_secret
```

> **Lưu ý:** `application-local.properties` đã có trong `.gitignore`, không bao giờ bị push lên GitHub.

### 3.4. Bật "Less secure app" / App Password Gmail

1. Bật 2-Step Verification trên tài khoản Google.
2. Vào [App Passwords](https://myaccount.google.com/apppasswords) → tạo App Password cho "Mail".
3. Dùng App Password đó cho `spring.mail.password`.

### 3.5. Chạy ứng dụng

```bash
# Development (hot reload với Spring DevTools)
mvn spring-boot:run

# Hoặc chạy file JAR đã build
java -jar target/taskmanagement-0.0.1-SNAPSHOT.jar
```

### 3.6. Truy cập

| Giao diện | URL |
|-----------|-----|
| Swagger UI (API docs) | http://localhost:8081/swagger-ui.html |
| Spring Boot Actuator | http://localhost:8081/actuator |

---

## 4. Triển khai trên VPS

### 4.1. Cài đặt Java 21 & PostgreSQL

```bash
# Cập nhật hệ thống
sudo apt update && sudo apt upgrade -y

# Cài Java 21
sudo apt install -y openjdk-21-jdk
java -version

# Cài PostgreSQL
sudo apt install -y postgresql postgresql-contrib

# Bật và khởi động PostgreSQL
sudo systemctl enable postgresql
sudo systemctl start postgresql
```

### 4.2. Cài đặt Database

```bash
# Đăng nhập PostgreSQL
sudo -u postgres psql

# Tạo database và user
CREATE DATABASE task_management;
CREATE USER postgres WITH PASSWORD 'YOUR_DB_PASSWORD';
GRANT ALL PRIVILEGES ON DATABASE task_management TO postgres;
\q
```

### 4.3. Tạo thư mục triển khai

```bash
sudo mkdir -p /opt/taskmanagement/app
sudo mkdir -p /opt/taskmanagement/config
sudo mkdir -p /opt/taskmanagement/logs

# Phân quyền
sudo chown -R $(whoami):$(whoami) /opt/taskmanagement
```

### 4.4. Upload JAR lên VPS

```bash
# Cách 1: Từ máy local — build JAR rồi upload (scp)
mvn clean package -DskipTests
scp target/taskmanagement-0.0.1-SNAPSHOT.jar root@YOUR_VPS_IP:/opt/taskmanagement/app/

# Cách 2: Dùng Git trên VPS (khuyên dùng — luôn có code mới nhất)
git clone https://github.com/<your-username>/TaskManagement.git /opt/taskmanagement/app
cd /opt/taskmanagement/app
mvn clean package -DskipTests

# Cách 3: Trên VPS — pull code mới nhất (sau khi đã clone lần đầu)
cd /opt/taskmanagement/app
git pull origin main
mvn clean package -DskipTests
cp target/taskmanagement-0.0.1-SNAPSHOT.jar /opt/taskmanagement/app/
```

### 4.5. Tạo file cấu hình production

Tạo file `/opt/taskmanagement/config/application-prod.properties`:

```properties
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/task_management
spring.datasource.username=postgres
spring.datasource.password=YOUR_DB_PASSWORD

# JWT — BẮT BUỘC thay bằng key mới, 256-bit Base64
jwt.secret=YOUR_SUPER_SECRET_256BIT_KEY_BASE64_ENCODED

# Mail
spring.mail.username=your_email@gmail.com
spring.mail.password=your_gmail_app_password

# Cloudinary
cloudinary.api-secret=YOUR_CLOUDINARY_API_SECRET

# CORS — thay bằng domain thật của frontend
cors.allowed-origins=https://your-frontend-domain.com

# Dev tools — tắt trên production
spring.devtools.restart.enabled=false
spring.devtools.livereload.enabled=false

# SQL logs — tắt trên production
spring.jpa.show-sql=false
```

### 4.6. Chạy ứng dụng

#### Cách 1 — Dùng `screen` (khuyên dùng)

```bash
# Tạo session screen
screen -S taskapp

# Chạy app
cd /opt/taskmanagement/app
java -jar taskmanagement-0.0.1-SNAPSHOT.jar \
  --spring.config.additional-location=file:/opt/taskmanagement/config/application-prod.properties \
  >> /opt/taskmanagement/logs/app.log 2>&1

# Thoát session mà không tắt: Ctrl+A rồi nhấn D

# Quay lại xem log
screen -r taskapp

# Xem log
tail -f /opt/taskmanagement/logs/app.log
```

#### Cách 2 — Dùng `nohup`

```bash
cd /opt/taskmanagement/app
nohup java -jar taskmanagement-0.0.1-SNAPSHOT.jar \
  --spring.config.additional-location=file:/opt/taskmanagement/config/application-prod.properties \
  > /opt/taskmanagement/logs/app.log 2>&1 &

# Kiểm tra tiến trình
ps aux | grep taskmanagement
```

#### Cách 3 — Dùng systemd service (chuyên nghiệp)

Tạo file `/etc/systemd/system/taskmanagement.service`:

```ini
[Unit]
Description=TaskManagement API
After=network.target postgresql.service

[Service]
Type=simple
User=root
WorkingDirectory=/opt/taskmanagement/app
ExecStart=/usr/bin/java -jar /opt/taskmanagement/app/taskmanagement-0.0.1-SNAPSHOT.jar \
  --spring.config.additional-location=file:/opt/taskmanagement/config/application-prod.properties
Restart=always
RestartSec=10
StandardOutput=append:/opt/taskmanagement/logs/app.log
StandardError=append:/opt/taskmanagement/logs/app.log

[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl daemon-reload
sudo systemctl enable taskmanagement
sudo systemctl start taskmanagement
sudo systemctl status taskmanagement

# Xem log
journalctl -u taskmanagement -f
```

### 4.9. Cập nhật code mới (Sau khi push lên GitHub)

#### Dùng systemd

```bash
# 1. Dừng ứng dụng
sudo systemctl stop taskmanagement

# 2. Pull code mới nhất từ GitHub
cd /opt/taskmanagement/app
git pull origin main

# 3. Rebuild
mvn clean package -DskipTests

# 4. Copy JAR mới
cp target/taskmanagement-0.0.1-SNAPSHOT.jar /opt/taskmanagement/app/

# 5. Khởi động lại
sudo systemctl start taskmanagement
sudo systemctl status taskmanagement
```

#### Dùng screen

```bash
# 1. Thoát khỏi screen (Ctrl+A rồi D)
# 2. Dừng process
pkill -f taskmanagement

# 3. Pull & rebuild
cd /opt/taskmanagement/app
git pull origin main
mvn clean package -DskipTests
cp target/taskmanagement-0.0.1-SNAPSHOT.jar /opt/taskmanagement/app/

# 4. Chạy lại
screen -S taskapp
java -jar /opt/taskmanagement/app/taskmanagement-0.0.1-SNAPSHOT.jar \
  --spring.config.additional-location=file:/opt/taskmanagement/config/application-prod.properties
# Ctrl+A rồi D để thoát
```

### 4.9b. Triển khai bằng Docker (Docker Compose)

**Ưu điểm:** Không cần cài Java trên VPS, tự động chạy PostgreSQL cùng app.

#### Bước 1 — Cài Docker trên VPS

```bash
# Cài Docker
curl -fsSL https://get.docker.com | sh

# Cài Docker Compose
apt install docker-compose -y

# Bật và khởi động Docker
systemctl enable docker
systemctl start docker

# Kiểm tra
docker --version
docker-compose --version
```

#### Bước 2 — Copy project lên VPS

```bash
# Từ máy local — push lên GitHub trước
git add .
git commit -m "Add Docker support"
git push origin main

# Trên VPS
cd /opt
git clone https://github.com/<your-username>/TaskManagement.git
cd TaskManagement
```

#### Bước 3 — Tạo file `.env`

```bash
cp .env.docker .env
nano .env
```

Điền đầy đủ các biến:

```env
DB_PASSWORD=your_postgres_password
JWT_SECRET=your_jwt_secret_64_chars_random
MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=your_gmail_app_password
CLOUDINARY_CLOUD_NAME=your_cloud_name
CLOUDINARY_API_KEY=your_api_key
CLOUDINARY_API_SECRET=your_api_secret
CORS_ORIGINS=*
```

> ⚠️ **Bắt buộc phải điền đầy đủ các biến** — app sẽ không chạy nếu thiếu `DB_PASSWORD`, `JWT_SECRET`, `CLOUDINARY_API_SECRET`.

#### Bước 4 — Chạy Docker Compose

```bash
# Build và chạy (lần đầu)
docker-compose up -d --build

# Xem trạng thái
docker-compose ps

# Xem log
docker-compose logs -f app
```

#### Bước 5 — Mở port (nếu dùng firewall)

```bash
ufw allow 8080/tcp
```

#### Bước 6 — Test

```bash
curl http://localhost:8080/api/tasks
```

App chạy tại `http://160.22.107.121:8080`

---

### Cập nhật code mới khi dùng Docker

```bash
# 1. Pull code mới
cd /opt/TaskManagement
git pull origin main

# 2. Rebuild và chạy lại
docker-compose up -d --build

# 3. Xem log để kiểm tra
docker-compose logs -f app
```

---

### Các lệnh Docker thường dùng

```bash
# Dừng app
docker-compose down

# Xem log real-time
docker-compose logs -f

# Restart
docker-compose restart app

# Rebuild không dùng cache
docker-compose build --no-cache
docker-compose up -d

# Xóa hoàn toàn (bao gồm database)
docker-compose down -v

# Kiểm tra health
docker-compose ps

# Vào container để debug
docker exec -it taskmanagement-app sh
docker exec -it taskmanagement-db psql -U postgres -d task_management
```

---

### So sánh: Chạy trực tiếp vs Docker

| Tiêu chí | Chạy trực tiếp (JAR) | Docker Compose |
|-----------|----------------------|----------------|
| Cần cài Java | Có | Không |
| Cần cài PostgreSQL riêng | Có | Không (tự tạo) |
| Port mặc định | 8080 | 8080 |
| Khởi động lại | systemctl | docker restart |
| Log | journalctl | docker-compose logs |
| Rebuild | mvn + scp | docker-compose build |

---

### 4.10. Cấu hình Firewall

```bash
sudo ufw allow 22    # SSH
sudo ufw allow 8081  # Ứng dụng (hoặc dùng Nginx reverse proxy qua 80/443)
sudo ufw enable
```

### 4.8. (Tùy chọn) Nginx reverse proxy qua HTTPS

Cài SSL miễn phí với Certbot:

```bash
sudo apt install -y nginx certbot python3-certbot-nginx

# Cấu hình Nginx
sudo nano /etc/nginx/sites-available/taskmanagement
```

```nginx
server {
    listen 80;
    server_name your-domain.com;

    location / {
        proxy_pass http://127.0.0.1:8081;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

```bash
sudo ln -s /etc/nginx/sites-available/taskmanagement /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx

# Lấy SSL certificate
sudo certbot --nginx -d your-domain.com
```

---

## 5. Đưa lên GitHub

### 5.1. Khởi tạo Git repository (nếu chưa có)

```bash
cd TaskManagement
git init
git add .
git commit -m "Initial commit"
```

### 5.2. Loại trừ file nhạy cảm

Đảm bảo `.gitignore` có các mục sau:

```gitignore
# Sensitive config files
application-local.properties
src/main/resources/application-local.properties
.env
.env.*
*.env

# Build output
target/

# IDE
.idea/
*.iml

# OS
.DS_Store
Thumbs.db

# Maven
.mvn/wrapper/maven-wrapper.jar
```

### 5.3. Tạo repo trên GitHub

1. Vào [GitHub](https://github.com) → **New Repository**
2. Đặt tên: `TaskManagement`
3. **KHÔNG** tick "Initialize with README"
4. Copy remote URL (ví dụ: `https://github.com/username/TaskManagement.git`)

### 5.4. Push lên GitHub

```bash
# Thêm remote
git remote add origin https://github.com/username/TaskManagement.git

# Push lên nhánh main
git branch -M main
git push -u origin main
```

### 5.5. Thiết lập GitHub Secrets (nếu dùng CI/CD)

Vào **GitHub Repo → Settings → Secrets and variables → Actions**, thêm:

| Secret Name | Mô tả |
|-------------|-------|
| `DB_PASSWORD` | Mật khẩu PostgreSQL |
| `JWT_SECRET` | JWT secret key (Base64 256-bit) |
| `MAIL_USERNAME` | Gmail address |
| `MAIL_PASSWORD` | Gmail App Password |
| `CLOUDINARY_API_SECRET` | Cloudinary API Secret |
| `VPS_HOST` | IP VPS |
| `VPS_USERNAME` | SSH username |
| `VPS_SSH_KEY` | Private SSH key để deploy |

---

## 6. API Reference

Base URL: `http://localhost:8081/api` (local) hoặc `https://your-domain.com/api` (production)

### 6.1. Authentication

#### Đăng nhập

```
POST /api/auth/login
Content-Type: application/json

Request:
{
  "username": "manager",
  "password": "manager123"
}

Response (200):
{
  "message": "Login successful",
  "status": 200,
  "data": {
    "token": "eyJhbGciOiJIUzM4NCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 86400000,
    "user": {
      "id": 1,
      "username": "manager",
      "fullName": "Manager",
      "email": "manager@email.com",
      "role": "MANAGER",
      "active": true
    }
  }
}
```

#### Đăng ký Staff

```
POST /api/auth/register
Content-Type: application/json

Request:
{
  "username": "staff01",       // 6–50 ký tự
  "password": "Pass1234",      // Tối thiểu 8 ký tự, gồm chữ và số
  "email": "staff01@email.com",
  "fullName": "Nguyễn Văn A"  // 10–100 ký tự
}

Response (201):
{
  "message": "Staff account registered successfully. Please check your email to activate your account.",
  "status": 201,
  "data": null
}
```

#### Xác minh tài khoản (Staff)

```
POST /api/auth/verify
Content-Type: application/json

Request:
{
  "email": "staff01@email.com",
  "code": "123456"
}

Response (200):
{
  "message": "Account activated successfully. You can now log in.",
  "status": 200,
  "data": null
}
```

#### Gửi lại mã xác minh

```
POST /api/auth/send-verification-code
Content-Type: application/json

Request:
{
  "email": "staff01@email.com"
}

Response (200):
{
  "message": "Verification code sent to your email",
  "status": 200,
  "data": null
}
```

---

### 6.2. Tasks

**Tất cả các endpoint Task đều cần header:**
```
Authorization: Bearer <JWT_TOKEN>
```

#### Danh sách task (phân trang)

```
GET /api/tasks?status=PENDING&keyword=bug&page=0&size=10&sortBy=deadline&sortDir=asc

Query params:
  - status: PENDING | COMPLETED (optional)
  - keyword: tìm trong title, description (optional)
  - page: số trang, bắt đầu từ 0 (default: 0)
  - size: số bản ghi/trang, max 100 (default: 10)
  - sortBy: createdAt | updatedAt | deadline | title | startDate (default: createdAt)
  - sortDir: asc | desc (default: desc)

Response (200):
{
  "message": "Tasks retrieved successfully",
  "status": 200,
  "data": {
    "content": [
      {
        "id": 1,
        "title": "Fix login bug",
        "description": "Cannot login on mobile",
        "status": "PENDING",
        "startDate": "2026-07-05",
        "deadline": "2026-07-10",
        "assignedTo": {
          "id": 2,
          "username": "staff01",
          "fullName": "Nguyễn Văn A"
        },
        "assignedBy": {
          "id": 1,
          "username": "manager"
        },
        "category": {
          "id": 1,
          "name": "Bug Fix"
        },
        "completionNote": null,
        "completionImage": null,
        "completedAt": null,
        "createdAt": "2026-07-05T10:00:00",
        "updatedAt": "2026-07-05T10:00:00"
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 1,
    "totalPages": 1,
    "first": true,
    "last": true
  }
}
```

#### Chi tiết task

```
GET /api/tasks/{id}

Response (200):
{
  "message": "Task retrieved successfully",
  "status": 200,
  "data": { ... }
}
```

#### Tạo task thủ công (Manager)

```
POST /api/tasks
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json

Request:
{
  "title": "Fix login bug",
  "description": "Cannot login on mobile",
  "startDate": "2026-07-05",
  "deadline": "2026-07-10",
  "assignedToId": 2,
  "categoryId": 1
}

Response (201):
{
  "message": "Task created successfully",
  "status": 201,
  "data": { ... }
}
```

#### Giao task theo danh mục (Manager)

```
POST /api/tasks/assign-by-category
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json

Request:
{
  "categoryId": 1,
  "assignedToId": 2,
  "deadline": "2026-07-10",
  "description": "Optional extra description"
}

Response (201):
{
  "message": "Task assigned by category successfully",
  "status": 201,
  "data": { ... }
}
```

#### Cập nhật task (Manager)

```
PUT /api/tasks/{id}
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json

Request:
{
  "title": "Updated title",
  "description": "Updated description",
  "startDate": "2026-07-06",
  "deadline": "2026-07-15",
  "assignedToId": 3,
  "categoryId": 2
}

Response (200):
{
  "message": "Task updated successfully",
  "status": 200,
  "data": { ... }
}
```

#### Xóa task (Manager)

```
DELETE /api/tasks/{id}
Authorization: Bearer <JWT_TOKEN>

Response (200):
{
  "message": "Task deleted successfully",
  "status": 200,
  "data": null
}

Error (400) — task đã hoàn thành:
{
  "message": "Cannot delete a completed task. Completed tasks are preserved for records.",
  "status": 400,
  "data": null
}
```

#### Đổi trạng thái task

```
PATCH /api/tasks/{id}/status?status=COMPLETED
Authorization: Bearer <JWT_TOKEN>

Response (200):
{
  "message": "Task status updated successfully",
  "status": 200,
  "data": { ... }
}
```

#### Staff hoàn thành task

```
POST /api/tasks/{id}/complete
Authorization: Bearer <JWT_TOKEN>
Content-Type: multipart/form-data

Fields:
  - note: string (optional) — ghi chú khi hoàn thành
  - image: file (optional) — hình ảnh bằng chứng (max 5MB)

Response (200):
{
  "message": "Task completed successfully",
  "status": 200,
  "data": {
    "id": 1,
    "status": "COMPLETED",
    "completionNote": "Đã sửa xong",
    "completionImage": "https://res.cloudinary.com/...",
    "completedAt": "2026-07-05T15:00:00",
    ...
  }
}
```

#### Gửi email nhắc nhở (Manager)

```
POST /api/tasks/{id}/remind
Authorization: Bearer <JWT_TOKEN>

Response (200):
{
  "message": "Reminder email sent successfully",
  "status": 200,
  "data": null
}
```

---

### 6.3. Task Categories

```
GET /api/task-categories

Response (200):
{
  "message": "Task categories retrieved successfully",
  "status": 200,
  "data": [
    {
      "id": 1,
      "name": "Bug Fix",
      "description": "Các lỗi cần sửa",
      "taskCount": 5
    },
    {
      "id": 2,
      "name": "New Feature",
      "description": "Tính năng mới",
      "taskCount": 3
    }
  ]
}
```

```
POST /api/task-categories
Authorization: Bearer <JWT_TOKEN> (Manager only)
Content-Type: application/json

Request:
{
  "name": "Bug Fix",
  "description": "Các lỗi cần sửa"
}

Response (201):
{
  "message": "Task category created successfully",
  "status": 201,
  "data": { "id": 1, "name": "Bug Fix", "description": "Các lỗi cần sửa", "taskCount": 0 }
}
```

```
PUT /api/task-categories/{id}
Authorization: Bearer <JWT_TOKEN> (Manager only)
Content-Type: application/json

Request:
{
  "name": "Bug Fixes",
  "description": "Updated description"
}
```

```
DELETE /api/task-categories/{id}
Authorization: Bearer <JWT_TOKEN> (Manager only)

Response (200) — xóa thành công
Response (400) — không xóa được nếu đang có task sử dụng category này
```

---

### 6.4. Users

#### Lấy thông tin cá nhân

```
GET /api/users/me
Authorization: Bearer <JWT_TOKEN>

Response (200):
{
  "message": "User profile retrieved successfully",
  "status": 200,
  "data": {
    "id": 1,
    "username": "manager",
    "fullName": "Manager",
    "email": "manager@email.com",
    "role": "MANAGER",
    "active": true
  }
}
```

#### Cập nhật thông tin cá nhân

```
PUT /api/users/me
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json

Request:
{
  "fullName": "Nguyễn Quản Lý",
  "email": "newemail@email.com"
}

Response (200):
{
  "message": "Profile updated successfully",
  "status": 200,
  "data": { ... }
}
```

#### Đổi mật khẩu

```
PUT /api/users/me/change-password
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json

Request:
{
  "currentPassword": "oldpassword",
  "newPassword": "NewPass123"  // Tối thiểu 8 ký tự, gồm chữ và số
}

Response (200):
{
  "message": "Password changed successfully",
  "status": 200,
  "data": null
}

Error (400) — sai mật khẩu cũ:
{
  "message": "Current password is incorrect",
  "status": 400,
  "data": null
}
```

#### Danh sách người dùng (Manager only)

```
GET /api/users?role=STAFF&active=true
Authorization: Bearer <JWT_TOKEN> (Manager only)

Response (200):
{
  "message": "Users retrieved successfully",
  "status": 200,
  "data": [
    {
      "id": 2,
      "username": "staff01",
      "fullName": "Nguyễn Văn A",
      "email": "staff01@email.com",
      "role": "STAFF",
      "active": true
    }
  ]
}
```

---

## 7. Môi trường & Biến số

### Biến môi trường (Environment Variables)

| Biến | Mô tả | Bắt buộc | Ví dụ |
|------|--------|-----------|-------|
| `DB_HOST` | Host PostgreSQL | Có | `localhost` |
| `DB_PORT` | Port PostgreSQL | Không | `5432` |
| `DB_NAME` | Tên database | Có | `task_management` |
| `DB_USERNAME` | Username PostgreSQL | Có | `postgres` |
| `DB_PASSWORD` | Password PostgreSQL | **Có** | `your_db_password` |
| `JWT_SECRET` | JWT secret key (Base64, ≥256-bit) | **Có** | `ZHVtbXlTZWNyZXRLZXlGb3JKV1RUZXN0aW5nMjU2Yml0cw==` |
| `JWT_EXPIRATION` | Token expire (ms) | Không | `86400000` |
| `MAIL_USERNAME` | Gmail address | **Có** | `your_email@gmail.com` |
| `MAIL_PASSWORD` | Gmail App Password | **Có** | `xxxx xxxx xxxx xxxx` |
| `CLOUDINARY_CLOUD_NAME` | Cloud name | Không | `dyrksdywm` |
| `CLOUDINARY_API_KEY` | API Key | Không | `947124991745588` |
| `CLOUDINARY_API_SECRET` | API Secret | **Có** | `your_cloudinary_secret` |
| `CORS_ORIGINS` | Danh sách origin (phân cách bởi dấu phẩy) | Không | `https://yourdomain.com` |

### Tạo JWT Secret mới

```bash
# Linux/Mac
openssl rand -base64 32

# PowerShell (Windows)
[Convert]::ToBase64String((1..32 | ForEach-Object { Get-Random -Maximum 256 }) -as [byte[]])

# Java command line
java -cp "target/taskmanagement-0.0.1-SNAPSHOT.jar" org.springframework.boot.loader.launch.JarLauncher 2>/dev/null || \
echo "Use: openssl rand -base64 32" # fallback
```

> **Quan trọng:** JWT Secret phải là Base64-encoded, độ dài tối thiểu 32 bytes (sau khi encode ~44 ký tự).

---

## Cấu trúc project

```
TaskManagement/
├── src/main/java/com/example/TaskManagement/
│   ├── config/          # Security, CORS, Swagger config
│   ├── controller/      # REST Controllers (Auth, Task, User, TaskCategory)
│   ├── dto/             # Request & Response DTOs
│   ├── exception/       # Custom exceptions & handlers
│   ├── model/           # JPA Entities + Enums
│   ├── repository/      # JPA Repositories
│   ├── scheduler/       # Cron job gửi email nhắc deadline
│   ├── security/        # JWT filter, UserDetails
│   └── service/         # Business logic + Implementation
├── src/main/resources/
│   ├── application.properties        # Config chung (không có secret)
│   └── application-local.properties   # Secret local (KHÔNG push lên GitHub)
├── src/test/
├── pom.xml
├── .gitignore
└── README.md
```
