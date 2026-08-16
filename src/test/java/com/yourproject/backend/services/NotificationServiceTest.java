package com.yourproject.backend.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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

import com.yourproject.backend.models.MedicineSchedule;
import com.yourproject.backend.models.MedicineScheduleStatus;
import com.yourproject.backend.models.Notification;
import com.yourproject.backend.models.NotificationStatus;
import com.yourproject.backend.repositories.NotificationRepository;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {
    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService service;

    @Test
    void createsUnreadMedicineReminderWithRequiredContent() {
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        MedicineSchedule schedule = MedicineSchedule.builder()
                .medicineName("Medicine A")
                .dosage("10 ml")
                .scheduledAt(Instant.parse("2026-08-15T01:00:00Z"))
                .status(MedicineScheduleStatus.NOT_YET)
                .build();

        Notification result = service.createMedicineReminder("patient-1", schedule);

        assertEquals("patient-1", result.getUserId());
        assertEquals(NotificationStatus.UNREAD, result.getStatus());
        assertTrue(result.getContent().contains("Medicine A"));
        assertTrue(result.getContent().contains("10 ml"));
        assertTrue(result.getContent().contains("NOT_YET"));
    }

    @Test
    void createsUnreadAppointmentCancelledNotification() {
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        Instant time = Instant.parse("2026-08-20T09:00:00Z");

        Notification result = service.createAppointmentCancelled("patient-1", time);

        assertEquals("patient-1", result.getUserId());
        assertEquals(NotificationStatus.UNREAD, result.getStatus());
        assertTrue(result.getContent().contains("cancelled"));
        assertTrue(result.getContent().contains("CANCELLED"));
    }

    @Test
    void patientMarksOwnNotificationRead() {
        Notification notification = Notification.builder()
                .id("notification-1")
                .userId("patient-1")
                .status(NotificationStatus.UNREAD)
                .build();
        when(notificationRepository.findByIdAndUserId("notification-1", "patient-1"))
                .thenReturn(Optional.of(notification));
        when(notificationRepository.save(notification)).thenReturn(notification);

        var response = service.markRead("patient-1", "notification-1");

        assertEquals(NotificationStatus.READ, response.getStatus());
        verify(notificationRepository).save(notification);
    }

    @Test
    void patientMarksAllUnreadNotificationsRead() {
        Notification first = Notification.builder().status(NotificationStatus.UNREAD).build();
        Notification second = Notification.builder().status(NotificationStatus.UNREAD).build();
        when(notificationRepository.findAllByUserIdAndStatus("patient-1", NotificationStatus.UNREAD))
                .thenReturn(List.of(first, second));

        service.markAllRead("patient-1");

        assertEquals(NotificationStatus.READ, first.getStatus());
        assertEquals(NotificationStatus.READ, second.getStatus());
        verify(notificationRepository).saveAll(List.of(first, second));
    }
}
