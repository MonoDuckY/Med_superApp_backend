package com.yourproject.backend.models;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "patient_otps")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientOtp {
    @Id
    private String id;
    @Indexed
    private String userId;
    @Indexed
    private String phoneLookup;
    private String codeHash;
    private int attempts;
    private Instant createdAt;
    @Indexed(expireAfterSeconds = 0)
    private Instant expiresAt;
    private Instant consumedAt;
}
