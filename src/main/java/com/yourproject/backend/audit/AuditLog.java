package com.yourproject.backend.audit;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "audit_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {
    @Id
    private String id;
    private Instant timestamp;
    private String actorUserId;
    private String actorRole;
    private String action;
    private String requestMethod;
    private String requestPath;
    private String queryString;
    private Integer statusCode;
    private Long durationMs;
    private String clientIp;
    private String backendInstanceId;
    private String backendHostName;
    private String backendIp;
}
