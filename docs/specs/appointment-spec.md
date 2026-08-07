# Appointment Specification

## Patient endpoints

| Method | Endpoint | Chức năng |
| --- | --- | --- |
| `GET` | `/api/patient/doctors` | Lấy danh sách Doctor active |
| `GET` | `/api/patient/appointments/available-slots` | Lọc slot available theo date/doctorName |
| `POST` | `/api/patient/appointments` | Đặt `doctorWorkSlotId` và chờ Staff confirm |
| `GET` | `/api/patient/appointments` | Lấy appointment của Patient |
| `PATCH` | `/api/patient/appointments/{appointmentId}/cancel` | Hủy appointment thuộc Patient |

## Staff endpoints

| Method | Endpoint | Chức năng |
| --- | --- | --- |
| `GET` | `/api/staff/doctors` | Lấy danh sách Doctor active cho Staff |
| `GET` | `/api/staff/scheduling/appointments/pending` | Lấy appointment pending |
| `GET` | `/api/staff/scheduling/appointments` | Lọc appointment theo status |
| `PATCH` | `/api/staff/scheduling/appointments/{id}/decision` | Confirm/reject appointment |
| `PATCH` | `/api/staff/scheduling/appointments/{id}/cancel` | Hospital cancellation |
| `PATCH` | `/api/staff/scheduling/appointments/{id}/reschedule` | Chuyển sang DoctorWorkSlot khác |
| `POST` | `/api/staff/scheduling/appointments` | Staff tạo appointment cho Patient |

## Business rules

- Patient chỉ đặt DoctorWorkSlot `AVAILABLE`.
- Patient không nhìn thấy và không được đặt trực tiếp DoctorWorkSlot ca đêm.
- Staff được phép tạo hoặc reschedule appointment vào DoctorWorkSlot ca đêm bằng các API Staff hiện có.
- Booking tạo appointment `PENDING_STAFF_CONFIRMATION` và giữ slot ở `SCHEDULING`.
- Staff confirm chuyển appointment sang `CONFIRMED` và slot sang `BOOKED`.
- Reject/cancel giải phóng slot khi phù hợp.
- Reschedule phải dùng slot thay thế hợp lệ và lưu thông tin reschedule.
- Patient không được có nhiều appointment hoạt động trong cùng ngày; appointment rejected/cancelled không còn được tính là hoạt động.

## Status flow

`PENDING_STAFF_CONFIRMATION → CONFIRMED → IN_PROGRESS → COMPLETED`.

Nhánh khác: `PENDING_STAFF_CONFIRMATION → REJECTED/CANCELLED`, `CONFIRMED → CANCELLED/NO_SHOW`.
