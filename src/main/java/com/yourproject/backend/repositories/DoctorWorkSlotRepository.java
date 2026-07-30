package com.yourproject.backend.repositories;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.yourproject.backend.models.DoctorWorkSlot;
import com.yourproject.backend.models.WorkSlotApprovalStatus;
import com.yourproject.backend.models.WorkSlotBookingStatus;

public interface DoctorWorkSlotRepository extends MongoRepository<DoctorWorkSlot, String> {
    List<DoctorWorkSlot> findAllBySubmissionIdOrderByStartAtAsc(String submissionId);

    List<DoctorWorkSlot> findAllBySubmissionIdAndDoctorIdOrderByStartAtAsc(String submissionId, String doctorId);

    List<DoctorWorkSlot> findAllByDoctorIdAndWorkDateBetweenOrderByStartAtAsc(
            String doctorId,
            LocalDate from,
            LocalDate to);

    List<DoctorWorkSlot> findAllByApprovalStatusOrderBySubmittedAtAsc(WorkSlotApprovalStatus approvalStatus);

    List<DoctorWorkSlot> findAllByApprovalStatusOrderBySubmittedAtDesc(WorkSlotApprovalStatus approvalStatus);

    List<DoctorWorkSlot> findAllByOrderBySubmittedAtDesc();

    List<DoctorWorkSlot> findAllByWorkDateAndSlotIdInAndConflictActiveTrue(
            LocalDate workDate,
            Collection<String> slotIds);

    List<DoctorWorkSlot> findAllByApprovalStatusAndBookingStatusAndStartAtBetweenOrderByStartAtAsc(
            WorkSlotApprovalStatus approvalStatus,
            WorkSlotBookingStatus bookingStatus,
            Instant from,
            Instant to);
}
