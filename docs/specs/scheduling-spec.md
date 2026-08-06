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
| `POST` | `/api/doctor/work-schedules` | Submit `workDate`, `session`, `roomId`, `note` |
| `PATCH` | `/api/doctor/work-schedules/{submissionId}` | Sửa submission pending |
| `DELETE` | `/api/doctor/work-schedules/{submissionId}` | Hủy submission theo rule hiện tại |

`session` là `MORNING`, `AFTERNOON` hoặc `FULL_TIME`; session chỉ dùng để chọn các WorkSlot theo giờ, không lưu như field nghiệp vụ chính.

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
