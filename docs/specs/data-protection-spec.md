# Data Protection Specification

## Password

- Password được hash một chiều bằng BCrypt cost 12 theo SecurityConfig hiện tại.
- Không lưu hoặc trả password plaintext.
- Password reset/change thu hồi token hiện tại.

## Patient data

- Field nhạy cảm của Patient dùng AES-256-GCM để mã hóa hai chiều.
- Mỗi ciphertext dùng nonce ngẫu nhiên và authentication tag để phát hiện sửa đổi.
- Các field cần exact lookup, đặc biệt phone, dùng HMAC-SHA-256 qua `PATIENT_LOOKUP_HMAC_KEY`.
- `phoneLookup` không thể giải mã; backend normalize phone rồi tính HMAC để query.

## Tokens and OTP

- Access token/refresh token chỉ lưu SHA-256 hash trong User.
- OTP lưu HMAC lookup hash, không lưu code plaintext.
- Password reset token lưu SHA-256 hash, có expiration và chỉ dùng một lần.

## Required secrets

| Secret | Công dụng |
| --- | --- |
| `JWT_SECRET` | Ký/xác minh JWT |
| `PATIENT_DATA_AES_KEY` | Mã hóa/giải mã patient data |
| `PATIENT_LOOKUP_HMAC_KEY` | Exact lookup dữ liệu đã bảo vệ |
| `FIREBASE_SERVICE_ACCOUNT_PATH` | Firebase Admin credential |
| `SMS_GATEWAY_REGISTRATION_KEY` | Xác thực Android Gateway |

Secret không được commit. AES/HMAC key phải giống nhau trên mọi backend instance; mất hoặc đổi AES key làm dữ liệu cũ không giải mã được.
