# API Conventions Specification

## Base behavior

- API dùng JSON và Spring MVC.
- Endpoint bảo vệ nhận `Authorization: Bearer <accessToken>`.
- Date-time dùng ISO-8601 UTC, ví dụ `2026-08-04T17:43:07.887Z`.
- Date không có thời gian dùng `yyyy-MM-dd`.

## Required response wrapper

Mọi controller trả về:

```json
{
  "success": true,
  "message": "Operation completed successfully.",
  "data": {},
  "errorCode": null
}
```

Khi lỗi:

```json
{
  "success": false,
  "message": "Validation message.",
  "data": null,
  "errorCode": "VALIDATION_ERROR"
}
```

## HTTP status conventions

| Status | Ý nghĩa |
| --- | --- |
| `200` | Đọc/cập nhật/xử lý thành công |
| `201` | Tạo resource thành công |
| `400` | JSON, validation hoặc state transition không hợp lệ |
| `401` | Chưa đăng nhập hoặc credential/token không hợp lệ |
| `403` | Đã đăng nhập nhưng sai quyền/sai chủ sở hữu |
| `404` | Resource không tồn tại |
| `409` | Unique conflict hoặc optimistic locking conflict |
| `500` | Lỗi ngoài dự kiến; chi tiết chỉ ghi vào server log |

## Authorization roles

Các role hiện có: `ADMIN`, `DOCTOR`, `STAFF`, `RESEARCHER`, `PATIENT`. Controller dùng Spring authority dạng `ROLE_<ROLE_NAME>`.
