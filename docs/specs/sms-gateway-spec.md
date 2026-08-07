# SMS Gateway Specification

## Architecture

Spring Boot tạo SMS job và gửi FCM data message đến Android Gateway. Android nhận `phoneNumber`, `content`, `jobId`, dùng SIM gửi SMS, sau đó báo kết quả về backend.

## Gateway authentication

Gateway API không dùng JWT; bắt buộc header:

```http
X-Gateway-Registration-Key: <SMS_GATEWAY_REGISTRATION_KEY>
```

## Endpoints

| Method | Endpoint | Chức năng |
| --- | --- | --- |
| `POST` | `/api/sms-gateway/devices/register` | Đăng ký/cập nhật Android FCM token |
| `GET` | `/api/sms-gateway/jobs/{jobId}` | Lấy SMS payload |
| `POST` | `/api/sms-gateway/jobs/{jobId}/complete` | Báo `sent` và failure reason |
| `POST` | `/api/admin/sms-gateway/send-test` | Admin gửi lệnh SMS test qua FCM |

## Rules

- Không ghi OTP plaintext vào MongoDB; SMS job chỉ tồn tại để giao payload và theo dõi gửi.
- OTP console output hiện được giữ cho môi trường test.
- Gateway device và job cần được dọn giữa integration tests.
- Firebase service-account JSON và registration key không được commit.
