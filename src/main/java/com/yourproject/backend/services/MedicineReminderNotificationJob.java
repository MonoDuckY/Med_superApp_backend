package com.yourproject.backend.services;

import java.time.Duration;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.yourproject.backend.models.Appointment;
import com.yourproject.backend.models.MedicalRecord;
import com.yourproject.backend.models.MedicineSchedule;
import com.yourproject.backend.models.Prescription;
import com.yourproject.backend.repositories.AppointmentRepository;
import com.yourproject.backend.repositories.MedicalRecordRepository;
import com.yourproject.backend.repositories.MedicineScheduleRepository;
import com.yourproject.backend.repositories.PrescriptionRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MedicineReminderNotificationJob {
    private static final Logger LOGGER = LoggerFactory.getLogger(MedicineReminderNotificationJob.class);
    private static final Duration REMINDER_LEAD_TIME = Duration.ofMinutes(30);

    private final MedicineScheduleRepository medicineScheduleRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final AppointmentRepository appointmentRepository;
    private final NotificationService notificationService;

    @Scheduled(fixedDelayString = "${app.notification.medicine-reminder-job-delay-ms:60000}")
    public void createUpcomingMedicineReminders() {
        Instant now = Instant.now();
        medicineScheduleRepository.findPendingReminders(now, now.plus(REMINDER_LEAD_TIME))
                .forEach(this::createReminder);
    }

    private void createReminder(MedicineSchedule schedule) {
        try {
            Prescription prescription = prescriptionRepository.findById(schedule.getPrescriptionId())
                    .orElseThrow();
            MedicalRecord medicalRecord = medicalRecordRepository.findById(prescription.getMedicalRecordId())
                    .orElseThrow();
            Appointment appointment = appointmentRepository.findById(medicalRecord.getAppointmentId())
                    .orElseThrow();
            String patientId = resolvePatientId(appointment);
            if (patientId == null) {
                throw new IllegalStateException("Appointment has no patient ID.");
            }
            notificationService.createMedicineReminder(patientId, schedule);
            schedule.setNotified(true);
            medicineScheduleRepository.save(schedule);
        } catch (RuntimeException exception) {
            LOGGER.warn("Unable to create medicine reminder notification for schedule {}: {}",
                    schedule.getId(), exception.getMessage());
        }
    }

    private String resolvePatientId(Appointment appointment) {
        if (appointment.getPatientId() != null && !appointment.getPatientId().isBlank()) {
            return appointment.getPatientId();
        }
        return appointment.getPatientUserId() == null || appointment.getPatientUserId().isBlank()
                ? null
                : appointment.getPatientUserId();
    }
}
