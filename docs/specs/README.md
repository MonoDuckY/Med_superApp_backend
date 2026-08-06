# Backend Specifications

Thư mục này là nguồn đặc tả kỹ thuật cho hành vi backend đang được triển khai. Khi business rule, API, database schema, quyền truy cập hoặc status flow thay đổi, spec liên quan và `CHANGELOG.md` phải được cập nhật trong cùng pull request.

## Specification index

| File | Phạm vi |
| --- | --- |
| `api-conventions-spec.md` | JSON wrapper, HTTP status, authentication và date-time |
| `authentication-spec.md` | Password login, Patient OTP, trusted device, JWT, refresh, logout, forgot password |
| `user-management-spec.md` | Admin quản lý account và hồ sơ User |
| `scheduling-spec.md` | Room, work slot, Doctor schedule và Staff approval |
| `appointment-spec.md` | Patient/Staff booking, confirm, reject, cancel và reschedule |
| `clinical-medication-spec.md` | Doctor examination, vital signs, diagnosis, prescription và medicine schedule |
| `sms-gateway-spec.md` | Firebase và Android SMS Gateway |
| `data-protection-spec.md` | BCrypt, AES-256-GCM, HMAC lookup và token hashing |
| `database-spec.md` | Các MongoDB collection và liên kết logic hiện tại |

## Update policy

Một thay đổi phải cập nhật spec nếu làm thay đổi ít nhất một mục sau:

- Endpoint, request, response hoặc HTTP status.
- Role/permission hoặc authentication behavior.
- Business rule, validation hoặc state transition.
- MongoDB collection, field, index hoặc migration.
- Encryption, token, OTP hoặc secret handling.

Quy trình hoàn thành task:

1. Sửa code và migration.
2. Sửa unit/integration tests.
3. Cập nhật spec liên quan.
4. Cập nhật `CHANGELOG.md` trong mục `Unreleased`.
5. Kiểm tra Swagger tại `/v3/api-docs` và `/swagger-ui/index.html`.

## Current known divergence

ERD cập nhật ngày 2026-08-05 đề xuất `MedicalRecord`, collection `Role`, một role cho mỗi User và phone có thể trùng theo role. Những thay đổi này chưa được triển khai trong code tại thời điểm tạo bộ spec này. Spec hiện tại mô tả hành vi code đang chạy; khi migration được triển khai phải cập nhật lại các file liên quan.
