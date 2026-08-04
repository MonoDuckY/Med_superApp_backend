package com.yourproject.backend.services.impl;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yourproject.backend.dtos.requests.MedicineScheduleRequest;
import com.yourproject.backend.dtos.requests.UpdateClinicalInformationRequest;
import com.yourproject.backend.dtos.requests.UpdateDiagnosisRequest;
import com.yourproject.backend.dtos.requests.UpdateMedicineScheduleTimeRequest;
import com.yourproject.backend.dtos.requests.UpsertPrescriptionRequest;
import com.yourproject.backend.dtos.responses.AppointmentResponse;
import com.yourproject.backend.dtos.responses.DoctorExaminationResponse;
import com.yourproject.backend.dtos.responses.MedicineScheduleResponse;
import com.yourproject.backend.dtos.responses.PrescriptionResponse;
import com.yourproject.backend.dtos.responses.UserResponse;
import com.yourproject.backend.dtos.responses.VitalSignResponse;
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
import com.yourproject.backend.models.Prescription;
import com.yourproject.backend.models.User;
import com.yourproject.backend.models.UserRole;
import com.yourproject.backend.models.VitalSign;
import com.yourproject.backend.repositories.AppointmentRepository;
import com.yourproject.backend.repositories.DoctorWorkSlotRepository;
import com.yourproject.backend.repositories.MedicineScheduleRepository;
import com.yourproject.backend.repositories.PrescriptionRepository;
import com.yourproject.backend.repositories.UserRepository;
import com.yourproject.backend.repositories.VitalSignRepository;
import com.yourproject.backend.services.AppointmentService;
import com.yourproject.backend.services.ClinicalMedicationService;
import com.yourproject.backend.services.PatientDataProtectionService;
import com.yourproject.backend.services.UserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ClinicalMedicationServiceImpl implements ClinicalMedicationService {
    private final AppointmentRepository appointmentRepository;
    private final DoctorWorkSlotRepository doctorWorkSlotRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final MedicineScheduleRepository medicineScheduleRepository;
    private final VitalSignRepository vitalSignRepository;
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

        if (request.getVitalSigns() != null) {
            vitalSignRepository.deleteAllByAppointmentId(appointmentId);
            List<VitalSign> vitalSigns = request.getVitalSigns().stream()
                    .map(item -> VitalSign.builder()
                            .appointmentId(appointmentId)
                            .vitalName(item.getVitalName().trim())
                            .vitalNumber(item.getVitalNumber().trim())
                            .vitalUnit(trimToNull(item.getVitalUnit()))
                            .build())
                    .toList();
            vitalSignRepository.saveAll(vitalSigns);
        }
        appointment.setUpdatedAt(Instant.now());
        appointmentRepository.save(appointment);
        return toExaminationResponse(appointment);
    }

    @Override
    public DoctorExaminationResponse updateDiagnosis(
            String doctorId, String appointmentId, UpdateDiagnosisRequest request) {
        Appointment appointment = requireMutableExamination(doctorId, appointmentId);
        appointment.setDiagnosis(request.getDiagnosis().trim());
        appointment.setUpdatedAt(Instant.now());
        return toExaminationResponse(appointmentRepository.save(appointment));
    }

    @Override
    @Transactional
    public PrescriptionResponse createPrescription(
            String doctorId, String appointmentId, UpsertPrescriptionRequest request) {
        requireMutableExamination(doctorId, appointmentId);
        validateSchedules(request.getMedicineSchedules());
        Prescription prescription = prescriptionRepository.save(Prescription.builder()
                .appointmentId(appointmentId)
                .content(trimToNull(request.getContent()))
                .build());
        List<MedicineSchedule> schedules = saveSchedules(prescription.getId(), request.getMedicineSchedules());
        return PrescriptionResponse.from(prescription, schedules);
    }

    @Override
    @Transactional
    public PrescriptionResponse updatePrescription(
            String doctorId,
            String appointmentId,
            String prescriptionId,
            UpsertPrescriptionRequest request) {
        requireMutableExamination(doctorId, appointmentId);
        Prescription prescription = prescriptionRepository.findById(prescriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Prescription was not found."));
        if (!appointmentId.equals(prescription.getAppointmentId())) {
            throw new ForbiddenException("Prescription does not belong to this appointment.");
        }
        validateSchedules(request.getMedicineSchedules());
        prescription.setContent(trimToNull(request.getContent()));
        prescriptionRepository.save(prescription);
        medicineScheduleRepository.deleteAllByPrescriptionId(prescriptionId);
        List<MedicineSchedule> schedules = saveSchedules(prescriptionId, request.getMedicineSchedules());
        return PrescriptionResponse.from(prescription, schedules);
    }

    @Override
    @Transactional
    public DoctorExaminationResponse completeExamination(String doctorId, String appointmentId) {
        Appointment appointment = requireMutableExamination(doctorId, appointmentId);
        if (trimToNull(appointment.getDiagnosis()) == null) {
            throw new BadRequestException("Diagnosis is required before completing an examination.");
        }
        if (vitalSignRepository.findAllByAppointmentIdOrderByVitalNameAsc(appointmentId).isEmpty()) {
            throw new BadRequestException("Clinical vital signs are required before completing an examination.");
        }
        List<Prescription> prescriptions = prescriptionRepository.findAllByAppointmentIdOrderByIdAsc(appointmentId);
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
        List<String> prescriptionIds = prescriptionRepository.findAllByAppointmentIdIn(appointmentIds).stream()
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
        List<Prescription> prescriptions = prescriptionRepository.findAllByAppointmentIdOrderByIdAsc(appointment.getId());
        var schedulesByPrescription = medicineScheduleRepository
                .findAllByPrescriptionIdInOrderByScheduledAtAsc(prescriptions.stream().map(Prescription::getId).toList())
                .stream().collect(Collectors.groupingBy(MedicineSchedule::getPrescriptionId));
        return DoctorExaminationResponse.builder()
                .appointment(appointmentService.toResponse(appointment))
                .patient(UserResponse.from(patient, patientDataProtectionService))
                .vitalSigns(vitalSignRepository.findAllByAppointmentIdOrderByVitalNameAsc(appointment.getId())
                        .stream().map(VitalSignResponse::from).toList())
                .prescriptions(prescriptions.stream()
                        .map(prescription -> PrescriptionResponse.from(
                                prescription,
                                schedulesByPrescription.getOrDefault(prescription.getId(), List.of())))
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
        String patientId = trimToNull(appointment.getPatientId()) != null
                ? appointment.getPatientId()
                : appointment.getPatientUserId();
        if (patientId == null) throw new ResourceNotFoundException("Appointment patient was not found.");
        return userService.getActiveUserById(patientId);
    }

    private MedicineSchedule requirePatientSchedule(String patientId, String scheduleId) {
        requirePatient(patientId);
        MedicineSchedule schedule = medicineScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Medicine schedule was not found."));
        Prescription prescription = prescriptionRepository.findById(schedule.getPrescriptionId())
                .orElseThrow(() -> new ResourceNotFoundException("Prescription was not found."));
        Appointment appointment = appointmentRepository.findById(prescription.getAppointmentId())
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

    private void requireDoctor(String doctorId) {
        User doctor = userService.getActiveUserById(doctorId);
        if (!doctor.getRoles().contains(UserRole.DOCTOR)) {
            throw new ForbiddenException("Only doctors can access examination operations.");
        }
    }

    private void requirePatient(String patientId) {
        User patient = userService.getActiveUserById(patientId);
        if (!patient.getRoles().contains(UserRole.PATIENT)) {
            throw new ForbiddenException("Only patients can access medicine schedules.");
        }
    }

    private void setIfPresent(String value, Consumer<String> setter) {
        if (value != null) setter.accept(trimToNull(value));
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
