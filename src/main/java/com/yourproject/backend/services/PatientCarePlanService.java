package com.yourproject.backend.services;

import java.util.List;

import com.yourproject.backend.dtos.requests.MealRequest;
import com.yourproject.backend.dtos.requests.UpdateMedicineScheduleTimeRequest;
import com.yourproject.backend.dtos.requests.WorkoutRequest;
import com.yourproject.backend.dtos.responses.MealResponse;
import com.yourproject.backend.dtos.responses.WorkoutResponse;

public interface PatientCarePlanService {
    List<MealResponse> getMeals(String patientId);
    MealResponse createMeal(String patientId, MealRequest request);
    MealResponse updateMealTime(String patientId, String mealId, UpdateMedicineScheduleTimeRequest request);
    MealResponse completeMeal(String patientId, String mealId);
    List<WorkoutResponse> getWorkouts(String patientId);
    WorkoutResponse createWorkout(String patientId, WorkoutRequest request);
    WorkoutResponse updateWorkoutTime(String patientId, String workoutId, UpdateMedicineScheduleTimeRequest request);
    WorkoutResponse completeWorkout(String patientId, String workoutId);
}
