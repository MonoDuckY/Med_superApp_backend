package com.yourproject.backend.repositories;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.yourproject.backend.models.MedicineSchedule;
import com.yourproject.backend.models.MedicineScheduleStatus;

public interface MedicineScheduleRepository extends MongoRepository<MedicineSchedule, String> {
    List<MedicineSchedule> findAllByPrescriptionIdOrderByScheduledAtAsc(String prescriptionId);
    List<MedicineSchedule> findAllByPrescriptionIdInOrderByScheduledAtAsc(Collection<String> prescriptionIds);
    boolean existsByPrescriptionIdAndMedicineNameAndDosageAndScheduledAt(
            String prescriptionId, String medicineName, String dosage, Instant scheduledAt);
    void deleteAllByPrescriptionId(String prescriptionId);
    List<MedicineSchedule> findAllByStatusAndScheduledAtBefore(
            MedicineScheduleStatus status, Instant scheduledAt);

    @Query("{'status': 'NOT_YET', 'scheduledAt': {'$gt': ?0, '$lte': ?1}, '$or': "
            + "[{'isNotified': false}, {'isNotified': {'$exists': false}}]}")
    List<MedicineSchedule> findPendingReminders(Instant after, Instant through);
}
