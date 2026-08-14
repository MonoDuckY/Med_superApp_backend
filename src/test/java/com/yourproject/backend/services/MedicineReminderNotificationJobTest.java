package com.yourproject.backend.services;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.yourproject.backend.models.Appointment;
import com.yourproject.backend.models.MedicalRecord;
import com.yourproject.backend.models.MedicineSchedule;
import com.yourproject.backend.models.MedicineScheduleStatus;
import com.yourproject.backend.models.Prescription;
import com.yourproject.backend.repositories.AppointmentRepository;
import com.yourproject.backend.repositories.MedicalRecordRepository;
import com.yourproject.backend.repositories.MedicineScheduleRepository;
import com.yourproject.backend.repositories.PrescriptionRepository;

@ExtendWith(MockitoExtension.class)
class MedicineReminderNotificationJobTest {
    @Mock
    private MedicineScheduleRepository medicineScheduleRepository;
    @Mock
    private PrescriptionRepository prescriptionRepository;
    @Mock
    private MedicalRecordRepository medicalRecordRepository;
    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private MedicineReminderNotificationJob job;

    @Test
    void createsReminderForScheduleWithinThirtyMinutes() {
        MedicineSchedule schedule = MedicineSchedule.builder()
                .id("schedule-1")
                .prescriptionId("prescription-1")
                .medicineName("Medicine A")
                .dosage("10 ml")
                .scheduledAt(Instant.now().plusSeconds(1200))
                .status(MedicineScheduleStatus.NOT_YET)
                .build();
        when(medicineScheduleRepository.findPendingReminders(any(), any())).thenReturn(List.of(schedule));
        when(prescriptionRepository.findById("prescription-1"))
                .thenReturn(Optional.of(Prescription.builder()
                        .id("prescription-1").medicalRecordId("record-1").build()));
        when(medicalRecordRepository.findById("record-1"))
                .thenReturn(Optional.of(MedicalRecord.builder()
                        .id("record-1").appointmentId("appointment-1").build()));
        when(appointmentRepository.findById("appointment-1"))
                .thenReturn(Optional.of(Appointment.builder()
                        .id("appointment-1").patientId("patient-1").build()));

        job.createUpcomingMedicineReminders();

        verify(notificationService).createMedicineReminder("patient-1", schedule);
        verify(medicineScheduleRepository).save(schedule);
        assertTrue(schedule.isNotified());
    }

    @Test
    void doesNothingWhenNoScheduleIsWithinReminderWindow() {
        when(medicineScheduleRepository.findPendingReminders(any(), any())).thenReturn(List.of());

        job.createUpcomingMedicineReminders();

        verify(notificationService, never()).createMedicineReminder(any(), any());
        verify(medicineScheduleRepository, never()).save(any());
    }
}
