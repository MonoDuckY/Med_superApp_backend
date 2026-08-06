package com.yourproject.backend.services.impl;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yourproject.backend.dtos.requests.ScheduleDecisionRequest;
import com.yourproject.backend.dtos.requests.SubmitWorkScheduleRequest;
import com.yourproject.backend.dtos.requests.BlockWorkSlotRequest;
import com.yourproject.backend.dtos.responses.WorkScheduleSubmissionResponse;
import com.yourproject.backend.exceptions.BadRequestException;
import com.yourproject.backend.exceptions.ConflictException;
import com.yourproject.backend.exceptions.ForbiddenException;
import com.yourproject.backend.exceptions.ResourceNotFoundException;
import com.yourproject.backend.models.ClinicRoom;
import com.yourproject.backend.models.DoctorWorkSlot;
import com.yourproject.backend.models.ScheduleDecision;
import com.yourproject.backend.models.User;
import com.yourproject.backend.models.UserRole;
import com.yourproject.backend.models.WorkSession;
import com.yourproject.backend.models.WorkSlot;
import com.yourproject.backend.models.DoctorWorkSlotStatus;
import com.yourproject.backend.models.Appointment;
import com.yourproject.backend.models.AppointmentStatus;
import com.yourproject.backend.repositories.AppointmentRepository;
import com.yourproject.backend.repositories.ClinicRoomRepository;
import com.yourproject.backend.repositories.DoctorWorkSlotRepository;
import com.yourproject.backend.repositories.WorkSlotRepository;
import com.yourproject.backend.repositories.UserRepository;
import com.yourproject.backend.services.PatientDataProtectionService;
import com.yourproject.backend.services.UserService;
import com.yourproject.backend.services.WorkScheduleService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WorkScheduleServiceImpl implements WorkScheduleService {
    private static final ZoneId HOSPITAL_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final DoctorWorkSlotRepository doctorWorkSlotRepository;
    private final WorkSlotRepository workSlotRepository;
    private final ClinicRoomRepository clinicRoomRepository;
    private final UserService userService;
    private final UserRepository userRepository;
    private final PatientDataProtectionService patientDataProtectionService;
    private final AppointmentRepository appointmentRepository;

    @Override
    @Transactional
    public List<DoctorWorkSlot> submit(String doctorId, SubmitWorkScheduleRequest request) {
        User doctor = requireRole(doctorId, UserRole.DOCTOR, "Only doctors can submit work schedules.");
        if (request.getWorkDate().isBefore(LocalDate.now(HOSPITAL_ZONE))) {
            throw new BadRequestException("Work date cannot be in the past.");
        }

        ClinicRoom room = clinicRoomRepository.findById(request.getRoomId().trim())
                .filter(ClinicRoom::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Active clinic room was not found."));
        List<WorkSlot> selectedSlots = getSlots(request.getSession());
        if (selectedSlots.isEmpty()) {
            throw new BadRequestException("No active work slots are available for the selected session.");
        }

        Instant now = Instant.now();
        if (selectedSlots.stream().map(slot -> toInstant(request.getWorkDate(), slot))
                .anyMatch(startAt -> !startAt.isAfter(now))) {
            throw new BadRequestException("Every selected work slot must start in the future.");
        }

        Set<String> slotIds = selectedSlots.stream().map(WorkSlot::getId).collect(Collectors.toSet());
        List<DoctorWorkSlot> conflicts = doctorWorkSlotRepository
                .findAllByWorkDateAndSlotIdIn(request.getWorkDate(), slotIds)
                .stream()
                .filter(existing -> existing.getStatus() != DoctorWorkSlotStatus.REJECTED
                        && existing.getStatus() != DoctorWorkSlotStatus.CANCELLED
                        && existing.getStatus() != DoctorWorkSlotStatus.CLOSED)
                .filter(existing -> existing.getDoctorId().equals(doctor.getId())
                        || existing.getRoomId().equals(room.getId()))
                .toList();
        if (!conflicts.isEmpty()) {
            String names = conflicts.stream().map(DoctorWorkSlot::getSlotId).distinct().sorted()
                    .collect(Collectors.joining(", "));
            throw new ConflictException("Work schedule conflicts with existing slots: " + names + ".");
        }

        String note = trimToNull(request.getNote());
        String submissionId = UUID.randomUUID().toString();
        List<DoctorWorkSlot> documents = new ArrayList<>();
        for (WorkSlot slot : selectedSlots) {
            documents.add(DoctorWorkSlot.builder()
                    .submissionId(submissionId)
                    .doctorId(doctor.getId())
                    .workDate(request.getWorkDate())
                    .slotId(slot.getId())
                    .roomId(room.getId())
                    .status(DoctorWorkSlotStatus.PENDING)
                    .note(note)
                    .submittedAt(now)
                    .conflictActive(true)
                    .updatedAt(now)
                    .build());
        }
        return doctorWorkSlotRepository.saveAll(documents);
    }

    @Override
    public List<DoctorWorkSlot> getDoctorSchedules(String doctorId, LocalDate from, LocalDate to) {
        requireRole(doctorId, UserRole.DOCTOR, "Only doctors can view doctor work schedules.");
        LocalDate effectiveFrom = from == null ? LocalDate.now(HOSPITAL_ZONE) : from;
        LocalDate effectiveTo = to == null ? effectiveFrom.plusDays(30) : to;
        if (effectiveTo.isBefore(effectiveFrom)) {
            throw new BadRequestException("The end date cannot be before the start date.");
        }
        return doctorWorkSlotRepository.findAllByDoctorIdAndWorkDateBetweenOrderByWorkDateAscSlotIdAsc(
                doctorId,
                effectiveFrom,
                effectiveTo);
    }

    @Override
    public List<DoctorWorkSlot> getPendingSchedules(String staffId) {
        requireStaff(staffId);
        return doctorWorkSlotRepository.findAllByStatusOrderBySubmittedAtAsc(DoctorWorkSlotStatus.PENDING);
    }

    @Override
    public List<DoctorWorkSlot> getSchedules(String staffId, DoctorWorkSlotStatus status) {
        requireStaff(staffId);
        return status == null
                ? doctorWorkSlotRepository.findAllByOrderBySubmittedAtDesc()
                : doctorWorkSlotRepository.findAllByStatusOrderBySubmittedAtDesc(status);
    }

    @Override
    @Transactional
    public List<DoctorWorkSlot> decide(String staffId, String submissionId, ScheduleDecisionRequest request) {
        requireStaff(staffId);
        List<DoctorWorkSlot> slots = doctorWorkSlotRepository.findAllBySubmissionIdOrderBySlotIdAsc(submissionId);
        if (slots.isEmpty()) {
            throw new ResourceNotFoundException("Work schedule submission was not found.");
        }
        if (slots.stream().anyMatch(slot -> slot.getStatus() != DoctorWorkSlotStatus.PENDING)) {
            throw new ConflictException("Only pending work schedule submissions can be reviewed.");
        }

        String rejectionReason = trimToNull(request.getRejectionReason());
        if (request.getDecision() == ScheduleDecision.REJECT && rejectionReason == null) {
            throw new BadRequestException("Rejection reason is required when rejecting a work schedule.");
        }
        if (request.getDecision() == ScheduleDecision.APPROVE
                && slots.stream().anyMatch(slot -> !startInstant(slot).isAfter(Instant.now()))) {
            throw new ConflictException("A work schedule with elapsed slots cannot be approved.");
        }

        Instant reviewedAt = Instant.now();
        for (DoctorWorkSlot slot : slots) {
            slot.setReviewedBy(staffId);
            slot.setReviewedAt(reviewedAt);
            slot.setUpdatedAt(reviewedAt);
            if (request.getDecision() == ScheduleDecision.APPROVE) {
                slot.setStatus(DoctorWorkSlotStatus.AVAILABLE);
                slot.setRejectionReason(null);
            } else {
                slot.setStatus(DoctorWorkSlotStatus.REJECTED);
                slot.setRejectionReason(rejectionReason);
                slot.setConflictActive(false);
            }
        }
        return doctorWorkSlotRepository.saveAll(slots);
    }

    @Override
    @Transactional
    public void cancelPendingSubmission(String doctorId, String submissionId) {
        requireRole(doctorId, UserRole.DOCTOR, "Only doctors can cancel doctor work schedules.");
        List<DoctorWorkSlot> slots = doctorWorkSlotRepository
                .findAllBySubmissionIdAndDoctorIdOrderBySlotIdAsc(submissionId, doctorId);
        if (slots.isEmpty()) {
            throw new ResourceNotFoundException("Work schedule submission was not found.");
        }
        if (slots.stream().anyMatch(slot -> slot.getStatus() != DoctorWorkSlotStatus.PENDING)) {
            throw new ConflictException("Only pending work schedule submissions can be cancelled by a doctor.");
        }

        Instant now = Instant.now();
        slots.forEach(slot -> {
            slot.setStatus(DoctorWorkSlotStatus.CANCELLED);
            slot.setConflictActive(false);
            slot.setUpdatedAt(now);
        });
        doctorWorkSlotRepository.saveAll(slots);
    }

    @Override
    @Transactional
    public List<DoctorWorkSlot> modifyPendingSubmission(
            String doctorId,
            String submissionId,
            SubmitWorkScheduleRequest request) {
        requireRole(doctorId, UserRole.DOCTOR, "Only doctors can modify pending work schedules.");
        List<DoctorWorkSlot> current = doctorWorkSlotRepository
                .findAllBySubmissionIdAndDoctorIdOrderBySlotIdAsc(submissionId, doctorId);
        if (current.isEmpty()) {
            throw new ResourceNotFoundException("Work schedule submission was not found.");
        }
        if (current.stream().anyMatch(slot -> slot.getStatus() != DoctorWorkSlotStatus.PENDING)) {
            throw new ConflictException("Only pending work schedule submissions can be modified by a doctor.");
        }
        return replaceSubmission(current, request, DoctorWorkSlotStatus.PENDING, null);
    }

    @Override
    @Transactional
    public List<DoctorWorkSlot> modifyApprovedSubmission(
            String staffId,
            String submissionId,
            SubmitWorkScheduleRequest request) {
        requireStaff(staffId);
        List<DoctorWorkSlot> current = doctorWorkSlotRepository
                .findAllBySubmissionIdOrderBySlotIdAsc(submissionId);
        if (current.isEmpty()) {
            throw new ResourceNotFoundException("Work schedule submission was not found.");
        }
        if (current.stream().anyMatch(slot -> slot.getStatus() != DoctorWorkSlotStatus.AVAILABLE)) {
            throw new ConflictException(
                    "Only approved work schedules without active appointments can be modified.");
        }
        return replaceSubmission(current, request, DoctorWorkSlotStatus.AVAILABLE, staffId);
    }

    @Override
    @Transactional
    public DoctorWorkSlot blockSlot(
            String staffId,
            String doctorWorkSlotId,
            BlockWorkSlotRequest request) {
        requireStaff(staffId);
        DoctorWorkSlot slot = doctorWorkSlotRepository.findById(doctorWorkSlotId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor work slot was not found."));
        if (slot.getStatus() != DoctorWorkSlotStatus.AVAILABLE
                && slot.getStatus() != DoctorWorkSlotStatus.SCHEDULING
                && slot.getStatus() != DoctorWorkSlotStatus.BOOKED) {
            throw new ConflictException("Only available or reserved work slots can be blocked.");
        }

        Instant now = Instant.now();
        appointmentRepository.findFirstByDoctorWorkSlotIdAndStatusIn(
                        slot.getId(),
                        List.of(AppointmentStatus.PENDING_STAFF_CONFIRMATION, AppointmentStatus.CONFIRMED))
                .ifPresent(appointment -> cancelAffectedAppointment(appointment, staffId, request.getReason(), now));

        slot.setStatus(DoctorWorkSlotStatus.CANCELLED);
        slot.setConflictActive(false);
        slot.setReviewedBy(staffId);
        slot.setReviewedAt(now);
        slot.setRejectionReason(request.getReason().trim());
        slot.setUpdatedAt(now);
        return doctorWorkSlotRepository.save(slot);
    }

    @Override
    public WorkScheduleSubmissionResponse toResponse(List<DoctorWorkSlot> slots) {
        if (slots == null || slots.isEmpty()) {
            throw new IllegalArgumentException("A work schedule submission must contain at least one slot.");
        }
        User doctor = userRepository.findById(slots.get(0).getDoctorId()).orElse(null);
        return WorkScheduleSubmissionResponse.from(slots, doctor, patientDataProtectionService);
    }

    @Override
    public List<WorkScheduleSubmissionResponse> toResponses(List<DoctorWorkSlot> slots) {
        List<String> doctorIds = slots.stream().map(DoctorWorkSlot::getDoctorId).distinct().toList();
        Map<String, User> doctors = userRepository.findAllById(doctorIds).stream()
                .collect(Collectors.toMap(User::getId, doctor -> doctor));
        return WorkScheduleSubmissionResponse.group(slots, doctors, patientDataProtectionService);
    }

    private List<WorkSlot> getSlots(WorkSession session) {
        List<WorkSlot> slots = workSlotRepository.findAllByOrderByStartTimeAsc();
        if (session == WorkSession.FULL_TIME) return slots;
        LocalTime noon = LocalTime.NOON;
        return slots.stream()
                .filter(slot -> session == WorkSession.MORNING
                        ? slot.getStartTime().isBefore(noon)
                        : !slot.getStartTime().isBefore(noon))
                .toList();
    }

    private List<DoctorWorkSlot> replaceSubmission(
            List<DoctorWorkSlot> current,
            SubmitWorkScheduleRequest request,
            DoctorWorkSlotStatus targetStatus,
            String reviewedBy) {
        DoctorWorkSlot first = current.get(0);
        if (request.getWorkDate().isBefore(LocalDate.now(HOSPITAL_ZONE))) {
            throw new BadRequestException("Work date cannot be in the past.");
        }
        ClinicRoom room = clinicRoomRepository.findById(request.getRoomId().trim())
                .filter(ClinicRoom::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Active clinic room was not found."));
        List<WorkSlot> selectedSlots = getSlots(request.getSession());
        if (selectedSlots.isEmpty()) {
            throw new BadRequestException("No work slots are available for the selected session.");
        }
        Instant now = Instant.now();
        if (selectedSlots.stream().map(slot -> toInstant(request.getWorkDate(), slot))
                .anyMatch(startAt -> !startAt.isAfter(now))) {
            throw new BadRequestException("Every selected work slot must start in the future.");
        }

        Set<String> selectedIds = selectedSlots.stream().map(WorkSlot::getId).collect(Collectors.toSet());
        boolean conflict = doctorWorkSlotRepository
                .findAllByWorkDateAndSlotIdIn(request.getWorkDate(), selectedIds)
                .stream()
                .filter(existing -> !first.getSubmissionId().equals(existing.getSubmissionId()))
                .filter(existing -> existing.getStatus() != DoctorWorkSlotStatus.REJECTED
                        && existing.getStatus() != DoctorWorkSlotStatus.CANCELLED
                        && existing.getStatus() != DoctorWorkSlotStatus.CLOSED)
                .anyMatch(existing -> existing.getDoctorId().equals(first.getDoctorId())
                        || existing.getRoomId().equals(room.getId()));
        if (conflict) {
            throw new ConflictException("Modified work schedule conflicts with an existing schedule.");
        }

        String note = trimToNull(request.getNote());
        List<DoctorWorkSlot> replacements = selectedSlots.stream()
                .map(definition -> DoctorWorkSlot.builder()
                        .submissionId(first.getSubmissionId())
                        .doctorId(first.getDoctorId())
                        .workDate(request.getWorkDate())
                        .slotId(definition.getId())
                        .roomId(room.getId())
                        .status(targetStatus)
                        .note(note)
                        .submittedAt(first.getSubmittedAt())
                        .reviewedBy(reviewedBy == null ? first.getReviewedBy() : reviewedBy)
                        .reviewedAt(reviewedBy == null ? first.getReviewedAt() : now)
                        .conflictActive(true)
                        .updatedAt(now)
                        .build())
                .toList();
        doctorWorkSlotRepository.deleteAll(current);
        return doctorWorkSlotRepository.saveAll(replacements);
    }

    private void cancelAffectedAppointment(
            Appointment appointment,
            String staffId,
            String reason,
            Instant cancelledAt) {
        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setActive(false);
        appointment.setCancelledBy(staffId);
        appointment.setCancelledAt(cancelledAt);
        appointment.setCancellationReason(reason.trim());
        appointment.setUpdatedAt(cancelledAt);
        appointmentRepository.save(appointment);
    }

    private Instant toInstant(LocalDate workDate, WorkSlot slot) {
        return workDate.atTime(slot.getStartTime()).atZone(HOSPITAL_ZONE).toInstant();
    }

    private Instant startInstant(DoctorWorkSlot doctorWorkSlot) {
        WorkSlot slot = workSlotRepository.findById(doctorWorkSlot.getSlotId())
                .orElseThrow(() -> new ResourceNotFoundException("Work slot definition was not found."));
        return toInstant(doctorWorkSlot.getWorkDate(), slot);
    }

    private User requireRole(String userId, UserRole role, String message) {
        User user = userService.getActiveUserById(userId);
        if (user.getRole() != role) {
            throw new ForbiddenException(message);
        }
        return user;
    }

    private void requireStaff(String userId) {
        User user = userService.getActiveUserById(userId);
        if (user.getRole() != UserRole.STAFF) {
            throw new ForbiddenException("Only staff can review work schedules.");
        }
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
