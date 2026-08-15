package com.yourproject.backend.dtos.responses;

import java.time.Instant;
import java.util.List;

import com.yourproject.backend.models.Meal;
import com.yourproject.backend.models.Dish;
import com.yourproject.backend.models.PlanScheduleStatus;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MealResponse {
    String id;
    String userId;
    String prescriptionId;
    String mealName;
    Instant scheduledAt;
    PlanScheduleStatus status;
    String note;
    List<DishResponse> dishes;

    public static MealResponse from(Meal meal, List<Dish> dishes) {
        return MealResponse.builder().id(meal.getId()).userId(meal.getUserId())
                .prescriptionId(meal.getPrescriptionId()).mealName(meal.getMealName())
                .scheduledAt(meal.getScheduledAt()).status(meal.getStatus()).note(meal.getNote())
                .dishes(dishes.stream().map(DishResponse::from).toList()).build();
    }
}
