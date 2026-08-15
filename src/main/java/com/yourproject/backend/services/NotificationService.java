package com.yourproject.backend.services;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yourproject.backend.dtos.responses.NotificationResponse;
import com.yourproject.backend.exceptions.ResourceNotFoundException;
import com.yourproject.backend.models.MedicineSchedule;
import com.yourproject.backend.models.Notification;
import com.yourproject.backend.models.NotificationStatus;
import com.yourproject.backend.repositories.NotificationRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private static final ZoneId HOSPITAL_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final NotificationRepository notificationRepository;

    public Notification createAppointmentApproved(String patientId, Instant appointmentTime) {
        return create(patientId, "Appointment confirmed for " + formatTime(appointmentTime)
                + ". Status: CONFIRMED.");
    }

    public Notification createAppointmentRescheduled(String patientId, Instant appointmentTime) {
        return create(patientId, "Appointment rescheduled to " + formatTime(appointmentTime)
                + ". Status: CONFIRMED.");
    }

    public Notification createAppointmentCancelled(String patientId, Instant appointmentTime) {
        return create(patientId, "Appointment for " + formatTime(appointmentTime)
                + " has been cancelled. Status: CANCELLED.");
    }

    public Notification createMedicineReminder(String patientId, MedicineSchedule schedule) {
        return create(patientId, "Medication reminder: " + schedule.getMedicineName()
                + ", dosage " + schedule.getDosage()
                + ", scheduled at " + formatTime(schedule.getScheduledAt())
                + ", status " + schedule.getStatus() + ".");
    }

    public List<NotificationResponse> getNotifications(String patientId) {
        return notificationRepository.findAllByUserIdOrderByNotifyTimeDesc(patientId).stream()
                .map(NotificationResponse::from)
                .toList();
    }

    public long getUnreadCount(String patientId) {
        return notificationRepository.countByUserIdAndStatus(patientId, NotificationStatus.UNREAD);
    }

    @Transactional
    public NotificationResponse markRead(String patientId, String notificationId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification was not found."));
        notification.setStatus(NotificationStatus.READ);
        return NotificationResponse.from(notificationRepository.save(notification));
    }

    @Transactional
    public void markAllRead(String patientId) {
        List<Notification> notifications = notificationRepository.findAllByUserIdAndStatus(
                patientId,
                NotificationStatus.UNREAD);
        notifications.forEach(notification -> notification.setStatus(NotificationStatus.READ));
        if (!notifications.isEmpty()) {
            notificationRepository.saveAll(notifications);
        }
    }

    private Notification create(String patientId, String content) {
        return notificationRepository.save(Notification.builder()
                .userId(patientId)
                .content(content)
                .notifyTime(Instant.now())
                .status(NotificationStatus.UNREAD)
                .build());
    }

    private String formatTime(Instant instant) {
        return TIME_FORMAT.format(instant.atZone(HOSPITAL_ZONE));
    }
}
