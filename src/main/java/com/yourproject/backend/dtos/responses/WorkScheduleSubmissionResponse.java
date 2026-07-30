package com.yourproject.backend.dtos.responses;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.yourproject.backend.models.WorkSlotApprovalStatus;
import com.yourproject.backend.models.User;
import com.yourproject.backend.services.PatientDataProtectionService;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WorkScheduleSubmissionResponse {
    private String submissionId;
    private String doctorId;
    private UserSummaryResponse doctor;
    private LocalDate workDate;
    private WorkSlotApprovalStatus approvalStatus;
    private List<DoctorWorkSlotResponse> slots;

    public static WorkScheduleSubmissionResponse from(List<com.yourproject.backend.models.DoctorWorkSlot> slots) {
        return from(slots, null, null);
    }

    public static WorkScheduleSubmissionResponse from(
            List<com.yourproject.backend.models.DoctorWorkSlot> slots,
            User doctor,
            PatientDataProtectionService patientDataProtectionService) {
        if (slots == null || slots.isEmpty()) {
            throw new IllegalArgumentException("A work schedule submission must contain at least one slot.");
        }
        com.yourproject.backend.models.DoctorWorkSlot first = slots.get(0);
        return WorkScheduleSubmissionResponse.builder()
                .submissionId(first.getSubmissionId())
                .doctorId(first.getDoctorId())
                .doctor(UserSummaryResponse.from(doctor, patientDataProtectionService))
                .workDate(first.getWorkDate())
                .approvalStatus(first.getApprovalStatus())
                .slots(slots.stream().map(DoctorWorkSlotResponse::from).toList())
                .build();
    }

    public static List<WorkScheduleSubmissionResponse> group(
            List<com.yourproject.backend.models.DoctorWorkSlot> slots) {
        return group(slots, Map.of(), null);
    }

    public static List<WorkScheduleSubmissionResponse> group(
            List<com.yourproject.backend.models.DoctorWorkSlot> slots,
            Map<String, User> doctors,
            PatientDataProtectionService patientDataProtectionService) {
        Map<String, List<com.yourproject.backend.models.DoctorWorkSlot>> grouped = new LinkedHashMap<>();
        for (com.yourproject.backend.models.DoctorWorkSlot slot : slots) {
            grouped.computeIfAbsent(slot.getSubmissionId(), ignored -> new java.util.ArrayList<>()).add(slot);
        }
        return grouped.values().stream()
                .map(submission -> from(
                        submission,
                        doctors.get(submission.get(0).getDoctorId()),
                        patientDataProtectionService))
                .toList();
    }
}
