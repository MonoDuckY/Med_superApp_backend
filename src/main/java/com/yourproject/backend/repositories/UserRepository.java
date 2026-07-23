package com.yourproject.backend.repositories;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.yourproject.backend.models.User;

public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByPhoneLookup(String phoneLookup);

    Optional<User> findByPhoneNumber(String phoneNumber);

    boolean existsByPhoneLookup(String phoneLookup);

    boolean existsByPatientIdLookup(String patientIdLookup);
}
