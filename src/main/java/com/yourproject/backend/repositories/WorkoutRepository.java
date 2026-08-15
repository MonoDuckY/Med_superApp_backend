package com.yourproject.backend.repositories;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.yourproject.backend.models.PlanScheduleStatus;
import com.yourproject.backend.models.Workout;

public interface WorkoutRepository extends MongoRepository<Workout, String> {
    List<Workout> findAllByUserIdOrderByScheduledAtAsc(String userId);
    List<Workout> findAllByPrescriptionIdInOrderByScheduledAtAsc(Collection<String> prescriptionIds);
    boolean existsByUserIdAndWorkoutNameAndScheduledAt(String userId, String workoutName, Instant scheduledAt);
    void deleteAllByPrescriptionId(String prescriptionId);
    List<Workout> findAllByStatusAndScheduledAtBefore(PlanScheduleStatus status, Instant scheduledAt);
}
