package com.yourproject.backend.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import com.mongodb.client.MongoClient;

@Service
public class AuditLogService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuditLogService.class);
    private final MongoTemplate auditMongoTemplate;

    public AuditLogService(
            MongoClient mongoClient,
            @Value("${app.audit.mongodb.database:audit_logs}") String auditDatabase) {
        this.auditMongoTemplate = new MongoTemplate(mongoClient, auditDatabase);
    }

    public void save(AuditLog auditLog) {
        try {
            auditMongoTemplate.save(auditLog);
        } catch (RuntimeException exception) {
            LOGGER.warn("Unable to persist audit log to the audit MongoDB database: {}", exception.getMessage());
        }
    }
}
