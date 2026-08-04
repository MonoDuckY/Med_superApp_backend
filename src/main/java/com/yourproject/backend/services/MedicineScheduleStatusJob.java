package com.yourproject.backend.services;

import java.time.Duration;
import java.time.Instant;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.yourproject.backend.models.MedicineScheduleStatus;
import com.yourproject.backend.repositories.MedicineScheduleRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MedicineScheduleStatusJob {
    private static final Duration TAKEN_GRACE_PERIOD = Duration.ofMinutes(60);

    private final MedicineScheduleRepository medicineScheduleRepository;

    @Scheduled(fixedDelayString = "${app.medicine-schedule.missed-job-delay-ms:60000}")
    public void markOverdueSchedulesAsMissed() {
        Instant cutoff = Instant.now().minus(TAKEN_GRACE_PERIOD);
        var overdueSchedules = medicineScheduleRepository.findAllByStatusAndScheduledAtBefore(
                MedicineScheduleStatus.NOT_YET,
                cutoff);
        overdueSchedules.forEach(schedule -> schedule.setStatus(MedicineScheduleStatus.MISSED));
        if (!overdueSchedules.isEmpty()) {
            medicineScheduleRepository.saveAll(overdueSchedules);
        }
    }
}
