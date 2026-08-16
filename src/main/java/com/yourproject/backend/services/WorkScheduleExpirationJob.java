package com.yourproject.backend.services;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.yourproject.backend.models.DoctorWorkSlotStatus;
import com.yourproject.backend.repositories.DoctorWorkSlotRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WorkScheduleExpirationJob {
    public static final String AUTOMATIC_REJECTION_REASON =
            "Automatically rejected because the work date arrived before staff approval.";
    private static final ZoneId HOSPITAL_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final DoctorWorkSlotRepository doctorWorkSlotRepository;

    @Scheduled(fixedDelayString = "${app.work-schedule.expiration-job-delay-ms:60000}")
    public void rejectExpiredPendingSchedules() {
        LocalDate today = LocalDate.now(HOSPITAL_ZONE);
        var expiredSlots = doctorWorkSlotRepository.findAllByStatusAndWorkDateLessThanEqual(
                DoctorWorkSlotStatus.PENDING,
                today);
        if (expiredSlots.isEmpty()) {
            return;
        }

        Instant reviewedAt = Instant.now();
        expiredSlots.forEach(slot -> {
            slot.setStatus(DoctorWorkSlotStatus.REJECTED);
            slot.setRejectionReason(AUTOMATIC_REJECTION_REASON);
            slot.setReviewedAt(reviewedAt);
            slot.setConflictActive(false);
            slot.setUpdatedAt(reviewedAt);
        });
        doctorWorkSlotRepository.saveAll(expiredSlots);
    }
}
