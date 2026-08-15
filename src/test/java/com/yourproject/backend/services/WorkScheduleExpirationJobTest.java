package com.yourproject.backend.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.yourproject.backend.models.DoctorWorkSlot;
import com.yourproject.backend.models.DoctorWorkSlotStatus;
import com.yourproject.backend.repositories.DoctorWorkSlotRepository;

@ExtendWith(MockitoExtension.class)
class WorkScheduleExpirationJobTest {
    private static final ZoneId HOSPITAL_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Mock
    private DoctorWorkSlotRepository doctorWorkSlotRepository;

    @InjectMocks
    private WorkScheduleExpirationJob job;

    @Test
    void rejectsPendingSchedulesWhenWorkDateHasArrived() {
        LocalDate today = LocalDate.now(HOSPITAL_ZONE);
        DoctorWorkSlot slot = DoctorWorkSlot.builder()
                .id("slot-1")
                .workDate(today)
                .status(DoctorWorkSlotStatus.PENDING)
                .conflictActive(true)
                .build();
        when(doctorWorkSlotRepository.findAllByStatusAndWorkDateLessThanEqual(
                DoctorWorkSlotStatus.PENDING,
                today)).thenReturn(List.of(slot));

        job.rejectExpiredPendingSchedules();

        assertEquals(DoctorWorkSlotStatus.REJECTED, slot.getStatus());
        assertEquals(WorkScheduleExpirationJob.AUTOMATIC_REJECTION_REASON, slot.getRejectionReason());
        assertNotNull(slot.getReviewedAt());
        assertNotNull(slot.getUpdatedAt());
        assertFalse(slot.isConflictActive());
        verify(doctorWorkSlotRepository).saveAll(List.of(slot));
    }

    @Test
    void doesNothingWhenNoPendingScheduleHasExpired() {
        LocalDate today = LocalDate.now(HOSPITAL_ZONE);
        when(doctorWorkSlotRepository.findAllByStatusAndWorkDateLessThanEqual(
                DoctorWorkSlotStatus.PENDING,
                today)).thenReturn(List.of());

        job.rejectExpiredPendingSchedules();

        verify(doctorWorkSlotRepository, never()).saveAll(anyList());
    }
}
