package com.yourproject.backend.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.yourproject.backend.models.DiagnosticRecord;

public interface DiagnosticRecordRepository extends MongoRepository<DiagnosticRecord, String> {
}
