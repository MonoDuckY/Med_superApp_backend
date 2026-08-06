# Clinical Examination and Medication Specification

## Authorization

- Chỉ Doctor được xem và sửa examination của chính mình.
- Chỉ Patient sở hữu prescription được xem/sửa medicine schedule của mình.

## Doctor endpoints

| Method | Endpoint | Chức năng |
| --- | --- | --- |
| `GET` | `/api/doctor/appointments` | Lấy appointment của Doctor, tùy chọn status |
| `GET` | `/api/doctor/appointments/{id}` | Lấy examination detail |
| `PATCH` | `/api/doctor/appointments/{id}/start` | `CONFIRMED → IN_PROGRESS` |
| `PATCH` | `/api/doctor/appointments/{id}/clinical-information` | Cập nhật sức khỏe lâu dài trong User và vital signs theo appointment |
| `PATCH` | `/api/doctor/appointments/{id}/diagnosis` | Cập nhật diagnosis hiện đang lưu trong Appointment |
| `POST` | `/api/doctor/appointments/{id}/prescriptions` | Tạo prescription và medicine schedules |
| `PATCH` | `/api/doctor/appointments/{id}/prescriptions/{prescriptionId}` | Thay content và schedules |
| `PATCH` | `/api/doctor/appointments/{id}/complete` | Hoàn tất examination |

## Current storage

- Sức khỏe tổng quát lâu dài: `users`.
- Vital signs từng lần khám: `vital_signs`, liên kết `appointmentId`.
- Diagnosis: `appointments.diagnosis`.
- Prescription: `prescriptions.appointmentId`.
- Medicine schedule: `medicine_schedules.prescriptionId`.

Complete yêu cầu diagnosis, ít nhất một vital sign và ít nhất một prescription; appointment chuyển `COMPLETED`, DoctorWorkSlot chuyển `CLOSED`.

## Patient medication endpoints

| Method | Endpoint | Chức năng |
| --- | --- | --- |
| `GET` | `/api/patient/medicine-schedules` | Lấy schedules từ appointment completed |
| `PATCH` | `/api/patient/medicine-schedules/{id}/time` | Đổi schedule `NOT_YET` sang thời gian tương lai |
| `PATCH` | `/api/patient/medicine-schedules/{id}/take` | `NOT_YET → TAKEN` |

Job định kỳ chuyển `NOT_YET → MISSED` sau 60 phút kể từ `scheduledAt` nếu Patient chưa xác nhận.

## Planned MedicalRecord migration

ERD mới thay `VitalSign` bằng `MedicalRecord`, chuyển diagnosis vào MedicalRecord và Prescription tham chiếu `medicalRecordId`. Đây là thay đổi chưa được triển khai; phải có migration dữ liệu và cập nhật API/test trước khi sửa phần Current storage của spec này.
