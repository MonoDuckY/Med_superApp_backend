# Patient Notification Specification

## Scope

Backend stores notifications only for Patients. It does not create Doctor notifications and does not send mobile push notifications.

Notifications are created for exactly these events:

- Staff approves a pending Patient appointment.
- Staff reschedules a pending or confirmed Patient appointment.
- A `NOT_YET` medicine schedule enters the 30-minute window before `scheduledAt`.

## Medicine reminder

- The reminder job runs every 60 seconds by default.
- It selects `NOT_YET` schedules where `scheduledAt` is after now and no later than 30 minutes from now.
- A schedule with `isNotified=true` is not selected again.
- After the notification is stored, the schedule is updated to `isNotified=true`.
- Changing a medicine schedule time resets `isNotified=false` so the new time can generate a reminder.
- `NOTIFICATION_MEDICINE_REMINDER_JOB_DELAY_MS` configures the job interval.

## Patient endpoints

| Method | Endpoint | Function |
| --- | --- | --- |
| `GET` | `/api/patient/notifications` | Get the authenticated Patient's notifications, newest first |
| `GET` | `/api/patient/notifications/unread-count` | Count unread notifications |
| `PATCH` | `/api/patient/notifications/{notificationId}/read` | Mark one owned notification as read |
| `PATCH` | `/api/patient/notifications/read-all` | Mark all owned notifications as read |

## Persistence

Collection `notifications` stores `id`, `userId`, `content`, `notifyTime`, and `status` (`UNREAD` or `READ`). Notification content contains the information required by UC-09; no `referenceId` or `dedupeKey` is stored.
