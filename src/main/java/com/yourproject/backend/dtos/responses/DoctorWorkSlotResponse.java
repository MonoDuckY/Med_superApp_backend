package com.yourproject.backend.dtos.responses;

import java.time.Instant;
import java.time.LocalDate;

import com.yourproject.backend.models.DoctorWorkSlot;
import com.yourproject.backend.models.DoctorWorkSlotStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DoctorWorkSlotResponse {
    private String id;
    private String submissionId;
    private String doctorId;
    private LocalDate workDate;
    private String slotId;
    private String roomId;
    private Instant startAt;
    private Instant endAt;
    private DoctorWorkSlotStatus status;
    private String note;
    private Instant submittedAt;
    private String reviewedBy;
    private Instant reviewedAt;
    private String rejectionReason;

    public static DoctorWorkSlotResponse from(DoctorWorkSlot slot) {
        return DoctorWorkSlotResponse.builder()
                .id(slot.getId())
                .submissionId(slot.getSubmissionId())
                .doctorId(slot.getDoctorId())
                .workDate(slot.getWorkDate())
                .slotId(slot.getSlotId())
                .roomId(slot.getRoomId())
                .startAt(slot.getStartAt())
                .endAt(slot.getEndAt())
                .status(slot.getStatus())
                .note(slot.getNote())
                .submittedAt(slot.getSubmittedAt())
                .reviewedBy(slot.getReviewedBy())
                .reviewedAt(slot.getReviewedAt())
                .rejectionReason(slot.getRejectionReason())
                .build();
    }
}
