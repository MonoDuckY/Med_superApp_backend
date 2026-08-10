# User Management Specification

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
- Doctor certificate là ảnh private trong Amazon S3. MongoDB chỉ lưu `certificateObjectKey`; Doctor có thể được tạo trước và Admin upload certificate sau.
- Chỉ Admin được upload, lấy presigned URL hoặc xóa certificate của Doctor. API danh sách Doctor dành cho Patient/Staff không trả `hasCertificate`, object key hoặc certificate URL.
- Certificate chỉ nhận JPEG, PNG hoặc WEBP, tối đa 5 MB.
- Presigned URL mặc định có hiệu lực 10 phút và không được lưu trong MongoDB.

## Doctor certificate endpoints

| Method | Endpoint | Chức năng |
| --- | --- | --- |
| `POST` | `/api/admin/users/{doctorId}/certificate` | Upload/replace ảnh certificate bằng `multipart/form-data`, field `file` |
| `GET` | `/api/admin/users/{doctorId}/certificate` | Tạo presigned URL tạm thời để xem ảnh |
| `DELETE` | `/api/admin/users/{doctorId}/certificate` | Xóa ảnh khỏi S3 và xóa object key khỏi User |
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
