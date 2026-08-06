# MongoDB Database Specification

## Current collections

| Collection | Mục đích/liên kết logic |
| --- | --- |
| `users` | Account, profile, role(s), token hash, trusted device và patient encrypted fields |
| `patient_otps` | Patient login OTP và password-reset OTP/token theo `purpose` |
| `sms_gateway_devices` | Android gateway registration/FCM token |
| `sms_gateway_jobs` | Trạng thái lệnh gửi SMS |
| `work_slots` | Danh mục slot bắt đầu/kết thúc |
| `clinic_rooms` | Danh mục phòng khám |
| `doctor_work_slots` | Lịch làm việc theo doctor/slot/room/date và submission |
| `appointments` | Patient booking tham chiếu DoctorWorkSlot |
| `vital_signs` | Chỉ số lần khám theo appointmentId |
| `prescriptions` | Đơn thuốc theo appointmentId |
| `medicine_schedules` | Lịch thuốc theo prescriptionId |

## MongoDB relationships

Quan hệ là manual reference bằng string/ObjectId; MongoDB không áp dụng foreign key. Service phải kiểm tra resource tồn tại, quyền sở hữu và status trước khi ghi.

## Important indexes

- User phone/phoneLookup hiện unique, sparse.
- VitalSign có index `appointmentId`.
- Prescription có index `appointmentId`.
- PatientOtp có index user, phone lookup, purpose và TTL expiration.
- MedicineSchedule chống trùng prescription/medicine/dosage/scheduledAt.
- Appointment và DoctorWorkSlot dùng optimistic locking/version ở các luồng cạnh tranh.

## Planned ERD changes

- Tạo `roles` và chuyển User về một role.
- Cho phone trùng khác role bằng unique compound `(phoneLookup, role)`.
- Thay `vital_signs` bằng `medical_records` unique theo appointmentId.
- Chuyển diagnosis từ Appointment sang MedicalRecord.
- Chuyển Prescription từ `appointmentId` sang `medicalRecordId`.

Các mục planned không được xem là current schema cho tới khi migration và test được merge.
