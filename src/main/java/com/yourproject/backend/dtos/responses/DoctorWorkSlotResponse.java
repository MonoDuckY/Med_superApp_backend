package com.yourproject.backend.dtos.responses;

import java.time.Instant;
import java.time.LocalDate;

import com.yourproject.backend.models.DoctorWorkSlot;
import com.yourproject.backend.models.WorkSlotApprovalStatus;
import com.yourproject.backend.models.WorkSlotBookingStatus;

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
    private String slotName;
    private String roomId;
    private String roomCode;
    private Instant startAt;
    private Instant endAt;
    private WorkSlotApprovalStatus approvalStatus;
    private WorkSlotBookingStatus bookingStatus;
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
                .slotName(slot.getSlotName())
                .roomId(slot.getRoomId())
                .roomCode(slot.getRoomCode())
                .startAt(slot.getStartAt())
                .endAt(slot.getEndAt())
                .approvalStatus(slot.getApprovalStatus())
                .bookingStatus(slot.getBookingStatus())
                .note(slot.getNote())
                .submittedAt(slot.getSubmittedAt())
                .reviewedBy(slot.getReviewedBy())
                .reviewedAt(slot.getReviewedAt())
                .rejectionReason(slot.getRejectionReason())
                .build();
    }
}
