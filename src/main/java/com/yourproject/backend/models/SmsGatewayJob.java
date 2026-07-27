package com.yourproject.backend.models;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "sms_gateway_jobs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SmsGatewayJob {
    @Id
    private String id;
    private String userId;
    private String encryptedPhoneNumber;
    private String encryptedContent;
    private SmsGatewayJobStatus status;
    private Instant createdAt;
    @Indexed(expireAfterSeconds = 0)
    private Instant expiresAt;
    private Instant completedAt;
    private String failureReason;
}
