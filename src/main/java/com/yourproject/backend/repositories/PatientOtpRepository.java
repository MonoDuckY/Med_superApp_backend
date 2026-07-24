package com.yourproject.backend.repositories;

import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import com.yourproject.backend.models.PatientOtp;

public interface PatientOtpRepository extends MongoRepository<PatientOtp, String> {
    Optional<PatientOtp> findTopByPhoneLookupOrderByCreatedAtDesc(String phoneLookup);
}
