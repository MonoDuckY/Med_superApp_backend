package com.yourproject.backend.repositories;

import java.util.Optional;
import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import com.yourproject.backend.models.AccountStatus;
import com.yourproject.backend.models.User;

public interface UserRepository extends MongoRepository<User, String> {
    java.util.Optional<User> findByRefreshTokenHash(String refreshTokenHash);
    Optional<User> findByPhoneLookupAndRoleId(String phoneLookup, String roleId);

    Optional<User> findByPhoneNumberAndRoleId(String phoneNumber, String roleId);

    boolean existsByPhoneLookupAndRoleId(String phoneLookup, String roleId);

    List<User> findAllByPhoneLookup(String phoneLookup);

    List<User> findAllByCitizenIdentificationLookup(String citizenIdentificationLookup);

    boolean existsByPatientIdLookup(String patientIdLookup);

    List<User> findAllByStatusAndRoleId(AccountStatus status, String roleId);
}
