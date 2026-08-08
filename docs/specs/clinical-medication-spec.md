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
- Meal plan: `meals.userId`; `prescriptionId` bắt buộc khi Doctor tạo trong Prescription và `null` khi Patient tự tạo.
- Mỗi Meal phải có ít nhất một Dish. Dish lưu riêng trong `dishes` và tham chiếu `mealId`; response Meal trả `dishes` dạng danh sách lồng.
- Workout plan: `workouts.userId`; `prescriptionId` bắt buộc khi Doctor tạo trong Prescription và `null` khi Patient tự tạo.
- Meal và Workout dùng trạng thái `NOT_YET → COMPLETED/MISSED`. Job đánh dấu `MISSED` sau 60 phút quá hạn.
- Plan do Patient tự tạo khả dụng ngay; plan do Doctor tạo trong Prescription chỉ khả dụng khi Appointment đã `COMPLETED`.

Complete yêu cầu MedicalRecord có diagnosis, ít nhất một chỉ số lâm sàng và ít nhất một prescription; appointment chuyển `COMPLETED`, DoctorWorkSlot chuyển `CLOSED`.

## Patient medication endpoints

| Method | Endpoint | Chức năng |
| --- | --- | --- |
| `GET` | `/api/patient/medicine-schedules` | Lấy schedules từ appointment completed |
| `PATCH` | `/api/patient/medicine-schedules/{id}/time` | Đổi schedule `NOT_YET` sang thời gian tương lai nhưng phải giữ nguyên ngày theo múi giờ `Asia/Ho_Chi_Minh` |
| `PATCH` | `/api/patient/medicine-schedules/{id}/take` | `NOT_YET → TAKEN` |

Job định kỳ chuyển `NOT_YET → MISSED` sau 60 phút kể từ `scheduledAt` nếu Patient chưa xác nhận.

## Patient meal and workout endpoints

| Method | Endpoint | Chức năng |
| --- | --- | --- |
| `GET` | `/api/patient/meal-plans` | Lấy toàn bộ Meal do Doctor hoặc Patient tạo |
| `POST` | `/api/patient/meal-plans` | Patient tự tạo Meal với `prescriptionId=null` |
| `PATCH` | `/api/patient/meal-plans/{id}/time` | Đổi thời gian Meal `NOT_YET`, phải tương lai và cùng ngày UTC+7 |
| `PATCH` | `/api/patient/meal-plans/{id}/complete` | Chuyển Meal `NOT_YET → COMPLETED` |
| `GET` | `/api/patient/workout-plans` | Lấy toàn bộ Workout do Doctor hoặc Patient tạo |
| `POST` | `/api/patient/workout-plans` | Patient tự tạo Workout với `prescriptionId=null` |
| `PATCH` | `/api/patient/workout-plans/{id}/time` | Đổi thời gian Workout `NOT_YET`, phải tương lai và cùng ngày UTC+7 |
| `PATCH` | `/api/patient/workout-plans/{id}/complete` | Chuyển Workout `NOT_YET → COMPLETED` |

Doctor gửi tùy chọn `meals` và `workouts` trong payload tạo/cập nhật Prescription. Backend tự gắn `userId` của Patient và `prescriptionId`; Patient không được tự chọn hai ID này.

Dish nhận `dishName`, `quantity`, `unit`, `totalCalories`, `totalProtein`, `totalCarbohydrates` và `totalFat`. `quantity` phải lớn hơn 0; các chỉ số dinh dưỡng không được âm.
