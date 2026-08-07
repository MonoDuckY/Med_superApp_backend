# Authentication Specification

## Current account identity

- Username đăng nhập là số điện thoại Việt Nam đã normalize thành `+84xxxxxxxxx`.
- Mỗi User tham chiếu đúng một Role qua `roleId`.
- Một số điện thoại có thể thuộc nhiều account khác role; cặp `(phoneLookup, roleId)` phải unique.
- Chỉ account `ACTIVE` được xác thực.
- `PATIENT`-only đăng nhập bằng OTP và không dùng password.

## Public endpoints

| Method | Endpoint | Chức năng |
| --- | --- | --- |
| `POST` | `/api/auth/login` | Password login cho account không phải Patient-only |
| `POST` | `/api/auth/patient-otp/request` | Gửi OTP hoặc login ngay nếu device trusted |
| `POST` | `/api/auth/patient-otp/verify` | Xác minh OTP và cấp token |
| `POST` | `/api/auth/refresh` | Đổi refresh token hợp lệ lấy token pair mới |
| `POST` | `/api/auth/forgot-password/request` | Gửi OTP reset password với response chống account enumeration |
| `POST` | `/api/auth/forgot-password/verify` | Xác minh OTP và cấp reset token một lần |
| `POST` | `/api/auth/forgot-password/reset` | Đặt password mới bằng reset token |

## Authenticated endpoints

| Method | Endpoint | Chức năng |
| --- | --- | --- |
| `POST` | `/api/auth/logout` | Thu hồi token hash của account |
| `POST` | `/api/auth/change-password` | Đổi password và thu hồi phiên hiện tại |
| `GET` | `/api/auth/me` | Lấy profile account đang đăng nhập |

## Password login

Request gồm `phoneNumber`, `role`, `password`, tùy chọn `deviceId`. Backend dùng `(phoneLookup, roleId)` để chọn đúng account. Password sai tăng `failedLoginAttempts`; đạt ngưỡng cấu hình thì đặt `lockedUntil`. Login thành công reset lockout, cập nhật `lastLoginAt`, phát access/refresh token và lưu hash token trong User.

## Patient OTP

- OTP gồm sáu chữ số, lưu dưới dạng HMAC lookup hash trong `patient_otps`.
- OTP có expiration, resend cooldown và maximum attempts.
- OTP mới consume OTP cũ còn hoạt động.
- OTP đúng consume record, trust `deviceId` và cấp token.
- Nếu request đến từ `deviceId` trùng device trusted của User, backend có thể cấp token mà không gửi OTP.

## JWT and sessions

- Access token là JWT HS256, subject là User ID và claim `role` chứa role name hiện tại.
- Access token mặc định sống 15 phút.
- Refresh token là chuỗi random, mặc định sống 7 ngày.
- Database chỉ lưu SHA-256 hash của access/refresh token.
- Mỗi User chỉ giữ một access token hash, một refresh token hash và một device ID tại một thời điểm.
- Password change/reset và account inactive thu hồi token hiện tại.

## Forgot password

Luồng: request OTP → verify OTP → nhận reset token → submit password mới. Reset token random 32 byte, database chỉ lưu SHA-256 hash, mặc định sống 10 phút và dùng một lần. Account Patient-only không dùng chức năng này.
