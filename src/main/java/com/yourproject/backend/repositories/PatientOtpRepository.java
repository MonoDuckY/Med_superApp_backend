package com.yourproject.backend.repositories;

import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import com.yourproject.backend.models.PatientOtp;
import com.yourproject.backend.models.OtpPurpose;

public interface PatientOtpRepository extends MongoRepository<PatientOtp, String> {
    Optional<PatientOtp> findTopByPhoneLookupAndPurposeOrderByCreatedAtDesc(String phoneLookup, OtpPurpose purpose);
    java.util.List<PatientOtp> findAllByPhoneLookupAndPurposeAndConsumedAtIsNull(String phoneLookup, OtpPurpose purpose);
}
