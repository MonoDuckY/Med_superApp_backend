# MongoDB Database Specification

## Current collections

| Collection | Mục đích/liên kết logic |
| --- | --- |
| `roles` | Danh mục role với ID cố định: `1=ADMIN`, `2=DOCTOR`, `3=STAFF`, `4=RESEARCHER`, `5=PATIENT`; `roleName` unique |
| `users` | Account, profile, một `roleId`, token hash, trusted device và patient encrypted fields |
| `patient_otps` | Patient login OTP và password-reset OTP/token theo `purpose` |
| `sms_gateway_devices` | Android gateway registration/FCM token |
| `sms_gateway_jobs` | Trạng thái lệnh gửi SMS |
| `work_slots` | Danh mục slot bắt đầu/kết thúc |
| `clinic_rooms` | Danh mục phòng khám |
| `doctor_work_slots` | Lịch làm việc theo doctor/slot/room/date và submission |
| `appointments` | Patient booking tham chiếu DoctorWorkSlot |
| `medical_records` | Hồ sơ lâm sàng và diagnosis, unique theo appointmentId |
| `prescriptions` | Đơn thuốc theo medicalRecordId |
| `medicine_schedules` | Lịch thuốc theo prescriptionId |

## MongoDB relationships

Quan hệ là manual reference bằng string/ObjectId; MongoDB không áp dụng foreign key. Service phải kiểm tra resource tồn tại, quyền sở hữu và status trước khi ghi.

## Important indexes

- User có unique compound index `(phoneLookup, roleId)`, cho phép cùng phone ở các role khác nhau.
- User có index `citizenIdentificationLookup` để query CCCD đã mã hóa.
- Role có unique index trên `roleName`.
- MedicalRecord có unique index `appointmentId`.
- Prescription có index `medicalRecordId`.
- PatientOtp có index user, phone lookup, purpose và TTL expiration.
- MedicineSchedule chống trùng prescription/medicine/dosage/scheduledAt.
- Appointment và DoctorWorkSlot dùng optimistic locking/version ở các luồng cạnh tranh.
