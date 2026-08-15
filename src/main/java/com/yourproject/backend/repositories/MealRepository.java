package com.yourproject.backend.repositories;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.yourproject.backend.models.Meal;
import com.yourproject.backend.models.PlanScheduleStatus;

public interface MealRepository extends MongoRepository<Meal, String> {
    List<Meal> findAllByUserIdOrderByScheduledAtAsc(String userId);
    List<Meal> findAllByPrescriptionIdInOrderByScheduledAtAsc(Collection<String> prescriptionIds);
    boolean existsByUserIdAndMealNameAndScheduledAt(String userId, String mealName, Instant scheduledAt);
    void deleteAllByPrescriptionId(String prescriptionId);
    List<Meal> findAllByStatusAndScheduledAtBefore(PlanScheduleStatus status, Instant scheduledAt);
}
