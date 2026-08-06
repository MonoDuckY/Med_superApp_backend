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
| `PATCH` | `/api/doctor/appointments/{id}/clinical-information` | Cập nhật sức khỏe lâu dài trong User và MedicalRecord theo appointment |
| `PATCH` | `/api/doctor/appointments/{id}/diagnosis` | Cập nhật diagnosis trong MedicalRecord |
| `POST` | `/api/doctor/appointments/{id}/prescriptions` | Tạo prescription và medicine schedules |
| `PATCH` | `/api/doctor/appointments/{id}/prescriptions/{prescriptionId}` | Thay content và schedules |
| `PATCH` | `/api/doctor/appointments/{id}/complete` | Hoàn tất examination |

## Current storage

- Sức khỏe tổng quát lâu dài: `users`.
- Hồ sơ từng lần khám: `medical_records`, unique theo `appointmentId`.
- MedicalRecord chứa `diagnosis`, `note`, `bloodPressure`, `heartRate`, `breathingRate`, `bodyTemperature`, `bloodLipids`.
- Prescription: `prescriptions.medicalRecordId`.
- Medicine schedule: `medicine_schedules.prescriptionId`.

Complete yêu cầu MedicalRecord có diagnosis, ít nhất một chỉ số lâm sàng và ít nhất một prescription; appointment chuyển `COMPLETED`, DoctorWorkSlot chuyển `CLOSED`.

## Patient medication endpoints

| Method | Endpoint | Chức năng |
| --- | --- | --- |
| `GET` | `/api/patient/medicine-schedules` | Lấy schedules từ appointment completed |
| `PATCH` | `/api/patient/medicine-schedules/{id}/time` | Đổi schedule `NOT_YET` sang thời gian tương lai |
| `PATCH` | `/api/patient/medicine-schedules/{id}/take` | `NOT_YET → TAKEN` |

Job định kỳ chuyển `NOT_YET → MISSED` sau 60 phút kể từ `scheduledAt` nếu Patient chưa xác nhận.
