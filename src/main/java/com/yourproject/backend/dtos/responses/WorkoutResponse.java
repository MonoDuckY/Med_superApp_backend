package com.yourproject.backend.dtos.responses;

import java.time.Instant;

import com.yourproject.backend.models.PlanScheduleStatus;
import com.yourproject.backend.models.Workout;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class WorkoutResponse {
    String id;
    String userId;
    String prescriptionId;
    String workoutName;
    String content;
    Instant scheduledAt;
    PlanScheduleStatus status;
    String note;

    public static WorkoutResponse from(Workout workout) {
        return WorkoutResponse.builder().id(workout.getId()).userId(workout.getUserId())
                .prescriptionId(workout.getPrescriptionId()).workoutName(workout.getWorkoutName())
                .content(workout.getContent()).scheduledAt(workout.getScheduledAt())
                .status(workout.getStatus()).note(workout.getNote()).build();
    }
}
