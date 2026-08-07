# Doctor Work Scheduling Specification

## Roles

- `DOCTOR`: xem options, submit, xem, sửa hoặc hủy submission phù hợp trạng thái.
- `STAFF`: quản lý room, duyệt/sửa/block work schedule.
- `ADMIN` không được dùng Staff scheduling API.

## Doctor endpoints

| Method | Endpoint | Chức năng |
| --- | --- | --- |
| `GET` | `/api/doctor/work-schedules/options` | Lấy slot và room active |
| `GET` | `/api/doctor/work-schedules` | Lấy submission theo khoảng ngày |
| `GET` | `/api/doctor/work-schedules/all` | Doctor xem lịch của mọi Doctor theo `from`, `to`, `status` |
| `POST` | `/api/doctor/work-schedules` | Submit `workDate`, `session`, `roomId`, `note` |
| `PATCH` | `/api/doctor/work-schedules/{submissionId}` | Sửa submission pending |
| `DELETE` | `/api/doctor/work-schedules/{submissionId}` | Hủy submission theo rule hiện tại |

`session` là `MORNING`, `AFTERNOON`, `FULL_TIME` hoặc `NIGHT`; session chỉ dùng để chọn các WorkSlot theo giờ, không lưu như field nghiệp vụ chính.

- `MORNING`: 08:00–12:00, gồm 8 slot.
- `AFTERNOON`: 13:00–17:00, gồm 8 slot.
- `FULL_TIME`: toàn bộ 16 slot ca ngày, không bao gồm ca đêm.
- `NIGHT`: 17:00 của `workDate` đến 08:00 ngày kế tiếp, gồm 30 slot 30 phút.
- Catalog gồm `Slot1`–`Slot46`; `Slot17`–`Slot46` là các slot ca đêm.
- Với slot ca đêm có `startTime` từ 00:00 đến trước 08:00, thời điểm thực tế được tính trên `workDate + 1 ngày`.

## Staff endpoints

| Method | Endpoint | Chức năng |
| --- | --- | --- |
| `POST` | `/api/staff/scheduling/clinic-rooms` | Tạo room |
| `GET` | `/api/staff/scheduling/clinic-rooms` | Lấy room active |
| `GET` | `/api/staff/scheduling/work-schedules/pending` | Lấy submission pending |
| `GET` | `/api/staff/scheduling/work-schedules` | Lọc submission theo status |
| `PATCH` | `/api/staff/scheduling/work-schedules/{submissionId}/decision` | Approve/reject submission |
| `PATCH` | `/api/staff/scheduling/work-schedules/{submissionId}` | Sửa schedule đã approve |
| `PATCH` | `/api/staff/scheduling/work-slots/{doctorWorkSlotId}/block` | Block slot và xử lý appointment liên quan |

## Status flow

`DoctorWorkSlotStatus`: `PENDING → REJECTED` hoặc `PENDING → AVAILABLE → SCHEDULING → BOOKED → IN_PROGRESS → CLOSED`. Slot có thể chuyển sang `CANCELLED` theo luồng block/cancel.

Mỗi submission có `submissionId` dùng nhóm nhiều DoctorWorkSlot. Không được tạo work slot trùng tổ hợp ngày, slot và room.

Mỗi phần tử trong `slots` của response lịch làm việc chứa cả `slotId` và object `slot` gồm `id`, `name`, `startTime`, `endTime` được lookup từ collection `work_slots`.
