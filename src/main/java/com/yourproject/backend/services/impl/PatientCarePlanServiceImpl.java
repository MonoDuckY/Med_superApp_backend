package com.yourproject.backend.services.impl;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import org.springframework.stereotype.Service;

import com.yourproject.backend.dtos.requests.MealRequest;
import com.yourproject.backend.dtos.requests.UpdateMedicineScheduleTimeRequest;
import com.yourproject.backend.dtos.requests.WorkoutRequest;
import com.yourproject.backend.dtos.responses.MealResponse;
import com.yourproject.backend.dtos.responses.WorkoutResponse;
import com.yourproject.backend.exceptions.BadRequestException;
import com.yourproject.backend.exceptions.ConflictException;
import com.yourproject.backend.exceptions.ForbiddenException;
import com.yourproject.backend.exceptions.ResourceNotFoundException;
import com.yourproject.backend.models.Meal;
import com.yourproject.backend.models.Dish;
import com.yourproject.backend.models.AppointmentStatus;
import com.yourproject.backend.models.PlanScheduleStatus;
import com.yourproject.backend.models.UserRole;
import com.yourproject.backend.models.Workout;
import com.yourproject.backend.repositories.MealRepository;
import com.yourproject.backend.repositories.DishRepository;
import com.yourproject.backend.repositories.AppointmentRepository;
import com.yourproject.backend.repositories.MedicalRecordRepository;
import com.yourproject.backend.repositories.PrescriptionRepository;
import com.yourproject.backend.repositories.WorkoutRepository;
import com.yourproject.backend.services.PatientCarePlanService;
import com.yourproject.backend.services.UserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PatientCarePlanServiceImpl implements PatientCarePlanService {
    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final MealRepository mealRepository;
    private final DishRepository dishRepository;
    private final WorkoutRepository workoutRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final AppointmentRepository appointmentRepository;
    private final UserService userService;

    @Override
    public List<MealResponse> getMeals(String patientId) {
        requirePatient(patientId);
        List<Meal> meals = mealRepository.findAllByUserIdOrderByScheduledAtAsc(patientId).stream()
                .filter(meal -> isAvailableToPatient(meal.getPrescriptionId()))
                .toList();
        var dishesByMeal = dishRepository.findAllByMealIdIn(meals.stream().map(Meal::getId).toList()).stream()
                .collect(java.util.stream.Collectors.groupingBy(Dish::getMealId));
        return meals.stream().map(meal -> MealResponse.from(
                meal, dishesByMeal.getOrDefault(meal.getId(), List.of()))).toList();
    }

    @Override
    public MealResponse createMeal(String patientId, MealRequest request) {
        requirePatient(patientId);
        validatePatientCreatedPlanTime(request.getScheduledAt());
        String name = request.getMealName().trim();
        if (mealRepository.existsByUserIdAndMealNameAndScheduledAt(patientId, name, request.getScheduledAt())) {
            throw new ConflictException("The same meal already exists at the selected time.");
        }
        Meal meal = mealRepository.save(Meal.builder().userId(patientId).prescriptionId(null)
                .mealName(name).scheduledAt(request.getScheduledAt()).status(PlanScheduleStatus.NOT_YET)
                .note(trimToNull(request.getNote())).build());
        List<Dish> dishes = saveDishes(meal.getId(), request.getDishes());
        return MealResponse.from(meal, dishes);
    }

    @Override
    public MealResponse updateMealTime(String patientId, String mealId, UpdateMedicineScheduleTimeRequest request) {
        Meal meal = requireOwnedMeal(patientId, mealId);
        requireMutable(meal.getStatus());
        validateReschedule(meal.getScheduledAt(), request.getScheduledAt());
        if (mealRepository.existsByUserIdAndMealNameAndScheduledAt(patientId, meal.getMealName(), request.getScheduledAt())) {
            throw new ConflictException("The same meal already exists at the selected time.");
        }
        meal.setScheduledAt(request.getScheduledAt());
        return MealResponse.from(mealRepository.save(meal), dishRepository.findAllByMealIdOrderByIdAsc(mealId));
    }

    @Override
    public MealResponse completeMeal(String patientId, String mealId) {
        Meal meal = requireOwnedMeal(patientId, mealId);
        requireMutable(meal.getStatus());
        meal.setStatus(PlanScheduleStatus.COMPLETED);
        return MealResponse.from(mealRepository.save(meal), dishRepository.findAllByMealIdOrderByIdAsc(mealId));
    }

    @Override
    public List<WorkoutResponse> getWorkouts(String patientId) {
        requirePatient(patientId);
        return workoutRepository.findAllByUserIdOrderByScheduledAtAsc(patientId).stream()
                .filter(workout -> isAvailableToPatient(workout.getPrescriptionId()))
                .map(WorkoutResponse::from).toList();
    }

    @Override
    public WorkoutResponse createWorkout(String patientId, WorkoutRequest request) {
        requirePatient(patientId);
        validatePatientCreatedPlanTime(request.getScheduledAt());
        String name = request.getWorkoutName().trim();
        if (workoutRepository.existsByUserIdAndWorkoutNameAndScheduledAt(patientId, name, request.getScheduledAt())) {
            throw new ConflictException("The same workout already exists at the selected time.");
        }
        return WorkoutResponse.from(workoutRepository.save(Workout.builder().userId(patientId).prescriptionId(null)
                .workoutName(name).content(trimToNull(request.getContent())).scheduledAt(request.getScheduledAt())
                .status(PlanScheduleStatus.NOT_YET).note(trimToNull(request.getNote())).build()));
    }

    @Override
    public WorkoutResponse updateWorkoutTime(
            String patientId, String workoutId, UpdateMedicineScheduleTimeRequest request) {
        Workout workout = requireOwnedWorkout(patientId, workoutId);
        requireMutable(workout.getStatus());
        validateReschedule(workout.getScheduledAt(), request.getScheduledAt());
        if (workoutRepository.existsByUserIdAndWorkoutNameAndScheduledAt(
                patientId, workout.getWorkoutName(), request.getScheduledAt())) {
            throw new ConflictException("The same workout already exists at the selected time.");
        }
        workout.setScheduledAt(request.getScheduledAt());
        return WorkoutResponse.from(workoutRepository.save(workout));
    }

    @Override
    public WorkoutResponse completeWorkout(String patientId, String workoutId) {
        Workout workout = requireOwnedWorkout(patientId, workoutId);
        requireMutable(workout.getStatus());
        workout.setStatus(PlanScheduleStatus.COMPLETED);
        return WorkoutResponse.from(workoutRepository.save(workout));
    }

    private Meal requireOwnedMeal(String patientId, String mealId) {
        requirePatient(patientId);
        Meal meal = mealRepository.findById(mealId)
                .orElseThrow(() -> new ResourceNotFoundException("Meal was not found."));
        if (!patientId.equals(meal.getUserId())) throw new ForbiddenException("Patients can only access their own meals.");
        requireAvailableToPatient(meal.getPrescriptionId());
        return meal;
    }

    private Workout requireOwnedWorkout(String patientId, String workoutId) {
        requirePatient(patientId);
        Workout workout = workoutRepository.findById(workoutId)
                .orElseThrow(() -> new ResourceNotFoundException("Workout was not found."));
        if (!patientId.equals(workout.getUserId())) throw new ForbiddenException("Patients can only access their own workouts.");
        requireAvailableToPatient(workout.getPrescriptionId());
        return workout;
    }

    private boolean isAvailableToPatient(String prescriptionId) {
        if (prescriptionId == null) return true;
        return prescriptionRepository.findById(prescriptionId)
                .flatMap(prescription -> medicalRecordRepository.findById(prescription.getMedicalRecordId()))
                .flatMap(record -> appointmentRepository.findById(record.getAppointmentId()))
                .map(appointment -> appointment.getStatus() == AppointmentStatus.COMPLETED)
                .orElse(false);
    }

    private void requireAvailableToPatient(String prescriptionId) {
        if (!isAvailableToPatient(prescriptionId)) {
            throw new ConflictException("Doctor-created care plans are available after the examination is completed.");
        }
    }

    private void requirePatient(String patientId) {
        if (userService.getActiveUserById(patientId).getRole() != UserRole.PATIENT) {
            throw new ForbiddenException("Only patients can access care plans.");
        }
    }

    private void requireMutable(PlanScheduleStatus status) {
        if (status != PlanScheduleStatus.NOT_YET) {
            throw new ConflictException("Only plans with NOT_YET status can be changed.");
        }
    }

    private void validateReschedule(Instant original, Instant requested) {
        validateFuture(requested);
        if (!original.atZone(VIETNAM_ZONE).toLocalDate().equals(requested.atZone(VIETNAM_ZONE).toLocalDate())) {
            throw new BadRequestException("Plan time must remain on the same calendar day.");
        }
    }

    private void validateFuture(Instant time) {
        if (!time.isAfter(Instant.now())) throw new BadRequestException("Plan time must be in the future.");
    }

    private void validatePatientCreatedPlanTime(Instant time) {
        validateFuture(time);
        if (!time.atZone(VIETNAM_ZONE).toLocalDate()
                .equals(Instant.now().atZone(VIETNAM_ZONE).toLocalDate())) {
            throw new BadRequestException("Patient-created care plan time must be within the current Vietnam calendar day.");
        }
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private List<Dish> saveDishes(String mealId, List<com.yourproject.backend.dtos.requests.DishRequest> requests) {
        return dishRepository.saveAll(requests.stream().map(request -> Dish.builder().mealId(mealId)
                .dishName(request.getDishName().trim()).quantity(request.getQuantity())
                .unit(trimToNull(request.getUnit())).totalCalories(request.getTotalCalories())
                .totalProtein(request.getTotalProtein()).totalCarbohydrates(request.getTotalCarbohydrates())
                .totalFat(request.getTotalFat()).build()).toList());
    }
}
