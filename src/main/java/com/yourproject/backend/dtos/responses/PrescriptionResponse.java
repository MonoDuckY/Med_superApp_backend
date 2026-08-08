package com.yourproject.backend.dtos.responses;

import java.util.List;

import com.yourproject.backend.models.MedicineSchedule;
import com.yourproject.backend.models.Meal;
import com.yourproject.backend.models.Dish;
import com.yourproject.backend.models.Prescription;
import com.yourproject.backend.models.Workout;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PrescriptionResponse {
    String id;
    String medicalRecordId;
    String content;
    List<MedicineScheduleResponse> medicineSchedules;
    List<MealResponse> meals;
    List<WorkoutResponse> workouts;

    public static PrescriptionResponse from(
            Prescription prescription,
            List<MedicineSchedule> schedules,
            List<Meal> meals,
            List<Dish> dishes,
            List<Workout> workouts) {
        return PrescriptionResponse.builder()
                .id(prescription.getId())
                .medicalRecordId(prescription.getMedicalRecordId())
                .content(prescription.getContent())
                .medicineSchedules(schedules.stream().map(MedicineScheduleResponse::from).toList())
                .meals(meals.stream().map(meal -> MealResponse.from(meal, dishes.stream()
                        .filter(dish -> meal.getId().equals(dish.getMealId())).toList())).toList())
                .workouts(workouts.stream().map(WorkoutResponse::from).toList())
                .build();
    }
}
