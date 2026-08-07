# Clinical Examination and Medication Specification

## Authorization

- Chỉ Doctor được xem và sửa examination của chính mình.
- Doctor được xem lịch sử toàn bộ appointment và MedicalRecord của một Patient, kể cả appointment do Doctor khác đảm nhiệm.
- Danh sách Patient của Doctor chỉ gồm những Patient từng có appointment với chính Doctor đó.
- Chỉ Patient sở hữu prescription được xem/sửa medicine schedule của mình.

## Doctor endpoints

| Method | Endpoint | Chức năng |
| --- | --- | --- |
| `GET` | `/api/doctor/appointments` | Lấy appointment của Doctor, tùy chọn status |
| `GET` | `/api/doctor/appointments/{id}` | Lấy examination detail |
| `GET` | `/api/doctor/appointments/patients` | Lấy danh sách Patient duy nhất từng có appointment với Doctor đang đăng nhập; trả `id`, `fullName`, `phoneNumber`, `certificate` |
| `GET` | `/api/doctor/appointments/patients/{patientId}/medical-records` | Lấy toàn bộ appointment của Patient cùng MedicalRecord, Prescription và MedicineSchedule; không giới hạn theo Doctor phụ trách |
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
| `PATCH` | `/api/patient/medicine-schedules/{id}/time` | Đổi schedule `NOT_YET` sang thời gian tương lai nhưng phải giữ nguyên ngày theo múi giờ `Asia/Ho_Chi_Minh` |
| `PATCH` | `/api/patient/medicine-schedules/{id}/take` | `NOT_YET → TAKEN` |

Job định kỳ chuyển `NOT_YET → MISSED` sau 60 phút kể từ `scheduledAt` nếu Patient chưa xác nhận.
