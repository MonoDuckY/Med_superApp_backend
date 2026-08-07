# Changelog

All notable backend changes are documented in this file. This project follows a lightweight Keep a Changelog structure; version tags are added when the team creates a release.

## Unreleased

### Added

- Doctor-wide work-schedule search endpoint at `GET /api/doctor/work-schedules/all`.
- Staff doctor directory endpoint at `GET /api/staff/doctors`.
- Doctor night work session with 30-minute slots from 17:00 through 08:00 the following day.
- Role catalog collection seeded with stable IDs: `1=ADMIN`, `2=DOCTOR`, `3=STAFF`, `4=RESEARCHER`, `5=PATIENT`.
- Feature specifications under `docs/specs` for API conventions, authentication, user management, scheduling, appointments, clinical medication, SMS gateway, data protection and MongoDB schema.
- Three-step forgot-password flow with OTP verification and a one-time reset token.
- Doctor examination, prescription and Patient medicine-schedule APIs.
- Staff schedule/appointment filtering, blocking, rescheduling and Staff-created appointments.

### Changed

- Doctor work-schedule responses now include the resolved WorkSlot object for every `slotId`.
- Doctor directory and Staff patient search now query the fixed numeric role IDs instead of legacy role names.
- Startup migration converts legacy User `roleId` names to the fixed numeric IDs.
- Updated work-slot and integration fixtures to match the 46-slot catalog and numeric role IDs.
- Replaced `vital_signs` with one `medical_records` document per appointment, moved diagnosis out of Appointment, and changed Prescription references to `medicalRecordId`.
- Patient slot search and direct booking exclude night shifts; Staff may create or reschedule appointments into approved night slots.
- Role catalog startup synchronization now replaces legacy role documents whose IDs do not match the fixed `1..5` mapping.
- User now references one role through `roleId`; authentication and authorization use the singular role.
- Login and forgot-password account selection now require `role` and use `(phoneLookup, roleId)`.
- Phone numbers may be reused by accounts with different roles, while remaining unique within one role.
- Password reset now uses `request → verify → reset`; the final request no longer contains OTP.
- Medicine schedule uses full `scheduledAt` date-time and supports `NOT_YET`, `TAKEN`, `MISSED`.
- Appointment supports `IN_PROGRESS` examination state.

### Security

- Password reset token is random, short-lived, one-time and stored only as SHA-256 hash.
- Password reset invalidates access/refresh token hashes and clears account lockout.

## Documentation update rule

Every pull request that changes public APIs, validation, permissions, status transitions, persistence or security must also update the relevant file in `docs/specs` and add an entry under `Unreleased`.
