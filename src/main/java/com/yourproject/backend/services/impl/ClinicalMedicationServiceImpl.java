package com.yourproject.backend.services.impl;

import java.time.Instant;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yourproject.backend.dtos.requests.MedicineScheduleRequest;
import com.yourproject.backend.dtos.requests.MealRequest;
import com.yourproject.backend.dtos.requests.UpdateClinicalInformationRequest;
import com.yourproject.backend.dtos.requests.UpdateDiagnosisRequest;
import com.yourproject.backend.dtos.requests.UpdateMedicineScheduleTimeRequest;
import com.yourproject.backend.dtos.requests.UpsertPrescriptionRequest;
import com.yourproject.backend.dtos.requests.WorkoutRequest;
import com.yourproject.backend.dtos.responses.AppointmentResponse;
import com.yourproject.backend.dtos.responses.DoctorExaminationResponse;
import com.yourproject.backend.dtos.responses.MedicineScheduleResponse;
import com.yourproject.backend.dtos.responses.MedicalRecordResponse;
import com.yourproject.backend.dtos.responses.PrescriptionResponse;
import com.yourproject.backend.dtos.responses.UserResponse;
import com.yourproject.backend.dtos.responses.UserSummaryResponse;
import com.yourproject.backend.exceptions.BadRequestException;
import com.yourproject.backend.exceptions.ConflictException;
import com.yourproject.backend.exceptions.ForbiddenException;
import com.yourproject.backend.exceptions.ResourceNotFoundException;
import com.yourproject.backend.models.Appointment;
import com.yourproject.backend.models.AppointmentStatus;
import com.yourproject.backend.models.DoctorWorkSlot;
import com.yourproject.backend.models.DoctorWorkSlotStatus;
import com.yourproject.backend.models.MedicineSchedule;
import com.yourproject.backend.models.MedicineScheduleStatus;
import com.yourproject.backend.models.Meal;
import com.yourproject.backend.models.Dish;
import com.yourproject.backend.models.MedicalRecord;
import com.yourproject.backend.models.PlanScheduleStatus;
import com.yourproject.backend.models.Prescription;
import com.yourproject.backend.models.User;
import com.yourproject.backend.models.UserRole;
import com.yourproject.backend.models.Workout;
import com.yourproject.backend.repositories.AppointmentRepository;
import com.yourproject.backend.repositories.DoctorWorkSlotRepository;
import com.yourproject.backend.repositories.MedicineScheduleRepository;
import com.yourproject.backend.repositories.MealRepository;
import com.yourproject.backend.repositories.DishRepository;
import com.yourproject.backend.repositories.MedicalRecordRepository;
import com.yourproject.backend.repositories.PrescriptionRepository;
import com.yourproject.backend.repositories.UserRepository;
import com.yourproject.backend.repositories.WorkoutRepository;
import com.yourproject.backend.services.AppointmentService;
import com.yourproject.backend.services.ClinicalMedicationService;
import com.yourproject.backend.services.PatientDataProtectionService;
import com.yourproject.backend.services.UserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ClinicalMedicationServiceImpl implements ClinicalMedicationService {
    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final AppointmentRepository appointmentRepository;
    private final DoctorWorkSlotRepository doctorWorkSlotRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final MedicineScheduleRepository medicineScheduleRepository;
    private final MealRepository mealRepository;
    private final DishRepository dishRepository;
    private final WorkoutRepository workoutRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final AppointmentService appointmentService;
    private final PatientDataProtectionService patientDataProtectionService;

    @Override
    public List<AppointmentResponse> getDoctorAppointments(String doctorId, AppointmentStatus status) {
        requireDoctor(doctorId);
        List<String> workSlotIds = doctorWorkSlotRepository.findAllByDoctorIdOrderByWorkDateDescSlotIdAsc(doctorId)
                .stream().map(DoctorWorkSlot::getId).toList();
        if (workSlotIds.isEmpty()) return List.of();
        List<Appointment> appointments = appointmentRepository
                .findAllByDoctorWorkSlotIdInOrderByRequestedAtDesc(workSlotIds).stream()
                .filter(appointment -> status == null || appointment.getStatus() == status)
                .toList();
        return appointmentService.toResponses(appointments);
    }

    @Override
    public DoctorExaminationResponse getDoctorExamination(String doctorId, String appointmentId) {
        Appointment appointment = requireDoctorAppointment(doctorId, appointmentId);
        return toExaminationResponse(appointment);
    }

    @Override
    public List<UserSummaryResponse> getDoctorPatients(String doctorId) {
        requireDoctor(doctorId);
        LinkedHashMap<String, UserSummaryResponse> patients = new LinkedHashMap<>();
        for (Appointment appointment : findDoctorAppointments(doctorId)) {
            User patient = requirePatientForAppointment(appointment);
            patients.putIfAbsent(
                    patient.getId(),
                    UserSummaryResponse.from(patient, patientDataProtectionService));
        }
        return List.copyOf(patients.values());
    }

    @Override
    public List<DoctorExaminationResponse> getPatientMedicalRecordHistory(String doctorId, String patientId) {
        requireDoctor(doctorId);
        requirePatient(patientId);
        return appointmentRepository.findAllForPatient(patientId).stream()
                .map(this::toExaminationResponse)
                .toList();
    }

    @Override
    @Transactional
    public DoctorExaminationResponse startExamination(String doctorId, String appointmentId) {
        Appointment appointment = requireDoctorAppointment(doctorId, appointmentId);
        if (appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            throw new ConflictException("Only confirmed appointments can be started.");
        }
        DoctorWorkSlot slot = requireOwnedWorkSlot(doctorId, appointment);
        if (slot.getStatus() != DoctorWorkSlotStatus.BOOKED) {
            throw new ConflictException("Doctor work slot is not booked.");
        }
        appointment.setStatus(AppointmentStatus.IN_PROGRESS);
        appointment.setUpdatedAt(Instant.now());
        slot.setStatus(DoctorWorkSlotStatus.IN_PROGRESS);
        slot.setUpdatedAt(Instant.now());
        doctorWorkSlotRepository.save(slot);
        return toExaminationResponse(appointmentRepository.save(appointment));
    }

    @Override
    @Transactional
    public DoctorExaminationResponse updateClinicalInformation(
            String doctorId, String appointmentId, UpdateClinicalInformationRequest request) {
        Appointment appointment = requireMutableExamination(doctorId, appointmentId);
        User patient = requirePatientForAppointment(appointment);
        patientDataProtectionService.decryptPatientFields(patient);
        setIfPresent(request.getMedicalHistory(), patient::setMedicalHistory);
        setIfPresent(request.getCurrentSickness(), patient::setCurrentSickness);
        if (request.getHeight() != null) patient.setHeight(request.getHeight());
        if (request.getWeight() != null) patient.setWeight(request.getWeight());
        setIfPresent(request.getBloodType(), patient::setBloodType);
        patient.setUpdatedAt(Instant.now());
        patientDataProtectionService.encryptPatientFields(patient);
        userRepository.save(patient);

        MedicalRecord medicalRecord = getOrCreateMedicalRecord(appointmentId);
        setIfPresent(request.getNote(), medicalRecord::setNote);
        setIfPresent(request.getBloodPressure(), medicalRecord::setBloodPressure);
        if (request.getHeartRate() != null) medicalRecord.setHeartRate(request.getHeartRate());
        if (request.getBreathingRate() != null) medicalRecord.setBreathingRate(request.getBreathingRate());
        if (request.getBodyTemperature() != null) medicalRecord.setBodyTemperature(request.getBodyTemperature());
        if (request.getBloodLipids() != null) medicalRecord.setBloodLipids(request.getBloodLipids());
        medicalRecordRepository.save(medicalRecord);
        appointment.setUpdatedAt(Instant.now());
        appointmentRepository.save(appointment);
        return toExaminationResponse(appointment);
    }

    @Override
    public DoctorExaminationResponse updateDiagnosis(
            String doctorId, String appointmentId, UpdateDiagnosisRequest request) {
        Appointment appointment = requireMutableExamination(doctorId, appointmentId);
        MedicalRecord medicalRecord = getOrCreateMedicalRecord(appointmentId);
        medicalRecord.setDiagnosis(request.getDiagnosis().trim());
        medicalRecordRepository.save(medicalRecord);
        appointment.setUpdatedAt(Instant.now());
        return toExaminationResponse(appointmentRepository.save(appointment));
    }

    @Override
    @Transactional
    public PrescriptionResponse createPrescription(
            String doctorId, String appointmentId, UpsertPrescriptionRequest request) {
        Appointment appointment = requireMutableExamination(doctorId, appointmentId);
        String patientId = resolvePatientId(appointment);
        MedicalRecord medicalRecord = medicalRecordRepository.save(getOrCreateMedicalRecord(appointmentId));
        validateSchedules(request.getMedicineSchedules());
        validateMeals(request.getMeals());
        validateWorkouts(request.getWorkouts());
        Prescription prescription = prescriptionRepository.save(Prescription.builder()
                .medicalRecordId(medicalRecord.getId())
                .content(trimToNull(request.getContent()))
                .build());
        List<MedicineSchedule> schedules = saveSchedules(prescription.getId(), request.getMedicineSchedules());
        List<Meal> meals = saveMeals(patientId, prescription.getId(), request.getMeals());
        List<Dish> dishes = findDishes(meals);
        List<Workout> workouts = saveWorkouts(patientId, prescription.getId(), request.getWorkouts());
        return PrescriptionResponse.from(prescription, schedules, meals, dishes, workouts);
    }

    @Override
    @Transactional
    public PrescriptionResponse updatePrescription(
            String doctorId,
            String appointmentId,
            String prescriptionId,
            UpsertPrescriptionRequest request) {
        Appointment appointment = requireMutableExamination(doctorId, appointmentId);
        String patientId = resolvePatientId(appointment);
        Prescription prescription = prescriptionRepository.findById(prescriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Prescription was not found."));
        MedicalRecord medicalRecord = medicalRecordRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Medical record was not found."));
        if (!medicalRecord.getId().equals(prescription.getMedicalRecordId())) {
            throw new ForbiddenException("Prescription does not belong to this appointment.");
        }
        validateSchedules(request.getMedicineSchedules());
        validateMeals(request.getMeals());
        validateWorkouts(request.getWorkouts());
        prescription.setContent(trimToNull(request.getContent()));
        prescriptionRepository.save(prescription);
        medicineScheduleRepository.deleteAllByPrescriptionId(prescriptionId);
        List<Meal> previousMeals = mealRepository.findAllByPrescriptionIdInOrderByScheduledAtAsc(List.of(prescriptionId));
        dishRepository.deleteAllByMealIdIn(previousMeals.stream().map(Meal::getId).toList());
        mealRepository.deleteAllByPrescriptionId(prescriptionId);
        workoutRepository.deleteAllByPrescriptionId(prescriptionId);
        List<MedicineSchedule> schedules = saveSchedules(prescriptionId, request.getMedicineSchedules());
        List<Meal> meals = saveMeals(patientId, prescriptionId, request.getMeals());
        List<Dish> dishes = findDishes(meals);
        List<Workout> workouts = saveWorkouts(patientId, prescriptionId, request.getWorkouts());
        return PrescriptionResponse.from(prescription, schedules, meals, dishes, workouts);
    }

    @Override
    @Transactional
    public DoctorExaminationResponse completeExamination(String doctorId, String appointmentId) {
        Appointment appointment = requireMutableExamination(doctorId, appointmentId);
        MedicalRecord medicalRecord = medicalRecordRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new BadRequestException("Medical record is required before completing an examination."));
        if (trimToNull(medicalRecord.getDiagnosis()) == null) {
            throw new BadRequestException("Diagnosis is required before completing an examination.");
        }
        if (!hasClinicalMeasurements(medicalRecord)) {
            throw new BadRequestException("Clinical vital signs are required before completing an examination.");
        }
        List<Prescription> prescriptions = prescriptionRepository
                .findAllByMedicalRecordIdOrderByIdAsc(medicalRecord.getId());
        if (prescriptions.isEmpty()) {
            throw new BadRequestException("At least one prescription is required before completing an examination.");
        }
        DoctorWorkSlot slot = requireOwnedWorkSlot(doctorId, appointment);
        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointment.setUpdatedAt(Instant.now());
        slot.setStatus(DoctorWorkSlotStatus.CLOSED);
        slot.setUpdatedAt(Instant.now());
        doctorWorkSlotRepository.save(slot);
        return toExaminationResponse(appointmentRepository.save(appointment));
    }

    @Override
    public List<MedicineScheduleResponse> getPatientMedicineSchedules(String patientId) {
        requirePatient(patientId);
        List<String> appointmentIds = appointmentRepository.findAllForPatient(patientId).stream()
                .filter(appointment -> appointment.getStatus() == AppointmentStatus.COMPLETED)
                .map(Appointment::getId).toList();
        if (appointmentIds.isEmpty()) return List.of();
        List<String> medicalRecordIds = medicalRecordRepository.findAllByAppointmentIdIn(appointmentIds).stream()
                .map(MedicalRecord::getId).toList();
        if (medicalRecordIds.isEmpty()) return List.of();
        List<String> prescriptionIds = prescriptionRepository.findAllByMedicalRecordIdIn(medicalRecordIds).stream()
                .map(Prescription::getId).toList();
        if (prescriptionIds.isEmpty()) return List.of();
        return medicineScheduleRepository.findAllByPrescriptionIdInOrderByScheduledAtAsc(prescriptionIds)
                .stream().map(MedicineScheduleResponse::from).toList();
    }

    @Override
    public MedicineScheduleResponse updatePatientScheduleTime(
            String patientId, String scheduleId, UpdateMedicineScheduleTimeRequest request) {
        MedicineSchedule schedule = requirePatientSchedule(patientId, scheduleId);
        if (schedule.getStatus() != MedicineScheduleStatus.NOT_YET) {
            throw new ConflictException("Only medicine schedules that are not yet taken can be rescheduled.");
        }
        if (!request.getScheduledAt().isAfter(Instant.now())) {
            throw new BadRequestException("Medicine schedule time must be in the future.");
        }
        if (!schedule.getScheduledAt().atZone(VIETNAM_ZONE).toLocalDate()
                .equals(request.getScheduledAt().atZone(VIETNAM_ZONE).toLocalDate())) {
            throw new BadRequestException("Medicine schedule time must remain on the same calendar day.");
        }
        if (medicineScheduleRepository.existsByPrescriptionIdAndMedicineNameAndDosageAndScheduledAt(
                schedule.getPrescriptionId(), schedule.getMedicineName(), schedule.getDosage(), request.getScheduledAt())) {
            throw new ConflictException("The same medicine schedule already exists at the selected time.");
        }
        schedule.setScheduledAt(request.getScheduledAt());
        return MedicineScheduleResponse.from(medicineScheduleRepository.save(schedule));
    }

    @Override
    public MedicineScheduleResponse markMedicineTaken(String patientId, String scheduleId) {
        MedicineSchedule schedule = requirePatientSchedule(patientId, scheduleId);
        if (schedule.getStatus() != MedicineScheduleStatus.NOT_YET) {
            throw new ConflictException("Only medicine schedules with NOT_YET status can be marked as taken.");
        }
        schedule.setStatus(MedicineScheduleStatus.TAKEN);
        return MedicineScheduleResponse.from(medicineScheduleRepository.save(schedule));
    }

    private DoctorExaminationResponse toExaminationResponse(Appointment appointment) {
        User patient = requirePatientForAppointment(appointment);
        MedicalRecord medicalRecord = medicalRecordRepository.findByAppointmentId(appointment.getId()).orElse(null);
        List<Prescription> prescriptions = medicalRecord == null
                ? List.of()
                : prescriptionRepository.findAllByMedicalRecordIdOrderByIdAsc(medicalRecord.getId());
        var schedulesByPrescription = medicineScheduleRepository
                .findAllByPrescriptionIdInOrderByScheduledAtAsc(prescriptions.stream().map(Prescription::getId).toList())
                .stream().collect(Collectors.groupingBy(MedicineSchedule::getPrescriptionId));
        List<String> prescriptionIds = prescriptions.stream().map(Prescription::getId).toList();
        var mealsByPrescription = mealRepository.findAllByPrescriptionIdInOrderByScheduledAtAsc(prescriptionIds)
                .stream().collect(Collectors.groupingBy(Meal::getPrescriptionId));
        List<Dish> allDishes = dishRepository.findAllByMealIdIn(mealsByPrescription.values().stream()
                .flatMap(List::stream).map(Meal::getId).toList());
        var workoutsByPrescription = workoutRepository.findAllByPrescriptionIdInOrderByScheduledAtAsc(prescriptionIds)
                .stream().collect(Collectors.groupingBy(Workout::getPrescriptionId));
        return DoctorExaminationResponse.builder()
                .appointment(appointmentService.toResponse(appointment))
                .patient(UserResponse.from(patient, patientDataProtectionService))
                .medicalRecord(MedicalRecordResponse.from(medicalRecord))
                .prescriptions(prescriptions.stream()
                        .map(prescription -> PrescriptionResponse.from(
                                prescription,
                                schedulesByPrescription.getOrDefault(prescription.getId(), List.of()),
                                mealsByPrescription.getOrDefault(prescription.getId(), List.of()),
                                allDishes,
                                workoutsByPrescription.getOrDefault(prescription.getId(), List.of())))
                        .toList())
                .build();
    }

    private Appointment requireMutableExamination(String doctorId, String appointmentId) {
        Appointment appointment = requireDoctorAppointment(doctorId, appointmentId);
        if (appointment.getStatus() != AppointmentStatus.IN_PROGRESS) {
            throw new ConflictException("Examination information can only be changed while the appointment is in progress.");
        }
        return appointment;
    }

    private Appointment requireDoctorAppointment(String doctorId, String appointmentId) {
        requireDoctor(doctorId);
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment was not found."));
        requireOwnedWorkSlot(doctorId, appointment);
        return appointment;
    }

    private DoctorWorkSlot requireOwnedWorkSlot(String doctorId, Appointment appointment) {
        DoctorWorkSlot slot = doctorWorkSlotRepository.findById(appointment.getDoctorWorkSlotId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor work slot was not found."));
        if (!doctorId.equals(slot.getDoctorId())) {
            throw new ForbiddenException("Doctors can only access their own appointments.");
        }
        return slot;
    }

    private User requirePatientForAppointment(Appointment appointment) {
        String patientId = resolvePatientId(appointment);
        if (patientId == null) throw new ResourceNotFoundException("Appointment patient was not found.");
        return userService.getActiveUserById(patientId);
    }

    private List<Appointment> findDoctorAppointments(String doctorId) {
        List<String> workSlotIds = doctorWorkSlotRepository.findAllByDoctorIdOrderByWorkDateDescSlotIdAsc(doctorId)
                .stream().map(DoctorWorkSlot::getId).toList();
        return workSlotIds.isEmpty()
                ? List.of()
                : appointmentRepository.findAllByDoctorWorkSlotIdInOrderByRequestedAtDesc(workSlotIds);
    }

    private String resolvePatientId(Appointment appointment) {
        return trimToNull(appointment.getPatientId()) != null
                ? appointment.getPatientId()
                : appointment.getPatientUserId();
    }

    private MedicineSchedule requirePatientSchedule(String patientId, String scheduleId) {
        requirePatient(patientId);
        MedicineSchedule schedule = medicineScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Medicine schedule was not found."));
        Prescription prescription = prescriptionRepository.findById(schedule.getPrescriptionId())
                .orElseThrow(() -> new ResourceNotFoundException("Prescription was not found."));
        MedicalRecord medicalRecord = medicalRecordRepository.findById(prescription.getMedicalRecordId())
                .orElseThrow(() -> new ResourceNotFoundException("Medical record was not found."));
        Appointment appointment = appointmentRepository.findById(medicalRecord.getAppointmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Appointment was not found."));
        String ownerId = trimToNull(appointment.getPatientId()) != null
                ? appointment.getPatientId()
                : appointment.getPatientUserId();
        if (!patientId.equals(ownerId)) {
            throw new ForbiddenException("Patients can only access their own medicine schedules.");
        }
        if (appointment.getStatus() != AppointmentStatus.COMPLETED) {
            throw new ConflictException("Medicine schedules are available after the examination is completed.");
        }
        return schedule;
    }

    private List<MedicineSchedule> saveSchedules(String prescriptionId, List<MedicineScheduleRequest> requests) {
        return medicineScheduleRepository.saveAll(requests.stream()
                .map(request -> MedicineSchedule.builder()
                        .prescriptionId(prescriptionId)
                        .medicineName(request.getMedicineName().trim())
                        .dosage(request.getDosage().trim())
                        .scheduledAt(request.getScheduledAt())
                        .status(MedicineScheduleStatus.NOT_YET)
                        .note(trimToNull(request.getNote()))
                        .build())
                .toList());
    }

    private List<Meal> saveMeals(String patientId, String prescriptionId, List<MealRequest> requests) {
        if (requests == null || requests.isEmpty()) return List.of();
        List<Meal> meals = mealRepository.saveAll(requests.stream().map(request -> Meal.builder()
                .userId(patientId).prescriptionId(prescriptionId).mealName(request.getMealName().trim())
                .scheduledAt(request.getScheduledAt()).status(PlanScheduleStatus.NOT_YET)
                .note(trimToNull(request.getNote())).build()).toList());
        for (int index = 0; index < meals.size(); index++) {
            saveDishes(meals.get(index).getId(), requests.get(index).getDishes());
        }
        return meals;
    }

    private void saveDishes(String mealId, List<com.yourproject.backend.dtos.requests.DishRequest> requests) {
        dishRepository.saveAll(requests.stream().map(request -> Dish.builder().mealId(mealId)
                .dishName(request.getDishName().trim()).quantity(request.getQuantity())
                .unit(trimToNull(request.getUnit())).totalCalories(request.getTotalCalories())
                .totalProtein(request.getTotalProtein()).totalCarbohydrates(request.getTotalCarbohydrates())
                .totalFat(request.getTotalFat()).build()).toList());
    }

    private List<Dish> findDishes(List<Meal> meals) {
        if (meals.isEmpty()) return List.of();
        return dishRepository.findAllByMealIdIn(meals.stream().map(Meal::getId).toList());
    }

    private List<Workout> saveWorkouts(String patientId, String prescriptionId, List<WorkoutRequest> requests) {
        if (requests == null || requests.isEmpty()) return List.of();
        return workoutRepository.saveAll(requests.stream().map(request -> Workout.builder()
                .userId(patientId).prescriptionId(prescriptionId).workoutName(request.getWorkoutName().trim())
                .content(trimToNull(request.getContent())).scheduledAt(request.getScheduledAt())
                .status(PlanScheduleStatus.NOT_YET).note(trimToNull(request.getNote())).build()).toList());
    }

    private void validateSchedules(List<MedicineScheduleRequest> requests) {
        Set<String> uniqueSchedules = new HashSet<>();
        for (MedicineScheduleRequest request : requests) {
            if (!request.getScheduledAt().isAfter(Instant.now())) {
                throw new BadRequestException("Medicine schedule time must be in the future.");
            }
            String key = request.getMedicineName().trim().toLowerCase()
                    + "|" + request.getDosage().trim().toLowerCase()
                    + "|" + request.getScheduledAt();
            if (!uniqueSchedules.add(key)) {
                throw new ConflictException("Duplicate medicine schedules are not allowed at the same time.");
            }
        }
    }

    private void validateMeals(List<MealRequest> requests) {
        if (requests == null) return;
        Set<String> unique = new HashSet<>();
        for (MealRequest request : requests) {
            validateFuturePlanTime(request.getScheduledAt());
            String key = request.getMealName().trim().toLowerCase() + "|" + request.getScheduledAt();
            if (!unique.add(key)) throw new ConflictException("Duplicate meals are not allowed at the same time.");
        }
    }

    private void validateWorkouts(List<WorkoutRequest> requests) {
        if (requests == null) return;
        Set<String> unique = new HashSet<>();
        for (WorkoutRequest request : requests) {
            validateFuturePlanTime(request.getScheduledAt());
            String key = request.getWorkoutName().trim().toLowerCase() + "|" + request.getScheduledAt();
            if (!unique.add(key)) throw new ConflictException("Duplicate workouts are not allowed at the same time.");
        }
    }

    private void validateFuturePlanTime(Instant scheduledAt) {
        if (!scheduledAt.isAfter(Instant.now())) {
            throw new BadRequestException("Care plan time must be in the future.");
        }
    }

    private void requireDoctor(String doctorId) {
        User doctor = userService.getActiveUserById(doctorId);
        if (doctor.getRole() != UserRole.DOCTOR) {
            throw new ForbiddenException("Only doctors can access examination operations.");
        }
    }

    private void requirePatient(String patientId) {
        User patient = userService.getActiveUserById(patientId);
        if (patient.getRole() != UserRole.PATIENT) {
            throw new ForbiddenException("Only patients can access medicine schedules.");
        }
    }

    private MedicalRecord getOrCreateMedicalRecord(String appointmentId) {
        return medicalRecordRepository.findByAppointmentId(appointmentId)
                .orElseGet(() -> MedicalRecord.builder().appointmentId(appointmentId).build());
    }

    private boolean hasClinicalMeasurements(MedicalRecord medicalRecord) {
        return trimToNull(medicalRecord.getBloodPressure()) != null
                || medicalRecord.getHeartRate() != null
                || medicalRecord.getBreathingRate() != null
                || medicalRecord.getBodyTemperature() != null
                || medicalRecord.getBloodLipids() != null;
    }

    private void setIfPresent(String value, Consumer<String> setter) {
        if (value != null) setter.accept(trimToNull(value));
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
