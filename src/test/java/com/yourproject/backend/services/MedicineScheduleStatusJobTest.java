package com.yourproject.backend.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.yourproject.backend.models.MedicineSchedule;
import com.yourproject.backend.models.MedicineScheduleStatus;
import com.yourproject.backend.repositories.MedicineScheduleRepository;
import com.yourproject.backend.repositories.MealRepository;
import com.yourproject.backend.repositories.WorkoutRepository;

@ExtendWith(MockitoExtension.class)
class MedicineScheduleStatusJobTest {
    @Mock
    private MedicineScheduleRepository medicineScheduleRepository;
    @Mock
    private MealRepository mealRepository;
    @Mock
    private WorkoutRepository workoutRepository;

    @InjectMocks
    private MedicineScheduleStatusJob job;

    @Test
    void markOverdueSchedulesAsMissed_updatesNotYetSchedulesPastGracePeriod() {
        MedicineSchedule schedule = MedicineSchedule.builder()
                .status(MedicineScheduleStatus.NOT_YET)
                .scheduledAt(Instant.now().minusSeconds(7200))
                .build();
        when(medicineScheduleRepository.findAllByStatusAndScheduledAtBefore(
                eq(MedicineScheduleStatus.NOT_YET), any(Instant.class)))
                .thenReturn(List.of(schedule));
        when(mealRepository.findAllByStatusAndScheduledAtBefore(any(), any())).thenReturn(List.of());
        when(workoutRepository.findAllByStatusAndScheduledAtBefore(any(), any())).thenReturn(List.of());

        job.markOverdueSchedulesAsMissed();

        assertEquals(MedicineScheduleStatus.MISSED, schedule.getStatus());
        verify(medicineScheduleRepository).saveAll(List.of(schedule));
    }

    @Test
    void markOverdueSchedulesAsMissed_doesNotWriteWhenNothingIsOverdue() {
        when(medicineScheduleRepository.findAllByStatusAndScheduledAtBefore(
                eq(MedicineScheduleStatus.NOT_YET), any(Instant.class)))
                .thenReturn(List.of());
        when(mealRepository.findAllByStatusAndScheduledAtBefore(any(), any())).thenReturn(List.of());
        when(workoutRepository.findAllByStatusAndScheduledAtBefore(any(), any())).thenReturn(List.of());

        job.markOverdueSchedulesAsMissed();

        verify(medicineScheduleRepository, never()).saveAll(any());
    }
}
