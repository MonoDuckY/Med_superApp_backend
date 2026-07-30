package com.yourproject.backend.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.yourproject.backend.models.WorkSession;
import com.yourproject.backend.models.WorkSlot;

public interface WorkSlotRepository extends MongoRepository<WorkSlot, String> {
    Optional<WorkSlot> findByName(String name);

    List<WorkSlot> findAllByActiveTrueOrderByStartTimeAsc();

    List<WorkSlot> findAllByOrderByStartTimeAsc();

    List<WorkSlot> findAllBySessionAndActiveTrueOrderByStartTimeAsc(WorkSession session);
}
