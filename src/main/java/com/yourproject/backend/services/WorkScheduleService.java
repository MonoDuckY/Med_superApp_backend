package com.yourproject.backend.services;

import java.time.LocalDate;
import java.util.List;

import com.yourproject.backend.dtos.requests.ScheduleDecisionRequest;
import com.yourproject.backend.dtos.requests.SubmitWorkScheduleRequest;
import com.yourproject.backend.dtos.responses.WorkScheduleSubmissionResponse;
import com.yourproject.backend.models.DoctorWorkSlot;

public interface WorkScheduleService {
    List<DoctorWorkSlot> submit(String doctorId, SubmitWorkScheduleRequest request);

    List<DoctorWorkSlot> getDoctorSchedules(String doctorId, LocalDate from, LocalDate to);

    List<DoctorWorkSlot> getPendingSchedules(String staffId);

    List<DoctorWorkSlot> getSchedules(String staffId, com.yourproject.backend.models.WorkSlotApprovalStatus status);

    List<DoctorWorkSlot> decide(String staffId, String submissionId, ScheduleDecisionRequest request);

    void cancelPendingSubmission(String doctorId, String submissionId);

    WorkScheduleSubmissionResponse toResponse(List<DoctorWorkSlot> slots);

    List<WorkScheduleSubmissionResponse> toResponses(List<DoctorWorkSlot> slots);
}
