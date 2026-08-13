# Specification Changelog

## 2026-08-13

- Added `POST /api/staff/patients`; authenticated Staff creates a passwordless Patient account without supplying `role` or `password`.
- Doctors must submit and modify pending work schedules at least one calendar day before the work date.
- Added the work-schedule expiration job, which automatically rejects pending schedules when their work date arrives.
- Added `WORK_SCHEDULE_EXPIRATION_JOB_DELAY_MS`; the default job interval is 60 seconds.
