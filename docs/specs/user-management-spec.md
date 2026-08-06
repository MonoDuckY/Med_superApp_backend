# User Management Specification

## Authorization

Toàn bộ `/api/admin/users/**` yêu cầu `ADMIN`.

## Endpoints

| Method | Endpoint | Chức năng |
| --- | --- | --- |
| `POST` | `/api/admin/users` | Tạo account active |
| `GET` | `/api/admin/users` | Lấy toàn bộ account |
| `GET` | `/api/admin/users/{userId}` | Lấy account theo ID |
| `PATCH` | `/api/admin/users/{userId}` | Cập nhật profile, role/status và password tùy chọn |
| `PATCH` | `/api/admin/users/{userId}/status` | Toggle `ACTIVE ↔ INACTIVE` |

## Current role rules

- Request hiện dùng `roles` và yêu cầu ít nhất một role.
- Patient-only không cần password và xác thực bằng SMS OTP.
- Các account khác phải có password hợp lệ.
- Doctor phải có certificate.
- Admin không được toggle trạng thái chính account đang đăng nhập.

## Profile validation

- `phoneNumber` bắt buộc, normalize về `+84` và hiện phải unique toàn hệ thống.
- `fullName` bắt buộc.
- `dateOfBirth` không được ở tương lai.
- Patient yêu cầu `fullName`, `gender`, `dateOfBirth`, `phoneNumber`.
- Password dài 8–50, có lowercase, uppercase và ít nhất một digit hoặc special character.

## Status behavior

Khi chuyển sang `INACTIVE`, access/refresh token hash bị xóa. Account inactive không login, refresh hoặc gọi endpoint bảo vệ được.

## Planned ERD migration

ERD mới yêu cầu một role/User và cho phone trùng giữa role. Khi triển khai, request/response phải đổi `roles` thành `role`, repository lookup phải dùng `(phoneLookup, role)` và MongoDB phải dùng compound unique index tương ứng.
