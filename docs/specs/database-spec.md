# MongoDB Database Specification

## Current collections

Các collection dưới đây mặc định nằm trong database nghiệp vụ `med_super_app`, ngoại trừ `audit_logs.audit_logs`.

| Collection | Mục đích/liên kết logic |
| --- | --- |
| `roles` | Danh mục role với ID cố định: `1=ADMIN`, `2=DOCTOR`, `3=STAFF`, `4=RESEARCHER`, `5=PATIENT`; `roleName` unique |
| `users` | Account, profile, một `roleId`, token hash, trusted device, patient encrypted fields và `certificateObjectKey` của Doctor |
| `patient_otps` | Patient login OTP và password-reset OTP/token theo `purpose` |
| `sms_gateway_devices` | Android gateway registration/FCM token |
| `sms_gateway_jobs` | Trạng thái lệnh gửi SMS |
| `work_slots` | Danh mục slot bắt đầu/kết thúc |
| `clinic_rooms` | Danh mục phòng khám |
| `doctor_work_slots` | Lịch theo doctor/slot/date/submission; `roomId` null khi pending và được Staff gán khi approve |
| `appointments` | Patient booking tham chiếu DoctorWorkSlot |
| `medical_records` | Hồ sơ lâm sàng, diagnosis và danh sách S3 object key `medicalImages`, unique theo appointmentId |
| `prescriptions` | Đơn thuốc theo medicalRecordId |
| `medicine_schedules` | Lịch thuốc theo prescriptionId |
| `notifications` | Notification của Patient, nội dung, thời điểm và trạng thái read/unread |
| `meals` | Meal plan theo userId; prescriptionId nullable đối với plan Patient tự tạo |
| `dishes` | Món ăn thuộc Meal thông qua mealId |
| `workouts` | Workout plan theo userId; prescriptionId nullable đối với plan Patient tự tạo |
| `audit_logs.audit_logs` | Audit của request ghi; lưu tại database riêng `audit_logs` và nhận diện backend instance |

## MongoDB relationships

`medical_records.medicalImages` stores a list of private S3 object keys. The image bytes, public URLs and temporary presigned URLs are not stored in MongoDB.

Quan hệ là manual reference bằng string/ObjectId; MongoDB không áp dụng foreign key. Service phải kiểm tra resource tồn tại, quyền sở hữu và status trước khi ghi.

## Important indexes

- User có unique compound index `(phoneLookup, roleId)`, cho phép cùng phone ở các role khác nhau.
- User có index `citizenIdentificationLookup` để query CCCD đã mã hóa.
- `users.certificateObjectKey` chỉ lưu S3 object key; không lưu file, public URL hoặc presigned URL trong MongoDB.
- Role có unique index trên `roleName`.
- MedicalRecord có unique index `appointmentId`.
- Prescription có unique index `medicalRecordId`, bảo đảm một Appointment/MedicalRecord chỉ có một Prescription.
- PatientOtp có index user, phone lookup, purpose và TTL expiration.
- MedicineSchedule chống trùng prescription/medicine/dosage/scheduledAt.
- MedicineSchedule lưu `isNotified`; job chỉ tạo reminder khi field là false hoặc chưa tồn tại.
- Notification có index `(userId, notifyTime)` để lấy inbox mới nhất trước.
- Meal chống trùng `(userId, mealName, scheduledAt)`.
- Dish có index `mealId`; service xóa Dish trước khi thay thế Meal trong Prescription.
- Workout chống trùng `(userId, workoutName, scheduledAt)`.
- Appointment và DoctorWorkSlot dùng optimistic locking/version ở các luồng cạnh tranh.
