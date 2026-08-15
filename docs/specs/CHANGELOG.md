# Specification Changelog

## 2026-08-15 - Appointment cancellation notification

- Added automatic Patient notification when a pending or confirmed appointment is cancelled by Staff or Patient.
- Updated `notification-spec.md` with appointment cancellation event.

## 2026-08-15 - Doctor certificate PDF

- Doctor certificate upload now accepts PDF in addition to JPEG, PNG and WEBP.
- PDF uploads require `application/pdf` and a valid `%PDF-` file signature.
- MedicalRecord images remain image-only and do not accept PDF.

## 2026-08-15 - Blood pressure validation

- Blood pressure accepts only `1-3 digits/1-3 digits` and is normalized to `xxx/xxx` before persistence.

## 2026-08-14 - Medical record images

- Added private S3 medical-image upload and deletion for in-progress Doctor examinations.
- Added `medical_records.medicalImages` object-key storage and presigned URLs in examination responses.
- Added optional `AWS_S3_MEDICAL_IMAGE_PREFIX`; the default remains `medical-images`.

## 2026-08-14

- Added Patient notification persistence and read/unread APIs.
- Added notifications for approved/rescheduled appointments and medicine reminders 30 minutes before `scheduledAt`.
- Added `MedicineSchedule.isNotified` and reset it when the Patient changes reminder time.
- Removed `roomId` from Doctor work-schedule submit/modify requests.
- Pending DoctorWorkSlot documents now keep `roomId=null` until Staff review.
- Staff must provide an active `roomId` when approving a work schedule; room and Doctor conflicts are checked during approval.

## 2026-08-13

- Added `POST /api/staff/patients`; authenticated Staff creates a passwordless Patient account without supplying `role` or `password`.
- Doctors must submit and modify pending work schedules at least one calendar day before the work date.
- Added the work-schedule expiration job, which automatically rejects pending schedules when their work date arrives.
- Added `WORK_SCHEDULE_EXPIRATION_JOB_DELAY_MS`; the default job interval is 60 seconds.
