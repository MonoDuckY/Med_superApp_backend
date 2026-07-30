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
            Instant startAt = toInstant(request.getWorkDate(), slot);
            documents.add(DoctorWorkSlot.builder()
                    .submissionId(submissionId)
                    .doctorId(doctor.getId())
                    .workDate(request.getWorkDate())
                    .slotId(slot.getId())
                    .roomId(room.getId())
                    .startAt(startAt)
                    .endAt(request.getWorkDate().atTime(slot.getEndTime()).atZone(HOSPITAL_ZONE).toInstant())
                    .status(DoctorWorkSlotStatus.PENDING)
                    .note(note)
                    .submittedAt(now)
                    .conflictActive(true)
                    .createdAt(now)
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
        return doctorWorkSlotRepository.findAllByDoctorIdAndWorkDateBetweenOrderByStartAtAsc(
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
        List<DoctorWorkSlot> slots = doctorWorkSlotRepository.findAllBySubmissionIdOrderByStartAtAsc(submissionId);
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
                && slots.stream().anyMatch(slot -> !slot.getStartAt().isAfter(Instant.now()))) {
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
                .findAllBySubmissionIdAndDoctorIdOrderByStartAtAsc(submissionId, doctorId);
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

    private Instant toInstant(LocalDate workDate, WorkSlot slot) {
        return workDate.atTime(slot.getStartTime()).atZone(HOSPITAL_ZONE).toInstant();
    }

    private User requireRole(String userId, UserRole role, String message) {
        User user = userService.getActiveUserById(userId);
        if (!user.getRoles().contains(role)) {
            throw new ForbiddenException(message);
        }
        return user;
    }

    private void requireStaff(String userId) {
        User user = userService.getActiveUserById(userId);
        if (!user.getRoles().contains(UserRole.STAFF)) {
            throw new ForbiddenException("Only staff can review work schedules.");
        }
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
