package com.yourproject.backend.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;
import com.yourproject.backend.models.SmsGatewayJob;

public interface SmsGatewayJobRepository extends MongoRepository<SmsGatewayJob, String> {
}
