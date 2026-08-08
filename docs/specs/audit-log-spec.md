# Audit Log Specification

## Purpose

Backend ghi audit cho mọi request `POST`, `PUT`, `PATCH` và `DELETE` để nhận biết ai thực hiện thao tác và instance backend nào đã xử lý request.

## Storage

- Audit được lưu trong collection `audit_logs` của MongoDB database riêng `audit_logs`, không nằm trong `med_super_app`.
- Database có thể đổi bằng `AUDIT_MONGODB_DATABASE`; mặc định là `audit_logs`.
- Lỗi ghi audit chỉ được cảnh báo trong server log và không làm request nghiệp vụ thất bại.
- MongoDB user trong `MONGODB_URI` phải có role `readWrite` trên database `audit_logs`; nếu không, API vẫn chạy nhưng audit không được lưu.

## Automatic backend identity

- Lần chạy đầu, backend tạo UUID và lưu tại `.backend-instance-id`.
- File được bỏ qua bởi Git; mỗi máy tự có UUID mà thành viên không phải cấu hình IntelliJ.
- Backend dùng lại UUID sau restart và ghi thêm hostname cùng IPv4 local.
- Xóa file sẽ làm máy được nhận diện như một backend instance mới.
- Có thể đổi đường dẫn bằng `AUDIT_INSTANCE_ID_FILE`, đặc biệt khi chạy container cần persistent volume.

## Stored fields

Audit document gồm timestamp, actor user/role, action, HTTP method/path/query, response status, duration, client IP, backend instance ID, backend hostname và backend IP.

Audit không lưu request body, password, OTP, token, key mã hóa hoặc nội dung hồ sơ bệnh án.

## IP behavior

- `clientIp` ưu tiên IP đầu tiên trong `X-Forwarded-For`, nếu không có sẽ dùng `remoteAddr`.
- `backendIp` là IPv4 local không phải loopback được backend tự phát hiện.
- UUID là định danh chính vì IP và hostname có thể thay đổi hoặc trùng nhau.
