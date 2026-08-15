package com.yourproject.backend.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yourproject.backend.dtos.requests.MealRequest;
import com.yourproject.backend.dtos.requests.UpdateMedicineScheduleTimeRequest;
import com.yourproject.backend.dtos.requests.WorkoutRequest;
import com.yourproject.backend.dtos.responses.ApiResponse;
import com.yourproject.backend.dtos.responses.MealResponse;
import com.yourproject.backend.dtos.responses.DoctorExaminationResponse;
import com.yourproject.backend.dtos.responses.WorkoutResponse;
import com.yourproject.backend.services.ClinicalMedicationService;
import com.yourproject.backend.services.PatientCarePlanService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/patient")
@PreAuthorize("hasRole('PATIENT')")
@RequiredArgsConstructor
public class PatientCarePlanController {
    private final PatientCarePlanService patientCarePlanService;
    private final ClinicalMedicationService clinicalMedicationService;

    @GetMapping("/meal-plans")
    public ResponseEntity<ApiResponse<List<MealResponse>>> getMeals(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                "Meal plans retrieved successfully.", patientCarePlanService.getMeals(authentication.getName())));
    }

    @GetMapping("/medical-records")
    public ResponseEntity<ApiResponse<List<DoctorExaminationResponse>>> getMedicalRecords(
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                "Patient medical records retrieved successfully.",
                clinicalMedicationService.getOwnMedicalRecordHistory(authentication.getName())));
    }

    @PostMapping("/meal-plans")
    public ResponseEntity<ApiResponse<MealResponse>> createMeal(
            Authentication authentication, @Valid @RequestBody MealRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                "Meal plan created successfully.",
                patientCarePlanService.createMeal(authentication.getName(), request)));
    }

    @PatchMapping("/meal-plans/{mealId}/time")
    public ResponseEntity<ApiResponse<MealResponse>> updateMealTime(
            Authentication authentication,
            @PathVariable String mealId,
            @Valid @RequestBody UpdateMedicineScheduleTimeRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Meal plan time updated successfully.",
                patientCarePlanService.updateMealTime(authentication.getName(), mealId, request)));
    }

    @PatchMapping("/meal-plans/{mealId}/complete")
    public ResponseEntity<ApiResponse<MealResponse>> completeMeal(
            Authentication authentication, @PathVariable String mealId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Meal plan completed successfully.",
                patientCarePlanService.completeMeal(authentication.getName(), mealId)));
    }

    @GetMapping("/workout-plans")
    public ResponseEntity<ApiResponse<List<WorkoutResponse>>> getWorkouts(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                "Workout plans retrieved successfully.", patientCarePlanService.getWorkouts(authentication.getName())));
    }

    @PostMapping("/workout-plans")
    public ResponseEntity<ApiResponse<WorkoutResponse>> createWorkout(
            Authentication authentication, @Valid @RequestBody WorkoutRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                "Workout plan created successfully.",
                patientCarePlanService.createWorkout(authentication.getName(), request)));
    }

    @PatchMapping("/workout-plans/{workoutId}/time")
    public ResponseEntity<ApiResponse<WorkoutResponse>> updateWorkoutTime(
            Authentication authentication,
            @PathVariable String workoutId,
            @Valid @RequestBody UpdateMedicineScheduleTimeRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Workout plan time updated successfully.",
                patientCarePlanService.updateWorkoutTime(authentication.getName(), workoutId, request)));
    }

    @PatchMapping("/workout-plans/{workoutId}/complete")
    public ResponseEntity<ApiResponse<WorkoutResponse>> completeWorkout(
            Authentication authentication, @PathVariable String workoutId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Workout plan completed successfully.",
                patientCarePlanService.completeWorkout(authentication.getName(), workoutId)));
    }
}
