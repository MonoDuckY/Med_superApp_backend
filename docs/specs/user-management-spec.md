# User Management Specification

## Staff Patient account creation

- `POST /api/staff/patients` requires `STAFF` and accepts a JSON `StaffCreatePatientRequest`.
- The request does not contain `role` or `password`; backend always assigns `PATIENT` and stores no password.
- The new account is active and stores the authenticated Staff user ID in `createdBy`.
- The endpoint does not accept a certificate or multipart request.

## Authorization

Toàn bộ `/api/admin/users/**` yêu cầu `ADMIN`.

## Endpoints

| Method | Endpoint | Chức năng |
| --- | --- | --- |
| `POST` | `/api/admin/users` | Tạo account active |
| `GET` | `/api/admin/users` | Lấy account; hỗ trợ `phoneNumber`, `citizenIdentificationCode`, `role` |
| `GET` | `/api/admin/users/{userId}` | Lấy account theo ID |
| `PATCH` | `/api/admin/users/{userId}` | Cập nhật profile, role/status và password tùy chọn |
| `PATCH` | `/api/admin/users/{userId}/status` | Toggle `ACTIVE ↔ INACTIVE` |

## Role rules

- Request dùng `role` và yêu cầu đúng một role.
- User lưu `roleId` theo catalog cố định (`1=ADMIN`, `2=DOCTOR`, `3=STAFF`, `4=RESEARCHER`, `5=PATIENT`).
- Patient-only không cần password và xác thực bằng SMS OTP.
- Các account khác phải có password hợp lệ.
- Doctor certificate là ảnh private trong Amazon S3. MongoDB chỉ lưu `certificateObjectKey`.
- Khi Admin tạo Doctor qua `POST /api/admin/users`, part `certificate` là bắt buộc.
- Khi Admin patch Doctor, certificate mới là tùy chọn nếu Doctor đã có ảnh; gửi ảnh sẽ thay thế ảnh cũ.
- Role khác Doctor không được gửi certificate. Nếu đổi Doctor sang role khác, backend xóa certificate cũ.
- Chỉ response POST/PATCH user dành cho Admin mới trả presigned `certificateUrl`. API Patient/Staff không trả object key hoặc certificate URL.
- Certificate chỉ nhận JPEG, PNG hoặc WEBP, tối đa 5 MB.
- Presigned URL mặc định có hiệu lực 10 phút và không được lưu trong MongoDB.

## User mutation endpoints

| Method | Endpoint | Chức năng |
| --- | --- | --- |
| `POST` | `/api/admin/users` | Multipart part `user` chứa JSON create request; part `certificate` bắt buộc chỉ khi role là Doctor |
| `PATCH` | `/api/admin/users/{userId}` | Multipart part `user` chứa JSON update request; part `certificate` tùy chọn để thay ảnh Doctor |
- Admin không được toggle trạng thái chính account đang đăng nhập.

## Profile validation

- `phoneNumber` bắt buộc, normalize về `+84`; cặp `(phoneLookup, roleId)` phải unique.
- `fullName` bắt buộc.
- `dateOfBirth` không được ở tương lai.
- Patient yêu cầu `fullName`, `gender`, `dateOfBirth`, `phoneNumber`.
- Password dài 8–50, có lowercase, uppercase và ít nhất một digit hoặc special character.

Phone được tìm bằng `phoneLookup`; CCCD được tìm bằng `citizenIdentificationLookup`. Cả hai là HMAC lookup nên Patient vẫn có thể được query dù dữ liệu gốc lưu AES-GCM ciphertext.

## Status behavior

Khi chuyển sang `INACTIVE`, access/refresh token hash bị xóa. Account inactive không login, refresh hoặc gọi endpoint bảo vệ được.
