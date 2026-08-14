# Clinical Examination and Medication Specification

## Medical record images

- `POST /api/doctor/appointments/{appointmentId}/medical-images` uploads one JPEG, PNG or WEBP image from multipart part `image`.
- `DELETE /api/doctor/appointments/{appointmentId}/medical-images/{imageId}` deletes one image.
- Only the Doctor who owns the appointment may upload or delete images, and the appointment must be `IN_PROGRESS`.
- Each image must not exceed 5 MB. Multiple images are added through repeated upload requests.
- Files use the private S3 prefix `medical-images/<medicalRecordId>/` by default; MongoDB stores only object keys in `medical_records.medicalImages`.
- Examination responses expose `medicalImages[]` with `imageId`, temporary presigned `url` and `expiresAt`.
- Images are optional. After the examination is `COMPLETED`, they are read-only.

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
| `GET` | `/api/doctor/appointments/patients` | Lấy danh sách Patient duy nhất từng có appointment với Doctor đang đăng nhập; trả `id`, `fullName`, `phoneNumber` |
| `GET` | `/api/doctor/appointments/patients/{patientId}/medical-records` | Lấy toàn bộ appointment của Patient cùng MedicalRecord, Prescription và MedicineSchedule; không giới hạn theo Doctor phụ trách |
| `PATCH` | `/api/doctor/appointments/{id}/start` | `CONFIRMED → IN_PROGRESS` |
| `PATCH` | `/api/doctor/appointments/{id}/clinical-information` | Cập nhật sức khỏe lâu dài trong User và MedicalRecord theo appointment |
| `PATCH` | `/api/doctor/appointments/{id}/diagnosis` | Cập nhật diagnosis trong MedicalRecord |
| `POST` | `/api/doctor/appointments/{id}/medical-images` | Upload một ảnh MedicalRecord qua multipart part `image` |
| `DELETE` | `/api/doctor/appointments/{id}/medical-images/{imageId}` | Xóa ảnh MedicalRecord khi examination đang `IN_PROGRESS` |
| `POST` | `/api/doctor/appointments/{id}/prescriptions` | Tạo prescription và medicine schedules |
| `PATCH` | `/api/doctor/appointments/{id}/prescription` | Thay content và schedules của prescription duy nhất; không nhận prescription ID |
| `PATCH` | `/api/doctor/appointments/{id}/complete` | Hoàn tất examination |

## Current storage

- Sức khỏe tổng quát lâu dài: `users`.
- Hồ sơ từng lần khám: `medical_records`, unique theo `appointmentId`.
- MedicalRecord chứa `diagnosis`, `note`, `bloodPressure`, `heartRate`, `breathingRate`, `bodyTemperature`, `bloodLipids` và danh sách object key `medicalImages`.
- `bloodPressure` chỉ nhận hai nhóm từ 1 đến 3 chữ số, phân cách bằng `/`. Backend chuẩn hóa mỗi nhóm thành 3 chữ số trước khi lưu, ví dụ `120/80` thành `120/080` và `12/8` thành `012/008`.
- Mỗi Appointment chỉ có tối đa một Prescription; `prescriptions.medicalRecordId` là duy nhất.
- Medicine schedule: `medicine_schedules.prescriptionId`.
- Meal: `meals.userId`; `prescriptionId` bắt buộc khi Doctor tạo trong Prescription và `null` khi Patient tự ghi nhật ký.
- Mỗi Meal phải có ít nhất một Dish. Dish lưu riêng trong `dishes` và tham chiếu `mealId`; response Meal trả `dishes` dạng danh sách lồng.
- Workout: `workouts.userId`; `prescriptionId` bắt buộc khi Doctor tạo trong Prescription và `null` khi Patient tự ghi nhật ký.
- Meal và Workout do Doctor kê dùng trạng thái `NOT_YET → COMPLETED/MISSED` (Job đánh dấu `MISSED` sau 60 phút quá hạn). Hoạt động do Patient tự ghi nhật ký được lưu trực tiếp ở trạng thái `COMPLETED`.
- Hoạt động do Patient tự tạo khả dụng ngay; plan do Doctor tạo trong Prescription chỉ khả dụng khi Appointment đã `COMPLETED`.

Complete yêu cầu MedicalRecord có diagnosis, ít nhất một chỉ số lâm sàng và ít nhất một prescription; appointment chuyển `COMPLETED`, DoctorWorkSlot chuyển `CLOSED`.

## Patient medication endpoints

| Method | Endpoint | Chức năng |
| --- | --- | --- |
| `GET` | `/api/patient/medicine-schedules` | Lấy schedules từ appointment completed |
| `PATCH` | `/api/patient/medicine-schedules/{id}/time` | Đổi schedule `NOT_YET` sang thời gian tương lai nhưng phải giữ nguyên ngày theo múi giờ `Asia/Ho_Chi_Minh` |
| `PATCH` | `/api/patient/medicine-schedules/{id}/take` | `NOT_YET → TAKEN` |

Job định kỳ chuyển `NOT_YET → MISSED` sau 60 phút kể từ `scheduledAt` nếu Patient chưa xác nhận.

Job notification tạo reminder khi schedule `NOT_YET` còn không quá 30 phút và `isNotified=false`. Sau khi tạo, `isNotified=true`; khi Patient đổi giờ, field được reset về false.

## Patient meal and workout endpoints (Health Tracker)

| Method | Endpoint | Chức năng |
| --- | --- | --- |
| `GET` | `/api/patient/meal-plans` | Lấy toàn bộ Meal do Doctor hoặc Patient tạo |
| `POST` | `/api/patient/meal-plans` | Patient tự ghi nhật ký Meal với `prescriptionId=null`, trạng thái `COMPLETED`; thời gian phải ở hiện tại/quá khứ (`<= now`) và trong phạm vi từ 2 ngày trước đến hôm nay (UTC+7) |
| `PATCH` | `/api/patient/meal-plans/{id}/time` | Đổi thời gian Meal `NOT_YET`, phải cùng ngày UTC+7 |
| `PATCH` | `/api/patient/meal-plans/{id}/complete` | Chuyển Meal `NOT_YET → COMPLETED` (dành cho plan do Doctor kê) |
| `GET` | `/api/patient/workout-plans` | Lấy toàn bộ Workout do Doctor hoặc Patient tạo |
| `POST` | `/api/patient/workout-plans` | Patient tự ghi nhật ký Workout với `prescriptionId=null`, trạng thái `COMPLETED`; thời gian phải ở hiện tại/quá khứ (`<= now`) và trong phạm vi từ 2 ngày trước đến hôm nay (UTC+7) |
| `PATCH` | `/api/patient/workout-plans/{id}/time` | Đổi thời gian Workout `NOT_YET`, phải cùng ngày UTC+7 |
| `PATCH` | `/api/patient/workout-plans/{id}/complete` | Chuyển Workout `NOT_YET → COMPLETED` (dành cho plan do Doctor kê) |

Doctor gửi tùy chọn `meals` và `workouts` trong payload tạo/cập nhật Prescription. Backend tự gắn `userId` của Patient và `prescriptionId`; Patient không được tự chọn hai ID này.

Dish nhận `dishName`, `quantity`, `unit`, `totalCalories`, `totalProtein`, `totalCarbohydrates` và `totalFat`. `quantity` phải lớn hơn 0; các chỉ số dinh dưỡng không được âm.
