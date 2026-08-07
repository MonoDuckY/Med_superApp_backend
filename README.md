# Med Super App Backend — Hướng dẫn tích hợp Frontend

> Đặc tả kỹ thuật của các luồng backend nằm tại [`docs/specs/README.md`](docs/specs/README.md). Mọi thay đổi API, business rule, database hoặc security phải cập nhật spec liên quan và [`CHANGELOG.md`](CHANGELOG.md) trong cùng pull request.

Tài liệu này dành cho Web Admin, Web Doctor và Mobile App. Hiện backend đã triển khai nhóm API **xác thực** và **quản lý tài khoản của Admin**.

> Backend hiện có Auth, Admin User, Doctor/Staff scheduling, Patient appointment, Doctor examination, prescription, medicine schedule và Android SMS Gateway. Xem spec để phân biệt chức năng hiện tại với thay đổi ERD đang được lên kế hoạch.

##-3.6.vẻy importance
-Cách chạy: clone về
+dung inteliJ: copy đống trên Backend fix tại business doc về paste vào variable inviroment
+ko thì copy đống đấy gửi AI
-Vào firebase->project setting-> service accounts-> generate a private key( chọn java, dù tải cái đéo j cũng như nhau) 
-Nhét vào đâu thì đổi đường dẫn ở đó, r thay cái FIREBASE_SERVICE_ACCOUNT_PATH= cái đường dẫn đến file đó
-Chạy backend và mong nó hoạt động
PS: lỗi thì hỏi AI đừng hỏi t, t cũng đéo biết đâu :P  






## 1. Môi trường và Base URL

| Môi trường | Base URL |
| --- | --- |
| Local development | `http://localhost:8080` |

Mọi request có body phải gửi header:

```http
Content-Type: application/json
```

Với endpoint yêu cầu đăng nhập, gửi thêm:

```http
Authorization: Bearer <accessToken>
```

> Chỉ gửi chuỗi `accessToken`, không gửi `refreshToken` trong header. Từ khóa `Bearer` là bắt buộc.

## 2. Chuẩn phản hồi chung

Tất cả API trả về cùng một wrapper:

```json
{
  "success": true,
  "message": "Login successful.",
  "data": {},
  "errorCode": null
}
```

| Field | Kiểu | Ý nghĩa |
| --- | --- | --- |
| `success` | boolean | `true` nếu request thành công. |
| `message` | string | Thông điệp có thể hiển thị cho người dùng. |
| `data` | object/array/null | Dữ liệu của API. |
| `errorCode` | string/null | Mã lỗi machine-readable; `null` khi thành công. |

Ví dụ lỗi:

```json
{
  "success": false,
  "message": "Authentication is required.",
  "data": null,
  "errorCode": "UNAUTHORIZED"
}
```

## 3. Role và quyền

| Role | Diễn giải | Client chính |
| --- | --- | --- |
| `ADMIN` | Quản lý account, role và trạng thái account. | Web Admin |
| `DOCTOR` | Bác sĩ. | Web Doctor |
| `STAFF` | Nhân viên y tế/lễ tân. | Web |
| `RESEARCHER` | Nghiên cứu viên. | Web |
| `PATIENT` | Bệnh nhân. | Mobile App |

- Mỗi account chỉ có **một** role.
- Chỉ account có `status: "ACTIVE"` được đăng nhập và sử dụng token.
- Tất cả endpoint `/api/admin/users/**` chỉ cho role `ADMIN`.

## 4. Luồng đăng nhập và token

1. Gọi `POST /api/auth/login` bằng phone number/password.
2. Lưu `accessToken`, `refreshToken` và thông tin `user` từ response.
3. Gửi `accessToken` trong header Bearer cho API cần đăng nhập.
4. Khi access token hết hạn, gọi `POST /api/auth/refresh` với refresh token.
5. API refresh trả về **cặp token mới**. Frontend phải thay cả access token lẫn refresh token cũ.
6. Khi logout hoặc đổi password, xóa token đang lưu trên client.

Thời hạn mặc định của backend:

| Token | Thời hạn mặc định |
| --- | --- |
| Access token | 15 phút |
| Refresh token | 7 ngày |

Khuyến nghị lưu token:

- Mobile: dùng secure storage của hệ điều hành.
- Web: ưu tiên memory hoặc cơ chế storage bảo mật; không log token, không đưa token vào URL.

## 5. Danh sách API

| Method | Endpoint | Yêu cầu quyền | Mô tả |
| --- | --- | --- | --- |
| `POST` | `/api/auth/login` | Public | Đăng nhập và nhận token. |
| `POST` | `/api/auth/refresh` | Public | Đổi refresh token lấy cặp token mới. |
| `POST` | `/api/auth/logout` | Authenticated | Thu hồi refresh token hiện tại. |
| `POST` | `/api/auth/change-password` | Authenticated | Đổi password hiện tại. |
| `POST` | `/api/auth/forgot-password/request` | Public | Gửi OTP đặt lại mật khẩu theo số điện thoại. |
| `POST` | `/api/auth/forgot-password/verify` | Public | Xác minh OTP và nhận reset token dùng một lần. |
| `POST` | `/api/auth/forgot-password/reset` | Public | Đặt mật khẩu mới bằng reset token. |
| `GET` | `/api/auth/me` | Authenticated | Lấy profile account đang đăng nhập. |
| `GET` | `/api/staff/patients/search?name={name}&phoneNumber={phone}&citizenIdentificationCode={cccd}&n={count}` | `STAFF` | Tìm Patient bằng tên, số điện thoại hoặc CCCD. |
| `GET` | `/api/patient/doctors` | `PATIENT` | Lấy danh sách Doctor active. |
| `GET` | `/api/staff/doctors` | `STAFF` | Lấy danh sách Doctor active cho Staff. |
| `GET` | `/api/doctor/appointments` | `DOCTOR` | Lấy các appointment thuộc Doctor hiện tại. |
| `GET` | `/api/doctor/work-schedules/all` | `DOCTOR` | Lấy lịch làm việc của mọi Doctor; hỗ trợ `from`, `to`, `status`. |
| `GET` | `/api/doctor/appointments/{appointmentId}` | `DOCTOR` | Lấy hồ sơ khám, MedicalRecord và prescriptions. |
| `PATCH` | `/api/doctor/appointments/{appointmentId}/start` | `DOCTOR` | Chuyển appointment sang `IN_PROGRESS`. |
| `PATCH` | `/api/doctor/appointments/{appointmentId}/clinical-information` | `DOCTOR` | Cập nhật sức khỏe tổng quát và MedicalRecord. |
| `PATCH` | `/api/doctor/appointments/{appointmentId}/diagnosis` | `DOCTOR` | Cập nhật diagnosis trong MedicalRecord. |
| `POST` | `/api/doctor/appointments/{appointmentId}/prescriptions` | `DOCTOR` | Tạo prescription cùng medicine schedules. |
| `PATCH` | `/api/doctor/appointments/{appointmentId}/prescriptions/{prescriptionId}` | `DOCTOR` | Cập nhật prescription và thay thế schedules. |
| `PATCH` | `/api/doctor/appointments/{appointmentId}/complete` | `DOCTOR` | Hoàn thành examination. |
| `GET` | `/api/patient/medicine-schedules` | `PATIENT` | Lấy lịch uống thuốc của Patient. |
| `PATCH` | `/api/patient/medicine-schedules/{scheduleId}/time` | `PATIENT` | Đổi thời gian của schedule `NOT_YET`. |
| `PATCH` | `/api/patient/medicine-schedules/{scheduleId}/take` | `PATIENT` | Đánh dấu schedule là `TAKEN`. |
| `POST` | `/api/admin/users` | `ADMIN` | Tạo account. |
| `GET` | `/api/admin/users` | `ADMIN` | Lấy hoặc lọc account theo phone, CCCD và role. |
| `GET` | `/api/admin/users/{userId}` | `ADMIN` | Lấy chi tiết một account. |
| `PATCH` | `/api/admin/users/{userId}` | `ADMIN` | Cập nhật các field được gửi lên. |
| `DELETE` | `/api/admin/users/{userId}` | `ADMIN` | Khóa account, không xóa cứng dữ liệu. |

---

## 6. API xác thực

### 6.1 Đăng nhập

```http
POST /api/auth/login
```

Request body:

```json
{
  "phoneNumber": "0912345678",
  "password": "Admin123!",
  "deviceId": "optional-device-id"
}
```

| Field | Bắt buộc | Quy tắc |
| --- | --- | --- |
| `phoneNumber` | Có | Không rỗng; dùng `0xxxxxxxxx` hoặc `+84xxxxxxxxx`. |
| `password` | Có | Không rỗng, tối đa 50 ký tự. |
| `deviceId` | Không | Tối đa 255 ký tự; Mobile App nên gửi device ID ổn định. |

Response `200`:

```json
{
  "success": true,
  "message": "Login successful.",
  "data": {
    "accessToken": "eyJ...",
    "refreshToken": "QmFzZTY0VXJsVG9rZW4...",
    "tokenType": "Bearer",
    "expiresInSeconds": 900,
    "user": {
      "id": "687...",
      "role": "ADMIN",
      "status": "ACTIVE",
      "patientId": null,
      "fullName": "System Administrator",
      "gender": null,
      "dateOfBirth": null,
      "phoneNumber": "+84912345678",
      "createdAt": "2026-07-21T08:00:00Z",
      "updatedAt": "2026-07-21T08:00:00Z",
      "lastLoginAt": "2026-07-21T08:15:00Z"
    }
  },
  "errorCode": null
}
```

Lỗi thường gặp: `401 UNAUTHORIZED` nếu sai credential hoặc account bị `DISABLED`.

### 6.2 Làm mới token

```http
POST /api/auth/refresh
```

```json
{
  "refreshToken": "refresh-token-nhan-tu-login"
}
```

Response `200` có cùng cấu trúc `data` như API login. Refresh token cũ bị thu hồi sau khi refresh thành công; luôn ghi đè token cũ trên client.

### 6.3 Đăng xuất

```http
POST /api/auth/logout
Authorization: Bearer <accessToken>
```

```json
{
  "refreshToken": "refresh-token-hien-tai"
}
```

Response `200`:

```json
{
  "success": true,
  "message": "Logout successful.",
  "data": null,
  "errorCode": null
}
```

Sau response thành công, frontend phải xóa access token, refresh token và user state.

### 6.4 Đổi password

```http
POST /api/auth/change-password
Authorization: Bearer <accessToken>
```

```json
{
  "currentPassword": "Admin123!",
  "newPassword": "Admin456!",
  "confirmPassword": "Admin456!"
}
```

Yêu cầu password mới:

- 8–50 ký tự.
- Có ít nhất một chữ thường.
- Có ít nhất một chữ hoa.
- Có ít nhất một chữ số hoặc ký tự đặc biệt.
- Khác password hiện tại.

Sau khi đổi password thành công, backend thu hồi toàn bộ refresh token của account. Frontend nên xóa token hiện tại và điều hướng về màn hình login.

### 6.5 Quên mật khẩu

Yêu cầu OTP khi chưa đăng nhập:

```http
POST /api/auth/forgot-password/request
Content-Type: application/json
```

```json
{
  "phoneNumber": "0912345678"
}
```

Response luôn dùng thông báo chung để không làm lộ số điện thoại có tài khoản hay không. Tài khoản chỉ có role `PATIENT` không dùng API này vì Patient đăng nhập bằng OTP và không có mật khẩu.

Xác minh OTP và đặt mật khẩu mới:

```http
POST /api/auth/forgot-password/verify
Content-Type: application/json
```

```json
{
  "phoneNumber": "0912345678",
  "code": "123456"
}
```

Nếu OTP hợp lệ, response `data` trả về `resetToken` và `expiresInSeconds`. Frontend dùng reset token để mở màn hình nhập mật khẩu mới.

```http
POST /api/auth/forgot-password/reset
Content-Type: application/json
```

```json
{
  "resetToken": "token-returned-by-verify-api",
  "newPassword": "NewPassword2!",
  "confirmPassword": "NewPassword2!"
}
```

OTP hết hạn theo `OTP_EXPIRATION_MINUTES`, chỉ dùng được một lần, áp dụng cooldown và giới hạn số lần nhập. Reset thành công sẽ vô hiệu access token và refresh token hiện tại, xóa trạng thái lockout và yêu cầu đăng nhập lại.

### 6.6 Lấy account hiện tại

```http
GET /api/auth/me
Authorization: Bearer <accessToken>
```

Response `200`: `data` là object `UserResponse` như `data.user` trong response login.

---

## 7. API quản lý account — Admin

Tất cả API trong mục này cần:

```http
Authorization: Bearer <accessToken-cua-admin>
```

### 7.1 Tạo account

```http
POST /api/admin/users
```

Ví dụ tạo Doctor:

```json
{
  "password": "Doctor123!",
  "role": "DOCTOR",
  "fullName": "Dr Nguyen",
  "phoneNumber": "0912345678",
  "certificate": "Medical practice certificate"
}
```

Ví dụ tạo Patient:

```json
{
  "password": "Patient123!",
  "role": "PATIENT",
  "patientId": "PAT-0001",
  "fullName": "Nguyen Van A",
  "gender": "MALE",
  "dateOfBirth": "1995-01-01",
  "phoneNumber": "0912345678"
}
```

Response `201`: `data` là `UserResponse`; backend không trả `password` hoặc `passwordHash`.

Quy tắc quan trọng:

- `phoneNumber` là bắt buộc và được lưu theo chuẩn quốc tế `+84xxxxxxxxx`. Cùng một số có thể dùng cho các role khác nhau, nhưng cặp `phoneNumber + role` phải unique.
- Nhập `0912345678` được chuyển thành `+84912345678`; nhập `+84912345678` giữ nguyên.
- `PATIENT`: bắt buộc có `patientId`, `fullName`, `gender`, `dateOfBirth`, `phoneNumber`.
- `patientId` là unique.
- Ngày sinh, nếu được gửi, không được ở tương lai.
- Account mới mặc định có `status: "ACTIVE"`.

Các field profile có thể gửi khi tạo hoặc cập nhật account: `fullName`, `gender`, `dateOfBirth`, `address`, `citizenIdentificationCode`, `healthInsuranceCode`, `certificate`. Chỉ `phoneNumber`, `password` và `role` bắt buộc khi tạo mọi account; riêng `PATIENT` có thêm các field bắt buộc nêu trên.

> **Chuyển từ phiên bản username cũ:** khi backend khởi động, nó tự gỡ index MongoDB `username_1` cũ để account mới không còn bị ràng buộc bởi username. Dữ liệu account cũ không có `phoneNumber` vẫn tồn tại, nhưng không thể đăng nhập cho đến khi được cập nhật một số điện thoại hợp lệ. Tạo bootstrap Admin bằng `BOOTSTRAP_ADMIN_PHONE_NUMBER` nếu cần một Admin mới.

### 7.2 Lấy danh sách account

```http
GET /api/admin/users
```

Response `200`: `data` là mảng `UserResponse`, sắp xếp theo `createdAt` giảm dần.

### 7.3 Lấy chi tiết account

```http
GET /api/admin/users/{userId}
```

Ví dụ:

```text
GET /api/admin/users/687f00000000000000000001
```

Response `200`: `data` là một `UserResponse`.

### 7.4 Cập nhật account

```http
PATCH /api/admin/users/{userId}
```

Chỉ gửi field cần đổi. Ví dụ khóa account:

```json
{
  "status": "DISABLED"
}
```

Ví dụ cập nhật tên và role:

```json
{
  "fullName": "Dr Nguyen Van B",
  "role": "DOCTOR"
}
```

Các giá trị hợp lệ:

```text
role: ADMIN | DOCTOR | STAFF | RESEARCHER | PATIENT
status: ACTIVE | DISABLED
```

Nếu đổi role sang `PATIENT`, phải đồng thời đảm bảo đủ toàn bộ field bắt buộc của Patient. Response `200` trả object `UserResponse` sau cập nhật.

### 7.5 Khóa account

```http
DELETE /api/admin/users/{userId}
```

Response `200` chỉ đặt `status` thành `DISABLED`; không hard-delete document. Admin không thể khóa chính account của mình.

---

## 8. UserResponse

`UserResponse` xuất hiện trong login, `/api/auth/me` và API Admin:

| Field | Kiểu | Ghi chú |
| --- | --- | --- |
| `id` | string | MongoDB document ID; dùng cho `/api/admin/users/{userId}`. |
| `role` | enum | Một trong 5 role. |
| `status` | enum | `ACTIVE` hoặc `DISABLED`. |
| `patientId` | string/null | Có với Patient. |
| `fullName` | string/null | Tên hiển thị. |
| `gender` | string/null | Có với Patient. |
| `dateOfBirth` | `YYYY-MM-DD`/null | Có với Patient. |
| `phoneNumber` | string | Bắt buộc, unique; lưu chuẩn `+84xxxxxxxxx`. |
| `address` | string/null | Địa chỉ. |
| `citizenIdentificationCode` | string/null | Mã định danh công dân. |
| `healthInsuranceCode` | string/null | Mã bảo hiểm y tế. |
| `certificate` | string/null | Chứng chỉ chuyên môn. |
| `createdAt` | ISO-8601 UTC | Thời điểm tạo. |
| `updatedAt` | ISO-8601 UTC | Thời điểm cập nhật cuối. |
| `lastLoginAt` | ISO-8601 UTC/null | Lần đăng nhập thành công gần nhất. |

## 9. Xử lý lỗi ở frontend

| HTTP | `errorCode` thường gặp | Hành động frontend |
| --- | --- | --- |
| `400` | `VALIDATION_ERROR`, `BAD_REQUEST` | Hiển thị `message` tại form. |
| `401` | `UNAUTHORIZED` | Nếu refresh token còn, thử refresh một lần; nếu thất bại, xóa session và điều hướng login. |
| `403` | `FORBIDDEN` | Hiển thị không có quyền; không retry. |
| `404` | `NOT_FOUND` | Thông báo dữ liệu không tồn tại hoặc đã bị xóa. |
| `409` | `CONFLICT` | Báo phone number hoặc patient ID đã tồn tại. |
| `500` | `INTERNAL_SERVER_ERROR` | Thông báo lỗi hệ thống và ghi log client an toàn. |

Không hiển thị access token, refresh token hay password trong UI log, analytics hoặc crash report.

## 10. Chạy backend local cho Frontend

### 10.1 Điều kiện cần

- JDK 17 đã được cài.
- Có quyền kết nối MongoDB Atlas của nhóm.
- Public IP của máy đã được thêm vào Atlas **Network Access**.
- Database User của Atlas có quyền `readWrite` với database `med_super_app`.

> Không đưa MongoDB connection string có password thật vào README, source code, Postman collection công khai hoặc Git. Nhận `MONGODB_URI` của nhóm qua kênh quản lý secret riêng.

### 10.2 Tạo JWT secret cho local development

Mở PowerShell và chạy lệnh sau để tạo secret ngẫu nhiên 48 byte:

```powershell
$bytes = New-Object byte[] 48
[System.Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
[Convert]::ToBase64String($bytes)
```

Copy chuỗi kết quả vào `JWT_SECRET`. Secret phải có ít nhất 32 ký tự. Mỗi developer có thể dùng JWT secret local riêng khi tự chạy backend; production hoặc nhiều backend instance chạy chung phải dùng cùng một secret được quản lý an toàn.

### 10.3 Cấu hình IntelliJ

1. Mở **Run → Edit Configurations**.
2. Chọn cấu hình `BackendApplication`.
3. Mở **Environment variables** và thêm các biến sau.
4. Bấm **Apply → Run**.

```text
MONGODB_URI=mongodb+srv://<database-user>:<url-encoded-password>@<cluster-host>/med_super_app?retryWrites=true&w=majority
JWT_SECRET=<chuoi-duoc-tao-o-buoc-10.2>
BOOTSTRAP_ADMIN_PHONE_NUMBER=0912345678
BOOTSTRAP_ADMIN_PASSWORD=<mat-khau-admin-bootstrap>
BOOTSTRAP_ADMIN_FULL_NAME=System Administrator
```

Nếu password của Database User có ký tự như `@`, `:`, `/`, `?`, `#` hoặc `&`, phải URL-encode password trước khi đặt vào `MONGODB_URI`.

### 10.4 Bootstrap Admin có bắt buộc không?

`BOOTSTRAP_ADMIN_PHONE_NUMBER` và `BOOTSTRAP_ADMIN_PASSWORD` **không bắt buộc để backend khởi động**, nhưng cần thiết nếu database chưa có một Admin mà nhóm có thể đăng nhập.

- Backend tạo Admin bootstrap khi phone number cấu hình chưa tồn tại trong collection `users`.
- Nếu phone number đã tồn tại, backend giữ nguyên account/password cũ và ghi log `Bootstrap administrator account already exists; creation skipped.`
- Nếu không cấu hình hai biến này, backend vẫn chạy nhưng không tự tạo Admin; frontend sẽ không thể gọi API `/api/admin/users` nếu chưa có account `ADMIN` khác.
- Để tạo một Admin test mới mà không đụng vào Admin cũ, đặt một số điện thoại mới, ví dụ `0987654321`, rồi restart backend.

Password do Spring Security tự sinh trong console **không dùng** để gọi API login của project. Sau khi frontend test xong, không dùng password bootstrap mẫu cho production.

### 10.5 Dấu hiệu chạy thành công

Khi kết nối đúng, console có các dấu hiệu:

```text
Tomcat started on port 8080 (http)
Started BackendApplication
```

MongoDB driver phải hiển thị Atlas host dạng `*.mongodb.net`, không phải chỉ `localhost:27017`. Sau đó frontend có thể gọi `POST http://localhost:8080/api/auth/login` theo mục 6.1.

## 11. Swagger / OpenAPI

Sau khi backend chạy, mở Swagger UI tại:

```text
http://localhost:8080/swagger-ui/index.html
```

OpenAPI JSON dùng để import vào Postman hoặc sinh API client:

```text
http://localhost:8080/v3/api-docs
```

Để thử endpoint cần đăng nhập trên Swagger:

1. Gọi `POST /api/auth/login` với `phoneNumber` và `password` để nhận `accessToken`.
2. Nhấn **Authorize** ở góc trên bên phải.
3. Dán **chỉ access token** (Swagger tự thêm `Bearer `), rồi nhấn **Authorize**.
4. Gọi các endpoint như `GET /api/auth/me` hoặc `/api/admin/users`.

`/api/admin/users/**` yêu cầu account có role `ADMIN`. Không dán `refreshToken` vào Swagger Authorize.

## 12. Mã hóa dữ liệu Patient

Backend mã hóa AES-256-GCM các PII của `PATIENT`: phone number, Patient ID, họ tên, giới tính, ngày sinh, địa chỉ, CCCD và mã bảo hiểm. MongoDB chỉ lưu ciphertext cho các field này. Password vẫn dùng BCrypt và không thể giải mã.

Backend lưu `phoneLookup` cho mọi account và `patientIdLookup` cho Patient bằng HMAC-SHA-256. Các lookup này dùng để login, kiểm tra unique và không chứa plaintext.

Tạo hai key Base64 riêng, mỗi key 32 byte, bằng PowerShell:

```powershell
$bytes = New-Object byte[] 32
[System.Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
[Convert]::ToBase64String($bytes)
```

Chạy lệnh hai lần, sau đó cấu hình trong IntelliJ/hosting environment:

```text
PATIENT_DATA_AES_KEY=<base64-key-thu-nhat>
PATIENT_LOOKUP_HMAC_KEY=<base64-key-thu-hai>
```

Hai key phải giống nhau ở mọi backend instance dùng chung MongoDB, nhưng không được đưa vào Git, README công khai, Postman collection hoặc frontend/mobile.

### Migration dữ liệu cũ

1. Backup Atlas trước.
2. Chạy backend một lần với `PATIENT_DATA_MIGRATE_LEGACY_ON_STARTUP=true` để backfill lookup và mã hóa các Patient legacy.
3. Xác nhận login, API `/api/auth/me` và Admin API hoạt động.
4. Đặt lại `PATIENT_DATA_MIGRATE_LEGACY_ON_STARTUP=false` rồi restart backend.

Không bật migration nếu database có Patient thiếu các field bắt buộc hoặc phone/Patient ID bị trùng sau chuẩn hóa; hãy sửa dữ liệu đó trước.

## 13. Chạy Unit Test và Integration Test hoàn chỉnh

### 13.1 Phân biệt môi trường test

- Unit test dùng Mockito, không cần Docker, MongoDB Atlas, Firebase hoặc biến môi trường thật.
- Integration test khởi động Spring Boot, MockMvc, Spring Security và MongoDB 7 bằng Testcontainers.
- Integration test dùng fake `FcmGatewayService`; không gửi FCM hoặc SMS thật. OTP vẫn được in ra console trong môi trường hiện tại và test cũng đọc OTP từ fake FCM payload.
- Không cấu hình integration test chạy trực tiếp trên MongoDB Atlas của nhóm vì `MongoIntegrationTestBase` xóa dữ liệu test trước mỗi test case.

### 13.2 Cài Docker cho Integration Test

1. Cài Docker Desktop cho Windows và bật WSL 2 backend.
2. Mở Docker Desktop, chờ trạng thái `Engine running`.
3. Mở PowerShell và kiểm tra:

```powershell
docker version
docker run --rm hello-world
```

Nếu `docker` không được nhận diện, restart PowerShell/IntelliJ sau khi cài Docker Desktop.

### 13.3 Chạy bằng PowerShell

Chạy riêng unit test:

```powershell
cd D:\doAn2026\Med_superApp_backend
.\gradlew.bat test --tests "com.yourproject.backend.services.*" --tests "com.yourproject.backend.utils.*" --tests "com.yourproject.backend.config.*"
```

Chạy toàn bộ integration test:

```powershell
.\gradlew.bat test --tests "com.yourproject.backend.integration.*"
```

Chạy sạch toàn bộ project:

```powershell
.\gradlew.bat clean test
```

Lần chạy integration đầu tiên có thể chậm vì Testcontainers phải tải image `mongo:7.0`.

### 13.4 Chạy bằng IntelliJ IDEA

1. Mở Docker Desktop trước.
2. Trong IntelliJ, mở `src/test/java/com/yourproject/backend/integration`.
3. Nhấn chuột phải package `integration` và chọn **Run Tests in 'integration'**.
4. Để chạy toàn bộ, nhấn chuột phải `src/test/java` và chọn **Run All Tests**.
5. Integration test không cần nhập `MONGODB_URI`, Firebase key, JWT secret hoặc AES/HMAC key thật; test base tự tạo Mongo container và test key cô lập.

### 13.5 Cách xác nhận test thực sự chạy

Mở báo cáo sau khi chạy:

```text
build/reports/tests/test/index.html
```

Kết quả hoàn chỉnh phải thỏa mãn:

- `Failures = 0` và `Errors = 0`.
- Các class trong package `integration` không có trạng thái `Skipped`.
- Hiện tại project có 71 test: 46 unit test và 25 integration test.
- Nếu 25 integration test đều bị Skip nhưng Gradle vẫn báo `BUILD SUCCESSFUL`, Docker chưa được cài hoặc Docker Engine chưa chạy.

Các integration test hiện bao phủ: password login, lockout sau 5 lần sai, OTP bệnh nhân, trusted device, refresh-token rotation, logout, đổi mật khẩu, chặn Patient đổi mật khẩu, JWT không hợp lệ, account inactive và phân quyền Admin.

## 14. Doctor Work Schedule và Patient Appointment

Backend tự tạo 16 work slot, mỗi slot 30 phút:

- `Slot1` đến `Slot8`: 08:00-12:00.
- `Slot9` đến `Slot16`: 13:00-17:00.
- Doctor chọn `MORNING`, `AFTERNOON` hoặc `FULL_DAY`; một request tạo lần lượt 8, 8 hoặc 16 document `doctor_work_slots`.

### 14.1 API Doctor

| Method | Endpoint | Mô tả |
|---|---|---|
| `GET` | `/api/doctor/work-schedules/options` | Lấy 16 slot chuẩn và danh sách phòng đang active. |
| `GET` | `/api/doctor/work-schedules?from=YYYY-MM-DD&to=YYYY-MM-DD` | Xem lịch Doctor hiện tại. |
| `POST` | `/api/doctor/work-schedules` | Đăng ký ca làm việc và gửi Staff duyệt. |
| `DELETE` | `/api/doctor/work-schedules/{submissionId}` | Hủy submission còn `PENDING_APPROVAL`. |

Ví dụ đăng ký ca sáng:

```json
{
  "workDate": "2026-08-10",
  "session": "MORNING",
  "roomId": "<clinicRoomId>",
  "note": "Morning shift"
}
```

Admin có thể đặt password mới cho account không phải Patient-only:

```json
{
  "password": "AdminReset1!"
}
```

Password mới phải đạt password policy. Backend chỉ lưu BCrypt hash và sẽ thu hồi access token, refresh token hiện tại của user để buộc đăng nhập lại.

### 14.2 API Staff

| Method | Endpoint | Mô tả |
|---|---|---|
| `POST` | `/api/staff/scheduling/clinic-rooms` | Tạo phòng; `code` là duy nhất. |
| `GET` | `/api/staff/scheduling/clinic-rooms` | Lấy phòng đang active. |
| `GET` | `/api/staff/scheduling/work-schedules/pending` | Lấy submission Doctor đang chờ duyệt. |
| `GET` | `/api/staff/scheduling/work-schedules?status=APPROVED` | Lấy lịch làm việc theo trạng thái; bỏ `status` để lấy tất cả. |
| `PATCH` | `/api/staff/scheduling/work-schedules/{submissionId}/decision` | `APPROVE` hoặc `REJECT` lịch Doctor. |
| `GET` | `/api/staff/scheduling/appointments/pending` | Lấy Appointment Patient đang chờ duyệt lần hai. |
| `GET` | `/api/staff/scheduling/appointments?status=CONFIRMED` | Lấy appointment theo trạng thái; bỏ `status` để lấy tất cả. |
| `PATCH` | `/api/staff/scheduling/appointments/{appointmentId}/cancel` | Hủy appointment đã xác nhận và giải phóng slot; yêu cầu `cancellationReason`. |
| `PATCH` | `/api/patient/appointments/{appointmentId}/cancel` | Patient hủy appointment của mình khi đang chờ duyệt hoặc đã xác nhận. |
| `PATCH` | `/api/staff/scheduling/appointments/{appointmentId}/decision` | `APPROVE` hoặc `REJECT` Appointment. |

Khi reject phải gửi `rejectionReason`:

```json
{
  "decision": "REJECT",
  "rejectionReason": "Clinic room is unavailable"
}
```

### 14.3 API Patient

| Method | Endpoint | Mô tả |
|---|---|---|
| `GET` | `/api/patient/appointments/available-slots?date=YYYY-MM-DD&doctorId=...` | Chỉ trả slot Doctor đã được Staff duyệt và chưa được Patient khác giữ/đặt. |
| `POST` | `/api/patient/appointments` | Giữ slot và tạo Appointment `PENDING_STAFF_CONFIRMATION`. |
| `GET` | `/api/patient/appointments` | Xem Appointment của Patient đang đăng nhập. |

```json
{
  "doctorWorkSlotId": "<doctorWorkSlotId>",
  "note": "First visit"
}
```

Luồng trạng thái hiện tại không có thanh toán:

```text
Doctor submit -> PENDING_APPROVAL
Staff approve -> APPROVED + AVAILABLE
Patient book -> PENDING_STAFF_CONFIRMATION + PENDING_CONFIRMATION
Staff approve Appointment -> CONFIRMED + BOOKED
Staff reject Appointment -> REJECTED + AVAILABLE
```

Patient chỉ được đặt lịch trước tối thiểu 12 giờ, tối đa 30 ngày và chỉ có một Appointment active trong một ngày.
