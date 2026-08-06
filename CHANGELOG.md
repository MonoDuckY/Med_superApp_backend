# Changelog

All notable backend changes are documented in this file. This project follows a lightweight Keep a Changelog structure; version tags are added when the team creates a release.

## Unreleased

### Added

- Feature specifications under `docs/specs` for API conventions, authentication, user management, scheduling, appointments, clinical medication, SMS gateway, data protection and MongoDB schema.
- Three-step forgot-password flow with OTP verification and a one-time reset token.
- Doctor examination, prescription and Patient medicine-schedule APIs.
- Staff schedule/appointment filtering, blocking, rescheduling and Staff-created appointments.

### Changed

- Password reset now uses `request → verify → reset`; the final request no longer contains OTP.
- Medicine schedule uses full `scheduledAt` date-time and supports `NOT_YET`, `TAKEN`, `MISSED`.
- Appointment supports `IN_PROGRESS` examination state.

### Security

- Password reset token is random, short-lived, one-time and stored only as SHA-256 hash.
- Password reset invalidates access/refresh token hashes and clears account lockout.

### Planned

- Align User with one role and `(phoneLookup, role)` uniqueness.
- Add Role catalog collection.
- Replace VitalSign with MedicalRecord and migrate diagnosis/prescription references.

## Documentation update rule

Every pull request that changes public APIs, validation, permissions, status transitions, persistence or security must also update the relevant file in `docs/specs` and add an entry under `Unreleased`.
