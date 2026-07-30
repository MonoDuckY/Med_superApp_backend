package com.yourproject.backend.services.impl;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yourproject.backend.dtos.requests.AppointmentDecisionRequest;
import com.yourproject.backend.dtos.requests.BookAppointmentRequest;
import com.yourproject.backend.dtos.requests.CancelAppointmentRequest;
import com.yourproject.backend.dtos.responses.AppointmentResponse;
import com.yourproject.backend.exceptions.BadRequestException;
import com.yourproject.backend.exceptions.ConflictException;
import com.yourproject.backend.exceptions.ForbiddenException;
import com.yourproject.backend.exceptions.ResourceNotFoundException;
import com.yourproject.backend.models.Appointment;
import com.yourproject.backend.models.AppointmentStatus;
import com.yourproject.backend.models.AccountStatus;
import com.yourproject.backend.models.DoctorWorkSlot;
import com.yourproject.backend.models.ScheduleDecision;
import com.yourproject.backend.models.User;
import com.yourproject.backend.models.UserRole;
import com.yourproject.backend.models.DoctorWorkSlotStatus;
import com.yourproject.backend.repositories.AppointmentRepository;
import com.yourproject.backend.repositories.DoctorWorkSlotRepository;
import com.yourproject.backend.repositories.UserRepository;
import com.yourproject.backend.repositories.WorkSlotRepository;
import com.yourproject.backend.repositories.ClinicRoomRepository;
import com.yourproject.backend.services.AppointmentService;
import com.yourproject.backend.services.PatientDataProtectionService;
import com.yourproject.backend.services.UserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AppointmentServiceImpl implements AppointmentService {
    private static final ZoneId HOSPITAL_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final Duration MINIMUM_BOOKING_LEAD = Duration.ofHours(12);
    private static final Duration MAXIMUM_BOOKING_LEAD = Duration.ofDays(30);

    private final AppointmentRepository appointmentRepository;
    private final DoctorWorkSlotRepository doctorWorkSlotRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final PatientDataProtectionService patientDataProtectionService;
    private final MongoTemplate mongoTemplate;
    private final WorkSlotRepository workSlotRepository;
    private final ClinicRoomRepository clinicRoomRepository;

    @Override
    public List<DoctorWorkSlot> getAvailableSlots(String patientUserId, LocalDate date, String doctorId) {
        requirePatient(patientUserId);
        Instant now = Instant.now();
        Instant from = now.plus(MINIMUM_BOOKING_LEAD);
        Instant to = now.plus(MAXIMUM_BOOKING_LEAD);
        if (date != null) {
            Instant dateStart = date.atStartOfDay(HOSPITAL_ZONE).toInstant();
            Instant dateEnd = date.plusDays(1).atStartOfDay(HOSPITAL_ZONE).toInstant();
            if (dateEnd.isBefore(from) || dateStart.isAfter(to)) {
                return List.of();
            }
            if (dateStart.isAfter(from)) {
                from = dateStart;
            }
            if (dateEnd.isBefore(to)) {
                to = dateEnd;
            }
        }

        List<DoctorWorkSlot> availableSlots = doctorWorkSlotRepository
                .findAllByStatusAndStartAtBetweenOrderByStartAtAsc(
                        DoctorWorkSlotStatus.AVAILABLE,
                        from,
                        to)
                .stream()
                .filter(slot -> doctorId == null || doctorId.isBlank() || slot.getDoctorId().equals(doctorId))
                .toList();
        Map<String, User> doctors = userRepository.findAllById(
                        availableSlots.stream().map(DoctorWorkSlot::getDoctorId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(User::getId, doctor -> doctor));
        return availableSlots.stream()
                .filter(slot -> {
                    User doctor = doctors.get(slot.getDoctorId());
                    return doctor != null
                            && doctor.getRoles().contains(UserRole.DOCTOR)
                            && doctor.getStatus() == AccountStatus.ACTIVE;
                })
                .toList();
    }

    @Override
    @Transactional
    public Appointment book(String patientUserId, BookAppointmentRequest request) {
        User patient = requirePatient(patientUserId);
        DoctorWorkSlot currentSlot = doctorWorkSlotRepository.findById(request.getDoctorWorkSlotId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor work slot was not found."));
        User doctor = userService.getActiveUserById(currentSlot.getDoctorId());
        if (!doctor.getRoles().contains(UserRole.DOCTOR)) {
            throw new ConflictException("The selected work slot does not belong to an active doctor.");
        }
        validateBookableSlot(currentSlot);
        boolean alreadyBookedThatDay = appointmentRepository.findAllForPatient(patient.getId()).stream()
                .filter(appointment -> appointment.getStatus() != AppointmentStatus.CANCELLED
                        && appointment.getStatus() != AppointmentStatus.REJECTED)
                .map(appointment -> doctorWorkSlotRepository.findById(appointment.getDoctorWorkSlotId()).orElse(null))
                .anyMatch(slot -> slot != null && slot.getWorkDate().equals(currentSlot.getWorkDate()));
        if (alreadyBookedThatDay) {
            throw new ConflictException("A patient can only have one active appointment per day.");
        }

        Instant now = Instant.now();
        Query claimQuery = Query.query(Criteria.where("_id").is(currentSlot.getId())
                .and("status").is(DoctorWorkSlotStatus.AVAILABLE));
        Update claimUpdate = new Update()
                .set("status", DoctorWorkSlotStatus.SCHEDULING)
                .set("updatedAt", now);
        DoctorWorkSlot claimedSlot = mongoTemplate.findAndModify(
                claimQuery,
                claimUpdate,
                FindAndModifyOptions.options().returnNew(true),
                DoctorWorkSlot.class);
        if (claimedSlot == null) {
            throw new ConflictException("The selected doctor work slot is no longer available.");
        }

        return appointmentRepository.save(Appointment.builder()
                .patientId(patient.getId())
                .doctorWorkSlotId(claimedSlot.getId())
                .status(AppointmentStatus.PENDING_STAFF_CONFIRMATION)
                .requestedAt(now)
                .createdAt(now)
                .updatedAt(now)
                .build());
    }

    @Override
    public List<Appointment> getPatientAppointments(String patientUserId) {
        requirePatient(patientUserId);
        return appointmentRepository.findAllForPatient(patientUserId);
    }

    @Override
    public List<Appointment> getPendingAppointments(String staffId) {
        requireStaff(staffId);
        return appointmentRepository.findAllByStatusOrderByRequestedAtAsc(
                AppointmentStatus.PENDING_STAFF_CONFIRMATION);
    }

    @Override
    public List<Appointment> getAppointments(String staffId, AppointmentStatus status) {
        requireStaff(staffId);
        return status == null
                ? appointmentRepository.findAllByOrderByRequestedAtDesc()
                : appointmentRepository.findAllByStatusOrderByRequestedAtDesc(status);
    }

    @Override
    @Transactional
    public Appointment decide(String staffId, String appointmentId, AppointmentDecisionRequest request) {
        requireStaff(staffId);
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment was not found."));
        if (appointment.getStatus() != AppointmentStatus.PENDING_STAFF_CONFIRMATION) {
            throw new ConflictException("Only pending appointments can be reviewed.");
        }
        DoctorWorkSlot slot = doctorWorkSlotRepository.findById(appointment.getDoctorWorkSlotId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor work slot was not found."));
        if (slot.getStatus() != DoctorWorkSlotStatus.SCHEDULING) {
            throw new ConflictException("Doctor work slot is not awaiting appointment confirmation.");
        }

        String rejectionReason = trimToNull(request.getRejectionReason());
        if (request.getDecision() == ScheduleDecision.REJECT && rejectionReason == null) {
            throw new BadRequestException("Rejection reason is required when rejecting an appointment.");
        }
        if (request.getDecision() == ScheduleDecision.APPROVE && !slot.getStartAt().isAfter(Instant.now())) {
            throw new ConflictException("An appointment cannot be confirmed after its work slot has started.");
        }

        Instant reviewedAt = Instant.now();
        appointment.setReviewedBy(staffId);
        appointment.setReviewedAt(reviewedAt);
        appointment.setUpdatedAt(reviewedAt);
        slot.setUpdatedAt(reviewedAt);
        if (request.getDecision() == ScheduleDecision.APPROVE) {
            appointment.setStatus(AppointmentStatus.CONFIRMED);
            appointment.setRejectionReason(null);
            slot.setStatus(DoctorWorkSlotStatus.BOOKED);
        } else {
            appointment.setStatus(AppointmentStatus.REJECTED);
            appointment.setRejectionReason(rejectionReason);
            appointment.setActive(false);
            slot.setStatus(DoctorWorkSlotStatus.AVAILABLE);
        }
        doctorWorkSlotRepository.save(slot);
        return appointmentRepository.save(appointment);
    }

    @Override
    @Transactional
    public Appointment cancel(String staffId, String appointmentId, CancelAppointmentRequest request) {
        requireStaff(staffId);
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment was not found."));
        if (appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            throw new ConflictException("Only confirmed appointments can be cancelled.");
        }
        return cancelAppointment(appointment, staffId, request.getCancellationReason());
    }

    @Override
    @Transactional
    public Appointment cancelByPatient(
            String patientUserId,
            String appointmentId,
            CancelAppointmentRequest request) {
        User patient = requirePatient(patientUserId);
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment was not found."));
        String ownerId = patient.getId().equals(appointment.getPatientId())
                ? appointment.getPatientId()
                : appointment.getPatientUserId();
        if (!patient.getId().equals(ownerId)) {
            throw new ForbiddenException("Patients can only cancel their own appointments.");
        }
        if (appointment.getStatus() != AppointmentStatus.PENDING_STAFF_CONFIRMATION
                && appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            throw new ConflictException("Only pending or confirmed appointments can be cancelled by a patient.");
        }
        return cancelAppointment(appointment, patient.getId(), request.getCancellationReason());
    }

    private Appointment cancelAppointment(Appointment appointment, String cancelledBy, String cancellationReason) {
        DoctorWorkSlot slot = doctorWorkSlotRepository.findById(appointment.getDoctorWorkSlotId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor work slot was not found."));
        DoctorWorkSlotStatus expectedBookingStatus = appointment.getStatus()
                == AppointmentStatus.PENDING_STAFF_CONFIRMATION
                        ? DoctorWorkSlotStatus.SCHEDULING
                        : DoctorWorkSlotStatus.BOOKED;
        if (slot.getStatus() != expectedBookingStatus) {
            throw new ConflictException("Doctor work slot status does not match this appointment.");
        }
        if (!slot.getStartAt().isAfter(Instant.now())) {
            throw new ConflictException("An appointment cannot be cancelled after its work slot has started.");
        }

        Instant cancelledAt = Instant.now();
        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setActive(false);
        appointment.setCancelledBy(cancelledBy);
        appointment.setCancelledAt(cancelledAt);
        appointment.setCancellationReason(cancellationReason.trim());
        appointment.setUpdatedAt(cancelledAt);
        slot.setStatus(DoctorWorkSlotStatus.AVAILABLE);
        slot.setUpdatedAt(cancelledAt);

        doctorWorkSlotRepository.save(slot);
        return appointmentRepository.save(appointment);
    }

    @Override
    public Map<String, String> getDoctorNames(List<DoctorWorkSlot> slots) {
        return userRepository.findAllById(slots.stream().map(DoctorWorkSlot::getDoctorId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(User::getId, User::getFullName));
    }

    @Override
    public AppointmentResponse toResponse(Appointment appointment) {
        return toResponses(List.of(appointment)).get(0);
    }

    @Override
    public List<AppointmentResponse> toResponses(List<Appointment> appointments) {
        Map<String, DoctorWorkSlot> workSlots = doctorWorkSlotRepository.findAllById(
                        appointments.stream().map(Appointment::getDoctorWorkSlotId)
                                .filter(id -> id != null && !id.isBlank()).distinct().toList())
                .stream()
                .collect(Collectors.toMap(DoctorWorkSlot::getId, slot -> slot));
        Map<String, com.yourproject.backend.models.WorkSlot> slotDefinitions = workSlotRepository.findAllById(
                        workSlots.values().stream().map(DoctorWorkSlot::getSlotId)
                                .filter(id -> id != null && !id.isBlank()).distinct().toList())
                .stream().collect(Collectors.toMap(com.yourproject.backend.models.WorkSlot::getId, slot -> slot));
        Map<String, com.yourproject.backend.models.ClinicRoom> rooms = clinicRoomRepository.findAllById(
                        workSlots.values().stream().map(DoctorWorkSlot::getRoomId)
                                .filter(id -> id != null && !id.isBlank()).distinct().toList())
                .stream().collect(Collectors.toMap(com.yourproject.backend.models.ClinicRoom::getId, room -> room));
        appointments.forEach(appointment -> {
            DoctorWorkSlot slot = workSlots.get(appointment.getDoctorWorkSlotId());
            if (slot == null) return;
            appointment.setDoctorId(slot.getDoctorId());
            appointment.setSlotId(slot.getSlotId());
            appointment.setRoomId(slot.getRoomId());
            appointment.setAppointmentDate(slot.getWorkDate());
            appointment.setStartAt(slot.getStartAt());
            appointment.setEndAt(slot.getEndAt());
        });
        List<String> userIds = appointments.stream()
                .flatMap(appointment -> java.util.stream.Stream.of(
                        appointment.getDoctorId(),
                        appointment.getPatientId(),
                        appointment.getPatientUserId()))
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .toList();
        Map<String, User> users = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));
        appointments.forEach(appointment -> {
            if (!users.containsKey(appointment.getPatientId())
                    && users.containsKey(appointment.getPatientUserId())) {
                appointment.setPatientId(appointment.getPatientUserId());
            }
        });
        return appointments.stream()
                .map(appointment -> {
                    DoctorWorkSlot doctorWorkSlot = workSlots.get(appointment.getDoctorWorkSlotId());
                    return AppointmentResponse.from(appointment, users.get(appointment.getDoctorId()),
                            users.getOrDefault(appointment.getPatientId(), users.get(appointment.getPatientUserId())),
                            doctorWorkSlot,
                            doctorWorkSlot == null ? null : slotDefinitions.get(doctorWorkSlot.getSlotId()),
                            doctorWorkSlot == null ? null : rooms.get(doctorWorkSlot.getRoomId()),
                            patientDataProtectionService);
                })
                .toList();
    }

    private void validateBookableSlot(DoctorWorkSlot slot) {
        if (slot.getStatus() != DoctorWorkSlotStatus.AVAILABLE) {
            throw new ConflictException("The selected doctor work slot is not available.");
        }
        Instant now = Instant.now();
        if (slot.getStartAt().isBefore(now.plus(MINIMUM_BOOKING_LEAD))) {
            throw new BadRequestException("Appointments must be booked at least 12 hours in advance.");
        }
        if (slot.getStartAt().isAfter(now.plus(MAXIMUM_BOOKING_LEAD))) {
            throw new BadRequestException("Appointments cannot be booked more than 30 days in advance.");
        }
    }

    private User requirePatient(String userId) {
        User patient = userService.getActiveUserById(userId);
        if (!patient.getRoles().contains(UserRole.PATIENT)) {
            throw new ForbiddenException("Only patients can use patient appointment operations.");
        }
        return patient;
    }

    private void requireStaff(String userId) {
        User staff = userService.getActiveUserById(userId);
        if (!staff.getRoles().contains(UserRole.STAFF)) {
            throw new ForbiddenException("Only staff can review appointments.");
        }
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
