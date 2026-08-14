# Specification Changelog

## 2026-08-14

- Removed `roomId` from Doctor work-schedule submit/modify requests.
- Pending DoctorWorkSlot documents now keep `roomId=null` until Staff review.
- Staff must provide an active `roomId` when approving a work schedule; room and Doctor conflicts are checked during approval.

## 2026-08-13

- Added `POST /api/staff/patients`; authenticated Staff creates a passwordless Patient account without supplying `role` or `password`.
- Doctors must submit and modify pending work schedules at least one calendar day before the work date.
- Added the work-schedule expiration job, which automatically rejects pending schedules when their work date arrives.
- Added `WORK_SCHEDULE_EXPIRATION_JOB_DELAY_MS`; the default job interval is 60 seconds.
