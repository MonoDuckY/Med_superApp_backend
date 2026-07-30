package com.yourproject.backend.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.yourproject.backend.models.ClinicRoom;

public interface ClinicRoomRepository extends MongoRepository<ClinicRoom, String> {
    Optional<ClinicRoom> findByCode(String code);

    boolean existsByCode(String code);

    List<ClinicRoom> findAllByActiveTrueOrderByCodeAsc();
}
