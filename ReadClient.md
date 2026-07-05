   # Task Management API — Hướng dẫn Client A-Z (VibeCode)

> Tài liệu này mô tả đầy đủ cách khởi chạy backend và gọi toàn bộ REST API.
> Paste file này vào VibeCode để xây dựng frontend/client tương ứng.

---

## 1. Tổng quan hệ thống

| Hạng mục | Giá trị |
|----------|---------|
| Framework | Spring Boot 4.1 (Java 21) |
| Database | PostgreSQL |
| Auth | JWT Bearer Token |
| Base URL (local) | `http://localhost:8080` |
| Content-Type | `application/json` |

### Vai trò (Role)

| Role | Mô tả |
|------|-------|
| `MANAGER` | 1 tài khoản duy nhất, init khi server khởi động. Xem/sửa/xóa tất cả task, giao task cho staff, gửi email nhắc nhở. |
| `STAFF` | Đăng ký qua email + mã xác minh. Chỉ quản lý task được gán cho mình. |

### Tài khoản Manager mặc định

```
Username: manager
Password: manager123
Email:    manager@task.com
```

> Manager **không** đăng ký qua API. Chỉ có 1 manager trong hệ thống.

---

## 2. Khởi chạy Backend (bắt buộc trước khi gọi API)

### Bước 1 — Cài PostgreSQL, tạo database

```sql
CREATE DATABASE task_management;
```

### Bước 2 — Cấu hình `application.properties`

File: `src/main/resources/application.properties`

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/task_management
spring.datasource.username=postgres
spring.datasource.password=postgres
```

### Bước 3 — Cấu hình Gmail (gửi mã xác minh + email nhắc nhở)

File: `src/main/resources/application-local.properties`

```properties
spring.mail.username=your-email@gmail.com
spring.mail.password=your-gmail-app-password
```

> Cần bật **2-Step Verification** trên Google và tạo **App Password**.

### Bước 4 — Chạy server

```bash
# Windows
.\mvnw.cmd spring-boot:run

# Linux/Mac
./mvnw spring-boot:run
```

Server chạy tại: `http://localhost:8080`

---

## 3. Format Response chuẩn (TẤT CẢ API)

Mọi response đều theo cấu trúc:

```json
{
  "message": "Mô tả kết quả",
  "status": 200,
  "data": { }
}
```

| Field | Kiểu | Mô tả |
|-------|------|-------|
| `message` | string | Thông báo kết quả |
| `status` | number | HTTP status code (200, 201, 400, 401, 403, 404, 500) |
| `data` | object / array / null | Dữ liệu trả về (có thể null) |

### Response lỗi ví dụ

```json
{
  "message": "Invalid verification code",
  "status": 400
}
```

### Response lỗi validation

```json
{
  "message": "Validation failed",
  "status": 400,
  "data": {
    "email": "Email must be valid",
    "password": "Password must be at least 6 characters"
  }
}
```

---

## 4. Xác thực JWT

### Header bắt buộc (các API cần đăng nhập)

```
Authorization: Bearer <token>
Content-Type: application/json
```

### Lấy token

- Gọi `POST /api/auth/login` → lấy `data.token`
- Hoặc `POST /api/auth/register` (staff) → trả token luôn sau khi đăng ký thành công

### Lưu token ở client

```javascript
// Sau login/register thành công
localStorage.setItem("token", response.data.token);
localStorage.setItem("role", response.data.role);
localStorage.setItem("username", response.data.username);
```

### Gọi API có auth

```javascript
fetch("http://localhost:8080/api/tasks", {
  method: "GET",
  headers: {
    "Content-Type": "application/json",
    "Authorization": `Bearer ${localStorage.getItem("token")}`
  }
});
```

### Token hết hạn

- JWT hết hạn sau **24 giờ** (`jwt.expiration=86400000` ms)
- Khi nhận `401` → redirect về trang Login

---

## 5. Enum / Constants dùng trong API

### TaskStatus

| Giá trị | Ý nghĩa |
|---------|---------|
| `PENDING` | Chưa hoàn thành |
| `COMPLETED` | Đã hoàn thành |

### Role

| Giá trị | Ý nghĩa |
|---------|---------|
| `MANAGER` | Quản lý |
| `STAFF` | Nhân viên |

### Định dạng datetime

```
ISO-8601: "2026-07-10T17:00:00"
```

---

## 6. TypeScript Interfaces (cho VibeCode)

```typescript
// === Response wrapper ===
interface ApiResponse<T> {
  message: string;
  status: number;
  data: T | null;
}

// === Auth ===
interface LoginRequest {
  username: string;
  password: string;
}

interface SendVerificationCodeRequest {
  email: string;
}

interface RegisterRequest {
  username: string;       // 3-50 ký tự
  password: string;       // tối thiểu 6 ký tự
  email: string;
  verificationCode: string; // đúng 6 chữ số
}

interface AuthResponse {
  token: string;
  username: string;
  role: "MANAGER" | "STAFF";
}

// === Task ===
type TaskStatus = "PENDING" | "COMPLETED";

interface TaskRequest {
  title: string;           // bắt buộc, max 200
  description?: string;    // max 2000
  assignedToId?: number; // ID user staff (manager giao task)
  deadline?: string;       // ISO datetime, bắt buộc khi manager giao cho staff
}

interface TaskResponse {
  id: number;
  title: string;
  description: string;
  status: TaskStatus;
  assignedToId: number;
  assignedToUsername: string;
  createdById: number;
  createdByUsername: string;
  createdAt: string;
  updatedAt: string;
  deadline: string | null;
}
```

---

## 7. API AUTH — Không cần token

---

### 7.1. Gửi mã xác minh email (Staff đăng ký — Bước 1)

```
POST /api/auth/send-verification-code
```

**Request Body:**
```json
{
  "email": "staff@gmail.com"
}
```

**Response 200:**
```json
{
  "message": "Verification code sent to your email",
  "status": 200,
  "data": null
}
```

**Lưu ý:**
- Mã 6 chữ số, hiệu lực **10 phút**
- Email đã đăng ký → lỗi 400 `"Email is already registered"`
- Gọi lại API này sẽ **thay mã cũ** bằng mã mới

**cURL:**
```bash
curl -X POST http://localhost:8080/api/auth/send-verification-code \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"staff@gmail.com\"}"
```

**fetch:**
```javascript
const res = await fetch("http://localhost:8080/api/auth/send-verification-code", {
  method: "POST",
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify({ email: "staff@gmail.com" })
});
const json = await res.json();
console.log(json.message); // "Verification code sent to your email"
```

---

### 7.2. Đăng ký Staff (Bước 2 — sau khi nhận mã email)

```
POST /api/auth/register
```

**Request Body:**
```json
{
  "username": "staff01",
  "password": "staff123",
  "email": "staff@gmail.com",
  "verificationCode": "482913"
}
```

**Response 201:**
```json
{
  "message": "Staff account registered successfully",
  "status": 201,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "username": "staff01",
    "role": "STAFF"
  }
}
```

**Lỗi thường gặp:**

| message | status | Nguyên nhân |
|---------|--------|-------------|
| Invalid verification code | 400 | Mã sai |
| Verification code has expired | 400 | Mã hết hạn (>10 phút) |
| Username already exists | 400 | Username trùng |
| Email already exists | 400 | Email trùng |

**cURL:**
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"staff01\",\"password\":\"staff123\",\"email\":\"staff@gmail.com\",\"verificationCode\":\"482913\"}"
```

---

### 7.3. Đăng nhập

```
POST /api/auth/login
```

**Request Body:**
```json
{
  "username": "manager",
  "password": "manager123"
}
```

**Response 200:**
```json
{
  "message": "Login successful",
  "status": 200,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "username": "manager",
    "role": "MANAGER"
  }
}
```

**Lỗi 401:**
```json
{
  "message": "Invalid username or password",
  "status": 401
}
```

**cURL:**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"manager\",\"password\":\"manager123\"}"
```

---

## 8. API TASKS — Cần token JWT

> Thay `<TOKEN>` bằng JWT thực tế trong tất cả ví dụ bên dưới.

---

### 8.1. Lấy danh sách task

```
GET /api/tasks
GET /api/tasks?status=PENDING
GET /api/tasks?status=COMPLETED
GET /api/tasks?keyword=báo cáo
GET /api/tasks?status=PENDING&keyword=báo cáo
```

**Query params (tất cả optional):**

| Param | Kiểu | Mô tả |
|-------|------|-------|
| `status` | `PENDING` \| `COMPLETED` | Lọc theo trạng thái |
| `keyword` | string | Tìm trong title và description |

**Phân quyền:**
- `MANAGER` → thấy tất cả task
- `STAFF` → chỉ thấy task gán cho mình

**Response 200:**
```json
{
  "message": "Tasks retrieved successfully",
  "status": 200,
  "data": [
    {
      "id": 1,
      "title": "Viết báo cáo tuần",
      "description": "Báo cáo KPI tháng 7",
      "status": "PENDING",
      "assignedToId": 2,
      "assignedToUsername": "staff01",
      "createdById": 1,
      "createdByUsername": "manager",
      "createdAt": "2026-07-05T10:00:00",
      "updatedAt": "2026-07-05T10:00:00",
      "deadline": "2026-07-10T17:00:00"
    }
  ]
}
```

**cURL:**
```bash
curl -X GET "http://localhost:8080/api/tasks?status=PENDING" \
  -H "Authorization: Bearer <TOKEN>"
```

---

### 8.2. Lấy chi tiết 1 task

```
GET /api/tasks/{id}
```

**Response 200:**
```json
{
  "message": "Task retrieved successfully",
  "status": 200,
  "data": {
    "id": 1,
    "title": "Viết báo cáo tuần",
    "description": "Báo cáo KPI tháng 7",
    "status": "PENDING",
    "assignedToId": 2,
    "assignedToUsername": "staff01",
    "createdById": 1,
    "createdByUsername": "manager",
    "createdAt": "2026-07-05T10:00:00",
    "updatedAt": "2026-07-05T10:00:00",
    "deadline": "2026-07-10T17:00:00"
  }
}
```

**Lỗi 404:** `"Task not found with id: {id}"`
**Lỗi 403 (Staff):** `"You do not have permission to access this task"`

---

### 8.3. Tạo task mới

```
POST /api/tasks
```

**Request Body (Manager giao cho Staff):**
```json
{
  "title": "Viết báo cáo tuần",
  "description": "Báo cáo KPI tháng 7",
  "assignedToId": 2,
  "deadline": "2026-07-10T17:00:00"
}
```

**Request Body (Staff tự tạo task):**
```json
{
  "title": "Task cá nhân",
  "description": "Ghi chú công việc"
}
```

**Quy tắc:**
- Manager giao task cho staff khác → **bắt buộc** `deadline` (thời gian tương lai)
- Staff chỉ gán task cho chính mình
- Khi manager giao task có deadline → **tự động gửi email** thông báo tới email staff

**Response 201:**
```json
{
  "message": "Task created successfully",
  "status": 201,
  "data": {
    "id": 1,
    "title": "Viết báo cáo tuần",
    "status": "PENDING",
    "deadline": "2026-07-10T17:00:00",
    ...
  }
}
```

**cURL:**
```bash
curl -X POST http://localhost:8080/api/tasks \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d "{\"title\":\"Viết báo cáo tuần\",\"description\":\"Báo cáo KPI\",\"assignedToId\":2,\"deadline\":\"2026-07-10T17:00:00\"}"
```

---

### 8.4. Chỉnh sửa task

```
PUT /api/tasks/{id}
```

**Request Body:**
```json
{
  "title": "Viết báo cáo tuần (cập nhật)",
  "description": "Bổ sung số liệu Q2",
  "assignedToId": 2,
  "deadline": "2026-07-12T17:00:00"
}
```

**Response 200:**
```json
{
  "message": "Task updated successfully",
  "status": 200,
  "data": { ... }
}
```

> Đổi deadline → reset trạng thái nhắc nhở email, gửi lại email giao việc nếu manager giao cho staff.

---

### 8.5. Xóa task

```
DELETE /api/tasks/{id}
```

**Response 200:**
```json
{
  "message": "Task deleted successfully",
  "status": 200,
  "data": null
}
```

---

### 8.6. Đánh dấu hoàn thành / chưa hoàn thành

```
PATCH /api/tasks/{id}/status?status=COMPLETED
PATCH /api/tasks/{id}/status?status=PENDING
```

**Query param bắt buộc:**

| Param | Giá trị |
|-------|---------|
| `status` | `PENDING` hoặc `COMPLETED` |

**Response 200:**
```json
{
  "message": "Task status updated successfully",
  "status": 200,
  "data": {
    "id": 1,
    "status": "COMPLETED",
    ...
  }
}
```

**cURL:**
```bash
curl -X PATCH "http://localhost:8080/api/tasks/1/status?status=COMPLETED" \
  -H "Authorization: Bearer <TOKEN>"
```

---

### 8.7. Gửi email nhắc nhở (chỉ MANAGER)

```
POST /api/tasks/{id}/remind
```

**Response 200:**
```json
{
  "message": "Reminder email sent successfully",
  "status": 200,
  "data": null
}
```

**Lỗi:**
- 403: `"Only managers can send task reminders"`
- 400: `"Cannot send reminder for a completed task"`

**cURL:**
```bash
curl -X POST http://localhost:8080/api/tasks/1/remind \
  -H "Authorization: Bearer <TOKEN>"
```

---

## 9. Email tự động (không cần gọi API)

| Sự kiện | Hành động |
|---------|-----------|
| Staff đăng ký bước 1 | Gửi mã xác minh 6 số tới email |
| Manager giao task có deadline | Gửi email thông báo công việc mới |
| Mỗi ngày 8:00 sáng | Nhắc nhở task PENDING sắp đến hạn (trong 24h) hoặc quá hạn |
| Manager gọi `/remind` | Gửi nhắc nhở ngay lập tức |

---

## 10. Luồng sử dụng đầy đủ (End-to-End)

### Luồng A — Staff đăng ký & làm việc

```
1. POST /api/auth/send-verification-code     { email }
2. Kiểm tra email → lấy mã 6 số
3. POST /api/auth/register                 { username, password, email, verificationCode }
4. Lưu token từ response
5. GET  /api/tasks                         (xem task được giao)
6. PATCH /api/tasks/{id}/status?status=COMPLETED  (đánh dấu hoàn thành)
```

### Luồng B — Manager quản lý task

```
1. POST /api/auth/login                    { username: "manager", password: "manager123" }
2. Lưu token
3. POST /api/tasks                         { title, description, assignedToId, deadline }
4. GET  /api/tasks?status=PENDING          (theo dõi task chưa xong)
5. POST /api/tasks/{id}/remind             (nhắc staff qua email)
6. PUT  /api/tasks/{id}                    (sửa task nếu cần)
7. DELETE /api/tasks/{id}                  (xóa task)
```

---

## 11. Bảng tổng hợp tất cả Endpoint

| # | Method | Endpoint | Auth | Role | Mô tả |
|---|--------|----------|------|------|-------|
| 1 | POST | `/api/auth/send-verification-code` | Không | Public | Gửi mã xác minh email |
| 2 | POST | `/api/auth/register` | Không | Public | Đăng ký Staff |
| 3 | POST | `/api/auth/login` | Không | Public | Đăng nhập |
| 4 | GET | `/api/tasks` | Có | MANAGER, STAFF | Danh sách task |
| 5 | GET | `/api/tasks/{id}` | Có | MANAGER, STAFF | Chi tiết task |
| 6 | POST | `/api/tasks` | Có | MANAGER, STAFF | Tạo task |
| 7 | PUT | `/api/tasks/{id}` | Có | MANAGER, STAFF | Sửa task |
| 8 | DELETE | `/api/tasks/{id}` | Có | MANAGER, STAFF | Xóa task |
| 9 | PATCH | `/api/tasks/{id}/status` | Có | MANAGER, STAFF | Đổi trạng thái |
| 10 | POST | `/api/tasks/{id}/remind` | Có | MANAGER only | Gửi email nhắc nhở |

---

## 12. HTTP Status Codes

| Code | Ý nghĩa |
|------|---------|
| 200 | Thành công |
| 201 | Tạo mới thành công |
| 400 | Dữ liệu không hợp lệ / logic lỗi |
| 401 | Chưa đăng nhập / token sai / hết hạn |
| 403 | Không có quyền |
| 404 | Không tìm thấy resource |
| 500 | Lỗi server |

---

## 13. Gợi ý cấu trúc Frontend (VibeCode)

### Pages cần xây

| Page | Route gợi ý | API sử dụng |
|------|---------------|-------------|
| Login | `/login` | POST `/api/auth/login` |
| Register Staff | `/register` | POST send-verification-code → POST register |
| Task List | `/tasks` | GET `/api/tasks` |
| Task Detail | `/tasks/:id` | GET `/api/tasks/{id}` |
| Create Task | `/tasks/new` | POST `/api/tasks` |
| Edit Task | `/tasks/:id/edit` | PUT `/api/tasks/{id}` |

### API Service mẫu (JavaScript)

```javascript
const BASE_URL = "http://localhost:8080";

function getHeaders() {
  const token = localStorage.getItem("token");
  return {
    "Content-Type": "application/json",
    ...(token ? { Authorization: `Bearer ${token}` } : {})
  };
}

async function api(method, path, body) {
  const res = await fetch(`${BASE_URL}${path}`, {
    method,
    headers: getHeaders(),
    body: body ? JSON.stringify(body) : undefined
  });
  const json = await res.json();
  if (json.status >= 400) throw new Error(json.message);
  return json;
}

// Auth
export const sendVerificationCode = (email) =>
  api("POST", "/api/auth/send-verification-code", { email });

export const register = (data) =>
  api("POST", "/api/auth/register", data);

export const login = (username, password) =>
  api("POST", "/api/auth/login", { username, password });

// Tasks
export const getTasks = (status, keyword) => {
  const params = new URLSearchParams();
  if (status) params.set("status", status);
  if (keyword) params.set("keyword", keyword);
  const qs = params.toString();
  return api("GET", `/api/tasks${qs ? `?${qs}` : ""}`);
};

export const getTask = (id) => api("GET", `/api/tasks/${id}`);

export const createTask = (data) => api("POST", "/api/tasks", data);

export const updateTask = (id, data) => api("PUT", `/api/tasks/${id}`, data);

export const deleteTask = (id) => api("DELETE", `/api/tasks/${id}`);

export const updateTaskStatus = (id, status) =>
  api("PATCH", `/api/tasks/${id}/status?status=${status}`);

export const sendReminder = (id) => api("POST", `/api/tasks/${id}/remind`);
```

### Route Guard gợi ý

```javascript
// Chưa login → redirect /login
if (!localStorage.getItem("token")) navigate("/login");

// Chỉ MANAGER mới thấy nút "Gửi nhắc nhở" và form giao task cho staff
if (localStorage.getItem("role") === "MANAGER") { /* show manager UI */ }
```

---

## 14. Checklist test API (Postman / Thunder Client)

- [ ] Login manager → nhận token
- [ ] Gửi mã xác minh tới email thật
- [ ] Đăng ký staff với mã đúng → nhận token
- [ ] Login staff → nhận token
- [ ] Manager tạo task giao staff (có deadline) → staff nhận email
- [ ] Staff GET tasks → chỉ thấy task của mình
- [ ] Manager GET tasks → thấy tất cả
- [ ] Lọc `?status=PENDING`
- [ ] Tìm kiếm `?keyword=báo`
- [ ] PATCH status COMPLETED
- [ ] Manager POST remind → staff nhận email
- [ ] PUT sửa task
- [ ] DELETE xóa task
- [ ] Gọi API không token → 401
- [ ] Staff truy cập task người khác → 403

---

## 15. Lưu ý quan trọng cho VibeCode

1. **Luôn parse `message` và `status`** từ response, không dùng HTTP status code alone.
2. **Lưu JWT** sau login/register, gắn vào mọi request task.
3. **Register 2 bước**: form email → gửi mã → form đăng ký đầy đủ.
4. **Manager form tạo task**: bắt buộc chọn staff + deadline (datetime picker).
5. **Staff form tạo task**: không cần chọn người nhận, deadline optional.
6. **Filter UI**: dropdown status (ALL / PENDING / COMPLETED) + ô search keyword.
7. **Toggle hoàn thành**: gọi PATCH status thay vì PUT toàn bộ task.
8. **CORS**: nếu frontend chạy port khác (ví dụ `localhost:3000`), backend cần thêm `@CrossOrigin` hoặc CORS config — hiện chưa có, VibeCode có thể yêu cầu thêm nếu gặp lỗi CORS.

---

*Tài liệu đồng bộ với codebase TaskManagement — Spring Boot REST API.*
