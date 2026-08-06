package com.yourproject.backend.repositories;

import java.util.Optional;
import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.yourproject.backend.models.User;

public interface UserRepository extends MongoRepository<User, String> {
    java.util.Optional<User> findByRefreshTokenHash(String refreshTokenHash);
    Optional<User> findByPhoneLookupAndRoleId(String phoneLookup, String roleId);

    Optional<User> findByPhoneNumberAndRoleId(String phoneNumber, String roleId);

    boolean existsByPhoneLookupAndRoleId(String phoneLookup, String roleId);

    boolean existsByPatientIdLookup(String patientIdLookup);

    @Query("{'status': 'ACTIVE', 'roleId': 'DOCTOR'}")
    List<User> findActiveDoctors();

    @Query("{'status': 'ACTIVE', 'roleId': 'PATIENT'}")
    List<User> findActivePatients();
}
