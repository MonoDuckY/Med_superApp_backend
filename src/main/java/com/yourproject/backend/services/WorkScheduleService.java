package com.yourproject.backend.services;

import java.time.LocalDate;
import java.util.List;

import com.yourproject.backend.dtos.requests.ScheduleDecisionRequest;
import com.yourproject.backend.dtos.requests.SubmitWorkScheduleRequest;
import com.yourproject.backend.dtos.requests.BlockWorkSlotRequest;
import com.yourproject.backend.dtos.responses.WorkScheduleSubmissionResponse;
import com.yourproject.backend.models.DoctorWorkSlot;
import com.yourproject.backend.models.DoctorWorkSlotStatus;

public interface WorkScheduleService {
    List<DoctorWorkSlot> submit(String doctorId, SubmitWorkScheduleRequest request);

    List<DoctorWorkSlot> getDoctorSchedules(String doctorId, LocalDate from, LocalDate to);

    List<DoctorWorkSlot> getAllDoctorSchedules(
            String requestingDoctorId,
            LocalDate from,
            LocalDate to,
            DoctorWorkSlotStatus status);

    List<DoctorWorkSlot> getPendingSchedules(String staffId);

    List<DoctorWorkSlot> getSchedules(String staffId, com.yourproject.backend.models.DoctorWorkSlotStatus status);

    List<DoctorWorkSlot> decide(String staffId, String submissionId, ScheduleDecisionRequest request);

    void cancelPendingSubmission(String doctorId, String submissionId);

    List<DoctorWorkSlot> modifyPendingSubmission(
            String doctorId,
            String submissionId,
            SubmitWorkScheduleRequest request);

    List<DoctorWorkSlot> modifyApprovedSubmission(
            String staffId,
            String submissionId,
            SubmitWorkScheduleRequest request);

    DoctorWorkSlot blockSlot(String staffId, String doctorWorkSlotId, BlockWorkSlotRequest request);

    WorkScheduleSubmissionResponse toResponse(List<DoctorWorkSlot> slots);

    List<WorkScheduleSubmissionResponse> toResponses(List<DoctorWorkSlot> slots);
}
