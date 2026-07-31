package com.yourproject.backend.repositories;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.yourproject.backend.models.DoctorWorkSlot;
import com.yourproject.backend.models.DoctorWorkSlotStatus;

public interface DoctorWorkSlotRepository extends MongoRepository<DoctorWorkSlot, String> {
    List<DoctorWorkSlot> findAllBySubmissionIdOrderBySlotIdAsc(String submissionId);

    List<DoctorWorkSlot> findAllBySubmissionIdAndDoctorIdOrderBySlotIdAsc(String submissionId, String doctorId);

    List<DoctorWorkSlot> findAllByDoctorIdAndWorkDateBetweenOrderByWorkDateAscSlotIdAsc(
            String doctorId,
            LocalDate from,
            LocalDate to);

    List<DoctorWorkSlot> findAllByStatusOrderBySubmittedAtAsc(DoctorWorkSlotStatus status);

    List<DoctorWorkSlot> findAllByStatusOrderBySubmittedAtDesc(DoctorWorkSlotStatus status);

    List<DoctorWorkSlot> findAllByOrderBySubmittedAtDesc();

    List<DoctorWorkSlot> findAllByWorkDateAndSlotIdInAndConflictActiveTrue(
            LocalDate workDate,
            Collection<String> slotIds);

    List<DoctorWorkSlot> findAllByWorkDateAndSlotIdIn(LocalDate workDate, Collection<String> slotIds);

    List<DoctorWorkSlot> findAllByStatusOrderByWorkDateAscSlotIdAsc(DoctorWorkSlotStatus status);
}
